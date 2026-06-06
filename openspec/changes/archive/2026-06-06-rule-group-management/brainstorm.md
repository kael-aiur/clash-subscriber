# 规则组管理功能 - 头脑风暴

## 背景

Clash 订阅管理中心当前可以管理多个订阅源、获取配置、通过构建流水线合并处理后推送到 Mihomo 实例。但缺少一个关键能力：**复用订阅供应商配置好的规则**。

用户在构建自己的订阅配置时，往往需要大量规则（DOMAIN-SUFFIX, GEOIP, IP-CIDR 等），这些规则在供应商的订阅中已经配置好了，但无法被单独提取和复用。

## 核心概念

### 规则组 (RuleGroup)

从订阅中提取出的规则集合，包含：
- **规则列表**：标准 Clash 规则字符串
- **代理对象**：规则中引用的代理名称的抽象表示

### 代理对象 (RuleProxyObject)

规则中引用的代理名称被抽象为"代理对象"。这是一个占位符概念：
- 从订阅提取时，自动识别规则中引用的代理组名
- 使用时，通过映射将占位符替换为实际的代理组名或节点名

示例：
```
订阅原始规则:  DOMAIN-SUFFIX,google.com,美国节点
提取后规则:    DOMAIN-SUFFIX,google.com,{{px-001}}
代理对象:      px-001 → 源名称: "美国节点"
```

## 设计决策

### Q1: 规则组放在哪个模块？

**决策：放在 module-processor**

理由：规则组本质上是规则处理的一种形式，与现有的 RuleModifyProcessor、ProxyGroupProcessor 等同属处理层。

### Q2: 提取策略是什么？

**决策：自动提取所有被规则引用的代理名，排除内置名**

- 扫描所有规则，提取第 3 个字段的代理名
- 过滤掉 DIRECT、REJECT、PASS 等 Clash 内置名
- 为每个唯一的代理名生成一个代理对象（带唯一 ID）

### Q3: 同步策略是什么？

**决策：按需同步，完全覆盖**

- 订阅列表页提供"提取规则组"/"重新提取"按钮
- 每次点击都完全重新生成规则组，覆盖所有用户编辑
- 不点击则规则组保持独立，不受订阅更新影响
- 不存在增量合并，每次都是全量覆盖

### Q4: 占位符格式？

**决策：`{{px-001}}` 格式**

- 双大括号包裹代理对象 ID
- 内置名（DIRECT/REJECT）保持原样，不做占位符替换

### Q5: 可编辑性？

**决策：全部可编辑**

- 规则内容：可增删改，结构化列表展示
- 代理对象：可增删改名
- 基本信息：名称、描述
- 从订阅提取的规则组也可手动编辑（但重新提取会覆盖）

### Q6: 规则如何解析展示？

**决策：逗号分割为三段**

- 规则格式：`类型,参数,代理名`（如 `DOMAIN-SUFFIX,google.com,Proxy`）
- MATCH 等无参数规则只有两段：`MATCH,代理名`
- 前端以结构化列表展示，每行显示类型+参数和代理对象

## 数据模型

```java
// 规则组
public class RuleGroup {
    private String id;
    private String name;
    private String description;
    private String sourceSubscriptionId;  // 可 null（手动创建时）
    private List<String> rules;           // 含占位符的规则字符串
    private List<RuleProxyObject> proxyObjects;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// 代理对象
public class RuleProxyObject {
    private String id;          // 如 "px-001"
    private String sourceName;  // 源配置中的原始名称
    private String description;
}
```

## API 设计

```
GET    /api/rule-groups              列表
POST   /api/rule-groups              手动创建
GET    /api/rule-groups/{id}         详情
PUT    /api/rule-groups/{id}         编辑
DELETE /api/rule-groups/{id}         删除
POST   /api/rule-groups/extract      从订阅提取（body: { subscriptionId }）
```

## 前端设计

### 订阅列表页变化
- 每个订阅卡片增加"提取规则组"按钮
- 已有规则组时显示"查看规则组"和"重新提取"

### 规则组列表页（新增）
- 列表展示：名称、来源订阅、规则数、代理对象数
- 手动创建按钮

### 规则组详情/编辑页（新增）
- 基本信息编辑区
- 代理对象表格（ID、源名称、描述、操作）
- 规则列表表格（序号、类型+参数、代理对象引用、操作）
- 来源订阅的规则组显示提示："重新提取将覆盖所有手动修改"

## 与构建流水线的集成

**暂不实现**，后续单独设计。规则组管理功能先独立做好，集成方案另行考虑。
