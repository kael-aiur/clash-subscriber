# Mihomo 实例详情页 — 设计探索

## 背景

当前 Mihomo 实例管理只有一个列表页面（`/mihomo-instances`），支持基本的 CRUD、健康检查和手动推送配置。用户希望能深入了解每个实例的转发规则，直观地看到域名请求如何经过规则匹配、代理组路由、最终到达目标服务。

## 需求总结

- Mihomo 实例详情页，包含三个标签页：实例信息、转发规则、推送历史
- **转发规则标签**：用户输入域名 → 从 Mihomo API 获取当前配置 → 解析规则匹配 → 用 Vue Flow 流程图展示转发路径
- 支持代理组嵌套展开/折叠，显示分支路径（如 Select 类型代理组的多个可选节点）

## 决策记录

### D1: 转发规则数据来源
**决策：从 Mihomo API 实时获取**
- 通过 Mihomo 的 REST API（GET /configs）获取当前运行中的配置
- 最准确，能反映实例实际运行状态
- 缺点：需要实例在线

### D2: 流程图展示层次
**决策：可展开/折叠**
- 默认展示 规则 → 代理组 → 最终出口
- 点击代理组节点可展开查看内部子组和具体代理节点
- 兼顾简洁和信息完整性

### D3: 前端可视化库
**决策：Vue Flow**
- 基于 Vue 3 的节点图库，TypeScript 支持良好
- 支持自定义节点、连线、交互
- 社区活跃，文档完善

### D4: 规则过滤方式
**决策：用户输入域名查询**
- 用户输入域名，系统匹配规则并展示对应的转发路径
- 避免一次性展示上千条规则导致的性能和可读性问题

### D5: 代理组分支展示
**决策：带分支的路径查询**
- 如果匹配到的代理组包含多个可选节点（如 Select 类型），展示所有分支
- URLTest/Fallback 类型也展示所有节点

### D6: 解析架构
**决策：后端主导**
- 后端负责：从 Mihomo 获取配置 → 解析规则 → 匹配域名 → 构建流程图数据
- 前端只负责渲染 Vue Flow 图
- 前端简单，逻辑集中

## 架构方案

### 后端 API

新增端点：`GET /api/mihomo-instances/{id}/forwarding-path?domain=google.com`

处理流程：
1. 通过 MihomoHttpClient 调用 Mihomo 的 `GET /configs` 获取当前配置
2. 用 SnakeYAML 解析 YAML 配置
3. 提取 `rules`、`proxy-groups`、`proxies` 三个核心数据
4. 按规则优先级匹配用户输入的域名
5. 构建流程图节点和边的数据结构
6. 返回 Vue Flow 格式的 JSON

规则匹配支持：DOMAIN、DOMAIN-SUFFIX、DOMAIN-KEYWORD、IP-CIDR、GEOIP、MATCH（兜底）

### 流程图数据结构

节点类型：
- **Domain** — 用户输入的域名（起点）
- **Rule** — 匹配到的规则
- **ProxyGroup** — 代理组
- **Proxy** — 具体代理节点
- **Target** — 最终出口（DIRECT/REJECT）

返回格式：
```json
{
  "nodes": [
    { "id": "domain", "type": "domain", "data": { "label": "google.com" } },
    { "id": "rule-0", "type": "rule", "data": { "label": "DOMAIN-SUFFIX,google.com" } },
    { "id": "group-proxy", "type": "proxyGroup", "data": { "label": "Proxy", "type": "select" } },
    { "id": "node-hk01", "type": "proxy", "data": { "label": "HK-01", "type": "vmess" } }
  ],
  "edges": [
    { "id": "e1", "source": "domain", "target": "rule-0" },
    { "id": "e2", "source": "rule-0", "target": "group-proxy" },
    { "id": "e3", "source": "group-proxy", "target": "node-hk01" }
  ]
}
```

### 前端页面

路由：`/mihomo-instances/:id` → `MihomoInstanceDetailView.vue`

标签页：
1. **实例信息** — 基本信息展示和编辑（名称、API URL、Secret、启用状态、健康状态）
2. **转发规则** — 域名输入框 + 查询按钮 + Vue Flow 流程图
3. **推送历史** — 推送记录列表（时间、状态、关联管线）

流程图节点样式：
- Domain 节点：蓝色圆角
- Rule 节点：绿色
- ProxyGroup 节点：橙色，可点击展开/折叠
- Proxy 节点：紫色
- DIRECT/REJECT：灰色终端节点

自动布局：使用 dagre 算法

列表页改动：在 MihomoInstanceView.vue 的实例名称上添加链接，点击跳转到详情页。

## 技术栈

- 后端：Java 21, Spring Boot, SnakeYAML, OkHttp
- 前端：Vue 3, TypeScript, Vue Flow, Element Plus, dagre
