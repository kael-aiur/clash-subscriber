# Hash Routing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将前端路由从 HTML5 history 模式改为 hash 模式

**Architecture:** 修改 Vue Router 配置，将 `createWebHistory()` 替换为 `createWebHashHistory()`，使 URL 格式从 `/path` 变为 `/#/path`

**Tech Stack:** Vue 3, Vue Router 4

---

### Task 1: 修改路由配置

**Files:**
- Modify: `module-web/frontend/src/router/index.ts`

- [ ] **Step 1: 修改导入语句**

将第 1 行的 `createWebHistory` 改为 `createWebHashHistory`：

```typescript
import { createRouter, createWebHashHistory } from 'vue-router'
```

- [ ] **Step 2: 修改路由创建**

将第 3 行的 `history: createWebHistory()` 改为 `history: createWebHashHistory()`：

```typescript
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    // ... 路由配置保持不变
  ],
})
```

- [ ] **Step 3: 验证修改**

运行前端开发服务器验证路由正常工作：

```bash
cd module-web/frontend && npm run dev
```

访问 `http://localhost:5173/#/subscriptions` 确认页面正常加载

- [ ] **Step 4: 提交代码**

```bash
git add module-web/frontend/src/router/index.ts
git commit -m "feat(frontend): 将路由模式从 history 改为 hash"
```
