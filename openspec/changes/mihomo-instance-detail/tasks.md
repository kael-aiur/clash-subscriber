## 1. 后端 — Mihomo 配置获取

- [ ] 1.1 在 `MihomoHttpClient` 中新增 `getConfig()` 方法，调用 Mihomo 的 `GET /configs` API 获取当前运行配置
- [ ] 1.2 在 `MihomoService` 接口中新增 `getConfig(instanceId)` 方法
- [ ] 1.3 在 `MihomoServiceImpl` 中实现 `getConfig()` 方法
- [ ] 1.4 在 `MihomoInstanceController` 中新增 `GET /{id}/config` 端点

## 2. 后端 — 转发路径解析引擎

- [ ] 2.1 创建 `ForwardingPathService` 服务类，负责转发路径解析的核心逻辑
- [ ] 2.2 实现配置解析方法：从 YAML 配置中提取 `rules`、`proxy-groups`、`proxies` 数据结构
- [ ] 2.3 实现域名规则匹配方法：支持 DOMAIN、DOMAIN-SUFFIX、DOMAIN-KEYWORD、IP-CIDR、GEOIP、MATCH 类型，按优先级返回第一个匹配的规则
- [ ] 2.4 实现代理组解析方法：解析代理组的名称、类型、包含的代理节点和子代理组引用
- [ ] 2.5 实现流程图数据构建方法：根据匹配结果构建 Vue Flow 格式的 nodes 和 edges 数据结构，支持分支和嵌套代理组
- [ ] 2.6 创建 `ForwardingPathResult` DTO 类，包含 nodes 和 edges 列表

## 3. 后端 — 转发路径 API 端点

- [ ] 3.1 在 `MihomoInstanceController` 中新增 `GET /{id}/forwarding-path?domain=xxx` 端点
- [ ] 3.2 端点调用 `MihomoHttpClient.getConfig()` 获取配置，再调用 `ForwardingPathService` 解析并返回结果
- [ ] 3.3 处理异常情况：实例离线、配置获取失败、域名参数缺失

## 4. 前端 — 依赖安装和基础设置

- [ ] 4.1 安装前端依赖：`@vue-flow/core`、`@vue-flow/background`、`@vue-flow/controls`、`dagre`、`@types/dagre`
- [ ] 4.2 在 `api/mihomo.ts` 中新增 `getForwardingPath(id, domain)` API 方法

## 5. 前端 — 实例详情页框架

- [ ] 5.1 创建 `MihomoInstanceDetailView.vue` 详情页主组件，使用 `el-tabs` 实现三个标签页
- [ ] 5.2 实现实例信息标签页：展示实例基本信息（名称、API URL、Secret、启用状态、健康状态），支持编辑
- [ ] 5.3 实现推送历史标签页：展示该实例的推送记录列表
- [ ] 5.4 在 `router/index.ts` 中新增 `/mihomo-instances/:id` 路由

## 6. 前端 — 转发规则标签页

- [ ] 6.1 创建 `ForwardingRuleTab.vue` 组件，包含域名输入框和查询按钮
- [ ] 6.2 集成 Vue Flow 组件，配置基本画布（背景、缩放、平移）
- [ ] 6.3 创建自定义节点组件：DomainNode、RuleNode、ProxyGroupNode、ProxyNode、TargetNode
- [ ] 6.4 实现代理组节点的展开/折叠交互逻辑
- [ ] 6.5 集成 dagre 自动布局算法，根据节点和边数据自动计算节点位置

## 7. 前端 — 列表页跳转

- [ ] 7.1 修改 `MihomoInstanceView.vue`，将实例名称改为可点击链接，跳转到详情页
