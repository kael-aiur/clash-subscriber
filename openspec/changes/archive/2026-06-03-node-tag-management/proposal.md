## Why

订阅详情页的"代理节点"Tab 中，由于前端硬编码的 `FLAG_REGION_MAP` 仅覆盖约30个国家旗 emoji，大量节点无法被正确归类而落入"其他"分组。用户无法自定义匹配规则，也无法按实际需求扩展分类。节点标签管理功能允许用户通过配置匹配规则（字符串数组）对节点进行灵活分类，提升订阅详情的可用性。

## What Changes

**节点分组逻辑**
- From: 前端硬编码 `FLAG_REGION_MAP`，仅通过 emoji 映射分类
- To: 从后端获取用户配置的全局标签列表，按优先级顺序进行字符串匹配分类
- Impact: 非破坏性变更，"其他"兜底逻辑保留

**新增标签管理能力**
- 新增后端 NodeTag CRUD API（`/api/node-tags`）
- 新增前端标签管理页面（独立路由）
- 新增前端标签匹配逻辑替代硬编码映射

## Capabilities

### New Capabilities
- `node-tag-management`: 节点标签的 CRUD 管理，包括标签实体（名称、优先级、匹配规则）、JSON 文件存储、REST API
- `node-tag-matching`: 基于标签配置的节点分类匹配，在订阅详情页按标签分组展示代理节点

### Modified Capabilities

无。现有功能需求未变更。

## Impact

- **后端新增**：NodeTag 模型、Repository、Service、Controller（6个文件）
- **前端改动**：SubscriptionView.vue（重写分组逻辑）、新增标签管理页面、新增 API 调用
- **数据存储**：新增 `data/node-tags/` 目录
- **API**：新增 `/api/node-tags` 端点（GET/POST/PUT/DELETE）
- **依赖**：无新增外部依赖
