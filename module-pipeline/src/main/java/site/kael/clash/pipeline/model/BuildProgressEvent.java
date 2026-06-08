package site.kael.clash.pipeline.model;

import java.time.LocalDateTime;

/**
 * 构建进度事件模型，用于 SSE 推送
 */
public class BuildProgressEvent {

    public enum EventType {
        STEP_STATUS,
        BUILD_COMPLETE,
        BUILD_ERROR
    }

    private EventType type;
    private Integer stepIndex;
    private String stepName;
    private String status;
    private Long timestamp;
    private Long duration;
    private Integer totalSteps;
    private Integer successSteps;
    private Integer failedSteps;
    private String errorMessage;

    public BuildProgressEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建步骤状态变更事件
     */
    public static BuildProgressEvent stepStatus(int stepIndex, String stepName, String status) {
        BuildProgressEvent event = new BuildProgressEvent();
        event.setType(EventType.STEP_STATUS);
        event.setStepIndex(stepIndex);
        event.setStepName(stepName);
        event.setStatus(status);
        return event;
    }

    /**
     * 创建构建完成事件
     */
    public static BuildProgressEvent buildComplete(String status, long duration,
            int totalSteps, int successSteps, int failedSteps) {
        BuildProgressEvent event = new BuildProgressEvent();
        event.setType(EventType.BUILD_COMPLETE);
        event.setStatus(status);
        event.setDuration(duration);
        event.setTotalSteps(totalSteps);
        event.setSuccessSteps(successSteps);
        event.setFailedSteps(failedSteps);
        return event;
    }

    /**
     * 创建构建错误事件
     */
    public static BuildProgressEvent buildError(String message) {
        BuildProgressEvent event = new BuildProgressEvent();
        event.setType(EventType.BUILD_ERROR);
        event.setErrorMessage(message);
        return event;
    }

    // Getters and Setters

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Integer getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(Integer stepIndex) {
        this.stepIndex = stepIndex;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Integer getSuccessSteps() {
        return successSteps;
    }

    public void setSuccessSteps(Integer successSteps) {
        this.successSteps = successSteps;
    }

    public Integer getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(Integer failedSteps) {
        this.failedSteps = failedSteps;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
