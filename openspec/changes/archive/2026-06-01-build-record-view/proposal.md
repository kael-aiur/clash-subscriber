## Why

当前构建流程页面需要点击"历史"按钮打开抽屉才能查看构建记录，交互不够直观。用户期望在表格行左侧有展开三角，直接查看构建记录，并能点击进入详情页查看每个环节的执行情况和数据流转。这将显著提升构建流程的可观测性和调试效率。

## What Changes

**构建流程表格交互**
- From: 点击"历史"按钮打开抽屉查看记录
- To: 表格行左侧展开三角，直接展开显示构建记录列表
- Impact: 非破坏性变更，现有功能完全保留

**构建记录详情页**
- From: 抽屉中显示文本日志
- To: 独立详情页，使用流程图展示 4 个构建环节，支持查看每个环节的输入输出
- Impact: 新增页面，不影响现有功能

**数据模型增强**
- From: BuildRecord 仅有 logs 字段（文本日志）
- To: BuildRecord 增加 steps 字段（结构化环节数据）
- Impact: 向后兼容，旧数据 steps 为空

## Capabilities

### New Capabilities
- `build-record-detail`: 构建记录详情页，展示线性流程图和每个环节的输入输出数据

### Modified Capabilities
- `build-pipeline-table`: 构建流程表格页面，将抽屉交互改为展开行交互

## Impact

- **前端文件**: BuildPipelineView.vue（修改）、新增 BuildRecordDetailView.vue、router/index.ts（修改）、build-pipeline.ts（修改类型定义）
- **后端文件**: 新增 BuildStep.java、修改 BuildRecord.java、修改 BuildPipelineServiceImpl.java
- **API**: 现有 GET /api/build-records/{id} 返回数据结构增强（新增 steps 字段）
- **存储**: BuildRecord JSON 文件新增 steps 字段（向后兼容）
