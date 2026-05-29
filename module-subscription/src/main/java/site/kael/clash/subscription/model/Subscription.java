package site.kael.clash.subscription.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Subscription {
    private String id;
    private String name;
    private String url;
    private String userAgent;
    private Map<String, String> headers = new HashMap<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastFetchedAt;

    public Subscription() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(LocalDateTime lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }
}
