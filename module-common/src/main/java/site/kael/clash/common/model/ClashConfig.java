package site.kael.clash.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClashConfig {
    private String name;
    private Map<String, Object> raw = new HashMap<>();
    private List<ProxyNode> proxies = new ArrayList<>();
    private Map<String, Object> proxyGroups = new HashMap<>();
    private List<Object> rules = new ArrayList<>();

    public ClashConfig() {}

    public ClashConfig(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, Object> getRaw() { return raw; }
    public void setRaw(Map<String, Object> raw) { this.raw = raw; }
    public List<ProxyNode> getProxies() { return proxies; }
    public void setProxies(List<ProxyNode> proxies) { this.proxies = proxies; }
    public Map<String, Object> getProxyGroups() { return proxyGroups; }
    public void setProxyGroups(Map<String, Object> proxyGroups) { this.proxyGroups = proxyGroups; }
    public List<Object> getRules() { return rules; }
    public void setRules(List<Object> rules) { this.rules = rules; }
}
