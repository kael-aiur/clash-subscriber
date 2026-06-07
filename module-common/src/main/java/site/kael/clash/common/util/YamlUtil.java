package site.kael.clash.common.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlUtil {
    private static final Yaml yaml;
    private static final Yaml yamlForDump;

    static {
        // 用于解析的 Yaml（默认配置）
        yaml = new Yaml();

        // 用于输出的 Yaml（配置 UTF-8 编码，避免中文乱码）
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        options.setIndent(2);
        yamlForDump = new Yaml(options);
    }

    public static Map<String, Object> parseYaml(String content) {
        return yaml.load(content);
    }

    public static String dump(Object data) {
        return yamlForDump.dump(data);
    }

    public static ClashConfig parseClashConfig(String content) {
        Map<String, Object> data = yaml.load(content);
        ClashConfig config = new ClashConfig();
        config.setRaw(data);

        if (data.containsKey("name")) {
            config.setName(String.valueOf(data.get("name")));
        }

        if (data.containsKey("proxies")) {
            List<Map<String, Object>> proxies = (List<Map<String, Object>>) data.get("proxies");
            for (Map<String, Object> proxy : proxies) {
                ProxyNode node = new ProxyNode();
                node.setName((String) proxy.get("name"));
                node.setType((String) proxy.get("type"));
                node.setServer((String) proxy.get("server"));
                node.setPort((Integer) proxy.get("port"));
                proxy.remove("name");
                proxy.remove("type");
                proxy.remove("server");
                proxy.remove("port");
                node.setExtra(proxy);
                config.getProxies().add(node);
            }
        }

        if (data.containsKey("proxy-groups")) {
            Object rawGroups = data.get("proxy-groups");
            if (rawGroups instanceof List) {
                List<Map<String, Object>> groupList = (List<Map<String, Object>>) rawGroups;
                Map<String, Object> groupMap = new LinkedHashMap<>();
                for (Map<String, Object> group : groupList) {
                    String name = (String) group.get("name");
                    if (name != null) {
                        groupMap.put(name, group);
                    }
                }
                config.setProxyGroups(groupMap);
            }
        }

        if (data.containsKey("rules")) {
            config.setRules((List<Object>) data.get("rules"));
        }

        return config;
    }
}
