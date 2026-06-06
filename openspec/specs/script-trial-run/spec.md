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

### Requirement: 后端 SHALL 提供试运行 API

后端 SHALL 提供 `POST /api/scripts/try-run` 端点，接收脚本内容和订阅源 ID，获取订阅源配置后执行脚本并返回结果。

#### Scenario: 执行成功
- **WHEN** 用户选择订阅源并点击试运行，脚本执行成功
- **THEN** 响应 SHALL 包含 `success: true` 和变更摘要（proxies、proxy-groups、rules 变化前后数量）

#### Scenario: 执行失败
- **WHEN** 用户选择订阅源并点击试运行，脚本执行抛出异常
- **THEN** 响应 SHALL 包含 `success: false` 和错误信息（含堆栈跟踪）

#### Scenario: 获取订阅源配置失败
- **WHEN** 试运行时订阅源获取失败（网络错误、配置无效等）
- **THEN** 响应 SHALL 包含 `success: false` 和相应的错误描述

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
