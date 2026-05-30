package site.kael.clash.processor.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 脚本执行引擎：封装 GraalVM JS 引擎的脚本执行逻辑
 * <p>
 * 兼容 Clash Verge 规范：
 * - 将 ClashConfig 转为脚本规范的 JSON（proxy-groups 为 Array 格式）
 * - 在 JS 中解析为 config 对象，调用 main(config, profileName) 入口函数
 * - 将脚本输出的 JSON 还原为 ClashConfig
 */
@Component
public class ScriptEngine {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行 JavaScript 脚本，处理 ClashConfig 配置
     *
     * @param scriptContent 脚本内容
     * @param config        输入配置
     * @param profileName   配置名称，传递给脚本的 main 函数
     * @return 脚本处理后的配置
     * @throws BusinessException 脚本执行失败时抛出
     */
    public ClashConfig execute(String scriptContent, ClashConfig config, String profileName) {
        String safeName = profileName == null ? "" : profileName.replace("'", "\\'");
        String configJson = configToJson(config);

        try (Context jsContext = Context.create("js")) {
            Value bindings = jsContext.getBindings("js");
            bindings.putMember("configJson", configJson);

            jsContext.eval("js", "var config = JSON.parse(configJson);");
            jsContext.eval("js", scriptContent);

            // 兼容 Clash Verge 规范：若脚本定义了 main 函数则自动调用作为入口
            Value mainFunc = bindings.getMember("main");
            if (mainFunc != null && mainFunc.canExecute()) {
                mainFunc.execute(bindings.getMember("config"), safeName);
            }

            String resultJson = jsContext.eval("js", "JSON.stringify(config)").asString();
            return jsonToConfig(resultJson, config.getName());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("脚本执行失败: " + e.getMessage());
        }
    }

    /**
     * 将 ClashConfig 转为脚本规范的 JSON 字符串
     * <ul>
     *   <li>proxy-groups: Map → Array（每项添加 name 字段）</li>
     *   <li>proxies: List&lt;ProxyNode&gt; → Array of Maps</li>
     *   <li>rules: List&lt;Object&gt; → Array</li>
     *   <li>顶层 key 统一小写</li>
     *   <li>其他 raw 数据（dns, mode 等）保留</li>
     * </ul>
     */
    String configToJson(ClashConfig config) {
        try {
            Map<String, Object> scriptConfig = new LinkedHashMap<>();

            // 将 raw 数据的 key 统一小写
            for (Map.Entry<String, Object> entry : config.getRaw().entrySet()) {
                scriptConfig.put(entry.getKey().toLowerCase(), entry.getValue());
            }

            // proxies: List<ProxyNode> → Array of Maps
            scriptConfig.put("proxies", config.getProxies().stream()
                    .map(this::proxyNodeToMap)
                    .toList());

            // proxy-groups: 转为 Array 格式（脚本和 Verge 协议要求 Array）
            // 优先使用 typed 字段（处理器可能已修改），否则从 raw 中转换
            if (config.getProxyGroups() != null && !config.getProxyGroups().isEmpty()) {
                scriptConfig.put("proxy-groups", mapToGroupArray(config.getProxyGroups()));
            } else if (scriptConfig.get("proxy-groups") instanceof Map<?, ?> rawGroups) {
                scriptConfig.put("proxy-groups", mapToGroupArray((Map<String, Object>) rawGroups));
            }

            // rules
            if (config.getRules() != null && !config.getRules().isEmpty()) {
                scriptConfig.put("rules", config.getRules());
            }

            return objectMapper.writeValueAsString(scriptConfig);
        } catch (JsonProcessingException e) {
            throw new BusinessException("配置序列化失败: " + e.getMessage());
        }
    }

    /**
     * 将脚本输出的 JSON 字符串还原为 ClashConfig
     * <ul>
     *   <li>proxy-groups: Array → Map（移除 name 字段，用作 key）</li>
     *   <li>proxies: Array of Maps → List&lt;ProxyNode&gt;</li>
     *   <li>rules: Array → List&lt;Object&gt;</li>
     * </ul>
     */
    ClashConfig jsonToConfig(String json, String configName) {
        try {
            Map<String, Object> resultMap = objectMapper.readValue(json, new TypeReference<>() {});
            ClashConfig result = new ClashConfig(configName);
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

            return result;
        } catch (JsonProcessingException e) {
            throw new BusinessException("脚本结果解析失败: " + e.getMessage());
        }
    }

    /**
     * ProxyNode → Map（name, type, server, port + extra 字段）
     */
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

    /**
     * Map → ProxyNode（提取 name, type, server, port，其余放入 extra）
     */
    private ProxyNode mapToProxyNode(Map<String, Object> map) {
        ProxyNode node = new ProxyNode();
        node.setName((String) map.get("name"));
        node.setType((String) map.get("type"));
        node.setServer((String) map.get("server"));
        Object portObj = map.get("port");
        node.setPort(portObj instanceof Number n ? n.intValue() : 0);
        Map<String, Object> extra = new HashMap<>(map);
        extra.remove("name");
        extra.remove("type");
        extra.remove("server");
        extra.remove("port");
        node.setExtra(extra);
        return node;
    }

    /**
     * proxy-groups Map → Array 格式
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
     * proxy-groups Array → Map 格式
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
}
