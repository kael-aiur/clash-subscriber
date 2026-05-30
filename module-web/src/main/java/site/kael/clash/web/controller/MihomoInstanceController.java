package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.mihomo.model.ForwardingPathResult;
import site.kael.clash.mihomo.model.HealthStatus;
import site.kael.clash.mihomo.model.MihomoInstance;
import site.kael.clash.mihomo.service.ForwardingPathService;
import site.kael.clash.mihomo.service.MihomoService;

import java.util.List;
import java.util.Map;

/**
 * Mihomo 实例管理 REST 控制器
 * <p>
 * 提供实例的 CRUD、健康检查和配置推送功能。
 */
@RestController
@RequestMapping("/api/mihomo-instances")
public class MihomoInstanceController {

    private static final Logger log = LoggerFactory.getLogger(MihomoInstanceController.class);

    private final MihomoService mihomoService;
    private final ForwardingPathService forwardingPathService;

    public MihomoInstanceController(MihomoService mihomoService, ForwardingPathService forwardingPathService) {
        this.mihomoService = mihomoService;
        this.forwardingPathService = forwardingPathService;
    }

    /**
     * 查询所有 Mihomo 实例
     */
    @GetMapping
    public ResponseEntity<List<MihomoInstance>> findAll() {
        log.debug("查询所有 Mihomo 实例");
        return ResponseEntity.ok(mihomoService.findAll());
    }

    /**
     * 创建 Mihomo 实例
     */
    @PostMapping
    public ResponseEntity<MihomoInstance> create(@RequestBody MihomoInstance instance) {
        log.info("创建 Mihomo 实例: name={}", instance.getName());
        MihomoInstance created = mihomoService.create(instance);
        return ResponseEntity.ok(created);
    }

    /**
     * 根据 ID 查询 Mihomo 实例
     */
    @GetMapping("/{id}")
    public ResponseEntity<MihomoInstance> findById(@PathVariable String id) {
        log.debug("查询 Mihomo 实例: id={}", id);
        return mihomoService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new BusinessException(404, "Mihomo 实例不存在: " + id));
    }

    /**
     * 更新 Mihomo 实例
     */
    @PutMapping("/{id}")
    public ResponseEntity<MihomoInstance> update(@PathVariable String id,
                                                 @RequestBody MihomoInstance instance) {
        log.info("更新 Mihomo 实例: id={}", id);
        instance.setId(id);
        MihomoInstance updated = mihomoService.update(instance);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除 Mihomo 实例
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        log.info("删除 Mihomo 实例: id={}", id);
        mihomoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 检查单个实例的健康状态
     */
    @GetMapping("/{id}/health")
    public ResponseEntity<HealthStatus> checkHealth(@PathVariable String id) {
        log.debug("检查实例健康状态: id={}", id);
        HealthStatus status = mihomoService.checkHealth(id);
        return ResponseEntity.ok(status);
    }

    /**
     * 检查所有实例的健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, HealthStatus>> checkHealthAll() {
        log.debug("检查所有实例健康状态");
        Map<String, HealthStatus> statuses = mihomoService.checkHealthAll();
        return ResponseEntity.ok(statuses);
    }

    /**
     * 向单个实例推送配置
     */
    @PostMapping("/{id}/push")
    public ResponseEntity<Void> pushConfig(@PathVariable String id,
                                           @RequestBody ClashConfig config) {
        log.info("向实例推送配置: id={}", id);
        mihomoService.pushConfig(id, config);
        return ResponseEntity.ok().build();
    }

    /**
     * 向所有已启用实例推送配置
     */
    @PostMapping("/push")
    public ResponseEntity<Map<String, Boolean>> pushConfigAll(@RequestBody ClashConfig config) {
        log.info("向所有实例推送配置");
        Map<String, Boolean> results = mihomoService.pushConfigAll(config);
        return ResponseEntity.ok(results);
    }

    /**
     * 查询域名的转发路径
     */
    @GetMapping("/{id}/forwarding-path")
    public ResponseEntity<ForwardingPathResult> getForwardingPath(
            @PathVariable String id,
            @RequestParam String domain) {
        log.info("查询转发路径: id={}, domain={}", id, domain);
        try {
            String rulesJson = mihomoService.getRules(id);
            String proxiesJson = mihomoService.getProxies(id);
            log.debug("获取规则数据长度: {}", rulesJson.length());
            log.debug("获取代理数据长度: {}", proxiesJson.length());
            ForwardingPathResult result = forwardingPathService.resolveForwardingPath(rulesJson, proxiesJson, domain);
            log.info("转发路径查询结果: {} 个节点, {} 条边", result.getNodes().size(), result.getEdges().size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询转发路径失败: id={}, domain={}, error={}", id, domain, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取实例当前配置
     */
    @GetMapping("/{id}/config")
    public ResponseEntity<String> getConfig(@PathVariable String id) {
        log.debug("获取实例配置: id={}", id);
        String configYaml = mihomoService.getConfig(id);
        return ResponseEntity.ok(configYaml);
    }
}
