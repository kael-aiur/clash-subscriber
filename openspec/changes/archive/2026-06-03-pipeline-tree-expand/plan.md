# 构建流程树状展开实现计划

**Goal:** 将构建流程表格从嵌套表格改为 el-table lazy 树状展开。

**Architecture:** 纯前端改动，使用 el-table 的 `row-key` + `lazy` + `load` 实现懒加载树。

**Tech Stack:** Vue 3, Element Plus, TypeScript

---

## Task 1: 类型和数据结构

- [ ] **Step 1:** 在 `module-web/frontend/src/api/build-pipeline.ts` 新增 TreeRow 接口
- [ ] **Step 2:** 在 `BuildPipelineView.vue` 中清理嵌套表格相关代码

## Task 2: 表格改为树状展开

- [ ] **Step 1:** 修改 el-table 配置，添加 `lazy`、`load`、`row-key`
- [ ] **Step 2:** 实现 `loadTreeChildren` 懒加载方法
- [ ] **Step 3:** 叶子节点列定义（时间、状态、错误信息）
- [ ] **Step 4:** 叶子节点样式 CSS
- [ ] **Step 5:** 叶子节点点击跳转

## 提交点

- Commit 1: 前端表格改为树状展开（一个 commit 完成）
