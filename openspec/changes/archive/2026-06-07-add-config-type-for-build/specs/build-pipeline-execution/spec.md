## MODIFIED Requirements

### Requirement: 构建流程执行逻辑
构建流程执行 SHALL 根据 configType 选择不同的配置来源。

#### Scenario: 订阅源模式执行
- **WHEN** 系统执行构建流程，configType 为 "subscription" 或 null
- **THEN** 系统 SHALL 按订阅源模式执行：拉取主订阅配置 → 合并额外订阅节点 → 脚本处理 → 推送到 Mihomo

#### Scenario: 配置组合模式执行
- **WHEN** 系统执行构建流程，configType 为 "config-profile"
- **THEN** 系统 SHALL 按配置组合模式执行：获取配置组合并生成完整配置 → 脚本处理 → 推送到 Mihomo

#### Scenario: 配置组合模式配置生成
- **WHEN** 系统执行配置组合模式构建流程
- **THEN** 系统 SHALL 调用 ConfigGeneratorService.generate(profile) 生成完整配置，每次构建都重新生成，不使用缓存

### Requirement: 构建流程执行步骤
构建流程执行 SHALL 包含以下步骤，根据配置类型选择执行。

#### Scenario: 订阅源模式步骤
- **WHEN** 系统执行订阅源模式构建流程
- **THEN** 系统 SHALL 执行以下步骤：
  1. 拉取主订阅配置
  2. 合并额外订阅节点
  3. 脚本处理（可选）
  4. 推送到 Mihomo

#### Scenario: 配置组合模式步骤
- **WHEN** 系统执行配置组合模式构建流程
- **THEN** 系统 SHALL 执行以下步骤：
  1. 获取配置组合并生成完整配置
  2. 脚本处理（可选）
  3. 推送到 Mihomo

#### Scenario: 脚本处理支持
- **WHEN** 系统执行构建流程，scriptName 不为空
- **THEN** 系统 SHALL 执行脚本处理，无论配置类型是订阅源模式还是配置组合模式

### Requirement: 构建流程执行记录
构建流程执行 SHALL 记录详细的执行步骤和日志。

#### Scenario: 执行步骤记录
- **WHEN** 系统执行构建流程
- **THEN** 系统 SHALL 记录每个步骤的名称、状态、输入、输出和耗时

#### Scenario: 执行日志记录
- **WHEN** 系统执行构建流程
- **THEN** 系统 SHALL 记录执行过程中的日志，包括成功、警告和错误信息

#### Scenario: 执行状态更新
- **WHEN** 系统执行构建流程完成
- **THEN** 系统 SHALL 更新构建流程的 lastRunAt 和 lastRunStatus 字段
