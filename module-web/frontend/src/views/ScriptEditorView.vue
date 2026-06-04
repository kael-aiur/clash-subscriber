<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scriptApi } from '@/api/script'
import ScriptTrialPanel from '@/components/ScriptTrialPanel.vue'
import ScriptCodePanel from '@/components/ScriptCodePanel.vue'

const route = useRoute()
const router = useRouter()

const scriptName = ref('')
const initialContent = ref('')
const isDirty = ref(false)
const panelWidth = ref(260)
const codePanelRef = ref<InstanceType<typeof ScriptCodePanel>>()

const defaultTemplate = `/**
 * 脚本入口函数
 * @param {Object} config - Clash 配置对象
 * @returns {Object} 处理后的配置
 */
function main(config) {
  // 在此编写你的脚本逻辑
  return config
}
`

onMounted(async () => {
  const name = route.params.name as string
  if (name && name !== '__new__') {
    try {
      const res = await scriptApi.get(name)
      scriptName.value = name
      initialContent.value = res.data
    } catch {
      ElMessage.error('脚本不存在')
      router.replace('/scripts')
    }
  } else {
    scriptName.value = ''
    initialContent.value = defaultTemplate
  }
})

// 可拖动分隔条
const startResize = (e: MouseEvent) => {
  e.preventDefault()
  const startX = e.clientX
  const startWidth = panelWidth.value

  const onMove = (e: MouseEvent) => {
    const newWidth = startWidth + (e.clientX - startX)
    panelWidth.value = Math.max(200, Math.min(500, newWidth))
  }

  const onUp = () => {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }

  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

// 保存
const handleSave = async (content: string) => {
  const name = scriptName.value
  if (!name) {
    ElMessage.warning('请先输入脚本名称')
    return
  }
  try {
    await scriptApi.save({ name, content })
    ElMessage.success('保存成功')
    isDirty.value = false
  } catch {
    ElMessage.error('保存失败')
  }
}

// 获取编辑器内容（供试运行使用）
const getContent = () => codePanelRef.value?.getContent() ?? initialContent.value

// 返回列表
const handleBack = () => {
  router.push('/scripts')
}

// 页面离开保护
onBeforeRouteLeave((_to, _from, next) => {
  if (isDirty.value) {
    ElMessageBox.confirm('脚本尚未保存，确定离开？', '提示', { type: 'warning' })
      .then(() => next())
      .catch(() => next(false))
  } else {
    next()
  }
})
</script>

<template>
  <div class="editor-layout">
    <!-- 左侧面板 -->
    <div class="left-panel" :style="{ width: panelWidth + 'px' }">
      <ScriptTrialPanel
        v-model:script-name="scriptName"
        :get-content="getContent"
      />
    </div>

    <!-- 分隔条 -->
    <div class="divider" @mousedown="startResize"></div>

    <!-- 右侧面板 -->
    <div class="right-panel">
      <div class="panel-header">
        <el-button size="small" @click="handleBack">
          ← 返回列表
        </el-button>
      </div>
      <ScriptCodePanel
        ref="codePanelRef"
        :script-name="scriptName"
        :initial-content="initialContent"
        @save="handleSave"
        @dirty="isDirty = $event"
      />
    </div>
  </div>
</template>

<style scoped>
.editor-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.left-panel {
  flex-shrink: 0;
  border-right: 1px solid var(--el-border-color-lighter);
  overflow: hidden;
}

.divider {
  width: 4px;
  cursor: col-resize;
  background: var(--el-border-color-lighter);
  flex-shrink: 0;
  transition: background 0.2s;
}

.divider:hover {
  background: var(--el-color-primary);
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}
</style>
