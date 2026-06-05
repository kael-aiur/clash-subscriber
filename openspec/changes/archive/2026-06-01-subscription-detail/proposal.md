## Why

当前订阅源管理页面只能看到名称、URL 和最后获取时间，点击"获取"后仅弹出统计摘要（节点数、组数、规则数）。用户无法查看订阅源的实际内容——不知道有哪些节点可用、节点组怎么组织、规则如何配置。这导致每次确认订阅内容都需要导出 YAML 手动查看，效率低下。

## What Changes

**订阅源列表新增"详情"操作**
- From: 列表仅有"获取"、"编辑"、"删除"三个操作
- To: 新增"详情"按钮，点击打开详情抽屉
- Impact: 非破坏性变更，仅新增 UI 元素

**新增订阅源详情抽屉**
- From: 无详情查看能力
- To: 右侧抽屉包含四个标签页
  - 基本信息：名称、URL、UA、Headers、缓存时间等
  - 代理节点：表格展示节点名称、类型、服务器、端口
  - 节点组：可展开表格，展开后显示组内节点（tag 列表）
  - 规则：表格展示规则类型、匹配值、策略，支持搜索
- Impact: 非破坏性变更，仅新增 UI 组件

**数据来源**
- From: 无
- To: 复用现有 `POST /api/subscriptions/{id}/fetch` 接口返回的 ClashConfig 数据
- Impact: 无需新增后端 API

## Capabilities

### New Capabilities
- `subscription-detail-view`: 订阅源详情查看功能，包含四标签页抽屉组件（基本信息、代理节点、节点组、规则）

### Modified Capabilities

无。本次变更仅涉及前端新增组件，不修改现有功能的需求。

## Impact

**前端**
- `SubscriptionView.vue`：新增"详情"按钮和详情抽屉组件
- `api/subscription.ts`：可能需要调整 fetch 接口的返回类型定义（如当前不返回完整 raw 数据）

**后端**
- 无变更。复用现有 `SubscriptionController.fetch()` 接口

**依赖**
- Element Plus：el-drawer、el-tabs、el-table（已引入）
- 无新增第三方依赖
