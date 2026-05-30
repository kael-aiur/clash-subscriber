## ADDED Requirements

### Requirement: ScriptEngine SHALL 封装全部脚本执行逻辑

ScriptEngine 类 SHALL 提供单一公开方法 `execute(String scriptContent, ClashConfig config, String profileName)`，接收脚本内容、配置和配置名称，返回修改后的 ClashConfig。调用方无需了解内部的 JSON 转换和 JS 引擎细节。

#### Scenario: 调用方通过 execute 方法执行脚本
- **WHEN** 传入脚本内容、ClashConfig 和 profileName
- **THEN** 返回修改后的 ClashConfig 对象，内部的 JSON 转换和 GraalVM 调用对调用方透明

---

### Requirement: configToJson SHALL 将 ClashConfig 转为 Verge 规范的 JSON

configToJson 方法 SHALL 将 ClashConfig 转为 JSON 字符串，遵循以下规则：
- `proxyGroups` (Map) SHALL 转为 `proxy-groups` Array 格式，每项包含 `name` 字段
- `proxies` (List<ProxyNode>) SHALL 转为 Array of Maps
- `rules` (List<Object>) SHALL 转为 Array
- 所有顶层 key SHALL 转为小写

#### Scenario: proxy-groups 从 Map 转为 Array
- **WHEN** ClashConfig 的 proxyGroups 为 `{"Auto": {type: "url-test", proxies: [...]}}`
- **THEN** JSON 中 `proxy-groups` 为 `[{name: "Auto", type: "url-test", proxies: [...]}]`

#### Scenario: proxies 从 ProxyNode 转为 Map 数组
- **WHEN** ClashConfig 的 proxies 包含 ProxyNode 对象
- **THEN** JSON 中 `proxies` 为包含 name、type、server、port 及 extra 字段的 Map 数组

#### Scenario: 顶层 key 小写化
- **WHEN** ClashConfig 的 raw 中包含大写 key（如 `Proxy-Groups`）
- **THEN** JSON 中所有顶层 key SHALL 为小写（如 `proxy-groups`）

---

### Requirement: jsonToConfig SHALL 将 JSON 反序列化为 ClashConfig

jsonToConfig 方法 SHALL 将脚本返回的 JSON 字符串解析为 ClashConfig 对象，遵循以下规则：
- `proxy-groups` (Array) SHALL 转回 `proxyGroups` Map 格式，以 `name` 作为 key
- `proxies` (Array of Maps) SHALL 转回 `List<ProxyNode>`
- `rules` (Array) SHALL 转回 `List<Object>`

#### Scenario: proxy-groups 从 Array 转回 Map
- **WHEN** JSON 中 `proxy-groups` 为 `[{name: "Auto", type: "url-test"}]`
- **THEN** ClashConfig 的 proxyGroups 为 `{"Auto": {type: "url-test"}}`

#### Scenario: proxies 从 Map 数组转回 ProxyNode
- **WHEN** JSON 中 proxies 为 `[{name: "node1", type: "ss", server: "1.2.3.4", port: 443}]`
- **THEN** ClashConfig 的 proxies 包含对应的 ProxyNode 对象

---

### Requirement: execute SHALL 自动调用 main 函数作为脚本入口

execute 方法 SHALL 在执行脚本后检测是否存在 `main` 函数，若存在则自动调用 `main(config, profileName)` 并使用返回值作为结果。

#### Scenario: 脚本定义了 main 函数
- **WHEN** 脚本中定义了 `function main(config, profileName) { return config; }`
- **THEN** execute SHALL 调用 main 并返回其结果

#### Scenario: 脚本未定义 main 函数
- **WHEN** 脚本中未定义 main 函数
- **THEN** execute SHALL 直接使用 config 绑定作为结果

---

### Requirement: ScriptProcessor SHALL 委托 ScriptEngine 执行脚本

ScriptProcessor SHALL 简化为薄委托层，仅负责读取脚本文件和调用 ScriptEngine，不再包含 JSON 转换或格式处理逻辑。

#### Scenario: ScriptProcessor 执行脚本
- **WHEN** 收到脚本名称和 ClashConfig
- **THEN** 读取脚本文件后委托 ScriptEngine.execute 执行，返回 ClashConfig

#### Scenario: 脚本文件不存在
- **WHEN** 指定的脚本文件不存在
- **THEN** SHALL 抛出 BusinessException
