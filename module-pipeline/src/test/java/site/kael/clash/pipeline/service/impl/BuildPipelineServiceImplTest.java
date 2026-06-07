package site.kael.clash.pipeline.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.mihomo.model.MihomoInstance;
import site.kael.clash.mihomo.service.MihomoService;
import site.kael.clash.pipeline.model.BuildPipeline;
import site.kael.clash.pipeline.model.BuildRecord;
import site.kael.clash.pipeline.model.ConfigType;
import site.kael.clash.pipeline.repository.BuildPipelineRepository;
import site.kael.clash.pipeline.repository.BuildRecordRepository;
import site.kael.clash.processor.model.ConfigProfile;
import site.kael.clash.processor.repository.ConfigProfileRepository;
import site.kael.clash.processor.service.ConfigGeneratorService;
import site.kael.clash.processor.service.PipelineService;
import site.kael.clash.scheduler.service.SchedulerService;
import site.kael.clash.subscription.model.Subscription;
import site.kael.clash.subscription.service.SubscriptionService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BuildPipelineServiceImpl 单元测试
 * <p>
 * 测试配置类型选择功能，包括配置组合模式、订阅源模式、向后兼容性和验证逻辑。
 */
@ExtendWith(MockitoExtension.class)
class BuildPipelineServiceImplTest {

    @Mock
    private BuildPipelineRepository pipelineRepository;

    @Mock
    private BuildRecordRepository recordRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private PipelineService pipelineService;

    @Mock
    private MihomoService mihomoService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private ConfigProfileRepository configProfileRepository;

    @Mock
    private ConfigGeneratorService configGeneratorService;

    @InjectMocks
    private BuildPipelineServiceImpl buildPipelineService;

    private static final String PIPELINE_ID = "pipeline-001";
    private static final String TARGET_INSTANCE_ID = "instance-001";
    private static final String CONFIG_PROFILE_ID = "profile-001";
    private static final String PRIMARY_SUB_ID = "sub-001";

    @BeforeEach
    void setUp() {
        // 公共初始化逻辑
    }

    // ========== 配置组合模式执行测试 ==========

    @Nested
    @DisplayName("配置组合模式执行")
    class ConfigProfileExecutionTests {

        @Test
        @DisplayName("测试配置组合模式执行 - 成功")
        void testExecuteWithConfigProfile() {
            // 准备测试数据
            BuildPipeline pipeline = createConfigProfilePipeline();
            ConfigProfile profile = createConfigProfile();
            String yamlContent = createValidYamlContent();
            MihomoInstance instance = createMihomoInstance();

            // Mock 依赖
            when(pipelineRepository.findById(PIPELINE_ID)).thenReturn(Optional.of(pipeline));
            when(configProfileRepository.findById(CONFIG_PROFILE_ID)).thenReturn(Optional.of(profile));
            when(configGeneratorService.generate(profile)).thenReturn(yamlContent);
            when(mihomoService.findById(TARGET_INSTANCE_ID)).thenReturn(Optional.of(instance));
            when(recordRepository.save(any(BuildRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // 执行测试
            BuildRecord record = buildPipelineService.execute(PIPELINE_ID);

            // 验证结果
            assertNotNull(record);
            assertEquals("SUCCESS", record.getStatus());
            assertNull(record.getErrorMessage());
            assertEquals(PIPELINE_ID, record.getBuildPipelineId());

            // 验证调用
            verify(pipelineRepository).findById(PIPELINE_ID);
            verify(configProfileRepository, times(2)).findById(CONFIG_PROFILE_ID);
            verify(configGeneratorService).generate(profile);
            verify(mihomoService).pushConfig(eq(TARGET_INSTANCE_ID), any(ClashConfig.class));
            verify(recordRepository).save(any(BuildRecord.class));
            verify(pipelineRepository).save(pipeline);

            // 验证 pipeline 状态更新
            assertNotNull(pipeline.getLastRunAt());
            assertEquals("SUCCESS", pipeline.getLastRunStatus());
        }

        @Test
        @DisplayName("测试配置组合模式 - 配置组合不存在")
        void testExecuteWithConfigProfileNotFound() {
            // 准备测试数据
            BuildPipeline pipeline = createConfigProfilePipeline();

            // Mock 依赖
            when(pipelineRepository.findById(PIPELINE_ID)).thenReturn(Optional.of(pipeline));
            when(configProfileRepository.findById(CONFIG_PROFILE_ID)).thenReturn(Optional.empty());
            when(recordRepository.save(any(BuildRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // 执行测试
            BuildRecord record = buildPipelineService.execute(PIPELINE_ID);

            // 验证结果 - 应该记录失败状态
            assertNotNull(record);
            assertEquals("FAILED", record.getStatus());
            assertNotNull(record.getErrorMessage());
            assertTrue(record.getErrorMessage().contains("配置组合不存在"));
        }
    }

    // ========== 订阅源模式执行测试 ==========

    @Nested
    @DisplayName("订阅源模式执行")
    class SubscriptionExecutionTests {

        @Test
        @DisplayName("测试订阅源模式执行 - 成功")
        void testExecuteWithSubscription() {
            // 准备测试数据
            BuildPipeline pipeline = createSubscriptionPipeline();
            Subscription subscription = createSubscription();
            ClashConfig config = createClashConfig();
            MihomoInstance instance = createMihomoInstance();

            // Mock 依赖
            when(pipelineRepository.findById(PIPELINE_ID)).thenReturn(Optional.of(pipeline));
            when(subscriptionService.findById(PRIMARY_SUB_ID)).thenReturn(Optional.of(subscription));
            when(subscriptionService.fetch(PRIMARY_SUB_ID)).thenReturn(config);
            when(mihomoService.findById(TARGET_INSTANCE_ID)).thenReturn(Optional.of(instance));
            when(recordRepository.save(any(BuildRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // 执行测试
            BuildRecord record = buildPipelineService.execute(PIPELINE_ID);

            // 验证结果
            assertNotNull(record);
            assertEquals("SUCCESS", record.getStatus());
            assertNull(record.getErrorMessage());

            // 验证调用
            verify(pipelineRepository).findById(PIPELINE_ID);
            verify(subscriptionService).fetch(PRIMARY_SUB_ID);
            verify(mihomoService).pushConfig(eq(TARGET_INSTANCE_ID), any(ClashConfig.class));
            verify(recordRepository).save(any(BuildRecord.class));
        }

        @Test
        @DisplayName("测试订阅源模式 - fetch 返回 null")
        void testExecuteWithSubscriptionFetchReturnsNull() {
            // 准备测试数据
            BuildPipeline pipeline = createSubscriptionPipeline();
            Subscription subscription = createSubscription();
            MihomoInstance instance = createMihomoInstance();

            // Mock 依赖
            when(pipelineRepository.findById(PIPELINE_ID)).thenReturn(Optional.of(pipeline));
            when(subscriptionService.findById(PRIMARY_SUB_ID)).thenReturn(Optional.of(subscription));
            when(subscriptionService.fetch(PRIMARY_SUB_ID)).thenReturn(null);
            when(mihomoService.findById(TARGET_INSTANCE_ID)).thenReturn(Optional.of(instance));
            when(recordRepository.save(any(BuildRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // 执行测试
            BuildRecord record = buildPipelineService.execute(PIPELINE_ID);

            // 验证结果 - 应该成功，使用空配置
            assertNotNull(record);
            assertEquals("SUCCESS", record.getStatus());
        }
    }

    // ========== 向后兼容性测试 ==========

    @Nested
    @DisplayName("向后兼容性")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("测试 configType 为 null 时自动迁移为 subscription")
        void testExecuteWithNullConfigType() {
            // 准备测试数据 - configType 为 null
            BuildPipeline pipeline = createSubscriptionPipeline();
            pipeline.setConfigType(null);  // 模拟旧数据

            Subscription subscription = createSubscription();
            ClashConfig config = createClashConfig();
            MihomoInstance instance = createMihomoInstance();

            // Mock 依赖
            when(pipelineRepository.findById(PIPELINE_ID)).thenReturn(Optional.of(pipeline));
            when(subscriptionService.findById(PRIMARY_SUB_ID)).thenReturn(Optional.of(subscription));
            when(subscriptionService.fetch(PRIMARY_SUB_ID)).thenReturn(config);
            when(mihomoService.findById(TARGET_INSTANCE_ID)).thenReturn(Optional.of(instance));
            when(recordRepository.save(any(BuildRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // 执行测试
            BuildRecord record = buildPipelineService.execute(PIPELINE_ID);

            // 验证结果
            assertNotNull(record);
            assertEquals("SUCCESS", record.getStatus());

            // 验证 configType 被自动迁移
            assertEquals(ConfigType.SUBSCRIPTION.getValue(), pipeline.getConfigType());

            // 验证 pipeline 被保存（迁移后）
            verify(pipelineRepository, atLeastOnce()).save(pipeline);
        }

        @Test
        @DisplayName("测试 configType 为空字符串时自动迁移为 subscription")
        void testExecuteWithBlankConfigType() {
            // 准备测试数据 - configType 为空字符串
            BuildPipeline pipeline = createSubscriptionPipeline();
            pipeline.setConfigType("  ");  // 模拟旧数据

            Subscription subscription = createSubscription();
            ClashConfig config = createClashConfig();
            MihomoInstance instance = createMihomoInstance();

            // Mock 依赖
            when(pipelineRepository.findById(PIPELINE_ID)).thenReturn(Optional.of(pipeline));
            when(subscriptionService.findById(PRIMARY_SUB_ID)).thenReturn(Optional.of(subscription));
            when(subscriptionService.fetch(PRIMARY_SUB_ID)).thenReturn(config);
            when(mihomoService.findById(TARGET_INSTANCE_ID)).thenReturn(Optional.of(instance));
            when(recordRepository.save(any(BuildRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // 执行测试
            BuildRecord record = buildPipelineService.execute(PIPELINE_ID);

            // 验证结果
            assertNotNull(record);
            assertEquals("SUCCESS", record.getStatus());

            // 验证 configType 被自动迁移
            assertEquals(ConfigType.SUBSCRIPTION.getValue(), pipeline.getConfigType());
        }
    }

    // ========== 验证逻辑测试 ==========

    @Nested
    @DisplayName("配置类型验证")
    class ValidationTests {

        @Test
        @DisplayName("测试配置组合模式验证 - configProfileId 为空时抛出异常")
        void testValidationWithConfigProfile() {
            // 准备测试数据 - configProfileId 为空
            BuildPipeline pipeline = new BuildPipeline();
            pipeline.setName("测试流程");
            pipeline.setConfigType(ConfigType.CONFIG_PROFILE.getValue());
            pipeline.setConfigProfileId(null);  // 缺少必要字段
            pipeline.setTargetInstanceId(TARGET_INSTANCE_ID);

            // 执行测试并验证异常
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pipeline.validate()
            );

            assertTrue(exception.getMessage().contains("配置组合ID不能为空"));
        }

        @Test
        @DisplayName("测试配置组合模式验证 - configProfileId 为空字符串时抛出异常")
        void testValidationWithConfigProfileBlankId() {
            // 准备测试数据 - configProfileId 为空字符串
            BuildPipeline pipeline = new BuildPipeline();
            pipeline.setName("测试流程");
            pipeline.setConfigType(ConfigType.CONFIG_PROFILE.getValue());
            pipeline.setConfigProfileId("  ");  // 空白字符串
            pipeline.setTargetInstanceId(TARGET_INSTANCE_ID);

            // 执行测试并验证异常
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pipeline.validate()
            );

            assertTrue(exception.getMessage().contains("配置组合ID不能为空"));
        }

        @Test
        @DisplayName("测试订阅源模式验证 - primarySubscriptionId 为空时抛出异常")
        void testValidationWithSubscription() {
            // 准备测试数据 - primarySubscriptionId 为空
            BuildPipeline pipeline = new BuildPipeline();
            pipeline.setName("测试流程");
            pipeline.setConfigType(ConfigType.SUBSCRIPTION.getValue());
            pipeline.setPrimarySubscriptionId(null);  // 缺少必要字段
            pipeline.setTargetInstanceId(TARGET_INSTANCE_ID);

            // 执行测试并验证异常
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pipeline.validate()
            );

            assertTrue(exception.getMessage().contains("主订阅源ID不能为空"));
        }

        @Test
        @DisplayName("测试订阅源模式验证 - primarySubscriptionId 为空字符串时抛出异常")
        void testValidationWithSubscriptionBlankId() {
            // 准备测试数据 - primarySubscriptionId 为空字符串
            BuildPipeline pipeline = new BuildPipeline();
            pipeline.setName("测试流程");
            pipeline.setConfigType(ConfigType.SUBSCRIPTION.getValue());
            pipeline.setPrimarySubscriptionId("  ");  // 空白字符串
            pipeline.setTargetInstanceId(TARGET_INSTANCE_ID);

            // 执行测试并验证异常
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pipeline.validate()
            );

            assertTrue(exception.getMessage().contains("主订阅源ID不能为空"));
        }

        @Test
        @DisplayName("测试配置类型为空时抛出异常")
        void testValidationWithNullConfigType() {
            // 准备测试数据 - configType 为空
            BuildPipeline pipeline = new BuildPipeline();
            pipeline.setName("测试流程");
            pipeline.setConfigType(null);

            // 执行测试并验证异常
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> pipeline.validate()
            );

            assertTrue(exception.getMessage().contains("配置类型不能为空"));
        }

        @Test
        @DisplayName("测试配置组合模式验证 - 有效配置通过验证")
        void testValidationWithValidConfigProfile() {
            // 准备测试数据 - 有效配置
            BuildPipeline pipeline = new BuildPipeline();
            pipeline.setName("测试流程");
            pipeline.setConfigType(ConfigType.CONFIG_PROFILE.getValue());
            pipeline.setConfigProfileId(CONFIG_PROFILE_ID);
            pipeline.setTargetInstanceId(TARGET_INSTANCE_ID);

            // 执行测试 - 不应该抛出异常
            assertDoesNotThrow(() -> pipeline.validate());
        }

        @Test
        @DisplayName("测试订阅源模式验证 - 有效配置通过验证")
        void testValidationWithValidSubscription() {
            // 准备测试数据 - 有效配置
            BuildPipeline pipeline = new BuildPipeline();
            pipeline.setName("测试流程");
            pipeline.setConfigType(ConfigType.SUBSCRIPTION.getValue());
            pipeline.setPrimarySubscriptionId(PRIMARY_SUB_ID);
            pipeline.setTargetInstanceId(TARGET_INSTANCE_ID);

            // 执行测试 - 不应该抛出异常
            assertDoesNotThrow(() -> pipeline.validate());
        }
    }

    // ========== ConfigType 枚举测试 ==========

    @Nested
    @DisplayName("ConfigType 枚举")
    class ConfigTypeTests {

        @Test
        @DisplayName("测试 ConfigType.fromValue - subscription")
        void testConfigTypeFromValueSubscription() {
            ConfigType type = ConfigType.fromValue("subscription");
            assertEquals(ConfigType.SUBSCRIPTION, type);
        }

        @Test
        @DisplayName("测试 ConfigType.fromValue - config-profile")
        void testConfigTypeFromValueConfigProfile() {
            ConfigType type = ConfigType.fromValue("config-profile");
            assertEquals(ConfigType.CONFIG_PROFILE, type);
        }

        @Test
        @DisplayName("测试 ConfigType.fromValue - 无效值抛出异常")
        void testConfigTypeFromValueInvalid() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ConfigType.fromValue("invalid-type")
            );
        }

        @Test
        @DisplayName("测试 ConfigType.getValue")
        void testConfigTypeGetValue() {
            assertEquals("subscription", ConfigType.SUBSCRIPTION.getValue());
            assertEquals("config-profile", ConfigType.CONFIG_PROFILE.getValue());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建配置组合模式的 BuildPipeline
     */
    private BuildPipeline createConfigProfilePipeline() {
        BuildPipeline pipeline = new BuildPipeline();
        pipeline.setId(PIPELINE_ID);
        pipeline.setName("测试配置组合流程");
        pipeline.setConfigType(ConfigType.CONFIG_PROFILE.getValue());
        pipeline.setConfigProfileId(CONFIG_PROFILE_ID);
        pipeline.setTargetInstanceId(TARGET_INSTANCE_ID);
        pipeline.setEnabled(true);
        return pipeline;
    }

    /**
     * 创建订阅源模式的 BuildPipeline
     */
    private BuildPipeline createSubscriptionPipeline() {
        BuildPipeline pipeline = new BuildPipeline();
        pipeline.setId(PIPELINE_ID);
        pipeline.setName("测试订阅源流程");
        pipeline.setConfigType(ConfigType.SUBSCRIPTION.getValue());
        pipeline.setPrimarySubscriptionId(PRIMARY_SUB_ID);
        pipeline.setTargetInstanceId(TARGET_INSTANCE_ID);
        pipeline.setEnabled(true);
        return pipeline;
    }

    /**
     * 创建 ConfigProfile 测试数据
     */
    private ConfigProfile createConfigProfile() {
        ConfigProfile profile = new ConfigProfile();
        profile.setId(CONFIG_PROFILE_ID);
        profile.setName("测试配置组合");
        profile.setSubscriptionIds(List.of("sub-001", "sub-002"));
        return profile;
    }

    /**
     * 创建 Subscription 测试数据
     */
    private Subscription createSubscription() {
        Subscription subscription = new Subscription();
        subscription.setId(PRIMARY_SUB_ID);
        subscription.setName("测试订阅源");
        subscription.setUrl("https://example.com/sub");
        return subscription;
    }

    /**
     * 创建 MihomoInstance 测试数据
     */
    private MihomoInstance createMihomoInstance() {
        MihomoInstance instance = new MihomoInstance();
        instance.setId(TARGET_INSTANCE_ID);
        instance.setName("测试实例");
        instance.setApiUrl("http://localhost:9090");
        return instance;
    }

    /**
     * 创建 ClashConfig 测试数据
     */
    private ClashConfig createClashConfig() {
        ClashConfig config = new ClashConfig("test-config");
        List<ProxyNode> proxies = new ArrayList<>();

        ProxyNode node1 = new ProxyNode();
        node1.setName("node1");
        node1.setType("vmess");
        node1.setServer("1.2.3.4");
        node1.setPort(443);
        proxies.add(node1);

        ProxyNode node2 = new ProxyNode();
        node2.setName("node2");
        node2.setType("ss");
        node2.setServer("5.6.7.8");
        node2.setPort(8388);
        proxies.add(node2);

        config.setProxies(proxies);

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("proxies", proxies);
        config.setRaw(raw);

        return config;
    }

    /**
     * 创建有效的 YAML 内容
     */
    private String createValidYamlContent() {
        return """
                proxies:
                  - name: node1
                    type: vmess
                    server: 1.2.3.4
                    port: 443
                  - name: node2
                    type: ss
                    server: 5.6.7.8
                    port: 8388
                proxy-groups:
                  - name: Proxy
                    type: select
                    proxies:
                      - node1
                      - node2
                rules:
                  - DOMAIN-SUFFIX,google.com,Proxy
                  - MATCH,Proxy
                """;
    }
}
