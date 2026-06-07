package site.kael.clash.processor.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.processor.model.ConfigProfile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于 JSON 文件的配置组合仓储实现
 */
@Repository
public class JsonFileConfigProfileRepository implements ConfigProfileRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonFileConfigProfileRepository.class);

    private final ObjectMapper objectMapper;
    private final Path configProfileDir;

    public JsonFileConfigProfileRepository(
            ObjectMapper objectMapper,
            @Value("${data.path:data}") String dataPath) {
        this.objectMapper = objectMapper;
        this.configProfileDir = Paths.get(dataPath, "config-profiles");
        try {
            Files.createDirectories(configProfileDir);
            log.info("配置组合目录: {}", configProfileDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建配置组合目录: " + configProfileDir, e);
        }
    }

    @Override
    public ConfigProfile save(ConfigProfile profile) {
        Path filePath = configProfileDir.resolve(profile.getId() + ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), profile);
            log.debug("保存配置组合: {}", filePath);
            return profile;
        } catch (IOException e) {
            throw new RuntimeException("保存配置组合失败: " + profile.getId(), e);
        }
    }

    @Override
    public Optional<ConfigProfile> findById(String id) {
        Path filePath = configProfileDir.resolve(id + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            ConfigProfile profile = objectMapper.readValue(filePath.toFile(), ConfigProfile.class);
            return Optional.of(profile);
        } catch (IOException e) {
            throw new RuntimeException("读取配置组合失败: " + id, e);
        }
    }

    @Override
    public Optional<ConfigProfile> findByName(String name) {
        return findAll().stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst();
    }

    @Override
    public List<ConfigProfile> findAll() {
        List<ConfigProfile> profiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configProfileDir, "*.json")) {
            for (Path filePath : stream) {
                try {
                    ConfigProfile profile = objectMapper.readValue(filePath.toFile(), ConfigProfile.class);
                    profiles.add(profile);
                } catch (IOException e) {
                    log.warn("跳过无法读取的配置组合文件: {}", filePath, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("遍历配置组合目录失败", e);
        }
        return profiles;
    }

    @Override
    public void deleteById(String id) {
        Path filePath = configProfileDir.resolve(id + ".json");
        try {
            Files.deleteIfExists(filePath);
            log.debug("删除配置组合: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("删除配置组合失败: " + id, e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }
}
