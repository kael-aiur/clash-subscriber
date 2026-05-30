## 1. 创建 ScriptEngine 类

- [x] 1.1 新建 `module-processor/.../processor/engine/ScriptEngine.java`，定义 `execute(scriptContent, config, profileName)` 公开方法
- [x] 1.2 实现 `configToJson(ClashConfig)` 方法：proxyGroups Map→Array、proxies ProxyNode→Map、rules 保留、key 小写化
- [x] 1.3 实现 `jsonToConfig(String json, String name)` 方法：proxy-groups Array→Map、proxies Map→ProxyNode、rules 保留
- [x] 1.4 实现 `execute` 方法：拼装 JS 代码调用 `main(config, profileName)`，JSON.stringify 返回，try/catch 错误处理

## 2. 重构 ScriptProcessor

- [x] 2.1 移除 ScriptProcessor 中的 JSON 转换、proxy-groups 格式转换等逻辑
- [x] 2.2 注入 ScriptEngine，将 process 方法改为委托调用：读脚本文件 → engine.execute → 返回结果

## 3. 清理 BuildPipelineServiceImpl

- [x] 3.1 移除 BuildPipelineServiceImpl 中冗余的 `mapToGroupArray` 方法（如有）
- [x] 3.2 确认 `syncRawFromFields` 中的 proxy-groups Array 转换逻辑保留（Mihomo 推送仍需要）

## 4. 验证

- [x] 4.1 编译通过 `mvn compile`
- [x] 4.2 执行包含 main 函数的脚本（如美国节点脚本），确认返回正确的 ClashConfig
- [x] 4.3 推送到 Mihomo 确认 YAML 格式正确（需手动验证）
