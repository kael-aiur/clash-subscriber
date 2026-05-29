## ADDED Requirements

### Requirement: 实例注册管理

系统 SHALL 提供 Mihomo 实例的注册、更新、删除和查询功能。

#### Scenario: 注册 Mihomo 实例
- **WHEN** 用户提交实例信息（名称、API URL、密钥）
- **THEN** 系统生成唯一 ID，保存实例信息，初始状态为 UNKNOWN

#### Scenario: 查询实例列表
- **WHEN** 用户请求实例列表
- **THEN** 系统返回所有实例的基本信息和健康状态

#### Scenario: 删除实例
- **WHEN** 用户提交实例 ID 请求删除
- **THEN** 系统删除该实例的所有信息

---

### Requirement: 健康检查

系统 SHALL 支持对 Mihomo 实例进行健康检查。

#### Scenario: 单实例健康检查
- **WHEN** 用户请求检查某个实例的健康状态
- **THEN** 系统调用实例的 GET /version API，根据响应更新状态为 HEALTHY 或 UNHEALTHY

#### Scenario: 批量健康检查
- **WHEN** 用户请求检查所有实例的健康状态
- **THEN** 系统并发调用所有启用实例的健康检查 API，返回汇总结果

---

### Requirement: 配置推送

系统 SHALL 支持将 ClashConfig 推送到 Mihomo 实例。

#### Scenario: 推送到单个实例
- **WHEN** 用户请求将配置推送到指定实例
- **THEN** 系统通过 PUT /configs API 将 YAML 配置推送到该实例，返回推送结果

#### Scenario: 推送到所有实例
- **WHEN** 用户请求将配置推送到所有启用的实例
- **THEN** 系统并发推送到所有启用实例，返回每个实例的推送结果

#### Scenario: 推送失败处理
- **WHEN** 配置推送过程中某个实例返回错误
- **THEN** 系统记录错误信息，继续推送其他实例，最终返回汇总结果
