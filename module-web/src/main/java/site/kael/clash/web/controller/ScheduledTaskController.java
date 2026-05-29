package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.scheduler.model.ScheduledTask;
import site.kael.clash.scheduler.service.SchedulerService;

import java.util.List;

/**
 * 定时任务管理 REST 控制器
 * <p>
 * 提供定时任务的 CRUD、启停控制及手动触发功能。
 */
@RestController
@RequestMapping("/api/scheduled-tasks")
public class ScheduledTaskController {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskController.class);

    private final SchedulerService schedulerService;

    public ScheduledTaskController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    /**
     * 查询所有定时任务
     */
    @GetMapping
    public ResponseEntity<List<ScheduledTask>> findAll() {
        log.debug("查询所有定时任务");
        return ResponseEntity.ok(schedulerService.findAll());
    }

    /**
     * 创建定时任务
     */
    @PostMapping
    public ResponseEntity<ScheduledTask> create(@RequestBody ScheduledTask task) {
        log.info("创建定时任务: name={}", task.getName());
        ScheduledTask created = schedulerService.create(task);
        return ResponseEntity.ok(created);
    }

    /**
     * 根据 ID 查询定时任务
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduledTask> findById(@PathVariable String id) {
        log.debug("查询定时任务: id={}", id);
        return schedulerService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new BusinessException(404, "定时任务不存在: " + id));
    }

    /**
     * 更新定时任务
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduledTask> update(@PathVariable String id,
                                                @RequestBody ScheduledTask task) {
        log.info("更新定时任务: id={}", id);
        task.setId(id);
        ScheduledTask updated = schedulerService.update(task);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除定时任务
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        log.info("删除定时任务: id={}", id);
        schedulerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 启用定时任务
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable String id) {
        log.info("启用定时任务: id={}", id);
        schedulerService.enable(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 停用定时任务
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable String id) {
        log.info("停用定时任务: id={}", id);
        schedulerService.disable(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 手动触发定时任务
     */
    @PostMapping("/{id}/trigger")
    public ResponseEntity<Void> trigger(@PathVariable String id) {
        log.info("手动触发定时任务: id={}", id);
        schedulerService.trigger(id);
        return ResponseEntity.ok().build();
    }
}
