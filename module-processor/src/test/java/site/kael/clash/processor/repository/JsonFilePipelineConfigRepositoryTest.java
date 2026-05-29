package site.kael.clash.processor.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.kael.clash.processor.model.PipelineConfig;
import site.kael.clash.processor.model.PipelineStep;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonFilePipelineConfigRepositoryTest {

    private JsonFilePipelineConfigRepository repository;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = new JsonFilePipelineConfigRepository(objectMapper, tempDir.toString());
    }

    @Test
    void testSaveAndFindById() {
        PipelineConfig config = createTestConfig("test-1", "测试 Pipeline");

        repository.save(config);

        Optional<PipelineConfig> found = repository.findById("test-1");
        assertTrue(found.isPresent());
        assertEquals("test-1", found.get().getId());
        assertEquals("测试 Pipeline", found.get().getName());
        assertEquals(2, found.get().getSteps().size());
    }

    @Test
    void testFindByIdNotFound() {
        Optional<PipelineConfig> found = repository.findById("non-existent");
        assertFalse(found.isPresent());
    }

    @Test
    void testSaveOverwrite() {
        PipelineConfig config1 = createTestConfig("test-1", "原始名称");
        repository.save(config1);

        PipelineConfig config2 = createTestConfig("test-1", "更新名称");
        repository.save(config2);

        Optional<PipelineConfig> found = repository.findById("test-1");
        assertTrue(found.isPresent());
        assertEquals("更新名称", found.get().getName());
    }

    @Test
    void testFindAll() {
        repository.save(createTestConfig("p1", "Pipeline 1"));
        repository.save(createTestConfig("p2", "Pipeline 2"));
        repository.save(createTestConfig("p3", "Pipeline 3"));

        List<PipelineConfig> all = repository.findAll();
        assertEquals(3, all.size());
    }

    @Test
    void testFindAllEmpty() {
        List<PipelineConfig> all = repository.findAll();
        assertTrue(all.isEmpty());
    }

    @Test
    void testDeleteById() {
        repository.save(createTestConfig("test-1", "待删除"));
        assertTrue(repository.findById("test-1").isPresent());

        repository.deleteById("test-1");
        assertFalse(repository.findById("test-1").isPresent());
    }

    @Test
    void testDeleteByIdNotFound() {
        // 删除不存在的配置不应抛异常
        assertDoesNotThrow(() -> repository.deleteById("non-existent"));
    }

    @Test
    void testSaveCreatesDirectory() {
        // 使用一个不存在的子目录路径
        String nestedPath = tempDir.resolve("nested").resolve("data").toString();
        JsonFilePipelineConfigRepository nestedRepo =
                new JsonFilePipelineConfigRepository(objectMapper, nestedPath);

        PipelineConfig config = createTestConfig("test-nested", "嵌套目录测试");
        nestedRepo.save(config);

        Optional<PipelineConfig> found = nestedRepo.findById("test-nested");
        assertTrue(found.isPresent());
    }

    @Test
    void testFindAllSkipsInvalidFiles() throws IOException {
        // 保存一个正常的配置
        repository.save(createTestConfig("valid", "有效配置"));

        // 写入一个无效的 JSON 文件
        Path invalidFile = tempDir.resolve("pipelines").resolve("invalid.json");
        Files.createDirectories(invalidFile.getParent());
        Files.writeString(invalidFile, "{ invalid json }");

        // findAll 应跳过无效文件
        List<PipelineConfig> all = repository.findAll();
        assertEquals(1, all.size());
        assertEquals("valid", all.get(0).getId());
    }

    @Test
    void testStepConfigSerialization() {
        PipelineConfig config = new PipelineConfig();
        config.setId("step-config-test");
        config.setName("步骤配置测试");

        PipelineStep step = new PipelineStep();
        step.setProcessor("rule-modify");
        step.setConfig(Map.of("action", "replace", "target", "DIRECT"));
        config.setSteps(List.of(step));

        repository.save(config);

        Optional<PipelineConfig> found = repository.findById("step-config-test");
        assertTrue(found.isPresent());
        assertEquals("rule-modify", found.get().getSteps().get(0).getProcessor());
        assertEquals("replace", found.get().getSteps().get(0).getConfig().get("action"));
        assertEquals("DIRECT", found.get().getSteps().get(0).getConfig().get("target"));
    }

    private PipelineConfig createTestConfig(String id, String name) {
        PipelineConfig config = new PipelineConfig();
        config.setId(id);
        config.setName(name);

        PipelineStep step1 = new PipelineStep();
        step1.setProcessor("node-merge");
        step1.setConfig(Map.of("source", "sub1"));

        PipelineStep step2 = new PipelineStep();
        step2.setProcessor("rule-modify");
        step2.setConfig(Map.of("action", "append"));

        config.setSteps(List.of(step1, step2));
        return config;
    }
}
