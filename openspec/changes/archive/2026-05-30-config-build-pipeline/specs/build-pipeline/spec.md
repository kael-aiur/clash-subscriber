## ADDED Requirements

### Requirement: 创建构建流程

系统 SHALL 支持创建构建流程，包含名称、主订阅 ID、额外订阅 ID 列表（可选）、脚本名称（可选）、目标 mihomo 实例 ID、cron 表达式（可选）、启用状态。

#### Scenario: 创建完整构建流程
- **WHEN** 用户提交包含 name、primarySubscriptionId、targetInstanceId 的构建流程
- **THEN** 系统创建 BuildPipeline 记录，生成唯一 ID，设置 createdAt 和 updatedAt 时间戳

#### Scenario: 创建带定时的构建流程
- **WHEN** 用户提交包含 cronExpression（如 "0 2 * * *"）且 enabled=true 的构建流程
- **THEN** 系统创建记录并注册对应的 cron 定时任务

### Requirement: 编辑构建流程

系统 SHALL 支持修改已创建的构建流程的所有字段。

#### Scenario: 修改构建流程配置
- **WHEN** 用户更新构建流程的订阅、脚本或目标实例
- **THEN** 系统更新记录，刷新 updatedAt 时间戳

#### Scenario: 修改定时配置
- **WHEN** 用户修改 cronExpression 或切换 enabled 状态
- **THEN** 系统同步更新对应的 cron 任务（注册、更新或取消）

### Requirement: 删除构建流程

系统 SHALL 支持删除构建流程，删除时同步取消关联的 cron 任务。

#### Scenario: 删除构建流程
- **WHEN** 用户删除一个构建流程
- **THEN** 系统删除记录、取消 cron 任务、返回 204

### Requirement: 查询构建流程

系统 SHALL 支持查询所有构建流程列表和按 ID 查询单个构建流程详情。

#### Scenario: 查询所有构建流程
- **WHEN** 用户请求 GET /api/build-pipelines
- **THEN** 系统返回所有 BuildPipeline 列表

#### Scenario: 查询单个构建流程
- **WHEN** 用户请求 GET /api/build-pipelines/{id}
- **THEN** 系统返回对应 BuildPipeline；若不存在返回 404

### Requirement: 手动触发构建

系统 SHALL 支持手动触发指定构建流程的执行。

#### Scenario: 手动触发构建成功
- **WHEN** 用户请求 POST /api/build-pipelines/{id}/execute
- **THEN** 系统执行完整构建链路（拉取订阅 → 脚本处理 → 推送 mihomo），返回 BuildRecord

#### Scenario: 触发不存在的流程
- **WHEN** 用户触发不存在的构建流程
- **THEN** 系统返回 404 错误

### Requirement: 定时触发构建

系统 SHALL 在 cron 时间到达时自动执行对应的构建流程。

#### Scenario: 定时触发执行
- **WHEN** cron 时间到达且构建流程 enabled=true
- **THEN** 系统自动执行构建链路并记录 BuildRecord

### Requirement: 构建流程执行链路

系统 SHALL 按以下顺序执行构建：拉取主订阅配置 → 合并额外订阅节点 → 执行脚本处理（如有）→ 推送到目标 mihomo 实例。

#### Scenario: 有脚本的构建执行
- **WHEN** 构建流程配置了 scriptName
- **THEN** 系统生成包含 ScriptProcessor 步骤的 PipelineConfig 并执行

#### Scenario: 无脚本的构建执行
- **WHEN** 构建流程未配置 scriptName
- **THEN** 系统跳过脚本处理，直接将拉取的配置推送到目标实例

#### Scenario: 多订阅合并
- **WHEN** 构建流程配置了 additionalSubscriptionIds
- **THEN** 系统拉取所有订阅并将 ProxyNode 合并到同一个 ClashConfig
