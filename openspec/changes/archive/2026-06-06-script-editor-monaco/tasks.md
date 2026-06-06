## 1. 后端：试运行 API

- [x] 1.1 ScriptController 注入 SubscriptionService 依赖
- [x] 1.2 ScriptController 新增 `POST /api/scripts/try-run` 端点，接收 `{ scriptContent, subscriptionId }`，调用 `subscriptionService.fetch()` 获取配置后执行脚本，返回成功摘要或错误信息

## 2. 前端：Monaco Editor 集成

- [x] 2.1 安装 `monaco-editor` npm 依赖
- [x] 2.2 ScriptView.vue 中将 textarea 替换为 Monaco Editor 组件，配置 JavaScript 语言、语法高亮、行号、括号匹配
- [x] 2.3 启用 Monaco 的 JavaScript 语法错误诊断（DiagnosticsOptions）
- [x] 2.4 实现编辑器"格式化"按钮功能
- [x] 2.5 调整编辑/查看对话框中 Monaco Editor 的尺寸和样式

## 3. 前端：试运行功能

- [x] 3.1 `script.ts` 新增 `tryRun(scriptContent, subscriptionId)` API 方法
- [x] 3.2 ScriptView.vue 编辑对话框新增订阅源下拉选择框，加载订阅源列表
- [x] 3.3 新增"试运行"按钮，发送编辑器当前内容到后端，处理加载状态
- [x] 3.4 实现试运行结果面板：成功时显示变更摘要（可折叠查看完整 config），失败时显示红色错误信息
