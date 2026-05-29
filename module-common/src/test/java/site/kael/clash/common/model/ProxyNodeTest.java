package site.kael.clash.common.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProxyNodeTest {

    @Test
    void testCreateProxyNode() {
        ProxyNode node = new ProxyNode("node1", "vmess", "1.2.3.4", 443);
        assertEquals("node1", node.getName());
        assertEquals("vmess", node.getType());
        assertEquals("1.2.3.4", node.getServer());
        assertEquals(443, node.getPort());
    }

    @Test
    void testExtraMap() {
        ProxyNode node = new ProxyNode();
        node.getExtra().put("uuid", "test-uuid");
        assertEquals("test-uuid", node.getExtra().get("uuid"));
    }
}
