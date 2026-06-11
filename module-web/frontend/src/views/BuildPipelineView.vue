<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { buildPipelineApi, type BuildPipeline, type TreeRow } from '@/api/build-pipeline'
import { subscriptionApi, type Subscription } from '@/api/subscription'
import { mihomoApi, type MihomoInstance } from '@/api/mihomo'
import { scriptApi } from '@/api/script'
import { configProfileApi, type ConfigProfile } from '@/api/config-profile'
import BuildProgressModal from '@/components/BuildProgressModal.vue'

const router = useRouter()
const treeData = ref<TreeRow[]>([])
const subscriptions = ref<Subscription[]>([])
const instances = ref<MihomoInstance[]>([])
const scriptNames = ref<string[]>([])
const configProfiles = ref<ConfigProfile[]>([])
const loading = ref(false)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新建构建流程')
const form = ref<Partial<BuildPipeline>>({})

// 进度弹窗
const showProgressModal = ref(false)
const currentRecordId = ref('')
const currentPipelineType = ref<'subscription' | 'config-profile'>('subscription')

// 记录缓存
const loadedRecords = ref<Set<string>>(new Set())

const subscriptionMap = computed(() => {
  const map: Record<string, string> = {}
  subscriptions.value.forEach(s => { map[s.id] = s.name })
  return map
})

const instanceMap = computed(() => {
  const map: Record<string, string> = {}
  instances.value.forEach(i => { map[i.id] = i.name })
  return map
})

const loadData = async () => {
  loading.value = true
  try {
    const [pipeRes, subRes, instRes, scriptRes, profileRes] = await Promise.all([
      buildPipelineApi.list(),
      subscriptionApi.list(),
      mihomoApi.list(),
      scriptApi.list(),
      configProfileApi.list(),
    ])
    treeData.value = pipeRes.data.map(p => ({
      id: p.id,
      type: 'pipeline' as const,
      name: p.name,
      hasChildren: true,
      configType: p.configType,
      configProfileId: p.configProfileId,
      primarySubscriptionId: p.primarySubscriptionId,
      additionalSubscriptionIds: p.additionalSubscriptionIds,
      scriptName: p.scriptName,
      targetInstanceId: p.targetInstanceId,
      cronExpression: p.cronExpression,
      enabled: p.enabled,
      lastRunAt: p.lastRunAt,
      lastRunStatus: p.lastRunStatus,
    }))
    subscriptions.value = subRes.data
    instances.value = instRes.data
    scriptNames.value = scriptRes.data
    configProfiles.value = profileRes.data
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadTreeChildren = (row: TreeRow, _treeNode: unknown, resolve: (data: TreeRow[]) => void) => {
  buildPipelineApi.getRecords(row.id).then(res => {
    loadedRecords.value.add(row.id)
    const children: TreeRow[] = res.data.slice(0, 10).map(r => ({
      id: r.id,
      type: 'record' as const,
      name: r.startedAt?.replace('T', ' ').substring(0, 19) || '-',
      buildPipelineId: r.buildPipelineId,
      startedAt: r.startedAt,
      finishedAt: r.finishedAt,
      status: r.status,
      errorMessage: r.errorMessage,
      logs: r.logs,
      steps: r.steps,
    }))
    resolve(children)
  }).catch(() => {
    ElMessage.error('加载构建记录失败')
    resolve([])
  })
}

const openCreateDialog = () => {
  dialogTitle.value = '新建构建流程'
  form.value = {
    name: '',
    configType: 'subscription',
    configProfileId: '',
    primarySubscriptionId: '',
    additionalSubscriptionIds: [],
    scriptName: '',
    targetInstanceId: '',
    cronExpression: '',
    enabled: true,
  }
  dialogVisible.value = true
}

const openEditDialog = (pipeline: TreeRow) => {
  dialogTitle.value = '编辑构建流程'
  form.value = { ...pipeline }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.name || !form.value.targetInstanceId) {
    ElMessage.warning('请填写名称和目标实例')
    return
  }
  if (form.value.configType === 'subscription' && !form.value.primarySubscriptionId) {
    ElMessage.warning('请选择主订阅')
    return
  }
  if (form.value.configType === 'config-profile' && !form.value.configProfileId) {
    ElMessage.warning('请选择配置组合')
    return
  }
  try {
    if (form.value.id) {
      await buildPipelineApi.update(form.value.id, form.value)
    } else {
      await buildPipelineApi.create(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadedRecords.value.clear()
    await loadData()
  } catch {
    ElMessage.error('保存失败')
  }
}

const handleDelete = (pipeline: TreeRow) => {
  ElMessageBox.confirm(`确定删除构建流程「${pipeline.name}」？`, '确认删除', {
    type: 'warning',
  }).then(async () => {
    try {
      await buildPipelineApi.delete(pipeline.id)
      ElMessage.success('删除成功')
      loadedRecords.value.clear()
      await loadData()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleExecute = async (pipeline: TreeRow) => {
  try {
    const res = await buildPipelineApi.execute(pipeline.id)
    const recordId = res.data.recordId

    // 打开进度弹窗
    currentRecordId.value = recordId
    currentPipelineType.value = pipeline.configType || 'subscription'
    showProgressModal.value = true

    // 刷新列表（显示 RUNNING 状态）
    loadedRecords.value.delete(pipeline.id)
    await loadData()
  } catch {
    ElMessage.error('触发构建失败')
  }
}

const handleProgressClose = () => {
  showProgressModal.value = false
  currentRecordId.value = ''
  // 刷新列表
  loadedRecords.value.clear()
  loadData()
}

const goToRecordDetail = (row: TreeRow) => {
  if (row.type === 'record') {
    router.push(`/build-records/${row.id}`)
  }
}

const statusType = (status?: string) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

const statusLabel = (status?: string) => {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'RUNNING') return '运行中'
  return '-'
}

const formatTime = (time?: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const spanMethod = ({ row, column }: { row: TreeRow; column: { property?: string; label?: string } }) => {
  if (row.type !== 'record') return
  const label = column.label
  // 叶子行：脚本列合并4列，目标实例/定时/启用列隐藏；状态列合并操作列
  if (label === '脚本') return { rowspan: 1, colspan: 4 }
  if (label === '目标实例' || label === '定时' || label === '启用') return { rowspan: 0, colspan: 0 }
  if (label === '状态') return { rowspan: 1, colspan: 2 }
  if (label === '操作') return { rowspan: 0, colspan: 0 }
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>构建流程</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建构建流程
      </el-button>
    </div>

    <el-table
      :data="treeData"
      v-loading="loading"
      border
      stripe
      row-key="id"
      lazy
      :load="loadTreeChildren"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      :span-method="spanMethod"
      @row-click="goToRecordDetail"
    >
      <el-table-column prop="name" label="名称" min-width="180">
        <template #default="{ row }">
          <span v-if="row.type === 'record'" class="record-name">
            {{ formatTime(row.startedAt) }}
          </span>
          <span v-else>{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="配置来源 / 状态" min-width="150">
        <template #default="{ row }">
          <template v-if="row.type === 'pipeline'">
            <template v-if="row.configType === 'config-profile'">
              <el-tag type="success" size="small">配置组合</el-tag>
              {{ configProfiles.find(p => p.id === row.configProfileId)?.name || row.configProfileId }}
            </template>
            <template v-else>
              <el-tag type="primary" size="small">订阅源</el-tag>
              {{ subscriptionMap[row.primarySubscriptionId!] || row.primarySubscriptionId }}
            </template>
          </template>
          <template v-else>
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="脚本" width="120">
        <template #default="{ row }">
          <template v-if="row.type === 'pipeline'">
            {{ row.scriptName || '-' }}
          </template>
          <template v-else>
            <span class="record-error">{{ row.errorMessage || '-' }}</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="目标实例" min-width="150">
        <template #default="{ row }">
          <template v-if="row.type === 'pipeline'">
            {{ instanceMap[row.targetInstanceId!] || row.targetInstanceId }}
          </template>
        </template>
      </el-table-column>
      <el-table-column label="定时" width="120">
        <template #default="{ row }">
          <template v-if="row.type === 'pipeline'">
            {{ row.cronExpression || '-' }}
          </template>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <template v-if="row.type === 'pipeline'">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '是' : '否' }}
            </el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <template v-if="row.type === 'pipeline'">
            <el-tag :type="statusType(row.lastRunStatus)" size="small">
              {{ statusLabel(row.lastRunStatus) }}
            </el-tag>
          </template>
          <template v-else>
            <span class="record-time">{{ formatTime(row.finishedAt) }}</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <template v-if="row.type === 'pipeline'">
            <el-button size="small" type="success" @click.stop="handleExecute(row)">
              <el-icon><CaretRight /></el-icon>
              构建
            </el-button>
            <el-button size="small" @click.stop="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 构建进度弹窗 -->
    <BuildProgressModal
      :visible="showProgressModal"
      :record-id="currentRecordId"
      :pipeline-type="currentPipelineType"
      @close="handleProgressClose"
    />

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="构建流程名称" />
        </el-form-item>
        <el-form-item label="配置类型" required>
          <el-radio-group v-model="form.configType">
            <el-radio value="subscription">订阅源模式</el-radio>
            <el-radio value="config-profile">配置组合模式</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.configType === 'subscription'">
          <el-form-item label="主订阅" required>
            <el-select v-model="form.primarySubscriptionId" placeholder="选择主订阅" style="width: 100%">
              <el-option
                v-for="sub in subscriptions"
                :key="sub.id"
                :label="sub.name"
                :value="sub.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="额外订阅">
            <el-select
              v-model="form.additionalSubscriptionIds"
              multiple
              placeholder="选择额外订阅（可选）"
              style="width: 100%"
            >
              <el-option
                v-for="sub in subscriptions.filter(s => s.id !== form.primarySubscriptionId)"
                :key="sub.id"
                :label="sub.name"
                :value="sub.id"
              />
            </el-select>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="配置组合" required>
            <el-select v-model="form.configProfileId" placeholder="选择配置组合" style="width: 100%">
              <el-option
                v-for="profile in configProfiles"
                :key="profile.id"
                :label="profile.name"
                :value="profile.id"
              />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="脚本">
          <el-select v-model="form.scriptName" placeholder="选择脚本（可选）" clearable style="width: 100%">
            <el-option
              v-for="name in scriptNames"
              :key="name"
              :label="name"
              :value="name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标实例" required>
          <el-select v-model="form.targetInstanceId" placeholder="选择 Mihomo 实例" style="width: 100%">
            <el-option
              v-for="inst in instances"
              :key="inst.id"
              :label="inst.name"
              :value="inst.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Cron 表达式">
          <el-input v-model="form.cronExpression" placeholder="如 0 2 * * *（可选）" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.record-name {
  font-size: 13px;
  color: #606266;
}

.record-time {
  font-size: 12px;
  color: #909399;
}

.record-error {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

:deep(.el-table__row--level-1) {
  background-color: #fafafa;
}

:deep(.el-table__row--level-1 td) {
  font-size: 13px;
}

:deep(.el-table__row--level-1) {
  cursor: pointer;
}
</style>
