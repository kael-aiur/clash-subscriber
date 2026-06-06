# 构建流程配置类型设计文档

## 概述

本设计文档描述了为构建流程增加配置类型的功能，允许用户选择"订阅源模式"或"配置组合模式"作为配置来源，并确保每次构建都重新生成配置，不使用缓存。

## 背景

当前构建流程（BuildPipeline）只支持订阅源模式，需要选择主订阅和额外订阅。用户希望支持直接选择配置管理对象（ConfigProfile），简化配置流程，并确保每次构建都使用最新的配置。

## 目标

1. 为构建流程增加配置类型选择（订阅源模式/配置组合模式）
2. 配置组合模式下，直接选择 ConfigProfile 对象
3. 每次构建都重新生成配置，不使用缓存
4. 保持向后兼容性，现有构建流程自动迁移为订阅源模式
5. 配置组合模式下仍然支持脚本处理

## 设计方案

### 1. 数据模型设计

#### BuildPipeline 模型扩展

```java
public class BuildPipeline {
    // 现有字段保持不变
    private String id;
    private String name;
    private String primarySubscriptionId;      // 订阅源模式使用
    private List<String> additionalSubscriptionIds;  // 订阅源模式使用
    private String scriptName;
    private String targetInstanceId;
    private String cronExpression;
    private boolean enabled = true;
    
    // 新增字段
    private String configType;  // "subscription" 或 "config-profile"
    private String configProfileId;  // 配置组合模式使用
    
    // 时间戳字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRunAt;
    private String lastRunStatus;
}
```

#### ConfigType 枚举

```java
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
}
```

#### 向后兼容性处理

- 现有 BuildPipeline 的 `configType` 字段为 null 时，视为 `"subscription"`
- 执行逻辑中检查 `configType` 是否为 null，如果是则使用订阅源模式

### 2. 执行逻辑设计

#### BuildPipelineServiceImpl.execute() 方法修改

```java
@Override
public BuildRecord execute(String pipelineId) {
    BuildPipeline pipeline = pipelineRepository.findById(pipelineId)
            .orElseThrow(() -> new BusinessException(404, "构建流程不存在: " + pipelineId));

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
        if (configType == null || "subscription".equals(configType)) {
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

#### 订阅源模式执行逻辑

```java
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

#### 配置组合模式执行逻辑

```java
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

#### 执行流程对比

| 步骤 | 订阅源模式 | 配置组合模式 |
|------|-----------|-------------|
| 1 | 拉取主订阅配置 | 获取配置组合并生成完整配置 |
| 2 | 合并额外订阅节点 | （跳过） |
| 3 | 脚本处理（可选） | 脚本处理（可选） |
| 4 | 推送到 Mihomo | 推送到 Mihomo |

### 3. API 和前端设计

#### BuildPipeline 模型变更

```java
// 新增字段
private String configType;        // "subscription" 或 "config-profile"
private String configProfileId;   // 配置组合模式使用

// 验证逻辑
public void validate() {
    if (configType == null || "subscription".equals(configType)) {
        // 订阅源模式验证
        if (primarySubscriptionId == null || primarySubscriptionId.isBlank()) {
            throw new BusinessException("主订阅不能为空");
        }
    } else if ("config-profile".equals(configType)) {
        // 配置组合模式验证
        if (configProfileId == null || configProfileId.isBlank()) {
            throw new BusinessException("配置组合不能为空");
        }
    } else {
        throw new BusinessException("不支持的配置类型: " + configType);
    }
}
```

#### API 响应示例

```json
{
  "id": "pipeline-001",
  "name": "我的构建流程",
  "configType": "config-profile",
  "configProfileId": "profile-001",
  "primarySubscriptionId": null,
  "additionalSubscriptionIds": [],
  "scriptName": "optimize.js",
  "targetInstanceId": "mihomo-001",
  "cronExpression": "0 0 * * *",
  "enabled": true,
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00",
  "lastRunAt": null,
  "lastRunStatus": null
}
```

#### 前端表单设计

```
┌─────────────────────────────────────────────────┐
│ 构建流程配置                                      │
├─────────────────────────────────────────────────┤
│ 名称: [我的构建流程________________]              │
│                                                 │
│ 配置类型: ○ 订阅源模式  ● 配置组合模式           │
│                                                 │
│ ┌─ 订阅源模式 ─────────────────────────────┐    │
│ │ 主订阅: [选择订阅源________________]      │    │
│ │ 额外订阅: [+ 添加订阅源]                 │    │
│ └──────────────────────────────────────────┘    │
│                                                 │
│ ┌─ 配置组合模式 ───────────────────────────┐    │
│ │ 配置组合: [选择配置组合________________]  │    │
│ └──────────────────────────────────────────┘    │
│                                                 │
│ 脚本处理: [选择脚本________________] (可选)      │
│ 目标实例: [选择 Mihomo 实例________]             │
│ 定时表达式: [0 0 * * *____________]              │
│                                                 │
│ [取消]  [保存]                                   │
└─────────────────────────────────────────────────┘
```

#### 前端交互逻辑

1. 页面加载时，根据 `configType` 显示对应的表单区域
2. 切换配置类型时，隐藏/显示对应的表单区域
3. 保存时，根据配置类型验证必填字段
4. 编辑现有 BuildPipeline 时，自动回显配置类型和对应字段

### 4. 数据库和持久化设计

#### 数据库表结构变更

```sql
-- 新增字段
ALTER TABLE build_pipeline ADD COLUMN config_type VARCHAR(20) DEFAULT 'subscription';
ALTER TABLE build_pipeline ADD COLUMN config_profile_id VARCHAR(64);

-- 添加索引
CREATE INDEX idx_build_pipeline_config_type ON build_pipeline(config_type);
CREATE INDEX idx_build_pipeline_config_profile_id ON build_pipeline(config_profile_id);

-- 添加外键约束（可选）
ALTER TABLE build_pipeline ADD CONSTRAINT fk_build_pipeline_config_profile 
    FOREIGN KEY (config_profile_id) REFERENCES config_profile(id) ON DELETE SET NULL;
```

#### 数据迁移脚本

```sql
-- 将现有数据迁移为订阅源模式
UPDATE build_pipeline SET config_type = 'subscription' WHERE config_type IS NULL;

-- 验证迁移结果
SELECT config_type, COUNT(*) FROM build_pipeline GROUP BY config_type;
```

#### Repository 层修改

```java
@Repository
public interface BuildPipelineRepository extends JpaRepository<BuildPipeline, String> {
    
    // 按配置类型查询
    List<BuildPipeline> findByConfigType(String configType);
    
    // 按配置组合 ID 查询
    List<BuildPipeline> findByConfigProfileId(String configProfileId);
    
    // 查询所有启用的配置组合模式的构建流程
    @Query("SELECT bp FROM BuildPipeline bp WHERE bp.configType = 'config-profile' AND bp.enabled = true")
    List<BuildPipeline> findAllEnabledConfigProfilePipelines();
    
    // 查询所有启用的订阅源模式的构建流程
    @Query("SELECT bp FROM BuildPipeline bp WHERE (bp.configType = 'subscription' OR bp.configType IS NULL) AND bp.enabled = true")
    List<BuildPipeline> findAllEnabledSubscriptionPipelines();
}
```

#### 向后兼容性处理

```java
@Override
public BuildPipeline findById(String id) {
    BuildPipeline pipeline = pipelineRepository.findById(id)
            .orElseThrow(() -> new BusinessException(404, "构建流程不存在: " + id));
    
    // 向后兼容：如果 configType 为 null，设置为 "subscription"
    if (pipeline.getConfigType() == null) {
        pipeline.setConfigType("subscription");
        pipelineRepository.save(pipeline);
        log.info("自动迁移构建流程配置类型: {} ({})", pipeline.getName(), pipeline.getId());
    }
    
    return pipeline;
}
```

### 5. 测试设计

#### 单元测试

```java
@SpringBootTest
class BuildPipelineServiceImplTest {
    
    @Test
    void testExecuteWithConfigProfile() {
        // 准备测试数据
        ConfigProfile profile = createTestConfigProfile();
        configProfileRepository.save(profile);
        
        BuildPipeline pipeline = new BuildPipeline();
        pipeline.setName("测试配置组合模式");
        pipeline.setConfigType("config-profile");
        pipeline.setConfigProfileId(profile.getId());
        pipeline.setTargetInstanceId("test-instance");
        pipeline.setEnabled(true);
        pipelineRepository.save(pipeline);
        
        // 执行构建流程
        BuildRecord record = buildPipelineService.execute(pipeline.getId());
        
        // 验证结果
        assertEquals("SUCCESS", record.getStatus());
        assertNotNull(record.getSteps());
        assertTrue(record.getSteps().size() >= 2); // 至少包含配置组合生成和推送步骤
        
        // 验证配置组合生成步骤
        BuildStep configStep = record.getSteps().get(0);
        assertEquals("获取配置组合", configStep.getName());
        assertEquals("SUCCESS", configStep.getStatus());
    }
    
    @Test
    void testExecuteWithNullConfigType() {
        // 准备测试数据（模拟现有数据，configType 为 null）
        BuildPipeline pipeline = new BuildPipeline();
        pipeline.setName("测试向后兼容");
        pipeline.setConfigType(null); // 模拟现有数据
        pipeline.setPrimarySubscriptionId("test-subscription");
        pipeline.setTargetInstanceId("test-instance");
        pipeline.setEnabled(true);
        pipelineRepository.save(pipeline);
        
        // 执行构建流程
        BuildRecord record = buildPipelineService.execute(pipeline.getId());
        
        // 验证结果（应该自动迁移为订阅源模式）
        assertEquals("SUCCESS", record.getStatus());
        
        // 验证配置类型已自动迁移
        BuildPipeline updatedPipeline = pipelineRepository.findById(pipeline.getId()).orElseThrow();
        assertEquals("subscription", updatedPipeline.getConfigType());
    }
}
```

#### 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BuildPipelineControllerIntegrationTest {
    
    @Test
    void testCreateConfigProfilePipeline() {
        // 准备测试数据
        ConfigProfile profile = createTestConfigProfile();
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
}
```

#### 测试覆盖场景

| 测试场景 | 测试类型 | 验证点 |
|---------|---------|--------|
| 配置组合模式执行 | 单元测试 | 正确调用 ConfigGeneratorService.generate() |
| 订阅源模式执行 | 单元测试 | 正确执行拉取主订阅和合并额外订阅 |
| configType 为 null | 单元测试 | 自动迁移为订阅源模式 |
| 配置组合模式验证 | 单元测试 | 配置组合 ID 不能为空 |
| 订阅源模式验证 | 单元测试 | 主订阅不能为空 |
| 创建配置组合模式 API | 集成测试 | 正确创建和返回 |
| 创建订阅源模式 API | 集成测试 | 正确创建和返回 |
| 执行配置组合模式 API | 集成测试 | 正确执行和返回结果 |

### 6. 部署和迁移设计

#### 部署步骤

```
┌─────────────────────────────────────────────────────────────┐
│ 部署流程                                                    │
├─────────────────────────────────────────────────────────────┤
│ 1. 数据库迁移                                               │
│    ├─ 执行 SQL 迁移脚本                                      │
│    ├─ 新增 config_type 字段（默认值 'subscription'）          │
│    ├─ 新增 config_profile_id 字段                            │
│    └─ 创建索引                                              │
│                                                             │
│ 2. 代码部署                                                 │
│    ├─ 部署新版本代码                                         │
│    ├─ 验证服务启动正常                                       │
│    └─ 验证 API 接口正常                                      │
│                                                             │
│ 3. 数据验证                                                 │
│    ├─ 检查现有 BuildPipeline 数据                            │
│    ├─ 验证 config_type 字段已正确设置                        │
│    └─ 验证向后兼容性逻辑正常工作                              │
│                                                             │
│ 4. 功能验证                                                 │
│    ├─ 测试订阅源模式构建流程                                  │
│    ├─ 测试配置组合模式构建流程                                │
│    └─ 验证定时任务正常执行                                    │
└─────────────────────────────────────────────────────────────┘
```

#### 回滚方案

```sql
-- 回滚脚本：V20240101_001__add_config_type_to_build_pipeline_rollback.sql

-- 步骤 1：删除索引
DROP INDEX IF EXISTS idx_build_pipeline_config_type;
DROP INDEX IF EXISTS idx_build_pipeline_config_profile_id;

-- 步骤 2：删除字段
ALTER TABLE build_pipeline DROP COLUMN IF EXISTS config_type;
ALTER TABLE build_pipeline DROP COLUMN IF EXISTS config_profile_id;

-- 步骤 3：验证回滚
SELECT * FROM build_pipeline LIMIT 1;
-- 预期结果：表结构恢复到迁移前状态
```

#### 监控和告警

```java
@Component
public class BuildPipelineMigrationMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(BuildPipelineMigrationMonitor.class);
    
    @Autowired
    private BuildPipelineRepository pipelineRepository;
    
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void monitorMigrationStatus() {
        List<BuildPipeline> pipelines = pipelineRepository.findAll();
        
        long totalCount = pipelines.size();
        long migratedCount = pipelines.stream()
                .filter(p -> p.getConfigType() != null)
                .count();
        long nullCount = totalCount - migratedCount;
        
        if (nullCount > 0) {
            log.warn("发现未迁移的 BuildPipeline 记录: {}/{}", nullCount, totalCount);
            // 可以发送告警通知
        }
        
        log.debug("迁移状态监控: 已迁移 {}/{}", migratedCount, totalCount);
    }
}
```

#### 部署检查清单

```
□ 数据库迁移脚本执行成功
□ 新增字段已创建
□ 索引已创建
□ 现有数据已迁移（config_type 不为 null）
□ 代码部署成功
□ 服务启动正常
□ API 接口响应正常
□ 订阅源模式构建流程测试通过
□ 配置组合模式构建流程测试通过
□ 定时任务正常执行
□ 监控和告警配置完成
□ 回滚脚本准备就绪
□ 部署文档已更新
```

#### 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 数据库迁移失败 | 高 | 低 | 准备回滚脚本，备份数据库 |
| 向后兼容性问题 | 中 | 低 | 自动迁移逻辑，充分测试 |
| 配置组合模式执行失败 | 中 | 中 | 详细日志记录，快速回滚 |
| 定时任务异常 | 中 | 低 | 监控告警，手动触发验证 |

## 总结

本设计通过扩展 BuildPipeline 模型，增加了配置类型选择功能，支持订阅源模式和配置组合模式。配置组合模式下，直接调用 ConfigGeneratorService.generate() 生成完整配置，每次构建都重新生成，不使用缓存。设计保持了向后兼容性，现有构建流程会自动迁移为订阅源模式。

该设计方案改动最小，风险最低，满足当前需求，未来需要时可以再重构为策略模式或组合模式。
