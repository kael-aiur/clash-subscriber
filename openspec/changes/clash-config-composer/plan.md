# Clash 配置生成器实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建独立的配置生成器，支持动态组合订阅源、代理组、规则组，生成标准 Clash YAML 配置，支持 Basic Auth 认证分享

**Architecture:** 基于现有模块扩展，在 module-processor 中添加配置生成服务，在 module-web 中添加 REST API 和前端页面，复用现有的订阅源管理和规则组管理功能

**Tech Stack:** Java 21, Spring Boot 3.2.5, Vue 3, Element Plus, TypeScript

---

## 文件结构

### 后端文件

**模块: module-processor**

- `src/main/java/site/kael/clash/processor/model/ConfigProfile.java` - 配置组合模型
- `src/main/java/site/kael/clash/processor/model/ProxyGroupConfig.java` - 代理组配置模型
- `src/main/java/site/kael/clash/processor/model/ClashBasicConfig.java` - Clash 基础配置模型
- `src/main/java/site/kael/clash/processor/model/RuleGroupRef.java` - 规则组引用模型
- `src/main/java/site/kael/clash/processor/repository/ConfigProfileRepository.java` - 仓储接口
- `src/main/java/site/kael/clash/processor/repository/JsonFileConfigProfileRepository.java` - 仓储实现
- `src/main/java/site/kael/clash/processor/service/ConfigGeneratorService.java` - 服务接口
- `src/main/java/site/kael/clash/processor/service/impl/ConfigGeneratorServiceImpl.java` - 服务实现

**模块: module-web**

- `src/main/java/site/kael/clash/web/controller/ConfigProfileController.java` - 配置管理 API
- `src/main/java/site/kael/clash/web/auth/BasicAuthInterceptor.java` - Basic Auth 拦截器

**测试文件**

- `module-processor/src/test/java/site/kael/clash/processor/service/ConfigGeneratorServiceImplTest.java` - 服务单元测试
- `module-web/src/test/java/site/kael/clash/web/controller/ConfigProfileControllerTest.java` - API 集成测试

### 前端文件

- `module-web/frontend/src/views/ConfigProfileListView.vue` - 配置列表页
- `module-web/frontend/src/views/ConfigProfileEditView.vue` - 配置编辑页
- `module-web/frontend/src/components/config-profile/BasicInfoSection.vue` - 基本信息组件
- `module-web/frontend/src/components/config-profile/SubscriptionSelect.vue` - 订阅源选择组件
- `module-web/frontend/src/components/config-profile/ProxyGroupEditor.vue` - 代理组编辑组件
- `module-web/frontend/src/components/config-profile/RuleGroupSorter.vue` - 规则组排序组件
- `module-web/frontend/src/components/config-profile/BasicConfigSection.vue` - 基础信息配置组件
- `module-web/frontend/src/api/config-profile.ts` - API 调用

---

## Task 1: 创建数据模型

**Files:**
- Create: `module-processor/src/main/java/site/kael/clash/processor/model/ConfigProfile.java`
- Create: `module-processor/src/main/java/site/kael/clash/processor/model/ProxyGroupConfig.java`
- Create: `module-processor/src/main/java/site/kael/clash/processor/model/ClashBasicConfig.java`
- Create: `module-processor/src/main/java/site/kael/clash/processor/model/RuleGroupRef.java`

- [ ] **Step 1: 创建 ConfigProfile 模型类**

```java
package site.kael.clash.processor.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConfigProfile {
    private String id;
    private String name;
    private String description;
    private List<String> subscriptionIds = new ArrayList<>();
    private List<ProxyGroupConfig> proxyGroups = new ArrayList<>();
    private List<RuleGroupRef> ruleGroups = new ArrayList<>();
    private ClashBasicConfig basicConfig = new ClashBasicConfig();
    private String authUsername;
    private String authPassword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConfigProfile() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getSubscriptionIds() { return subscriptionIds; }
    public void setSubscriptionIds(List<String> subscriptionIds) { this.subscriptionIds = subscriptionIds; }
    public List<ProxyGroupConfig> getProxyGroups() { return proxyGroups; }
    public void setProxyGroups(List<ProxyGroupConfig> proxyGroups) { this.proxyGroups = proxyGroups; }
    public List<RuleGroupRef> getRuleGroups() { return ruleGroups; }
    public void setRuleGroups(List<RuleGroupRef> ruleGroups) { this.ruleGroups = ruleGroups; }
    public ClashBasicConfig getBasicConfig() { return basicConfig; }
    public void setBasicConfig(ClashBasicConfig basicConfig) { this.basicConfig = basicConfig; }
    public String getAuthUsername() { return authUsername; }
    public void setAuthUsername(String authUsername) { this.authUsername = authUsername; }
    public String getAuthPassword() { return authPassword; }
    public void setAuthPassword(String authPassword) { this.authPassword = authPassword; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: 创建 ProxyGroupConfig 模型类**

```java
package site.kael.clash.processor.model;

import java.util.ArrayList;
import java.util.List;

public class ProxyGroupConfig {
    private String name;
    private String type; // select, url-test, fallback, load-balance
    private List<String> nodeNames = new ArrayList<>();
    private List<String> matchKeywords = new ArrayList<>();
    private boolean includeAll;
    private String url;
    private int interval;

    public ProxyGroupConfig() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<String> getNodeNames() { return nodeNames; }
    public void setNodeNames(List<String> nodeNames) { this.nodeNames = nodeNames; }
    public List<String> getMatchKeywords() { return matchKeywords; }
    public void setMatchKeywords(List<String> matchKeywords) { this.matchKeywords = matchKeywords; }
    public boolean isIncludeAll() { return includeAll; }
    public void setIncludeAll(boolean includeAll) { this.includeAll = includeAll; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }
}
```

- [ ] **Step 3: 创建 ClashBasicConfig 模型类**

```java
package site.kael.clash.processor.model;

public class ClashBasicConfig {
    private int mixedPort = 7890;
    private int port = 7891;
    private int socksPort = 7892;
    private int redirPort = 7893;
    private boolean allowLan = false;
    private String mode = "rule";
    private String logLevel = "info";
    private String externalController = "127.0.0.1:9090";
    private String secret = "";

    public ClashBasicConfig() {}

    // Getters and Setters
    public int getMixedPort() { return mixedPort; }
    public void setMixedPort(int mixedPort) { this.mixedPort = mixedPort; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getSocksPort() { return socksPort; }
    public void setSocksPort(int socksPort) { this.socksPort = socksPort; }
    public int getRedirPort() { return redirPort; }
    public void setRedirPort(int redirPort) { this.redirPort = redirPort; }
    public boolean isAllowLan() { return allowLan; }
    public void setAllowLan(boolean allowLan) { this.allowLan = allowLan; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
    public String getExternalController() { return externalController; }
    public void setExternalController(String externalController) { this.externalController = externalController; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
```

- [ ] **Step 4: 创建 RuleGroupRef 模型类**

```java
package site.kael.clash.processor.model;

public class RuleGroupRef {
    private String ruleGroupId;
    private int priority;

    public RuleGroupRef() {}

    public RuleGroupRef(String ruleGroupId, int priority) {
        this.ruleGroupId = ruleGroupId;
        this.priority = priority;
    }

    // Getters and Setters
    public String getRuleGroupId() { return ruleGroupId; }
    public void setRuleGroupId(String ruleGroupId) { this.ruleGroupId = ruleGroupId; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}
```

- [ ] **Step 5: 提交代码**

```bash
git add module-processor/src/main/java/site/kael/clash/processor/model/ConfigProfile.java
git add module-processor/src/main/java/site/kael/clash/processor/model/ProxyGroupConfig.java
git add module-processor/src/main/java/site/kael/clash/processor/model/ClashBasicConfig.java
git add module-processor/src/main/java/site/kael/clash/processor/model/RuleGroupRef.java
git commit -m "feat(config-profile): 添加配置组合数据模型"
```

---

## Task 2: 创建存储层

**Files:**
- Create: `module-processor/src/main/java/site/kael/clash/processor/repository/ConfigProfileRepository.java`
- Create: `module-processor/src/main/java/site/kael/clash/processor/repository/JsonFileConfigProfileRepository.java`

- [ ] **Step 1: 创建 ConfigProfileRepository 接口**

```java
package site.kael.clash.processor.repository;

import site.kael.clash.processor.model.ConfigProfile;

import java.util.List;
import java.util.Optional;

public interface ConfigProfileRepository {

    ConfigProfile save(ConfigProfile profile);

    Optional<ConfigProfile> findById(String id);

    Optional<ConfigProfile> findByName(String name);

    List<ConfigProfile> findAll();

    void deleteById(String id);

    boolean existsByName(String name);
}
```

- [ ] **Step 2: 创建 JsonFileConfigProfileRepository 实现**

```java
package site.kael.clash.processor.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.processor.model.ConfigProfile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JsonFileConfigProfileRepository implements ConfigProfileRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonFileConfigProfileRepository.class);

    private final ObjectMapper objectMapper;
    private final Path configProfileDir;

    public JsonFileConfigProfileRepository(
            ObjectMapper objectMapper,
            @Value("${data.path:data}") String dataPath) {
        this.objectMapper = objectMapper;
        this.configProfileDir = Paths.get(dataPath, "config-profiles");
        try {
            Files.createDirectories(configProfileDir);
            log.info("配置组合目录: {}", configProfileDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建配置组合目录: " + configProfileDir, e);
        }
    }

    @Override
    public ConfigProfile save(ConfigProfile profile) {
        Path filePath = configProfileDir.resolve(profile.getId() + ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), profile);
            log.debug("保存配置组合: {}", filePath);
            return profile;
        } catch (IOException e) {
            throw new RuntimeException("保存配置组合失败: " + profile.getId(), e);
        }
    }

    @Override
    public Optional<ConfigProfile> findById(String id) {
        Path filePath = configProfileDir.resolve(id + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            ConfigProfile profile = objectMapper.readValue(filePath.toFile(), ConfigProfile.class);
            return Optional.of(profile);
        } catch (IOException e) {
            throw new RuntimeException("读取配置组合失败: " + id, e);
        }
    }

    @Override
    public Optional<ConfigProfile> findByName(String name) {
        return findAll().stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst();
    }

    @Override
    public List<ConfigProfile> findAll() {
        List<ConfigProfile> profiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configProfileDir, "*.json")) {
            for (Path filePath : stream) {
                try {
                    ConfigProfile profile = objectMapper.readValue(filePath.toFile(), ConfigProfile.class);
                    profiles.add(profile);
                } catch (IOException e) {
                    log.warn("跳过无法读取的配置组合文件: {}", filePath, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("遍历配置组合目录失败", e);
        }
        return profiles;
    }

    @Override
    public void deleteById(String id) {
        Path filePath = configProfileDir.resolve(id + ".json");
        try {
            Files.deleteIfExists(filePath);
            log.debug("删除配置组合: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("删除配置组合失败: " + id, e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }
}
```

- [ ] **Step 3: 提交代码**

```bash
git add module-processor/src/main/java/site/kael/clash/processor/repository/ConfigProfileRepository.java
git add module-processor/src/main/java/site/kael/clash/processor/repository/JsonFileConfigProfileRepository.java
git commit -m "feat(config-profile): 添加配置组合存储层"
```

---

## Task 3: 创建核心服务

**Files:**
- Create: `module-processor/src/main/java/site/kael/clash/processor/service/ConfigGeneratorService.java`
- Create: `module-processor/src/main/java/site/kael/clash/processor/service/impl/ConfigGeneratorServiceImpl.java`

- [ ] **Step 1: 创建 ConfigGeneratorService 接口**

```java
package site.kael.clash.processor.service;

import site.kael.clash.processor.model.ConfigProfile;

public interface ConfigGeneratorService {

    /**
     * 根据配置组合生成 Clash YAML 配置
     *
     * @param profile 配置组合
     * @return Clash YAML 配置字符串
     */
    String generate(ConfigProfile profile);

    /**
     * 根据配置名称生成 Clash YAML 配置
     *
     * @param name 配置名称
     * @return Clash YAML 配置字符串
     */
    String generateByName(String name);
}
```

- [ ] **Step 2: 创建 ConfigGeneratorServiceImpl 实现**

```java
package site.kael.clash.processor.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.model.*;
import site.kael.clash.processor.repository.ConfigProfileRepository;
import site.kael.clash.processor.repository.RuleGroupRepository;
import site.kael.clash.processor.service.ConfigGeneratorService;
import site.kael.clash.subscription.service.SubscriptionService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConfigGeneratorServiceImpl implements ConfigGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ConfigGeneratorServiceImpl.class);

    private final ConfigProfileRepository configProfileRepository;
    private final SubscriptionService subscriptionService;
    private final RuleGroupRepository ruleGroupRepository;

    public ConfigGeneratorServiceImpl(
            ConfigProfileRepository configProfileRepository,
            SubscriptionService subscriptionService,
            RuleGroupRepository ruleGroupRepository) {
        this.configProfileRepository = configProfileRepository;
        this.subscriptionService = subscriptionService;
        this.ruleGroupRepository = ruleGroupRepository;
    }

    @Override
    public String generate(ConfigProfile profile) {
        log.info("生成配置: name={}", profile.getName());

        // 1. 合并订阅源节点
        List<ProxyNode> allNodes = mergeSubscriptions(profile.getSubscriptionIds());

        // 2. 构建代理组
        Map<String, Object> proxyGroups = buildProxyGroups(profile.getProxyGroups(), allNodes);

        // 3. 构建规则
        List<String> rules = buildRules(profile.getRuleGroups());

        // 4. 构建完整配置
        ClashConfig config = buildClashConfig(profile, allNodes, proxyGroups, rules);

        // 5. 转换为 YAML
        return toYaml(config);
    }

    @Override
    public String generateByName(String name) {
        ConfigProfile profile = configProfileRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("配置不存在: " + name));
        return generate(profile);
    }

    private List<ProxyNode> mergeSubscriptions(List<String> subscriptionIds) {
        List<ProxyNode> allNodes = new ArrayList<>();
        for (String subscriptionId : subscriptionIds) {
            try {
                ClashConfig config = subscriptionService.fetch(subscriptionId);
                if (config != null && config.getProxies() != null) {
                    allNodes.addAll(config.getProxies());
                    log.info("合并订阅源节点: subscriptionId={}, nodes={}", subscriptionId, config.getProxies().size());
                }
            } catch (Exception e) {
                log.error("获取订阅源失败: subscriptionId={}", subscriptionId, e);
            }
        }
        return allNodes;
    }

    private Map<String, Object> buildProxyGroups(List<ProxyGroupConfig> groupConfigs, List<ProxyNode> allNodes) {
        Map<String, Object> proxyGroups = new LinkedHashMap<>();

        for (ProxyGroupConfig groupConfig : groupConfigs) {
            List<String> proxies = new ArrayList<>();

            if (groupConfig.isIncludeAll()) {
                // 包含所有节点
                proxies = allNodes.stream()
                        .map(ProxyNode::getName)
                        .collect(Collectors.toList());
            } else if (groupConfig.getNodeNames() != null && !groupConfig.getNodeNames().isEmpty()) {
                // 直接选择节点
                proxies = groupConfig.getNodeNames();
            } else if (groupConfig.getMatchKeywords() != null && !groupConfig.getMatchKeywords().isEmpty()) {
                // 标签匹配
                proxies = allNodes.stream()
                        .filter(node -> matchKeywords(node.getName(), groupConfig.getMatchKeywords()))
                        .map(ProxyNode::getName)
                        .collect(Collectors.toList());
            }

            Map<String, Object> groupData = new LinkedHashMap<>();
            groupData.put("type", groupConfig.getType());
            groupData.put("proxies", proxies);

            if (groupConfig.getUrl() != null) {
                groupData.put("url", groupConfig.getUrl());
            }
            if (groupConfig.getInterval() > 0) {
                groupData.put("interval", groupConfig.getInterval());
            }

            proxyGroups.put(groupConfig.getName(), groupData);
        }

        return proxyGroups;
    }

    private boolean matchKeywords(String nodeName, List<String> keywords) {
        String lowerName = nodeName.toLowerCase();
        return keywords.stream()
                .anyMatch(keyword -> lowerName.contains(keyword.toLowerCase()));
    }

    private List<String> buildRules(List<RuleGroupRef> ruleGroupRefs) {
        List<String> rules = new ArrayList<>();

        // 按优先级排序
        List<RuleGroupRef> sortedRefs = ruleGroupRefs.stream()
                .sorted(Comparator.comparingInt(RuleGroupRef::getPriority))
                .collect(Collectors.toList());

        for (RuleGroupRef ref : sortedRefs) {
            try {
                ruleGroupRepository.findById(ref.getRuleGroupId())
                        .ifPresent(ruleGroup -> {
                            if (ruleGroup.getRules() != null) {
                                rules.addAll(ruleGroup.getRules());
                            }
                        });
            } catch (Exception e) {
                log.error("读取规则组失败: ruleGroupId={}", ref.getRuleGroupId(), e);
            }
        }

        return rules;
    }

    private ClashConfig buildClashConfig(ConfigProfile profile, List<ProxyNode> allNodes,
                                         Map<String, Object> proxyGroups, List<String> rules) {
        ClashConfig config = new ClashConfig(profile.getName());

        // 设置基础信息
        ClashBasicConfig basicConfig = profile.getConfig();
        config.getRaw().put("mixed-port", basicConfig.getMixedPort());
        config.getRaw().put("port", basicConfig.getPort());
        config.getRaw().put("socks-port", basicConfig.getSocksPort());
        config.getRaw().put("redir-port", basicConfig.getRedirPort());
        config.getRaw().put("allow-lan", basicConfig.isAllowLan());
        config.getRaw().put("mode", basicConfig.getMode());
        config.getRaw().put("log-level", basicConfig.getLogLevel());
        config.getRaw().put("external-controller", basicConfig.getExternalController());
        if (basicConfig.getSecret() != null && !basicConfig.getSecret().isEmpty()) {
            config.getRaw().put("secret", basicConfig.getSecret());
        }

        // 设置节点
        config.setProxies(allNodes);

        // 设置代理组
        config.setProxyGroups(proxyGroups);

        // 设置规则
        config.setRules(rules);

        return config;
    }

    private String toYaml(ClashConfig config) {
        // TODO: 实现 YAML 转换
        // 可以使用 Jackson YAML 或 SnakeYAML
        return "";
    }
}
```

- [ ] **Step 3: 提交代码**

```bash
git add module-processor/src/main/java/site/kael/clash/processor/service/ConfigGeneratorService.java
git add module-processor/src/main/java/site/kael/clash/processor/service/impl/ConfigGeneratorServiceImpl.java
git commit -m "feat(config-profile): 添加配置生成服务"
```

---

## Task 4: 创建 REST API

**Files:**
- Create: `module-web/src/main/java/site/kael/clash/web/controller/ConfigProfileController.java`

- [ ] **Step 1: 创建 ConfigProfileController**

```java
package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.processor.model.ConfigProfile;
import site.kael.clash.processor.repository.ConfigProfileRepository;
import site.kael.clash.processor.service.ConfigGeneratorService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/config")
public class ConfigProfileController {

    private static final Logger log = LoggerFactory.getLogger(ConfigProfileController.class);

    private final ConfigProfileRepository configProfileRepository;
    private final ConfigGeneratorService configGeneratorService;

    public ConfigProfileController(
            ConfigProfileRepository configProfileRepository,
            ConfigGeneratorService configGeneratorService) {
        this.configProfileRepository = configProfileRepository;
        this.configGeneratorService = configGeneratorService;
    }

    @GetMapping("/list")
    public ResponseEntity<List<ConfigProfile>> list() {
        return ResponseEntity.ok(configProfileRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfigProfile> getById(@PathVariable String id) {
        return configProfileRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ConfigProfile profile) {
        // 检查名称是否已存在
        if (configProfileRepository.existsByName(profile.getName())) {
            return ResponseEntity.status(409).body("{\"error\": \"配置名称已存在\"}");
        }

        profile.setId(UUID.randomUUID().toString());
        ConfigProfile created = configProfileRepository.save(profile);
        log.info("创建配置: id={}, name={}", created.getId(), created.getName());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody ConfigProfile profile) {
        ConfigProfile existing = configProfileRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        // 检查名称是否与其他配置冲突
        if (!existing.getName().equals(profile.getName()) &&
            configProfileRepository.existsByName(profile.getName())) {
            return ResponseEntity.status(409).body("{\"error\": \"配置名称已存在\"}");
        }

        profile.setId(id);
        profile.setCreatedAt(existing.getCreatedAt());
        ConfigProfile updated = configProfileRepository.save(profile);
        log.info("更新配置: id={}, name={}", updated.getId(), updated.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (configProfileRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        configProfileRepository.deleteById(id);
        log.info("删除配置: id={}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{name}/clash")
    public ResponseEntity<String> getClashConfig(@PathVariable String name) {
        try {
            String yaml = configGeneratorService.generateByName(name);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/yaml")
                    .body(yaml);
        } catch (Exception e) {
            log.error("生成配置失败: name={}", name, e);
            return ResponseEntity.notFound().build();
        }
    }
}
```

- [ ] **Step 2: 提交代码**

```bash
git add module-web/src/main/java/site/kael/clash/web/controller/ConfigProfileController.java
git commit -m "feat(config-profile): 添加配置管理 REST API"
```

---

## Task 5: 添加 Basic Auth 认证

**Files:**
- Create: `module-web/src/main/java/site/kael/clash/web/auth/BasicAuthInterceptor.java`
- Modify: `module-web/src/main/java/site/kael/clash/web/config/WebMvcConfig.java`

- [ ] **Step 1: 创建 BasicAuthInterceptor**

```java
package site.kael.clash.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import site.kael.clash.processor.model.ConfigProfile;
import site.kael.clash.processor.repository.ConfigProfileRepository;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class BasicAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(BasicAuthInterceptor.class);

    private final ConfigProfileRepository configProfileRepository;

    public BasicAuthInterceptor(ConfigProfileRepository configProfileRepository) {
        this.configProfileRepository = configProfileRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 只拦截 /api/config/{name}/clash 请求
        if (!requestURI.matches("/api/config/[^/]+/clash")) {
            return true;
        }

        // 从 URI 中提取配置名称
        String name = extractConfigName(requestURI);
        if (name == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的请求路径");
            return false;
        }

        // 查找配置
        ConfigProfile profile = configProfileRepository.findByName(name).orElse(null);
        if (profile == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "配置不存在");
            return false;
        }

        // 如果没有设置认证信息，直接放行
        if (profile.getAuthUsername() == null || profile.getAuthUsername().isEmpty()) {
            return true;
        }

        // 验证 Basic Auth
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Config Profile\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "需要认证");
            return false;
        }

        String credentials = new String(Base64.getDecoder().decode(authHeader.substring(6)), StandardCharsets.UTF_8);
        String[] parts = credentials.split(":", 2);
        if (parts.length != 2) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "认证格式错误");
            return false;
        }

        String username = parts[0];
        String password = parts[1];

        if (!username.equals(profile.getAuthUsername()) || !password.equals(profile.getAuthPassword())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "用户名或密码错误");
            return false;
        }

        return true;
    }

    private String extractConfigName(String requestURI) {
        // /api/config/{name}/clash -> {name}
        String[] parts = requestURI.split("/");
        if (parts.length >= 4) {
            return parts[3];
        }
        return null;
    }
}
```

- [ ] **Step 2: 注册拦截器**

在 `WebMvcConfig.java` 中添加：

```java
@Autowired
private BasicAuthInterceptor basicAuthInterceptor;

@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(basicAuthInterceptor)
            .addPathPatterns("/api/config/*/clash");
}
```

- [ ] **Step 3: 提交代码**

```bash
git add module-web/src/main/java/site/kael/clash/web/auth/BasicAuthInterceptor.java
git add module-web/src/main/java/site/kael/clash/web/config/WebMvcConfig.java
git commit -m "feat(config-profile): 添加 Basic Auth 认证"
```

---

## Task 6: 创建前端页面

**Files:**
- Create: `module-web/frontend/src/views/ConfigProfileListView.vue`
- Create: `module-web/frontend/src/views/ConfigProfileEditView.vue`
- Create: `module-web/frontend/src/api/config-profile.ts`

- [ ] **Step 1: 创建 API 调用文件**

```typescript
// module-web/frontend/src/api/config-profile.ts
import request from './index'

export interface ConfigProfile {
  id?: string
  name: string
  description?: string
  subscriptionIds: string[]
  proxyGroups: ProxyGroupConfig[]
  ruleGroups: RuleGroupRef[]
  basicConfig: ClashBasicConfig
  authUsername?: string
  authPassword?: string
  createdAt?: string
  updatedAt?: string
}

export interface ProxyGroupConfig {
  name: string
  type: 'select' | 'url-test' | 'fallback' | 'load-balance'
  nodeNames: string[]
  matchKeywords: string[]
  includeAll: boolean
  url?: string
  interval?: number
}

export interface RuleGroupRef {
  ruleGroupId: string
  priority: number
}

export interface ClashBasicConfig {
  mixedPort: number
  port: number
  socksPort: number
  redirPort: number
  allowLan: boolean
  mode: 'rule' | 'global' | 'direct'
  logLevel: string
  externalController: string
  secret?: string
}

export function listConfigProfiles() {
  return request.get<ConfigProfile[]>('/api/config/list')
}

export function getConfigProfile(id: string) {
  return request.get<ConfigProfile>(`/api/config/${id}`)
}

export function createConfigProfile(data: ConfigProfile) {
  return request.post<ConfigProfile>('/api/config', data)
}

export function updateConfigProfile(id: string, data: ConfigProfile) {
  return request.put<ConfigProfile>(`/api/config/${id}`, data)
}

export function deleteConfigProfile(id: string) {
  return request.delete(`/api/config/${id}`)
}
```

- [ ] **Step 2: 创建配置列表页面**

```vue
<!-- module-web/frontend/src/views/ConfigProfileListView.vue -->
<template>
  <div class="config-profile-list">
    <div class="header">
      <h2>配置管理</h2>
      <el-button type="primary" @click="handleCreate">新建配置</el-button>
    </div>

    <el-table :data="configProfiles" style="width: 100%">
      <el-table-column prop="name" label="配置名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="订阅源数量">
        <template #default="{ row }">
          {{ row.subscriptionIds?.length || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="代理组数量">
        <template #default="{ row }">
          {{ row.proxyGroups?.length || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="规则组数量">
        <template #default="{ row }">
          {{ row.ruleGroups?.length || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" @click="handleCopyLink(row)">复制链接</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listConfigProfiles, deleteConfigProfile, type ConfigProfile } from '@/api/config-profile'

const router = useRouter()
const configProfiles = ref<ConfigProfile[]>([])

onMounted(() => {
  loadConfigProfiles()
})

async function loadConfigProfiles() {
  try {
    const { data } = await listConfigProfiles()
    configProfiles.value = data
  } catch (error) {
    ElMessage.error('加载配置列表失败')
  }
}

function handleCreate() {
  router.push('/config-profiles/new')
}

function handleEdit(profile: ConfigProfile) {
  router.push(`/config-profiles/${profile.id}`)
}

function handleCopyLink(profile: ConfigProfile) {
  const url = `${window.location.origin}/api/config/${profile.name}/clash`
  navigator.clipboard.writeText(url)
  ElMessage.success('链接已复制')
}

async function handleDelete(profile: ConfigProfile) {
  try {
    await ElMessageBox.confirm('确定删除该配置？', '提示', { type: 'warning' })
    await deleteConfigProfile(profile.id!)
    ElMessage.success('删除成功')
    loadConfigProfiles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped>
.config-profile-list {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
```

- [ ] **Step 3: 创建配置编辑页面**

```vue
<!-- module-web/frontend/src/views/ConfigProfileEditView.vue -->
<template>
  <div class="config-profile-edit">
    <div class="header">
      <h2>{{ isEdit ? '编辑配置' : '新建配置' }}</h2>
    </div>

    <el-form :model="form" label-width="120px">
      <!-- 基本信息 -->
      <el-card class="section">
        <template #header>
          <span>基本信息</span>
        </template>
        <el-form-item label="配置名称" required>
          <el-input v-model="form.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
      </el-card>

      <!-- 订阅源选择 -->
      <el-card class="section">
        <template #header>
          <span>订阅源选择</span>
        </template>
        <el-form-item label="订阅源">
          <el-select v-model="form.subscriptionIds" multiple placeholder="请选择订阅源">
            <el-option
              v-for="sub in subscriptions"
              :key="sub.id"
              :label="sub.name"
              :value="sub.id"
            />
          </el-select>
        </el-form-item>
      </el-card>

      <!-- 代理组配置 -->
      <el-card class="section">
        <template #header>
          <div class="card-header">
            <span>代理组配置</span>
            <el-button size="small" @click="addProxyGroup">添加代理组</el-button>
          </div>
        </template>
        <div v-for="(group, index) in form.proxyGroups" :key="index" class="proxy-group-item">
          <el-row :gutter="10">
            <el-col :span="6">
              <el-input v-model="group.name" placeholder="代理组名称" />
            </el-col>
            <el-col :span="4">
              <el-select v-model="group.type" placeholder="类型">
                <el-option label="手动选择" value="select" />
                <el-option label="自动测试" value="url-test" />
                <el-option label="故障转移" value="fallback" />
                <el-option label="负载均衡" value="load-balance" />
              </el-select>
            </el-col>
            <el-col :span="10">
              <el-radio-group v-model="group.mode">
                <el-radio label="all">全部节点</el-radio>
                <el-radio label="keyword">关键词匹配</el-radio>
                <el-radio label="select">手动选择</el-radio>
              </el-radio-group>
            </el-col>
            <el-col :span="4">
              <el-button type="danger" size="small" @click="removeProxyGroup(index)">删除</el-button>
            </el-col>
          </el-row>
          <el-row v-if="group.mode === 'keyword'" :gutter="10" style="margin-top: 10px">
            <el-col :span="24">
              <el-select v-model="group.matchKeywords" multiple filterable allow-create placeholder="输入关键词后回车">
              </el-select>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 规则组配置 -->
      <el-card class="section">
        <template #header>
          <span>规则组配置</span>
        </template>
        <el-form-item label="规则组">
          <el-select v-model="form.ruleGroupIds" multiple placeholder="请选择规则组">
            <el-option
              v-for="rg in ruleGroups"
              :key="rg.id"
              :label="rg.name"
              :value="rg.id"
            />
          </el-select>
        </el-form-item>
      </el-card>

      <!-- 认证配置 -->
      <el-card class="section">
        <template #header>
          <span>认证配置</span>
        </template>
        <el-form-item label="用户名">
          <el-input v-model="form.authUsername" placeholder="留空则不启用认证" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.authPassword" type="password" placeholder="请输入密码" />
        </el-form-item>
      </el-card>

      <!-- 基础信息配置 -->
      <el-card class="section">
        <template #header>
          <div class="card-header">
            <span>基础信息配置</span>
            <el-button size="small" @click="showBasicConfig = !showBasicConfig">
              {{ showBasicConfig ? '收起' : '展开' }}
            </el-button>
          </div>
        </template>
        <template v-if="showBasicConfig">
          <el-form-item label="混合端口">
            <el-input-number v-model="form.basicConfig.mixedPort" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="HTTP 端口">
            <el-input-number v-model="form.basicConfig.port" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="SOCKS5 端口">
            <el-input-number v-model="form.basicConfig.socksPort" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="允许局域网">
            <el-switch v-model="form.basicConfig.allowLan" />
          </el-form-item>
          <el-form-item label="模式">
            <el-select v-model="form.basicConfig.mode">
              <el-option label="规则模式" value="rule" />
              <el-option label="全局模式" value="global" />
              <el-option label="直连模式" value="direct" />
            </el-select>
          </el-form-item>
          <el-form-item label="日志级别">
            <el-select v-model="form.basicConfig.logLevel">
              <el-option label="静默" value="silent" />
              <el-option label="错误" value="error" />
              <el-option label="警告" value="warning" />
              <el-option label="信息" value="info" />
              <el-option label="调试" value="debug" />
            </el-select>
          </el-form-item>
          <el-form-item label="外部控制">
            <el-input v-model="form.basicConfig.externalController" placeholder="127.0.0.1:9090" />
          </el-form-item>
          <el-form-item label="管理密钥">
            <el-input v-model="form.basicConfig.secret" placeholder="留空则无密钥" />
          </el-form-item>
        </template>
      </el-card>

      <el-form-item>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
        <el-button @click="handleCancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getConfigProfile, createConfigProfile, updateConfigProfile } from '@/api/config-profile'
import { listSubscriptions } from '@/api/subscription'
import { listRuleGroups } from '@/api/ruleGroup'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => route.params.id !== 'new')
const showBasicConfig = ref(false)

const form = ref({
  name: '',
  description: '',
  subscriptionIds: [] as string[],
  proxyGroups: [] as any[],
  ruleGroupIds: [] as string[],
  basicConfig: {
    mixedPort: 7890,
    port: 7891,
    socksPort: 7892,
    redirPort: 7893,
    allowLan: false,
    mode: 'rule',
    logLevel: 'info',
    externalController: '127.0.0.1:9090',
    secret: ''
  },
  authUsername: '',
  authPassword: ''
})

const subscriptions = ref<any[]>([])
const ruleGroups = ref<any[]>([])

onMounted(() => {
  loadData()
})

async function loadData() {
  try {
    const [subRes, rgRes] = await Promise.all([
      listSubscriptions(),
      listRuleGroups()
    ])
    subscriptions.value = subRes.data
    ruleGroups.value = rgRes.data

    if (isEdit.value) {
      const { data } = await getConfigProfile(route.params.id as string)
      form.value = {
        ...data,
        ruleGroupIds: data.ruleGroups?.map(rg => rg.ruleGroupId) || []
      }
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

function addProxyGroup() {
  form.value.proxyGroups.push({
    name: '',
    type: 'select',
    nodeNames: [],
    matchKeywords: [],
    includeAll: true,
    mode: 'all'
  })
}

function removeProxyGroup(index: number) {
  form.value.proxyGroups.splice(index, 1)
}

async function handleSubmit() {
  try {
    const submitData = {
      ...form.value,
      ruleGroups: form.value.ruleGroupIds.map((id, index) => ({
        ruleGroupId: id,
        priority: index
      }))
    }

    if (isEdit.value) {
      await updateConfigProfile(route.params.id as string, submitData)
      ElMessage.success('更新成功')
    } else {
      await createConfigProfile(submitData)
      ElMessage.success('创建成功')
    }
    router.push('/config-profiles')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

function handleCancel() {
  router.push('/config-profiles')
}
</script>

<style scoped>
.config-profile-edit {
  padding: 20px;
}
.header {
  margin-bottom: 20px;
}
.section {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.proxy-group-item {
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 4px;
}
</style>
```

- [ ] **Step 4: 添加路由配置**

在 `router/index.ts` 中添加：

```typescript
{
  path: '/config-profiles',
  name: 'ConfigProfileList',
  component: () => import('@/views/ConfigProfileListView.vue')
},
{
  path: '/config-profiles/:id',
  name: 'ConfigProfileEdit',
  component: () => import('@/views/ConfigProfileEditView.vue')
}
```

- [ ] **Step 5: 提交代码**

```bash
git add module-web/frontend/src/views/ConfigProfileListView.vue
git add module-web/frontend/src/views/ConfigProfileEditView.vue
git add module-web/frontend/src/api/config-profile.ts
git add module-web/frontend/src/router/index.ts
git commit -m "feat(config-profile): 添加前端配置管理页面"
```

---

## Task 7: 测试与验证

- [ ] **Step 1: 编译后端代码**

```bash
mvn clean compile -DskipTests
```

- [ ] **Step 2: 运行单元测试**

```bash
mvn test -Dtest=ConfigGeneratorServiceImplTest
```

- [ ] **Step 3: 启动应用**

```bash
mvn spring-boot:run -pl module-web
```

- [ ] **Step 4: 手动测试配置生成功能**

1. 访问 http://localhost:31192
2. 进入配置管理页面
3. 创建新配置，选择订阅源、配置代理组、选择规则组
4. 保存配置
5. 复制配置链接
6. 使用 Clash Verge 导入链接

- [ ] **Step 5: 测试 Basic Auth 认证**

1. 编辑配置，设置用户名和密码
2. 使用 curl 测试认证：
   ```bash
   # 不带认证
   curl http://localhost:31192/api/config/test/clash
   # 应该返回 401

   # 带认证
   curl -u username:password http://localhost:31192/api/config/test/clash
   # 应该返回 YAML 配置
   ```

- [ ] **Step 6: 提交最终代码**

```bash
git add .
git commit -m "feat(config-profile): 完成配置生成功能"
```

---

## 完成

实施计划已完成。两个执行选项：

**1. Subagent-Driven (recommended)** - 我将为每个任务分发一个新的子代理，任务之间进行审查，快速迭代

**2. Inline Execution** - 在当前会话中执行任务，批量执行并设置检查点

选择哪种方式？
