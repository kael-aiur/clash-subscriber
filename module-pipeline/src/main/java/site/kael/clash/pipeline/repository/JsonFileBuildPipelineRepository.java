package site.kael.clash.pipeline.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.pipeline.model.BuildPipeline;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JsonFileBuildPipelineRepository implements BuildPipelineRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonFileBuildPipelineRepository.class);

    private final ObjectMapper objectMapper;
    private final Path pipelineDir;

    public JsonFileBuildPipelineRepository(
            ObjectMapper objectMapper,
            @Value("${data.path:data}") String dataPath) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.pipelineDir = Paths.get(dataPath, "build-pipelines");
        try {
            Files.createDirectories(pipelineDir);
            log.info("构建流程配置目录: {}", pipelineDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建构建流程配置目录: " + pipelineDir, e);
        }
    }

    @Override
    public BuildPipeline save(BuildPipeline pipeline) {
        Path filePath = pipelineDir.resolve(pipeline.getId() + ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), pipeline);
            log.debug("保存构建流程: {}", filePath);
            return pipeline;
        } catch (IOException e) {
            throw new RuntimeException("保存构建流程失败: " + pipeline.getId(), e);
        }
    }

    @Override
    public Optional<BuildPipeline> findById(String id) {
        Path filePath = pipelineDir.resolve(id + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(filePath.toFile(), BuildPipeline.class));
        } catch (IOException e) {
            throw new RuntimeException("读取构建流程失败: " + id, e);
        }
    }

    @Override
    public List<BuildPipeline> findAll() {
        List<BuildPipeline> pipelines = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pipelineDir, "*.json")) {
            for (Path filePath : stream) {
                try {
                    pipelines.add(objectMapper.readValue(filePath.toFile(), BuildPipeline.class));
                } catch (IOException e) {
                    log.warn("跳过无法读取的构建流程文件: {}", filePath, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("遍历构建流程配置目录失败", e);
        }
        return pipelines;
    }

    @Override
    public void deleteById(String id) {
        Path filePath = pipelineDir.resolve(id + ".json");
        try {
            Files.deleteIfExists(filePath);
            log.debug("删除构建流程: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("删除构建流程失败: " + id, e);
        }
    }
}
