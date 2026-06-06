package site.kael.clash.processor.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.processor.model.RuleGroup;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于 JSON 文件的规则组仓储实现
 */
@Repository
public class JsonFileRuleGroupRepository implements RuleGroupRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonFileRuleGroupRepository.class);

    private final ObjectMapper objectMapper;
    private final Path ruleGroupDir;

    public JsonFileRuleGroupRepository(
            ObjectMapper objectMapper,
            @Value("${data.path:data}") String dataPath) {
        this.objectMapper = objectMapper;
        this.ruleGroupDir = Paths.get(dataPath, "rule-groups");
        try {
            Files.createDirectories(ruleGroupDir);
            log.info("规则组目录: {}", ruleGroupDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建规则组目录: " + ruleGroupDir, e);
        }
    }

    @Override
    public RuleGroup save(RuleGroup ruleGroup) {
        Path filePath = ruleGroupDir.resolve(ruleGroup.getId() + ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), ruleGroup);
            log.debug("保存规则组: {}", filePath);
            return ruleGroup;
        } catch (IOException e) {
            throw new RuntimeException("保存规则组失败: " + ruleGroup.getId(), e);
        }
    }

    @Override
    public Optional<RuleGroup> findById(String id) {
        Path filePath = ruleGroupDir.resolve(id + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            RuleGroup ruleGroup = objectMapper.readValue(filePath.toFile(), RuleGroup.class);
            return Optional.of(ruleGroup);
        } catch (IOException e) {
            throw new RuntimeException("读取规则组失败: " + id, e);
        }
    }

    @Override
    public Optional<RuleGroup> findBySourceSubscriptionId(String subscriptionId) {
        return findAll().stream()
                .filter(rg -> subscriptionId.equals(rg.getSourceSubscriptionId()))
                .findFirst();
    }

    @Override
    public List<RuleGroup> findAll() {
        List<RuleGroup> ruleGroups = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(ruleGroupDir, "*.json")) {
            for (Path filePath : stream) {
                try {
                    RuleGroup ruleGroup = objectMapper.readValue(filePath.toFile(), RuleGroup.class);
                    ruleGroups.add(ruleGroup);
                } catch (IOException e) {
                    log.warn("跳过无法读取的规则组文件: {}", filePath, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("遍历规则组目录失败", e);
        }
        return ruleGroups;
    }

    @Override
    public void deleteById(String id) {
        Path filePath = ruleGroupDir.resolve(id + ".json");
        try {
            Files.deleteIfExists(filePath);
            log.debug("删除规则组: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("删除规则组失败: " + id, e);
        }
    }
}
