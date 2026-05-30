## ADDED Requirements

### Requirement: 获取实例当前配置

系统 SHALL 支持通过 Mihomo REST API 获取实例当前运行中的配置。

#### Scenario: 获取配置成功
- **WHEN** 系统调用 Mihomo 的 GET /configs API
- **THEN** 系统返回完整的 YAML 配置内容

#### Scenario: 获取配置失败
- **WHEN** Mihomo 实例离线或 API 返回错误
- **THEN** 系统返回明确的错误信息
