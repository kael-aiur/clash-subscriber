package site.kael.clash.processor.builtin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.api.ConfigProcessor;
import site.kael.clash.processor.api.ProcessingContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 脚本处理器：使用 GraalVM JS 引擎执行自定义 JavaScript 脚本
 * 兼容 Clash Verge 规范：若脚本定义了 main(config, profileName) 函数则自动调用作为入口
 * 否则直接从 config 绑定中读取处理结果
 */
@Component
public class ScriptProcessor implements ConfigProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScriptProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${data.path:data}")
    private String dataPath;

    @Override
    public String getName() {
        return "script";
    }

    @Override
    public int getOrder() {
        return 999;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ClashConfig process(ClashConfig input, ProcessingContext context) {
        Object scriptNameObj = context.getVariable("scriptName");
        if (!(scriptNameObj instanceof String scriptName) || scriptName.isBlank()) {
            throw new BusinessException("未指定脚本名称");
        }

        // 创建输入的拷贝，避免修改原始配置
        ClashConfig configCopy = copyConfig(input);

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

        // 构建脚本规范的 config 对象（proxy-groups 为 Array 格式）
        Map<String, Object> scriptConfig = new LinkedHashMap<>(configCopy.getRaw());
        scriptConfig.put("proxies", configCopy.getProxies().stream().map(this::proxyNodeToMap).toList());
        scriptConfig.put("proxy-groups", mapToGroupArray(configCopy.getProxyGroups()));
        if (configCopy.getRules() != null && !configCopy.getRules().isEmpty()) {
            scriptConfig.put("rules", configCopy.getRules());
        }

        // 使用 GraalVM JS 引擎执行脚本
        log.info("执行脚本: {}", scriptName);
        context.addLog("开始执行脚本: " + scriptName);

        try (Context jsContext = Context.create("js")) {
            org.graalvm.polyglot.Value bindings = jsContext.getBindings("js");

            // 将 config 序列化为 JSON，脚本中解析为纯 JS 对象，避免 Java 代理差异
            String configJson = objectMapper.writeValueAsString(scriptConfig);
            bindings.putMember("configJson", configJson);

            // 加载脚本并注入 JSON 解析
            jsContext.eval("js", "var config = JSON.parse(configJson);");
            jsContext.eval("js", scriptContent);

            // 兼容 Clash Verge 规范：若脚本定义了 main 函数则自动调用作为入口
            org.graalvm.polyglot.Value mainFunc = bindings.getMember("main");
            org.graalvm.polyglot.Value resultValue;
            if (mainFunc != null && mainFunc.canExecute()) {
                resultValue = mainFunc.execute(bindings.getMember("config"), "");
            } else {
                resultValue = bindings.getMember("config");
            }

            // 将脚本结果序列化为 JSON，再反序列化为 Java 对象
            String resultJson = jsContext.eval("js", "JSON.stringify(config)").asString();
            Map<String, Object> resultMap = objectMapper.readValue(resultJson, new TypeReference<>() {});

            ClashConfig result = new ClashConfig(configCopy.getName());
            result.setRaw(resultMap);

            // 还原 proxies
            if (resultMap.get("proxies") instanceof List<?> proxiesList) {
                List<ProxyNode> proxies = new ArrayList<>();
                for (Object obj : proxiesList) {
                    if (obj instanceof Map<?, ?> proxyMap) {
                        proxies.add(mapToProxyNode((Map<String, Object>) proxyMap));
                    }
                }
                result.setProxies(proxies);
            }

            // 还原 proxy-groups（Array → Map）
            if (resultMap.get("proxy-groups") instanceof List<?> groupsList) {
                result.setProxyGroups(groupArrayToMap(groupsList));
            }

            // 还原 rules
            if (resultMap.get("rules") instanceof List<?> rulesList) {
                result.setRules(new ArrayList<>(rulesList));
            }

            String message = String.format("脚本执行完成: %s", scriptName);
            log.info(message);
            context.addLog(message);

            return result;
        } catch (JsonProcessingException e) {
            String errorMsg = String.format("脚本结果序列化失败: %s, %s", scriptName, e.getMessage());
            log.error(errorMsg, e);
            throw new BusinessException(errorMsg);
        } catch (Exception e) {
            String errorMsg = String.format("脚本执行失败: %s, %s", scriptName, e.getMessage());
            log.error(errorMsg, e);
            throw new BusinessException(errorMsg);
        }
    }

    /**
     * 将 proxy-groups Map 转为脚本规范的 Array 格式
     * Map: {"组名": {type, proxies, ...}} → Array: [{name, type, proxies, ...}]
     */
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

    /**
     * 将脚本输出的 proxy-groups Array 转回 Map 格式
     * Array: [{name, type, proxies, ...}] → Map: {"组名": {type, proxies, ...}}
     */
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
        node.setPort((Integer) map.get("port"));
        Map<String, Object> extra = new HashMap<>(map);
        extra.remove("name");
        extra.remove("type");
        extra.remove("server");
        extra.remove("port");
        node.setExtra(extra);
        return node;
    }

    /**
     * 深拷贝 ClashConfig
     */
    private ClashConfig copyConfig(ClashConfig input) {
        ClashConfig copy = new ClashConfig(input.getName());
        copy.setProxies(new ArrayList<>(input.getProxies()));
        copy.setProxyGroups(new HashMap<>(input.getProxyGroups()));
        copy.setRules(new ArrayList<>(input.getRules()));
        copy.setRaw(new HashMap<>(input.getRaw()));
        return copy;
    }
}
