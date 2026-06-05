## Why

当前 Mihomo 实例管理只有一个列表页面，用户无法深入查看单个实例的转发规则配置。当配置包含数百条规则和多层嵌套代理组时，用户很难直观理解某个域名的请求会经过怎样的转发路径。需要一个详情页面，通过流程图可视化展示域名到最终出口的完整转发链路，帮助用户快速理解和调试配置。

## What Changes

**Mihomo 实例详情页**
- From: 只有列表页面，无法查看单个实例详情
- To: 新增详情页面 `/mihomo-instances/:id`，包含三个标签页（实例信息、转发规则、推送历史）
- Reason: 用户需要深入了解单个实例的配置和转发规则
- Impact: 非破坏性变更，新增页面不影响现有功能

**转发规则流程图**
- From: 无法直观查看域名的转发路径
- To: 用户输入域名，系统从 Mihomo API 获取当前配置，解析规则匹配后用 Vue Flow 流程图展示完整转发路径（域名 → 规则 → 代理组 → 代理节点/出口），支持代理组展开/折叠和分支展示
- Reason: Clash 配置的规则和代理组嵌套关系复杂，纯文本难以理解
- Impact: 新增后端 API 端点 `GET /api/mihomo-instances/{id}/forwarding-path`，新增 Vue Flow 依赖

**推送历史查看**
- From: 无法查看某个实例的历史推送记录
- To: 在详情页的推送历史标签页中展示该实例的推送记录
- Reason: 帮助用户追溯配置变更历史
- Impact: 可能需要新增按实例查询推送记录的 API

## Capabilities

### New Capabilities
- `mihomo-instance-detail`: Mihomo 实例详情页面，包含实例信息展示/编辑、转发规则流程图查询、推送历史查看三个标签页
- `forwarding-path-parsing`: 转发路径解析引擎，从 Mihomo 获取配置、解析规则、匹配域名、构建 Vue Flow 格式的流程图数据

### Modified Capabilities
- `mihomo-instance`: 新增获取实例当前配置的 API 端点（从 Mihomo 代理获取）
- `web-ui`: Mihomo 实例列表页添加跳转到详情页的链接

## Impact

**后端变更：**
- `MihomoHttpClient`：新增 `getConfig()` 方法，调用 Mihomo 的 `GET /configs` API
- `MihomoService`：新增 `getConfig()` 和 `getForwardingPath(instanceId, domain)` 方法
- `MihomoInstanceController`：新增 `GET /{id}/forwarding-path` 端点
- 新增 `ForwardingPathService`：负责规则解析、域名匹配、流程图数据构建
- 新增 DTO 类：`ForwardingPathResult`（包含 nodes 和 edges）

**前端变更：**
- 新增 `MihomoInstanceDetailView.vue`：详情页主组件，包含三个标签页
- 新增 `ForwardingRuleTab.vue`：转发规则标签页，包含域名输入和 Vue Flow 流程图
- 新增自定义节点组件：DomainNode、RuleNode、ProxyGroupNode、ProxyNode、TargetNode
- 新增 `api/mihomo.ts` 中的 `getForwardingPath()` 方法
- 修改 `MihomoInstanceView.vue`：实例名称添加跳转链接
- 新增依赖：`@vue-flow/core`、`@vue-flow/background`、`dagre`
