package site.kael.clash.pipeline.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.pipeline.model.BuildRecord;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class JsonFileBuildRecordRepository implements BuildRecordRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonFileBuildRecordRepository.class);

    private final ObjectMapper objectMapper;
    private final Path recordDir;

    public JsonFileBuildRecordRepository(
            ObjectMapper objectMapper,
            @Value("${data.path:data}") String dataPath) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.recordDir = Paths.get(dataPath, "build-records");
        try {
            Files.createDirectories(recordDir);
            log.info("构建记录目录: {}", recordDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建构建记录目录: " + recordDir, e);
        }
    }

    @Override
    public BuildRecord save(BuildRecord record) {
        Path filePath = recordDir.resolve(record.getId() + ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), record);
            log.debug("保存构建记录: {}", filePath);
            return record;
        } catch (IOException e) {
            throw new RuntimeException("保存构建记录失败: " + record.getId(), e);
        }
    }

    @Override
    public Optional<BuildRecord> findById(String id) {
        Path filePath = recordDir.resolve(id + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(filePath.toFile(), BuildRecord.class));
        } catch (IOException e) {
            throw new RuntimeException("读取构建记录失败: " + id, e);
        }
    }

    @Override
    public List<BuildRecord> findByBuildPipelineId(String buildPipelineId) {
        List<BuildRecord> records = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(recordDir, "*.json")) {
            for (Path filePath : stream) {
                try {
                    BuildRecord record = objectMapper.readValue(filePath.toFile(), BuildRecord.class);
                    if (buildPipelineId.equals(record.getBuildPipelineId())) {
                        records.add(record);
                    }
                } catch (IOException e) {
                    log.warn("跳过无法读取的构建记录文件: {}", filePath, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("遍历构建记录目录失败", e);
        }
        records.sort(Comparator.comparing(BuildRecord::getStartedAt).reversed());
        return records;
    }

    @Override
    public void deleteById(String id) {
        Path filePath = recordDir.resolve(id + ".json");
        try {
            Files.deleteIfExists(filePath);
            log.debug("删除构建记录: {}", filePath);
        } catch (IOException e) {
            // 清理失败不应影响构建主流程，仅记 warn
            log.warn("删除构建记录失败: {}, 原因: {}", id, e.getMessage());
        }
    }
}
