package site.kael.clash.scheduler.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.scheduler.model.ScheduledTask;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于 JSON 文件的定时任务仓储实现
 */
@Repository
public class JsonFileScheduledTaskRepository implements ScheduledTaskRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonFileScheduledTaskRepository.class);

    private final ObjectMapper objectMapper;
    private final Path taskDir;

    public JsonFileScheduledTaskRepository(
            ObjectMapper objectMapper,
            @Value("${data.path:data}") String dataPath) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.taskDir = Paths.get(dataPath, "scheduled-tasks");
        try {
            Files.createDirectories(taskDir);
            log.info("定时任务配置目录: {}", taskDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建定时任务配置目录: " + taskDir, e);
        }
    }

    @Override
    public ScheduledTask save(ScheduledTask task) {
        Path filePath = taskDir.resolve(task.getId() + ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), task);
            log.debug("保存定时任务: {}", filePath);
            return task;
        } catch (IOException e) {
            throw new RuntimeException("保存定时任务失败: " + task.getId(), e);
        }
    }

    @Override
    public Optional<ScheduledTask> findById(String id) {
        Path filePath = taskDir.resolve(id + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            ScheduledTask task = objectMapper.readValue(filePath.toFile(), ScheduledTask.class);
            return Optional.of(task);
        } catch (IOException e) {
            throw new RuntimeException("读取定时任务失败: " + id, e);
        }
    }

    @Override
    public List<ScheduledTask> findAll() {
        List<ScheduledTask> tasks = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taskDir, "*.json")) {
            for (Path filePath : stream) {
                try {
                    ScheduledTask task = objectMapper.readValue(filePath.toFile(), ScheduledTask.class);
                    tasks.add(task);
                } catch (IOException e) {
                    log.warn("跳过无法读取的定时任务文件: {}", filePath, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("遍历定时任务配置目录失败", e);
        }
        return tasks;
    }

    @Override
    public void deleteById(String id) {
        Path filePath = taskDir.resolve(id + ".json");
        try {
            Files.deleteIfExists(filePath);
            log.debug("删除定时任务: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("删除定时任务失败: " + id, e);
        }
    }
}
