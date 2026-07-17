## Why

当一个配置组合引用多个订阅源时，不同订阅源可能存在同名的「伪节点」（如 `套餐到期：长期有效`、`剩余流量：100GB` 等流量/到期信息伪装成的节点）。当前 `ConfigGeneratorServiceImpl` 在合并阶段对每个订阅源的节点全量 `addAll`，且 `toYaml` 会写出全部合并节点，导致最终配置的 `proxies` 列表出现重名节点，推送到 Mihomo 时被拒绝：

`HTTP 400 - {"message":"proxy 套餐到期：长期有效 is the duplicate name"}`

用户需要一种手段，在合并之前按订阅源维度过滤掉这类不需要的节点，让最终生成的节点表只保留期望的节点。

## What Changes

**订阅源节点采纳规则**
- From: 配置组合仅存储 `subscriptionIds`（订阅源 ID 列表），合并时全量采纳每个订阅源的全部节点
- To: 配置组合为每个选中的订阅源存储一份「节点采纳规则」，合并前按规则过滤节点
- Reason: 让用户能按订阅源剔除伪节点/垃圾节点，从源头消除重名冲突
- Impact: 修改 ConfigProfile 数据模型与编辑界面、修改配置生成流程；老数据自动兼容

**节点采纳规则模式**
- From: 无
- To: 支持两种模式——「全部节点 + 排除关键词」与「关键词匹配」（可叠加排除关键词）
- Reason: 覆盖最常见的「剔除到期/流量伪节点」与「只保留指定地区节点」两类场景
- Impact: 本期不支持「手动选择」模式（需额外的订阅源节点预览接口，留待后续）

## Capabilities

### Modified Capabilities

- `config-profile-management`: 配置组合新增「订阅源节点采纳规则」，用户可为每个选中的订阅源分别配置采纳模式与关键词
- `config-generation`: 合并订阅源节点前，按每个订阅源的采纳规则过滤节点，仅命中规则的节点进入最终 `proxies` 列表

## Impact

**代码影响**
- module-processor：`ConfigProfile` 模型新增 `subscriptionRefs`（含 `nodePolicy`）；新增共用的节点过滤器；`ConfigGeneratorServiceImpl` 在 fetch 后按规则过滤
- module-web 前端：`ConfigProfileEditView` 的「订阅源选择」卡片扩展为「订阅源 + 采纳规则」；`api/config-profile.ts` 类型同步

**兼容性影响**
- 已有 ConfigProfile 数据（`subscriptionIds`）在读取时自动映射为 `subscriptionRefs`，默认规则为「全部节点、排除关键词为空」，行为与现状完全一致

**边界（本期不解决的场景）**
- 本期不引入跨订阅源去重。两个订阅源中同名的好节点（如各自的「香港 01」）仍可能产生重名冲突；单订阅源内部的重名同样不在本期范围内。本期目标聚焦于通过采纳规则剔除伪节点
