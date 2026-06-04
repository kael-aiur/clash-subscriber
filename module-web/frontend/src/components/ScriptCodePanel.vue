<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as monaco from 'monaco-editor'

const props = defineProps<{
  scriptName: string
  initialContent: string
}>()

const emit = defineEmits<{
  save: [content: string]
  dirty: [isDirty: boolean]
}>()

const editorContainer = ref<HTMLElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null
const isDirty = ref(false)

const createEditor = (container: HTMLElement, content: string) => {
  return monaco.editor.create(container, {
    value: content,
    language: 'javascript',
    theme: 'vs',
    minimap: { enabled: false },
    lineNumbers: 'on',
    bracketPairColorization: { enabled: true },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    fontSize: 14,
    tabSize: 2,
  })
}

onMounted(async () => {
  await nextTick()
  if (editorContainer.value) {
    editor = createEditor(editorContainer.value, props.initialContent)
    editor.onDidChangeModelContent(() => {
      if (!isDirty.value) {
        isDirty.value = true
        emit('dirty', true)
      }
    })
  }
})

onBeforeUnmount(() => {
  editor?.dispose()
  editor = null
})

const getContent = () => editor?.getValue() ?? props.initialContent

const handleFormat = () => {
  editor?.getAction('editor.action.formatDocument')?.run()
}

const handleSave = () => {
  const content = getContent()
  emit('save', content)
  isDirty.value = false
  emit('dirty', false)
}

defineExpose({ getContent })
</script>

<template>
  <div class="code-panel">
    <div class="code-toolbar">
      <div class="toolbar-left">
        <span class="script-name">{{ scriptName || '新建脚本' }}</span>
        <span v-if="isDirty" class="dirty-indicator">未保存</span>
        <span v-else class="saved-indicator">已保存</span>
      </div>
      <div class="toolbar-right">
        <el-button size="small" @click="handleFormat">格式化</el-button>
        <el-button size="small" type="primary" @click="handleSave">保存</el-button>
      </div>
    </div>
    <div ref="editorContainer" class="editor-container"></div>
  </div>
</template>

<style scoped>
.code-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.code-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.script-name {
  font-weight: 600;
  font-size: 14px;
}

.dirty-indicator {
  color: var(--el-color-warning);
  font-size: 12px;
}

.saved-indicator {
  color: var(--el-color-success);
  font-size: 12px;
}

.editor-container {
  flex: 1;
  overflow: hidden;
}
</style>
