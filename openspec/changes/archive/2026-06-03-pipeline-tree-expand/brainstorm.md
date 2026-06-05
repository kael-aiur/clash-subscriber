# Brainstorm: 构建流程树状展开

## 背景

当前构建流程页面使用 `el-table` 的 `type="expand"` 展开行，展开后嵌套一个子表格显示构建记录。用户期望改为树状展开，每个流程行展开后直接显示构建历史作为叶子节点，视觉上更像一棵树。

## 现有实现分析

- 使用 `el-table-column type="expand"` + 嵌套 `el-table`
- 展开时懒加载构建记录，缓存在 `expandedRecords` 中
- 嵌套表格点击行跳转到详情页

## 设计决策

### Q1: 使用 Element Plus 的 tree 还是展开行？

**方案 A**: 使用 `el-table` 的 tree 功能（`row-key` + `treeProps` + `load`）
- 将 pipelines 和 records 统一为扁平数据
- 展开 pipeline 行时懒加载 records 作为子节点
- 树状视觉效果（缩进 + 展开图标）

**方案 B**: 保持 `type="expand"` 但优化嵌套表格样式
- 改变嵌套表格的样式使其看起来像树的叶子节点
- 本质还是嵌套表格

**决策**: 选择方案 A。Element Plus 的 el-table 支持懒加载树结构，通过 `load` 属性实现异步加载子节点。这比嵌套表格更符合"树状展开"的需求。

### Q2: 数据结构如何设计？

需要一个统一的行数据类型，pipeline 和 record 共用：

```typescript
interface TreeRow {
  id: string
  type: 'pipeline' | 'record'
  name: string
  // pipeline 字段
  primarySubscriptionId?: string
  scriptName?: string
  targetInstanceId?: string
  cronExpression?: string
  enabled?: boolean
  lastRunStatus?: string
  // record 字段
  startedAt?: string
  finishedAt?: string
  status?: string
  errorMessage?: string
}
```

### Q3: 树的展开逻辑？

- 初始数据：只加载 pipelines
- 展开 pipeline 行时：调用 `buildPipelineApi.getRecords(id)` 获取记录
- 将记录转换为 `TreeRow` 并作为子节点返回
- 使用 el-table 的 `load` 回调实现懒加载

### Q4: 叶子节点（构建记录）的展示？

- 叶子节点不需要展开三角
- 显示：开始时间、状态标签、错误信息
- 点击跳转到详情页
- 叶子行样式区分（如灰色背景、小字体）

## 技术方案

### 前端改动
1. 修改 `BuildPipelineView.vue`
   - 移除 `type="expand"` 列和嵌套表格
   - 改用 `row-key` + `lazy` + `load` 实现树状展开
   - 统一行数据类型，pipeline 和 record 共用
   - 叶子行样式区分
2. 保留现有的 API 调用和缓存逻辑

### 后端改动
- 无需改动，现有 API 已满足需求
