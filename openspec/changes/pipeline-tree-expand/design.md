## Context

当前构建流程页面使用 `el-table` 的 `type="expand"` 展开行，展开后嵌套一个子表格显示构建记录。用户期望改为树状展开，视觉上更像一棵树：父节点是构建流程，子节点（叶子）是构建历史。

## Goals / Non-Goals

**Goals:**
- 构建流程表格支持树状展开，父节点为流程，子节点为构建记录
- 展开时懒加载构建记录
- 叶子节点（构建记录）不可展开，点击跳转详情页
- 叶子节点样式与父节点区分

**Non-Goals:**
- 不改变现有 API
- 不改变构建记录详情页
- 不改变后端逻辑

## Decisions

### D1: 使用 el-table lazy tree 而非嵌套表格
- **选择**: 使用 `el-table` 的 `row-key` + `lazy` + `load` 属性实现树状展开
- **理由**: Element Plus 原生支持懒加载树表格，比嵌套表格更符合"树状"的视觉需求
- **已考虑 alternative**: 保持嵌套表格但改样式 — 本质还是两层表格，不够"树状"

### D2: 统一行数据类型
- **选择**: 定义 `TreeRow` 类型，pipeline 和 record 共用，通过 `type` 字段区分
- **理由**: el-table 树要求统一数据结构，不能混用不同类型
- **已考虑 alternative**: 保持原类型不变 — 无法适配 el-table 树的 `load` 回调

### D3: 懒加载 + 缓存
- **选择**: 展开 pipeline 时懒加载 records，缓存在 `Map<string, TreeRow[]>` 中
- **理由**: 避免重复请求，与现有实现一致
- **已考虑 alternative**: 页面加载时一次性获取所有记录 — 流程多时性能差

## Risks / Trade-offs

- [Risk] el-table 树的展开图标与原有展开三角样式不同 → 通过 CSS 调整
- [Trade-off] 叶子节点复用 el-table 行而非独立组件 → 样式灵活性降低，但实现更简单

## Migration Plan

N/A — 纯前端改动，无部署变更

## Open Questions

- 叶子节点是否需要显示更多字段（如耗时）？
