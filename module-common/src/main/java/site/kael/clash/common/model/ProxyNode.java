package site.kael.clash.common.model;

import java.util.HashMap;
import java.util.Map;

public class ProxyNode {
    private String name;
    private String type;
    private String server;
    private int port;
    private Map<String, Object> extra = new HashMap<>();

    public ProxyNode() {}

    public ProxyNode(String name, String type, String server, int port) {
        this.name = name;
        this.type = type;
        this.server = server;
        this.port = port;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }
}
