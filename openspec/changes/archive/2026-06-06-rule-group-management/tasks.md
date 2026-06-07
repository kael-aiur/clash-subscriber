## 1. 数据模型与持久化

- [x] 1.1 在 module-processor 中创建 `RuleProxyObject` 模型类（id, sourceName, description）
- [x] 1.2 在 module-processor 中创建 `RuleGroup` 模型类（id, name, description, sourceSubscriptionId, rules, proxyObjects, createdAt, updatedAt）
- [x] 1.3 创建 `RuleGroupRepository` 接口（save, findById, findAll, deleteById）
- [x] 1.4 实现 `JsonFileRuleGroupRepository`，存储到 `data/rule-groups/{id}.json`

## 2. 业务逻辑

- [x] 2.1 创建 `RuleGroupService` 接口（CRUD + extract 方法）
- [x] 2.2 实现 `RuleGroupServiceImpl`，注入 RuleGroupRepository 和 SubscriptionService
- [x] 2.3 实现从订阅提取规则组的逻辑：扫描规则、过滤内置名、生成代理对象、替换占位符
- [x] 2.4 实现重新提取逻辑：完全覆盖现有规则组内容，保留 ID
- [x] 2.5 实现规则解析工具方法：逗号分割规则字符串为类型/参数/代理名三段

## 3. REST API

- [x] 3.1 创建 `RuleGroupController`，实现 GET /api/rule-groups（列表）
- [x] 3.2 实现 POST /api/rule-groups（手动创建）
- [x] 3.3 实现 GET /api/rule-groups/{id}（详情）
- [x] 3.4 实现 PUT /api/rule-groups/{id}（更新）
- [x] 3.5 实现 DELETE /api/rule-groups/{id}（删除）
- [x] 3.6 实现 POST /api/rule-groups/extract（从订阅提取，body: { subscriptionId }）

## 4. 前端 — 规则组管理页面

- [x] 4.1 创建规则组列表页组件（名称、来源订阅、规则数、代理对象数、操作按钮）
- [x] 4.2 创建规则组详情/编辑页组件（基本信息编辑区）
- [x] 4.3 实现代理对象表格组件（ID、源名称、描述、编辑/删除操作、添加按钮）
- [x] 4.4 实现规则列表表格组件（序号、类型+参数、代理对象引用、编辑/删除操作、添加按钮）
- [x] 4.5 实现规则编辑弹窗（类型输入、参数输入、代理对象下拉选择）
- [x] 4.6 实现手动创建规则组的表单页面
- [x] 4.7 添加路由配置（规则组列表页、详情页、创建页）

## 5. 前端 — 订阅列表页集成

- [x] 5.1 订阅列表页增加"提取规则组"按钮（未提取时显示）
- [x] 5.2 订阅列表页增加"查看规则组"链接和"重新提取"按钮（已提取时显示）
- [x] 5.3 实现重新提取的确认对话框
- [x] 5.4 调用后端提取 API 并处理响应（成功后跳转到规则组详情页）
