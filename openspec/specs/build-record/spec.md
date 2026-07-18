## Purpose

管理 Clash 订阅构建流程的执行历史记录（BuildRecord）：记录每次构建的执行情况、按构建流程查询历史列表、查询单条记录详情，并约束每个流程保留的历史数量。
## Requirements
### Requirement: 记录构建历史

系统 SHALL 在每次构建执行完成后记录 BuildRecord，包含构建流程 ID、开始时间、结束时间、状态、错误信息（如有）、执行日志。

#### Scenario: 构建成功记录
- **WHEN** 构建执行成功完成
- **THEN** 系统创建 BuildRecord，status=SUCCESS，记录 startedAt、finishedAt 和 logs

#### Scenario: 构建失败记录
- **WHEN** 构建执行过程中发生异常
- **THEN** 系统创建 BuildRecord，status=FAILED，记录 errorMessage 和已产生的 logs

### Requirement: 查询构建历史列表

系统 SHALL 支持按构建流程 ID 查询其所有构建历史记录，按时间倒序排列。

#### Scenario: 查询指定流程的构建历史
- **WHEN** 用户请求 GET /api/build-pipelines/{id}/records
- **THEN** 系统返回该流程的所有 BuildRecord 列表，按 startedAt 倒序

### Requirement: 查询单条构建记录详情

系统 SHALL 支持按 ID 查询单条构建记录的完整信息（包含日志）。

#### Scenario: 查询构建记录详情
- **WHEN** 用户请求 GET /api/build-records/{id}
- **THEN** 系统返回该 BuildRecord 的完整信息，包含 logs 列表

#### Scenario: 查询不存在的记录
- **WHEN** 用户请求不存在的构建记录 ID
- **THEN** 系统返回 404 错误

### Requirement: 限制构建历史数量

系统 SHALL 在每次构建执行完成并保存 BuildRecord 后，按 `buildPipelineId`
维度对该构建流程的历史记录数量进行约束：按 `startedAt` 时间倒序，仅保留
最新的 10 条，删除更旧的记录。成功与失败的构建记录 SHALL 同等纳入计数与
清理，不同构建流程之间的历史计数与清理 SHALL 相互独立。

#### Scenario: 历史数量未超过上限

- **WHEN** 某构建流程完成第 N 次构建（N ≤ 10）并保存 BuildRecord
- **THEN** 系统保留该流程全部 N 条历史记录，不删除任何记录

#### Scenario: 历史数量超过上限

- **WHEN** 某构建流程完成构建并保存记录后，该流程历史总数为 11 条
- **THEN** 系统按 `startedAt` 倒序保留最新 10 条，删除 `startedAt` 最早
  的那条记录，使历史总数收敛为 10

#### Scenario: 失败构建同样触发清理

- **WHEN** 某构建流程执行失败并保存一条 `status=FAILED` 的记录后，该流程
  历史总数超过 10 条
- **THEN** 系统按 `startedAt` 倒序保留最新 10 条并删除超量记录，失败记录
  与成功记录同等对待

#### Scenario: 不同构建流程独立约束

- **WHEN** 构建流程 A 与构建流程 B 各自累积超过 10 条历史，且仅 A 完成
  一次构建
- **THEN** 系统仅将 A 的历史清理至 10 条，B 的历史记录不受影响

#### Scenario: 已删除记录不可再查询

- **WHEN** 某条 BuildRecord 因超量被删除
- **THEN** 该记录从持久层移除，后续 `findById` 返回不存在，按流程查询
  历史列表不再包含该记录

