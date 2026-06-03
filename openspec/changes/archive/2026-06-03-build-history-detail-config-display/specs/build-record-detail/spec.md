## MODIFIED Requirements

### Requirement: 构建流程的固定环节定义

系统 SHALL 将构建流程定义为 4 个固定环节：拉取主订阅、合并额外订阅、脚本处理、推送到 Mihomo。每个环节的 input/output SHALL 存储结构化数据对象，包含配置摘要和完整 YAML 快照。

#### Scenario: 标准构建流程环节
- **WHEN** 执行构建流程
- **THEN** 系统 SHALL 按顺序执行以下环节，并在每个环节记录丰富的 input/output 数据：
  1. 拉取主订阅配置
  2. 合并额外订阅节点
  3. 脚本处理（如有配置）
  4. 推送到目标 Mihomo 实例

#### Scenario: 无脚本时的环节处理
- **WHEN** 构建流程未配置 scriptName
- **THEN** 系统 SHALL 将"脚本处理"环节状态设为 SKIPPED

#### Scenario: 环节失败中断
- **WHEN** 某个环节执行失败
- **THEN** 系统 SHALL 记录该环节为 FAILED，后续环节设为 SKIPPED，整体构建状态为 FAILED

---

### Requirement: 记录构建步骤详情

系统 SHALL 在每次构建执行过程中记录每个环节的详细信息，包含环节名称、状态、开始时间、结束时间、结构化的输入输出数据、错误信息。输入输出数据 SHALL 包含配置摘要（节点数、代理组数、规则数、名称预览）和完整 YAML 快照。

#### Scenario: 构建步骤记录
- **WHEN** 构建执行完成（成功或失败）
- **THEN** 系统创建的 BuildRecord 中 SHALL 包含 steps 列表，每个 BuildStep 包含 name、status、startedAt、finishedAt、input（结构化对象）、output（结构化对象）、errorMessage 字段

#### Scenario: 步骤状态定义
- **WHEN** 构建步骤执行
- **THEN** 步骤 status SHALL 为以下之一：SUCCESS（成功）、FAILED（失败）、SKIPPED（跳过）

#### Scenario: 向后兼容
- **WHEN** 查询历史构建记录（旧格式的简单字符串 input/output）
- **THEN** 系统 SHALL 正常返回数据，前端以纯文本形式展示旧格式内容

#### Scenario: 拉取主订阅配置步骤的结构化数据
- **WHEN** 执行拉取主订阅配置步骤
- **THEN** 系统 SHALL 将 input 设置为 `{ "subscriptionName": "<订阅源名称>" }`，output 设置为 `{ "configSummary": { "nodeCount": N, "proxyGroupCount": N, "ruleCount": N, "nodeNames": [...], "proxyGroupNames": [...] }, "configYaml": "<完整YAML>" }`

#### Scenario: 合并额外订阅节点步骤的结构化数据
- **WHEN** 执行合并额外订阅节点步骤
- **THEN** 系统 SHALL 将 input 设置为包含主配置快照和各额外订阅配置快照的对象，output 设置为合并后的配置快照对象

#### Scenario: 脚本处理步骤的结构化数据
- **WHEN** 执行脚本处理步骤
- **THEN** 系统 SHALL 将 input 设置为脚本名称 + 合并后的配置快照，output 设置为脚本处理后的配置快照

#### Scenario: 推送到 Mihomo 步骤的结构化数据
- **WHEN** 执行推送到 Mihomo 步骤
- **THEN** 系统 SHALL 将 input 设置为实例名称 + 最终配置快照，output 设置为 `{ "success": true/false }`

---

### Requirement: 配置摘要提取

系统 SHALL 从 ClashConfig 对象中提取配置摘要信息，用于构建步骤的 input/output 展示。

#### Scenario: 提取配置摘要
- **WHEN** 构建步骤完成配置处理
- **THEN** 系统 SHALL 提取以下摘要信息：
  - nodeCount: 节点总数
  - proxyGroupCount: 代理组总数
  - ruleCount: 规则总数
  - nodeNames: 前 5 个节点名称列表
  - proxyGroupNames: 前 5 个代理组名称列表

#### Scenario: 配置转 YAML
- **WHEN** 需要保存配置快照
- **THEN** 系统 SHALL 将 ClashConfig 对象转换为完整的 YAML 字符串，确保字段同步（调用 syncRawFromFields）
