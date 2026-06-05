package site.kael.clash.subscription.service.impl;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.common.util.Base64Util;
import site.kael.clash.common.util.IdGenerator;
import site.kael.clash.common.util.YamlUtil;
import site.kael.clash.subscription.model.Subscription;
import site.kael.clash.subscription.repository.SubscriptionRepository;
import site.kael.clash.subscription.service.SubscriptionService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订阅源服务实现，提供 CRUD、远程获取、YAML/Base64 解析及本地缓存能力。
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    /** 默认 User-Agent，部分订阅服务商据此返回完整 Clash 配置 */
    private static final String DEFAULT_USER_AGENT = "Clash";

    /** 用于判断内容是否为 YAML 的关键词列表 */
    private static final Pattern YAML_MARKER_PATTERN = Pattern.compile(
            "^\\s*(proxies:|proxy-groups:|rules:|mixed-port:|port:|socks-port:|allow-lan:|mode:)",
            Pattern.MULTILINE
    );

    /** Base64 代理行格式：type://base64encodedinfo */
    private static final Pattern PROXY_LINE_PATTERN = Pattern.compile(
            "^(\\w+)://(.+)$"
    );

    private final SubscriptionRepository repository;
    private final OkHttpClient httpClient;
    private final String dataPath;
    private final Yaml yaml;

    public SubscriptionServiceImpl(SubscriptionRepository repository,
                                   @Value("${data.path:data}") String dataPath) {
        this.repository = repository;
        this.dataPath = dataPath;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
        this.yaml = new Yaml();

        // 确保缓存目录存在
        new File(dataPath + "/cache").mkdirs();
    }

    // ==================== CRUD ====================

    @Override
    public Subscription create(Subscription subscription) {
        subscription.setId(IdGenerator.generate());
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());
        log.info("创建订阅源: id={}, name={}", subscription.getId(), subscription.getName());
        return repository.save(subscription);
    }

    @Override
    public Subscription update(Subscription subscription) {
        if (subscription.getId() == null) {
            throw new BusinessException("更新订阅源时 ID 不能为空");
        }
        Optional<Subscription> existing = repository.findById(subscription.getId());
        if (existing.isEmpty()) {
            throw new BusinessException("订阅源不存在: " + subscription.getId());
        }
        subscription.setUpdatedAt(LocalDateTime.now());
        // 保留原始创建时间
        subscription.setCreatedAt(existing.get().getCreatedAt());
        log.info("更新订阅源: id={}", subscription.getId());
        return repository.save(subscription);
    }

    @Override
    public Optional<Subscription> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public List<Subscription> findAll() {
        return repository.findAll();
    }

    @Override
    public void deleteById(String id) {
        log.info("删除订阅源: id={}", id);
        repository.deleteById(id);
        // 同步删除缓存文件
        deleteCacheFile(id);
    }

    // ==================== 获取与解析 ====================

    @Override
    public ClashConfig fetch(String subscriptionId) {
        Subscription subscription = repository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException("订阅源不存在: " + subscriptionId));

        String responseBody;
        try {
            responseBody = doHttpRequest(subscription);
        } catch (Exception e) {
            log.warn("HTTP 请求失败，尝试从缓存加载: subscriptionId={}, error={}",
                    subscriptionId, e.getMessage());
            return loadFromCache(subscriptionId)
                    .orElseThrow(() -> new BusinessException("HTTP 请求失败且无可用缓存: " + e.getMessage()));
        }

        // 解析响应内容
        ClashConfig config = parseContent(responseBody);

        // 更新最后拉取时间
        subscription.setLastFetchedAt(LocalDateTime.now());
        repository.save(subscription);

        // 写入缓存（保存原始响应，避免解析时字段被移除）
        saveToCache(subscriptionId, responseBody);

        log.info("订阅获取成功: subscriptionId={}, proxyCount={}",
                subscriptionId, config.getProxies().size());
        return config;
    }

    // ==================== HTTP 请求 ====================

    /**
     * 执行 HTTP GET 请求，携带 User-Agent 和自定义请求头。
     */
    String doHttpRequest(Subscription subscription) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(subscription.getUrl())
                .get();

        // 设置 User-Agent
        String userAgent = subscription.getUserAgent() != null
                ? subscription.getUserAgent()
                : DEFAULT_USER_AGENT;
        builder.header("User-Agent", userAgent);

        // 设置自定义请求头
        if (subscription.getHeaders() != null) {
            for (Map.Entry<String, String> entry : subscription.getHeaders().entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP 请求失败: " + response.code());
            }
            if (response.body() == null) {
                throw new IOException("HTTP 响应体为空");
            }
            return response.body().string();
        }
    }

    // ==================== 内容解析 ====================

    /**
     * 自动检测内容格式并解析为 ClashConfig。
     * <p>
     * 优先判断是否为 YAML（通过关键词匹配），否则尝试 Base64 解码。
     */
    ClashConfig parseContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("订阅内容为空");
        }

        // 检测 YAML 格式
        if (isYamlContent(content)) {
            log.debug("检测到 YAML 格式，使用 YamlUtil 解析");
            return YamlUtil.parseClashConfig(content);
        }

        // 尝试 Base64 解码
        try {
            String decoded = Base64Util.decode(content.trim());
            ClashConfig config = parseBase64Decoded(decoded);
            if (config != null) {
                log.debug("检测到 Base64 格式，解析成功");
                return config;
            }
        } catch (Exception e) {
            log.debug("Base64 解码失败: {}", e.getMessage());
        }

        throw new BusinessException("无法解析订阅内容：既不是有效的 YAML 配置，也不是有效的 Base64 编码");
    }

    /**
     * 判断内容是否为 YAML 格式（通过特征关键词匹配）。
     */
    private boolean isYamlContent(String content) {
        return YAML_MARKER_PATTERN.matcher(content).find();
    }

    /**
     * 解析 Base64 解码后的内容为 ClashConfig。
     * <p>
     * 每行格式预期为：type://base64encodedinfo（如 ss://xxx、vmess://xxx）
     */
    private ClashConfig parseBase64Decoded(String decoded) {
        String[] lines = decoded.split("\\r?\\n");
        ClashConfig config = new ClashConfig();
        List<ProxyNode> proxies = new ArrayList<>();

        int nodeIndex = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher matcher = PROXY_LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            String type = matcher.group(1);
            String encodedInfo = matcher.group(2);

            try {
                String info = Base64Util.decode(encodedInfo);
                ProxyNode node = parseProxyInfo(type, info, nodeIndex);
                if (node != null) {
                    proxies.add(node);
                    nodeIndex++;
                }
            } catch (Exception e) {
                log.debug("解析代理行失败: {}, error: {}", line, e.getMessage());
            }
        }

        if (proxies.isEmpty()) {
            return null;
        }

        config.setProxies(proxies);
        return config;
    }

    /**
     * 解析单行代理信息为 ProxyNode。
     * <p>
     * 解码后的 info 格式因协议而异，这里做简单的通用解析。
     */
    private ProxyNode parseProxyInfo(String type, String info, int index) {
        ProxyNode node = new ProxyNode();
        node.setType(type);
        node.setName(type + "-" + index);

        // 尝试从 info 中提取 server 和 port
        // 常见格式：server:port 或 server:port?params
        try {
            String hostPort = info;
            int questionMark = info.indexOf('?');
            if (questionMark > 0) {
                hostPort = info.substring(0, questionMark);
            }
            // 处理 vmess 等 JSON 格式
            if (info.startsWith("{")) {
                // vmess 格式通常为 JSON，尝试简单提取
                Map<String, Object> json = new Yaml().load(info);
                if (json.containsKey("host")) {
                    node.setServer(String.valueOf(json.get("host")));
                }
                if (json.containsKey("port")) {
                    node.setPort(toInt(json.get("port")));
                }
                return node;
            }

            int lastColon = hostPort.lastIndexOf(':');
            if (lastColon > 0) {
                node.setServer(hostPort.substring(0, lastColon));
                node.setPort(Integer.parseInt(hostPort.substring(lastColon + 1)));
            } else {
                node.setServer(hostPort);
            }
        } catch (Exception e) {
            log.debug("解析代理地址失败: type={}, info={}", type, info);
            node.setServer("unknown");
            node.setPort(0);
        }

        return node;
    }

    private int toInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ==================== 缓存 ====================

    /**
     * 将原始响应内容保存到本地缓存文件。
     */
    void saveToCache(String subscriptionId, String content) {
        File cacheFile = getCacheFile(subscriptionId);
        try (FileWriter writer = new FileWriter(cacheFile)) {
            writer.write(content);
            log.debug("缓存已写入: {}", cacheFile.getAbsolutePath());
        } catch (IOException e) {
            log.warn("写入缓存失败: subscriptionId={}, error={}", subscriptionId, e.getMessage());
        }
    }

    /**
     * 从本地缓存文件加载 ClashConfig。若文件不存在则返回 empty。
     */
    Optional<ClashConfig> loadFromCache(String subscriptionId) {
        File cacheFile = getCacheFile(subscriptionId);
        if (!cacheFile.exists()) {
            log.debug("缓存文件不存在: {}", cacheFile.getAbsolutePath());
            return Optional.empty();
        }
        try {
            String content = Files.readString(cacheFile.toPath());
            ClashConfig config = YamlUtil.parseClashConfig(content);
            log.debug("从缓存加载成功: subscriptionId={}", subscriptionId);
            return Optional.of(config);
        } catch (Exception e) {
            log.warn("读取缓存失败: subscriptionId={}, error={}", subscriptionId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 删除指定订阅源的缓存文件。
     */
    private void deleteCacheFile(String subscriptionId) {
        File cacheFile = getCacheFile(subscriptionId);
        if (cacheFile.exists()) {
            cacheFile.delete();
            log.debug("缓存文件已删除: {}", cacheFile.getAbsolutePath());
        }
    }

    private File getCacheFile(String subscriptionId) {
        return new File(dataPath + "/cache/" + subscriptionId + ".yaml");
    }
}
