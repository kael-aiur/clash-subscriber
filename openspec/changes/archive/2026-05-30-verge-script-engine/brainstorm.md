# 脚本执行引擎设计探索

## 背景

当前 `ScriptProcessor` 直接在处理器中混杂了 JSON 序列化、proxy-groups 格式转换、GraalVM 引擎交互等逻辑，职责不清晰。需要独立抽象一个脚本执行引擎，按照 Clash Verge 脚本协议封装全部转换逻辑。

## Clash Verge Rev 脚本协议研究

来源：https://github.com/clash-verge-rev/clash-verge-rev

### 执行方式

- JS 引擎：Boa Engine（Rust 实现）
- 脚本入口：`main(config, profileName)` 函数
- 结果返回：`JSON.stringify(main(config, profileName) || '')`
- 错误处理：try/catch，错误前缀 `__error_flag__`

### Config 格式

Config 转为 JSON 对象传入 JS，所有 key 小写化：

- `proxy-groups`：**数组**，每项有 `name` 属性 `[{name, type, proxies, ...}]`
- `proxies`：**数组**，每项有 `name` 属性 `[{name, type, server, port, ...}]`
- `rules`：**字符串数组** `["MATCH,DIRECT"]`

关键文件：
- `src-tauri/src/enhance/script.rs` — 脚本执行引擎
- `src-tauri/src/enhance/field.rs` — key 小写化
- `src-tauri/src/utils/tmpl.rs` — 默认脚本模板

### 安全限制（暂不实现，仅参考）

| 参数 | 值 |
|------|------|
| 脚本超时 | 5 秒 |
| 最大 JSON | 10MB |
| 最大循环 | 1000 万次 |
| 最大日志条数 | 1000 |
| 最大日志总量 | 1MB |

## 当前问题

1. ScriptProcessor 职责过重：JSON 转换、格式处理、JS 引擎交互混在一起
2. proxy-groups 在 Java 中是 `Map<String, Object>`，脚本和 Mihomo 都期望 Array 格式
3. 转换逻辑散落在 ScriptProcessor 和 BuildPipelineServiceImpl 两处

## 设计决策

### 选择：独立 ScriptEngine 类

**接口设计**：
```java
public class ScriptEngine {
    public ClashConfig execute(String scriptContent, ClashConfig config, String profileName);
}
```

**内部方法**：
- `configToJson(ClashConfig)` — 序列化为 JSON，proxy-groups Map→Array，key 小写化
- `jsonToConfig(String json, String name)` — 反序列化为 ClashConfig，proxy-groups Array→Map

**执行流程**：
```
ClashConfig → configToJson → JSON string
    ↓
JS: var config = JSON.parse(json);
    result = main(config, 'profileName');
    JSON.stringify(result);
    ↓
JSON string → jsonToConfig → ClashConfig
```

**ScriptProcessor 简化为薄委托层**：
1. 读取脚本文件
2. 调用 engine.execute(scriptContent, configCopy, profileName)
3. 返回结果

### 关键转换规则

**configToJson**：
- `proxyGroups` (Map) → `proxy-groups` (Array，每项添加 name 字段)
- `proxies` (List<ProxyNode>) → `proxies` (Array of Maps)
- `rules` (List<Object>) → `rules` (Array)
- 所有顶层 key 小写化

**jsonToConfig**：
- `proxy-groups` (Array) → `proxyGroups` (Map，移除 name 作为 key)
- `proxies` (Array of Maps) → `proxies` (List<ProxyNode>)
- `rules` (Array) → `rules` (List<Object>)

### 不采用的方案

- 方案二（接口 + 实现）：当前只有一个 JS 引擎实现，过度抽象
- 方案三（仅重构方法）：职责仍混在处理器中，不利于后续扩展
