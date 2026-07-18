package site.kael.clash.pipeline.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.kael.clash.pipeline.model.BuildRecord;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JsonFileBuildRecordRepository 单元测试，聚焦删除能力。
 */
@DisplayName("JsonFileBuildRecordRepository 删除能力")
class JsonFileBuildRecordRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonFileBuildRecordRepository newRepo() {
        // 构造函数内部会注册 JavaTimeModule
        return new JsonFileBuildRecordRepository(new ObjectMapper(), tempDir.toString());
    }

    private BuildRecord newRecord(String id) {
        BuildRecord record = new BuildRecord();
        record.setId(id);
        record.setBuildPipelineId("pipeline-1");
        record.setStartedAt(LocalDateTime.now());
        record.setStatus("SUCCESS");
        return record;
    }

    @Test
    @DisplayName("删除已存在的记录：文件被移除且不可再查询")
    void deleteById_existingRecord_fileRemoved() {
        JsonFileBuildRecordRepository repo = newRepo();
        repo.save(newRecord("rec-1"));

        assertTrue(repo.findById("rec-1").isPresent(), "保存后应能查到");

        repo.deleteById("rec-1");

        assertFalse(repo.findById("rec-1").isPresent(), "删除后不应再查到");
        Path file = tempDir.resolve("build-records").resolve("rec-1.json");
        assertFalse(Files.exists(file), "底层 JSON 文件应已被删除");
    }

    @Test
    @DisplayName("删除不存在的 id 不抛异常")
    void deleteById_nonExisting_doesNotThrow() {
        JsonFileBuildRecordRepository repo = newRepo();
        assertDoesNotThrow(() -> repo.deleteById("not-exist"));
        assertFalse(repo.findById("not-exist").isPresent());
    }

    @Test
    @DisplayName("findByBuildPipelineId 仅返回匹配 pipelineId 的记录（多 pipeline 隔离）")
    void findByBuildPipelineId_onlyReturnsMatchingPipeline() {
        JsonFileBuildRecordRepository repo = newRepo();
        BuildRecord a1 = newRecord("a-1");
        a1.setBuildPipelineId("pipeline-A");
        BuildRecord a2 = newRecord("a-2");
        a2.setBuildPipelineId("pipeline-A");
        BuildRecord b1 = newRecord("b-1");
        b1.setBuildPipelineId("pipeline-B");
        repo.save(a1);
        repo.save(a2);
        repo.save(b1);

        List<BuildRecord> aRecords = repo.findByBuildPipelineId("pipeline-A");
        assertEquals(2, aRecords.size(), "pipeline-A 应返回 2 条");
        assertTrue(aRecords.stream().allMatch(r -> "pipeline-A".equals(r.getBuildPipelineId())));

        List<BuildRecord> bRecords = repo.findByBuildPipelineId("pipeline-B");
        assertEquals(1, bRecords.size(), "pipeline-B 应返回 1 条");
        assertEquals("b-1", bRecords.get(0).getId());
    }
}
