package site.kael.clash.processor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.api.ConfigProcessor;
import site.kael.clash.processor.api.ProcessingContext;
import site.kael.clash.processor.model.PipelineConfig;
import site.kael.clash.processor.model.PipelineStep;
import site.kael.clash.processor.service.impl.PipelineServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PipelineServiceImplTest {

    private PipelineService pipelineService;
    private ConfigProcessor addNodeProcessor;
    private ConfigProcessor renameProcessor;

    @BeforeEach
    void setUp() {
        // 创建测试用处理器
        addNodeProcessor = new ConfigProcessor() {
            @Override
            public String getName() { return "add-node"; }

            @Override
            public int getOrder() { return 10; }

            @Override
            public ClashConfig process(ClashConfig input, ProcessingContext context) {
                String nodeName = (String) context.getVariable("nodeName");
                if (nodeName != null) {
                    input.getProxies().add(new ProxyNode(nodeName, "vmess", "1.2.3.4", 443));
                }
                return input;
            }
        };

        renameProcessor = new ConfigProcessor() {
            @Override
            public String getName() { return "rename"; }

            @Override
            public int getOrder() { return 20; }

            @Override
            public ClashConfig process(ClashConfig input, ProcessingContext context) {
                String prefix = (String) context.getVariable("prefix");
                if (prefix != null) {
                    for (ProxyNode node : input.getProxies()) {
                        node.setName(prefix + node.getName());
                    }
                }
                return input;
            }
        };

        List<ConfigProcessor> processors = List.of(addNodeProcessor, renameProcessor);
        pipelineService = new PipelineServiceImpl(processors);
    }

    @Test
    void testExecuteWithDefaultContext() {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("测试 Pipeline");

        PipelineStep step = new PipelineStep();
        step.setProcessor("add-node");
        step.setConfig(Map.of("nodeName", "test-node"));
        pipeline.setSteps(List.of(step));

        ClashConfig input = new ClashConfig("test");
        ClashConfig result = pipelineService.execute(pipeline, input);

        assertEquals(1, result.getProxies().size());
        assertEquals("test-node", result.getProxies().get(0).getName());
    }

    @Test
    void testExecuteWithCustomContext() {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("测试 Pipeline");

        PipelineStep step = new PipelineStep();
        step.setProcessor("add-node");
        step.setConfig(Map.of("nodeName", "ctx-node"));
        pipeline.setSteps(List.of(step));

        ClashConfig input = new ClashConfig("test");
        ProcessingContext context = new ProcessingContext();
        context.setVariable("extra", "value");

        ClashConfig result = pipelineService.execute(pipeline, input, context);

        assertEquals(1, result.getProxies().size());
        assertEquals("ctx-node", result.getProxies().get(0).getName());
        // 自定义上下文的变量应保留
        assertEquals("value", context.getVariable("extra"));
    }

    @Test
    void testExecuteMultipleSteps() {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("多步骤 Pipeline");

        // 步骤1: 添加节点
        PipelineStep step1 = new PipelineStep();
        step1.setProcessor("add-node");
        step1.setConfig(Map.of("nodeName", "original"));

        // 步骤2: 重命名
        PipelineStep step2 = new PipelineStep();
        step2.setProcessor("rename");
        step2.setConfig(Map.of("prefix", "hk-"));

        pipeline.setSteps(List.of(step1, step2));

        ClashConfig input = new ClashConfig("test");
        ClashConfig result = pipelineService.execute(pipeline, input);

        assertEquals(1, result.getProxies().size());
        assertEquals("hk-original", result.getProxies().get(0).getName());
    }

    @Test
    void testExecuteEmptySteps() {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("空 Pipeline");
        pipeline.setSteps(new ArrayList<>());

        ClashConfig input = new ClashConfig("test");
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));

        ClashConfig result = pipelineService.execute(pipeline, input);

        // 空步骤应直接返回输入
        assertEquals(1, result.getProxies().size());
        assertEquals("node1", result.getProxies().get(0).getName());
    }

    @Test
    void testExecuteNullSteps() {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("null steps Pipeline");
        pipeline.setSteps(null);

        ClashConfig input = new ClashConfig("test");
        ClashConfig result = pipelineService.execute(pipeline, input);

        assertSame(input, result);
    }

    @Test
    void testExecuteProcessorNotFound() {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("错误 Pipeline");

        PipelineStep step = new PipelineStep();
        step.setProcessor("non-existent");
        pipeline.setSteps(List.of(step));

        ClashConfig input = new ClashConfig("test");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> pipelineService.execute(pipeline, input));
        assertTrue(exception.getMessage().contains("处理器不存在"));
        assertTrue(exception.getMessage().contains("non-existent"));
    }

    @Test
    void testExecuteLogsProgress() {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("日志测试 Pipeline");

        PipelineStep step = new PipelineStep();
        step.setProcessor("add-node");
        step.setConfig(Map.of("nodeName", "log-node"));
        pipeline.setSteps(List.of(step));

        ClashConfig input = new ClashConfig("test");
        ProcessingContext context = new ProcessingContext();

        pipelineService.execute(pipeline, input, context);

        // 验证上下文日志记录了执行进度
        assertFalse(context.getLogs().isEmpty());
        assertTrue(context.getLogs().stream().anyMatch(log -> log.contains("add-node")));
        assertTrue(context.getLogs().stream().anyMatch(log -> log.contains("Pipeline [日志测试 Pipeline] 执行完成")));
    }

    @Test
    void testStepConfigMergedIntoContext() {
        // 验证步骤配置被合并到上下文中
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("配置合并测试");

        PipelineStep step = new PipelineStep();
        step.setProcessor("add-node");
        step.setConfig(Map.of("nodeName", "merged-node", "customKey", "customValue"));
        pipeline.setSteps(List.of(step));

        ClashConfig input = new ClashConfig("test");
        ProcessingContext context = new ProcessingContext();

        pipelineService.execute(pipeline, input, context);

        // 步骤配置中的自定义键应合并到上下文
        assertEquals("customValue", context.getVariable("customKey"));
    }
}
