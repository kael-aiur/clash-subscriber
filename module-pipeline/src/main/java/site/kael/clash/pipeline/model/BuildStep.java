package site.kael.clash.pipeline.model;

import java.time.LocalDateTime;

public class BuildStep {
    private String name;
    private String status; // SUCCESS, FAILED, SKIPPED
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Object input;
    private Object output;
    private String errorMessage;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Object getInput() { return input; }
    public void setInput(Object input) { this.input = input; }

    public Object getOutput() { return output; }
    public void setOutput(Object output) { this.output = output; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
