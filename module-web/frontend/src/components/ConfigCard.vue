<script setup lang="ts">
import { ref } from 'vue'

interface ConfigSummary {
  nodeCount: number
  proxyGroupCount: number
  ruleCount: number
  nodeNames?: string[]
  proxyGroupNames?: string[]
}

const props = withDefaults(defineProps<{
  summary: ConfigSummary
  yamlContent?: string
  expandable?: boolean
}>(), {
  expandable: true
})

const expanded = ref(false)
</script>

<template>
  <div class="config-card">
    <div class="config-summary">
      <el-space :size="16">
        <el-tag type="info">节点: {{ summary.nodeCount }}</el-tag>
        <el-tag type="info">代理组: {{ summary.proxyGroupCount }}</el-tag>
        <el-tag type="info">规则: {{ summary.ruleCount }}</el-tag>
      </el-space>
    </div>

    <div v-if="summary.nodeNames && summary.nodeNames.length > 0" class="config-preview">
      <span class="preview-label">节点:</span>
      <span class="preview-text">
        {{ summary.nodeNames.join(', ') }}
        <template v-if="summary.nodeCount > summary.nodeNames.length">... (共{{ summary.nodeCount }}个)</template>
      </span>
    </div>

    <div v-if="summary.proxyGroupNames && summary.proxyGroupNames.length > 0" class="config-preview">
      <span class="preview-label">代理组:</span>
      <span class="preview-text">
        {{ summary.proxyGroupNames.join(', ') }}
        <template v-if="summary.proxyGroupCount > summary.proxyGroupNames.length">... (共{{ summary.proxyGroupCount }}个)</template>
      </span>
    </div>

    <div v-if="expandable && yamlContent" class="config-expand">
      <el-button text type="primary" @click="expanded = !expanded" size="small">
        {{ expanded ? '收起 ▲' : '展开 ▼' }}
      </el-button>
    </div>

    <el-collapse-transition>
      <div v-if="expanded && yamlContent" class="config-yaml">
        <el-input
          type="textarea"
          :model-value="yamlContent"
          readonly
          :autosize="{ minRows: 4, maxRows: 30 }"
          style="font-family: monospace;"
        />
      </div>
    </el-collapse-transition>
  </div>
</template>

<style scoped>
.config-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 12px;
  background: var(--el-fill-color-blank);
}

.config-summary {
  margin-bottom: 8px;
}

.config-preview {
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-top: 4px;
  line-height: 1.6;
}

.preview-label {
  color: var(--el-text-color-secondary);
  margin-right: 4px;
}

.preview-text {
  word-break: break-all;
}

.config-expand {
  margin-top: 8px;
  text-align: right;
}

.config-yaml {
  margin-top: 8px;
}
</style>
