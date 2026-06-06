package site.kael.clash.processor.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则组：从订阅提取或手动创建的规则集合。
 * 规则中引用的代理名被抽象为代理对象占位符（{{id}} 格式）。
 */
public class RuleGroup {

    private String id;
    private String name;
    private String description;
    /** 来源订阅 ID，手动创建时为 null */
    private String sourceSubscriptionId;
    /** 规则列表，代理名以 {{proxyObjectId}} 占位符表示 */
    private List<String> rules;
    /** 代理对象列表 */
    private List<RuleProxyObject> proxyObjects;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RuleGroup() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceSubscriptionId() { return sourceSubscriptionId; }
    public void setSourceSubscriptionId(String sourceSubscriptionId) { this.sourceSubscriptionId = sourceSubscriptionId; }
    public List<String> getRules() { return rules; }
    public void setRules(List<String> rules) { this.rules = rules; }
    public List<RuleProxyObject> getProxyObjects() { return proxyObjects; }
    public void setProxyObjects(List<RuleProxyObject> proxyObjects) { this.proxyObjects = proxyObjects; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
