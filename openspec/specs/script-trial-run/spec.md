# script-trial-run

## Purpose

TBD

## Requirements

### Requirement: 试运行 SHALL 支持选择订阅源

脚本编辑页面 SHALL 提供订阅源下拉选择框，列出所有已配置的订阅源供用户选择。

#### Scenario: 加载订阅源列表
- **WHEN** 用户打开脚本编辑对话框
- **THEN** 订阅源下拉框 SHALL 显示所有已配置的订阅源名称

#### Scenario: 未选择订阅源时禁用试运行
- **WHEN** 用户未选择任何订阅源
- **THEN** 试运行按钮 SHALL 处于禁用状态

---

### Requirement: 试运行 SHALL 使用编辑器当前内容执行

试运行 SHALL 使用 Monaco Editor 中当前的脚本内容（而非已保存的文件内容）发送到后端执行，允许用户测试未保存的修改。

#### Scenario: 试运行未保存的脚本
- **WHEN** 用户修改了编辑器中的脚本内容但未保存，然后点击"试运行"
- **THEN** SHALL 使用编辑器中当前的内容发送到后端执行，而非已保存的文件内容

---

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

---

### Requirement: 试运行结果 SHALL 在前端展示

前端 SHALL 展示试运行结果面板，成功时显示变更摘要，失败时显示错误信息。

#### Scenario: 展示成功结果
- **WHEN** 试运行成功返回
- **THEN** 结果面板 SHALL 显示变更摘要（各字段变化前后数量），并提供可折叠区域查看完整输出 config

#### Scenario: 展示失败结果
- **WHEN** 试运行失败返回
- **THEN** 结果面板 SHALL 以红色样式显示错误信息和堆栈跟踪

#### Scenario: 执行中的加载状态
- **WHEN** 试运行请求已发出但尚未返回
- **THEN** 试运行按钮 SHALL 显示加载状态，结果面板 SHALL 显示加载中提示
