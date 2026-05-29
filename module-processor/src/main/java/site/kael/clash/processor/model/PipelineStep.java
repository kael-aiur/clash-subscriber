package site.kael.clash.processor.model;

import java.util.HashMap;
import java.util.Map;

public class PipelineStep {
    private String processor;
    private Map<String, Object> config = new HashMap<>();

    public String getProcessor() { return processor; }
    public void setProcessor(String processor) { this.processor = processor; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
}
