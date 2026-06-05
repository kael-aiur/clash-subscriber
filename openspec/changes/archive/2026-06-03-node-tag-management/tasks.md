## 1. 后端 - NodeTag 模型与存储

- [x] 1.1 创建 `NodeTag` 实体类（id, name, priority, patterns, createdAt, updatedAt）
- [x] 1.2 创建 `NodeTagRepository` 接口（findAll, findById, save, deleteById）
- [x] 1.3 创建 `JsonFileNodeTagRepository` 实现（JSON 文件存储到 `data/node-tags/`）

## 2. 后端 - NodeTag 服务层

- [x] 2.1 创建 `NodeTagService` 接口
- [x] 2.2 创建 `NodeTagServiceImpl` 实现（CRUD 业务逻辑，findAll 按 priority 排序）

## 3. 后端 - NodeTag API

- [x] 3.1 创建 `NodeTagController`（GET/POST/PUT/DELETE `/api/node-tags`）

## 4. 前端 - API 与类型定义

- [x] 4.1 在 `subscription.ts` 中新增 `NodeTag` TypeScript 接口定义
- [x] 4.2 新增 `nodeTagApi`（CRUD API 调用方法）

## 5. 前端 - 订阅详情页节点分组改造

- [x] 5.1 在 SubscriptionView.vue 中加载标签列表（页面初始化时获取）
- [x] 5.2 重写 `regionGroups` 计算属性：按标签优先级匹配节点名进行分组，保留"其他"兜底
- [x] 5.3 删除 `FLAG_REGION_MAP` 硬编码映射

## 6. 前端 - 标签管理页面

- [x] 6.1 创建 `NodeTagManageView.vue`（表格展示标签列表 + 新增/编辑弹窗 + 删除确认）
- [x] 6.2 在路由和侧边栏导航中添加标签管理页面入口
