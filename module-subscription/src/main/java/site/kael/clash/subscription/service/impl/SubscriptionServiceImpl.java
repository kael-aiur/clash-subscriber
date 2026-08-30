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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订阅源服务实现，提供 CRUD、远程获取、本地配置、YAML/Base64 解析及本地缓存能力。
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    /** 默认 User-Agent，部分订阅服务商据此返回完整 Clash 配置 */
    private static final String DEFAULT_USER_AGENT = "Clash";

    /** 订阅类型 */
    public static final String TYPE_REMOTE = "remote";
    public static final String TYPE_LOCAL = "local";
    private static final Set<String> SUPPORTED_TYPES = Set.of(TYPE_REMOTE, TYPE_LOCAL);

    /** 单个本地订阅允许保存的最大内容大小（1MB） */
    private static final int MAX_LOCAL_CONTENT_LENGTH = 1024 * 1024;

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
        prepareForSave(subscription, null);
        log.info("创建订阅源: id={}, name={}, type={}",
                subscription.getId(), subscription.getName(), subscription.getType());
        return repository.save(subscription);
    }

    @Override
    public Subscription update(Subscription subscription) {
        if (subscription.getId() == null) {
            throw new BusinessException("更新订阅源时 ID 不能为空");
        }
        Subscription existing = repository.findById(subscription.getId())
                .orElseThrow(() -> new BusinessException("订阅源不存在: " + subscription.getId()));
        subscription.setUpdatedAt(LocalDateTime.now());
        // 保留原始创建时间
        subscription.setCreatedAt(existing.getCreatedAt());
        prepareForSave(subscription, existing);
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

        if (isLocal(subscription)) {
            return fetchLocal(subscription);
        }
        return fetchRemote(subscription);
    }

    @Override
    public String getSavedContent(String subscriptionId) {
        Subscription subscription = repository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException("订阅源不存在: " + subscriptionId));
        if (!isLocal(subscription)) {
            return "";
        }
        return readCacheFile(subscriptionId)
                .orElseThrow(() -> new BusinessException("本地订阅配置文件不存在，请重新保存订阅源"));
    }

    private ClashConfig fetchLocal(Subscription subscription) {
        String content = readCacheFile(subscription.getId())
                .orElseThrow(() -> new BusinessException("本地订阅配置文件不存在，请重新保存订阅源"));
        ClashConfig config = parseContent(content);
        subscription.setLastFetchedAt(LocalDateTime.now());
        repository.save(subscription);
        log.info("本地订阅读取成功: subscriptionId={}, proxyCount={}",
                subscription.getId(), config.getProxies().size());
        return config;
    }

    private ClashConfig fetchRemote(Subscription subscription) {
        String responseBody;
        try {
            responseBody = doHttpRequest(subscription);
        } catch (Exception e) {
            log.warn("HTTP 请求失败，尝试从缓存加载: subscriptionId={}, error={}",
                    subscription.getId(), e.getMessage());
            return loadFromCache(subscription.getId())
                    .orElseThrow(() -> new BusinessException("HTTP 请求失败且无可用缓存: " + e.getMessage()));
        }

        // 解析响应内容
        ClashConfig config = parseContent(responseBody);

        // 更新最后拉取时间
        subscription.setLastFetchedAt(LocalDateTime.now());
        repository.save(subscription);

        // 写入缓存（保存原始响应，避免解析时字段被移除）
        saveToCache(subscription.getId(), responseBody);

        log.info("订阅获取成功: subscriptionId={}, proxyCount={}",
                subscription.getId(), config.getProxies().size());
        return config;
    }

    // ==================== 本地订阅 ====================

    /**
     * 校验订阅数据并持久化本地配置。
     * JSON 模型中的 content 只作为传输字段，实际内容保存到缓存文件。
     */
    private void prepareForSave(Subscription subscription, Subscription existing) {
        if (subscription.getType() == null || subscription.getType().isBlank()) {
            subscription.setType(TYPE_REMOTE);
        }
        if (!SUPPORTED_TYPES.contains(subscription.getType())) {
            throw new BusinessException("不支持的订阅类型: " + subscription.getType());
        }

        if (isRemote(subscription)) {
            if (subscription.getUrl() == null || subscription.getUrl().isBlank()) {
                throw new BusinessException("远程订阅的 URL 不能为空");
            }
            subscription.setContent(null);
            return;
        }

        if (subscription.getName() == null || subscription.getName().isBlank()) {
            throw new BusinessException("订阅源名称不能为空");
        }
        subscription.setUrl(null);
        subscription.setUserAgent(null);
        if (subscription.getHeaders() != null && !subscription.getHeaders().isEmpty()) {
            subscription.setHeaders(new java.util.HashMap<>());
        }

        boolean contentChanged = subscription.getContent() != null;
        if (contentChanged) {
            String content = subscription.getContent();
            if (content.isBlank()) {
                throw new BusinessException("本地订阅配置内容不能为空");
            }
            if (content.length() > MAX_LOCAL_CONTENT_LENGTH) {
                throw new BusinessException("本地订阅配置内容不能超过 1MB");
            }
            // 立即校验，避免保存无法被构建流程使用的配置
            parseContent(content);
            saveToCache(subscription.getId(), content);
        } else if (existing == null) {
            throw new BusinessException("本地订阅配置内容不能为空");
        } else {
            // 更新元数据时不清空既有本地配置；缓存文件丢失时报错，避免生成不可用订阅
            boolean cacheExists = getCacheFile(subscription.getId()).exists();
            if (!cacheExists) {
                throw new BusinessException("本地订阅配置文件不存在，请提交配置内容");
            }
        }
        subscription.setContent(null);
    }

    private boolean isLocal(Subscription subscription) {
        return TYPE_LOCAL.equalsIgnoreCase(subscription.getType());
    }

    private boolean isRemote(Subscription subscription) {
        return TYPE_REMOTE.equalsIgnoreCase(subscription.getType());
    }

    private Optional<String> readCacheFile(String subscriptionId) {
        File cacheFile = getCacheFile(subscriptionId);
        if (!cacheFile.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(cacheFile.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BusinessException("读取本地订阅配置失败: " + e.getMessage());
        }
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
        try (FileWriter writer = new FileWriter(cacheFile, StandardCharsets.UTF_8)) {
            writer.write(content);
            log.debug("缓存已写入: {}", cacheFile.getAbsolutePath());
        } catch (IOException e) {
            throw new BusinessException("保存本地订阅配置失败: " + e.getMessage());
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
            String content = Files.readString(cacheFile.toPath(), StandardCharsets.UTF_8);
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
