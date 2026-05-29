package site.kael.clash.processor.builtin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.api.ProcessingContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateRemoveProcessorTest {

    private DuplicateRemoveProcessor processor;
    private ProcessingContext context;

    @BeforeEach
    void setUp() {
        processor = new DuplicateRemoveProcessor();
        context = new ProcessingContext();
    }

    @Test
    void testName() {
        assertEquals("duplicate-remove", processor.getName());
    }

    @Test
    void testOrder() {
        assertEquals(50, processor.getOrder());
    }

    @Test
    void testRemoveDuplicates() {
        ClashConfig input = new ClashConfig("test");
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));
        input.getProxies().add(new ProxyNode("node2", "vmess", "1.2.3.5", 443));
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.6", 443)); // 重复
        input.getProxies().add(new ProxyNode("node3", "vmess", "1.2.3.7", 443));
        input.getProxies().add(new ProxyNode("node2", "vmess", "1.2.3.8", 443)); // 重复

        ClashConfig output = processor.process(input, context);

        assertEquals(3, output.getProxies().size());
        assertEquals("node1", output.getProxies().get(0).getName());
        assertEquals("node2", output.getProxies().get(1).getName());
        assertEquals("node3", output.getProxies().get(2).getName());
        // 保留第一个出现的节点地址
        assertEquals("1.2.3.4", output.getProxies().get(0).getServer());
        assertEquals("1.2.3.5", output.getProxies().get(1).getServer());
    }

    @Test
    void testNoDuplicates() {
        ClashConfig input = new ClashConfig("test");
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));
        input.getProxies().add(new ProxyNode("node2", "vmess", "1.2.3.5", 443));

        ClashConfig output = processor.process(input, context);

        assertEquals(2, output.getProxies().size());
    }

    @Test
    void testEmptyProxies() {
        ClashConfig input = new ClashConfig("test");

        ClashConfig output = processor.process(input, context);

        assertTrue(output.getProxies().isEmpty());
    }

    @Test
    void testDoesNotMutateInput() {
        ClashConfig input = new ClashConfig("test");
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.5", 443));

        int originalSize = input.getProxies().size();
        processor.process(input, context);

        assertEquals(originalSize, input.getProxies().size());
    }

    @Test
    void testLogMessage() {
        ClashConfig input = new ClashConfig("test");
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.5", 443));
        input.getProxies().add(new ProxyNode("node2", "vmess", "1.2.3.6", 443));

        processor.process(input, context);

        assertFalse(context.getLogs().isEmpty());
        assertTrue(context.getLogs().get(0).contains("移除重复节点 1"));
    }
}
