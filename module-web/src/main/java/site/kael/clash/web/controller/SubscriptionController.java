package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.subscription.model.Subscription;
import site.kael.clash.subscription.service.SubscriptionService;

import java.util.List;

/**
 * 订阅源管理 REST 控制器
 * <p>
 * 提供订阅源的 CRUD 操作及远程获取功能。
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * 查询所有订阅源
     */
    @GetMapping
    public ResponseEntity<List<Subscription>> findAll() {
        log.debug("查询所有订阅源");
        return ResponseEntity.ok(subscriptionService.findAll());
    }

    /**
     * 创建订阅源
     */
    @PostMapping
    public ResponseEntity<Subscription> create(@RequestBody Subscription subscription) {
        log.info("创建订阅源: name={}", subscription.getName());
        Subscription created = subscriptionService.create(subscription);
        return ResponseEntity.ok(created);
    }

    /**
     * 根据 ID 查询订阅源
     */
    @GetMapping("/{id}")
    public ResponseEntity<Subscription> findById(@PathVariable String id) {
        log.debug("查询订阅源: id={}", id);
        return subscriptionService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new BusinessException(404, "订阅源不存在: " + id));
    }

    /**
     * 更新订阅源
     */
    @PutMapping("/{id}")
    public ResponseEntity<Subscription> update(@PathVariable String id,
                                               @RequestBody Subscription subscription) {
        log.info("更新订阅源: id={}", id);
        subscription.setId(id);
        Subscription updated = subscriptionService.update(subscription);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除订阅源
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        log.info("删除订阅源: id={}", id);
        subscriptionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 远程获取并解析订阅源配置
     */
    @PostMapping("/{id}/fetch")
    public ResponseEntity<ClashConfig> fetch(@PathVariable String id) {
        log.info("获取订阅源配置: id={}", id);
        ClashConfig config = subscriptionService.fetch(id);
        return ResponseEntity.ok(config);
    }
}
