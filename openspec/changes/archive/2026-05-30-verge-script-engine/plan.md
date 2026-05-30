# ScriptEngine 引擎重构实现计划

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 将脚本执行逻辑从 ScriptProcessor 中抽离，封装为独立的 ScriptEngine 类，兼容 Clash Verge 脚本协议。

**Architecture:** ScriptEngine 封装 ClashConfig↔JSON 双向转换和 GraalVM JS 执行。configToJson 将 ClashConfig 转为 Verge 规范 JSON（proxy-groups Array、key 小写化），jsonToConfig 将脚本返回的 JSON 解析回 ClashConfig。ScriptProcessor 简化为薄委托层。

**Tech Stack:** Java 21, GraalVM Polyglot 23.1.2, Jackson 2.15.4, Spring Boot 3.2.5

---

## Task 1: 创建 ScriptEngine 类

**Files:**
- Create: `module-processor/src/main/java/site/kael/clash/processor/engine/ScriptEngine.java`
- Read: `module-common/src/main/java/site/kael/clash/common/model/ClashConfig.java`
- Read: `module-common/src/main/java/site/kael/clash/common/model/ProxyNode.java`

- [ ] **Step 1:** 创建 ScriptEngine.java，定义类结构和公开方法

```java
package site.kael.clash.processor.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScriptEngine {

    private static final Logger log = LoggerFactory.getLogger(ScriptEngine.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行脚本，返回修改后的 ClashConfig
     * 兼容 Clash Verge 协议：config 转 JSON 传入，调用 main(config, profileName)
     */
    public ClashConfig execute(String scriptContent, ClashConfig config, String profileName) {
        // TODO: 实现
        throw new UnsupportedOperationException();
    }

    /**
     * ClashConfig → JSON 字符串（Verge 规范格式）
     */
    String configToJson(ClashConfig config) {
        // TODO: 实现
        throw new UnsupportedOperationException();
    }

    /**
     * JSON 字符串 → ClashConfig
     */
    ClashConfig jsonToConfig(String json, String name) {
        // TODO: 实现
        throw new UnsupportedOperationException();
    }
}
```

- [ ] **Step 2:** 编译验证类结构正确

Run: `mvn compile -pl module-processor -q`

- [ ] **Step 3:** 实现 configToJson 方法

将 `ScriptProcessor` 中的转换逻辑迁移到 `configToJson`：

```java
@SuppressWarnings("unchecked")
String configToJson(ClashConfig config) {
    try {
        Map<String, Object> scriptConfig = new LinkedHashMap<>();

        // 合并 raw 中的非代理数据（如 dns、mode 等），key 小写化
        for (Map.Entry<String, Object> entry : config.getRaw().entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (!key.equals("proxies") && !key.equals("proxy-groups") && !key.equals("rules")) {
                scriptConfig.put(key, entry.getValue());
            }
        }

        // proxies: ProxyNode → Map 数组
        scriptConfig.put("proxies", config.getProxies().stream()
                .map(this::proxyNodeToMap).toList());

        // proxy-groups: Map → Array（带 name 字段）
        scriptConfig.put("proxy-groups", mapToGroupArray(config.getProxyGroups()));

        // rules
        if (config.getRules() != null && !config.getRules().isEmpty()) {
            scriptConfig.put("rules", config.getRules());
        }

        return objectMapper.writeValueAsString(scriptConfig);
    } catch (JsonProcessingException e) {
        throw new BusinessException("配置序列化失败: " + e.getMessage());
    }
}
```

- [ ] **Step 4:** 实现 jsonToConfig 方法

```java
@SuppressWarnings("unchecked")
ClashConfig jsonToConfig(String json, String name) {
    try {
        Map<String, Object> resultMap = objectMapper.readValue(json, new TypeReference<>() {});

        ClashConfig config = new ClashConfig(name);
        config.setRaw(resultMap);

        // proxies: Map 数组 → ProxyNode
        if (resultMap.get("proxies") instanceof List<?> proxiesList) {
            List<ProxyNode> proxies = new ArrayList<>();
            for (Object obj : proxiesList) {
                if (obj instanceof Map<?, ?> proxyMap) {
                    proxies.add(mapToProxyNode((Map<String, Object>) proxyMap));
                }
            }
            config.setProxies(proxies);
        }

        // proxy-groups: Array → Map
        if (resultMap.get("proxy-groups") instanceof List<?> groupsList) {
            config.setProxyGroups(groupArrayToMap(groupsList));
        }

        // rules
        if (resultMap.get("rules") instanceof List<?> rulesList) {
            config.setRules(new ArrayList<>(rulesList));
        }

        return config;
    } catch (JsonProcessingException e) {
        throw new BusinessException("脚本结果解析失败: " + e.getMessage());
    }
}
```

- [ ] **Step 5:** 实现 execute 方法

```java
public ClashConfig execute(String scriptContent, ClashConfig config, String profileName) {
    String safeName = profileName == null ? "" : profileName.replace("'", "\\'");
    String configJson = configToJson(config);

    log.debug("脚本输入 JSON: {}", configJson);

    try (Context jsContext = Context.create("js")) {
        Value bindings = jsContext.getBindings("js");
        bindings.putMember("configJson", configJson);

        // 解析 JSON 为纯 JS 对象
        jsContext.eval("js", "var config = JSON.parse(configJson);");
        // 加载脚本（定义 main 等函数）
        jsContext.eval("js", scriptContent);

        // 自动调用 main(config, profileName) 作为入口
        Value mainFunc = bindings.getMember("main");
        if (mainFunc != null && mainFunc.canExecute()) {
            mainFunc.execute(bindings.getMember("config"), safeName);
        }

        // JSON.stringify 返回结果
        String resultJson = jsContext.eval("js", "JSON.stringify(config)").asString();
        log.debug("脚本输出 JSON: {}", resultJson);

        return jsonToConfig(resultJson, config.getName());
    } catch (BusinessException e) {
        throw e;
    } catch (Exception e) {
        throw new BusinessException("脚本执行失败: " + e.getMessage());
    }
}
```

- [ ] **Step 6:** 添加辅助方法（从 ScriptProcessor 迁移）

```java
private Map<String, Object> proxyNodeToMap(ProxyNode node) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", node.getName());
    map.put("type", node.getType());
    map.put("server", node.getServer());
    map.put("port", node.getPort());
    if (node.getExtra() != null) {
        map.putAll(node.getExtra());
    }
    return map;
}

private ProxyNode mapToProxyNode(Map<String, Object> map) {
    ProxyNode node = new ProxyNode();
    node.setName((String) map.get("name"));
    node.setType((String) map.get("type"));
    node.setServer((String) map.get("server"));
    Object portObj = map.get("port");
    node.setPort(portObj instanceof Number n ? n.intValue() : null);
    Map<String, Object> extra = new HashMap<>(map);
    extra.remove("name");
    extra.remove("type");
    extra.remove("server");
    extra.remove("port");
    node.setExtra(extra);
    return node;
}

@SuppressWarnings("unchecked")
private List<Map<String, Object>> mapToGroupArray(Map<String, Object> proxyGroups) {
    List<Map<String, Object>> list = new ArrayList<>();
    if (proxyGroups == null) return list;
    for (Map.Entry<String, Object> entry : proxyGroups.entrySet()) {
        if (entry.getValue() instanceof Map<?, ?> groupMap) {
            Map<String, Object> group = new LinkedHashMap<>((Map<String, Object>) groupMap);
            group.put("name", entry.getKey());
            list.add(group);
        }
    }
    return list;
}

@SuppressWarnings("unchecked")
private Map<String, Object> groupArrayToMap(List<?> groupsList) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (Object obj : groupsList) {
        if (obj instanceof Map<?, ?> groupMap) {
            String name = (String) groupMap.get("name");
            if (name != null) {
                Map<String, Object> group = new LinkedHashMap<>((Map<String, Object>) groupMap);
                group.remove("name");
                map.put(name, group);
            }
        }
    }
    return map;
}
```

- [ ] **Step 7:** 编译验证

Run: `mvn compile -pl module-processor -q`

- [ ] **Step 8:** 提交

```bash
git add module-processor/src/main/java/site/kael/clash/processor/engine/ScriptEngine.java
git commit -m "feat(processor): 新增 ScriptEngine 脚本执行引擎"
```

---

## Task 2: 重构 ScriptProcessor 委托 ScriptEngine

**Files:**
- Modify: `module-processor/src/main/java/site/kael/clash/processor/builtin/ScriptProcessor.java`

- [ ] **Step 1:** 重写 ScriptProcessor，注入 ScriptEngine，移除所有转换逻辑

```java
package site.kael.clash.processor.builtin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.processor.api.ConfigProcessor;
import site.kael.clash.processor.api.ProcessingContext;
import site.kael.clash.processor.engine.ScriptEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

@Component
public class ScriptProcessor implements ConfigProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScriptProcessor.class);

    private final ScriptEngine scriptEngine;

    @Value("${data.path:data}")
    private String dataPath;

    public ScriptProcessor(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    @Override
    public String getName() {
        return "script";
    }

    @Override
    public int getOrder() {
        return 999;
    }

    @Override
    public ClashConfig process(ClashConfig input, ProcessingContext context) {
        Object scriptNameObj = context.getVariable("scriptName");
        if (!(scriptNameObj instanceof String scriptName) || scriptName.isBlank()) {
            throw new BusinessException("未指定脚本名称");
        }

        // 加载脚本文件
        Path scriptPath = Paths.get(dataPath, "scripts", scriptName + ".js");
        if (!Files.exists(scriptPath)) {
            throw new BusinessException("脚本不存在: " + scriptName);
        }

        String scriptContent;
        try {
            scriptContent = Files.readString(scriptPath);
        } catch (IOException e) {
            throw new BusinessException("读取脚本失败: " + scriptName + ", " + e.getMessage());
        }

        // 拷贝配置，避免修改原始数据
        ClashConfig configCopy = copyConfig(input);

        // 委托 ScriptEngine 执行
        log.info("执行脚本: {}", scriptName);
        context.addLog("开始执行脚本: " + scriptName);

        ClashConfig result = scriptEngine.execute(scriptContent, configCopy, "");

        String message = String.format("脚本执行完成: %s", scriptName);
        log.info(message);
        context.addLog(message);

        return result;
    }

    private ClashConfig copyConfig(ClashConfig input) {
        ClashConfig copy = new ClashConfig(input.getName());
        copy.setProxies(new ArrayList<>(input.getProxies()));
        copy.setProxyGroups(new HashMap<>(input.getProxyGroups()));
        copy.setRules(new ArrayList<>(input.getRules()));
        copy.setRaw(new HashMap<>(input.getRaw()));
        return copy;
    }
}
```

- [ ] **Step 2:** 编译验证

Run: `mvn compile -q`

- [ ] **Step 3:** 提交

```bash
git add module-processor/src/main/java/site/kael/clash/processor/builtin/ScriptProcessor.java
git commit -m "refactor(processor): ScriptProcessor 委托 ScriptEngine 执行脚本"
```

---

## Task 3: 清理 BuildPipelineServiceImpl 冗余逻辑

**Files:**
- Read: `module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImpl.java`

- [ ] **Step 1:** 确认 BuildPipelineServiceImpl 中的 `mapToGroupArray` 是否仍需要

`syncRawFromFields` 中的 `mapToGroupArray` 是将 proxyGroups Map 转为 Array 格式写入 raw，用于 Mihomo 推送。这个逻辑在 ScriptEngine 内部的 `configToJson` 中也有，但 `syncRawFromFields` 是独立的推送路径，需要保留。

确认：保留 `syncRawFromFields` 中的 `mapToGroupArray`，不做修改。

- [ ] **Step 2:** 编译验证

Run: `mvn compile -q`

---

## Task 4: 端到端验证

- [ ] **Step 1:** 编译全项目

Run: `mvn compile -q`
Expected: 编译成功，无错误

- [ ] **Step 2:** 启动服务并执行包含 main 函数的脚本

执行构建流程，确认脚本正常执行并返回正确的 ClashConfig。

- [ ] **Step 3:** 提交最终代码

```bash
git add -A
git commit -m "feat(processor): 完成 ScriptEngine 引擎重构，兼容 Clash Verge 脚本协议"
```
