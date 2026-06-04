package site.kael.clash.subscription.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.util.Base64Util;
import site.kael.clash.subscription.model.Subscription;
import site.kael.clash.subscription.repository.SubscriptionRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionServiceImplTest {

    private SubscriptionRepository repository;
    private SubscriptionServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        repository = mock(SubscriptionRepository.class);
        service = spy(new SubscriptionServiceImpl(repository, tempDir.toString()));
    }

    // ==================== create ====================

    @Test
    void create_generatesIdAndSetsTimestamps() {
        Subscription input = new Subscription();
        input.setName("测试订阅");
        input.setUrl("https://example.com/sub");

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Subscription result = service.create(input);

        assertNotNull(result.getId());
        assertEquals(12, result.getId().length());
        assertEquals("测试订阅", result.getName());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(repository).save(result);
    }

    // ==================== update ====================

    @Test
    void update_existingSubscription_updatesTimestamp() {
        Subscription existing = new Subscription();
        existing.setId("abc123");
        existing.setName("旧名称");
        existing.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));

        when(repository.findById("abc123")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Subscription updated = new Subscription();
        updated.setId("abc123");
        updated.setName("新名称");

        Subscription result = service.update(updated);

        assertEquals("新名称", result.getName());
        // 保留原始创建时间
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(repository).save(updated);
    }

    @Test
    void update_nullId_throwsException() {
        Subscription sub = new Subscription();
        sub.setName("test");

        assertThrows(BusinessException.class, () -> service.update(sub));
    }

    @Test
    void update_notFound_throwsException() {
        Subscription sub = new Subscription();
        sub.setId("nonexistent");

        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.update(sub));
    }

    // ==================== findById ====================

    @Test
    void findById_delegatesToRepository() {
        Subscription sub = new Subscription();
        sub.setId("test-id");
        when(repository.findById("test-id")).thenReturn(Optional.of(sub));

        Optional<Subscription> result = service.findById("test-id");

        assertTrue(result.isPresent());
        assertEquals("test-id", result.get().getId());
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        Optional<Subscription> result = service.findById("missing");

        assertFalse(result.isPresent());
    }

    // ==================== findAll ====================

    @Test
    void findAll_delegatesToRepository() {
        when(repository.findAll()).thenReturn(List.of(new Subscription(), new Subscription()));

        List<Subscription> result = service.findAll();

        assertEquals(2, result.size());
    }

    // ==================== deleteById ====================

    @Test
    void deleteById_deletesFromRepositoryAndCache() throws Exception {
        // 先创建一个缓存文件
        Path cacheFile = tempDir.resolve("cache/to-delete.yaml");
        Files.createDirectories(cacheFile.getParent());
        Files.writeString(cacheFile, "proxies: []");

        service.deleteById("to-delete");

        verify(repository).deleteById("to-delete");
        assertFalse(Files.exists(cacheFile), "缓存文件应被删除");
    }

    @Test
    void deleteById_noCacheFile_doesNotThrow() {
        assertDoesNotThrow(() -> service.deleteById("no-cache"));
        verify(repository).deleteById("no-cache");
    }

    // ==================== fetch ====================

    @Test
    void fetch_yamlContent_parsesSuccessfully() throws Exception {
        Subscription sub = new Subscription();
        sub.setId("yaml-sub");
        sub.setUrl("https://example.com/clash");
        sub.setName("YAML订阅");

        when(repository.findById("yaml-sub")).thenReturn(Optional.of(sub));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String yamlContent = """
                mixed-port: 7890
                proxies:
                  - name: node1
                    type: ss
                    server: s1.example.com
                    port: 443
                  - name: node2
                    type: vmess
                    server: s2.example.com
                    port: 8443
                """;

        doReturn(yamlContent).when(service).doHttpRequest(any());

        ClashConfig config = service.fetch("yaml-sub");

        assertNotNull(config);
        assertEquals(2, config.getProxies().size());
        assertEquals("node1", config.getProxies().get(0).getName());
        assertEquals("ss", config.getProxies().get(0).getType());
        assertEquals("s1.example.com", config.getProxies().get(0).getServer());
        assertEquals(443, config.getProxies().get(0).getPort());

        // 验证 lastFetchedAt 被更新
        assertNotNull(sub.getLastFetchedAt());
        verify(repository).save(sub);
    }

    @Test
    void fetch_base64Content_parsesSuccessfully() throws Exception {
        Subscription sub = new Subscription();
        sub.setId("b64-sub");
        sub.setUrl("https://example.com/sub");
        sub.setName("Base64订阅");

        when(repository.findById("b64-sub")).thenReturn(Optional.of(sub));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 构造 Base64 编码的代理列表：每行为 type://base64(server:port)
        String proxyLine1 = "ss://" + Base64Util.encode("s1.example.com:443");
        String proxyLine2 = "trojan://" + Base64Util.encode("s2.example.com:8443");
        String base64Content = Base64Util.encode(proxyLine1 + "\n" + proxyLine2);

        doReturn(base64Content).when(service).doHttpRequest(any());

        ClashConfig config = service.fetch("b64-sub");

        assertNotNull(config);
        assertFalse(config.getProxies().isEmpty());
        assertEquals("ss", config.getProxies().get(0).getType());
        assertEquals("s1.example.com", config.getProxies().get(0).getServer());
        assertEquals(443, config.getProxies().get(0).getPort());
    }

    @Test
    void fetch_httpFailure_loadsFromCache() throws Exception {
        Subscription sub = new Subscription();
        sub.setId("cached-sub");
        sub.setUrl("https://example.com/sub");

        when(repository.findById("cached-sub")).thenReturn(Optional.of(sub));

        // 写入缓存文件
        String cachedYaml = """
                proxies:
                  - name: cached-node
                    type: ss
                    server: cached.example.com
                    port: 443
                """;
        Path cacheFile = tempDir.resolve("cache/cached-sub.yaml");
        Files.createDirectories(cacheFile.getParent());
        Files.writeString(cacheFile, cachedYaml);

        doThrow(new IOException("连接超时")).when(service).doHttpRequest(any());

        ClashConfig config = service.fetch("cached-sub");

        assertNotNull(config);
        assertFalse(config.getProxies().isEmpty());
        assertEquals("cached-node", config.getProxies().get(0).getName());
    }

    @Test
    void fetch_httpFailureNoCache_throwsException() throws Exception {
        Subscription sub = new Subscription();
        sub.setId("no-cache-sub");
        sub.setUrl("https://example.com/sub");

        when(repository.findById("no-cache-sub")).thenReturn(Optional.of(sub));

        doThrow(new IOException("连接超时")).when(service).doHttpRequest(any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.fetch("no-cache-sub"));
        assertTrue(ex.getMessage().contains("HTTP 请求失败且无可用缓存"));
    }

    @Test
    void fetch_subscriptionNotFound_throwsException() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.fetch("missing"));
        assertTrue(ex.getMessage().contains("订阅源不存在"));
    }

    // ==================== parseContent ====================

    @Test
    void parseContent_yamlWithProxies_parsesCorrectly() {
        String yaml = """
                proxies:
                  - name: test-node
                    type: ss
                    server: test.com
                    port: 443
                rules:
                  - MATCH,DIRECT
                """;

        ClashConfig config = service.parseContent(yaml);

        assertNotNull(config);
        assertEquals(1, config.getProxies().size());
        assertEquals("test-node", config.getProxies().get(0).getName());
    }

    @Test
    void parseContent_yamlWithMixedPort_detectedAsYaml() {
        String yaml = """
                mixed-port: 7890
                allow-lan: false
                mode: rule
                """;

        ClashConfig config = service.parseContent(yaml);

        assertNotNull(config);
    }

    @Test
    void parseContent_emptyContent_throwsException() {
        assertThrows(BusinessException.class, () -> service.parseContent(""));
        assertThrows(BusinessException.class, () -> service.parseContent(null));
        assertThrows(BusinessException.class, () -> service.parseContent("   "));
    }

    @Test
    void parseContent_invalidContent_throwsException() {
        assertThrows(BusinessException.class, () -> service.parseContent("这不是YAML也不是Base64!!!"));
    }

    @Test
    void parseContent_base64ProxyList_parsesNodes() {
        String proxyLine1 = "ss://" + Base64Util.encode("server1.com:443");
        String proxyLine2 = "vmess://" + Base64Util.encode("server2.com:8443");
        String base64 = Base64Util.encode(proxyLine1 + "\n" + proxyLine2);

        ClashConfig config = service.parseContent(base64);

        assertNotNull(config);
        assertEquals(2, config.getProxies().size());
        assertEquals("ss", config.getProxies().get(0).getType());
        assertEquals("vmess", config.getProxies().get(1).getType());
    }

    // ==================== cache ====================

    @Test
    void saveAndLoadCache_roundTrip() {
        String yamlContent = "mixed-port: 7890\nproxies:\n- {name: n1, type: ss, server: s1.com, port: 443}\n";

        service.saveToCache("cache-test", yamlContent);

        Optional<ClashConfig> loaded = service.loadFromCache("cache-test");
        assertTrue(loaded.isPresent());
        assertNotNull(loaded.get().getRaw());
    }

    @Test
    void loadFromCache_nonexistent_returnsEmpty() {
        Optional<ClashConfig> result = service.loadFromCache("nonexistent");
        assertFalse(result.isPresent());
    }
}
