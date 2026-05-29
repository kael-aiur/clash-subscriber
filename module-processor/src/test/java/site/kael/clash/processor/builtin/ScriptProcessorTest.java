package site.kael.clash.processor.builtin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.api.ProcessingContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ScriptProcessorTest {

    private ScriptProcessor processor;
    private ProcessingContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        processor = new ScriptProcessor();
        context = new ProcessingContext();

        // 通过反射设置 dataPath
        Field dataPathField = ScriptProcessor.class.getDeclaredField("dataPath");
        dataPathField.setAccessible(true);
        dataPathField.set(processor, tempDir.toString());
    }

    @Test
    void testName() {
        assertEquals("script", processor.getName());
    }

    @Test
    void testOrder() {
        assertEquals(999, processor.getOrder());
    }

    @Test
    void testScriptNotFound() {
        ClashConfig input = new ClashConfig("test");
        context.setVariable("scriptName", "nonexistent");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> processor.process(input, context));
        assertTrue(exception.getMessage().contains("脚本不存在"));
    }

    @Test
    void testNoScriptName() {
        ClashConfig input = new ClashConfig("test");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> processor.process(input, context));
        assertTrue(exception.getMessage().contains("未指定脚本名称"));
    }

    @Test
    void testScriptWithBlankName() {
        ClashConfig input = new ClashConfig("test");
        context.setVariable("scriptName", "  ");

        assertThrows(BusinessException.class, () -> processor.process(input, context));
    }

    @Test
    void testScriptWithNullName() {
        ClashConfig input = new ClashConfig("test");
        context.setVariable("scriptName", null);

        assertThrows(BusinessException.class, () -> processor.process(input, context));
    }

    /**
     * 以下测试需要 GraalVM JS 引擎支持。
     * GraalVM JS 运行时需要 GraalVM JDK 或独立的 native 组件，
     * 在标准 JDK 环境下可能无法运行。标记为 @Disabled，可在 GraalVM 环境下手动启用。
     */
    @Test
    @Disabled("需要 GraalVM JS 引擎支持，标准 JDK 环境下无法运行")
    void testExecuteSimpleScript() throws IOException {
        // 创建脚本目录和文件
        Path scriptsDir = tempDir.resolve("scripts");
        Files.createDirectories(scriptsDir);
        String script = """
                // 简单脚本：添加一个新节点
                var newNode = {
                    name: "script-node",
                    type: "vmess",
                    server: "10.0.0.1",
                    port: 443
                };
                config.proxies.push(newNode);
                """;
        Files.writeString(scriptsDir.resolve("test-script.js"), script);

        ClashConfig input = new ClashConfig("test");
        input.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));
        context.setVariable("scriptName", "test-script");

        ClashConfig output = processor.process(input, context);

        // 脚本操作的是 config 的拷贝
        assertNotNull(output);
    }

    @Test
    @Disabled("需要 GraalVM JS 引擎支持，标准 JDK 环境下无法运行")
    void testLogMessage() throws IOException {
        // 创建脚本目录和文件
        Path scriptsDir = tempDir.resolve("scripts");
        Files.createDirectories(scriptsDir);
        Files.writeString(scriptsDir.resolve("log-test.js"), "// 空脚本");

        ClashConfig input = new ClashConfig("test");
        context.setVariable("scriptName", "log-test");

        processor.process(input, context);

        assertFalse(context.getLogs().isEmpty());
        assertTrue(context.getLogs().stream().anyMatch(log -> log.contains("脚本执行完成")));
    }
}
