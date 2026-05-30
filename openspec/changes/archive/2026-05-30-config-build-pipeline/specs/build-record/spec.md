## ADDED Requirements

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
