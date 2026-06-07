# 脚本编辑器全屏布局 Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 将脚本编辑器从 800px 对话框升级为全屏左右分栏页面，左侧集成试运行交互（步骤可视化 + ConfigCard 卡片），右侧为 Monaco 编辑器。

**Architecture:** 新增独立路由页面 `/scripts/edit/:name`，容器组件 ScriptEditorView 管理左右分栏布局和可拖动分隔条，左面板 ScriptTrialPanel 负责试运行交互，右面板 ScriptCodePanel 负责代码编辑。后端拆分试运行为 preview-subscription + try-run 两个 API。

**Tech Stack:** Vue 3 + TypeScript + Element Plus + Monaco Editor + Spring Boot

---

## Task 1: 后端 API — preview-subscription 端点

**Files:**
- Modify: `module-web/src/main/java/site/kael/clash/web/controller/ScriptController.java`

- [ ] **Step 1: 添加 YAML 序列化依赖导入**

在 ScriptController.java 顶部添加所需的 import：

```java
import site.kael.clash.common.model.ProxyNode;
import java.util.Collections;
import org.yaml.snakeyaml.Yaml;
```

- [ ] **Step 2: 添加 buildConfigSummary 辅助方法**

在 ScriptController.java 中添加私有方法（复用 BuildPipelineServiceImpl 的逻辑）：

```java
private Map<String, Object> buildConfigSummary(ClashConfig config) {
    List<ProxyNode> proxies = config.getProxies() != null ? config.getProxies() : Collections.emptyList();
    Map<String, Object> groups = config.getProxyGroups() != null ? config.getProxyGroups() : Collections.emptyMap();
    List<Object> rules = config.getRules() != null ? config.getRules() : Collections.emptyList();

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("nodeCount", proxies.size());
    summary.put("proxyGroupCount", groups.size());
    summary.put("ruleCount", rules.size());
    summary.put("nodeNames", proxies.stream().limit(5).map(ProxyNode::getName).collect(Collectors.toList()));
    summary.put("proxyGroupNames", groups.keySet().stream().limit(5).collect(Collectors.toList()));
    return summary;
}

private String serializeToYaml(Map<String, Object> raw) {
    Yaml yaml = new Yaml();
    return yaml.dump(raw);
}
```

- [ ] **Step 3: 添加 preview-subscription 端点**

在 ScriptController.java 中添加新端点：

```java
/**
 * 预览订阅源配置：获取订阅源的完整配置摘要和 YAML
 */
@PostMapping("/preview-subscription")
public ResponseEntity<Map<String, Object>> previewSubscription(@RequestBody Map<String, String> body) {
    String subscriptionId = body.get("subscriptionId");
    if (subscriptionId == null || subscriptionId.isBlank()) {
        throw new BusinessException(400, "请选择订阅源");
    }

    log.info("预览订阅配置: subscriptionId={}", subscriptionId);

    try {
        ClashConfig config = subscriptionService.fetch(subscriptionId);
        Map<String, Object> summary = buildConfigSummary(config);
        String yaml = serializeToYaml(config.getRaw());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("yaml", yaml);

        return ResponseEntity.ok(response);
    } catch (Exception e) {
        log.warn("预览订阅配置失败: {}", e.getMessage());
        throw new BusinessException("获取订阅配置失败: " + e.getMessage());
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
cd /Users/kael/workspace/github/kael-aiur/clash-subscriber && mvn compile -pl module-web -am
```

- [ ] **Step 5: 提交**

```bash
git add module-web/src/main/java/site/kael/clash/web/controller/ScriptController.java
git commit -m "feat(script): 新增 preview-subscription API 端点"
```

---

## Task 2: 后端 API — 修改 try-run 返回格式

**Files:**
- Modify: `module-web/src/main/java/site/kael/clash/web/controller/ScriptController.java`

- [ ] **Step 1: 修改 tryRun 方法返回格式**

在 `tryRun` 方法中，在构建 response 之前添加输入/输出摘要和 YAML：

```java
// 在 try 块内，计算完 proxiesAfter 等之后：

Map<String, Object> inputSummary = buildConfigSummary(config);
String inputYaml = serializeToYaml(config.getRaw());

Map<String, Object> outputSummary = buildConfigSummary(result);
String outputYaml = serializeToYaml(result.getRaw());

Map<String, Object> response = new LinkedHashMap<>();
response.put("success", true);
response.put("summary", summary);
response.put("config", result.getRaw());
response.put("inputSummary", inputSummary);
response.put("inputYaml", inputYaml);
response.put("outputSummary", outputSummary);
response.put("outputYaml", outputYaml);
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/kael/workspace/github/kael-aiur/clash-subscriber && mvn compile -pl module-web -am
```

- [ ] **Step 3: 提交**

```bash
git add module-web/src/main/java/site/kael/clash/web/controller/ScriptController.java
git commit -m "feat(script): try-run 返回格式新增输入/输出配置摘要"
```

---

## Task 3: 前端 API 层

**Files:**
- Modify: `module-web/frontend/src/api/script.ts`

- [ ] **Step 1: 更新类型定义和新增 API 方法**

将 `module-web/frontend/src/api/script.ts` 修改为：

```typescript
import api from './index'

export interface ScriptData {
  name: string
  content: string
}

export interface ConfigSummary {
  nodeCount: number
  proxyGroupCount: number
  ruleCount: number
  nodeNames?: string[]
  proxyGroupNames?: string[]
}

export interface PreviewSubscriptionResult {
  summary: ConfigSummary
  yaml: string
}

export interface TryRunResult {
  success: boolean
  summary?: {
    proxiesBefore: number
    proxiesAfter: number
    groupsBefore: number
    groupsAfter: number
    rulesBefore: number
    rulesAfter: number
  }
  config?: Record<string, unknown>
  error?: string
  inputSummary?: ConfigSummary
  inputYaml?: string
  outputSummary?: ConfigSummary
  outputYaml?: string
}

export const scriptApi = {
  list() {
    return api.get<string[]>('/scripts')
  },

  get(name: string) {
    return api.get<string>(`/scripts/${name}`)
  },

  save(data: ScriptData) {
    return api.post('/scripts', data)
  },

  delete(name: string) {
    return api.delete(`/scripts/${name}`)
  },

  tryRun(scriptContent: string, subscriptionId: string) {
    return api.post<TryRunResult>('/scripts/try-run', { scriptContent, subscriptionId })
  },

  previewSubscription(subscriptionId: string) {
    return api.post<PreviewSubscriptionResult>('/scripts/preview-subscription', { subscriptionId })
  },
}
```

- [ ] **Step 2: 提交**

```bash
git add module-web/frontend/src/api/script.ts
git commit -m "feat(script): 前端 API 新增 previewSubscription 方法和类型定义"
```

---

## Task 4: 创建 ScriptCodePanel.vue 右侧面板

**Files:**
- Create: `module-web/frontend/src/components/ScriptCodePanel.vue`

- [ ] **Step 1: 创建 ScriptCodePanel.vue**

```vue
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
```

- [ ] **Step 2: 提交**

```bash
git add module-web/frontend/src/components/ScriptCodePanel.vue
git commit -m "feat(script): 创建 ScriptCodePanel 右侧面板组件"
```

---

## Task 5: 创建 ScriptTrialPanel.vue 左侧面板

**Files:**
- Create: `module-web/frontend/src/components/ScriptTrialPanel.vue`

- [ ] **Step 1: 创建 ScriptTrialPanel.vue**

```vue
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
```

- [ ] **Step 2: 提交**

```bash
git add module-web/frontend/src/components/ScriptTrialPanel.vue
git commit -m "feat(script): 创建 ScriptTrialPanel 左侧面板组件"
```

---

## Task 6: 创建 ScriptEditorView.vue 容器

**Files:**
- Create: `module-web/frontend/src/views/ScriptEditorView.vue`

- [ ] **Step 1: 创建 ScriptEditorView.vue**

```vue
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
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

const isNew = computed(() => route.params.name === '__new__')

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
onBeforeRouteLeave((to, from, next) => {
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
```

- [ ] **Step 2: 提交**

```bash
git add module-web/frontend/src/views/ScriptEditorView.vue
git commit -m "feat(script): 创建 ScriptEditorView 全屏编辑器容器"
```

---

## Task 7: 路由与列表页集成

**Files:**
- Modify: `module-web/frontend/src/router/index.ts`
- Modify: `module-web/frontend/src/views/ScriptView.vue`

- [ ] **Step 1: 添加路由**

在 `module-web/frontend/src/router/index.ts` 的 routes 数组中，在 `/scripts` 路由之后添加：

```typescript
{
  path: '/scripts/edit/:name',
  name: 'ScriptEditor',
  component: () => import('@/views/ScriptEditorView.vue'),
  meta: { title: '编辑脚本' },
},
```

- [ ] **Step 2: 修改 ScriptView.vue — 编辑按钮改为路由跳转**

在 ScriptView.vue 中：

1. 移除 `dialogVisible`、`dialogTitle`、`form`、`editorContainer`、`editor`、`createEditor`、`watch(dialogVisible)`、`openAddDialog`、`openEditDialog`、`handleSubmit`、`handleFormat`、试运行相关变量等编辑对话框相关代码

2. 修改"添加脚本"按钮：
```vue
<el-button type="primary" @click="router.push('/scripts/edit/__new__')">
```

3. 修改表格中"编辑"按钮：
```vue
<el-button size="small" @click="router.push(`/scripts/edit/${row.name}`)">编辑</el-button>
```

4. 移除编辑对话框的 `<el-dialog>` 部分

5. 添加 `useRouter`：
```typescript
import { useRouter } from 'vue-router'
const router = useRouter()
```

- [ ] **Step 3: 编译验证**

```bash
cd /Users/kael/workspace/github/kael-aiur/clash-subscriber/module-web/frontend && npm run build
```

- [ ] **Step 4: 提交**

```bash
git add module-web/frontend/src/router/index.ts module-web/frontend/src/views/ScriptView.vue
git commit -m "feat(script): 路由集成，列表页编辑按钮改为路由跳转"
```

---

## Task 8: 手动验证

- [ ] **Step 1: 启动后端和前端，验证路由跳转**

访问 `/scripts`，点击"编辑"按钮，确认跳转到全屏编辑器页面

- [ ] **Step 2: 验证可拖动分隔条**

拖动分隔条，确认左面板宽度在 200-500px 范围内调整

- [ ] **Step 3: 验证试运行流程**

选择订阅源，点击试运行，确认三个步骤依次执行，输入/输出卡片正确显示

- [ ] **Step 4: 验证页面离开保护**

修改脚本内容但不保存，尝试离开页面，确认弹出确认框
