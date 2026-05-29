package site.kael.clash.common.util;

import org.yaml.snakeyaml.Yaml;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;

import java.util.List;
import java.util.Map;

public class YamlUtil {
    private static final Yaml yaml = new Yaml();

    public static Map<String, Object> parseYaml(String content) {
        return yaml.load(content);
    }

    public static ClashConfig parseClashConfig(String content) {
        Map<String, Object> data = yaml.load(content);
        ClashConfig config = new ClashConfig();
        config.setRaw(data);

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

        return config;
    }
}
