package site.kael.clash.processor.model;

import java.util.ArrayList;
import java.util.List;

public class PipelineConfig {
    private String id;
    private String name;
    private List<PipelineStep> steps = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<PipelineStep> getSteps() { return steps; }
    public void setSteps(List<PipelineStep> steps) { this.steps = steps; }
}
