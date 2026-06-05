# 订阅源详情功能 - 头脑风暴

## 问题空间

当前订阅源管理页面只展示基本信息（名称、URL、最后获取时间），点击"获取"后仅显示一个统计摘要（节点数、组数、规则数）。用户无法查看订阅源的详细内容——具体有哪些节点、节点组怎么组织、规则怎么配置。

## 用户需求

- 查看所有代理节点（名称、类型、服务器、端口）
- 查看所有节点组（组名、类型、包含哪些节点、测速配置）
- 查看所有规则（DOMAIN / DOMAIN-SUFFIX / IP-CIDR 等）

## 数据结构分析

### 代理节点 (proxies)
```
- name: "🇭🇰 香港 | 01"
  type: ss
  server: xxx.xxx.com
  port: 443
  password: xxx
  ...
```
结构清晰，适合表格展示。

### 节点组 (proxy-groups)
```
- name: "🇭🇰 香港节点"
  type: select / url-test / fallback
  proxies: [节点1, 节点2, ...]
  url: http://...        # 仅 url-test/fallback
  interval: 86400        # 仅 url-test/fallback
```
关键特征：
- 组内 proxies 是节点名称的引用列表
- 三种类型：select（手动选择）、url-test（自动测速）、fallback（故障转移）
- 通常 3-5 个组，每组可能包含 50+ 节点

### 规则 (rules)
```
['DOMAIN,flybit.vip,DIRECT', 'DOMAIN-SUFFIX,services.googleapis.cn,FlyBit', ...]
```
纯字符串数组，格式：`类型,匹配值,策略`

## 方案探索

### 标签页划分
四标签页：基本信息、代理节点、节点组、规则

### 节点组展示方案
- **方案 A：可展开表格** ← 选定
  - 与代理节点、规则的表格风格保持一致
  - 展开行显示组内节点列表
  - 支持排序和筛选
- 方案 B：卡片 + 抽屉（视觉友好但信息密度低）
- 方案 C：树形选择器（直观但与其他标签页风格不统一）

## 设计决策

1. 使用 el-drawer（抽屉）而非新页面，从列表点击"详情"触发
2. 四个标签页用 el-tabs 组件
3. 节点组用可展开表格，展开行用 tag 列表展示组内节点
4. 数据来源：复用现有的 `GET /api/subscriptions/{id}/fetch` 接口
5. 规则表格支持搜索/筛选（规则数量可能上千）
