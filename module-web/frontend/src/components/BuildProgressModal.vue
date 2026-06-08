<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { buildPipelineApi } from '@/api/build-pipeline'

interface StepStatusEvent {
  type: 'step-status'
  stepIndex: number
  stepName: string
  status: 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'
  timestamp: number
}

interface BuildCompleteEvent {
  type: 'build-complete'
  status: 'SUCCESS' | 'FAILED'
  duration: number
  totalSteps: number
  successSteps: number
  failedSteps: number
  errorMessage?: string
}

interface BuildErrorEvent {
  type: 'build-error'
  message: string
}

interface Props {
  visible: boolean
  recordId: string
  pipelineType: 'subscription' | 'config-profile'
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
}>()

const steps = ref<Array<{ name: string; status: string; errorMessage?: string }>>([])
const activeStep = ref(0)
const isCompleted = ref(false)
const result = ref<BuildCompleteEvent | null>(null)
const eventSource = ref<EventSource | null>(null)
const reconnectCount = ref(0)
const MAX_RECONNECT = 3

// 初始化步骤
const initSteps = () => {
  if (props.pipelineType === 'subscription') {
    steps.value = [
      { name: '拉取主订阅配置', status: 'WAITING' },
      { name: '合并额外订阅节点', status: 'WAITING' },
      { name: '脚本处理', status: 'WAITING' },
      { name: '推送到 Mihomo', status: 'WAITING' }
    ]
  } else {
    steps.value = [
      { name: '获取配置组合', status: 'WAITING' },
      { name: '生成完整配置', status: 'WAITING' },
      { name: '脚本处理', status: 'WAITING' },
      { name: '推送到 Mihomo', status: 'WAITING' }
    ]
  }
}

// 获取步骤状态映射
const getStepStatus = (status: string) => {
  switch (status) {
    case 'WAITING': return 'wait'
    case 'RUNNING': return 'process'
    case 'SUCCESS': return 'finish'
    case 'FAILED': return 'error'
    case 'SKIPPED': return 'success'
    default: return 'wait'
  }
}

// 结果图标
const resultIcon = computed(() => {
  return result.value?.status === 'SUCCESS' ? 'success' : 'error'
})

// 结果标题
const resultTitle = computed(() => {
  return result.value?.status === 'SUCCESS' ? '构建成功' : '构建失败'
})

// 结果副标题
const resultSubTitle = computed(() => {
  if (!result.value) return ''
  if (result.value.status === 'FAILED' && result.value.errorMessage) {
    return result.value.errorMessage
  }
  return `共 ${result.value.totalSteps} 个步骤，${result.value.successSteps} 个成功，${result.value.failedSteps} 个失败`
})

// 格式化时长
const formatDuration = (ms: number) => {
  if (ms < 1000) return `${ms} 毫秒`
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) return `${seconds} 秒`
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes} 分 ${remainingSeconds} 秒`
}

// 订阅 SSE
const subscribe = () => {
  if (!props.recordId) return

  eventSource.value = buildPipelineApi.subscribeProgress(props.recordId)

  eventSource.value.addEventListener('step-status', (e) => {
    const data = JSON.parse((e as MessageEvent).data) as StepStatusEvent
    if (data.stepIndex < steps.value.length) {
      steps.value[data.stepIndex].status = data.status
      activeStep.value = data.stepIndex
    }
  })

  eventSource.value.addEventListener('build-complete', (e) => {
    const data = JSON.parse((e as MessageEvent).data) as BuildCompleteEvent
    result.value = data
    isCompleted.value = true
    eventSource.value?.close()
    eventSource.value = null
  })

  eventSource.value.addEventListener('build-error', (e) => {
    const data = JSON.parse((e as MessageEvent).data) as BuildErrorEvent
    ElMessage.error(data.message)
    eventSource.value?.close()
    eventSource.value = null
  })

  eventSource.value.onerror = () => {
    if (reconnectCount.value < MAX_RECONNECT) {
      reconnectCount.value++
      console.warn(`SSE 连接中断，第 ${reconnectCount.value} 次重连...`)
      eventSource.value?.close()
      setTimeout(subscribe, 1000 * reconnectCount.value)
    } else {
      ElMessage.error('连接中断，请刷新页面查看构建结果')
      eventSource.value?.close()
      eventSource.value = null
    }
  }
}

// 取消（关闭弹窗，构建继续后台运行）
const handleCancel = () => {
  eventSource.value?.close()
  eventSource.value = null
  emit('close')
}

// 确定
const handleConfirm = () => {
  emit('close')
}

// 监听 visible 变化
watch(() => props.visible, (newVal) => {
  if (newVal && props.recordId) {
    initSteps()
    isCompleted.value = false
    result.value = null
    activeStep.value = 0
    reconnectCount.value = 0
    subscribe()
  }
})

// 组件挂载时如果已显示则订阅
onMounted(() => {
  if (props.visible && props.recordId) {
    initSteps()
    subscribe()
  }
})

// 组件卸载时关闭连接
onUnmounted(() => {
  if (eventSource.value) {
    eventSource.value.close()
    eventSource.value = null
  }
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="构建进度"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    width="500px"
    class="build-progress-modal"
  >
    <!-- 步骤条 -->
    <div class="steps-container">
      <el-steps direction="vertical" :active="activeStep" finish-status="success">
        <el-step
          v-for="(step, index) in steps"
          :key="index"
          :title="step.name"
          :status="getStepStatus(step.status)"
        >
          <template #description>
            <span v-if="step.status === 'RUNNING'" class="running-text">
              执行中...
            </span>
            <span v-else-if="step.status === 'FAILED'" class="failed-text">
              {{ step.errorMessage || '执行失败' }}
            </span>
          </template>
        </el-step>
      </el-steps>
    </div>

    <!-- 构建结果摘要 -->
    <div v-if="isCompleted" class="result-summary">
      <el-result
        :icon="resultIcon"
        :title="resultTitle"
        :sub-title="resultSubTitle"
      >
        <template #extra>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="总耗时">{{ formatDuration(result?.duration || 0) }}</el-descriptions-item>
            <el-descriptions-item label="总步骤">{{ result?.totalSteps || 0 }}</el-descriptions-item>
            <el-descriptions-item label="成功">{{ result?.successSteps || 0 }}</el-descriptions-item>
            <el-descriptions-item label="失败">{{ result?.failedSteps || 0 }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-result>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <el-button v-if="!isCompleted" @click="handleCancel">取消</el-button>
      <el-button v-if="isCompleted" type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.steps-container {
  padding: 20px 0;
}

.running-text {
  color: #409eff;
  font-size: 12px;
}

.failed-text {
  color: #f56c6c;
  font-size: 12px;
}

.result-summary {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}
</style>
