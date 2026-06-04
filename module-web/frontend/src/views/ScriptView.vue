<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scriptApi } from '@/api/script'
import * as monaco from 'monaco-editor'
import { useRouter } from 'vue-router'

const router = useRouter()

// 启用 JS 语法错误诊断（类型定义不完整，使用 as any 绕过）
const tsDefaults = (monaco.languages.typescript as any).javascriptDefaults
if (tsDefaults) {
  tsDefaults.setDiagnosticsOptions({
    noSemanticValidation: true,
    noSyntaxValidation: false,
  })
}

const scriptNames = ref<string[]>([])
const loading = ref(false)
const viewDialogVisible = ref(false)
const viewName = ref('')

const form = ref({
  name: '',
  content: '',
})

// 查看内容编辑器
const viewEditorContainer = ref<HTMLElement>()
let viewEditor: monaco.editor.IStandaloneCodeEditor | null = null

const createEditor = (container: HTMLElement, content: string, readOnly = false) => {
  return monaco.editor.create(container, {
    value: content,
    language: 'javascript',
    theme: 'vs',
    readOnly,
    minimap: { enabled: false },
    lineNumbers: 'on',
    bracketPairColorization: { enabled: true },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    fontSize: 14,
    tabSize: 2,
  })
}

watch(viewDialogVisible, async (visible) => {
  if (visible) {
    await nextTick()
    requestAnimationFrame(() => {
      if (viewEditorContainer.value) {
        viewEditor = createEditor(viewEditorContainer.value, form.value.content, true)
        requestAnimationFrame(() => viewEditor?.layout())
      }
    })
  } else {
    viewEditor?.dispose()
    viewEditor = null
  }
})

const loadScripts = async () => {
  loading.value = true
  try {
    const res = await scriptApi.list()
    scriptNames.value = res.data
  } catch {
    ElMessage.error('加载脚本列表失败')
  } finally {
    loading.value = false
  }
}

const handleView = async (name: string) => {
  try {
    const res = await scriptApi.get(name)
    form.value = { name, content: res.data }
    viewName.value = name
    viewDialogVisible.value = true
  } catch {
    ElMessage.error('获取脚本内容失败')
  }
}

const handleDelete = (name: string) => {
  ElMessageBox.confirm(`确定删除脚本「${name}」？`, '确认删除', {
    type: 'warning',
  }).then(async () => {
    try {
      await scriptApi.delete(name)
      ElMessage.success('删除成功')
      await loadScripts()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  loadScripts()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h2>脚本管理</h2>
      <el-button type="primary" @click="router.push('/scripts/edit/__new__')">
        <el-icon><Plus /></el-icon>
        添加脚本
      </el-button>
    </div>

    <el-table :data="scriptNames.map(name => ({ name }))" v-loading="loading" border stripe>
      <el-table-column prop="name" label="脚本名称" min-width="300" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="info" @click="handleView(row.name)">
            <el-icon><View /></el-icon>
            查看
          </el-button>
          <el-button size="small" @click="router.push(`/scripts/edit/${row.name}`)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.name)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 查看内容对话框 -->
    <el-dialog v-model="viewDialogVisible" :title="`查看脚本: ${viewName}`" width="800px" destroy-on-close>
      <div ref="viewEditorContainer" style="width: 100%; height: 500px; border: 1px solid #dcdfe6; border-radius: 4px;"></div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
