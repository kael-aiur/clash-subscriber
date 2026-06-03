## ADDED Requirements

### Requirement: 获取 Mihomo 当前配置

系统 SHALL 通过 Mihomo REST API 获取实例当前运行中的配置。

#### Scenario: 成功获取配置
- **WHEN** 系统调用 Mihomo 的 GET /configs API
- **THEN** 系统返回完整的 YAML 配置内容

#### Scenario: 获取配置失败
- **WHEN** Mihomo 实例离线或 API 返回错误
- **THEN** 系统返回明确的错误信息

---

### Requirement: 规则解析

系统 SHALL 解析 Clash/Mihomo 配置中的路由规则。

#### Scenario: 解析规则列表
- **WHEN** 系统获取到配置的 rules 字段
- **THEN** 系统按顺序解析每条规则，提取规则类型、匹配值、目标代理组

#### Scenario: 支持的规则类型
- **WHEN** 系统解析规则
- **THEN** 系统支持 DOMAIN、DOMAIN-SUFFIX、DOMAIN-KEYWORD、IP-CIDR、GEOIP、MATCH 类型

---

### Requirement: 代理组解析

系统 SHALL 解析配置中的代理组及其嵌套关系。

#### Scenario: 解析代理组
- **WHEN** 系统获取到配置的 proxy-groups 字段
- **THEN** 系统解析每个代理组的名称、类型（select/url-test/fallback/load-balance）、包含的代理节点或子代理组

#### Scenario: 识别代理节点和子代理组
- **WHEN** 代理组的 proxies 列表包含其他代理组名称
- **THEN** 系统识别为子代理组引用，而非代理节点

---

### Requirement: 域名规则匹配

系统 SHALL 根据用户输入的域名匹配对应的路由规则。

#### Scenario: 精确域名匹配
- **WHEN** 用户输入的域名与 DOMAIN 规则完全匹配
- **THEN** 系统返回该规则及其目标代理组

#### Scenario: 域名后缀匹配
- **WHEN** 用户输入的域名以 DOMAIN-SUFFIX 规则的值结尾
- **THEN** 系统返回该规则及其目标代理组

#### Scenario: 域名关键词匹配
- **WHEN** 用户输入的域名包含 DOMAIN-KEYWORD 规则的值
- **THEN** 系统返回该规则及其目标代理组

#### Scenario: 兜底规则匹配
- **WHEN** 用户输入的域名不匹配任何具体规则
- **THEN** 系统返回 MATCH 兜底规则及其目标代理组

#### Scenario: 规则优先级
- **WHEN** 多条规则都能匹配用户输入的域名
- **THEN** 系统返回配置中位置最靠前的规则（优先级最高）

---

### Requirement: 流程图数据构建

系统 SHALL 构建 Vue Flow 格式的流程图数据结构。

#### Scenario: 构建节点
- **WHEN** 系统完成域名匹配和代理组解析
- **THEN** 系统构建包含域名节点、规则节点、代理组节点、代理节点、出口节点的节点列表

#### Scenario: 构建边
- **WHEN** 系统构建完节点
- **THEN** 系统构建连接各节点的边，形成从域名到最终出口的有向路径

#### Scenario: 分支路径
- **WHEN** 代理组包含多个可选节点（如 Select 类型）
- **THEN** 系统为每个可选节点创建分支边

#### Scenario: 嵌套代理组
- **WHEN** 代理组包含子代理组引用
- **THEN** 系统递归构建子代理组的节点和边

#### Scenario: 特殊出口节点
- **WHEN** 规则或代理组指向 DIRECT 或 REJECT
- **THEN** 系统创建对应的特殊出口节点

---

### Requirement: 转发路径查询 API

系统 SHALL 提供转发路径查询的 REST API 端点。

#### Scenario: 查询转发路径
- **WHEN** 客户端发送 GET /api/mihomo-instances/{id}/forwarding-path?domain=xxx
- **THEN** 系统返回该域名的转发路径数据，格式为 Vue Flow 的 { nodes, edges } 结构

#### Scenario: 参数校验
- **WHEN** 请求缺少 domain 参数或实例 ID 不存在
- **THEN** 系统返回 400 或 404 错误响应
