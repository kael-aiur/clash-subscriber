## ADDED Requirements

### Requirement: Mihomo 实例详情页

系统 SHALL 提供 Mihomo 实例的详情管理页面。

#### Scenario: 详情页访问
- **WHEN** 用户访问 `/mihomo-instances/:id` 路由
- **THEN** 系统展示该实例的详情页面，包含实例信息、转发规则、推送历史三个标签页

#### Scenario: 从列表页跳转
- **WHEN** 用户在实例列表页点击实例名称
- **THEN** 系统跳转到该实例的详情页

---

## MODIFIED Requirements

### Requirement: Mihomo 实例管理页面

系统 SHALL 提供 Mihomo 实例的 Web 管理界面。

#### Scenario: 实例列表展示
- **WHEN** 用户访问实例管理页面
- **THEN** 展示所有实例的列表，包括名称、URL、健康状态、最后检查时间，实例名称可点击跳转到详情页

#### Scenario: 一键推送配置
- **WHEN** 用户点击推送按钮
- **THEN** 选择 Pipeline 和目标实例，执行推送并展示结果
