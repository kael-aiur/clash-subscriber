## 1. 数据模型

- [x] 1.1 新增 `NodePolicy` 模型（mode、excludeKeywords、matchKeywords）
- [x] 1.2 新增 `SubscriptionRef` 模型（subscriptionId、nodePolicy）
- [x] 1.3 `ConfigProfile` 新增 `subscriptionRefs` 字段，保留 `subscriptionIds` 读取兼容

## 2. 节点过滤

- [x] 2.1 抽出共用的节点过滤器 `NodeFilter`（复用 matchKeywords/excludeKeywords 匹配逻辑）
- [x] 2.2 `resolveProxyGroups` 改为复用 `NodeFilter`，消除重复逻辑
- [x] 2.3 编写 `NodeFilter` 单元测试（默认、排除、匹配、排除+匹配叠加、大小写）

## 3. 配置生成流程

- [x] 3.1 `ConfigGeneratorServiceImpl` 改用 `subscriptionRefs` 驱动 fetch
- [x] 3.2 每个 `ClashConfig` fetch 后按对应 `nodePolicy` 过滤 `getProxies()`
- [x] 3.3 默认规则（全部节点、排除词为空）行为与现状一致
- [x] 3.4 `subscriptionIds` → `subscriptionRefs` 兼容读取映射
- [x] 3.5 编写生成流程相关单元/集成测试

## 4. 前端

- [x] 4.1 `api/config-profile.ts` 新增 `SubscriptionRef` / `NodePolicy` 类型，`ConfigProfile` 改用 `subscriptionRefs`
- [x] 4.2 `ConfigProfileEditView` 订阅源卡片改造：选中订阅源展示为列表，每行附 mode 单选 + 条件渲染
- [x] 4.3 复用代理组配置的 UI 样式（mode 单选、关键词多选输入）
- [x] 4.4 编辑态加载时把 `subscriptionRefs` 还原为表单结构
- [x] 4.5 保存时序列化为 `subscriptionRefs`
- [x] 4.6 排除关键词输入框补充示例占位文案（到期、剩余、流量、余额）

## 5. 验证

- [x] 5.1 默认规则生成结果与升级前一致（回归）
- [x] 5.2 配置排除关键词后，proxies 不含伪节点，Mihomo 推送成功
- [x] 5.3 老 ConfigProfile 数据加载与生成正常
