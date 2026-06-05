<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { buildRecordApi, type BuildRecord, type BuildStep } from '@/api/build-pipeline'
import ConfigCard from '@/components/ConfigCard.vue'

const route = useRoute()
const router = useRouter()
const record = ref<BuildRecord | null>(null)
const loading = ref(false)
const activeStep = ref<number>(-1)

const recordId = computed(() => route.params.id as string)

const loadRecord = async () => {
  loading.value = true
  try {
    const res = await buildRecordApi.get(recordId.value)
    record.value = res.data
  } catch {
    ElMessage.error('加载构建记录失败')
  } finally {
    loading.value = false
  }
}

const stepStatus = (step: BuildStep) => {
  if (step.status === 'SUCCESS') return 'success'
  if (step.status === 'FAILED') return 'error'
  if (step.status === 'SKIPPED') return 'wait'
  return 'wait'
}

const stepIcon = (step: BuildStep) => {
  if (step.status === 'SUCCESS') return 'Check'
  if (step.status === 'FAILED') return 'Close'
  if (step.status === 'SKIPPED') return 'DArrowRight'
  return 'Loading'
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

const formatJson = (data: any) => {
  if (data === null || data === undefined) return '-'
  if (typeof data === 'string') return data
  return JSON.stringify(data, null, 2)
}

const isConfigData = (data: any): boolean => {
  return data !== null && typeof data === 'object' && !Array.isArray(data) && 'configSummary' in data
}

const isStep1Input = (data: any): boolean => {
  return data !== null && typeof data === 'object' && 'subscriptionName' in data
}

const isStep4Output = (data: any): boolean => {
  return data !== null && typeof data === 'object' && 'success' in data
}

const selectStep = (index: number) => {
  activeStep.value = activeStep.value === index ? -1 : index
}

const goBack = () => {
  router.push('/build-pipelines')
}

onMounted(loadRecord)
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <el-button @click="goBack" style="margin-right: 16px;">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h2>构建记录详情</h2>
    </div>

    <template v-if="record">
      <!-- 基本信息 -->
      <el-card style="margin-bottom: 20px;">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="记录 ID">
            <code>{{ record.id }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ formatTime(record.startedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="结束时间">
            {{ formatTime(record.finishedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(record.status)">
              {{ statusLabel(record.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="耗时" :span="2">
            <template v-if="record.startedAt && record.finishedAt">
              {{ ((new Date(record.finishedAt).getTime() - new Date(record.startedAt).getTime()) / 1000).toFixed(1) }}s
            </template>
            <template v-else>-</template>
          </el-descriptions-item>
          <el-descriptions-item v-if="record.errorMessage" label="错误信息" :span="3">
            <el-text type="danger">{{ record.errorMessage }}</el-text>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 流程图 -->
      <el-card>
        <template #header>
          <span>构建流程</span>
        </template>

        <div v-if="record.steps && record.steps.length > 0" class="steps-container">
          <el-steps :active="activeStep" direction="vertical" :space="100">
            <el-step
              v-for="(step, index) in record.steps"
              :key="index"
              :title="step.name"
              :status="stepStatus(step)"
              :description="step.status === 'SKIPPED' ? '已跳过' : ''"
              @click="selectStep(index)"
              style="cursor: pointer;"
            >
              <template #icon>
                <el-icon><component :is="stepIcon(step)" /></el-icon>
              </template>
            </el-step>
          </el-steps>

          <!-- 环节详情面板 -->
          <div v-if="activeStep >= 0 && record.steps[activeStep]" class="step-detail">
            <el-card shadow="never">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>{{ record.steps[activeStep].name }}</span>
                  <el-tag :type="statusType(record.steps[activeStep].status)" size="small">
                    {{ statusLabel(record.steps[activeStep].status) }}
                  </el-tag>
                </div>
              </template>

              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="开始时间">
                  {{ formatTime(record.steps[activeStep].startedAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="结束时间">
                  {{ formatTime(record.steps[activeStep].finishedAt) }}
                </el-descriptions-item>
                <el-descriptions-item v-if="record.steps[activeStep].errorMessage" label="错误信息">
                  <el-text type="danger">{{ record.steps[activeStep].errorMessage }}</el-text>
                </el-descriptions-item>
              </el-descriptions>

              <div style="margin-top: 16px;">
                <h4 style="margin-bottom: 8px;">输入</h4>
                <template v-if="isStep1Input(record.steps[activeStep].input)">
                  <el-tag>订阅源: {{ record.steps[activeStep].input.subscriptionName }}</el-tag>
                </template>
                <template v-else-if="isConfigData(record.steps[activeStep].input)">
                  <template v-if="record.steps[activeStep].input.subscriptionName || record.steps[activeStep].input.scriptName || record.steps[activeStep].input.instanceName">
                    <el-tag v-if="record.steps[activeStep].input.subscriptionName" style="margin-bottom: 8px;">订阅源: {{ record.steps[activeStep].input.subscriptionName }}</el-tag>
                    <el-tag v-if="record.steps[activeStep].input.scriptName" style="margin-bottom: 8px;">脚本: {{ record.steps[activeStep].input.scriptName }}</el-tag>
                    <el-tag v-if="record.steps[activeStep].input.instanceName" style="margin-bottom: 8px;">实例: {{ record.steps[activeStep].input.instanceName }}</el-tag>
                  </template>
                  <ConfigCard
                    v-if="record.steps[activeStep].input.configSummary"
                    :summary="record.steps[activeStep].input.configSummary"
                    :yaml-content="record.steps[activeStep].input.configYaml"
                    style="margin-top: 8px;"
                  />
                </template>
                <template v-else>
                  <el-input
                    type="textarea"
                    :rows="4"
                    :model-value="formatJson(record.steps[activeStep].input)"
                    readonly
                    style="font-family: monospace;"
                  />
                </template>
              </div>

              <div style="margin-top: 16px;">
                <h4 style="margin-bottom: 8px;">输出</h4>
                <template v-if="isStep4Output(record.steps[activeStep].output)">
                  <el-tag :type="record.steps[activeStep].output.success ? 'success' : 'danger'">
                    {{ record.steps[activeStep].output.success ? '推送成功' : '推送失败' }}
                  </el-tag>
                </template>
                <template v-else-if="isConfigData(record.steps[activeStep].output)">
                  <ConfigCard
                    :summary="record.steps[activeStep].output.configSummary"
                    :yaml-content="record.steps[activeStep].output.configYaml"
                  />
                </template>
                <template v-else>
                  <el-input
                    type="textarea"
                    :rows="4"
                    :model-value="formatJson(record.steps[activeStep].output)"
                    readonly
                    style="font-family: monospace;"
                  />
                </template>
              </div>
            </el-card>
          </div>
        </div>

        <el-empty v-else description="无环节数据（历史记录可能不包含详细步骤信息）" />
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.steps-container {
  display: flex;
  gap: 24px;
}

.steps-container .el-steps {
  min-width: 200px;
  flex-shrink: 0;
}

.step-detail {
  flex: 1;
  min-width: 0;
}
</style>
