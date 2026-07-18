## Why

构建流程每次执行（同步 `execute` 与异步 `executeAsync`，含定时 cron 触发）
完成后都会落一条 `BuildRecord`，当前实现以独立 JSON 文件存于
`data/build-records/`，**无上限、永不清理**。长期运行后该目录会无限膨胀，
历史列表也会被陈旧记录淹没。本 change 作为配置构建流程的配套优化，
在每个构建流程维度限制只保留最新 10 条，使磁盘占用可控、历史聚焦近期。

## What Changes

**构建历史数量限制**
- From: 每次构建完成无条件写入 `BuildRecord`，历史记录无上限、永不删除
- To: 每次构建完成、保存记录后，按 `startedAt` 时间顺序保留该构建流程
  最新的 10 条，删除更旧的记录
- Reason: 防止 `build-records/` 目录无限增长，历史列表聚焦近期运行
- Impact: non-breaking。仅删除过期历史文件，不影响构建主流程与对外 API

## Capabilities

### New Capabilities

无。本 change 不引入新 capability。

### Modified Capabilities

- `build-record`: 新增「限制构建历史数量」需求 —— 每个构建流程保留最新
  10 条历史，超出按时间顺序删除。

## Impact

- **代码**：
  - `BuildRecordRepository`（接口）：新增 `deleteById(String id)`
  - `JsonFileBuildRecordRepository`（实现）：实现文件删除
  - `BuildPipelineServiceImpl`：新增 `pruneOldRecords(pipelineId)`，
    在两个保存点（同步 `execute`、异步 `executeWithProgress`）之后调用
- **持久层**：`data/build-records/` 目录在后续每次构建后自动收敛到每
  pipeline 10 条；历史遗留的超量记录会在对应 pipeline 下次构建后自然清理，
  无需独立迁移脚本
- **API / 依赖 / 数据库**：无变更
