## ADDED Requirements

### Requirement: 处理器接口

系统 SHALL 定义统一的处理器接口（ConfigProcessor），支持处理器链模式。

#### Scenario: 处理器执行顺序
- **WHEN** Pipeline 包含多个处理器
- **THEN** 系统按处理器的 order 属性从小到大依次执行

#### Scenario: 处理器上下文传递
- **WHEN** 处理器链执行过程中
- **THEN** 每个处理器可访问 ProcessingContext 中的共享变量和日志

---

### Requirement: 内置处理器

系统 SHALL 提供以下内置处理器：节点合并、规则修改、代理组重组、去重。

#### Scenario: 合并多源节点
- **WHEN** NodeMergeProcessor 收到多个订阅源的 ClashConfig
- **THEN** 将所有源的 proxies 列表合并到一个 ClashConfig 中

#### Scenario: 修改出站规则
- **WHEN** RuleModifyProcessor 收到规则修改指令（添加、删除、替换）
- **THEN** 按指令修改 ClashConfig 的 rules 列表

#### Scenario: 重组代理组
- **WHEN** ProxyGroupProcessor 收到分组策略（如按地区分组）
- **THEN** 根据策略重新组织 proxyGroups

#### Scenario: 去除重复节点
- **WHEN** DuplicateRemoveProcessor 收到包含重复节点的配置
- **THEN** 按节点名称和服务器地址去重，保留第一个出现的节点

---

### Requirement: 脚本处理器

系统 SHALL 支持通过 GraalVM JS 脚本扩展处理逻辑。

#### Scenario: 执行用户脚本
- **WHEN** ScriptProcessor 收到脚本名称和输入 ClashConfig
- **THEN** 加载 data/scripts/ 目录下的对应脚本，在 GraalVM JS 沙箱中执行，返回处理后的 ClashConfig

#### Scenario: 脚本访问 Java 对象
- **WHEN** 用户脚本执行过程中
- **THEN** 脚本可直接访问和修改 ClashConfig、ProxyNode 等 Java 对象

---

### Requirement: Pipeline 配置

系统 SHALL 支持通过 JSON 配置定义 Pipeline 的执行步骤。

#### Scenario: 创建 Pipeline
- **WHEN** 用户提交 Pipeline 配置（名称、处理器列表及参数）
- **THEN** 系统保存配置，生成唯一 ID

#### Scenario: 执行 Pipeline
- **WHEN** 用户请求执行某个 Pipeline，传入输入 ClashConfig
- **THEN** 系统按配置的步骤依次调用处理器，返回最终处理结果
