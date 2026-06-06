package site.kael.clash.processor.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.common.util.YamlUtil;
import site.kael.clash.processor.api.ProcessingContext;
import site.kael.clash.processor.builtin.NodeMergeProcessor;
import site.kael.clash.processor.builtin.ProxyGroupProcessor;
import site.kael.clash.processor.model.*;
import site.kael.clash.processor.repository.ConfigProfileRepository;
import site.kael.clash.processor.repository.RuleGroupRepository;
import site.kael.clash.processor.service.ConfigGeneratorService;
import site.kael.clash.subscription.service.SubscriptionService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 配置生成服务实现：根据配置组合，合并订阅源节点、构建代理组和规则，生成完整 Clash 配置。
 */
@Service
public class ConfigGeneratorServiceImpl implements ConfigGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ConfigGeneratorServiceImpl.class);

    private final ConfigProfileRepository configProfileRepository;
    private final SubscriptionService subscriptionService;
    private final RuleGroupRepository ruleGroupRepository;
    private final NodeMergeProcessor nodeMergeProcessor;
    private final ProxyGroupProcessor proxyGroupProcessor;

    public ConfigGeneratorServiceImpl(
            ConfigProfileRepository configProfileRepository,
            SubscriptionService subscriptionService,
            RuleGroupRepository ruleGroupRepository,
            NodeMergeProcessor nodeMergeProcessor,
            ProxyGroupProcessor proxyGroupProcessor) {
        this.configProfileRepository = configProfileRepository;
        this.subscriptionService = subscriptionService;
        this.ruleGroupRepository = ruleGroupRepository;
        this.nodeMergeProcessor = nodeMergeProcessor;
        this.proxyGroupProcessor = proxyGroupProcessor;
    }

    @Override
    public String generate(ConfigProfile profile) {
        log.info("生成配置: name={}", profile.getName());

        // 1. 获取订阅源配置
        List<ClashConfig> subscriptionConfigs = fetchSubscriptions(profile.getSubscriptionIds());

        // 2. 使用 NodeMergeProcessor 合并节点
        ProcessingContext mergeContext = new ProcessingContext();
        mergeContext.setVariable("mergeConfigs", subscriptionConfigs);
        ClashConfig mergedConfig = nodeMergeProcessor.process(new ClashConfig(profile.getName()), mergeContext);

        // 3. 解析代理组配置并使用 ProxyGroupProcessor 构建代理组
        List<Map<String, Object>> resolvedGroups = resolveProxyGroups(profile.getProxyGroups(), mergedConfig.getProxies());
        ProcessingContext groupContext = new ProcessingContext();
        Map<String, Object> proxyGroupConfig = new HashMap<>();
        proxyGroupConfig.put("groups", resolvedGroups);
        groupContext.setVariable("proxyGroupConfig", proxyGroupConfig);
        ClashConfig finalConfig = proxyGroupProcessor.process(mergedConfig, groupContext);

        // 4. 构建规则
        List<String> rules = buildRules(profile.getRuleGroups());

        // 5. 构建完整配置并转为 YAML
        return toYaml(profile, finalConfig.getProxies(), finalConfig.getProxyGroups(), rules);
    }

    @Override
    public String generateByName(String name) {
        ConfigProfile profile = configProfileRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("配置不存在: " + name));
        return generate(profile);
    }

    /**
     * 获取多个订阅源的配置
     */
    private List<ClashConfig> fetchSubscriptions(List<String> subscriptionIds) {
        List<ClashConfig> configs = new ArrayList<>();
        for (String subscriptionId : subscriptionIds) {
            try {
                ClashConfig config = subscriptionService.fetch(subscriptionId);
                if (config != null) {
                    configs.add(config);
                    log.info("获取订阅源配置: subscriptionId={}, nodes={}", subscriptionId,
                            config.getProxies() != null ? config.getProxies().size() : 0);
                }
            } catch (Exception e) {
                log.error("获取订阅源失败: subscriptionId={}", subscriptionId, e);
            }
        }
        return configs;
    }

    /**
     * 解析代理组配置为 ProxyGroupProcessor 所需的格式。
     * 将 includeAll、nodeNames、matchKeywords 等配置解析为实际的节点名列表。
     */
    private List<Map<String, Object>> resolveProxyGroups(List<ProxyGroupConfig> groupConfigs, List<ProxyNode> allNodes) {
        List<Map<String, Object>> groups = new ArrayList<>();

        for (ProxyGroupConfig groupConfig : groupConfigs) {
            List<String> proxies = new ArrayList<>();

            if (groupConfig.isIncludeAll()) {
                // 包含所有节点
                proxies = allNodes.stream()
                        .map(ProxyNode::getName)
                        .collect(Collectors.toList());
            } else if (groupConfig.getNodeNames() != null && !groupConfig.getNodeNames().isEmpty()) {
                // 直接选择指定节点
                proxies = new ArrayList<>(groupConfig.getNodeNames());
            } else if (groupConfig.getMatchKeywords() != null && !groupConfig.getMatchKeywords().isEmpty()) {
                // 按关键词匹配节点
                proxies = allNodes.stream()
                        .filter(node -> matchKeywords(node.getName(), groupConfig.getMatchKeywords()))
                        .map(ProxyNode::getName)
                        .collect(Collectors.toList());
            }

            Map<String, Object> groupData = new LinkedHashMap<>();
            groupData.put("name", groupConfig.getName());
            groupData.put("type", groupConfig.getType());
            groupData.put("proxies", proxies);

            if (groupConfig.getUrl() != null) {
                groupData.put("url", groupConfig.getUrl());
            }
            if (groupConfig.getInterval() > 0) {
                groupData.put("interval", groupConfig.getInterval());
            }

            groups.add(groupData);
        }

        return groups;
    }

    /**
     * 检查节点名是否包含任意一个关键词（不区分大小写）
     */
    private boolean matchKeywords(String nodeName, List<String> keywords) {
        String lowerName = nodeName.toLowerCase();
        return keywords.stream()
                .anyMatch(keyword -> lowerName.contains(keyword.toLowerCase()));
    }

    /**
     * 按优先级排序后，从规则组中收集所有规则。
     */
    private List<String> buildRules(List<RuleGroupRef> ruleGroupRefs) {
        List<String> rules = new ArrayList<>();

        // 按优先级排序（数值越小越靠前）
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

    /**
     * 将配置组合转换为 Clash YAML 配置字符串。
     * 构建符合 Clash 配置格式的 Map 结构后序列化为 YAML。
     */
    private String toYaml(ConfigProfile profile, List<ProxyNode> allNodes,
                          Map<String, Object> proxyGroups, List<String> rules) {
        Map<String, Object> yamlMap = new LinkedHashMap<>();

        // 基础配置
        ClashBasicConfig basicConfig = profile.getBasicConfig();
        yamlMap.put("mixed-port", basicConfig.getMixedPort());
        yamlMap.put("port", basicConfig.getPort());
        yamlMap.put("socks-port", basicConfig.getSocksPort());
        yamlMap.put("redir-port", basicConfig.getRedirPort());
        yamlMap.put("allow-lan", basicConfig.isAllowLan());
        yamlMap.put("mode", basicConfig.getMode());
        yamlMap.put("log-level", basicConfig.getLogLevel());
        yamlMap.put("external-controller", basicConfig.getExternalController());
        if (basicConfig.getSecret() != null && !basicConfig.getSecret().isEmpty()) {
            yamlMap.put("secret", basicConfig.getSecret());
        }

        // 代理节点
        List<Map<String, Object>> proxyList = new ArrayList<>();
        for (ProxyNode node : allNodes) {
            Map<String, Object> proxyMap = new LinkedHashMap<>();
            proxyMap.put("name", node.getName());
            proxyMap.put("type", node.getType());
            proxyMap.put("server", node.getServer());
            proxyMap.put("port", node.getPort());
            proxyMap.putAll(node.getExtra());
            proxyList.add(proxyMap);
        }
        yamlMap.put("proxies", proxyList);

        // 代理组（将 Map 转为 Clash YAML 要求的 List 格式）
        List<Map<String, Object>> proxyGroupList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : proxyGroups.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> groupData = new LinkedHashMap<>((Map<String, Object>) entry.getValue());
                groupData.put("name", entry.getKey());
                proxyGroupList.add(groupData);
            }
        }
        yamlMap.put("proxy-groups", proxyGroupList);

        // 规则
        yamlMap.put("rules", rules);

        return YamlUtil.dump(yamlMap);
    }
}
