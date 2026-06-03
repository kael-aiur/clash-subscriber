## Why

订阅源详情页面的代理节点标签页当前以平铺表格展示所有节点，当节点数量多时（数十到上百个）浏览混乱，用户难以快速定位特定地区的节点。规则标签页已实现按策略分组并支持展开/折叠，代理节点需要类似的交互优化。节点名称中普遍包含国旗 emoji，为按地区自动分组提供了可靠基础。

## What Changes

**代理节点标签页展示方式**
- From: 所有节点平铺在一个表格中，无分组
- To: 按国旗 emoji 自动识别地区，分组展示（折叠面板），每组内仍用表格展示详情；支持按节点名称搜索过滤
- Reason: 提升大量节点时的浏览效率
- Impact: 非破坏性变更，仅前端展示层修改

## Capabilities

### New Capabilities

无新增 capability。

### Modified Capabilities

- `subscription-detail-view`: 代理节点标签页的展示需求从"平铺表格"变更为"按地区分组的折叠面板 + 搜索过滤"

## Impact

- **代码**：仅修改 `module-web/frontend/src/views/SubscriptionView.vue`
- **API**：无变更
- **依赖**：无新增依赖
- **系统**：无后端变更，重新构建前端即可生效
