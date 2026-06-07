package site.kael.clash.processor.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 规则组引用：在配置组合中引用规则组，通过优先级决定规则的拼接顺序。
 * 支持代理对象映射，将规则组中的 {{id}} 占位符映射到实际的代理组名称。
 */
public class RuleGroupRef {

    private String ruleGroupId;
    /** 优先级，数值越小越靠前 */
    private int priority;
    /** 代理对象映射：key 为代理对象 ID，value 为实际的代理组名称 */
    private Map<String, String> proxyObjectMappings = new HashMap<>();

    public RuleGroupRef() {}

    public RuleGroupRef(String ruleGroupId, int priority) {
        this.ruleGroupId = ruleGroupId;
        this.priority = priority;
    }

    public String getRuleGroupId() { return ruleGroupId; }
    public void setRuleGroupId(String ruleGroupId) { this.ruleGroupId = ruleGroupId; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public Map<String, String> getProxyObjectMappings() { return proxyObjectMappings; }
    public void setProxyObjectMappings(Map<String, String> proxyObjectMappings) { this.proxyObjectMappings = proxyObjectMappings; }
}
