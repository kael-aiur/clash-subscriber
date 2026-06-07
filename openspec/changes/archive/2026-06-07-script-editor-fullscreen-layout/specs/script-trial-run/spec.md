## MODIFIED Requirements

### Requirement: 试运行 API

系统 SHALL 提供试运行 API，支持分步执行：先获取订阅配置，再执行脚本。

#### Scenario: 获取订阅配置
- **WHEN** 客户端调用 `POST /api/scripts/preview-subscription`，请求体包含 `subscriptionId`
- **THEN** 系统获取该订阅源的完整配置，返回配置摘要和 YAML 内容

#### Scenario: 执行脚本
- **WHEN** 客户端调用 `POST /api/scripts/try-run`，请求体包含 `scriptContent` 和 `subscriptionId`
- **THEN** 系统获取订阅配置、执行脚本、返回执行结果、变更摘要、输入/输出配置摘要

#### Scenario: 脚本执行失败
- **WHEN** 脚本执行过程中抛出异常
- **THEN** API 返回 `success: false` 和错误信息，不返回配置摘要

---

### Requirement: preview-subscription 端点

`POST /api/scripts/preview-subscription` 端点 SHALL 返回订阅源的配置摘要。

#### Scenario: 成功获取配置
- **WHEN** 请求包含有效的 `subscriptionId`
- **THEN** 返回 `{ config, summary: { nodeCount, proxyGroupCount, ruleCount, nodeNames, proxyGroupNames }, yaml }`

#### Scenario: 订阅源不存在
- **WHEN** 请求包含无效的 `subscriptionId`
- **THEN** 返回 404 错误

---

### Requirement: try-run 端点返回格式

`POST /api/scripts/try-run` 端点 SHALL 返回包含输入/输出配置摘要的完整结果。

#### Scenario: 成功执行
- **WHEN** 脚本执行成功
- **THEN** 返回 `{ success: true, summary: { proxiesBefore, proxiesAfter, groupsBefore, groupsAfter, rulesBefore, rulesAfter }, config, inputSummary, inputYaml, outputSummary, outputYaml }`

#### Scenario: 执行失败
- **WHEN** 脚本执行失败
- **THEN** 返回 `{ success: false, error: "错误信息" }`
