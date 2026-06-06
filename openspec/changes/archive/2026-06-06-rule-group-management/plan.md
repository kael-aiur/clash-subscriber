# 规则组管理 实现计划

**Goal:** 实现规则组的 CRUD 管理和从订阅自动提取功能，支持代理对象占位符机制。

**Architecture:** 在 module-processor 中新增 RuleGroup 模型、Repository、Service 和 Controller，遵循项目现有的 JSON 文件持久化模式。前端新增规则组管理页面，并在订阅列表页集成提取入口。

**Tech Stack:** Java 21, Spring Boot 3.2.5, Jackson, Vue 3, TypeScript

---

## Task 1: 数据模型与持久化

- [ ] **Step 1.1:** 创建 `RuleProxyObject.java`
  - 路径: `module-processor/src/main/java/site/kael/clash/processor/model/RuleProxyObject.java`
  - 字段: id (String), sourceName (String), description (String)
  - 无参构造 + getter/setter

- [ ] **Step 1.2:** 创建 `RuleGroup.java`
  - 路径: `module-processor/src/main/java/site/kael/clash/processor/model/RuleGroup.java`
  - 字段: id, name, description, sourceSubscriptionId, rules (List<String>), proxyObjects (List<RuleProxyObject>), createdAt, updatedAt
  - 无参构造中初始化 createdAt/updatedAt = LocalDateTime.now()

- [ ] **Step 1.3:** 创建 `RuleGroupRepository.java` 接口
  - 路径: `module-processor/src/main/java/site/kael/clash/processor/repository/RuleGroupRepository.java`
  - 方法: save, findById, findAll, deleteById

- [ ] **Step 1.4:** 实现 `JsonFileRuleGroupRepository.java`
  - 路径: `module-processor/src/main/java/site/kael/clash/processor/repository/JsonFileRuleGroupRepository.java`
  - 参照 `JsonFilePipelineConfigRepository` 的实现模式
  - 存储目录: `data/rule-groups/`

- [ ] **Step 1.5:** 编译验证
  ```bash
  mvn compile -pl module-processor
  ```

---

## Task 2: 业务逻辑

- [ ] **Step 2.1:** 创建 `RuleGroupService.java` 接口
  - 路径: `module-processor/src/main/java/site/kael/clash/processor/service/RuleGroupService.java`
  - 方法: findAll, findById, create, update, delete, extractFromSubscription

- [ ] **Step 2.2:** 创建规则解析工具方法
  - 在 Service 实现中添加 `parseRule(String rule)` 方法
  - 逗号分割为 2-3 段：类型、参数（可选）、代理名/占位符
  - 添加 `isBuiltInProxy(String name)` 方法，判断 DIRECT/REJECT/PASS

- [ ] **Step 2.3:** 实现 `RuleGroupServiceImpl.java` 的 CRUD 方法
  - 路径: `module-processor/src/main/java/site/kael/clash/processor/service/impl/RuleGroupServiceImpl.java`
  - 使用 IdGenerator.generate() 生成 ID
  - create 时设置 createdAt/updatedAt
  - update 时更新 updatedAt

- [ ] **Step 2.4:** 实现 extractFromSubscription 方法
  - 调用 SubscriptionService.fetch(subscriptionId) 获取 ClashConfig
  - 扫描 rules 中每条规则的第 3 个字段
  - 过滤内置名，收集唯一代理名
  - 为每个代理名生成 RuleProxyObject（IdGenerator 生成 ID）
  - 替换规则中的代理名为 `{{id}}` 占位符
  - 创建 RuleGroup，name 为 "{订阅名}的规则组"
  - 如已存在该订阅的规则组，覆盖更新（保留 ID）

- [ ] **Step 2.5:** 编译验证
  ```bash
  mvn compile -pl module-processor
  ```

---

## Task 3: REST API

- [ ] **Step 3.1:** 创建 `RuleGroupController.java`
  - 路径: `module-processor/src/main/java/site/kael/clash/processor/controller/RuleGroupController.java`
  - @RestController, @RequestMapping("/api/rule-groups")
  - 构造器注入 RuleGroupService

- [ ] **Step 3.2:** 实现 CRUD 端点
  - GET / → findAll
  - GET /{id} → findById
  - POST / → create
  - PUT /{id} → update
  - DELETE /{id} → delete

- [ ] **Step 3.3:** 实现提取端点
  - POST /extract → extractFromSubscription，body: { subscriptionId: "..." }
  - 返回创建/更新的 RuleGroup

- [ ] **Step 3.4:** 编译并测试
  ```bash
  mvn compile
  # 手动启动服务测试 API
  ```

---

## Task 4: 前端 — 规则组管理页面

- [ ] **Step 4.1:** 创建 API 调用模块
  - 路径: `module-web/frontend/src/api/ruleGroup.ts`
  - 方法: getList, getById, create, update, delete, extract

- [ ] **Step 4.2:** 创建规则组列表页
  - 路径: `module-web/frontend/src/views/RuleGroupList.vue`
  - 表格列: 名称、来源订阅、规则数、代理对象数、操作（查看/编辑/删除）

- [ ] **Step 4.3:** 创建规则组详情/编辑页
  - 路径: `module-web/frontend/src/views/RuleGroupDetail.vue`
  - 基本信息编辑区（名称、描述）
  - 代理对象表格（ID、源名称、描述、操作）
  - 规则列表表格（序号、类型+参数、代理对象引用、操作）
  - 来源订阅的规则组显示覆盖提示

- [ ] **Step 4.4:** 实现代理对象编辑组件
  - 表格内行编辑或弹窗编辑
  - 支持新增、修改、删除

- [ ] **Step 4.5:** 实现规则编辑组件
  - 弹窗编辑：类型输入、参数输入、代理对象下拉选择（含内置名选项）
  - 支持新增、修改、删除、排序

- [ ] **Step 4.6:** 添加路由配置
  - 在 router 中添加规则组相关路由
  - 列表页: /rule-groups
  - 详情页: /rule-groups/:id

---

## Task 5: 前端 — 订阅列表页集成

- [ ] **Step 5.1:** 订阅列表页增加规则组状态和操作按钮
  - 查询订阅时附带规则组状态（是否已提取）
  - 未提取: 显示"提取规则组"按钮
  - 已提取: 显示"查看规则组"链接 + "重新提取"按钮

- [ ] **Step 5.2:** 实现提取/重新提取逻辑
  - 调用 POST /api/rule-groups/extract
  - 重新提取时弹出确认对话框
  - 成功后跳转到规则组详情页

- [ ] **Step 5.3:** 前端构建验证
  ```bash
  cd module-web/frontend && npm run build
  ```
