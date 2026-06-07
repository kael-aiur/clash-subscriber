# 构建流程配置类型实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为构建流程增加配置类型选择，支持订阅源模式和配置组合模式，每次构建重新生成配置，保持向后兼容性。

**Architecture:** 扩展 BuildPipeline 模型，增加 configType 和 configProfileId 字段。修改 BuildPipelineServiceImpl.execute() 方法，根据 configType 选择不同的配置来源。配置组合模式下直接调用 ConfigGeneratorService.generate() 生成完整配置，不使用缓存。

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Maven

---

## Task 1: 数据库迁移脚本

**Files:**
- Create: `module-pipeline/src/main/resources/db/migration/V20240101_001__add_config_type_to_build_pipeline.sql`
- Create: `module-pipeline/src/main/resources/db/migration/V20240101_001__add_config_type_to_build_pipeline_rollback.sql`

- [ ] **Step 1: 创建数据库迁移脚本**

```sql
-- V20240101_001__add_config_type_to_build_pipeline.sql

-- 步骤 1：新增字段
ALTER TABLE build_pipeline ADD COLUMN config_type VARCHAR(20) DEFAULT 'subscription';
ALTER TABLE build_pipeline ADD COLUMN config_profile_id VARCHAR(64);

-- 步骤 2：更新现有数据
UPDATE build_pipeline SET config_type = 'subscription' WHERE config_type IS NULL;

-- 步骤 3：添加索引
CREATE INDEX idx_build_pipeline_config_type ON build_pipeline(config_type);
CREATE INDEX idx_build_pipeline_config_profile_id ON build_pipeline(config_profile_id);

-- 步骤 4：验证数据
SELECT 
    config_type,
    COUNT(*) as count,
    SUM(CASE WHEN config_type IS NULL THEN 1 ELSE 0 END) as null_count
FROM build_pipeline 
GROUP BY config_type;
```

- [ ] **Step 2: 创建回滚脚本**

```sql
-- V20240101_001__add_config_type_to_build_pipeline_rollback.sql

-- 步骤 1：删除索引
DROP INDEX IF EXISTS idx_build_pipeline_config_type;
DROP INDEX IF EXISTS idx_build_pipeline_config_profile_id;

-- 步骤 2：删除字段
ALTER TABLE build_pipeline DROP COLUMN IF EXISTS config_type;
ALTER TABLE build_pipeline DROP COLUMN IF EXISTS config_profile_id;

-- 步骤 3：验证回滚
SELECT * FROM build_pipeline LIMIT 1;
```

- [ ] **Step 3: 验证脚本语法**

Run: `psql -h localhost -U postgres -d clash_subscriber -f module-pipeline/src/main/resources/db/migration/V20240101_001__add_config_type_to_build_pipeline.sql`
Expected: 脚本执行成功，无语法错误

- [ ] **Step 4: Commit**

```bash
git add module-pipeline/src/main/resources/db/migration/
git commit -m "db: 添加构建流程配置类型迁移脚本"
```

---

## Task 2: 修改 BuildPipeline 模型

**Files:**
- Modify: `module-pipeline/src/main/java/site/kael/clash/pipeline/model/BuildPipeline.java`
- Create: `module-pipeline/src/main/java/site/kael/clash/pipeline/model/ConfigType.java`

- [ ] **Step 1: 创建 ConfigType 枚举类**

```java
package site.kael.clash.pipeline.model;

/**
 * 构建流程配置类型枚举
 */
public enum ConfigType {
    SUBSCRIPTION("subscription"),      // 订阅源模式
    CONFIG_PROFILE("config-profile");  // 配置组合模式
    
    private final String value;
    
    ConfigType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static ConfigType fromValue(String value) {
        for (ConfigType type : ConfigType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的配置类型: " + value);
    }
}
```

- [ ] **Step 2: 修改 BuildPipeline 模型，增加新字段**

```java
package site.kael.clash.pipeline.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BuildPipeline {
    private String id;
    private String name;
    private String primarySubscriptionId;
    private List<String> additionalSubscriptionIds = new ArrayList<>();
    private String scriptName;
    private String targetInstanceId;
    private String cronExpression;
    private boolean enabled = true;
    
    // 新增字段
    private String configType;        // "subscription" 或 "config-profile"
    private String configProfileId;   // 配置组合模式使用
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRunAt;
    private String lastRunStatus;

    // 现有 getter/setter 保持不变
    
    public String getConfigType() { return configType; }
    public void setConfigType(String configType) { this.configType = configType; }
    public String getConfigProfileId() { return configProfileId; }
    public void setConfigProfileId(String configProfileId) { this.configProfileId = configProfileId; }
    
    /**
     * 验证配置类型和必填字段
     */
    public void validate() {
        if (configType == null || "subscription".equals(configType)) {
            // 订阅源模式验证
            if (primarySubscriptionId == null || primarySubscriptionId.isBlank()) {
                throw new IllegalArgumentException("主订阅不能为空");
            }
        } else if ("config-profile".equals(configType)) {
            // 配置组合模式验证
            if (configProfileId == null || configProfileId.isBlank()) {
                throw new IllegalArgumentException("配置组合不能为空");
            }
        } else {
            throw new IllegalArgumentException("不支持的配置类型: " + configType);
        }
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl module-pipeline`
Expected: 编译成功，无错误

- [ ] **Step 4: Commit**

```bash
git add module-pipeline/src/main/java/site/kael/clash/pipeline/model/
git commit -m "feat(model): 为 BuildPipeline 增加配置类型字段"
```

---

## Task 3: 修改 BuildPipelineRepository

**Files:**
- Modify: `module-pipeline/src/main/java/site/kael/clash/pipeline/repository/BuildPipelineRepository.java`
- Modify: `module-pipeline/src/main/java/site/kael/clash/pipeline/repository/JsonFileBuildPipelineRepository.java`

- [ ] **Step 1: 修改 BuildPipelineRepository 接口，增加查询方法**

```java
package site.kael.clash.pipeline.repository;

import site.kael.clash.pipeline.model.BuildPipeline;
import java.util.List;

public interface BuildPipelineRepository {
    // 现有方法保持不变
    BuildPipeline save(BuildPipeline pipeline);
    BuildPipeline findById(String id);
    List<BuildPipeline> findAll();
    void deleteById(String id);
    
    // 新增查询方法
    List<BuildPipeline> findByConfigType(String configType);
    List<BuildPipeline> findByConfigProfileId(String configProfileId);
}
```

- [ ] **Step 2: 修改 JsonFileBuildPipelineRepository 实现，增加查询方法**

```java
@Override
public List<BuildPipeline> findByConfigType(String configType) {
    return findAll().stream()
            .filter(pipeline -> configType.equals(pipeline.getConfigType()))
            .collect(Collectors.toList());
}

@Override
public List<BuildPipeline> findByConfigProfileId(String configProfileId) {
    return findAll().stream()
            .filter(pipeline -> configProfileId.equals(pipeline.getConfigProfileId()))
            .collect(Collectors.toList());
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl module-pipeline`
Expected: 编译成功，无错误

- [ ] **Step 4: Commit**

```bash
git add module-pipeline/src/main/java/site/kael/clash/pipeline/repository/
git commit -m "feat(repository): 为 BuildPipelineRepository 增加配置类型查询方法"
```

---

## Task 4: 修改 BuildPipelineServiceImpl

**Files:**
- Modify: `module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImpl.java`

- [ ] **Step 1: 增加 ConfigProfileRepository 依赖**

```java
private final ConfigProfileRepository configProfileRepository;
private final ConfigGeneratorService configGeneratorService;

public BuildPipelineServiceImpl(
        BuildPipelineRepository pipelineRepository,
        BuildRecordRepository recordRepository,
        SubscriptionService subscriptionService,
        PipelineService pipelineService,
        MihomoService mihomoService,
        SchedulerService schedulerService,
        ConfigProfileRepository configProfileRepository,
        ConfigGeneratorService configGeneratorService) {
    this.pipelineRepository = pipelineRepository;
    this.recordRepository = recordRepository;
    this.subscriptionService = subscriptionService;
    this.pipelineService = pipelineService;
    this.mihomoService = mihomoService;
    this.schedulerService = schedulerService;
    this.configProfileRepository = configProfileRepository;
    this.configGeneratorService = configGeneratorService;
}
```

- [ ] **Step 2: 修改 execute() 方法，支持配置类型选择**

```java
@Override
public BuildRecord execute(String pipelineId) {
    BuildPipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new BusinessException(404, "构建流程不存在: " + pipelineId));

    // 向后兼容：自动迁移 configType 为 null 的记录
    if (pipeline.getConfigType() == null) {
        pipeline.setConfigType("subscription");
        pipelineRepository.save(pipeline);
        log.info("自动迁移构建流程配置类型: {} ({})", pipeline.getName(), pipeline.getId());
    }

    BuildRecord record = new BuildRecord();
    record.setId(IdGenerator.generate());
    record.setBuildPipelineId(pipelineId);
    record.setStartedAt(LocalDateTime.now());
    record.setStatus("RUNNING");

    log.info("开始执行构建流程: {} ({})", pipeline.getName(), pipelineId);
    List<BuildStep> steps = record.getSteps();

    try {
        ClashConfig config;
        
        // 根据配置类型选择配置来源
        String configType = pipeline.getConfigType();
        if ("subscription".equals(configType)) {
            // 订阅源模式：拉取主订阅配置 + 合并额外订阅
            config = executeSubscriptionMode(pipeline, record, steps);
        } else if ("config-profile".equals(configType)) {
            // 配置组合模式：直接生成完整配置
            config = executeConfigProfileMode(pipeline, record, steps);
        } else {
            throw new BusinessException("不支持的配置类型: " + configType);
        }

        // 脚本处理（两种模式都支持）
        if (pipeline.getScriptName() != null && !pipeline.getScriptName().isBlank()) {
            config = executeScriptProcessing(pipeline, config, record, steps);
        }

        // 推送到目标 mihomo 实例
        executePushToMihomo(pipeline, config, record, steps);

        // 记录成功
        record.setStatus("SUCCESS");
        record.setFinishedAt(LocalDateTime.now());
        pipeline.setLastRunAt(record.getFinishedAt());
        pipeline.setLastRunStatus("SUCCESS");
        pipelineRepository.save(pipeline);

        log.info("构建流程执行成功: {} ({})", pipeline.getName(), pipelineId);

    } catch (Exception e) {
        record.setStatus("FAILED");
        record.setErrorMessage(e.getMessage());
        record.setFinishedAt(LocalDateTime.now());
        record.getLogs().add("ERROR: " + e.getMessage());

        pipeline.setLastRunAt(record.getFinishedAt());
        pipeline.setLastRunStatus("FAILED");
        pipelineRepository.save(pipeline);

        log.error("构建流程执行失败: {} ({}), 原因: {}", pipeline.getName(), pipelineId, e.getMessage(), e);
    }

    return recordRepository.save(record);
}
```

- [ ] **Step 3: 实现 executeSubscriptionMode() 方法**

```java
/**
 * 订阅源模式：拉取主订阅配置 + 合并额外订阅
 */
private ClashConfig executeSubscriptionMode(BuildPipeline pipeline, BuildRecord record, List<BuildStep> steps) {
    // 步骤 1：拉取主订阅配置
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

    // 步骤 2：合并额外订阅节点
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

    return config;
}
```

- [ ] **Step 4: 实现 executeConfigProfileMode() 方法**

```java
/**
 * 配置组合模式：直接生成完整配置
 */
private ClashConfig executeConfigProfileMode(BuildPipeline pipeline, BuildRecord record, List<BuildStep> steps) {
    // 步骤 1：获取配置组合
    String configProfileName = configProfileRepository.findById(pipeline.getConfigProfileId())
            .map(site.kael.clash.processor.model.ConfigProfile::getName)
            .orElse(pipeline.getConfigProfileId());
    BuildStep step1 = startStep("获取配置组合", Map.of("configProfileName", configProfileName));
    
    // 获取配置组合
    ConfigProfile profile = configProfileRepository.findById(pipeline.getConfigProfileId())
            .orElseThrow(() -> new BusinessException(404, "配置组合不存在: " + pipeline.getConfigProfileId()));
    
    // 生成完整配置（每次构建都重新生成，不使用缓存）
    String yaml = configGeneratorService.generate(profile);
    ClashConfig config = yamlToClashConfig(yaml, profile.getName());
    
    Map<String, Object> step1Output = new LinkedHashMap<>();
    step1Output.put("configSummary", buildConfigSummary(config));
    step1Output.put("configYaml", yaml);
    finishStep(step1, "SUCCESS", step1Output);
    steps.add(step1);
    record.getLogs().add("配置组合生成完成: " + profile.getName());
    record.getLogs().add("节点总数: " + (config.getProxies() != null ? config.getProxies().size() : 0));

    return config;
}
```

- [ ] **Step 5: 实现辅助方法**

```java
/**
 * 将 YAML 字符串转换为 ClashConfig 对象
 */
private ClashConfig yamlToClashConfig(String yaml, String name) {
    Map<String, Object> raw = YamlUtil.load(yaml);
    ClashConfig config = new ClashConfig(name);
    config.setRaw(raw);
    
    // 解析 proxies
    if (raw.containsKey("proxies")) {
        List<Map<String, Object>> proxyList = (List<Map<String, Object>>) raw.get("proxies");
        List<ProxyNode> proxies = proxyList.stream()
                .map(this::mapToProxyNode)
                .collect(Collectors.toList());
        config.setProxies(proxies);
    }
    
    return config;
}

/**
 * 将 Map 转换为 ProxyNode 对象
 */
private ProxyNode mapToProxyNode(Map<String, Object> map) {
    ProxyNode node = new ProxyNode();
    node.setName((String) map.get("name"));
    node.setType((String) map.get("type"));
    node.setServer((String) map.get("server"));
    node.setPort((Integer) map.get("port"));
    
    // 将其他字段放入 extra
    Map<String, Object> extra = new LinkedHashMap<>(map);
    extra.remove("name");
    extra.remove("type");
    extra.remove("server");
    extra.remove("port");
    node.setExtra(extra);
    
    return node;
}
```

- [ ] **Step 6: 修改 create() 和 update() 方法，支持配置类型验证**

```java
@Override
public BuildPipeline create(BuildPipeline pipeline) {
    // 验证配置类型
    pipeline.validate();
    
    // 设置默认值
    if (pipeline.getConfigType() == null) {
        pipeline.setConfigType("subscription");
    }
    
    // 生成 ID 和时间戳
    pipeline.setId(IdGenerator.generate());
    LocalDateTime now = LocalDateTime.now();
    pipeline.setCreatedAt(now);
    pipeline.setUpdatedAt(now);
    
    // 保存并同步定时任务
    BuildPipeline saved = pipelineRepository.save(pipeline);
    syncCron(saved);
    log.info("创建构建流程: {} ({})", saved.getName(), saved.getId());
    return saved;
}

@Override
public BuildPipeline update(BuildPipeline pipeline) {
    // 验证配置类型
    pipeline.validate();
    
    // 检查是否存在
    if (pipeline.getId() == null || pipeline.getId().isBlank()) {
        throw new BusinessException("构建流程 ID 不能为空");
    }
    pipelineRepository.findById(pipeline.getId())
            .orElseThrow(() -> new BusinessException("构建流程不存在: " + pipeline.getId()));
    
    // 更新时间戳
    pipeline.setUpdatedAt(LocalDateTime.now());
    
    // 保存并同步定时任务
    BuildPipeline saved = pipelineRepository.save(pipeline);
    syncCron(saved);
    log.info("更新构建流程: {} ({})", saved.getName(), saved.getId());
    return saved;
}
```

- [ ] **Step 7: 验证编译**

Run: `mvn compile -pl module-pipeline`
Expected: 编译成功，无错误

- [ ] **Step 8: Commit**

```bash
git add module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImpl.java
git commit -m "feat(service): 修改 BuildPipelineServiceImpl 支持配置类型选择"
```

---

## Task 5: 修改 BuildPipelineController

**Files:**
- Modify: `module-web/src/main/java/site/kael/clash/web/controller/BuildPipelineController.java`

- [ ] **Step 1: 修改控制器，支持新增字段**

```java
package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.pipeline.model.BuildPipeline;
import site.kael.clash.pipeline.model.BuildRecord;
import site.kael.clash.pipeline.service.BuildPipelineService;

import java.util.List;

@RestController
@RequestMapping("/api/build-pipelines")
public class BuildPipelineController {

    private static final Logger log = LoggerFactory.getLogger(BuildPipelineController.class);

    private final BuildPipelineService buildPipelineService;

    public BuildPipelineController(BuildPipelineService buildPipelineService) {
        this.buildPipelineService = buildPipelineService;
    }

    @GetMapping
    public ResponseEntity<List<BuildPipeline>> findAll() {
        return ResponseEntity.ok(buildPipelineService.findAll());
    }

    @PostMapping
    public ResponseEntity<BuildPipeline> create(@RequestBody BuildPipeline pipeline) {
        log.info("创建构建流程: name={}, configType={}", pipeline.getName(), pipeline.getConfigType());
        return ResponseEntity.ok(buildPipelineService.create(pipeline));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildPipeline> findById(@PathVariable String id) {
        return ResponseEntity.ok(buildPipelineService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BuildPipeline> update(@PathVariable String id, @RequestBody BuildPipeline pipeline) {
        log.info("更新构建流程: id={}, configType={}", id, pipeline.getConfigType());
        pipeline.setId(id);
        return ResponseEntity.ok(buildPipelineService.update(pipeline));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        log.info("删除构建流程: id={}", id);
        buildPipelineService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<BuildRecord> execute(@PathVariable String id) {
        log.info("手动触发构建流程: id={}", id);
        return ResponseEntity.ok(buildPipelineService.execute(id));
    }

    @GetMapping("/{id}/records")
    public ResponseEntity<List<BuildRecord>> findRecords(@PathVariable String id) {
        return ResponseEntity.ok(buildPipelineService.findRecords(id));
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl module-web`
Expected: 编译成功，无错误

- [ ] **Step 3: Commit**

```bash
git add module-web/src/main/java/site/kael/clash/web/controller/BuildPipelineController.java
git commit -m "feat(controller): 修改 BuildPipelineController 支持配置类型字段"
```

---

## Task 6: 编写单元测试

**Files:**
- Create: `module-pipeline/src/test/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImplTest.java`

- [ ] **Step 1: 创建测试目录和测试类**

```java
package site.kael.clash.pipeline.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.pipeline.model.BuildPipeline;
import site.kael.clash.pipeline.model.BuildRecord;
import site.kael.clash.pipeline.repository.BuildPipelineRepository;
import site.kael.clash.pipeline.repository.BuildRecordRepository;
import site.kael.clash.processor.model.ConfigProfile;
import site.kael.clash.processor.repository.ConfigProfileRepository;
import site.kael.clash.processor.service.ConfigGeneratorService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildPipelineServiceImplTest {

    @Mock
    private BuildPipelineRepository pipelineRepository;
    
    @Mock
    private BuildRecordRepository recordRepository;
    
    @Mock
    private ConfigProfileRepository configProfileRepository;
    
    @Mock
    private ConfigGeneratorService configGeneratorService;
    
    @InjectMocks
    private BuildPipelineServiceImpl buildPipelineService;

    @Test
    void testExecuteWithConfigProfile() {
        // 准备测试数据
        ConfigProfile profile = new ConfigProfile();
        profile.setId("profile-1");
        profile.setName("测试配置组合");
        
        BuildPipeline pipeline = new BuildPipeline();
        pipeline.setId("pipeline-1");
        pipeline.setName("测试配置组合模式");
        pipeline.setConfigType("config-profile");
        pipeline.setConfigProfileId("profile-1");
        pipeline.setTargetInstanceId("instance-1");
        
        BuildRecord record = new BuildRecord();
        record.setId("record-1");
        
        // 模拟依赖
        when(pipelineRepository.findById("pipeline-1")).thenReturn(Optional.of(pipeline));
        when(configProfileRepository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(configGeneratorService.generate(profile)).thenReturn("proxies:\n  - name: test");
        when(recordRepository.save(any(BuildRecord.class))).thenReturn(record);
        
        // 执行测试
        BuildRecord result = buildPipelineService.execute("pipeline-1");
        
        // 验证结果
        assertNotNull(result);
        verify(configGeneratorService).generate(profile);
    }

    @Test
    void testExecuteWithNullConfigType() {
        // 准备测试数据
        BuildPipeline pipeline = new BuildPipeline();
        pipeline.setId("pipeline-1");
        pipeline.setName("测试向后兼容");
        pipeline.setConfigType(null); // 模拟现有数据
        pipeline.setPrimarySubscriptionId("sub-1");
        pipeline.setTargetInstanceId("instance-1");
        
        BuildRecord record = new BuildRecord();
        record.setId("record-1");
        
        // 模拟依赖
        when(pipelineRepository.findById("pipeline-1")).thenReturn(Optional.of(pipeline));
        when(recordRepository.save(any(BuildRecord.class))).thenReturn(record);
        
        // 执行测试
        BuildRecord result = buildPipelineService.execute("pipeline-1");
        
        // 验证结果
        assertNotNull(result);
        assertEquals("subscription", pipeline.getConfigType());
        verify(pipelineRepository).save(pipeline);
    }

    @Test
    void testValidationWithConfigProfile() {
        // 测试配置组合模式验证
        BuildPipeline pipeline = new BuildPipeline();
        pipeline.setName("测试验证");
        pipeline.setConfigType("config-profile");
        pipeline.setConfigProfileId(null); // 缺少配置组合 ID
        
        assertThrows(IllegalArgumentException.class, () -> {
            pipeline.validate();
        });
    }

    @Test
    void testValidationWithSubscription() {
        // 测试订阅源模式验证
        BuildPipeline pipeline = new BuildPipeline();
        pipeline.setName("测试验证");
        pipeline.setConfigType("subscription");
        pipeline.setPrimarySubscriptionId(null); // 缺少主订阅
        
        assertThrows(IllegalArgumentException.class, () -> {
            pipeline.validate();
        });
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `mvn test -pl module-pipeline -Dtest=BuildPipelineServiceImplTest`
Expected: 所有测试通过

- [ ] **Step 3: Commit**

```bash
git add module-pipeline/src/test/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImplTest.java
git commit -m "test: 添加 BuildPipelineServiceImpl 单元测试"
```

---

## Task 7: 编写集成测试

**Files:**
- Create: `module-web/src/test/java/site/kael/clash/web/controller/BuildPipelineControllerIntegrationTest.java`

- [ ] **Step 1: 创建集成测试类**

```java
package site.kael.clash.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import site.kael.clash.pipeline.model.BuildPipeline;
import site.kael.clash.pipeline.model.BuildRecord;
import site.kael.clash.pipeline.repository.BuildPipelineRepository;
import site.kael.clash.processor.model.ConfigProfile;
import site.kael.clash.processor.repository.ConfigProfileRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BuildPipelineControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private BuildPipelineRepository pipelineRepository;
    
    @Autowired
    private ConfigProfileRepository configProfileRepository;

    @Test
    void testCreateConfigProfilePipeline() {
        // 准备测试数据
        ConfigProfile profile = new ConfigProfile();
        profile.setId("test-profile");
        profile.setName("测试配置组合");
        profile.setSubscriptionIds(List.of("sub-1", "sub-2"));
        configProfileRepository.save(profile);
        
        // 创建构建流程
        BuildPipeline pipeline = new BuildPipeline();
        pipeline.setName("集成测试-配置组合模式");
        pipeline.setConfigType("config-profile");
        pipeline.setConfigProfileId(profile.getId());
        pipeline.setTargetInstanceId("test-instance");
        pipeline.setEnabled(true);
        
        // 发送请求
        ResponseEntity<BuildPipeline> response = restTemplate.postForEntity(
                "/api/build-pipelines", pipeline, BuildPipeline.class);
        
        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("config-profile", response.getBody().getConfigType());
        assertEquals(profile.getId(), response.getBody().getConfigProfileId());
    }

    @Test
    void testCreateSubscriptionPipeline() {
        // 创建构建流程
        BuildPipeline pipeline = new BuildPipeline();
        pipeline.setName("集成测试-订阅源模式");
        pipeline.setConfigType("subscription");
        pipeline.setPrimarySubscriptionId("test-subscription");
        pipeline.setTargetInstanceId("test-instance");
        pipeline.setEnabled(true);
        
        // 发送请求
        ResponseEntity<BuildPipeline> response = restTemplate.postForEntity(
                "/api/build-pipelines", pipeline, BuildPipeline.class);
        
        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("subscription", response.getBody().getConfigType());
        assertEquals("test-subscription", response.getBody().getPrimarySubscriptionId());
    }
}
```

- [ ] **Step 2: 运行集成测试**

Run: `mvn test -pl module-web -Dtest=BuildPipelineControllerIntegrationTest`
Expected: 所有测试通过

- [ ] **Step 3: Commit**

```bash
git add module-web/src/test/java/site/kael/clash/web/controller/BuildPipelineControllerIntegrationTest.java
git commit -m "test: 添加 BuildPipelineController 集成测试"
```

---

## Task 8: 准备部署文档

**Files:**
- Create: `docs/deployment/add-config-type-for-build.md`

- [ ] **Step 1: 创建部署文档**

```markdown
# 构建流程配置类型部署文档

## 部署步骤

### 1. 数据库迁移
```bash
# 执行迁移脚本
psql -h localhost -U postgres -d clash_subscriber -f module-pipeline/src/main/resources/db/migration/V20240101_001__add_config_type_to_build_pipeline.sql

# 验证迁移结果
psql -h localhost -U postgres -d clash_subscriber -c "SELECT config_type, COUNT(*) FROM build_pipeline GROUP BY config_type;"
```

### 2. 代码部署
```bash
# 编译打包
mvn clean package -DskipTests

# 部署新版本
java -jar target/clash-subscriber.jar
```

### 3. 验证
```bash
# 验证服务启动
curl http://localhost:8080/api/build-pipelines

# 测试订阅源模式
curl -X POST http://localhost:8080/api/build-pipelines \
  -H "Content-Type: application/json" \
  -d '{"name":"测试订阅源","configType":"subscription","primarySubscriptionId":"sub-1","targetInstanceId":"instance-1"}'

# 测试配置组合模式
curl -X POST http://localhost:8080/api/build-pipelines \
  -H "Content-Type: application/json" \
  -d '{"name":"测试配置组合","configType":"config-profile","configProfileId":"profile-1","targetInstanceId":"instance-1"}'
```

## 回滚方案

### 1. 回滚数据库
```bash
psql -h localhost -U postgres -d clash_subscriber -f module-pipeline/src/main/resources/db/migration/V20240101_001__add_config_type_to_build_pipeline_rollback.sql
```

### 2. 回滚代码
```bash
# 部署旧版本
java -jar target/clash-subscriber-old.jar
```

## 监控告警

- 监控数据库迁移状态
- 监控构建流程执行成功率
- 监控配置组合模式执行异常
```

- [ ] **Step 2: Commit**

```bash
git add docs/deployment/add-config-type-for-build.md
git commit -m "docs: 添加构建流程配置类型部署文档"
```

---

## 完成

所有任务已完成！现在可以运行 `/opsx:apply` 或让我开始实施这些任务。

**执行选项：**

1. **Subagent-Driven（推荐）** - 我为每个任务分派新的子任务，任务之间进行审查，快速迭代

2. **Inline Execution** - 在当前会话中执行任务，批量执行并设置检查点

你选择哪种方式？
