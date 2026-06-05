## Why

当前脚本编辑器采用 800px 对话框，Monaco Editor 高度固定 400px，试运行区域与代码编辑上下排列，空间利用率低。编辑复杂脚本时频繁滚动，试运行结果展示简陋（JSON 折叠），无法直观对比输入/输出配置。升级为全屏左右分栏布局可大幅提升编辑体验和试运行的可视化效果。

## What Changes

**编辑器布局**
- From: 800px el-dialog 对话框，上下排列
- To: 全屏独立路由页面 `/scripts/edit/:name`，左右分栏（左侧试运行面板 + 右侧代码编辑器）
- Impact: 非破坏性变更，列表页功能不变

**试运行交互**
- From: 单次 API 调用，结果以 el-alert + JSON 折叠展示
- To: 分步执行（获取配置 → 执行脚本 → 结果），el-steps 步骤条可视化，输入/输出 ConfigCard 卡片展示
- Impact: 非破坏性变更，后端需新增一个 API 端点

**左侧面板**
- From: 无
- To: 可拖动宽度面板（默认 260px），集成脚本名称、订阅源选择、试运行按钮、步骤条、输入/输出卡片
- Impact: 新增组件 ScriptTrialPanel.vue

## Capabilities

### New Capabilities
- `script-editor-fullscreen`: 全屏左右分栏脚本编辑器页面，包含可拖动分隔条、左侧面板（试运行交互）、右侧面板（Monaco 编辑器）
- `script-trial-steps`: 试运行分步执行与可视化，el-steps 步骤条展示三个环节状态，输入/输出 ConfigCard 卡片展示

### Modified Capabilities
- `script-trial-run`: 试运行 API 拆分为 preview-subscription + try-run 两个端点，try-run 返回格式新增输入/输出配置摘要

## Impact

- **前端新增**：ScriptEditorView.vue、ScriptTrialPanel.vue、ScriptCodePanel.vue
- **前端修改**：router/index.ts（新路由）、ScriptView.vue（编辑按钮改为路由跳转）、api/script.ts（新 API）
- **后端修改**：ScriptController.java（新增 preview-subscription 端点，修改 try-run 返回格式）
- **复用**：ConfigCard.vue（输入/输出卡片）
