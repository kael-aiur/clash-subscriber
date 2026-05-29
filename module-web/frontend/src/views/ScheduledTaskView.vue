<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scheduledTaskApi, pipelineApi } from '@/api/scheduled-task'
import { mihomoApi } from '@/api/mihomo'
import type { ScheduledTask, PipelineConfig } from '@/api/scheduled-task'
import type { MihomoInstance } from '@/api/mihomo'

const tasks = ref<ScheduledTask[]>([])
const pipelines = ref<PipelineConfig[]>([])
const instances = ref<MihomoInstance[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加定时任务')
const editingId = ref<string | null>(null)

const form = ref<Partial<ScheduledTask>>({
  name: '',
  pipelineId: '',
  targetInstances: [],
  cronExpression: '',
  enabled: true,
})

const statusType = (status?: string) => {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'danger'
    case 'RUNNING': return 'warning'
    default: return 'info'
  }
}

const statusLabel = (status?: string) => {
  switch (status) {
    case 'SUCCESS': return '成功'
    case 'FAILED': return '失败'
    case 'RUNNING': return '运行中'
    default: return '-'
  }
}

const getPipelineName = (id: string) => {
  return pipelines.value.find(p => p.id === id)?.name || id
}

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await scheduledTaskApi.list()
    tasks.value = res.data
  } catch {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

const loadPipelines = async () => {
  try {
    const res = await pipelineApi.list()
    pipelines.value = res.data
  } catch {
    // 静默失败
  }
}

const loadInstances = async () => {
  try {
    const res = await mihomoApi.list()
    instances.value = res.data
  } catch {
    // 静默失败
  }
}

const openDialog = (task?: ScheduledTask) => {
  if (task) {
    dialogTitle.value = '编辑定时任务'
    editingId.value = task.id
    form.value = { ...task }
  } else {
    dialogTitle.value = '添加定时任务'
    editingId.value = null
    form.value = {
      name: '',
      pipelineId: '',
      targetInstances: [],
      cronExpression: '',
      enabled: true,
    }
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.name || !form.value.pipelineId || !form.value.cronExpression) {
    ElMessage.warning('请填写名称、Pipeline 和 Cron 表达式')
    return
  }

  try {
    if (editingId.value) {
      await scheduledTaskApi.update(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await scheduledTaskApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadTasks()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (task: ScheduledTask) => {
  ElMessageBox.confirm(`确定删除任务「${task.name}」？`, '确认删除', {
    type: 'warning',
  }).then(async () => {
    try {
      await scheduledTaskApi.delete(task.id)
      ElMessage.success('删除成功')
      await loadTasks()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleToggle = async (task: ScheduledTask) => {
  try {
    if (task.enabled) {
      await scheduledTaskApi.disable(task.id)
      ElMessage.success('已禁用')
    } else {
      await scheduledTaskApi.enable(task.id)
      ElMessage.success('已启用')
    }
    await loadTasks()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleTrigger = async (task: ScheduledTask) => {
  try {
    await scheduledTaskApi.trigger(task.id)
    ElMessage.success('手动触发成功')
    await loadTasks()
  } catch {
    ElMessage.error('触发失败')
  }
}

const formatDate = (date?: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadTasks()
  loadPipelines()
  loadInstances()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h2>定时任务管理</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        添加任务
      </el-button>
    </div>

    <el-table :data="tasks" v-loading="loading" border stripe>
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column label="Pipeline" min-width="150">
        <template #default="{ row }">
          {{ getPipelineName(row.pipelineId) }}
        </template>
      </el-table-column>
      <el-table-column prop="cronExpression" label="Cron 表达式" width="150" />
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="handleToggle(row)" />
        </template>
      </el-table-column>
      <el-table-column label="最后运行状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.lastRunStatus)">{{ statusLabel(row.lastRunStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后运行时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.lastRunAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="handleTrigger(row)">
            <el-icon><CaretRight /></el-icon>
            触发
          </el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form label-width="120px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="输入任务名称" />
        </el-form-item>
        <el-form-item label="Pipeline" required>
          <el-select v-model="form.pipelineId" placeholder="选择 Pipeline" style="width: 100%">
            <el-option
              v-for="p in pipelines"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标实例">
          <el-select
            v-model="form.targetInstances"
            multiple
            placeholder="选择目标 Mihomo 实例（可选）"
            style="width: 100%"
          >
            <el-option
              v-for="inst in instances"
              :key="inst.id"
              :label="inst.name"
              :value="inst.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Cron 表达式" required>
          <el-input v-model="form.cronExpression" placeholder="如: 0 0 */6 * * *" />
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
  </div>
</template>
