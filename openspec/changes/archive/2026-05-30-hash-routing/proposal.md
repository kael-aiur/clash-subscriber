# 提案：Hash Routing

## Why

当前前端使用 HTML5 history 模式的路由（`createWebHistory()`），URL 格式为路径模式（如 `/subscriptions`）。这种模式需要服务器配置支持，将所有路由重定向到 index.html，否则刷新页面会返回 404。

改为 hash 模式（`createWebHashHistory()`）后，URL 格式变为 `/#/subscriptions`，无需服务器配置，部署更简单，兼容性更好。

## What Changes

**路由模式变更**
- From: `createWebHistory()` (HTML5 history 模式)
- To: `createWebHashHistory()` (hash 模式)
- Reason: 简化部署，无需服务器配置
- Impact: URL 格式变化，非破坏性变更

## Capabilities

### New Capabilities

无

### Modified Capabilities

- `frontend-routing`: 路由模式从 history 改为 hash

## Impact

**受影响的文件：**
- `module-web/frontend/src/router/index.ts` - 修改路由创建方式

**依赖：**
- 无新增依赖

**API：**
- 无变更
