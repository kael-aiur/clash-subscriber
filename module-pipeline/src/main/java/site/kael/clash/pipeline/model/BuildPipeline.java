package site.kael.clash.pipeline.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BuildPipeline {
    private String id;
    private String name;
    private String configType;
    private String configProfileId;
    private String primarySubscriptionId;
    private List<String> additionalSubscriptionIds = new ArrayList<>();
    private String scriptName;
    private String targetInstanceId;
    private String cronExpression;
    private boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRunAt;
    private String lastRunStatus;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getConfigType() { return configType; }
    public void setConfigType(String configType) { this.configType = configType; }
    public String getConfigProfileId() { return configProfileId; }
    public void setConfigProfileId(String configProfileId) { this.configProfileId = configProfileId; }
    public String getPrimarySubscriptionId() { return primarySubscriptionId; }
    public void setPrimarySubscriptionId(String primarySubscriptionId) { this.primarySubscriptionId = primarySubscriptionId; }
    public List<String> getAdditionalSubscriptionIds() { return additionalSubscriptionIds; }
    public void setAdditionalSubscriptionIds(List<String> additionalSubscriptionIds) { this.additionalSubscriptionIds = additionalSubscriptionIds; }
    public String getScriptName() { return scriptName; }
    public void setScriptName(String scriptName) { this.scriptName = scriptName; }
    public String getTargetInstanceId() { return targetInstanceId; }
    public void setTargetInstanceId(String targetInstanceId) { this.targetInstanceId = targetInstanceId; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    public String getLastRunStatus() { return lastRunStatus; }
    public void setLastRunStatus(String lastRunStatus) { this.lastRunStatus = lastRunStatus; }

    /**
     * 验证 BuildPipeline 配置是否有效
     *
     * @throws IllegalArgumentException 如果验证失败
     */
    public void validate() {
        if (configType == null || configType.isBlank()) {
            throw new IllegalArgumentException("配置类型不能为空");
        }

        ConfigType type = ConfigType.fromValue(configType);

        switch (type) {
            case SUBSCRIPTION:
                if (primarySubscriptionId == null || primarySubscriptionId.isBlank()) {
                    throw new IllegalArgumentException("订阅源模式下，主订阅源ID不能为空");
                }
                break;
            case CONFIG_PROFILE:
                if (configProfileId == null || configProfileId.isBlank()) {
                    throw new IllegalArgumentException("配置组合模式下，配置组合ID不能为空");
                }
                break;
        }
    }
}
