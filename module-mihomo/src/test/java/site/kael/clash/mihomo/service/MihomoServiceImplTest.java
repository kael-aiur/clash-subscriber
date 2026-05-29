package site.kael.clash.mihomo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.mihomo.client.MihomoHttpClient;
import site.kael.clash.mihomo.model.HealthStatus;
import site.kael.clash.mihomo.model.MihomoInstance;
import site.kael.clash.mihomo.repository.MihomoInstanceRepository;
import site.kael.clash.mihomo.service.impl.MihomoServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MihomoServiceImplTest {

    @Mock
    private MihomoInstanceRepository repository;

    @Mock
    private MihomoHttpClient httpClient;

    @InjectMocks
    private MihomoServiceImpl service;

    private MihomoInstance sampleInstance;

    @BeforeEach
    void setUp() {
        sampleInstance = new MihomoInstance();
        sampleInstance.setId("test-001");
        sampleInstance.setName("测试实例");
        sampleInstance.setApiUrl("http://localhost:9090");
        sampleInstance.setApiSecret("secret123");
        sampleInstance.setEnabled(true);
        sampleInstance.setStatus(HealthStatus.UNKNOWN);
    }

    // ========== CRUD 测试 ==========

    @Test
    void create_shouldGenerateIdAndSave() {
        MihomoInstance input = new MihomoInstance();
        input.setName("新实例");
        input.setApiUrl("http://localhost:9090");

        when(repository.save(any(MihomoInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MihomoInstance result = service.create(input);

        assertNotNull(result.getId());
        assertEquals("新实例", result.getName());

        ArgumentCaptor<MihomoInstance> captor = ArgumentCaptor.forClass(MihomoInstance.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getId());
    }

    @Test
    void update_shouldSaveExistingInstance() {
        when(repository.findById("test-001")).thenReturn(Optional.of(sampleInstance));
        when(repository.save(any(MihomoInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sampleInstance.setName("更新后的名称");
        MihomoInstance result = service.update(sampleInstance);

        assertEquals("更新后的名称", result.getName());
        verify(repository).save(sampleInstance);
    }

    @Test
    void update_shouldThrowWhenIdIsNull() {
        MihomoInstance noId = new MihomoInstance();
        noId.setName("无ID");

        assertThrows(BusinessException.class, () -> service.update(noId));
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        MihomoInstance missing = new MihomoInstance();
        missing.setId("nonexistent");

        assertThrows(BusinessException.class, () -> service.update(missing));
    }

    @Test
    void findById_shouldDelegateToRepository() {
        when(repository.findById("test-001")).thenReturn(Optional.of(sampleInstance));

        Optional<MihomoInstance> result = service.findById("test-001");

        assertTrue(result.isPresent());
        assertEquals("测试实例", result.get().getName());
    }

    @Test
    void findAll_shouldDelegateToRepository() {
        when(repository.findAll()).thenReturn(List.of(sampleInstance));

        List<MihomoInstance> result = service.findAll();

        assertEquals(1, result.size());
    }

    @Test
    void deleteById_shouldDelegateToRepository() {
        service.deleteById("test-001");

        verify(repository).deleteById("test-001");
    }

    // ========== 健康检查测试 ==========

    @Test
    void checkHealth_shouldReturnHealthyWhenOk() {
        when(repository.findById("test-001")).thenReturn(Optional.of(sampleInstance));
        when(httpClient.checkHealth("http://localhost:9090", "secret123")).thenReturn(true);
        when(repository.save(any(MihomoInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HealthStatus status = service.checkHealth("test-001");

        assertEquals(HealthStatus.HEALTHY, status);
        verify(repository).save(argThat(inst ->
                inst.getStatus() == HealthStatus.HEALTHY && inst.getLastHealthCheck() != null));
    }

    @Test
    void checkHealth_shouldReturnUnhealthyWhenFail() {
        when(repository.findById("test-001")).thenReturn(Optional.of(sampleInstance));
        when(httpClient.checkHealth("http://localhost:9090", "secret123")).thenReturn(false);
        when(repository.save(any(MihomoInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HealthStatus status = service.checkHealth("test-001");

        assertEquals(HealthStatus.UNHEALTHY, status);
    }

    @Test
    void checkHealth_shouldThrowWhenNotFound() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.checkHealth("nonexistent"));
    }

    @Test
    void checkHealthAll_shouldCheckOnlyEnabledInstances() {
        MihomoInstance disabled = new MihomoInstance();
        disabled.setId("disabled-001");
        disabled.setEnabled(false);

        MihomoInstance enabled2 = new MihomoInstance();
        enabled2.setId("enabled-002");
        enabled2.setName("实例2");
        enabled2.setApiUrl("http://localhost:9091");
        enabled2.setApiSecret(null);
        enabled2.setEnabled(true);

        when(repository.findAll()).thenReturn(List.of(sampleInstance, disabled, enabled2));
        // checkHealth 内部会调用 findById
        when(repository.findById("test-001")).thenReturn(Optional.of(sampleInstance));
        when(repository.findById("enabled-002")).thenReturn(Optional.of(enabled2));
        // sampleInstance 健康
        when(httpClient.checkHealth("http://localhost:9090", "secret123")).thenReturn(true);
        // enabled2 不健康
        when(httpClient.checkHealth("http://localhost:9091", null)).thenReturn(false);
        when(repository.save(any(MihomoInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, HealthStatus> results = service.checkHealthAll();

        assertEquals(2, results.size());
        assertEquals(HealthStatus.HEALTHY, results.get("test-001"));
        assertEquals(HealthStatus.UNHEALTHY, results.get("enabled-002"));
        assertFalse(results.containsKey("disabled-001"));
    }

    @Test
    void checkHealthAll_shouldHandleExceptionPerInstance() {
        MihomoInstance instance2 = new MihomoInstance();
        instance2.setId("inst-002");
        instance2.setApiUrl("http://localhost:9091");
        instance2.setApiSecret(null);
        instance2.setEnabled(true);

        when(repository.findAll()).thenReturn(List.of(sampleInstance, instance2));
        // checkHealth 内部会调用 findById
        when(repository.findById("test-001")).thenReturn(Optional.of(sampleInstance));
        when(repository.findById("inst-002")).thenReturn(Optional.of(instance2));
        // sampleInstance 健康
        when(httpClient.checkHealth("http://localhost:9090", "secret123")).thenReturn(true);
        // instance2 的 httpClient 调用抛异常
        when(httpClient.checkHealth("http://localhost:9091", null)).thenThrow(new RuntimeException("连接超时"));
        when(repository.save(any(MihomoInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, HealthStatus> results = service.checkHealthAll();

        assertEquals(2, results.size());
        assertEquals(HealthStatus.HEALTHY, results.get("test-001"));
        assertEquals(HealthStatus.UNHEALTHY, results.get("inst-002"));
    }

    // ========== 配置推送测试 ==========

    @Test
    void pushConfig_shouldSerializeAndPush() {
        when(repository.findById("test-001")).thenReturn(Optional.of(sampleInstance));

        ClashConfig config = new ClashConfig("test-config");
        config.getRaw().put("port", 7890);
        config.getRaw().put("mode", "rule");

        service.pushConfig("test-001", config);

        ArgumentCaptor<String> yamlCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).pushConfig(eq("http://localhost:9090"), eq("secret123"), yamlCaptor.capture());

        String yamlOutput = yamlCaptor.getValue();
        assertTrue(yamlOutput.contains("port"));
        assertTrue(yamlOutput.contains("7890"));
        assertTrue(yamlOutput.contains("mode"));
        assertTrue(yamlOutput.contains("rule"));
    }

    @Test
    void pushConfig_shouldThrowWhenNotFound() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        ClashConfig config = new ClashConfig();
        assertThrows(BusinessException.class, () -> service.pushConfig("nonexistent", config));
    }

    @Test
    void pushConfigAll_shouldPushToAllEnabledInstances() {
        MihomoInstance disabled = new MihomoInstance();
        disabled.setId("disabled-001");
        disabled.setEnabled(false);

        MihomoInstance enabled2 = new MihomoInstance();
        enabled2.setId("enabled-002");
        enabled2.setApiUrl("http://localhost:9091");
        enabled2.setEnabled(true);

        when(repository.findAll()).thenReturn(List.of(sampleInstance, disabled, enabled2));

        ClashConfig config = new ClashConfig("test-config");
        config.getRaw().put("port", 7890);

        Map<String, Boolean> results = service.pushConfigAll(config);

        assertEquals(2, results.size());
        assertTrue(results.get("test-001"));
        assertTrue(results.get("enabled-002"));
        assertFalse(results.containsKey("disabled-001"));

        // 推送了两次（两个启用的实例）
        verify(httpClient, times(2)).pushConfig(anyString(), any(), anyString());
    }

    @Test
    void pushConfigAll_shouldHandleFailurePerInstance() {
        MihomoInstance instance2 = new MihomoInstance();
        instance2.setId("inst-002");
        instance2.setApiUrl("http://localhost:9091");
        instance2.setEnabled(true);

        when(repository.findAll()).thenReturn(List.of(sampleInstance, instance2));
        doNothing().when(httpClient).pushConfig(eq("http://localhost:9090"), eq("secret123"), anyString());
        doThrow(new BusinessException("推送失败")).when(httpClient).pushConfig(eq("http://localhost:9091"), any(), anyString());

        ClashConfig config = new ClashConfig("test-config");

        Map<String, Boolean> results = service.pushConfigAll(config);

        assertEquals(2, results.size());
        assertTrue(results.get("test-001"));
        assertFalse(results.get("inst-002"));
    }
}
