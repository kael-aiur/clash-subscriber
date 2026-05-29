package site.kael.clash.processor.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.processor.model.PipelineConfig;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于 JSON 文件的 Pipeline 配置仓储实现
 */
@Repository
public class JsonFilePipelineConfigRepository implements PipelineConfigRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonFilePipelineConfigRepository.class);

    private final ObjectMapper objectMapper;
    private final Path pipelineDir;

    public JsonFilePipelineConfigRepository(
            ObjectMapper objectMapper,
            @Value("${data.path:data}") String dataPath) {
        this.objectMapper = objectMapper;
        this.pipelineDir = Paths.get(dataPath, "pipelines");
        try {
            Files.createDirectories(pipelineDir);
            log.info("Pipeline 配置目录: {}", pipelineDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建 Pipeline 配置目录: " + pipelineDir, e);
        }
    }

    @Override
    public PipelineConfig save(PipelineConfig config) {
        Path filePath = pipelineDir.resolve(config.getId() + ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), config);
            log.debug("保存 Pipeline 配置: {}", filePath);
            return config;
        } catch (IOException e) {
            throw new RuntimeException("保存 Pipeline 配置失败: " + config.getId(), e);
        }
    }

    @Override
    public Optional<PipelineConfig> findById(String id) {
        Path filePath = pipelineDir.resolve(id + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            PipelineConfig config = objectMapper.readValue(filePath.toFile(), PipelineConfig.class);
            return Optional.of(config);
        } catch (IOException e) {
            throw new RuntimeException("读取 Pipeline 配置失败: " + id, e);
        }
    }

    @Override
    public List<PipelineConfig> findAll() {
        List<PipelineConfig> configs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pipelineDir, "*.json")) {
            for (Path filePath : stream) {
                try {
                    PipelineConfig config = objectMapper.readValue(filePath.toFile(), PipelineConfig.class);
                    configs.add(config);
                } catch (IOException e) {
                    log.warn("跳过无法读取的 Pipeline 配置文件: {}", filePath, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("遍历 Pipeline 配置目录失败", e);
        }
        return configs;
    }

    @Override
    public void deleteById(String id) {
        Path filePath = pipelineDir.resolve(id + ".json");
        try {
            Files.deleteIfExists(filePath);
            log.debug("删除 Pipeline 配置: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("删除 Pipeline 配置失败: " + id, e);
        }
    }
}
