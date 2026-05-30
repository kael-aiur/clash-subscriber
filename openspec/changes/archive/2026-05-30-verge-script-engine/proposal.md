## Why

当前 `ScriptProcessor` 将 GraalVM 引擎交互、JSON 序列化、proxy-groups 格式转换等逻辑混在一起，职责不清晰且难以维护。更关键的是，config 传递方式未遵循 Clash Verge 脚本协议（应为 JSON 对象而非 Java 代理），导致脚本中 `proxy-groups` 等字段的类型与 Verge 规范不一致，兼容性问题频发。需要独立抽象脚本执行引擎，将协议转换封装在引擎内部。

## What Changes

**ScriptEngine 引擎类（新增）**
- 新增 `ScriptEngine` 类，封装全部脚本执行逻辑
- 内部处理 ClashConfig↔JSON 双向转换（proxy-groups Map↔Array、key 小写化）
- 执行 JS 脚本并调用 `main(config, profileName)` 入口函数
- 对外暴露单一方法：`execute(scriptContent, config, profileName) → ClashConfig`

**ScriptProcessor 改造（修改）**
- From: 包含 JSON 转换、格式处理、JS 引擎交互等全部逻辑
- To: 简化为薄委托层，只负责读取脚本文件、调用 ScriptEngine、返回结果
- Reason: 职责分离，便于测试和维护
- Impact: 非破坏性变更，接口不变

**BuildPipelineServiceImpl 清理（修改）**
- From: 手动调用 mapToGroupArray 做格式转换
- To: 移除冗余的 proxy-groups 转换逻辑，由 ScriptEngine 统一处理
- Reason: 消除重复代码
- Impact: 非破坏性变更

## Capabilities

### New Capabilities
- `script-engine`: 脚本执行引擎，封装 ClashConfig↔JSON 转换和 GraalVM JS 执行，兼容 Clash Verge 脚本协议

### Modified Capabilities
（无现有 spec 需要修改）

## Impact

- **代码文件**：`ScriptEngine.java`（新增）、`ScriptProcessor.java`（重构）、`BuildPipelineServiceImpl.java`（清理）
- **依赖**：无新增依赖，复用现有 GraalVM polyglot + Jackson
- **API**：无外部 API 变更
- **兼容性**：现有脚本无需修改即可工作
