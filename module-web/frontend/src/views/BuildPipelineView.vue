<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { buildPipelineApi, type BuildPipeline, type BuildRecord } from '@/api/build-pipeline'
import { subscriptionApi, type Subscription } from '@/api/subscription'
import { mihomoApi, type MihomoInstance } from '@/api/mihomo'
import { scriptApi } from '@/api/script'

const router = useRouter()
const pipelines = ref<BuildPipeline[]>([])
const subscriptions = ref<Subscription[]>([])
const instances = ref<MihomoInstance[]>([])
const scriptNames = ref<string[]>([])
const loading = ref(false)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新建构建流程')
const form = ref<Partial<BuildPipeline>>({})

// 展开行的构建记录缓存
const expandedRecords = ref<Record<string, BuildRecord[]>>({})
const expandedLoading = ref<Record<string, boolean>>({})

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
    const [pipeRes, subRes, instRes, scriptRes] = await Promise.all([
      buildPipelineApi.list(),
      subscriptionApi.list(),
      mihomoApi.list(),
      scriptApi.list(),
    ])
    pipelines.value = pipeRes.data
    subscriptions.value = subRes.data
    instances.value = instRes.data
    scriptNames.value = scriptRes.data
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  dialogTitle.value = '新建构建流程'
  form.value = {
    name: '',
    primarySubscriptionId: '',
    additionalSubscriptionIds: [],
    scriptName: '',
    targetInstanceId: '',
    cronExpression: '',
    enabled: true,
  }
  dialogVisible.value = true
}

const openEditDialog = (pipeline: BuildPipeline) => {
  dialogTitle.value = '编辑构建流程'
  form.value = { ...pipeline }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.name || !form.value.primarySubscriptionId || !form.value.targetInstanceId) {
    ElMessage.warning('请填写名称、主订阅和目标实例')
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
    await loadData()
  } catch {
    ElMessage.error('保存失败')
  }
}

const handleDelete = (pipeline: BuildPipeline) => {
  ElMessageBox.confirm(`确定删除构建流程「${pipeline.name}」？`, '确认删除', {
    type: 'warning',
  }).then(async () => {
    try {
      await buildPipelineApi.delete(pipeline.id)
      ElMessage.success('删除成功')
      await loadData()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleExecute = async (pipeline: BuildPipeline) => {
  try {
    const res = await buildPipelineApi.execute(pipeline.id)
    const record = res.data
    if (record.status === 'SUCCESS') {
      ElMessage.success('构建成功')
    } else {
      ElMessage.error(`构建失败: ${record.errorMessage || '未知错误'}`)
    }
    await loadData()
    // 刷新展开行的记录
    if (expandedRecords.value[pipeline.id]) {
      loadRecords(pipeline.id)
    }
  } catch {
    ElMessage.error('触发构建失败')
  }
}

const handleExpandChange = (row: BuildPipeline, expanded: boolean) => {
  if (expanded && !expandedRecords.value[row.id]) {
    loadRecords(row.id)
  }
}

const loadRecords = async (pipelineId: string) => {
  expandedLoading.value[pipelineId] = true
  try {
    const res = await buildPipelineApi.getRecords(pipelineId)
    expandedRecords.value[pipelineId] = res.data.slice(0, 10)
  } catch {
    ElMessage.error('加载构建记录失败')
  } finally {
    expandedLoading.value[pipelineId] = false
  }
}

const goToRecordDetail = (recordId: string) => {
  router.push(`/build-records/${recordId}`)
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

    <el-table :data="pipelines" v-loading="loading" border stripe row-key="id" @expand-change="handleExpandChange">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div style="padding: 12px 24px;">
            <el-table
              :data="expandedRecords[row.id] || []"
              v-loading="expandedLoading[row.id]"
              border
              size="small"
              @row-click="(record: BuildRecord) => goToRecordDetail(record.id)"
              style="cursor: pointer;"
            >
              <el-table-column label="开始时间" width="180">
                <template #default="{ row: record }">
                  {{ record.startedAt?.replace('T', ' ').substring(0, 19) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row: record }">
                  <el-tag :type="statusType(record.status)" size="small">
                    {{ statusLabel(record.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="错误信息" min-width="200">
                <template #default="{ row: record }">
                  {{ record.errorMessage || '-' }}
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!expandedLoading[row.id] && (!expandedRecords[row.id] || expandedRecords[row.id].length === 0)" description="暂无构建记录" :image-size="60" />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column label="主订阅" min-width="150">
        <template #default="{ row }">
          {{ subscriptionMap[row.primarySubscriptionId] || row.primarySubscriptionId }}
        </template>
      </el-table-column>
      <el-table-column label="脚本" width="120">
        <template #default="{ row }">
          {{ row.scriptName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="目标实例" min-width="150">
        <template #default="{ row }">
          {{ instanceMap[row.targetInstanceId] || row.targetInstanceId }}
        </template>
      </el-table-column>
      <el-table-column label="定时" width="120">
        <template #default="{ row }">
          {{ row.cronExpression || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.lastRunStatus)" size="small">
            {{ statusLabel(row.lastRunStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="handleExecute(row)">
            <el-icon><CaretRight /></el-icon>
            构建
          </el-button>
          <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="构建流程名称" />
        </el-form-item>
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
