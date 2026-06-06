package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.pipeline.model.BuildPipeline;
import site.kael.clash.pipeline.model.BuildRecord;
import site.kael.clash.pipeline.service.BuildPipelineService;

import java.util.List;

@RestController
@RequestMapping("/api/build-pipelines")
public class BuildPipelineController {

    private static final Logger log = LoggerFactory.getLogger(BuildPipelineController.class);

    private final BuildPipelineService buildPipelineService;

    public BuildPipelineController(BuildPipelineService buildPipelineService) {
        this.buildPipelineService = buildPipelineService;
    }

    @GetMapping
    public ResponseEntity<List<BuildPipeline>> findAll() {
        return ResponseEntity.ok(buildPipelineService.findAll());
    }

    @PostMapping
    public ResponseEntity<BuildPipeline> create(@RequestBody BuildPipeline pipeline) {
        log.info("创建构建流程: name={}, configType={}", pipeline.getName(), pipeline.getConfigType());
        return ResponseEntity.ok(buildPipelineService.create(pipeline));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildPipeline> findById(@PathVariable String id) {
        return ResponseEntity.ok(buildPipelineService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BuildPipeline> update(@PathVariable String id, @RequestBody BuildPipeline pipeline) {
        log.info("更新构建流程: id={}, configType={}", id, pipeline.getConfigType());
        pipeline.setId(id);
        return ResponseEntity.ok(buildPipelineService.update(pipeline));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        log.info("删除构建流程: id={}", id);
        buildPipelineService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<BuildRecord> execute(@PathVariable String id) {
        log.info("手动触发构建流程: id={}", id);
        return ResponseEntity.ok(buildPipelineService.execute(id));
    }

    @GetMapping("/{id}/records")
    public ResponseEntity<List<BuildRecord>> findRecords(@PathVariable String id) {
        return ResponseEntity.ok(buildPipelineService.findRecords(id));
    }
}
