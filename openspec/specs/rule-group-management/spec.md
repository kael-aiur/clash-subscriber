## Purpose

管理从订阅配置中提取的规则组，支持手动创建、编辑和结构化展示规则与代理对象。

---

## ADDED Requirements

### Requirement: 规则组 CRUD

系统 SHALL 提供规则组的完整 CRUD 操作（创建、读取、更新、删除）。

#### Scenario: 手动创建规则组
- **WHEN** 用户提交规则组信息（名称、描述、规则列表、代理对象列表）
- **THEN** 系统生成唯一 ID，保存规则组，sourceSubscriptionId 为 null，返回创建结果

#### Scenario: 查询规则组列表
- **WHEN** 用户请求规则组列表
- **THEN** 系统返回所有规则组的基本信息（ID、名称、来源订阅 ID、规则数量、代理对象数量、创建时间）

#### Scenario: 查询规则组详情
- **WHEN** 用户请求某个规则组的详情
- **THEN** 系统返回规则组的完整信息，包括规则列表和代理对象列表

#### Scenario: 更新规则组
- **WHEN** 用户提交规则组 ID 和更新内容（名称、描述、规则列表、代理对象列表）
- **THEN** 系统更新对应规则组的信息，更新 updatedAt 时间戳

#### Scenario: 删除规则组
- **WHEN** 用户提交规则组 ID 请求删除
- **THEN** 系统删除该规则组的持久化文件

---

### Requirement: 代理对象管理

系统 SHALL 管理规则组中的代理对象，每个代理对象包含唯一 ID、源名称和可选描述。

#### Scenario: 自动生成代理对象 ID
- **WHEN** 系统创建代理对象（无论是提取还是手动创建）
- **THEN** 系统使用 IdGenerator 自动生成唯一 ID

#### Scenario: 编辑代理对象属性
- **WHEN** 用户修改代理对象的源名称或描述
- **THEN** 系统更新代理对象属性，代理对象 ID 不可修改

#### Scenario: 删除代理对象
- **WHEN** 用户删除某个代理对象
- **THEN** 系统从规则组中移除该代理对象，规则中引用该代理对象的占位符保持不变（用户需自行处理引用）

---

### Requirement: 从订阅提取规则组

系统 SHALL 支持从已获取的订阅配置中自动提取规则组。

#### Scenario: 首次提取规则组
- **WHEN** 用户对某个尚未提取规则组的订阅触发提取操作
- **THEN** 系统执行以下步骤：
  1. 获取订阅的最新 ClashConfig（使用缓存或重新获取）
  2. 扫描所有规则，提取第 3 个字段作为代理名
  3. 过滤掉内置代理名（DIRECT、REJECT、PASS）
  4. 为每个唯一代理名生成代理对象（自动生成 ID，源名称为原始代理名）
  5. 将规则中的代理名替换为 `{{代理对象ID}}` 占位符
  6. 创建规则组，name 为"{订阅名称}的规则组"，sourceSubscriptionId 为订阅 ID
  7. 保存并返回规则组

#### Scenario: 重新提取规则组（完全覆盖）
- **WHEN** 用户对某个已提取过规则组的订阅再次触发提取操作
- **THEN** 系统完全重新生成规则组，覆盖现有规则组的所有内容（规则、代理对象、名称），保留原规则组 ID

#### Scenario: 提取时订阅无规则
- **WHEN** 用户触发提取操作，但订阅配置中没有规则
- **THEN** 系统返回错误提示，说明订阅中没有可提取的规则

---

### Requirement: 规则结构化展示

系统 SHALL 将规则字符串解析为结构化数据进行展示。

#### Scenario: 解析标准三段式规则
- **WHEN** 系统解析规则字符串 `DOMAIN-SUFFIX,google.com,{{px-001}}`
- **THEN** 系统将其解析为：类型=`DOMAIN-SUFFIX`，参数=`google.com`，代理对象引用=`px-001`

#### Scenario: 解析两段式规则（无参数）
- **WHEN** 系统解析规则字符串 `MATCH,{{px-003}}`
- **THEN** 系统将其解析为：类型=`MATCH`，参数=空，代理对象引用=`px-003`

#### Scenario: 解析引用内置名的规则
- **WHEN** 系统解析规则字符串 `DOMAIN-SUFFIX,bilibili.com,DIRECT`
- **THEN** 系统将其解析为：类型=`DOMAIN-SUFFIX`，参数=`bilibili.com`，代理名=`DIRECT`（非占位符引用）

#### Scenario: 展示代理对象引用
- **WHEN** 前端展示规则列表中的代理对象列
- **THEN** 系统同时显示代理对象的源名称和 ID（如"美国节点 (px-001)"），内置名直接显示名称

---

### Requirement: 规则组数据持久化

系统 SHALL 使用 JSON 文件持久化规则组数据，遵循项目统一的 Repository 模式。

#### Scenario: 保存规则组
- **WHEN** 系统保存规则组
- **THEN** 系统将规则组序列化为 JSON 文件，存储到 `data/rule-groups/{id}.json`

#### Scenario: 加载规则组列表
- **WHEN** 系统加载规则组列表
- **THEN** 系统读取 `data/rule-groups/` 目录下所有 JSON 文件，反序列化为 RuleGroup 对象列表