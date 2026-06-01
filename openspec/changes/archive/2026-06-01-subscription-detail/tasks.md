## 1. 前端类型定义

- [x] 1.1 在 `api/subscription.ts` 中补充 `ProxyNode` 接口（name, type, server, port, 以及扩展字段索引签名）
- [x] 1.2 在 `api/subscription.ts` 中补充 `ProxyGroup` 接口（name, type, proxies: string[], url?, interval?）
- [x] 1.3 将 `ClashConfig` 接口的 `proxies` 类型从 `unknown[]` 改为 `ProxyNode[]`，`proxyGroups` 类型改为 `Record<string, ProxyGroup>`

## 2. 详情抽屉基础框架

- [x] 2.1 在 `SubscriptionView.vue` 的操作列添加"详情"按钮
- [x] 2.2 添加详情抽屉组件（el-drawer），包含 el-tabs 四个标签页
- [x] 2.3 实现点击"详情"时调用 fetch 接口并将结果存入组件状态
- [x] 2.4 处理 fetch 加载状态和错误提示

## 3. 基本信息标签页

- [x] 3.1 使用 el-descriptions 展示配置名称、订阅 URL、User-Agent、最后获取时间
- [x] 3.2 展示自定义 Headers（如有）

## 4. 代理节点标签页

- [x] 4.1 使用 el-table 展示节点列表，列为：名称、类型、服务器、端口
- [x] 4.2 标签页标题显示节点总数

## 5. 节点组标签页

- [x] 5.1 将 proxyGroups（Record 转 Array）转为数组用于表格数据源
- [x] 5.2 使用 el-table 展示节点组列表，列为：组名、类型、节点数、测速间隔
- [x] 5.3 实现行展开功能，展开行内以 el-tag 列表展示组内节点名称
- [x] 5.4 标签页标题显示节点组总数

## 6. 规则标签页

- [x] 6.1 解析规则字符串（如 `DOMAIN,example.com,DIRECT`）拆分为类型、匹配值、策略三列
- [x] 6.2 使用 el-table 展示规则列表
- [x] 6.3 添加搜索输入框，按关键词实时过滤规则
- [x] 6.4 标签页标题显示规则总数
