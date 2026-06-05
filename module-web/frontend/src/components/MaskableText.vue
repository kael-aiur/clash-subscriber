<script setup lang="ts">
import { ref, computed } from 'vue'
import { View, Hide } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  text: string
  masked?: boolean
  fullyMasked?: boolean
}>(), {
  masked: true,
  fullyMasked: false
})

const isMasked = ref(props.masked)

const displayText = computed(() => {
  if (!isMasked.value) return props.text

  // 完全隐藏模式
  if (props.fullyMasked) {
    return '••••••••'
  }

  // 部分隐藏模式（显示域名）
  try {
    const url = new URL(props.text)
    // 显示协议、主机名和端口，隐藏路径和参数
    const host = url.port ? `${url.hostname}:${url.port}` : url.hostname
    return `${url.protocol}//${host}/...`
  } catch {
    // 非 URL 格式，显示前 10 个字符 + ...
    if (props.text.length > 10) {
      return props.text.substring(0, 10) + '...'
    }
    return props.text
  }
})

const toggle = () => {
  isMasked.value = !isMasked.value
}
</script>

<template>
  <div class="maskable-text">
    <span class="text" :title="isMasked ? '点击眼睛图标查看完整内容' : text">{{ displayText }}</span>
    <el-icon class="toggle-btn" role="button" tabindex="0" aria-label="切换显示" @click="toggle" @keydown.enter="toggle" @keydown.space.prevent="toggle">
      <Hide v-if="isMasked" />
      <View v-else />
    </el-icon>
  </div>
</template>

<style scoped>
.maskable-text {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
}

.text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.toggle-btn {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.toggle-btn:hover {
  color: var(--el-color-primary);
}
</style>
