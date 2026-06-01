## Why

构建历史详情页面当前只展示简单的 ID 和描述文本（如订阅 ID、"节点数: 25"），用户无法直观看到每步处理的实际配置内容。这导致构建流程的可追溯性不足——用户需要对比不同步骤的配置变化时，只能通过 ID 推断，无法直接查看配置快照。改进后，用户可在任意步骤查看配置摘要（节点数、代理组数、规则数、节点名称预览）和完整 YAML，快速定位配置在哪个步骤发生了变化。

## What Changes

**构建步骤输入输出数据丰富化**
- From: 各步骤 input 存储 ID（订阅 ID、实例 ID），output 存储简单描述文本
- To: 各步骤 input 存储可读名称 + 配置摘要，output 存储配置摘要 + 完整 YAML 快照
- Reason: 用户需要看到实际的配置内容而非 ID
- Impact: 非破坏性变更，旧记录的简单字符串格式仍可正常展示

**前端配置卡片展示组件**
- From: 使用 `formatJson()` 直接展示 input/output 的 JSON
- To: 使用 ConfigCard 组件展示配置摘要（数字 + 名称预览），支持展开查看完整 YAML
- Reason: 结构化展示比原始 JSON 更直观
- Impact: 仅影响 BuildRecordDetailView 页面

## Capabilities

### New Capabilities
- `config-snapshot-card`: 配置快照卡片组件，展示节点数/代理组数/规则数摘要、节点和代理组名称预览、可展开的完整 YAML 内容

### Modified Capabilities
- `build-record-detail`: 构建步骤的 input/output 字段从简单字符串改为结构化对象，包含配置摘要和 YAML 快照

## Impact

- **后端**: `BuildPipelineServiceImpl.java` — 修改 `execute()` 方法中各步骤的 input/output 设置，新增 `buildConfigSummary()` 和 `configToYaml()` 辅助方法
- **前端**: `BuildRecordDetailView.vue` — 步骤详情展示改为使用 ConfigCard 组件
- **前端**: 新增 `ConfigCard.vue` 组件
- **API**: 无变更，BuildStep 的 input/output 仍为 Object 类型
- **存储**: 构建记录文件体积会增大（包含完整 YAML 快照）
