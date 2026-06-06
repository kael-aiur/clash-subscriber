package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.processor.model.ConfigProfile;
import site.kael.clash.processor.repository.ConfigProfileRepository;
import site.kael.clash.processor.service.ConfigGeneratorService;

import java.util.List;
import java.util.UUID;

/**
 * 配置组合管理 REST API
 * <p>
 * 提供配置组合的 CRUD 操作及 Clash 配置生成功能。
 */
@RestController
@RequestMapping("/api/config")
public class ConfigProfileController {

    private static final Logger log = LoggerFactory.getLogger(ConfigProfileController.class);

    private final ConfigProfileRepository configProfileRepository;
    private final ConfigGeneratorService configGeneratorService;

    public ConfigProfileController(
            ConfigProfileRepository configProfileRepository,
            ConfigGeneratorService configGeneratorService) {
        this.configProfileRepository = configProfileRepository;
        this.configGeneratorService = configGeneratorService;
    }

    /**
     * 查询所有配置组合
     */
    @GetMapping("/list")
    public ResponseEntity<List<ConfigProfile>> list() {
        return ResponseEntity.ok(configProfileRepository.findAll());
    }

    /**
     * 根据 ID 查询配置组合
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<ConfigProfile> getById(@PathVariable String id) {
        return configProfileRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建配置组合
     * <p>
     * 名称重复时返回 409 Conflict。
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ConfigProfile profile) {
        // 检查名称是否已存在
        if (configProfileRepository.existsByName(profile.getName())) {
            return ResponseEntity.status(409).body("{\"error\": \"配置名称已存在\"}");
        }

        profile.setId(UUID.randomUUID().toString());
        ConfigProfile created = configProfileRepository.save(profile);
        log.info("创建配置: id={}, name={}", created.getId(), created.getName());
        return ResponseEntity.ok(created);
    }

    /**
     * 更新配置组合
     * <p>
     * 名称与其他配置冲突时返回 409 Conflict。
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody ConfigProfile profile) {
        ConfigProfile existing = configProfileRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        // 检查名称是否与其他配置冲突
        if (!existing.getName().equals(profile.getName()) &&
            configProfileRepository.existsByName(profile.getName())) {
            return ResponseEntity.status(409).body("{\"error\": \"配置名称已存在\"}");
        }

        profile.setId(id);
        profile.setCreatedAt(existing.getCreatedAt());
        ConfigProfile updated = configProfileRepository.save(profile);
        log.info("更新配置: id={}, name={}", updated.getId(), updated.getName());
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除配置组合
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (configProfileRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        configProfileRepository.deleteById(id);
        log.info("删除配置: id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 根据配置名称获取完整的 Clash YAML 配置
     */
    @GetMapping("/{name}")
    public ResponseEntity<String> getClashConfig(@PathVariable String name) {
        try {
            String yaml = configGeneratorService.generateByName(name);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/yaml; charset=utf-8")
                    .body(yaml);
        } catch (Exception e) {
            log.error("生成配置失败: name={}", name, e);
            return ResponseEntity.notFound().build();
        }
    }
}
