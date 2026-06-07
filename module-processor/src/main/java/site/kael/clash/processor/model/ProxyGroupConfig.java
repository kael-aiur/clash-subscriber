package site.kael.clash.processor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 代理组配置：定义 Clash 代理组的名称、类型和节点筛选方式。
 */
public class ProxyGroupConfig {

    private String name;
    /** 代理组类型：select, url-test, fallback, load-balance */
    private String type;
    /** 直接指定的节点名称列表 */
    private List<String> nodeNames = new ArrayList<>();
    /** 按关键词匹配节点 */
    private List<String> matchKeywords = new ArrayList<>();
    /** 排除包含指定关键词的节点（如流量信息、到期时间等） */
    private List<String> excludeKeywords = new ArrayList<>();
    /** 是否包含所有节点 */
    private boolean includeAll;
    /** 健康检查 URL */
    private String url;
    /** 健康检查间隔（秒） */
    private int interval;

    public ProxyGroupConfig() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<String> getNodeNames() { return nodeNames; }
    public void setNodeNames(List<String> nodeNames) { this.nodeNames = nodeNames; }
    public List<String> getMatchKeywords() { return matchKeywords; }
    public void setMatchKeywords(List<String> matchKeywords) { this.matchKeywords = matchKeywords; }
    public List<String> getExcludeKeywords() { return excludeKeywords; }
    public void setExcludeKeywords(List<String> excludeKeywords) { this.excludeKeywords = excludeKeywords; }
    public boolean isIncludeAll() { return includeAll; }
    public void setIncludeAll(boolean includeAll) { this.includeAll = includeAll; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }
}
