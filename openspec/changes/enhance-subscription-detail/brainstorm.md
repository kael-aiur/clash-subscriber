# Brainstorm: 增强订阅源详情 - 节点组与规则展示

## 背景

用户反馈订阅源管理页的详情只能看到代理节点，看不到节点组和规则。

## 根因分析

经过代码探索发现：

1. **后端模型** `ClashConfig` 已有 `proxyGroups`（Map<String, Object>）和 `rules`（List<Object>）字段
2. **前端详情页** 已有 4 个标签页（基本信息、代理节点、节点组、规则），UI 完整
3. **根因**：`YamlUtil.parseClashConfig()` 只提取了 `proxies`，未提取 `proxy-groups` 和 `rules`

## 需求确认

### 范围：修复 + 优化展示

**后端修复：**
- 在 `YamlUtil.parseClashConfig()` 中补充提取 `proxy-groups`、`rules`、`name` 字段

**前端优化 - 节点组：**
- 展开组内节点时显示详细信息（类型、服务器），不只是名称
- 组类型可视化：select(蓝)、url-test(绿)、fallback(橙)、load-balance(紫)
- 特殊策略高亮：DIRECT(绿)、REJECT(红)

**前端优化 - 规则：**
- 规则策略可点击，跳转到对应节点组
- 按规则类型分组筛选（DOMAIN、IP-CIDR、GEOIP 等）
- 原始 YAML 文本展示

## 方案选择

**选定方案：节点组关系图**
- 树形层级图展示代理组之间的引用关系
- 以全局代理组（GLOBAL 或第一个 select 组）为根节点
- 点击节点展开详情面板

**否决方案：**
- 方案 A（增强标签页）：改动小但展示效果有限
- 方案 C（分栏联动）：改动过大，需重构布局

## 设计详情

### 后端修改
- 文件：`module-common/.../util/YamlUtil.java`
- 在 `parseClashConfig()` 方法中增加：
  - 提取 `proxy-groups` → `config.setProxyGroups()`
  - 提取 `rules` → `config.setRules()`
  - 提取 `name` → `config.setName()`

### 前端修改

**标签页调整：**
- 保留「基本信息」和「代理节点」标签页
- 将「节点组」和「规则」合并为「配置关系」标签页
- 「配置关系」内：左侧树形关系图，右侧详情面板

**关系图渲染：**
- CSS + Vue 组件手动构建，不引入第三方图表库
- 代理组层级通常不超过 3 层，CSS flexbox 足够
- 连线用 CSS 伪元素绘制

**组类型颜色：**
| 类型 | 颜色 |
|------|------|
| select | 蓝色 |
| url-test / urltest | 绿色 |
| fallback | 橙色 |
| load-balance | 紫色 |
| 其他 | 灰色 |

**特殊策略高亮：**
| 策略 | 颜色 |
|------|------|
| DIRECT | 绿色 |
| REJECT | 红色 |

**详情面板内容：**
- 选中组的完整信息（名称、类型、测速URL、间隔等）
- 组内成员列表（区分代理节点和子代理组，子代理组可点击跳转）
- 引用该组作为策略的规则列表（支持按类型筛选）
- "查看原始配置"按钮，弹窗展示原始 YAML 文本

### 涉及文件
- `module-common/.../util/YamlUtil.java` — 后端解析
- `module-web/frontend/src/views/SubscriptionView.vue` — 前端详情页
- `module-web/frontend/src/api/subscription.ts` — TypeScript 类型（如需更新）
