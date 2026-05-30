# 设计文档：Hash Routing

## Context

当前前端使用 Vue Router 4 的 `createWebHistory()` (HTML5 history 模式)，URL 格式为路径模式（如 `/subscriptions`）。

用户希望改为 hash 模式，URL 格式变为 `/#/subscriptions`。

**当前状态：**
- Vue 3 + Vue Router 4
- 使用 `createWebHistory()`
- 路由配置文件：`module-web/frontend/src/router/index.ts`

## Goals / Non-Goals

**Goals:**
- 将路由模式从 history 改为 hash
- URL 格式从 `/path` 变为 `/#/path`
- 保持所有路由功能不变

**Non-Goals:**
- 不修改其他前端代码
- 不修改 Vite 配置
- 不改变路由行为

## Decisions

### D1: 使用 `createWebHashHistory()`

- **选择**: 将 `createWebHistory()` 改为 `createWebHashHistory()`
- **理由**: 这是 Vue Router 官方提供的 hash 模式实现，简单直接
- **已考虑 alternative**: 无，这是唯一标准方案

## Risks / Trade-offs

- **[Trade-off]** URL 格式变化，用户看到的 URL 会包含 `#` → 这是 hash 模式的标准行为，可接受
- **[风险]** 无 → 这是 Vue Router 的标准用法，完全向后兼容

## Migration Plan

N/A — 本 change 不涉及部署变更，仅修改前端路由配置。

## Open Questions

无
