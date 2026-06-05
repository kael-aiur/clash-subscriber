# 节点标签管理 - 头脑风暴

## 背景

当前订阅详情页的"代理节点"Tab 中，所有节点被归类到"其他"类别。现有的分组逻辑依赖前端硬编码的 `FLAG_REGION_MAP`（约30个国家旗 emoji 映射），只能识别以特定 emoji 开头的节点名，大量节点无法归类。

用户希望实现一个**节点标签管理功能**，通过可配置的匹配规则将节点按国家/地区归类。

## 问题分析

### 现状

- 节点 (`ProxyNode`) 只有 `name`, `type`, `server`, `port`, `extra` 字段，无分类元数据
- 前端 `regionGroups` 逻辑：提取节点名首字符 emoji → 查 `FLAG_REGION_MAP` → 映射不到则归入"其他"
- 节点不在后端持久化，仅在订阅获取时临时存在于 `ClashConfig` 中
- 现有 Processor 架构（`duplicate-remove`, `node-merge`, `proxy-group` 等）在 build pipeline 中使用，但订阅详情预览不走 pipeline

### 核心约束

- 节点没有元数据，只能通过节点名字符串匹配来分类
- 不同服务商的节点命名风格不同（有的用 emoji，有的用中文，有的用英文缩写）
- 需要用户可配置，而非硬编码

## 决策记录

### Q1: 标签是全局的还是订阅级的？

**决策：全局标签**

理由：
- 大多数节点命名有通用规律（国家名、地区代码），跨订阅通用
- 全局管理更简单，不需要每个订阅单独配置
- 如果某个订阅的命名风格特殊，可以通过在标签中添加更多匹配规则来覆盖

### Q2: 匹配冲突怎么处理？

**决策：支持优先级，按优先级顺序匹配，第一个命中即停止**

理由：
- 用户可以精确控制匹配顺序（例如"美国"标签优先于泛匹配标签）
- "其他"作为兜底，未命中任何标签的节点归入"其他"
- 优先级用数字表示，越小越优先

### Q3: 标签匹配在哪里执行？

**决策：纯前端匹配**

理由：
- 改动最小，不影响现有后端架构
- 标签配置从后端 API 获取，匹配逻辑在前端 `computed` 属性中完成
- 替换现有的 `FLAG_REGION_MAP` 硬编码逻辑即可
- 后端只需提供标签的 CRUD API + JSON 文件存储（与现有 Subscription、MihomoInstance 一致）

## 设计方案

### 数据模型

```
NodeTag {
  id:          string    (UUID)
  name:        string    标签显示名，如"美国"
  priority:    number    优先级，越小越优先
  patterns:    string[]  匹配规则数组，节点名包含任意一个即命中
  createdAt:   datetime
  updatedAt:   datetime
}
```

### 匹配算法

```
输入: 节点列表 + 标签列表(按 priority 升序)

对每个节点:
  遍历标签列表(按 priority):
    如果 node.name 包含 tag.patterns 中的任意一个字符串:
      归入该标签分组，停止遍历
  如果未命中任何标签:
    归入"其他"
```

### 后端改动

新增:
- `NodeTag.java` - 实体类
- `NodeTagRepository.java` - 仓库接口
- `JsonFileNodeTagRepository.java` - JSON 文件存储实现
- `NodeTagController.java` - REST API
- `NodeTagService.java` / `NodeTagServiceImpl.java` - 服务层

API:
- `GET /api/node-tags` - 获取全部标签（按 priority 排序）
- `POST /api/node-tags` - 创建标签
- `PUT /api/node-tags/{id}` - 更新标签
- `DELETE /api/node-tags/{id}` - 删除标签

存储: `data/node-tags/{id}.json`

### 前端改动

改动:
- `SubscriptionView.vue` - 删除 `FLAG_REGION_MAP`，加载标签列表，重写 `regionGroups` 计算属性
- `subscription.ts` - 新增 `NodeTag` 类型定义和 API 调用

新增:
- 标签管理页面（独立路由或设置子页面）- 表格展示 + 新增/编辑弹窗 + 优先级调整

### 预置标签

考虑预置一套常见国家/地区的标签配置，让用户开箱即用，同时可以自由增删改。
