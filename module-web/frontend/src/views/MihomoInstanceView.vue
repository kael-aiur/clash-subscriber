<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mihomoApi } from '@/api/mihomo'
import type { MihomoInstance } from '@/api/mihomo'
import type { ClashConfig } from '@/api/subscription'
import MaskableText from '@/components/MaskableText.vue'

const instances = ref<MihomoInstance[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加 Mihomo 实例')
const editingId = ref<string | null>(null)
const pushDialogVisible = ref(false)
const pushTargetId = ref<string | null>(null)
const pushConfigText = ref('')

const form = ref<Partial<MihomoInstance>>({
  name: '',
  apiUrl: '',
  apiSecret: '',
  enabled: true,
})

const statusType = (status: string) => {
  switch (status) {
    case 'HEALTHY': return 'success'
    case 'UNHEALTHY': return 'danger'
    default: return 'info'
  }
}

const statusLabel = (status: string) => {
  switch (status) {
    case 'HEALTHY': return '健康'
    case 'UNHEALTHY': return '异常'
    default: return '未知'
  }
}

const loadInstances = async () => {
  loading.value = true
  try {
    const res = await mihomoApi.list()
    instances.value = res.data
  } catch {
    ElMessage.error('加载实例列表失败')
  } finally {
    loading.value = false
  }
}

const openDialog = (inst?: MihomoInstance) => {
  if (inst) {
    dialogTitle.value = '编辑 Mihomo 实例'
    editingId.value = inst.id
    form.value = { ...inst }
  } else {
    dialogTitle.value = '添加 Mihomo 实例'
    editingId.value = null
    form.value = { name: '', apiUrl: '', apiSecret: '', enabled: true }
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.name || !form.value.apiUrl) {
    ElMessage.warning('请填写名称和 API 地址')
    return
  }

  try {
    if (editingId.value) {
      await mihomoApi.update(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await mihomoApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadInstances()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (inst: MihomoInstance) => {
  ElMessageBox.confirm(`确定删除实例「${inst.name}」？`, '确认删除', {
    type: 'warning',
  }).then(async () => {
    try {
      await mihomoApi.delete(inst.id)
      ElMessage.success('删除成功')
      await loadInstances()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleHealthCheck = async (inst: MihomoInstance) => {
  try {
    await mihomoApi.healthCheck(inst.id)
    ElMessage.success('健康检查完成')
    await loadInstances()
  } catch {
    ElMessage.error('健康检查失败')
  }
}

const handleHealthCheckAll = async () => {
  try {
    await mihomoApi.healthCheckAll()
    ElMessage.success('全部健康检查完成')
    await loadInstances()
  } catch {
    ElMessage.error('健康检查失败')
  }
}

const openPushDialog = (inst?: MihomoInstance) => {
  pushTargetId.value = inst?.id || null
  pushConfigText.value = ''
  pushDialogVisible.value = true
}

const handlePush = async () => {
  let config: ClashConfig
  try {
    config = JSON.parse(pushConfigText.value)
  } catch {
    ElMessage.error('JSON 格式不正确')
    return
  }

  try {
    if (pushTargetId.value) {
      await mihomoApi.pushConfig(pushTargetId.value, config)
    } else {
      await mihomoApi.pushConfigAll(config)
    }
    ElMessage.success('配置推送成功')
    pushDialogVisible.value = false
  } catch {
    ElMessage.error('配置推送失败')
  }
}

const formatDate = (date?: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(loadInstances)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>Mihomo 实例管理</h2>
      <div>
        <el-button @click="handleHealthCheckAll">
          <el-icon><CircleCheck /></el-icon>
          全部健康检查
        </el-button>
        <el-button @click="openPushDialog()">
          <el-icon><Upload /></el-icon>
          推送到全部
        </el-button>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>
          添加实例
        </el-button>
      </div>
    </div>

    <el-table :data="instances" v-loading="loading" border stripe>
      <el-table-column prop="name" label="名称" min-width="150">
        <template #default="{ row }">
          <router-link :to="`/mihomo-instances/${row.id}`" class="instance-link">
            {{ row.name }}
          </router-link>
        </template>
      </el-table-column>
      <el-table-column label="API 地址" min-width="250">
        <template #default="{ row }">
          <MaskableText :text="row.apiUrl" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后检查时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.lastHealthCheck) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="handleHealthCheck(row)">
            <el-icon><CircleCheck /></el-icon>
            检查
          </el-button>
          <el-button size="small" type="warning" @click="openPushDialog(row)">
            <el-icon><Upload /></el-icon>
            推送
          </el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="输入实例名称" />
        </el-form-item>
        <el-form-item label="API 地址" required>
          <el-input v-model="form.apiUrl" placeholder="http://host:port" />
        </el-form-item>
        <el-form-item label="API 密钥">
          <el-input v-model="form.apiSecret" placeholder="可选" type="password" show-password />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 推送配置对话框 -->
    <el-dialog v-model="pushDialogVisible" title="推送配置" width="600px">
      <el-alert
        :title="pushTargetId ? '推送到指定实例' : '推送到所有实例'"
        type="info"
        :closable="false"
        style="margin-bottom: 16px;"
      />
      <el-input
        v-model="pushConfigText"
        type="textarea"
        :rows="12"
        placeholder="粘贴 ClashConfig JSON..."
      />
      <template #footer>
        <el-button @click="pushDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePush">推送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.instance-link {
  color: #409eff;
  text-decoration: none;
}
.instance-link:hover {
  text-decoration: underline;
}
</style>
