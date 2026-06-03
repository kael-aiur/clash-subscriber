## Context

订阅源管理页详情功能存在数据缺失：后端 `YamlUtil.parseClashConfig()` 只解析了 `proxies` 字段，未提取 `proxy-groups` 和 `rules`。前端已有完整的标签页 UI（包括节点组和规则），但由于后端未返回数据，这些标签页始终为空。

当前状态：
- 后端模型 `ClashConfig` 已有 `proxyGroups` 和 `rules` 字段定义
- 前端详情页已有 4 个标签页（基本信息、代理节点、节点组、规则）
- 解析函数 `YamlUtil.parseClashConfig()` 仅提取 `proxies`
- 订阅服务商返回的 Clash YAML 配置包含完整的 `proxy-groups` 和 `rules`

## Goals / Non-Goals

**Goals:**
- 修复后端 YAML 解析，正确提取 `proxy-groups`、`rules`、`name` 字段
- 在前端用树形关系图展示代理组之间的层级引用关系
- 支持点击节点组查看详细信息和关联规则
- 通过颜色区分代理组类型，高亮特殊策略（DIRECT/REJECT）

**Non-Goals:**
- 不引入第三方图表库（ECharts、D3、vue-flow 等）
- 不修改代理节点标签页的现有展示
- 不改变 CRUD 和获取订阅的现有 API
- 不处理规则的编辑或重排功能

## Decisions

### D1：后端解析方式

- **选择**：在 `YamlUtil.parseClashConfig()` 中直接从已解析的 `Map<String, Object>` 提取字段
- **理由**：YAML 已被 SnakeYAML 解析为 Map，直接 get 即可，无需额外依赖
- **已考虑 alternative**：创建独立的 `proxy-groups` 和 `rules` 的 POJO 模型 → 拒绝，因为 proxy-groups 结构多样（select/url-test/fallback/load-balance 各有不同字段），用 `Map<String, Object>` 更灵活，与现有 `raw` 字段风格一致

### D2：前端标签页结构

- **选择**：将「节点组」和「规则」合并为「配置关系」标签页，左侧树形图、右侧详情面板
- **理由**：关系图天然需要同时展示组和规则的关联，合并在同一视图中交互更连贯
- **已考虑 alternative**：保留独立标签页，在节点组标签页内嵌关系图 → 拒绝，因为规则与组的关联展示需要跨标签页跳转，体验割裂

### D3：关系图渲染方案

- **选择**：CSS + Vue 组件手动构建树形图
- **理由**：代理组层级通常 ≤3 层，节点数量有限，CSS flexbox + 伪元素绘制连线完全够用，避免引入重量级依赖
- **已考虑 alternative**：引入 vue-flow 或 ECharts → 拒绝，增加包体积和学习成本，对当前场景过度设计

### D4：组类型颜色方案

- **选择**：
  - select → 蓝色 (#409EFF)
  - url-test / urltest → 绿色 (#67C23A)
  - fallback → 橙色 (#E6A23C)
  - load-balance → 紫色 (#9B59B6)
  - 其他 → 灰色 (#909399)
- **理由**：与 Element Plus 的语义色一致，用户直觉认知一致
- **已考虑 alternative**：自定义配色方案 → 拒绝，增加设计复杂度且无明显收益

### D5：特殊策略高亮

- **选择**：DIRECT 绿色、REJECT 红色，其余策略默认样式
- **理由**：DIRECT 和 REJECT 是 Clash 配置中最常见的特殊策略，需要视觉区分
- **已考虑 alternative**：所有策略都有颜色 → 拒绝，策略名称通常是自定义组名，无法预定义颜色

## Risks / Trade-offs

- [Risk] 部分订阅源的 proxy-groups 结构不标准（缺少 type 字段等） → Mitigation：解析时做空值防御，缺失字段显示为默认值
- [Risk] 大量规则（>1000条）可能导致详情面板渲染卡顿 → Mitigation：规则列表使用虚拟滚动或分页
- [Risk] 代理组层级过深（>5层）导致树形图布局混乱 → Mitigation：限制展开层级，深层级折叠显示
- [Trade-off] 用 CSS 而非图表库绘制树形图，连线样式相对简单 → 接受，因为层级浅、节点少，简单连线已足够清晰

## Migration Plan

N/A — 本变更不涉及部署变更。纯前端+后端代码修改，无数据库迁移、无 API 变更。

部署步骤：
1. 修改 `YamlUtil.java` 并运行单元测试
2. 修改前端组件并验证构建
3. 用户点击「获取」按钮重新拉取订阅，即可看到节点组和规则数据

## Open Questions

- 无遗留问题。设计方案已通过用户逐节确认。
