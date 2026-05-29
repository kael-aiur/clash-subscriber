package site.kael.clash.common.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClashConfigTest {

    @Test
    void testCreateClashConfig() {
        ClashConfig config = new ClashConfig("test-config");
        assertEquals("test-config", config.getName());
        assertTrue(config.getProxies().isEmpty());
        assertTrue(config.getRules().isEmpty());
    }

    @Test
    void testAddProxy() {
        ClashConfig config = new ClashConfig();
        config.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));
        assertEquals(1, config.getProxies().size());
    }
}
