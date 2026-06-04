<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scriptApi } from '@/api/script'
import type { TryRunResult } from '@/api/script'
import { subscriptionApi } from '@/api/subscription'
import type { Subscription } from '@/api/subscription'
import * as monaco from 'monaco-editor'

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
const dialogVisible = ref(false)
const dialogTitle = ref('添加脚本')
const viewDialogVisible = ref(false)
const viewName = ref('')

const form = ref({
  name: '',
  content: '',
})

// 试运行相关
const subscriptions = ref<Subscription[]>([])
const selectedSubId = ref('')
const tryRunLoading = ref(false)
const tryRunResult = ref<TryRunResult | null>(null)

// Monaco Editor
const editorContainer = ref<HTMLElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

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

watch(dialogVisible, async (visible) => {
  if (visible) {
    await nextTick()
    // 等待对话框动画完成后再初始化编辑器
    requestAnimationFrame(() => {
      if (editorContainer.value) {
        editor = createEditor(editorContainer.value, form.value.content)
        // 强制重新布局，确保容器宽度正确
        requestAnimationFrame(() => editor?.layout())
      }
    })
  } else {
    editor?.dispose()
    editor = null
  }
})

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

const loadSubscriptions = async () => {
  try {
    const res = await subscriptionApi.list()
    subscriptions.value = res.data
  } catch {
    ElMessage.error('加载订阅源列表失败')
  }
}

const openAddDialog = () => {
  dialogTitle.value = '添加脚本'
  form.value = { name: '', content: '' }
  tryRunResult.value = null
  selectedSubId.value = ''
  dialogVisible.value = true
}

const openEditDialog = async (name: string) => {
  dialogTitle.value = '编辑脚本'
  tryRunResult.value = null
  selectedSubId.value = ''
  try {
    const res = await scriptApi.get(name)
    form.value = { name, content: res.data }
    dialogVisible.value = true
  } catch {
    ElMessage.error('获取脚本内容失败')
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

const handleSubmit = async () => {
  const content = editor?.getValue() ?? form.value.content
  if (!form.value.name || !content) {
    ElMessage.warning('请填写脚本名称和内容')
    return
  }

  try {
    await scriptApi.save({ name: form.value.name, content })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadScripts()
  } catch {
    ElMessage.error('保存失败')
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

const handleFormat = () => {
  editor?.getAction('editor.action.formatDocument')?.run()
}

const handleTryRun = async () => {
  const content = editor?.getValue() ?? form.value.content
  if (!content || !selectedSubId.value) return

  tryRunLoading.value = true
  tryRunResult.value = null
  try {
    const res = await scriptApi.tryRun(content, selectedSubId.value)
    tryRunResult.value = res.data
  } catch {
    ElMessage.error('试运行请求失败')
  } finally {
    tryRunLoading.value = false
  }
}

onMounted(() => {
  loadScripts()
  loadSubscriptions()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h2>脚本管理</h2>
      <el-button type="primary" @click="openAddDialog">
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
          <el-button size="small" @click="openEditDialog(row.name)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.name)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="脚本名称" />
        </el-form-item>
        <el-form-item label="内容" required>
          <div ref="editorContainer" style="width: 100%; height: 400px; border: 1px solid #dcdfe6; border-radius: 4px;"></div>
        </el-form-item>
        <el-form-item label="试运行">
          <div style="display: flex; gap: 12px; align-items: center;">
            <el-select v-model="selectedSubId" placeholder="选择订阅源" style="width: 240px;">
              <el-option
                v-for="sub in subscriptions"
                :key="sub.id"
                :label="sub.name"
                :value="sub.id"
              />
            </el-select>
            <el-button
              type="success"
              :loading="tryRunLoading"
              :disabled="!selectedSubId"
              @click="handleTryRun"
            >
              ▶ 试运行
            </el-button>
          </div>
        </el-form-item>
        <el-form-item v-if="tryRunResult" label="运行结果">
          <div v-if="tryRunResult.success" style="width: 100%;">
            <el-alert type="success" :closable="false" show-icon>
              <template #title>
                执行成功 —
                代理节点: {{ tryRunResult.summary!.proxiesBefore }} → {{ tryRunResult.summary!.proxiesAfter }}，
                代理分组: {{ tryRunResult.summary!.groupsBefore }} → {{ tryRunResult.summary!.groupsAfter }}，
                规则: {{ tryRunResult.summary!.rulesBefore }} → {{ tryRunResult.summary!.rulesAfter }}
              </template>
            </el-alert>
            <el-collapse style="margin-top: 8px;">
              <el-collapse-item title="查看完整输出 config">
                <pre style="max-height: 300px; overflow: auto; font-size: 12px;">{{ JSON.stringify(tryRunResult.config, null, 2) }}</pre>
              </el-collapse-item>
            </el-collapse>
          </div>
          <div v-else style="width: 100%;">
            <el-alert type="error" :closable="false" show-icon>
              <template #title>执行失败</template>
              <template #default>
                <pre style="font-size: 12px; white-space: pre-wrap;">{{ tryRunResult.error }}</pre>
              </template>
            </el-alert>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleFormat">格式化</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看内容对话框 -->
    <el-dialog v-model="viewDialogVisible" :title="`查看脚本: ${viewName}`" width="800px" destroy-on-close>
      <div ref="viewEditorContainer" style="width: 100%; height: 500px; border: 1px solid #dcdfe6; border-radius: 4px;"></div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
