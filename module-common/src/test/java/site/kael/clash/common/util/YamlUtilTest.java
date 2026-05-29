package site.kael.clash.common.util;

import org.junit.jupiter.api.Test;
import site.kael.clash.common.model.ClashConfig;
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
}
