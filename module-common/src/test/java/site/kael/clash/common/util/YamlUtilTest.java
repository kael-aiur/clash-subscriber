package site.kael.clash.common.util;

import org.junit.jupiter.api.Test;
import site.kael.clash.common.model.ClashConfig;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlUtilTest {

    @Test
    void testParseClashConfig() {
        String yaml = """
                proxies:
                  - name: node1
                    type: vmess
                    server: 1.2.3.4
                    port: 443
                """;
        ClashConfig config = YamlUtil.parseClashConfig(yaml);
        assertEquals(1, config.getProxies().size());
        assertEquals("node1", config.getProxies().get(0).getName());
    }

    @Test
    void testParseFullConfig() {
        String yaml = """
                name: MyClash
                proxies:
                  - name: node1
                    type: vmess
                    server: 1.2.3.4
                    port: 443
                  - name: node2
                    type: ss
                    server: 5.6.7.8
                    port: 8388
                proxy-groups:
                  - name: Proxy
                    type: select
                    proxies:
                      - node1
                      - node2
                  - name: Auto
                    type: url-test
                    url: http://www.gstatic.com/generate_204
                    interval: 300
                    proxies:
                      - node1
                      - node2
                rules:
                  - DOMAIN-SUFFIX,google.com,Proxy
                  - DOMAIN-KEYWORD,facebook,Proxy
                  - MATCH,Proxy
                """;
        ClashConfig config = YamlUtil.parseClashConfig(yaml);

        // name
        assertEquals("MyClash", config.getName());

        // proxies
        assertEquals(2, config.getProxies().size());
        assertEquals("node1", config.getProxies().get(0).getName());
        assertEquals("node2", config.getProxies().get(1).getName());

        // proxy-groups（列表转 Map，key 为组名）
        Map<String, Object> groups = config.getProxyGroups();
        assertEquals(2, groups.size());
        assertTrue(groups.containsKey("Proxy"));
        assertTrue(groups.containsKey("Auto"));
        Map<String, Object> proxyGroup = (Map<String, Object>) groups.get("Proxy");
        assertEquals("select", proxyGroup.get("type"));
        Map<String, Object> autoGroup = (Map<String, Object>) groups.get("Auto");
        assertEquals("url-test", autoGroup.get("type"));

        // rules
        List<Object> rules = config.getRules();
        assertEquals(3, rules.size());
        assertEquals("DOMAIN-SUFFIX,google.com,Proxy", rules.get(0));
        assertEquals("MATCH,Proxy", rules.get(2));
    }

    @Test
    void testParseConfigMissingOptionalFields() {
        String yaml = """
                proxies:
                  - name: node1
                    type: vmess
                    server: 1.2.3.4
                    port: 443
                """;
        ClashConfig config = YamlUtil.parseClashConfig(yaml);

        // 缺少 name、proxy-groups、rules 时不抛异常，使用默认值
        assertNull(config.getName());
        assertTrue(config.getProxyGroups().isEmpty());
        assertTrue(config.getRules().isEmpty());
        // proxies 正常解析
        assertEquals(1, config.getProxies().size());
    }
}
