## Why

订阅源管理页的详情功能存在数据缺失。后端 YAML 解析器只提取了代理节点（proxies），未提取代理组（proxy-groups）和规则（rules），导致前端详情页的节点组和规则标签页始终为空。用户无法查看订阅源的完整配置结构，也无法理解节点之间的分组关系和路由规则，严重影响配置管理效率。

## What Changes

**后端 YAML 解析**
- From: `YamlUtil.parseClashConfig()` 只提取 `proxies` 字段
- To: 同时提取 `proxy-groups`、`rules`、`name` 字段到 `ClashConfig` 对象
- Reason: 订阅源返回的 Clash YAML 包含完整配置，但解析器遗漏了关键字段
- Impact: 非破坏性变更，API 响应结构不变（字段本来就有定义，只是值为空）

**前端节点组展示**
- From: 节点组标签页显示空表格（因后端数据为空）
- To: 用树形关系图展示代理组层级引用关系，支持组类型颜色区分和特殊策略高亮
- Reason: 代理组之间存在引用关系（如 select 组包含 url-test 组），需要可视化展示
- Impact: 纯 UI 变更，不影响 API

**前端规则展示**
- From: 规则标签页显示空表格
- To: 展示完整规则列表，支持按类型筛选、策略可跳转到对应节点组、原始 YAML 查看
- Reason: 规则与节点组存在关联关系，需要联动展示
- Impact: 纯 UI 变更，不影响 API

## Capabilities

### New Capabilities

（无新增 capability）

### Modified Capabilities

- `subscription-detail-view`: 扩展节点组和规则的展示能力，从基础表格升级为树形关系图 + 详情面板联动

## Impact

- **后端代码**：`module-common/.../util/YamlUtil.java` — 增加字段提取逻辑（3-5 行代码）
- **前端代码**：`module-web/frontend/src/views/SubscriptionView.vue` — 重构节点组和规则标签页为「配置关系」标签页
- **API**：无变更，`POST /api/subscriptions/{id}/fetch` 返回的 `ClashConfig` 结构不变
- **依赖**：无新增依赖
- **数据库**：无变更
