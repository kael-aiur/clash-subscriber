<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { scriptApi } from '@/api/script'
import type { ConfigSummary } from '@/api/script'
import { subscriptionApi } from '@/api/subscription'
import type { Subscription } from '@/api/subscription'
import ConfigCard from '@/components/ConfigCard.vue'

interface TrialState {
  step: number  // 0=未开始, 1=获取中, 2=执行中, 3=完成
  status: 'wait' | 'process' | 'finish' | 'error' | 'success'
  inputSummary?: ConfigSummary
  inputYaml?: string
  outputSummary?: ConfigSummary
  outputYaml?: string
  changeSummary?: {
    proxiesBefore: number; proxiesAfter: number
    groupsBefore: number; groupsAfter: number
    rulesBefore: number; rulesAfter: number
  }
  error?: string
}

const props = defineProps<{
  scriptName: string
  getContent: () => string
}>()

const emit = defineEmits<{
  'update:scriptName': [name: string]
}>()

const subscriptions = ref<Subscription[]>([])
const selectedSubId = ref('')
const trialState = ref<TrialState>({ step: 0, status: 'wait' })
const tryRunLoading = ref(false)

const loadSubscriptions = async () => {
  try {
    const res = await subscriptionApi.list()
    subscriptions.value = res.data
  } catch {
    ElMessage.error('加载订阅源列表失败')
  }
}

loadSubscriptions()

const steps = [
  { title: '获取订阅配置', description: '' },
  { title: '执行脚本', description: '' },
  { title: '执行结果', description: '' },
]

const activeStep = computed(() => {
  if (trialState.value.step === 0) return 0
  return trialState.value.step - 1
})

const stepStatus = (index: number) => {
  const s = trialState.value
  if (s.step === 0) return 'wait'
  if (index + 1 < s.step) return 'finish'
  if (index + 1 === s.step) return s.status
  return 'wait'
}

const handleTryRun = async () => {
  const content = props.getContent()
  if (!content) {
    ElMessage.warning('脚本内容不能为空')
    return
  }
  if (!selectedSubId.value) {
    ElMessage.warning('请选择订阅源')
    return
  }

  tryRunLoading.value = true
  trialState.value = { step: 1, status: 'process' }

  try {
    // 步骤 1：获取订阅配置
    const previewRes = await scriptApi.previewSubscription(selectedSubId.value)
    trialState.value = {
      ...trialState.value,
      step: 1,
      status: 'finish',
      inputSummary: previewRes.data.summary,
      inputYaml: previewRes.data.yaml,
    }

    // 步骤 2：执行脚本
    trialState.value = { ...trialState.value, step: 2, status: 'process' }
    const runRes = await scriptApi.tryRun(content, selectedSubId.value)

    if (runRes.data.success) {
      trialState.value = {
        step: 3,
        status: 'success',
        inputSummary: previewRes.data.summary,
        inputYaml: previewRes.data.yaml,
        outputSummary: runRes.data.outputSummary,
        outputYaml: runRes.data.outputYaml,
        changeSummary: runRes.data.summary,
      }
    } else {
      trialState.value = {
        ...trialState.value,
        step: 2,
        status: 'error',
        error: runRes.data.error,
      }
    }
  } catch (e: any) {
    const currentStep = trialState.value.step
    trialState.value = {
      ...trialState.value,
      step: currentStep,
      status: 'error',
      error: e?.message || '请求失败',
    }
  } finally {
    tryRunLoading.value = false
  }
}

const resetTrial = () => {
  trialState.value = { step: 0, status: 'wait' }
}
</script>

<template>
  <div class="trial-panel">
    <!-- 脚本名称 -->
    <div class="panel-section">
      <div class="section-label">脚本名称</div>
      <el-input
        :model-value="scriptName"
        @update:model-value="emit('update:scriptName', $event)"
        placeholder="输入脚本名称"
        size="small"
      />
    </div>

    <!-- 试运行控制 -->
    <div class="panel-section">
      <div class="section-label">试运行</div>
      <el-select
        v-model="selectedSubId"
        placeholder="选择订阅源"
        size="small"
        style="width: 100%; margin-bottom: 8px;"
      >
        <el-option
          v-for="sub in subscriptions"
          :key="sub.id"
          :label="sub.name"
          :value="sub.id"
        />
      </el-select>
      <div style="display: flex; gap: 8px;">
        <el-button
          type="success"
          size="small"
          :loading="tryRunLoading"
          :disabled="!selectedSubId"
          style="flex: 1;"
          @click="handleTryRun"
        >
          ▶ 试运行
        </el-button>
        <el-button
          v-if="trialState.step > 0"
          size="small"
          @click="resetTrial"
        >
          重置
        </el-button>
      </div>
    </div>

    <!-- 步骤条 -->
    <div class="panel-section" v-if="trialState.step > 0">
      <el-steps direction="vertical" :active="activeStep" :space="40">
        <el-step
          v-for="(step, index) in steps"
          :key="index"
          :title="step.title"
          :status="stepStatus(index)"
        />
      </el-steps>
    </div>

    <!-- 错误信息 -->
    <div v-if="trialState.status === 'error' && trialState.error" class="panel-section">
      <el-alert type="error" :closable="false" show-icon>
        <template #title>{{ trialState.error }}</template>
      </el-alert>
    </div>

    <!-- 输入卡片 -->
    <div v-if="trialState.inputSummary" class="panel-section">
      <div class="section-label">📥 输入（订阅源配置）</div>
      <ConfigCard
        :summary="trialState.inputSummary"
        :yaml-content="trialState.inputYaml"
      />
    </div>

    <!-- 变更摘要 -->
    <div v-if="trialState.changeSummary" class="panel-section">
      <div class="section-label">📊 变更摘要</div>
      <div class="change-tags">
        <el-tag type="info" size="small">
          节点: {{ trialState.changeSummary.proxiesBefore }} → {{ trialState.changeSummary.proxiesAfter }}
        </el-tag>
        <el-tag type="info" size="small">
          代理组: {{ trialState.changeSummary.groupsBefore }} → {{ trialState.changeSummary.groupsAfter }}
        </el-tag>
        <el-tag type="info" size="small">
          规则: {{ trialState.changeSummary.rulesBefore }} → {{ trialState.changeSummary.rulesAfter }}
        </el-tag>
      </div>
    </div>

    <!-- 输出卡片 -->
    <div v-if="trialState.outputSummary" class="panel-section">
      <div class="section-label">📤 输出（脚本处理后）</div>
      <ConfigCard
        :summary="trialState.outputSummary"
        :yaml-content="trialState.outputYaml"
      />
    </div>
  </div>
</template>

<style scoped>
.trial-panel {
  padding: 12px;
  overflow-y: auto;
  height: 100%;
}

.panel-section {
  margin-bottom: 16px;
}

.section-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
  font-weight: 500;
}

.change-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
