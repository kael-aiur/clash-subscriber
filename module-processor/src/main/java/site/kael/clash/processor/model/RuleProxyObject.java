package site.kael.clash.processor.model;

/**
 * 规则组中的代理对象，作为规则引用的抽象占位符。
 * 规则中的代理名在提取时被替换为 {{id}} 格式的占位符。
 */
public class RuleProxyObject {

    private String id;
    private String sourceName;
    private String description;

    public RuleProxyObject() {}

    public RuleProxyObject(String id, String sourceName) {
        this.id = id;
        this.sourceName = sourceName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
