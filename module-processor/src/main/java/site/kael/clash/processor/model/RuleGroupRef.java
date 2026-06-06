package site.kael.clash.processor.model;

/**
 * 规则组引用：在配置组合中引用规则组，通过优先级决定规则的拼接顺序。
 */
public class RuleGroupRef {

    private String ruleGroupId;
    /** 优先级，数值越小越靠前 */
    private int priority;

    public RuleGroupRef() {}

    public RuleGroupRef(String ruleGroupId, int priority) {
        this.ruleGroupId = ruleGroupId;
        this.priority = priority;
    }

    public String getRuleGroupId() { return ruleGroupId; }
    public void setRuleGroupId(String ruleGroupId) { this.ruleGroupId = ruleGroupId; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}
