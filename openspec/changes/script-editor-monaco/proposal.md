## Why

当前脚本管理页面使用纯 textarea 编辑 JavaScript 脚本，没有任何语法高亮、错误提示或代码格式化功能。用户编写脚本时无法及时发现语法错误，必须保存后通过构建流水线才能验证，调试效率低。此外，用户无法在保存前预览脚本对订阅配置的实际影响，只能反复试错。将编辑器升级为 Monaco Editor 并提供试运行功能，可以显著提升脚本编写和调试体验。

## What Changes

**脚本编辑器升级**
- From: 纯 textarea，monospace 字体，无语法辅助
- To: Monaco Editor，JavaScript 语法高亮、括号匹配、行号、实时语法错误诊断
- Impact: 非破坏性变更，仅影响前端 ScriptView.vue

**新增试运行功能**
- From: 脚本只能保存后通过构建流水线验证
- To: 用户可选择订阅源，直接在编辑页面执行脚本，查看变更摘要（proxy-groups/rules 数量变化）或错误堆栈
- Impact: 新增后端 API `POST /api/scripts/try-run`，前端新增试运行 UI

## Capabilities

### New Capabilities
- `script-code-editor`: Monaco Editor 集成，提供 JavaScript 语法高亮、括号匹配、错误诊断、代码格式化
- `script-trial-run`: 试运行功能，用户选择订阅源执行脚本，查看变更摘要或错误信息

### Modified Capabilities

## Impact

- 前端新增依赖：`monaco-editor`（~800KB gzip，用户已确认可接受）
- 后端 `ScriptController` 新增 `POST /api/scripts/try-run` 端点
- 后端 `ScriptController` 需注入 `SubscriptionService` 依赖
- 改动文件：`ScriptView.vue`、`script.ts`、`ScriptController.java`、`package.json`
