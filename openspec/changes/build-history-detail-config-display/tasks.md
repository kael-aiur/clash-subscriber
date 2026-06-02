## 1. 后端辅助方法

- [x] 1.1 在 BuildPipelineServiceImpl 中新增 `buildConfigSummary(ClashConfig)` 方法，提取 nodeCount、proxyGroupCount、ruleCount、nodeNames（前5个）、proxyGroupNames（前5个）
- [x] 1.2 在 BuildPipelineServiceImpl 中新增 `configToYaml(ClashConfig)` 方法，调用 syncRawFromFields 后转为 YAML 字符串

## 2. 后端构建步骤数据丰富化

- [x] 2.1 修改"拉取主订阅配置"步骤：input 改为 `{ subscriptionName }`，output 改为 `{ configSummary, configYaml }`
- [x] 2.2 修改"合并额外订阅节点"步骤：input 改为主配置快照 + 各额外订阅配置快照，output 改为合并后的配置快照
- [x] 2.3 修改"脚本处理"步骤：input 改为脚本名称 + 合并后配置快照，output 改为处理后配置快照
- [x] 2.4 修改"推送到 Mihomo"步骤：input 改为实例名称 + 最终配置快照，output 改为 `{ success: true/false }`

## 3. 前端 ConfigCard 组件

- [x] 3.1 新建 ConfigCard.vue 组件，展示节点数/代理组数/规则数摘要
- [x] 3.2 实现节点名称和代理组名称的预览展示（最多5个，逗号分隔，显示总数）
- [x] 3.3 实现展开/收起功能，展开后显示完整 YAML 内容
- [x] 3.4 处理 expandable=false 或 yamlContent 为空的情况

## 4. 前端 BuildRecordDetailView 改造

- [x] 4.1 修改步骤详情区域，使用 ConfigCard 替代 formatJson() 展示配置相关数据
- [x] 4.2 步骤输入中的 subscriptionName、instanceName、scriptName 显示为文本标签
- [x] 4.3 处理旧格式数据（input/output 为简单字符串时以纯文本展示）
