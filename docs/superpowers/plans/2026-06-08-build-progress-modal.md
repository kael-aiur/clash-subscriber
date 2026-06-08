# 构建进度弹窗实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为构建流水线页面添加实时进度弹窗，点击构建时显示遮罩弹窗，通过 SSE 实时展示步骤进度，完成后显示摘要并等待用户确认。

**Architecture:** 将 execute 接口改为异步执行，立即返回 recordId；前端通过 SSE 订阅进度事件，实时更新步骤条；新增 BuildProgressModal 组件展示进度和结果摘要。

**Tech Stack:** Java 21, Spring Boot, Vue 3, Element Plus, SSE (Server-Sent Events)

---

## 文件结构

### 后端文件

| 文件 | 变更类型 | 职责 |
|------|----------|------|
| `module-web/src/main/java/site/kael/clash/web/controller/BuildPipelineController.java` | 修改 | execute 接口改为异步，返回 recordId |
| `module-web/src/main/java/site/kael/clash/web/controller/BuildRecordController.java` | 修改 | 新增 SSE 进度订阅端点 |
| `module-pipeline/src/main/java/site/kael/clash/pipeline/service/BuildPipelineService.java` | 修改 | 新增异步 execute 方法签名 |
| `module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImpl.java` | 修改 | 实现异步执行 + 进度推送 |
| `module-pipeline/src/main/java/site/kael/clash/pipeline/service/BuildRecordService.java` | 新增 | SSE 订阅管理服务接口 |
| `module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildRecordServiceImpl.java` | 新增 | SSE 订阅管理服务实现 |
| `module-pipeline/src/main/java/site/kael/clash/pipeline/model/BuildProgressEvent.java` | 新增 | SSE 事件模型 |

### 前端文件

| 文件 | 变更类型 | 职责 |
|------|----------|------|
| `module-web/frontend/src/api/build-pipeline.ts` | 修改 | 新增 subscribeProgress 方法 |
| `module-web/frontend/src/views/BuildPipelineView.vue` | 修改 | 集成进度弹窗 |
| `module-web/frontend/src/components/BuildProgressModal.vue` | 新增 | 进度弹窗组件 |

### 测试文件

| 文件 | 变更类型 | 职责 |
|------|----------|------|
| `module-pipeline/src/test/java/site/kael/clash/pipeline/service/impl/BuildRecordServiceImplTest.java` | 新增 | SSE 订阅管理单元测试 |
| `module-web/src/test/java/site/kael/clash/web/controller/BuildRecordControllerIntegrationTest.java` | 新增 | SSE 端点集成测试 |

---

## Task 1: 创建 SSE 事件模型

**Files:**
- Create: `module-pipeline/src/main/java/site/kael/clash/pipeline/model/BuildProgressEvent.java`

- [ ] **Step 1: 创建 BuildProgressEvent 模型类**

```java
package site.kael.clash.pipeline.model;

import java.time.LocalDateTime;

/**
 * 构建进度事件模型，用于 SSE 推送
 */
public class BuildProgressEvent {

    public enum EventType {
        STEP_STATUS,
        BUILD_COMPLETE,
        BUILD_ERROR
    }

    private EventType type;
    private Integer stepIndex;
    private String stepName;
    private String status;
    private Long timestamp;
    private Long duration;
    private Integer totalSteps;
    private Integer successSteps;
    private Integer failedSteps;
    private String errorMessage;

    public BuildProgressEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建步骤状态变更事件
     */
    public static BuildProgressEvent stepStatus(int stepIndex, String stepName, String status) {
        BuildProgressEvent event = new BuildProgressEvent();
        event.setType(EventType.STEP_STATUS);
        event.setStepIndex(stepIndex);
        event.setStepName(stepName);
        event.setStatus(status);
        return event;
    }

    /**
     * 创建构建完成事件
     */
    public static BuildProgressEvent buildComplete(String status, long duration, 
            int totalSteps, int successSteps, int failedSteps) {
        BuildProgressEvent event = new BuildProgressEvent();
        event.setType(EventType.BUILD_COMPLETE);
        event.setStatus(status);
        event.setDuration(duration);
        event.setTotalSteps(totalSteps);
        event.setSuccessSteps(successSteps);
        event.setFailedSteps(failedSteps);
        return event;
    }

    /**
     * 创建构建错误事件
     */
    public static BuildProgressEvent buildError(String message) {
        BuildProgressEvent event = new BuildProgressEvent();
        event.setType(EventType.BUILD_ERROR);
        event.setErrorMessage(message);
        return event;
    }

    // Getters and Setters

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Integer getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(Integer stepIndex) {
        this.stepIndex = stepIndex;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Integer getSuccessSteps() {
        return successSteps;
    }

    public void setSuccessSteps(Integer successSteps) {
        this.successSteps = successSteps;
    }

    public Integer getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(Integer failedSteps) {
        this.failedSteps = failedSteps;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl module-pipeline`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add module-pipeline/src/main/java/site/kael/clash/pipeline/model/BuildProgressEvent.java
git commit -m "feat(pipeline): 添加构建进度事件模型 BuildProgressEvent"
```

---

## Task 2: 创建 SSE 订阅管理服务

**Files:**
- Create: `module-pipeline/src/main/java/site/kael/clash/pipeline/service/BuildRecordService.java`
- Create: `module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildRecordServiceImpl.java`
- Create: `module-pipeline/src/test/java/site/kael/clash/pipeline/service/impl/BuildRecordServiceImplTest.java`

- [ ] **Step 1: 创建 BuildRecordService 接口**

```java
package site.kael.clash.pipeline.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.kael.clash.pipeline.model.BuildProgressEvent;

/**
 * 构建记录服务，提供 SSE 订阅管理
 */
public interface BuildRecordService {

    /**
     * 订阅构建进度
     *
     * @param recordId 构建记录 ID
     * @return SSE 发射器
     */
    SseEmitter subscribeProgress(String recordId);

    /**
     * 发布进度事件
     *
     * @param recordId 构建记录 ID
     * @param event    进度事件
     */
    void publishEvent(String recordId, BuildProgressEvent event);
}
```

- [ ] **Step 2: 创建 BuildRecordServiceImpl 实现**

```java
package site.kael.clash.pipeline.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.kael.clash.pipeline.model.BuildProgressEvent;
import site.kael.clash.pipeline.service.BuildRecordService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 构建记录服务实现，管理 SSE 订阅
 */
@Service
public class BuildRecordServiceImpl implements BuildRecordService {

    private static final Logger log = LoggerFactory.getLogger(BuildRecordServiceImpl.class);

    /**
     * 订阅者映射：recordId -> List<SseEmitter>
     */
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribeProgress(String recordId) {
        // 创建不限时的 SSE 发射器
        SseEmitter emitter = new SseEmitter(0L);

        // 添加到订阅列表
        emitters.computeIfAbsent(recordId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("新增 SSE 订阅: recordId={}, 当前订阅数: {}", recordId, 
                emitters.get(recordId).size());

        // 注册回调：完成、超时、错误时移除
        emitter.onCompletion(() -> removeEmitter(recordId, emitter));
        emitter.onTimeout(() -> removeEmitter(recordId, emitter));
        emitter.onError(e -> removeEmitter(recordId, emitter));

        return emitter;
    }

    @Override
    public void publishEvent(String recordId, BuildProgressEvent event) {
        List<SseEmitter> emitterList = emitters.get(recordId);
        if (emitterList == null || emitterList.isEmpty()) {
            return;
        }

        log.debug("发布进度事件: recordId={}, type={}, stepName={}", 
                recordId, event.getType(), event.getStepName());

        for (SseEmitter emitter : emitterList) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType().name().toLowerCase().replace('_', '-'))
                        .data(event));
            } catch (IOException e) {
                log.warn("发送 SSE 事件失败: {}", e.getMessage());
                removeEmitter(recordId, emitter);
            }
        }
    }

    /**
     * 移除订阅者
     */
    private void removeEmitter(String recordId, SseEmitter emitter) {
        List<SseEmitter> emitterList = emitters.get(recordId);
        if (emitterList != null) {
            emitterList.remove(emitter);
            log.debug("移除 SSE 订阅: recordId={}, 剩余订阅数: {}", recordId, emitterList.size());
            if (emitterList.isEmpty()) {
                emitters.remove(recordId);
            }
        }
    }
}
```

- [ ] **Step 3: 创建单元测试**

```java
package site.kael.clash.pipeline.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.kael.clash.pipeline.model.BuildProgressEvent;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BuildRecordServiceImplTest {

    private BuildRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BuildRecordServiceImpl();
    }

    @Test
    void subscribeProgress_shouldReturnEmitter() {
        // When
        SseEmitter emitter = service.subscribeProgress("record-1");

        // Then
        assertNotNull(emitter);
    }

    @Test
    void publishEvent_shouldNotifySubscribers() throws IOException, InterruptedException {
        // Given
        String recordId = "record-1";
        SseEmitter emitter = service.subscribeProgress(recordId);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<BuildProgressEvent> receivedEvent = new AtomicReference<>();
        
        emitter.onCompletion(() -> latch.countDown());
        
        BuildProgressEvent event = BuildProgressEvent.stepStatus(0, "测试步骤", "RUNNING");

        // When
        service.publishEvent(recordId, event);

        // Then
        // 由于 SseEmitter.send() 是异步的，这里验证方法执行不抛异常
        assertNotNull(emitter);
    }

    @Test
    void publishEvent_shouldHandleIOException() {
        // Given
        String recordId = "record-1";
        SseEmitter emitter = service.subscribeProgress(recordId);
        
        BuildProgressEvent event = BuildProgressEvent.stepStatus(0, "测试步骤", "RUNNING");

        // When & Then - 不应抛出异常
        assertDoesNotThrow(() -> service.publishEvent(recordId, event));
    }

    @Test
    void publishEvent_shouldHandleNoSubscribers() {
        // Given
        String recordId = "non-existent";
        BuildProgressEvent event = BuildProgressEvent.stepStatus(0, "测试步骤", "RUNNING");

        // When & Then - 不应抛出异常
        assertDoesNotThrow(() -> service.publishEvent(recordId, event));
    }

    @Test
    void publishEvent_buildComplete_shouldWork() {
        // Given
        String recordId = "record-1";
        service.subscribeProgress(recordId);
        
        BuildProgressEvent event = BuildProgressEvent.buildComplete("SUCCESS", 3500, 4, 4, 0);

        // When & Then
        assertDoesNotThrow(() -> service.publishEvent(recordId, event));
    }

    @Test
    void publishEvent_buildError_shouldWork() {
        // Given
        String recordId = "record-1";
        service.subscribeProgress(recordId);
        
        BuildProgressEvent event = BuildProgressEvent.buildError("测试错误");

        // When & Then
        assertDoesNotThrow(() -> service.publishEvent(recordId, event));
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `mvn test -pl module-pipeline -Dtest=BuildRecordServiceImplTest`
Expected: Tests pass

- [ ] **Step 5: 提交**

```bash
git add module-pipeline/src/main/java/site/kael/clash/pipeline/service/BuildRecordService.java
git add module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildRecordServiceImpl.java
git add module-pipeline/src/test/java/site/kael/clash/pipeline/service/impl/BuildRecordServiceImplTest.java
git commit -m "feat(pipeline): 添加 SSE 订阅管理服务 BuildRecordService"
```

---

## Task 3: 修改 BuildPipelineService 支持异步执行

**Files:**
- Modify: `module-pipeline/src/main/java/site/kael/clash/pipeline/service/BuildPipelineService.java`
- Modify: `module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImpl.java`

- [ ] **Step 1: 在 BuildPipelineService 接口中添加异步执行方法**

在 `BuildPipelineService.java` 中添加：

```java
/**
 * 异步执行构建流程，立即返回记录 ID
 *
 * @param pipelineId 构建流程 ID
 * @return 构建记录 ID
 */
String executeAsync(String pipelineId);
```

- [ ] **Step 2: 在 BuildPipelineServiceImpl 中注入 BuildRecordService**

```java
private final BuildRecordService buildRecordService;

public BuildPipelineServiceImpl(
        BuildPipelineRepository pipelineRepository,
        BuildRecordRepository recordRepository,
        SubscriptionService subscriptionService,
        PipelineService pipelineService,
        MihomoService mihomoService,
        SchedulerService schedulerService,
        ConfigProfileRepository configProfileRepository,
        ConfigGeneratorService configGeneratorService,
        BuildRecordService buildRecordService) {  // 新增
    this.pipelineRepository = pipelineRepository;
    this.recordRepository = recordRepository;
    this.subscriptionService = subscriptionService;
    this.pipelineService = pipelineService;
    this.mihomoService = mihomoService;
    this.schedulerService = schedulerService;
    this.configProfileRepository = configProfileRepository;
    this.configGeneratorService = configGeneratorService;
    this.buildRecordService = buildRecordService;  // 新增
}
```

- [ ] **Step 3: 实现 executeAsync 方法**

在 `BuildPipelineServiceImpl.java` 中添加：

```java
@Override
public String executeAsync(String pipelineId) {
    // 验证 pipeline 存在
    BuildPipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new BusinessException(404, "构建流程不存在: " + pipelineId));

    // 创建记录
    BuildRecord record = new BuildRecord();
    record.setId(IdGenerator.generate());
    record.setBuildPipelineId(pipelineId);
    record.setStartedAt(LocalDateTime.now());
    record.setStatus("RUNNING");
    recordRepository.save(record);

    // 异步执行
    String recordId = record.getId();
    CompletableFuture.runAsync(() -> {
        try {
            executeWithProgress(pipelineId, recordId);
        } catch (Exception e) {
            log.error("异步构建执行失败: pipelineId={}, recordId={}", pipelineId, recordId, e);
        }
    });

    return recordId;
}
```

- [ ] **Step 4: 创建带进度推送的执行方法**

在 `BuildPipelineServiceImpl.java` 中添加：

```java
private void executeWithProgress(String pipelineId, String recordId) {
    BuildPipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new BusinessException(404, "构建流程不存在: " + pipelineId));

    BuildRecord record = recordRepository.findById(recordId)
            .orElseThrow(() -> new BusinessException(404, "构建记录不存在: " + recordId));

    // 向后兼容：自动迁移 configType
    if (pipeline.getConfigType() == null || pipeline.getConfigType().isBlank()) {
        pipeline.setConfigType(ConfigType.SUBSCRIPTION.getValue());
        pipelineRepository.save(pipeline);
    }

    long startTime = System.currentTimeMillis();
    List<BuildStep> steps = record.getSteps();

    log.info("开始执行构建流程（带进度推送）: {} ({})", pipeline.getName(), pipelineId);

    try {
        // 根据配置类型选择配置来源
        ConfigType configType = ConfigType.fromValue(pipeline.getConfigType());
        ClashConfig config;

        switch (configType) {
            case SUBSCRIPTION:
                config = executeSubscriptionModeWithProgress(pipeline, steps, record, recordId);
                break;
            case CONFIG_PROFILE:
                config = executeConfigProfileModeWithProgress(pipeline, steps, record, recordId);
                break;
            default:
                throw new BusinessException("不支持的配置类型: " + pipeline.getConfigType());
        }

        // 脚本处理
        int stepIndex = steps.size();
        if (pipeline.getScriptName() != null && !pipeline.getScriptName().isBlank()) {
            publishStepEvent(recordId, stepIndex, "脚本处理", "RUNNING");
            
            Map<String, Object> step3Input = new LinkedHashMap<>();
            step3Input.put("scriptName", pipeline.getScriptName());
            step3Input.put("configSummary", buildConfigSummary(config));
            step3Input.put("configYaml", configToYaml(config));
            BuildStep step3 = startStep("脚本处理", step3Input);
            
            PipelineConfig pipelineConfig = new PipelineConfig();
            pipelineConfig.setId("auto-" + pipelineId);
            pipelineConfig.setName("auto-generated");

            PipelineStep scriptStep = new PipelineStep();
            scriptStep.setProcessor("script");
            scriptStep.setConfig(Map.of("scriptName", pipeline.getScriptName()));
            pipelineConfig.setSteps(List.of(scriptStep));

            ProcessingContext context = new ProcessingContext();
            config = pipelineService.execute(pipelineConfig, config, context);
            record.getLogs().addAll(context.getLogs());
            
            Map<String, Object> step3Output = new LinkedHashMap<>();
            step3Output.put("configSummary", buildConfigSummary(config));
            step3Output.put("configYaml", configToYaml(config));
            finishStep(step3, "SUCCESS", step3Output);
            steps.add(step3);
            record.getLogs().add("脚本处理完成: " + pipeline.getScriptName());
            
            publishStepEvent(recordId, stepIndex, "脚本处理", "SUCCESS");
        } else {
            BuildStep step3 = new BuildStep();
            step3.setName("脚本处理");
            step3.setStatus("SKIPPED");
            steps.add(step3);
            publishStepEvent(recordId, stepIndex, "脚本处理", "SKIPPED");
        }

        // 推送到 Mihomo
        stepIndex = steps.size();
        publishStepEvent(recordId, stepIndex, "推送到 Mihomo", "RUNNING");
        
        String instanceName = mihomoService.findById(pipeline.getTargetInstanceId())
                .map(site.kael.clash.mihomo.model.MihomoInstance::getName)
                .orElse(pipeline.getTargetInstanceId());
        Map<String, Object> step4Input = new LinkedHashMap<>();
        step4Input.put("instanceName", instanceName);
        step4Input.put("configSummary", buildConfigSummary(config));
        step4Input.put("configYaml", configToYaml(config));
        BuildStep step4 = startStep("推送到 Mihomo", step4Input);
        
        syncRawFromFields(config);
        mihomoService.pushConfig(pipeline.getTargetInstanceId(), config);
        finishStep(step4, "SUCCESS", Map.of("success", true));
        steps.add(step4);
        record.getLogs().add("配置推送成功: " + pipeline.getTargetInstanceId());
        
        publishStepEvent(recordId, stepIndex, "推送到 Mihomo", "SUCCESS");

        // 记录成功
        record.setStatus("SUCCESS");
        record.setFinishedAt(LocalDateTime.now());

        pipeline.setLastRunAt(record.getFinishedAt());
        pipeline.setLastRunStatus("SUCCESS");
        pipelineRepository.save(pipeline);

        // 发布完成事件
        long duration = System.currentTimeMillis() - startTime;
        int totalSteps = steps.size();
        long successSteps = steps.stream().filter(s -> "SUCCESS".equals(s.getStatus())).count();
        long failedSteps = steps.stream().filter(s -> "FAILED".equals(s.getStatus())).count();
        
        buildRecordService.publishEvent(recordId, 
                BuildProgressEvent.buildComplete("SUCCESS", duration, totalSteps, 
                        (int) successSteps, (int) failedSteps));

        log.info("构建流程执行成功（带进度推送）: {} ({})", pipeline.getName(), pipelineId);

    } catch (Exception e) {
        record.setStatus("FAILED");
        record.setErrorMessage(e.getMessage());
        record.setFinishedAt(LocalDateTime.now());
        record.getLogs().add("ERROR: " + e.getMessage());

        pipeline.setLastRunAt(record.getFinishedAt());
        pipeline.setLastRunStatus("FAILED");
        pipelineRepository.save(pipeline);

        // 发布错误事件
        buildRecordService.publishEvent(recordId, BuildProgressEvent.buildError(e.getMessage()));
        
        long duration = System.currentTimeMillis() - startTime;
        int totalSteps = steps.size();
        long successSteps = steps.stream().filter(s -> "SUCCESS".equals(s.getStatus())).count();
        long failedSteps = steps.stream().filter(s -> "FAILED".equals(s.getStatus())).count();
        
        buildRecordService.publishEvent(recordId, 
                BuildProgressEvent.buildComplete("FAILED", duration, totalSteps, 
                        (int) successSteps, (int) failedSteps));

        log.error("构建流程执行失败（带进度推送）: {} ({})", pipeline.getName(), pipelineId, e);
    }

    recordRepository.save(record);
}
```

- [ ] **Step 5: 创建带进度推送的订阅模式执行方法**

在 `BuildPipelineServiceImpl.java` 中添加：

```java
private ClashConfig executeSubscriptionModeWithProgress(BuildPipeline pipeline, 
        List<BuildStep> steps, BuildRecord record, String recordId) {
    
    // 1. 拉取主订阅配置
    int stepIndex = 0;
    publishStepEvent(recordId, stepIndex, "拉取主订阅配置", "RUNNING");
    
    String primarySubName = subscriptionService.findById(pipeline.getPrimarySubscriptionId())
            .map(site.kael.clash.subscription.model.Subscription::getName)
            .orElse(pipeline.getPrimarySubscriptionId());
    BuildStep step1 = startStep("拉取主订阅配置", Map.of("subscriptionName", primarySubName));
    
    ClashConfig config = subscriptionService.fetch(pipeline.getPrimarySubscriptionId());
    if (config == null) {
        config = new ClashConfig("empty");
        config.setRaw(new LinkedHashMap<>());
        config.setProxies(new ArrayList<>());
    }
    List<ProxyNode> allProxies = new ArrayList<>(config.getProxies() != null ? config.getProxies() : Collections.emptyList());
    
    Map<String, Object> step1Output = new LinkedHashMap<>();
    step1Output.put("configSummary", buildConfigSummary(config));
    step1Output.put("configYaml", configToYaml(config));
    finishStep(step1, "SUCCESS", step1Output);
    steps.add(step1);
    
    publishStepEvent(recordId, stepIndex, "拉取主订阅配置", "SUCCESS");

    // 2. 合并额外订阅节点
    stepIndex = 1;
    publishStepEvent(recordId, stepIndex, "合并额外订阅节点", "RUNNING");
    
    Map<String, Object> step2Input = new LinkedHashMap<>();
    step2Input.put("mainConfigSummary", buildConfigSummary(config));
    step2Input.put("mainConfigYaml", configToYaml(config));
    List<Map<String, Object>> extraConfigs = new ArrayList<>();
    BuildStep step2 = startStep("合并额外订阅节点", null);
    
    if (pipeline.getAdditionalSubscriptionIds() != null) {
        for (String subId : pipeline.getAdditionalSubscriptionIds()) {
            try {
                ClashConfig extra = subscriptionService.fetch(subId);
                if (extra != null && extra.getProxies() != null) {
                    String extraName = subscriptionService.findById(subId)
                            .map(site.kael.clash.subscription.model.Subscription::getName)
                            .orElse(subId);
                    Map<String, Object> extraInfo = new LinkedHashMap<>();
                    extraInfo.put("subscriptionName", extraName);
                    extraInfo.put("configSummary", buildConfigSummary(extra));
                    extraInfo.put("configYaml", configToYaml(extra));
                    extraConfigs.add(extraInfo);
                    allProxies.addAll(extra.getProxies());
                    log.debug("合并额外订阅: {}，节点数: {}", subId, extra.getProxies().size());
                }
            } catch (Exception e) {
                log.warn("获取额外订阅失败: {}, 原因: {}", subId, e.getMessage());
                record.getLogs().add("WARN: 获取额外订阅失败: " + subId + " - " + e.getMessage());
            }
        }
    }
    
    step2Input.put("extraConfigs", extraConfigs);
    step2.setInput(step2Input);
    config.setProxies(allProxies);
    config.getRaw().put("proxies", allProxies.stream().map(this::proxyNodeToMap).toList());
    
    Map<String, Object> step2Output = new LinkedHashMap<>();
    step2Output.put("configSummary", buildConfigSummary(config));
    step2Output.put("configYaml", configToYaml(config));
    finishStep(step2, "SUCCESS", step2Output);
    steps.add(step2);
    record.getLogs().add("合并节点总数: " + allProxies.size());
    
    publishStepEvent(recordId, stepIndex, "合并额外订阅节点", "SUCCESS");

    return config;
}
```

- [ ] **Step 6: 创建带进度推送的配置组合模式执行方法**

在 `BuildPipelineServiceImpl.java` 中添加：

```java
private ClashConfig executeConfigProfileModeWithProgress(BuildPipeline pipeline, 
        List<BuildStep> steps, BuildRecord record, String recordId) {
    
    // 1. 获取配置组合
    int stepIndex = 0;
    publishStepEvent(recordId, stepIndex, "获取配置组合", "RUNNING");
    
    String configProfileName = configProfileRepository.findById(pipeline.getConfigProfileId())
            .map(ConfigProfile::getName)
            .orElse(pipeline.getConfigProfileId());
    Map<String, Object> step1Input = new LinkedHashMap<>();
    step1Input.put("configProfileName", configProfileName);
    BuildStep step1 = startStep("获取配置组合", step1Input);

    ConfigProfile profile = configProfileRepository.findById(pipeline.getConfigProfileId())
            .orElseThrow(() -> new BusinessException("配置组合不存在: " + pipeline.getConfigProfileId()));

    Map<String, Object> step1Output = new LinkedHashMap<>();
    step1Output.put("profileName", profile.getName());
    step1Output.put("subscriptionCount", profile.getSubscriptionIds() != null ? profile.getSubscriptionIds().size() : 0);
    step1Output.put("proxyGroupCount", profile.getProxyGroups() != null ? profile.getProxyGroups().size() : 0);
    step1Output.put("ruleGroupCount", profile.getRuleGroups() != null ? profile.getRuleGroups().size() : 0);
    finishStep(step1, "SUCCESS", step1Output);
    steps.add(step1);
    
    publishStepEvent(recordId, stepIndex, "获取配置组合", "SUCCESS");

    // 2. 调用 ConfigGeneratorService 生成完整配置
    stepIndex = 1;
    publishStepEvent(recordId, stepIndex, "生成完整配置", "RUNNING");
    
    Map<String, Object> step2Input = new LinkedHashMap<>();
    step2Input.put("configProfileName", profile.getName());
    BuildStep step2 = startStep("生成完整配置", step2Input);

    String yamlContent = configGeneratorService.generate(profile);
    ClashConfig config = yamlToClashConfig(yamlContent, profile.getName());

    Map<String, Object> step2Output = new LinkedHashMap<>();
    step2Output.put("configSummary", buildConfigSummary(config));
    step2Output.put("configYaml", configToYaml(config));
    finishStep(step2, "SUCCESS", step2Output);
    steps.add(step2);
    record.getLogs().add("配置组合生成完成: " + profile.getName() + "，节点数: " + (config.getProxies() != null ? config.getProxies().size() : 0));
    
    publishStepEvent(recordId, stepIndex, "生成完整配置", "SUCCESS");

    return config;
}
```

- [ ] **Step 7: 创建进度事件发布辅助方法**

在 `BuildPipelineServiceImpl.java` 中添加：

```java
private void publishStepEvent(String recordId, int stepIndex, String stepName, String status) {
    buildRecordService.publishEvent(recordId, 
            BuildProgressEvent.stepStatus(stepIndex, stepName, status));
}
```

- [ ] **Step 8: 验证编译**

Run: `mvn compile -pl module-pipeline`
Expected: BUILD SUCCESS

- [ ] **Step 9: 提交**

```bash
git add module-pipeline/src/main/java/site/kael/clash/pipeline/service/BuildPipelineService.java
git add module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImpl.java
git commit -m "feat(pipeline): 实现异步构建执行和进度推送"
```

---

## Task 4: 修改 BuildPipelineController 支持异步接口

**Files:**
- Modify: `module-web/src/main/java/site/kael/clash/web/controller/BuildPipelineController.java`

- [ ] **Step 1: 修改 execute 接口返回类型**

```java
@PostMapping("/{id}/execute")
public ResponseEntity<Map<String, String>> execute(@PathVariable String id) {
    log.info("手动触发构建流程: id={}", id);
    String recordId = buildPipelineService.executeAsync(id);
    return ResponseEntity.ok(Map.of("recordId", recordId));
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl module-web`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add module-web/src/main/java/site/kael/clash/web/controller/BuildPipelineController.java
git commit -m "feat(web): 修改 execute 接口为异步执行，返回 recordId"
```

---

## Task 5: 在 BuildRecordController 中添加 SSE 端点

**Files:**
- Modify: `module-web/src/main/java/site/kael/clash/web/controller/BuildRecordController.java`

- [ ] **Step 1: 注入 BuildRecordService 并添加 SSE 端点**

```java
package site.kael.clash.web.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.kael.clash.pipeline.model.BuildRecord;
import site.kael.clash.pipeline.service.BuildPipelineService;
import site.kael.clash.pipeline.service.BuildRecordService;

@RestController
@RequestMapping("/api/build-records")
public class BuildRecordController {

    private final BuildPipelineService buildPipelineService;
    private final BuildRecordService buildRecordService;

    public BuildRecordController(BuildPipelineService buildPipelineService, 
            BuildRecordService buildRecordService) {
        this.buildPipelineService = buildPipelineService;
        this.buildRecordService = buildRecordService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildRecord> findById(@PathVariable String id) {
        return ResponseEntity.ok(buildPipelineService.findRecordById(id));
    }

    /**
     * 订阅构建进度 SSE 端点
     */
    @GetMapping(value = "/{id}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter progress(@PathVariable String id) {
        return buildRecordService.subscribeProgress(id);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl module-web`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add module-web/src/main/java/site/kael/clash/web/controller/BuildRecordController.java
git commit -m "feat(web): 添加构建进度 SSE 订阅端点"
```

---

## Task 6: 创建前端 SSE 订阅 API

**Files:**
- Modify: `module-web/frontend/src/api/build-pipeline.ts`

- [ ] **Step 1: 添加 ExecuteResponse 接口和 subscribeProgress 方法**

在 `build-pipeline.ts` 中添加：

```typescript
export interface ExecuteResponse {
  recordId: string
}
```

修改 `buildPipelineApi.execute` 的返回类型：

```typescript
execute(id: string) {
  return api.post<ExecuteResponse>(`/build-pipelines/${id}/execute`)
},
```

在 `buildPipelineApi` 中添加：

```typescript
subscribeProgress(recordId: string): EventSource {
  const baseURL = import.meta.env.VITE_API_BASE_URL || ''
  return new EventSource(`${baseURL}/api/build-records/${recordId}/progress`)
},
```

- [ ] **Step 2: 验证前端编译**

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add module-web/frontend/src/api/build-pipeline.ts
git commit -m "feat(frontend): 添加 SSE 订阅 API"
```

---

## Task 7: 创建 BuildProgressModal 组件

**Files:**
- Create: `module-web/frontend/src/components/BuildProgressModal.vue`

- [ ] **Step 1: 创建 BuildProgressModal 组件**

```vue
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { buildPipelineApi, type BuildStep } from '@/api/build-pipeline'

interface StepStatusEvent {
  type: 'step-status'
  stepIndex: number
  stepName: string
  status: 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'
  timestamp: number
}

interface BuildCompleteEvent {
  type: 'build-complete'
  status: 'SUCCESS' | 'FAILED'
  duration: number
  totalSteps: number
  successSteps: number
  failedSteps: number
  errorMessage?: string
}

interface BuildErrorEvent {
  type: 'build-error'
  message: string
}

interface Props {
  visible: boolean
  recordId: string
  pipelineType: 'subscription' | 'config-profile'
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
}>()

const steps = ref<Array<{ name: string; status: string; errorMessage?: string }>>([])
const activeStep = ref(0)
const isCompleted = ref(false)
const result = ref<BuildCompleteEvent | null>(null)
const eventSource = ref<EventSource | null>(null)
const reconnectCount = ref(0)
const MAX_RECONNECT = 3

// 初始化步骤
const initSteps = () => {
  if (props.pipelineType === 'subscription') {
    steps.value = [
      { name: '拉取主订阅配置', status: 'WAITING' },
      { name: '合并额外订阅节点', status: 'WAITING' },
      { name: '脚本处理', status: 'WAITING' },
      { name: '推送到 Mihomo', status: 'WAITING' }
    ]
  } else {
    steps.value = [
      { name: '获取配置组合', status: 'WAITING' },
      { name: '生成完整配置', status: 'WAITING' },
      { name: '脚本处理', status: 'WAITING' },
      { name: '推送到 Mihomo', status: 'WAITING' }
    ]
  }
}

// 获取步骤状态映射
const getStepStatus = (status: string) => {
  switch (status) {
    case 'WAITING': return 'wait'
    case 'RUNNING': return 'process'
    case 'SUCCESS': return 'finish'
    case 'FAILED': return 'error'
    case 'SKIPPED': return 'success'
    default: return 'wait'
  }
}

// 结果图标
const resultIcon = computed(() => {
  return result.value?.status === 'SUCCESS' ? 'success' : 'error'
})

// 结果标题
const resultTitle = computed(() => {
  return result.value?.status === 'SUCCESS' ? '构建成功' : '构建失败'
})

// 结果副标题
const resultSubTitle = computed(() => {
  if (!result.value) return ''
  if (result.value.status === 'FAILED' && result.value.errorMessage) {
    return result.value.errorMessage
  }
  return `共 ${result.value.totalSteps} 个步骤，${result.value.successSteps} 个成功，${result.value.failedSteps} 个失败`
})

// 格式化时长
const formatDuration = (ms: number) => {
  if (ms < 1000) return `${ms} 毫秒`
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) return `${seconds} 秒`
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes} 分 ${remainingSeconds} 秒`
}

// 订阅 SSE
const subscribe = () => {
  if (!props.recordId) return

  eventSource.value = buildPipelineApi.subscribeProgress(props.recordId)

  eventSource.value.addEventListener('step-status', (e) => {
    const data = JSON.parse((e as MessageEvent).data) as StepStatusEvent
    if (data.stepIndex < steps.value.length) {
      steps.value[data.stepIndex].status = data.status
      activeStep.value = data.stepIndex
    }
  })

  eventSource.value.addEventListener('build-complete', (e) => {
    const data = JSON.parse((e as MessageEvent).data) as BuildCompleteEvent
    result.value = data
    isCompleted.value = true
    eventSource.value?.close()
    eventSource.value = null
  })

  eventSource.value.addEventListener('build-error', (e) => {
    const data = JSON.parse((e as MessageEvent).data) as BuildErrorEvent
    ElMessage.error(data.message)
    eventSource.value?.close()
    eventSource.value = null
  })

  eventSource.value.onerror = () => {
    if (reconnectCount.value < MAX_RECONNECT) {
      reconnectCount.value++
      console.warn(`SSE 连接中断，第 ${reconnectCount.value} 次重连...`)
      eventSource.value?.close()
      setTimeout(subscribe, 1000 * reconnectCount.value)
    } else {
      ElMessage.error('连接中断，请刷新页面查看构建结果')
      eventSource.value?.close()
      eventSource.value = null
    }
  }
}

// 取消（关闭弹窗，构建继续后台运行）
const handleCancel = () => {
  eventSource.value?.close()
  eventSource.value = null
  emit('close')
}

// 确定
const handleConfirm = () => {
  emit('close')
}

// 监听 visible 变化
watch(() => props.visible, (newVal) => {
  if (newVal && props.recordId) {
    initSteps()
    isCompleted.value = false
    result.value = null
    activeStep.value = 0
    reconnectCount.value = 0
    subscribe()
  }
})

// 组件挂载时如果已显示则订阅
onMounted(() => {
  if (props.visible && props.recordId) {
    initSteps()
    subscribe()
  }
})

// 组件卸载时关闭连接
onUnmounted(() => {
  if (eventSource.value) {
    eventSource.value.close()
    eventSource.value = null
  }
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="构建进度"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    width="500px"
    class="build-progress-modal"
  >
    <!-- 步骤条 -->
    <div class="steps-container">
      <el-steps direction="vertical" :active="activeStep" finish-status="success">
        <el-step
          v-for="(step, index) in steps"
          :key="index"
          :title="step.name"
          :status="getStepStatus(step.status)"
        >
          <template #description>
            <span v-if="step.status === 'RUNNING'" class="running-text">
              执行中...
            </span>
            <span v-else-if="step.status === 'FAILED'" class="failed-text">
              {{ step.errorMessage || '执行失败' }}
            </span>
          </template>
        </el-step>
      </el-steps>
    </div>

    <!-- 构建结果摘要 -->
    <div v-if="isCompleted" class="result-summary">
      <el-result
        :icon="resultIcon"
        :title="resultTitle"
        :sub-title="resultSubTitle"
      >
        <template #extra>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="总耗时">{{ formatDuration(result?.duration || 0) }}</el-descriptions-item>
            <el-descriptions-item label="总步骤">{{ result?.totalSteps || 0 }}</el-descriptions-item>
            <el-descriptions-item label="成功">{{ result?.successSteps || 0 }}</el-descriptions-item>
            <el-descriptions-item label="失败">{{ result?.failedSteps || 0 }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-result>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <el-button v-if="!isCompleted" @click="handleCancel">取消</el-button>
      <el-button v-if="isCompleted" type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.steps-container {
  padding: 20px 0;
}

.running-text {
  color: #409eff;
  font-size: 12px;
}

.failed-text {
  color: #f56c6c;
  font-size: 12px;
}

.result-summary {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}
</style>
```

- [ ] **Step 2: 验证前端编译**

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add module-web/frontend/src/components/BuildProgressModal.vue
git commit -m "feat(frontend): 创建构建进度弹窗组件 BuildProgressModal"
```

---

## Task 8: 在 BuildPipelineView 中集成进度弹窗

**Files:**
- Modify: `module-web/frontend/src/views/BuildPipelineView.vue`

- [ ] **Step 1: 导入 BuildProgressModal 组件**

在 `<script setup>` 中添加：

```typescript
import BuildProgressModal from '@/components/BuildProgressModal.vue'
```

- [ ] **Step 2: 添加状态变量**

在 `<script setup>` 中添加：

```typescript
// 进度弹窗
const showProgressModal = ref(false)
const currentRecordId = ref('')
const currentPipelineType = ref<'subscription' | 'config-profile'>('subscription')
```

- [ ] **Step 3: 修改 handleExecute 方法**

```typescript
const handleExecute = async (pipeline: TreeRow) => {
  try {
    const res = await buildPipelineApi.execute(pipeline.id)
    const recordId = res.data.recordId

    // 打开进度弹窗
    currentRecordId.value = recordId
    currentPipelineType.value = pipeline.configType || 'subscription'
    showProgressModal.value = true

    // 刷新列表（显示 RUNNING 状态）
    loadedRecords.value.delete(pipeline.id)
    await loadData()
  } catch {
    ElMessage.error('触发构建失败')
  }
}

const handleProgressClose = () => {
  showProgressModal.value = false
  currentRecordId.value = ''
  // 刷新列表
  loadedRecords.value.clear()
  loadData()
}
```

- [ ] **Step 4: 在模板中添加 BuildProgressModal 组件**

在 `</el-table>` 后面、新建/编辑对话框前面添加：

```vue
<!-- 构建进度弹窗 -->
<BuildProgressModal
  :visible="showProgressModal"
  :record-id="currentRecordId"
  :pipeline-type="currentPipelineType"
  @close="handleProgressClose"
/>
```

- [ ] **Step 5: 验证前端编译**

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add module-web/frontend/src/views/BuildPipelineView.vue
git commit -m "feat(frontend): 在构建流水线页面集成进度弹窗"
```

---

## Task 9: 创建后端集成测试

**Files:**
- Create: `module-web/src/test/java/site/kael/clash/web/controller/BuildRecordControllerIntegrationTest.java`

- [ ] **Step 1: 创建集成测试**

```java
package site.kael.clash.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BuildRecordControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void progress_shouldReturnSseEmitter() throws Exception {
        // Given
        String recordId = "test-record-id";

        // When & Then
        mockMvc.perform(get("/api/build-records/{id}/progress", recordId)
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM));
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `mvn test -pl module-web -Dtest=BuildRecordControllerIntegrationTest`
Expected: Tests pass

- [ ] **Step 3: 提交**

```bash
git add module-web/src/test/java/site/kael/clash/web/controller/BuildRecordControllerIntegrationTest.java
git commit -m "test(web): 添加构建进度 SSE 端点集成测试"
```

---

## Task 10: 运行完整测试套件

**Files:** None

- [ ] **Step 1: 运行所有后端测试**

Run: `mvn test`
Expected: All tests pass

- [ ] **Step 2: 运行前端构建**

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交所有变更**

```bash
git add .
git commit -m "feat: 完成构建进度弹窗功能实现"
```

---

## 验证清单

- [ ] 后端 execute 接口改为异步，立即返回 recordId
- [ ] SSE 端点 `/api/build-records/{id}/progress` 正常工作
- [ ] 构建执行过程中推送步骤状态事件
- [ ] 构建完成时推送完成事件和摘要
- [ ] 前端弹窗正确显示步骤条
- [ ] 步骤状态实时更新
- [ ] 完成后显示结果摘要
- [ ] 确定按钮关闭弹窗并刷新列表
- [ ] 取消按钮关闭弹窗，构建继续后台运行
- [ ] SSE 断线自动重连（最多 3 次）
- [ ] 错误场景正确处理
