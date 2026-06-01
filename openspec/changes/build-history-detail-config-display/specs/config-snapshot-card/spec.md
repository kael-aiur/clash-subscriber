## ADDED Requirements

### Requirement: 配置快照卡片展示

系统 SHALL 提供 ConfigCard 组件，用于在构建历史详情页面中展示配置快照的摘要信息和完整 YAML 内容。

#### Scenario: 展示配置摘要
- **WHEN** ConfigCard 组件接收到 configSummary 数据
- **THEN** 组件 SHALL 展示以下摘要信息：
  - 节点数（nodeCount）
  - 代理组数（proxyGroupCount）
  - 规则数（ruleCount）
  - 节点名称预览（nodeNames，最多 5 个）
  - 代理组名称预览（proxyGroupNames，最多 5 个）

#### Scenario: 展示节点和代理组预览
- **WHEN** configSummary 包含 nodeNames 和 proxyGroupNames
- **THEN** 组件 SHALL 以逗号分隔的标签形式展示名称列表，并在末尾显示总数（如"共25个"）

#### Scenario: 展开查看完整 YAML
- **WHEN** ConfigCard 的 expandable 为 true 且 yamlContent 不为空
- **THEN** 组件 SHALL 显示展开/收起按钮，点击后展示完整的 YAML 内容

#### Scenario: 不可展开的卡片
- **WHEN** ConfigCard 的 expandable 为 false 或 yamlContent 为空
- **THEN** 组件 SHALL 不显示展开按钮，仅展示摘要信息

---

### Requirement: 配置快照卡片在构建步骤中的使用

系统 SHALL 在构建历史详情页面的每个步骤中，使用 ConfigCard 组件展示配置相关的输入输出数据。

#### Scenario: 步骤输入中展示配置卡片
- **WHEN** 步骤的 input 数据包含 configSummary 字段
- **THEN** 页面 SHALL 使用 ConfigCard 展示输入的配置信息

#### Scenario: 步骤输出中展示配置卡片
- **WHEN** 步骤的 output 数据包含 configSummary 字段
- **THEN** 页面 SHALL 使用 ConfigCard 展示输出的配置信息

#### Scenario: 旧数据兼容展示
- **WHEN** 步骤的 input/output 为简单字符串（旧格式数据）
- **THEN** 页面 SHALL 以纯文本形式展示，不使用 ConfigCard
