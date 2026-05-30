package site.kael.clash.scheduler.service.impl;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.common.util.IdGenerator;
import site.kael.clash.mihomo.service.MihomoService;
import site.kael.clash.processor.model.PipelineConfig;
import site.kael.clash.processor.repository.PipelineConfigRepository;
import site.kael.clash.processor.service.PipelineService;
import site.kael.clash.scheduler.model.ScheduledTask;
import site.kael.clash.scheduler.model.TaskStatus;
import site.kael.clash.scheduler.repository.ScheduledTaskRepository;
import site.kael.clash.scheduler.service.SchedulerService;
import site.kael.clash.subscription.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时任务调度服务实现
 * <p>
 * 提供定时任务的 CRUD 管理、cron 调度注册/取消、以及任务执行逻辑
 * （获取订阅 -> Pipeline 处理 -> 推送配置到 Mihomo 实例）。
 */
@Service
public class SchedulerServiceImpl implements SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    private final ScheduledTaskRepository taskRepository;
    private final PipelineConfigRepository pipelineConfigRepository;
    private final PipelineService pipelineService;
    private final MihomoService mihomoService;
    private final SubscriptionService subscriptionService;
    private final TaskScheduler taskScheduler;

    /**
     * 已注册的定时任务 Future，用于取消调度
     */
    private final Map<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    public SchedulerServiceImpl(
            ScheduledTaskRepository taskRepository,
            PipelineConfigRepository pipelineConfigRepository,
            PipelineService pipelineService,
            MihomoService mihomoService,
            SubscriptionService subscriptionService,
            TaskScheduler taskScheduler) {
        this.taskRepository = taskRepository;
        this.pipelineConfigRepository = pipelineConfigRepository;
        this.pipelineService = pipelineService;
        this.mihomoService = mihomoService;
        this.subscriptionService = subscriptionService;
        this.taskScheduler = taskScheduler;
    }

    /**
     * 应用启动时，重新注册所有已启用的定时任务
     */
    @PostConstruct
    public void init() {
        log.info("初始化定时任务调度...");
        List<ScheduledTask> tasks = taskRepository.findAll();
        for (ScheduledTask task : tasks) {
            if (task.isEnabled() && task.getCronExpression() != null && !task.getCronExpression().isBlank()) {
                try {
                    registerCronJob(task);
                    log.info("已恢复定时任务: {} ({})", task.getName(), task.getId());
                } catch (Exception e) {
                    log.error("恢复定时任务失败: {} ({}), 原因: {}", task.getName(), task.getId(), e.getMessage());
                }
            }
        }
        log.info("定时任务初始化完成，共恢复 {} 个任务", scheduledJobs.size());
    }

    // ========== CRUD 操作 ==========

    @Override
    public ScheduledTask create(ScheduledTask task) {
        task.setId(IdGenerator.generate());
        task.setLastRunAt(null);
        task.setLastRunStatus(null);
        ScheduledTask saved = taskRepository.save(task);
        log.info("创建定时任务: {} ({})", saved.getName(), saved.getId());

        // 如果创建时已启用且有 cron 表达式，立即注册
        if (saved.isEnabled() && saved.getCronExpression() != null && !saved.getCronExpression().isBlank()) {
            try {
                registerCronJob(saved);
            } catch (Exception e) {
                log.warn("创建时注册 cron 调度失败: {}", e.getMessage());
            }
        }
        return saved;
    }

    @Override
    public ScheduledTask update(ScheduledTask task) {
        if (task.getId() == null || task.getId().isBlank()) {
            throw new BusinessException("任务 ID 不能为空");
        }
        // 确认任务存在
        taskRepository.findById(task.getId())
                .orElseThrow(() -> new BusinessException("定时任务不存在: " + task.getId()));

        ScheduledTask saved = taskRepository.save(task);
        log.info("更新定时任务: {} ({})", saved.getName(), saved.getId());

        // 如果任务已启用，重新注册 cron 调度（cron 表达式可能已变更）
        cancelCronJob(task.getId());
        if (saved.isEnabled() && saved.getCronExpression() != null && !saved.getCronExpression().isBlank()) {
            try {
                registerCronJob(saved);
            } catch (Exception e) {
                log.warn("更新后注册 cron 调度失败: {}", e.getMessage());
            }
        }
        return saved;
    }

    @Override
    public Optional<ScheduledTask> findById(String id) {
        return taskRepository.findById(id);
    }

    @Override
    public List<ScheduledTask> findAll() {
        return taskRepository.findAll();
    }

    @Override
    public void deleteById(String id) {
        cancelCronJob(id);
        taskRepository.deleteById(id);
        log.info("删除定时任务: {}", id);
    }

    // ========== 启停控制 ==========

    @Override
    public void enable(String taskId) {
        ScheduledTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("定时任务不存在: " + taskId));

        if (task.getCronExpression() == null || task.getCronExpression().isBlank()) {
            throw new BusinessException("定时任务 cron 表达式为空，无法启用: " + taskId);
        }

        task.setEnabled(true);
        taskRepository.save(task);
        registerCronJob(task);
        log.info("启用定时任务: {} ({})", task.getName(), task.getId());
    }

    @Override
    public void disable(String taskId) {
        ScheduledTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("定时任务不存在: " + taskId));

        task.setEnabled(false);
        taskRepository.save(task);
        cancelCronJob(taskId);
        log.info("停用定时任务: {} ({})", task.getName(), task.getId());
    }

    @Override
    public void trigger(String taskId) {
        // 确认任务存在
        taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("定时任务不存在: " + taskId));

        log.info("手动触发定时任务: {}", taskId);
        executeTask(taskId);
    }

    // ========== 构建流程 Cron 管理 ==========

    @Override
    public void registerBuildPipelineCron(String pipelineId, String cronExpression, Runnable callback) {
        cancelBuildPipelineCron(pipelineId);
        CronTrigger trigger = new CronTrigger(cronExpression);
        ScheduledFuture<?> future = taskScheduler.schedule(callback, trigger);
        scheduledJobs.put("bp-" + pipelineId, future);
        log.info("注册构建流程 cron 调度: {} ({})", pipelineId, cronExpression);
    }

    @Override
    public void cancelBuildPipelineCron(String pipelineId) {
        ScheduledFuture<?> future = scheduledJobs.remove("bp-" + pipelineId);
        if (future != null) {
            future.cancel(false);
            log.info("取消构建流程 cron 调度: {}", pipelineId);
        }
    }

    // ========== Cron 调度管理 ==========

    /**
     * 注册 cron 定时任务
     */
    private void registerCronJob(ScheduledTask task) {
        CronTrigger trigger = new CronTrigger(task.getCronExpression());
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeTask(task.getId()),
                trigger
        );
        scheduledJobs.put(task.getId(), future);
    }

    /**
     * 取消已注册的 cron 定时任务
     */
    private void cancelCronJob(String taskId) {
        ScheduledFuture<?> future = scheduledJobs.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

    // ========== 任务执行逻辑 ==========

    /**
     * 执行定时任务：获取订阅 -> Pipeline 处理 -> 推送配置
     */
    void executeTask(String taskId) {
        log.info("开始执行定时任务: {}", taskId);

        // 1. 查找任务
        ScheduledTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("定时任务不存在: {}", taskId);
            return;
        }

        // 2. 更新状态为运行中
        task.setLastRunStatus(TaskStatus.RUNNING);
        task.setLastRunAt(LocalDateTime.now());
        taskRepository.save(task);

        try {
            // 3. 查找 Pipeline 配置
            PipelineConfig pipelineConfig = pipelineConfigRepository.findById(task.getPipelineId())
                    .orElseThrow(() -> new BusinessException("Pipeline 配置不存在: " + task.getPipelineId()));

            // 4. 获取所有订阅配置并合并
            ClashConfig mergedConfig = fetchAndMergeSubscriptions();

            // 5. 执行 Pipeline 处理
            ClashConfig processedConfig = pipelineService.execute(pipelineConfig, mergedConfig);

            // 6. 推送到目标 Mihomo 实例
            pushToTargetInstances(task, processedConfig);

            // 7. 更新状态为成功
            task.setLastRunStatus(TaskStatus.SUCCESS);
            task.setLastRunAt(LocalDateTime.now());
            taskRepository.save(task);
            log.info("定时任务执行成功: {} ({})", task.getName(), taskId);

        } catch (Exception e) {
            // 8. 更新状态为失败
            task.setLastRunStatus(TaskStatus.FAILED);
            task.setLastRunAt(LocalDateTime.now());
            taskRepository.save(task);
            log.error("定时任务执行失败: {} ({}), 原因: {}", task.getName(), taskId, e.getMessage(), e);
        }
    }

    /**
     * 获取所有订阅源的配置并合并为一个 ClashConfig
     * <p>
     * 单个订阅源获取失败不会中断整体任务，会记录警告并跳过。
     */
    private ClashConfig fetchAndMergeSubscriptions() {
        List<ProxyNode> allProxies = new ArrayList<>();

        subscriptionService.findAll().forEach(subscription -> {
            try {
                ClashConfig config = subscriptionService.fetch(subscription.getId());
                if (config != null && config.getProxies() != null) {
                    allProxies.addAll(config.getProxies());
                    log.debug("成功获取订阅源: {}，节点数: {}",
                            subscription.getName(), config.getProxies().size());
                }
            } catch (Exception e) {
                log.warn("获取订阅源失败: {} ({}), 原因: {}",
                        subscription.getName(), subscription.getId(), e.getMessage());
            }
        });

        ClashConfig merged = new ClashConfig("merged-subscriptions");
        merged.setProxies(allProxies);
        log.info("合并订阅源节点总数: {}", allProxies.size());
        return merged;
    }

    /**
     * 推送配置到目标任务指定的 Mihomo 实例
     * <p>
     * 如果指定了目标实例列表则推送到指定实例，否则推送到所有已启用实例。
     * 单个实例推送失败不会中断整体任务。
     */
    private void pushToTargetInstances(ScheduledTask task, ClashConfig config) {
        List<String> targets = task.getTargetInstances();

        if (targets == null || targets.isEmpty()) {
            // 未指定目标，推送到所有已启用实例
            Map<String, Boolean> results = mihomoService.pushConfigAll(config);
            long successCount = results.values().stream().filter(Boolean::booleanValue).count();
            log.info("配置推送到所有实例: 成功 {}/{}", successCount, results.size());
        } else {
            // 推送到指定目标实例
            int successCount = 0;
            for (String instanceId : targets) {
                try {
                    mihomoService.pushConfig(instanceId, config);
                    successCount++;
                    log.debug("配置推送成功: {}", instanceId);
                } catch (Exception e) {
                    log.warn("配置推送失败: {}, 原因: {}", instanceId, e.getMessage());
                }
            }
            log.info("配置推送到指定实例: 成功 {}/{}", successCount, targets.size());
        }
    }
}
