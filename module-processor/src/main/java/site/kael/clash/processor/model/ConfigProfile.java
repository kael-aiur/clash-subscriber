package site.kael.clash.processor.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置组合：将多个订阅源、代理组、规则组和基础配置组合为一个可推送的完整 Clash 配置。
 */
public class ConfigProfile {

    private String id;
    private String name;
    private String description;
    /** 关联的订阅源 ID 列表 */
    private List<String> subscriptionIds = new ArrayList<>();
    /** 代理组配置列表 */
    private List<ProxyGroupConfig> proxyGroups = new ArrayList<>();
    /** 规则组引用列表（按优先级排序） */
    private List<RuleGroupRef> ruleGroups = new ArrayList<>();
    /** Clash 基础配置 */
    private ClashBasicConfig basicConfig = new ClashBasicConfig();
    /** 认证用户名（用于外部控制器访问） */
    private String authUsername;
    /** 认证密码 */
    private String authPassword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConfigProfile() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

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
