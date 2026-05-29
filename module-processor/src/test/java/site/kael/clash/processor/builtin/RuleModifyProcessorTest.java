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

class RuleModifyProcessorTest {

    private RuleModifyProcessor processor;
    private ProcessingContext context;

    @BeforeEach
    void setUp() {
        processor = new RuleModifyProcessor();
        context = new ProcessingContext();
    }

    @Test
    void testName() {
        assertEquals("rule-modify", processor.getName());
    }

    @Test
    void testOrder() {
        assertEquals(200, processor.getOrder());
    }

    @Test
    void testAddRules() {
        ClashConfig input = new ClashConfig("test");
        input.getRules().add("DOMAIN-SUFFIX,google.com,PROXY");

        Map<String, Object> ruleModify = new HashMap<>();
        ruleModify.put("add", List.of("DOMAIN-SUFFIX,youtube.com,PROXY", "DOMAIN-SUFFIX,github.com,PROXY"));
        context.setVariable("ruleModify", ruleModify);

        ClashConfig output = processor.process(input, context);

        assertEquals(3, output.getRules().size());
        assertEquals("DOMAIN-SUFFIX,google.com,PROXY", output.getRules().get(0));
        assertEquals("DOMAIN-SUFFIX,youtube.com,PROXY", output.getRules().get(1));
        assertEquals("DOMAIN-SUFFIX,github.com,PROXY", output.getRules().get(2));
    }

    @Test
    void testRemoveRules() {
        ClashConfig input = new ClashConfig("test");
        input.getRules().add("DOMAIN-SUFFIX,google.com,PROXY");
        input.getRules().add("DOMAIN-SUFFIX,youtube.com,PROXY");
        input.getRules().add("DOMAIN-SUFFIX,github.com,PROXY");

        Map<String, Object> ruleModify = new HashMap<>();
        ruleModify.put("remove", List.of("DOMAIN-SUFFIX,youtube.com,PROXY"));
        context.setVariable("ruleModify", ruleModify);

        ClashConfig output = processor.process(input, context);

        assertEquals(2, output.getRules().size());
        assertEquals("DOMAIN-SUFFIX,google.com,PROXY", output.getRules().get(0));
        assertEquals("DOMAIN-SUFFIX,github.com,PROXY", output.getRules().get(1));
    }

    @Test
    void testReplaceRules() {
        ClashConfig input = new ClashConfig("test");
        input.getRules().add("DOMAIN-SUFFIX,google.com,PROXY");
        input.getRules().add("DOMAIN-SUFFIX,youtube.com,PROXY");

        Map<String, Object> ruleModify = new HashMap<>();
        Map<String, String> replace = new HashMap<>();
        replace.put("DOMAIN-SUFFIX,google.com,PROXY", "DOMAIN-SUFFIX,google.com,DIRECT");
        ruleModify.put("replace", replace);
        context.setVariable("ruleModify", ruleModify);

        ClashConfig output = processor.process(input, context);

        assertEquals(2, output.getRules().size());
        assertEquals("DOMAIN-SUFFIX,google.com,DIRECT", output.getRules().get(0));
        assertEquals("DOMAIN-SUFFIX,youtube.com,PROXY", output.getRules().get(1));
    }

    @Test
    void testCombinedOperations() {
        ClashConfig input = new ClashConfig("test");
        input.getRules().add("DOMAIN-SUFFIX,google.com,PROXY");
        input.getRules().add("DOMAIN-SUFFIX,youtube.com,PROXY");
        input.getRules().add("DOMAIN-SUFFIX,github.com,PROXY");

        Map<String, Object> ruleModify = new HashMap<>();
        ruleModify.put("remove", List.of("DOMAIN-SUFFIX,youtube.com,PROXY"));
        Map<String, String> replace = new HashMap<>();
        replace.put("DOMAIN-SUFFIX,google.com,PROXY", "DOMAIN-SUFFIX,google.com,DIRECT");
        ruleModify.put("replace", replace);
        ruleModify.put("add", List.of("DOMAIN-SUFFIX,stackoverflow.com,PROXY"));
        context.setVariable("ruleModify", ruleModify);

        ClashConfig output = processor.process(input, context);

        assertEquals(3, output.getRules().size());
        assertTrue(output.getRules().contains("DOMAIN-SUFFIX,google.com,DIRECT"));
        assertTrue(output.getRules().contains("DOMAIN-SUFFIX,github.com,PROXY"));
        assertTrue(output.getRules().contains("DOMAIN-SUFFIX,stackoverflow.com,PROXY"));
        assertFalse(output.getRules().contains("DOMAIN-SUFFIX,youtube.com,PROXY"));
    }

    @Test
    void testNoRuleModifyConfig() {
        ClashConfig input = new ClashConfig("test");
        input.getRules().add("DOMAIN-SUFFIX,google.com,PROXY");

        ClashConfig output = processor.process(input, context);

        assertEquals(1, output.getRules().size());
        assertFalse(context.getLogs().isEmpty());
    }

    @Test
    void testRemoveNonExistentRule() {
        ClashConfig input = new ClashConfig("test");
        input.getRules().add("DOMAIN-SUFFIX,google.com,PROXY");

        Map<String, Object> ruleModify = new HashMap<>();
        ruleModify.put("remove", List.of("DOMAIN-SUFFIX,nonexistent.com,PROXY"));
        context.setVariable("ruleModify", ruleModify);

        ClashConfig output = processor.process(input, context);

        assertEquals(1, output.getRules().size());
    }

    @Test
    void testReplaceNonExistentRule() {
        ClashConfig input = new ClashConfig("test");
        input.getRules().add("DOMAIN-SUFFIX,google.com,PROXY");

        Map<String, Object> ruleModify = new HashMap<>();
        Map<String, String> replace = new HashMap<>();
        replace.put("DOMAIN-SUFFIX,nonexistent.com,PROXY", "DOMAIN-SUFFIX,nonexistent.com,DIRECT");
        ruleModify.put("replace", replace);
        context.setVariable("ruleModify", ruleModify);

        ClashConfig output = processor.process(input, context);

        assertEquals(1, output.getRules().size());
        assertEquals("DOMAIN-SUFFIX,google.com,PROXY", output.getRules().get(0));
    }

    @Test
    void testDoesNotMutateInput() {
        ClashConfig input = new ClashConfig("test");
        input.getRules().add("DOMAIN-SUFFIX,google.com,PROXY");

        Map<String, Object> ruleModify = new HashMap<>();
        ruleModify.put("add", List.of("DOMAIN-SUFFIX,youtube.com,PROXY"));
        context.setVariable("ruleModify", ruleModify);

        int originalSize = input.getRules().size();
        processor.process(input, context);

        assertEquals(originalSize, input.getRules().size());
    }

    @Test
    void testLogMessage() {
        ClashConfig input = new ClashConfig("test");
        input.getRules().add("DOMAIN-SUFFIX,google.com,PROXY");

        Map<String, Object> ruleModify = new HashMap<>();
        ruleModify.put("add", List.of("DOMAIN-SUFFIX,youtube.com,PROXY"));
        ruleModify.put("remove", List.of("DOMAIN-SUFFIX,google.com,PROXY"));
        context.setVariable("ruleModify", ruleModify);

        processor.process(input, context);

        assertFalse(context.getLogs().isEmpty());
        assertTrue(context.getLogs().get(0).contains("添加 1 条"));
        assertTrue(context.getLogs().get(0).contains("删除 1 条"));
    }
}
