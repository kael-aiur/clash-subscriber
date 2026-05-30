# Brainstorm: Hash Routing

## 背景

当前前端使用 Vue Router 的 `createWebHistory()` (HTML5 history 模式)，URL 格式为路径模式（如 `/subscriptions`）。

用户希望改为 hash 模式，URL 格式变为 `/#/subscriptions`。

## 决策

### Q1: 为什么需要改为 hash 模式？

Hash 模式的优势：
1. **无需服务器配置** - 不需要配置服务器将所有路由重定向到 index.html
2. **兼容性更好** - 在所有浏览器中都能正常工作
3. **部署更简单** - 特别是在嵌入到 Spring Boot 等后端服务中时

### Q2: 改动范围

仅需修改一个文件：
- `module-web/frontend/src/router/index.ts`

改动内容：
- 将 `createWebHistory()` 改为 `createWebHashHistory()`

## 设计

### 技术方案

1. **修改路由创建方式**
   - 从: `import { createRouter, createWebHistory } from 'vue-router'`
   - 到: `import { createRouter, createWebHashHistory } from 'vue-router'`

2. **修改路由配置**
   - 从: `history: createWebHistory()`
   - 到: `history: createWebHashHistory()`

### 影响分析

- **URL 格式变化**: `/subscriptions` → `/#/subscriptions`
- **无需修改其他代码**: Vue Router 的 API 保持不变
- **无需修改 Vite 配置**: Hash 模式不需要特殊配置

## 风险评估

- **低风险**: 这是 Vue Router 的标准用法，完全向后兼容
- **无破坏性**: 所有路由功能保持不变，只是 URL 格式变化
