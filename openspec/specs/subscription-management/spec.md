## ADDED Requirements

### Requirement: 订阅源 CRUD

系统 SHALL 提供订阅源的完整 CRUD 操作（创建、读取、更新、删除）。

#### Scenario: 创建订阅源
- **WHEN** 用户提交订阅源信息（名称、URL、User-Agent、自定义请求头）
- **THEN** 系统生成唯一 ID，保存订阅源信息，返回创建结果

#### Scenario: 查询订阅源列表
- **WHEN** 用户请求订阅源列表
- **THEN** 系统返回所有订阅源的基本信息（ID、名称、URL、最后获取时间）

#### Scenario: 更新订阅源
- **WHEN** 用户提交订阅源 ID 和更新内容
- **THEN** 系统更新对应订阅源的信息，保留未修改的字段

#### Scenario: 删除订阅源
- **WHEN** 用户提交订阅源 ID 请求删除
- **THEN** 系统删除该订阅源及其缓存数据

---

### Requirement: 订阅获取与解析

系统 SHALL 支持从订阅链接获取并解析 Clash 配置。

#### Scenario: 获取完整 Clash 配置
- **WHEN** 用户请求获取某个订阅源，该订阅源配置了 User-Agent
- **THEN** 系统使用配置的 User-Agent 和自定义请求头发送 HTTP 请求，解析返回的 YAML 配置为 ClashConfig 对象

#### Scenario: 解析 Base64 订阅内容
- **WHEN** 用户请求获取某个订阅源，返回内容为 Base64 编码
- **THEN** 系统解码 Base64 内容，按行解析为 ProxyNode 列表，组装为 ClashConfig 对象

#### Scenario: 自动检测响应格式
- **WHEN** 系统收到订阅响应
- **THEN** 系统自动检测响应格式（YAML 或 Base64），使用对应的解析策略

---

### Requirement: 订阅缓存

系统 SHALL 缓存最近获取的订阅配置。

#### Scenario: 缓存订阅结果
- **WHEN** 系统成功获取并解析订阅配置
- **THEN** 系统将结果缓存到 data/cache/{id}.yaml，更新 lastFetchedAt 时间戳

#### Scenario: 使用缓存数据
- **WHEN** 用户请求获取订阅但网络不可用
- **THEN** 系统返回缓存的配置数据（如果存在）
