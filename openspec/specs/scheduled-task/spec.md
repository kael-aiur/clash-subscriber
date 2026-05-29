## ADDED Requirements

### Requirement: 定时任务管理

系统 SHALL 提供定时任务的创建、更新、删除和查询功能。

#### Scenario: 创建定时任务
- **WHEN** 用户提交任务信息（名称、Pipeline ID、目标实例、cron 表达式）
- **THEN** 系统生成唯一 ID，保存任务配置，初始状态为启用

#### Scenario: 启用/禁用任务
- **WHEN** 用户请求启用或禁用某个任务
- **THEN** 系统更新任务的启用状态，启用时注册 cron 调度，禁用时取消调度

#### Scenario: 删除任务
- **WHEN** 用户提交任务 ID 请求删除
- **THEN** 系统取消该任务的 cron 调度，删除任务信息

---

### Requirement: 定时执行

系统 SHALL 按 cron 表达式定时执行任务。

#### Scenario: 定时触发执行
- **WHEN** 到达任务的 cron 触发时间
- **THEN** 系统自动执行任务：获取订阅 → 执行 Pipeline → 推送到目标实例

#### Scenario: 执行结果记录
- **WHEN** 任务执行完成（成功或失败）
- **THEN** 系统更新 lastRunAt 和 lastRunStatus，记录执行日志

---

### Requirement: 手动触发

系统 SHALL 支持手动触发任务立即执行。

#### Scenario: 手动触发任务
- **WHEN** 用户请求手动触发某个任务
- **THEN** 系统立即执行该任务，不等待 cron 触发
