# 脚本管理优化 — 设计文档

## 概述

将脚本管理页面的编辑器从纯 textarea 替换为 Monaco Editor，并新增试运行功能，允许用户选择订阅源执行脚本查看效果。

## 目标

1. 提供 JavaScript 语法高亮、括号匹配、行号显示
2. 提供 JS 语法错误实时诊断（红色波浪线）
3. 支持格式化代码
4. 支持选择订阅源试运行脚本，查看变更摘要或错误信息

## 不做

- 断点调试
- 脚本模板插入
- 智能补全（config 属性提示）
- 脚本版本管理
- 脚本存储方式变更

## 架构

### 前端

#### Monaco Editor 集成

`ScriptView.vue` 中的 textarea 替换为 Monaco Editor：

- npm 依赖：`monaco-editor`
- 语言：`javascript`
- 主题：跟随系统（`vs` / `vs-dark`）
- 启用功能：行号、括号匹配、自动缩进、JS 语法错误诊断
- 禁用功能：智能补全（`quickSuggestions: false`）
- 按钮：保存、试运行、格式化

#### 试运行 UI

编辑器下方新增试运行区域：

- 订阅源下拉选择（el-select），数据来自 `GET /api/subscriptions`
- 试运行按钮，点击后发送当前编辑器内容到后端
- 结果面板：
  - 成功：显示变更摘要（proxies/rules/groups 变化数量），可折叠查看完整 config JSON
  - 失败：显示错误信息和堆栈，红色高亮

#### API 变更

`src/api/script.ts` 新增方法：

```typescript
tryRun(scriptContent: string, subscriptionId: string): Promise<TryRunResult>
```

### 后端

#### 新增 API

`ScriptController.java` 新增端点：

```
POST /api/scripts/try-run
Content-Type: application/json

{
  "scriptContent": "function main(config, profileName) { ... }",
  "subscriptionId": "abc123"
}
```

实现流程：
1. 调用 `subscriptionService.fetch(subscriptionId)` 获取订阅源的 ClashConfig
2. 调用 `ScriptEngine.execute(scriptContent, config)` 执行脚本
3. 返回执行结果

成功响应：
```json
{
  "success": true,
  "summary": {
    "proxiesBefore": 15,
    "proxiesAfter": 15,
    "groupsBefore": 5,
    "groupsAfter": 8,
    "rulesBefore": 120,
    "rulesAfter": 135
  },
  "config": { }
}
```

失败响应：
```json
{
  "success": false,
  "error": "ReferenceError: xxx is not defined\n    at main(<script>:3:15)"
}
```

#### 依赖关系

- `ScriptController` 依赖 `SubscriptionService`（通过构造器注入）
- `ScriptEngine` 无需改动，直接复用

## 改动文件清单

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `module-web/frontend/package.json` | 修改 | 新增 `monaco-editor` 依赖 |
| `module-web/frontend/src/views/ScriptView.vue` | 重写 | textarea → Monaco + 试运行 UI |
| `module-web/frontend/src/api/script.ts` | 修改 | 新增 `tryRun()` |
| `module-web/.../controller/ScriptController.java` | 修改 | 新增 `POST /try-run` 端点 |

## 风险

- Monaco Editor 包体积较大（~800KB gzip），但用户已确认不在意
- 试运行需要实时请求订阅源，可能有网络延迟，需要 loading 状态
