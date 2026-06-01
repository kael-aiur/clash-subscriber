package site.kael.clash.pipeline.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BuildRecord {
    private String id;
    private String buildPipelineId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
    private String errorMessage;
    private List<String> logs = new ArrayList<>();
    private List<BuildStep> steps = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuildPipelineId() { return buildPipelineId; }
    public void setBuildPipelineId(String buildPipelineId) { this.buildPipelineId = buildPipelineId; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public List<String> getLogs() { return logs; }
    public void setLogs(List<String> logs) { this.logs = logs; }
    public List<BuildStep> getSteps() { return steps; }
    public void setSteps(List<BuildStep> steps) { this.steps = steps; }
}
