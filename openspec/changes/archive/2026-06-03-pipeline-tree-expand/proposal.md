## Why

当前构建流程页面使用嵌套表格展示构建记录，虽然功能完整但视觉上不够直观。用户期望改为树状展开，父节点是构建流程，展开后直接显示构建历史作为叶子节点，交互更自然、层次更清晰。

## What Changes

**构建流程表格交互**
- From: `type="expand"` 展开行 + 嵌套子表格
- To: `lazy` 树状展开，pipeline 为父节点，records 为子节点
- Impact: 纯前端改动，非破坏性变更

## Capabilities

### New Capabilities
- 无新增 capability

### Modified Capabilities
- `build-pipeline-table`: 构建流程表格的展开交互从嵌套表格改为树状展开

## Impact

- **前端文件**: `BuildPipelineView.vue`（主要修改）
- **API**: 无需改动
- **后端**: 无需改动
