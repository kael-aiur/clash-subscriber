# 构建历史详情 — 配置内容展示设计

## Context

构建历史详情页面当前只展示简单的 ID 和描述文本，无法看到实际的配置内容。用户需要在各步骤中看到订阅源名称、完整的配置 YAML、以及配置摘要（节点数、代理组数、规则数）。

## Goals

1. 拉取主订阅配置：输入显示订阅源名称，输出显示配置摘要和可展开的 YAML
2. 合并额外订阅节点：输入显示主配置 + 各额外配置的摘要，输出显示合并后的配置
3. 脚本处理：输入显示合并后的配置，输出显示处理后的配置
4. 推送到 Mihomo：输入显示实例名称和最终配置，输出显示成功/失败
5. 所有配置内容用 ConfigCard 组件展示：数字摘要 + 节点/代理组名称预览 + 可展开的 YAML

## Decisions

### 数据存储：构建时保存完整快照

在 `BuildPipelineServiceImpl.execute()` 方法中，修改各步骤的 input/output 存储内容：

- **input/output 字段存储结构化对象**（复用 BuildStep 的 Object 类型，不修改模型）
- 构建时将完整的 ClashConfig 转为 YAML 字符串并保存
- 同时保存配置摘要信息用于卡片展示

### 辅助方法

新增两个辅助方法：

```java
// 从 ClashConfig 提取摘要
private Map<String, Object> buildConfigSummary(ClashConfig config) {
    return Map.of(
        "nodeCount", config.getProxies().size(),
        "proxyGroupCount", config.getProxyGroups().size(),
        "ruleCount", config.getRules().size(),
        "nodeNames", config.getProxies().stream().limit(5).map(ProxyNode::getName).toList(),        // 最多 5 个用于预览
        "proxyGroupNames", config.getProxyGroups().keySet().stream().limit(5).toList()               // 最多 5 个用于预览
    );
}

// ClashConfig 转 YAML 字符串
private String configToYaml(ClashConfig config) {
    syncRawFromFields(config);
    return yaml.dump(config.getRaw());
}
```

### 各步骤数据结构

#### 步骤 1：拉取主订阅配置

```json
{
  "input": { "subscriptionName": "机场A" },
  "output": {
    "configSummary": {
      "nodeCount": 25,
      "proxyGroupCount": 5,
      "ruleCount": 120,
      "nodeNames": ["香港01", "日本02", ...],
      "proxyGroupNames": ["自动选择", "手动选择", ...]
    },
    "configYaml": "proxies:\n  - name: 香港01\n..."
  }
}
```

#### 步骤 2：合并额外订阅节点

> `mainConfigSummary`/`mainConfigYaml` 来自步骤 1 拉取的主订阅配置（即合并前的主配置快照）。

```json
{
  "input": {
    "mainConfigSummary": { ... },
    "mainConfigYaml": "...",
    "extraConfigs": [
      {
        "subscriptionName": "机场B",
        "configSummary": { "nodeCount": 15, ... },
        "configYaml": "..."
      }
    ]
  },
  "output": {
    "configSummary": { "nodeCount": 40, ... },
    "configYaml": "..."
  }
}
```

#### 步骤 3：脚本处理

```json
{
  "input": {
    "scriptName": "clean-rules",
    "configSummary": { ... },
    "configYaml": "..."
  },
  "output": {
    "configSummary": { ... },
    "configYaml": "..."
  }
}
```

#### 步骤 4：推送到 Mihomo

```json
{
  "input": {
    "instanceName": "本地节点",
    "configSummary": { ... },
    "configYaml": "..."
  },
  "output": { "success": true }
}
```

## Components

### ConfigCard.vue（新增）

配置内容展示组件，用于所有步骤中展示配置信息。

**Props**:
- `summary`: `{ nodeCount, proxyGroupCount, ruleCount, nodeNames, proxyGroupNames }`
  - `nodeNames` 和 `proxyGroupNames` 各最多 5 个，用于预览展示
- `yamlContent`: `string`（完整 YAML，可选）
- `expandable`: `boolean`（默认 true）

**展示结构**:
```
┌─────────────────────────────────────────┐
│  节点: 25    代理组: 5    规则: 120      │  ← 摘要行
│  ─────────────────────────────────────  │
│  香港01, 日本02, 美国03, ... (共25个)    │  ← 节点预览
│  自动选择, 手动选择, ... (共5个)         │  ← 代理组预览
│                          [展开 ▼]       │  ← 展开按钮
├─────────────────────────────────────────┤
│  proxies:                               │  ← 展开后显示
│    - name: 香港01                       │     完整 YAML
│      type: ss                           │
│      server: hk01.example.com           │
│  ...                                    │
└─────────────────────────────────────────┘
```

### BuildRecordDetailView.vue（修改）

- 步骤详情区域使用 ConfigCard 替代当前的 `formatJson()` 展示
- 步骤输入中的 `subscriptionName`、`instanceName`、`scriptName` 显示为文本标签
- 不同步骤根据 input/output 结构选择合适的展示方式

## Backend Changes

### BuildPipelineServiceImpl.java

修改 `execute()` 方法中各步骤的 input/output 设置：

1. 拉取主订阅后，调用 `buildConfigSummary()` 和 `configToYaml()` 构建 output 对象
2. 合并额外节点时，收集各额外订阅的配置摘要和 YAML 作为 input，合并后的配置作为 output
3. 脚本处理时，将合并后的配置作为 input，处理后的配置作为 output
4. 推送时，将最终配置作为 input，推送结果作为 output

### 辅助方法

新增 `buildConfigSummary()` 和 `configToYaml()` 两个私有辅助方法。

## Risks

- **存储空间**: 保存完整 YAML 会增大构建记录文件大小。可通过限制保留的构建记录数量来控制。
- **旧数据兼容**: 旧的构建记录 input/output 仍是简单字符串，前端需兼容处理。
- **大配置性能**: 超大配置（数千节点）的 YAML 渲染可能较慢，可用虚拟滚动或懒加载优化。
