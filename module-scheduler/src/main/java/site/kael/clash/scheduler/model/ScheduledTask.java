package site.kael.clash.scheduler.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduledTask {
    private String id;
    private String name;
    private String pipelineId;
    private List<String> targetInstances = new ArrayList<>();
    private String cronExpression;
    private boolean enabled = true;
    private LocalDateTime lastRunAt;
    private TaskStatus lastRunStatus;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPipelineId() { return pipelineId; }
    public void setPipelineId(String pipelineId) { this.pipelineId = pipelineId; }
    public List<String> getTargetInstances() { return targetInstances; }
    public void setTargetInstances(List<String> targetInstances) { this.targetInstances = targetInstances; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    public TaskStatus getLastRunStatus() { return lastRunStatus; }
    public void setLastRunStatus(TaskStatus lastRunStatus) { this.lastRunStatus = lastRunStatus; }
}
