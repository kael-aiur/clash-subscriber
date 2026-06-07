package site.kael.clash.processor.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.util.IdGenerator;
import site.kael.clash.processor.model.RuleGroup;
import site.kael.clash.processor.model.RuleProxyObject;
import site.kael.clash.processor.repository.RuleGroupRepository;
import site.kael.clash.processor.service.RuleGroupService;
import site.kael.clash.subscription.model.Subscription;
import site.kael.clash.subscription.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 规则组服务实现
 */
@Service
public class RuleGroupServiceImpl implements RuleGroupService {

    private static final Logger log = LoggerFactory.getLogger(RuleGroupServiceImpl.class);

    /** Clash 内置代理名，提取时自动排除 */
    private static final Set<String> BUILT_IN_PROXIES = Set.of(
            "DIRECT", "REJECT", "PASS", "GLOBAL"
    );

    private final RuleGroupRepository ruleGroupRepository;
    private final SubscriptionService subscriptionService;

    public RuleGroupServiceImpl(RuleGroupRepository ruleGroupRepository,
                                 SubscriptionService subscriptionService) {
        this.ruleGroupRepository = ruleGroupRepository;
        this.subscriptionService = subscriptionService;
    }

    @Override
    public List<RuleGroup> findAll() {
        return ruleGroupRepository.findAll();
    }

    @Override
    public Optional<RuleGroup> findById(String id) {
        return ruleGroupRepository.findById(id);
    }

    @Override
    public Optional<RuleGroup> findBySourceSubscriptionId(String subscriptionId) {
        return ruleGroupRepository.findBySourceSubscriptionId(subscriptionId);
    }

    @Override
    public RuleGroup create(RuleGroup ruleGroup) {
        if (ruleGroup.getId() == null) {
            ruleGroup.setId(IdGenerator.generate());
        }
        // 为没有 ID 的代理对象自动生成 ID
        if (ruleGroup.getProxyObjects() != null) {
            for (RuleProxyObject proxy : ruleGroup.getProxyObjects()) {
                if (proxy.getId() == null || proxy.getId().isBlank()) {
                    proxy.setId(IdGenerator.generate());
                }
            }
        }
        ruleGroup.setCreatedAt(LocalDateTime.now());
        ruleGroup.setUpdatedAt(LocalDateTime.now());
        return ruleGroupRepository.save(ruleGroup);
    }

    @Override
    public RuleGroup update(RuleGroup ruleGroup) {
        // 为没有 ID 的代理对象自动生成 ID
        if (ruleGroup.getProxyObjects() != null) {
            for (RuleProxyObject proxy : ruleGroup.getProxyObjects()) {
                if (proxy.getId() == null || proxy.getId().isBlank()) {
                    proxy.setId(IdGenerator.generate());
                }
            }
        }
        ruleGroup.setUpdatedAt(LocalDateTime.now());
        return ruleGroupRepository.save(ruleGroup);
    }

    @Override
    public void deleteById(String id) {
        ruleGroupRepository.deleteById(id);
    }

    @Override
    public RuleGroup extractFromSubscription(String subscriptionId) {
        // 获取订阅信息
        Subscription subscription = subscriptionService.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("订阅不存在: " + subscriptionId));

        // 获取订阅配置
        ClashConfig config = subscriptionService.fetch(subscriptionId);
        List<Object> rules = config.getRules();
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("订阅中没有可提取的规则: " + subscription.getName());
        }

        // 扫描规则中引用的所有代理名
        Set<String> referencedProxyNames = new LinkedHashSet<>();
        for (Object rule : rules) {
            String proxyName = extractProxyNameFromRule(rule.toString());
            if (proxyName != null && !BUILT_IN_PROXIES.contains(proxyName)) {
                referencedProxyNames.add(proxyName);
            }
        }

        if (referencedProxyNames.isEmpty()) {
            throw new IllegalArgumentException("订阅规则中没有可提取的代理名: " + subscription.getName());
        }

        // 为每个代理名生成代理对象
        Map<String, String> proxyNameToIdMap = new LinkedHashMap<>();
        List<RuleProxyObject> proxyObjects = new ArrayList<>();
        for (String proxyName : referencedProxyNames) {
            String id = IdGenerator.generate();
            proxyNameToIdMap.put(proxyName, id);
            proxyObjects.add(new RuleProxyObject(id, proxyName));
        }

        // 替换规则中的代理名为占位符
        List<String> processedRules = new ArrayList<>();
        for (Object rule : rules) {
            String ruleStr = rule.toString();
            String replaced = replaceProxyNamesWithPlaceholders(ruleStr, proxyNameToIdMap);
            processedRules.add(replaced);
        }

        // 查找已有规则组（覆盖更新）或创建新的
        RuleGroup ruleGroup = ruleGroupRepository.findBySourceSubscriptionId(subscriptionId)
                .orElseGet(RuleGroup::new);

        ruleGroup.setName(subscription.getName() + "的规则组");
        ruleGroup.setSourceSubscriptionId(subscriptionId);
        ruleGroup.setRules(processedRules);
        ruleGroup.setProxyObjects(proxyObjects);
        ruleGroup.setUpdatedAt(LocalDateTime.now());
        if (ruleGroup.getCreatedAt() == null) {
            ruleGroup.setCreatedAt(LocalDateTime.now());
        }
        if (ruleGroup.getId() == null) {
            ruleGroup.setId(IdGenerator.generate());
        }

        RuleGroup saved = ruleGroupRepository.save(ruleGroup);
        log.info("从订阅 [{}] 提取规则组完成: {} 条规则, {} 个代理对象",
                subscription.getName(), processedRules.size(), proxyObjects.size());
        return saved;
    }

    /**
     * 从规则字符串中提取代理名（第 3 个字段，或 MATCH 等无参数规则的第 2 个字段）
     */
    String extractProxyNameFromRule(String rule) {
        String[] parts = rule.split(",", -1);
        if (parts.length >= 3) {
            return parts[2].trim();
        } else if (parts.length == 2) {
            // MATCH,ProxyName 或 FINAL,ProxyName
            String type = parts[0].trim();
            if ("MATCH".equals(type) || "FINAL".equals(type)) {
                return parts[1].trim();
            }
        }
        return null;
    }

    /**
     * 将规则字符串中的代理名替换为 {{id}} 占位符
     */
    private String replaceProxyNamesWithPlaceholders(String rule, Map<String, String> proxyNameToIdMap) {
        String[] parts = rule.split(",", -1);
        if (parts.length >= 3) {
            String proxyName = parts[2].trim();
            String id = proxyNameToIdMap.get(proxyName);
            if (id != null) {
                parts[2] = "{{" + id + "}}";
                return String.join(",", parts);
            }
        } else if (parts.length == 2) {
            String type = parts[0].trim();
            if ("MATCH".equals(type) || "FINAL".equals(type)) {
                String proxyName = parts[1].trim();
                String id = proxyNameToIdMap.get(proxyName);
                if (id != null) {
                    parts[1] = "{{" + id + "}}";
                    return String.join(",", parts);
                }
            }
        }
        return rule;
    }
}
