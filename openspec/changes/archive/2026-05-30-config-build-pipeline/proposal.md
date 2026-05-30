## Why

当前定时任务（ScheduledTask）承担了构建和调度双重职责，导致订阅获取逻辑无法独立触发、每次拉取全部订阅而非按需选择、无构建历史记录。随着订阅源和 mihomo 实例增多，用户需要更灵活的构建管理能力：指定特定订阅、可选脚本处理、手动/定时触发、完整构建历史追溯。

## What Changes

**新增"构建流程"概念**
- From: ScheduledTask 直接编排 fetch-all → pipeline → push 全流程
- To: BuildPipeline 独立封装构建链路（订阅 → 脚本 → 推送），ScheduledTask 简化为仅触发 BuildPipeline
- Impact: 新增模块 module-pipeline，新增前端页面，不破坏现有 API

**定时调度内嵌**
- From: 定时任务独立实体 ScheduledTask，关联 pipelineId + targetInstances
- To: BuildPipeline 自带 cronExpression，定时配置与构建配置在同一界面管理
- Impact: ScheduledTask 标记废弃，前端定时任务菜单保留但提示迁移

## Capabilities

### New Capabilities
- `build-pipeline`: 构建流程的 CRUD、手动触发执行、cron 定时调度、构建历史记录管理
- `build-record`: 构建历史记录的查询和展示

### Modified Capabilities
无

## Impact

- **新增模块**: `module-pipeline`（model、repository、service）
- **新增 API**: `/api/build-pipelines`（CRUD + execute + records）、`/api/build-records/{id}`
- **前端变更**: 新增"构建流程"菜单和 BuildPipelineView.vue 页面
- **依赖变更**: module-pipeline 依赖 module-subscription、module-processor、module-mihomo、module-common
- **现有代码**: SchedulerService 需要感知 BuildPipeline 的 cron 配置变更（增删改时同步 cron 任务）
