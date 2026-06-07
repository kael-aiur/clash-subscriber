## 1. 数据模型

- [ ] 1.1 创建 ConfigProfile 模型类
- [ ] 1.2 创建 ProxyGroupConfig 模型类
- [ ] 1.3 创建 ClashBasicConfig 模型类
- [ ] 1.4 创建 RuleGroupRef 模型类

## 2. 存储层

- [ ] 2.1 创建 ConfigProfileRepository 接口
- [ ] 2.2 实现 JsonFileConfigProfileRepository
- [ ] 2.3 添加配置名称唯一性校验

## 3. 核心服务

- [ ] 3.1 创建 ConfigGeneratorService 接口
- [ ] 3.2 实现配置生成功能
- [ ] 3.3 实现订阅源节点合并逻辑
- [ ] 3.4 实现代理组构建逻辑（标签匹配、直接选择、全部节点）
- [ ] 3.5 实现规则组合并逻辑
- [ ] 3.6 实现基础信息应用逻辑
- [ ] 3.7 实现 YAML 格式输出

## 4. REST API

- [ ] 4.1 创建 ConfigController
- [ ] 4.2 实现创建配置接口 POST /api/config
- [ ] 4.3 实现更新配置接口 PUT /api/config/{id}
- [ ] 4.4 实现删除配置接口 DELETE /api/config/{id}
- [ ] 4.5 实现查询配置列表接口 GET /api/config/list
- [ ] 4.6 实现查询单个配置接口 GET /api/config/{id}

## 5. Basic Auth 认证

- [ ] 5.1 实现 Basic Auth 认证拦截器
- [ ] 5.2 实现配置获取接口 GET /api/config/{name}（带认证）
- [ ] 5.3 实现认证信息验证逻辑

## 6. 前端页面

- [ ] 6.1 创建配置列表页面 ConfigProfileListView.vue
- [ ] 6.2 创建配置编辑页面 ConfigProfileEditView.vue
- [ ] 6.3 创建基本信息配置组件 BasicInfoSection.vue
- [ ] 6.4 创建订阅源选择组件 SubscriptionSelect.vue
- [ ] 6.5 创建代理组编辑组件 ProxyGroupEditor.vue
- [ ] 6.6 创建规则组排序组件 RuleGroupSorter.vue
- [ ] 6.7 创建基础信息配置组件 BasicConfigSection.vue
- [ ] 6.8 添加前端 API 调用

## 7. 测试

- [ ] 7.1 编写 ConfigGeneratorService 单元测试
- [ ] 7.2 编写 ConfigController 集成测试
- [ ] 7.3 手动测试配置生成功能
- [ ] 7.4 手动测试 Basic Auth 认证功能
- [ ] 7.5 测试与 Clash Verge 客户端的兼容性
