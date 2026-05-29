package site.kael.clash.processor.builtin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.api.ProcessingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NodeMergeProcessorTest {

    private NodeMergeProcessor processor;
    private ProcessingContext context;

    @BeforeEach
    void setUp() {
        processor = new NodeMergeProcessor();
        context = new ProcessingContext();
    }

    @Test
    void testName() {
        assertEquals("node-merge", processor.getName());
    }

    @Test
    void testOrder() {
        assertEquals(100, processor.getOrder());
    }

    @Test
    void testMergeProxies() {
        ClashConfig input = new ClashConfig("main");
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));

        ClashConfig merge1 = new ClashConfig("sub1");
        merge1.getProxies().add(new ProxyNode("node2", "vmess", "1.2.3.5", 443));
        merge1.getProxies().add(new ProxyNode("node3", "vmess", "1.2.3.6", 443));

        ClashConfig merge2 = new ClashConfig("sub2");
        merge2.getProxies().add(new ProxyNode("node4", "vmess", "1.2.3.7", 443));

        List<ClashConfig> mergeConfigs = new ArrayList<>();
        mergeConfigs.add(merge1);
        mergeConfigs.add(merge2);
        context.setVariable("mergeConfigs", mergeConfigs);

        ClashConfig output = processor.process(input, context);

        assertEquals(4, output.getProxies().size());
        assertEquals("node1", output.getProxies().get(0).getName());
        assertEquals("node2", output.getProxies().get(1).getName());
        assertEquals("node3", output.getProxies().get(2).getName());
        assertEquals("node4", output.getProxies().get(3).getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMergeProxyGroups() {
        ClashConfig input = new ClashConfig("main");
        Map<String, Object> inputGroups = new HashMap<>();
        Map<String, Object> autoGroup = new HashMap<>();
        autoGroup.put("type", "url-test");
        autoGroup.put("proxies", new ArrayList<>(List.of("node1")));
        inputGroups.put("auto", autoGroup);
        input.setProxyGroups(inputGroups);

        ClashConfig merge1 = new ClashConfig("sub1");
        Map<String, Object> mergeGroups = new HashMap<>();
        Map<String, Object> mergeAutoGroup = new HashMap<>();
        mergeAutoGroup.put("type", "url-test");
        mergeAutoGroup.put("proxies", new ArrayList<>(List.of("node2")));
        mergeGroups.put("auto", mergeAutoGroup);

        Map<String, Object> newGroup = new HashMap<>();
        newGroup.put("type", "select");
        newGroup.put("proxies", new ArrayList<>(List.of("node3")));
        mergeGroups.put("custom", newGroup);
        merge1.setProxyGroups(mergeGroups);

        List<ClashConfig> mergeConfigs = new ArrayList<>();
        mergeConfigs.add(merge1);
        context.setVariable("mergeConfigs", mergeConfigs);

        ClashConfig output = processor.process(input, context);

        // auto 组的代理应该合并
        Map<String, Object> outputGroups = output.getProxyGroups();
        assertTrue(outputGroups.containsKey("auto"));
        Map<String, Object> outputAutoGroup = (Map<String, Object>) outputGroups.get("auto");
        List<String> autoProxies = (List<String>) outputAutoGroup.get("proxies");
        assertEquals(2, autoProxies.size());
        assertTrue(autoProxies.contains("node1"));
        assertTrue(autoProxies.contains("node2"));

        // custom 组应该被添加
        assertTrue(outputGroups.containsKey("custom"));
    }

    @Test
    void testNoMergeConfigs() {
        ClashConfig input = new ClashConfig("main");
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));

        ClashConfig output = processor.process(input, context);

        assertEquals(1, output.getProxies().size());
        assertFalse(context.getLogs().isEmpty());
    }

    @Test
    void testDoesNotMutateInput() {
        ClashConfig input = new ClashConfig("main");
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));

        ClashConfig merge1 = new ClashConfig("sub1");
        merge1.getProxies().add(new ProxyNode("node2", "vmess", "1.2.3.5", 443));

        List<ClashConfig> mergeConfigs = new ArrayList<>();
        mergeConfigs.add(merge1);
        context.setVariable("mergeConfigs", mergeConfigs);

        int originalSize = input.getProxies().size();
        processor.process(input, context);

        assertEquals(originalSize, input.getProxies().size());
    }

    @Test
    void testLogMessage() {
        ClashConfig input = new ClashConfig("main");
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));

        ClashConfig merge1 = new ClashConfig("sub1");
        merge1.getProxies().add(new ProxyNode("node2", "vmess", "1.2.3.5", 443));

        List<ClashConfig> mergeConfigs = new ArrayList<>();
        mergeConfigs.add(merge1);
        context.setVariable("mergeConfigs", mergeConfigs);

        processor.process(input, context);

        assertFalse(context.getLogs().isEmpty());
        assertTrue(context.getLogs().get(0).contains("合并了 1 个配置源"));
        assertTrue(context.getLogs().get(0).contains("新增 1 个节点"));
    }
}
