# 脚本编辑器全屏布局 - 头脑风暴

## 背景

当前脚本编辑器使用 800px 宽的 el-dialog，Monaco Editor 高度固定 400px，试运行区域与代码编辑上下排列，空间利用率低。用户希望升级为全屏左右分栏布局，左侧为试运行交互面板，右侧为代码编辑器。

## 决策记录

### Q1: 全屏实现方式

**选项**：
- A. 新路由页面 `/scripts/edit/:name`
- B. 全屏对话框（100vw × 100vh）
- C. el-dialog fullscreen 属性

**决策**：A — 新路由页面。独立页面更清晰，路由可直接访问，浏览器前进/后退可用。

### Q2: 脚本名称输入框位置

**选项**：
- A. 右侧工具栏内
- B. 左侧面板顶部
- C. 不需要（列表页编辑）

**决策**：B — 左侧面板顶部。名称是脚本标识，与试运行操作同侧更合理。

### Q3: 左侧面板宽度

**选项**：
- A. 固定 260px
- B. 可拖动调整

**决策**：B — 可拖动调整。默认 260px，最小 200px，最大 500px。分隔条 4px 宽，hover 变色提示。

### Q4: 试运行执行流程

**选项**：
- A. 严格顺序执行（1→2→3，失败停止）
- B. 分步确认执行
- C. 一键执行 + 实时进度

**决策**：A — 严格顺序执行。点击试运行后依次执行三个步骤，任一步骤失败则停止后续步骤。

### Q5: 步骤展示形式

**选项**：
- A. 步骤条 + 状态图标（自定义样式）
- B. el-steps 组件

**决策**：B — 使用 Element Plus 的 el-steps 组件，direction=vertical，更规范统一。

### Q6: 输入/输出卡片展示

**选项**：
- A. 两张卡片并存
- B. 输出替换输入

**决策**：A — 两张卡片并存。步骤 1 成功后显示输入卡片，步骤 2 成功后显示输出卡片，两者同时可见。

### Q7: 卡片内容

**选项**：
- A. 统计标签（节点/代理组/规则）
- B. 节点列表预览
- C. 代理组列表预览
- D. 完整 YAML 查看

**决策**：全部（A+B+C+D）。复用现有 ConfigCard 组件，支持统计标签、节点/代理组列表预览、可展开查看完整 YAML。

### Q8: 输出卡片变更摘要

**选项**：
- A. 显示变更摘要（before → after）
- B. 仅展示最终数据

**决策**：A — 输出卡片额外显示变更摘要，使用 el-tag 展示 `节点: 15 → 18`、`代理组: 5 → 8`、`规则: 120 → 135`。

### Q9: API 设计

**选项**：
- A. 前端拆分调用（两个 API）
- B. 后端返回两份数据（单个 API）

**决策**：A — 拆分为两个 API。分步调用能真实反映执行进度，步骤 1 失败时不执行步骤 2。

### Q10: 实现方案

**选项**：
- A. 单组件方案（ScriptEditorView.vue 约 400 行）
- B. 组件拆分方案（容器 + ScriptTrialPanel + ScriptCodePanel）
- C. 组件 + Composable 方案

**决策**：B — 组件拆分方案。左面板（试运行交互）和右面板（代码编辑）职责清晰，但不需要过度抽取 composable。

## 设计方案

### 组件结构

```
views/ScriptEditorView.vue          ← 容器，管理路由 + 全屏布局 + 可拖动分隔条
├── components/ScriptTrialPanel.vue ← 左侧试运行面板
└── components/ScriptCodePanel.vue  ← 右侧代码编辑器面板
```

### 路由

```typescript
// 新增路由
{
  path: '/scripts/edit/:name',
  name: 'ScriptEditor',
  component: () => import('@/views/ScriptEditorView.vue'),
  meta: { title: '编辑脚本' }
}
```

- 编辑已有脚本：`/scripts/edit/:name`
- 新建脚本：`/scripts/edit/__new__`（特殊标识，保存时提示输入名称）

### ScriptEditorView.vue（容器）

**职责**：
- 全屏布局（100vw × 100vh），左右分栏
- 可拖动分隔条实现（mousedown + mousemove）
- 从 URL 参数读取脚本名称，加载脚本内容
- 协调左右面板通信

**核心状态**：
```typescript
const panelWidth = ref(260)           // 左面板宽度
const scriptName = ref('')            // 脚本名称
const scriptContent = ref('')         // 脚本内容（Monaco 实时值）
const isDirty = ref(false)            // 是否有未保存修改
const trialState = ref<TrialState>()  // 试运行状态
```

**分隔条实现**：
```typescript
const startResize = (e: MouseEvent) => {
  const startX = e.clientX
  const startWidth = panelWidth.value
  const onMove = (e: MouseEvent) => {
    panelWidth.value = Math.max(200, Math.min(500, startWidth + e.clientX - startX))
  }
  const onUp = () => {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}
```

**页面离开保护**：
```typescript
onBeforeRouteLeave((to, from, next) => {
  if (isDirty.value) {
    ElMessageBox.confirm('脚本尚未保存，确定离开？', '提示', { type: 'warning' })
      .then(() => next())
      .catch(() => next(false))
  } else {
    next()
  }
})
```

### ScriptTrialPanel.vue（左侧面板）

**布局**：
```
ScriptTrialPanel (可拖动宽度，默认 260px)
├── 脚本名称输入 (el-input)
├── 试运行区域
│   ├── 订阅源选择 (el-select)
│   └── 试运行按钮 (el-button, type=success)
├── 步骤条 (el-steps, direction=vertical)
│   ├── Step 1: 获取订阅配置
│   ├── Step 2: 执行脚本
│   └── Step 3: 执行结果
├── 输入卡片 (v-if step1.success)
│   └── ConfigCard (summary + yaml)
└── 输出卡片 (v-if step2.success)
    ├── 变更摘要 (el-tag: before → after)
    └── ConfigCard (summary + yaml)
```

**试运行状态**：
```typescript
interface TrialState {
  step: 0 | 1 | 2 | 3        // 0=未开始, 1=获取中, 2=执行中, 3=完成
  status: 'wait' | 'process' | 'finish' | 'error' | 'success'
  inputConfig?: ConfigSummary   // 步骤1成功后的订阅配置摘要
  inputYaml?: string            // 步骤1成功后的配置 YAML
  outputConfig?: ConfigSummary  // 步骤2成功后的输出配置摘要
  outputYaml?: string           // 步骤2成功后的输出 YAML
  changeSummary?: {             // 变更对比
    proxiesBefore: number; proxiesAfter: number
    groupsBefore: number; groupsAfter: number
    rulesBefore: number; rulesAfter: number
  }
  error?: string               // 失败时的错误信息
}
```

**步骤流转逻辑**：
```
点击试运行
  → step=1 (process): 调用 POST /api/scripts/preview-subscription
    → 成功: step=1 (finish), 显示输入卡片
    → 失败: step=1 (error), 显示错误信息, 停止
  → step=2 (process): 调用 POST /api/scripts/try-run
    → 成功: step=2 (finish), 显示输出卡片 + 变更摘要
    → 失败: step=2 (error), 显示错误信息, 停止
  → step=3 (success): 执行完成
```

**Props / Emits**：
```typescript
// Props
defineProps<{
  modelValue: TrialState
  scriptName: string
  subscriptions: Subscription[]
}>()

// Emits
defineEmits<{
  'update:modelValue': [state: TrialState]
  'tryRun': [subscriptionId: string]
  'update:scriptName': [name: string]
}>()
```

### ScriptCodePanel.vue（右侧面板）

**布局**：
```
ScriptCodePanel (flex: 1, 自适应宽度)
├── 工具栏 (height: 40px, border-bottom)
│   ├── 脚本名称 (只读展示, 或 "新建脚本")
│   ├── 已保存/未保存状态指示
│   └── 按钮组: 格式化 | 保存 | 返回列表
└── Monaco Editor (flex: 1, 全高)
    ├── language: javascript
    ├── theme: vs (浅色)
    ├── minimap: 禁用
    ├── 行号、括号颜色化、自动布局
    └── 字号 14px, tab 2空格
```

**工具栏按钮**：
| 按钮 | 行为 |
|------|------|
| 格式化 | Monaco `editor.getAction('editor.action.formatDocument').run()` |
| 保存 | 调用 `scriptApi.save(name, content)`，显示成功/失败提示 |
| 返回列表 | `router.push('/scripts')`，如有未保存修改则弹确认框 |

**Props / Emits**：
```typescript
// Props
defineProps<{
  scriptName: string
  initialContent: string
}>()

// Emits
defineEmits<{
  save: [content: string]
  dirty: [isDirty: boolean]
}>()
```

### API 变更

**新增接口**：
```
POST /api/scripts/preview-subscription
Body: { subscriptionId: string }
Response: {
  config: ClashConfig,
  summary: {
    nodeCount: number,
    proxyGroupCount: number,
    ruleCount: number,
    nodeNames: string[],
    proxyGroupNames: string[]
  },
  yaml: string
}
```

**修改接口**：
```
POST /api/scripts/try-run
Body: { scriptContent: string, subscriptionId: string }
Response: {
  success: boolean,
  summary: { proxiesBefore, proxiesAfter, groupsBefore, groupsAfter, rulesBefore, rulesAfter },
  config: ClashConfig,
  // 新增：输入配置摘要（供输出卡片对比展示）
  inputSummary: { nodeCount, proxyGroupCount, ruleCount, nodeNames, proxyGroupNames },
  inputYaml: string,
  // 新增：输出配置摘要
  outputSummary: { nodeCount, proxyGroupCount, ruleCount, nodeNames, proxyGroupNames },
  outputYaml: string
}
```

**保留接口**（列表页不变）：
- `GET /api/scripts` — 列表
- `GET /api/scripts/:name` — 获取内容
- `POST /api/scripts` — 保存
- `DELETE /api/scripts/:name` — 删除

### 错误处理

| 场景 | 处理方式 |
|------|----------|
| 脚本加载失败（路由参数无效） | 提示"脚本不存在"，返回按钮跳回列表 |
| 订阅配置获取失败（步骤 1） | 步骤条显示 error 状态，输入卡片位置显示错误信息 |
| 脚本执行失败（步骤 2） | 步骤条显示 error 状态，输出卡片位置显示错误堆栈 |
| 保存失败 | ElMessage.error 提示，不离开页面 |
| 未保存离开 | ElMessageBox.confirm 确认框 |
| 网络中断 | 试运行按钮 loading 状态自动恢复，提示"请求失败" |

### 新建脚本流程

```
/scripts/edit/__new__
├── 初始状态：名称为空，内容为空模板
├── 试运行：需要先输入名称，选择订阅源，直接用编辑器内容执行
└── 保存时：若名称为空，弹窗要求输入名称
```

**空模板内容**：
```javascript
/**
 * 脚本入口函数
 * @param {Object} config - Clash 配置对象
 * @returns {Object} 处理后的配置
 */
function main(config) {
  // 在此编写你的脚本逻辑
  return config
}
```

## 需要修改的文件

### 新增文件
- `module-web/frontend/src/views/ScriptEditorView.vue` — 全屏编辑器容器
- `module-web/frontend/src/components/ScriptTrialPanel.vue` — 左侧试运行面板
- `module-web/frontend/src/components/ScriptCodePanel.vue` — 右侧代码编辑器面板

### 修改文件
- `module-web/frontend/src/router/index.ts` — 新增 `/scripts/edit/:name` 路由
- `module-web/frontend/src/views/ScriptView.vue` — 列表页"编辑"按钮改为跳转路由，移除编辑对话框
- `module-web/frontend/src/api/script.ts` — 新增 `previewSubscription` API
- `module-web/src/main/java/site/kael/clash/web/controller/ScriptController.java` — 新增 `preview-subscription` 端点，修改 `try-run` 返回格式

### 复用文件（不修改）
- `module-web/frontend/src/components/ConfigCard.vue` — 输入/输出卡片复用
