package site.kael.clash.processor.builtin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.processor.api.ProcessingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProxyGroupProcessorTest {

    private ProxyGroupProcessor processor;
    private ProcessingContext context;

    @BeforeEach
    void setUp() {
        processor = new ProxyGroupProcessor();
        context = new ProcessingContext();
    }

    @Test
    void testName() {
        assertEquals("proxy-group", processor.getName());
    }

    @Test
    void testOrder() {
        assertEquals(300, processor.getOrder());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReplaceProxyGroups() {
        ClashConfig input = new ClashConfig("test");
        Map<String, Object> oldGroups = new HashMap<>();
        oldGroups.put("old-group", Map.of("type", "select", "proxies", List.of("node1")));
        input.setProxyGroups(oldGroups);

        Map<String, Object> proxyGroupConfig = new HashMap<>();
        List<Map<String, Object>> groups = new ArrayList<>();

        Map<String, Object> autoGroup = new HashMap<>();
        autoGroup.put("name", "auto");
        autoGroup.put("type", "url-test");
        autoGroup.put("proxies", List.of("node1", "node2"));
        groups.add(autoGroup);

        Map<String, Object> selectGroup = new HashMap<>();
        selectGroup.put("name", "proxy");
        selectGroup.put("type", "select");
        selectGroup.put("proxies", List.of("auto", "node1", "node2"));
        groups.add(selectGroup);

        proxyGroupConfig.put("groups", groups);
        context.setVariable("proxyGroupConfig", proxyGroupConfig);

        ClashConfig output = processor.process(input, context);

        Map<String, Object> outputGroups = output.getProxyGroups();
        assertEquals(2, outputGroups.size());
        assertTrue(outputGroups.containsKey("auto"));
        assertTrue(outputGroups.containsKey("proxy"));
        assertFalse(outputGroups.containsKey("old-group"));

        Map<String, Object> outputAutoGroup = (Map<String, Object>) outputGroups.get("auto");
        assertEquals("url-test", outputAutoGroup.get("type"));
        List<String> autoProxies = (List<String>) outputAutoGroup.get("proxies");
        assertEquals(2, autoProxies.size());
    }

    @Test
    void testGroupWithExtraFields() {
        ClashConfig input = new ClashConfig("test");

        Map<String, Object> proxyGroupConfig = new HashMap<>();
        List<Map<String, Object>> groups = new ArrayList<>();

        Map<String, Object> autoGroup = new HashMap<>();
        autoGroup.put("name", "auto");
        autoGroup.put("type", "url-test");
        autoGroup.put("proxies", List.of("node1"));
        autoGroup.put("url", "http://www.gstatic.com/generate_204");
        autoGroup.put("interval", 300);
        groups.add(autoGroup);

        proxyGroupConfig.put("groups", groups);
        context.setVariable("proxyGroupConfig", proxyGroupConfig);

        ClashConfig output = processor.process(input, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> outputAutoGroup = (Map<String, Object>) output.getProxyGroups().get("auto");
        assertEquals("http://www.gstatic.com/generate_204", outputAutoGroup.get("url"));
        assertEquals(300, outputAutoGroup.get("interval"));
    }

    @Test
    void testNoProxyGroupConfig() {
        ClashConfig input = new ClashConfig("test");
        Map<String, Object> oldGroups = new HashMap<>();
        oldGroups.put("existing", Map.of("type", "select"));
        input.setProxyGroups(oldGroups);

        ClashConfig output = processor.process(input, context);

        assertEquals(1, output.getProxyGroups().size());
        assertFalse(context.getLogs().isEmpty());
    }

    @Test
    void testNoGroupsList() {
        ClashConfig input = new ClashConfig("test");

        Map<String, Object> proxyGroupConfig = new HashMap<>();
        context.setVariable("proxyGroupConfig", proxyGroupConfig);

        ClashConfig output = processor.process(input, context);

        assertTrue(output.getProxyGroups().isEmpty());
    }

    @Test
    void testDoesNotMutateInput() {
        ClashConfig input = new ClashConfig("test");
        Map<String, Object> oldGroups = new HashMap<>();
        oldGroups.put("existing", Map.of("type", "select"));
        input.setProxyGroups(oldGroups);

        Map<String, Object> proxyGroupConfig = new HashMap<>();
        List<Map<String, Object>> groups = new ArrayList<>();
        groups.add(Map.of("name", "new", "type", "select", "proxies", List.of("node1")));
        proxyGroupConfig.put("groups", groups);
        context.setVariable("proxyGroupConfig", proxyGroupConfig);

        int originalSize = input.getProxyGroups().size();
        processor.process(input, context);

        assertEquals(originalSize, input.getProxyGroups().size());
    }

    @Test
    void testLogMessage() {
        ClashConfig input = new ClashConfig("test");

        Map<String, Object> proxyGroupConfig = new HashMap<>();
        List<Map<String, Object>> groups = new ArrayList<>();
        groups.add(Map.of("name", "auto", "type", "url-test", "proxies", List.of("node1")));
        groups.add(Map.of("name", "proxy", "type", "select", "proxies", List.of("auto")));
        proxyGroupConfig.put("groups", groups);
        context.setVariable("proxyGroupConfig", proxyGroupConfig);

        processor.process(input, context);

        assertFalse(context.getLogs().isEmpty());
        assertTrue(context.getLogs().get(0).contains("设置 2 个代理组"));
    }
}
