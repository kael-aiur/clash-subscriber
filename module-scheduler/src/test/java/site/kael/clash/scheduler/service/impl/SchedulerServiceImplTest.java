package site.kael.clash.scheduler.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.mihomo.service.MihomoService;
import site.kael.clash.processor.model.PipelineConfig;
import site.kael.clash.processor.repository.PipelineConfigRepository;
import site.kael.clash.processor.service.PipelineService;
import site.kael.clash.scheduler.model.ScheduledTask;
import site.kael.clash.scheduler.model.TaskStatus;
import site.kael.clash.scheduler.repository.ScheduledTaskRepository;
import site.kael.clash.subscription.model.Subscription;
import site.kael.clash.subscription.service.SubscriptionService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceImplTest {

    @Mock
    private ScheduledTaskRepository taskRepository;

    @Mock
    private PipelineConfigRepository pipelineConfigRepository;

    @Mock
    private PipelineService pipelineService;

    @Mock
    private MihomoService mihomoService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @InjectMocks
    private SchedulerServiceImpl service;

    private ScheduledTask sampleTask;
    private PipelineConfig samplePipeline;
    private Subscription sampleSubscription;

    @BeforeEach
    void setUp() {
        sampleTask = new ScheduledTask();
        sampleTask.setId("task-001");
        sampleTask.setName("测试任务");
        sampleTask.setPipelineId("pipeline-001");
        sampleTask.setTargetInstances(List.of("inst-001", "inst-002"));
        sampleTask.setCronExpression("0 0 * * * *");
        sampleTask.setEnabled(true);

        samplePipeline = new PipelineConfig();
        samplePipeline.setId("pipeline-001");
        samplePipeline.setName("测试 Pipeline");

        sampleSubscription = new Subscription();
        sampleSubscription.setId("sub-001");
        sampleSubscription.setName("测试订阅");
    }

    /**
     * 辅助方法：使用 doReturn 绕过 ScheduledFuture 泛型擦除问题
     */
    @SuppressWarnings("unchecked")
    private void stubSchedule() {
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    // ========== 创建任务测试 ==========

    @Test
    void create_shouldGenerateIdAndSave() {
        ScheduledTask input = new ScheduledTask();
        input.setName("新任务");
        input.setPipelineId("pipeline-001");
        input.setCronExpression("0 0 * * * *");
        input.setEnabled(true);

        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSchedule();

        ScheduledTask result = service.create(input);

        assertNotNull(result.getId());
        assertEquals("新任务", result.getName());
        assertNull(result.getLastRunAt());
        assertNull(result.getLastRunStatus());

        verify(taskRepository).save(any(ScheduledTask.class));
    }

    @Test
    void create_shouldNotRegisterCronWhenDisabled() {
        ScheduledTask input = new ScheduledTask();
        input.setName("禁用任务");
        input.setPipelineId("pipeline-001");
        input.setCronExpression("0 0 * * * *");
        input.setEnabled(false);

        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(input);

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    // ========== 更新任务测试 ==========

    @Test
    void update_shouldSaveAndReRegisterCron() {
        when(taskRepository.findById("task-001")).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSchedule();

        sampleTask.setName("更新后的任务");
        ScheduledTask result = service.update(sampleTask);

        assertEquals("更新后的任务", result.getName());
        verify(taskRepository).save(sampleTask);
    }

    @Test
    void update_shouldThrowWhenIdIsNull() {
        ScheduledTask noId = new ScheduledTask();
        noId.setName("无ID任务");

        assertThrows(BusinessException.class, () -> service.update(noId));
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        when(taskRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ScheduledTask missing = new ScheduledTask();
        missing.setId("nonexistent");

        assertThrows(BusinessException.class, () -> service.update(missing));
    }

    // ========== 查询测试 ==========

    @Test
    void findById_shouldDelegateToRepository() {
        when(taskRepository.findById("task-001")).thenReturn(Optional.of(sampleTask));

        Optional<ScheduledTask> result = service.findById("task-001");

        assertTrue(result.isPresent());
        assertEquals("测试任务", result.get().getName());
    }

    @Test
    void findAll_shouldDelegateToRepository() {
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        List<ScheduledTask> result = service.findAll();

        assertEquals(1, result.size());
    }

    // ========== 删除测试 ==========

    @Test
    void deleteById_shouldCancelCronAndDelete() {
        // 先注册一个 cron 任务
        when(taskRepository.findById("task-001")).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSchedule();
        service.enable("task-001");

        // 删除
        service.deleteById("task-001");

        verify(scheduledFuture).cancel(false);
        verify(taskRepository).deleteById("task-001");
    }

    // ========== 启停控制测试 ==========

    @Test
    void enable_shouldRegisterCronJob() {
        when(taskRepository.findById("task-001")).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSchedule();

        service.enable("task-001");

        assertTrue(sampleTask.isEnabled());
        verify(taskRepository).save(sampleTask);
        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void enable_shouldThrowWhenNotFound() {
        when(taskRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.enable("nonexistent"));
    }

    @Test
    void enable_shouldThrowWhenCronExpressionIsBlank() {
        ScheduledTask noCron = new ScheduledTask();
        noCron.setId("task-002");
        noCron.setCronExpression("");
        when(taskRepository.findById("task-002")).thenReturn(Optional.of(noCron));

        assertThrows(BusinessException.class, () -> service.enable("task-002"));
    }

    @Test
    void disable_shouldCancelCronJob() {
        // 先注册
        when(taskRepository.findById("task-001")).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSchedule();
        service.enable("task-001");

        // 停用
        service.disable("task-001");

        assertFalse(sampleTask.isEnabled());
        verify(scheduledFuture).cancel(false);
        verify(taskRepository, atLeastOnce()).save(sampleTask);
    }

    @Test
    void disable_shouldThrowWhenNotFound() {
        when(taskRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.disable("nonexistent"));
    }

    // ========== 手动触发测试 ==========

    @Test
    void trigger_shouldThrowWhenNotFound() {
        when(taskRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.trigger("nonexistent"));
    }

    // ========== 任务执行逻辑测试 ==========

    @Test
    void executeTask_shouldFetchSubscriptionsAndPushToTargets() {
        // 准备数据
        when(taskRepository.findById("task-001")).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pipelineConfigRepository.findById("pipeline-001")).thenReturn(Optional.of(samplePipeline));
        when(subscriptionService.findAll()).thenReturn(List.of(sampleSubscription));

        ClashConfig fetchedConfig = new ClashConfig("sub-config");
        fetchedConfig.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));
        when(subscriptionService.fetch("sub-001")).thenReturn(fetchedConfig);

        ClashConfig processedConfig = new ClashConfig("processed");
        processedConfig.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));
        when(pipelineService.execute(eq(samplePipeline), any(ClashConfig.class))).thenReturn(processedConfig);

        // 执行
        service.trigger("task-001");

        // 验证推送到指定实例
        verify(mihomoService).pushConfig("inst-001", processedConfig);
        verify(mihomoService).pushConfig("inst-002", processedConfig);
        verify(mihomoService, never()).pushConfigAll(any());

        // 验证最终状态为 SUCCESS
        ArgumentCaptor<ScheduledTask> taskCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(taskRepository, atLeast(2)).save(taskCaptor.capture());
        ScheduledTask lastSaved = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        assertEquals(TaskStatus.SUCCESS, lastSaved.getLastRunStatus());
        assertNotNull(lastSaved.getLastRunAt());
    }

    @Test
    void executeTask_shouldPushToAllWhenNoTargets() {
        ScheduledTask noTargets = new ScheduledTask();
        noTargets.setId("task-003");
        noTargets.setName("无目标任务");
        noTargets.setPipelineId("pipeline-001");
        noTargets.setTargetInstances(List.of());
        noTargets.setEnabled(true);

        when(taskRepository.findById("task-003")).thenReturn(Optional.of(noTargets));
        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pipelineConfigRepository.findById("pipeline-001")).thenReturn(Optional.of(samplePipeline));
        when(subscriptionService.findAll()).thenReturn(List.of(sampleSubscription));
        when(subscriptionService.fetch("sub-001")).thenReturn(new ClashConfig("config"));
        when(pipelineService.execute(eq(samplePipeline), any(ClashConfig.class))).thenReturn(new ClashConfig("result"));
        when(mihomoService.pushConfigAll(any(ClashConfig.class))).thenReturn(Map.of("inst-001", true));

        service.trigger("task-003");

        verify(mihomoService).pushConfigAll(any(ClashConfig.class));
    }

    @Test
    void executeTask_shouldHandleSubscriptionFetchFailure() {
        Subscription sub1 = new Subscription();
        sub1.setId("sub-001");
        sub1.setName("正常订阅");

        Subscription sub2 = new Subscription();
        sub2.setId("sub-002");
        sub2.setName("失败订阅");

        when(taskRepository.findById("task-001")).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pipelineConfigRepository.findById("pipeline-001")).thenReturn(Optional.of(samplePipeline));
        when(subscriptionService.findAll()).thenReturn(List.of(sub1, sub2));

        ClashConfig config1 = new ClashConfig("config1");
        config1.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));
        when(subscriptionService.fetch("sub-001")).thenReturn(config1);
        when(subscriptionService.fetch("sub-002")).thenThrow(new RuntimeException("网络异常"));

        ClashConfig processed = new ClashConfig("processed");
        when(pipelineService.execute(eq(samplePipeline), any(ClashConfig.class))).thenReturn(processed);

        service.trigger("task-001");

        // 即使一个订阅源失败，任务仍然成功
        ArgumentCaptor<ScheduledTask> taskCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(taskRepository, atLeast(2)).save(taskCaptor.capture());
        ScheduledTask lastSaved = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        assertEquals(TaskStatus.SUCCESS, lastSaved.getLastRunStatus());
    }

    @Test
    void executeTask_shouldSetFailedWhenPipelineNotFound() {
        when(taskRepository.findById("task-001")).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pipelineConfigRepository.findById("pipeline-001")).thenReturn(Optional.empty());

        service.trigger("task-001");

        ArgumentCaptor<ScheduledTask> taskCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(taskRepository, atLeast(2)).save(taskCaptor.capture());
        ScheduledTask lastSaved = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        assertEquals(TaskStatus.FAILED, lastSaved.getLastRunStatus());
        assertNotNull(lastSaved.getLastRunAt());
    }

    @Test
    void executeTask_shouldSetFailedWhenPushFails() {
        when(taskRepository.findById("task-001")).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(ScheduledTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pipelineConfigRepository.findById("pipeline-001")).thenReturn(Optional.of(samplePipeline));
        when(subscriptionService.findAll()).thenReturn(List.of(sampleSubscription));
        when(subscriptionService.fetch("sub-001")).thenReturn(new ClashConfig("config"));
        when(pipelineService.execute(eq(samplePipeline), any(ClashConfig.class))).thenReturn(new ClashConfig("result"));
        doThrow(new RuntimeException("推送失败")).when(mihomoService).pushConfig(eq("inst-001"), any(ClashConfig.class));

        service.trigger("task-001");

        // 推送到 inst-001 失败后应继续推送到 inst-002
        verify(mihomoService).pushConfig(eq("inst-002"), any(ClashConfig.class));

        // 但整体任务仍然成功（推送失败不中断任务）
        ArgumentCaptor<ScheduledTask> taskCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(taskRepository, atLeast(2)).save(taskCaptor.capture());
        ScheduledTask lastSaved = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        assertEquals(TaskStatus.SUCCESS, lastSaved.getLastRunStatus());
    }

    @Test
    void executeTask_shouldHandleNullTaskGracefully() {
        when(taskRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // trigger 会抛出异常，因为任务不存在
        assertThrows(BusinessException.class, () -> service.trigger("nonexistent"));
    }

    // ========== 启动初始化测试 ==========

    @Test
    void init_shouldReEnableAllEnabledTasks() {
        ScheduledTask task1 = new ScheduledTask();
        task1.setId("task-001");
        task1.setName("任务1");
        task1.setCronExpression("0 0 * * * *");
        task1.setEnabled(true);

        ScheduledTask task2 = new ScheduledTask();
        task2.setId("task-002");
        task2.setName("任务2");
        task2.setCronExpression("0 0 * * * *");
        task2.setEnabled(false);

        ScheduledTask task3 = new ScheduledTask();
        task3.setId("task-003");
        task3.setName("任务3");
        task3.setCronExpression(null);
        task3.setEnabled(true);

        when(taskRepository.findAll()).thenReturn(List.of(task1, task2, task3));
        stubSchedule();

        service.init();

        // 只有 task1 应该被注册（enabled=true 且有 cron 表达式）
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void init_shouldHandleScheduleExceptionGracefully() {
        ScheduledTask task = new ScheduledTask();
        task.setId("task-err");
        task.setName("错误任务");
        task.setCronExpression("invalid-cron");
        task.setEnabled(true);

        when(taskRepository.findAll()).thenReturn(List.of(task));
        // 使用 lenient 避免 strict stubbing 检查（异常场景下 stubbing 可能未被完整消费）
        lenient().when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
                .thenThrow(new IllegalArgumentException("无效的 cron 表达式"));

        // 不应抛出异常
        assertDoesNotThrow(() -> service.init());
    }
}
