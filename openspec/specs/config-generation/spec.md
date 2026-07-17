## ADDED Requirements

### Requirement: 生成 Clash 配置

系统 SHALL 根据配置组合动态生成标准 Clash YAML 配置。

#### Scenario: 成功生成配置

- **WHEN** 系统收到配置生成请求（通过配置名称）
- **THEN** 系统根据配置组合生成完整的 Clash YAML 配置，包含 proxies、proxy-groups、rules 等字段

#### Scenario: 配置组合不存在

- **WHEN** 系统收到的配置名称不存在
- **THEN** 系统返回 404 Not Found 错误

---

### Requirement: 合并订阅源节点

系统 SHALL 能够合并多个订阅源的节点到一个配置中，并在合并前按每个订阅源的节点采纳规则过滤节点，使最终 `proxies` 列表仅包含命中规则的节点。

#### Scenario: 按采纳规则过滤后合并

- **WHEN** 配置组合选择了多个订阅源，且为各订阅源配置了节点采纳规则
- **THEN** 系统获取每个订阅源的节点，按其采纳规则过滤后，仅将命中规则的节点合并到配置的 proxies 列表中

#### Scenario: 默认采纳规则下的合并

- **WHEN** 配置组合中某订阅源未显式配置采纳规则
- **THEN** 系统按默认规则（全部节点、排除关键词为空）采纳该订阅源的全部节点参与合并

#### Scenario: 订阅源获取失败

- **WHEN** 某个订阅源获取失败
- **THEN** 系统记录错误日志，跳过该订阅源，继续处理其他订阅源

---

### Requirement: 构建代理组

系统 SHALL 根据代理组配置构建代理组。

#### Scenario: 标签匹配代理组

- **WHEN** 代理组配置为标签匹配方式，包含关键词列表
- **THEN** 系统匹配节点名称包含任一关键词的节点，构建代理组

#### Scenario: 直接选择代理组

- **WHEN** 代理组配置为直接选择方式，包含节点列表
- **THEN** 系统使用用户选择的节点构建代理组

#### Scenario: 全部节点代理组

- **WHEN** 代理组配置为全部节点方式
- **THEN** 系统使用所有节点构建代理组

---

### Requirement: 构建规则

系统 SHALL 根据规则组配置构建规则列表。

#### Scenario: 合并规则组

- **WHEN** 配置组合包含多个规则组
- **THEN** 系统按优先级顺序读取规则组，合并为最终的规则列表

#### Scenario: 规则组读取失败

- **WHEN** 某个规则组读取失败
- **THEN** 系统记录错误日志，跳过该规则组，继续处理其他规则组

---

### Requirement: 应用基础信息

系统 SHALL 将基础信息应用到生成的配置中。

#### Scenario: 应用端口配置

- **WHEN** 配置组合包含端口配置（mixedPort、port、socksPort 等）
- **THEN** 系统将这些配置写入生成的 Clash 配置中

#### Scenario: 应用认证配置

- **WHEN** 配置组合包含认证配置（external-controller、secret 等）
- **THEN** 系统将这些配置写入生成的 Clash 配置中

---

### Requirement: 输出 YAML 格式

系统 SHALL 将生成的配置转换为标准 YAML 格式。

#### Scenario: 生成 YAML 输出

- **WHEN** 系统完成配置构建
- **THEN** 系统将配置转换为 YAML 格式，返回给客户端

---

### Requirement: 使用现有处理器

系统 SHALL 复用现有的 NodeMergeProcessor、ProxyGroupProcessor 等处理器。

#### Scenario: 复用节点合并处理器

- **WHEN** 系统需要合并节点
- **THEN** 系统调用 NodeMergeProcessor 处理

#### Scenario: 复用代理组处理器

- **WHEN** 系统需要构建代理组
- **THEN** 系统调用 ProxyGroupProcessor 处理
