## Context

当前 `ScriptProcessor` 承担了过多职责：GraalVM 引擎交互、ClashConfig↔JSON 序列化、proxy-groups Map↔Array 格式转换、main 函数调用封装。这些逻辑混在一个类中，难以测试和维护。

Clash Verge Rev 作为 Clash 配置管理的事实标准，定义了脚本协议：config 转为 JSON（key 小写化、proxy-groups 为数组）传入 JS，调用 `main(config, profileName)`，JSON.stringify 返回结果。当前实现未完全遵循此协议。

**相关代码**：
- `module-processor/.../builtin/ScriptProcessor.java` — 当前脚本处理器
- `module-pipeline/.../impl/BuildPipelineServiceImpl.java` — 含 proxy-groups 格式转换

## Goals / Non-Goals

**Goals:**
- 抽象独立的 `ScriptEngine` 类，封装全部脚本执行逻辑
- 完全兼容 Clash Verge 脚本协议（config JSON 格式、main 入口、key 小写化）
- ScriptProcessor 简化为薄委托层，只负责文件读取和调用 engine
- 转换逻辑集中在 engine 内部，外部只关注 ClashConfig 输入输出

**Non-Goals:**
- 不实现安全限制（超时、循环次数、JSON 大小等），后续按需添加
- 不替换 GraalVM 引擎，保持现有依赖
- 不修改 PipelineService 或其他处理器的接口

## Decisions

### D1：独立 ScriptEngine 类而非接口+实现

- **选择**：创建具体类 `ScriptEngine`，不定义接口
- **理由**：当前只有一个 GraalVM JS 引擎实现，接口抽象无实际收益
- **已考虑 alternatives**：
  - 接口+GraalScriptEngine 实现：过度抽象，增加理解成本
  - 仅重构 ScriptProcessor 内部方法：职责仍混在处理器层

### D2：JSON 序列化作为 config 传递方式

- **选择**：ClashConfig → JSON string → JS 解析为纯对象
- **理由**：与 Verge 协议一致，避免 GraalVM Java 代理的兼容性问题（如 proxy-groups 作为 Map 被脚本当作 Array 访问）
- **已考虑 alternatives**：
  - 直接传递 Java 对象给 GraalVM：proxy-groups 类型不匹配，putMember 不可靠
  - 使用 GraalVM ProxyObject：复杂度高，收益不明确

### D3：proxy-groups 在 engine 内部自动转换

- **选择**：configToJson 时 Map→Array，jsonToConfig 时 Array→Map
- **理由**：Java 存储用 Map（键值查找），脚本和 Mihomo 都期望 Array（带 name 属性）。转换封装在 engine 内部，调用方无感知
- **已考虑 alternatives**：
  - 全局改用 Array 存储：破坏现有 Processor 链的数据格式
  - 在 BuildPipelineServiceImpl 中转换：逻辑分散，易遗漏

### D4：key 小写化遵循 Verge 规范

- **选择**：configToJson 时将所有顶层 key 转为小写
- **理由**：Verge 在传入脚本前执行 `use_lowercase()`，确保脚本中 key 一致
- **已考虑 alternatives**：不转换 key — 可能导致脚本中 key 大小写不一致

## Risks / Trade-offs

- [Risk] JSON 序列化/反序列化可能丢失 Java 特殊类型（如 Integer vs Long）→ Mitigation：使用 Jackson 的标准类型映射，Integer/Long 统一为 Number
- [Risk] ProxyNode 的 extra 字段中嵌套对象的类型可能不精确 → Mitigation：JSON 反序列化时统一用 Map<String, Object>，与现有 raw 处理一致
- [Trade-off] 暂不做安全限制 → 接受理由：当前使用场景为内部服务，脚本来源可控

## Migration Plan

N/A — 本 change 不涉及部署变更，仅重构内部实现。

**验收步骤**：
1. 编译通过 `mvn compile`
2. 现有脚本（如美国节点脚本）执行成功
3. 推送到 Mihomo 的 YAML 格式正确

## Open Questions

- profileName 参数：当前构建流程中传空字符串，后续是否需要支持从 BuildPipeline 传递实际名称？
