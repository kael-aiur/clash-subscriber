## ADDED Requirements

### Requirement: 构建流程配置类型选择
构建流程 SHALL 支持两种配置类型：订阅源模式（subscription）和配置组合模式（config-profile）。

#### Scenario: 创建订阅源模式构建流程
- **WHEN** 用户创建构建流程，指定 configType 为 "subscription"
- **THEN** 系统 SHALL 保存构建流程，并关联 primarySubscriptionId 和 additionalSubscriptionIds

#### Scenario: 创建配置组合模式构建流程
- **WHEN** 用户创建构建流程，指定 configType 为 "config-profile"
- **THEN** 系统 SHALL 保存构建流程，并关联 configProfileId

#### Scenario: 配置类型验证
- **WHEN** 用户创建构建流程，configType 为 "config-profile"，但 configProfileId 为空
- **THEN** 系统 SHALL 返回错误："配置组合不能为空"

#### Scenario: 订阅源模式验证
- **WHEN** 用户创建构建流程，configType 为 "subscription"，但 primarySubscriptionId 为空
- **THEN** 系统 SHALL 返回错误："主订阅不能为空"

### Requirement: 向后兼容性迁移
现有构建流程（configType 为 null）SHALL 自动迁移为订阅源模式（configType = "subscription"）。

#### Scenario: 查询现有构建流程
- **WHEN** 系统查询构建流程，configType 为 null
- **THEN** 系统 SHALL 自动将 configType 设置为 "subscription" 并保存

#### Scenario: 执行现有构建流程
- **WHEN** 系统执行构建流程，configType 为 null
- **THEN** 系统 SHALL 按订阅源模式执行，并自动迁移 configType 为 "subscription"

### Requirement: 配置组合模式字段验证
配置组合模式下，configProfileId SHALL 必填，primarySubscriptionId 和 additionalSubscriptionIds 可选。

#### Scenario: 配置组合模式字段验证
- **WHEN** 用户创建构建流程，configType 为 "config-profile"
- **THEN** 系统 SHALL 验证 configProfileId 不为空，primarySubscriptionId 和 additionalSubscriptionIds 可为空

#### Scenario: 订阅源模式字段验证
- **WHEN** 用户创建构建流程，configType 为 "subscription"
- **THEN** 系统 SHALL 验证 primarySubscriptionId 不为空，configProfileId 可为空
