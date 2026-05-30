package site.kael.clash.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.pipeline.model.BuildRecord;
import site.kael.clash.pipeline.service.BuildPipelineService;

@RestController
@RequestMapping("/api/build-records")
public class BuildRecordController {

    private final BuildPipelineService buildPipelineService;

    public BuildRecordController(BuildPipelineService buildPipelineService) {
        this.buildPipelineService = buildPipelineService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildRecord> findById(@PathVariable String id) {
        return ResponseEntity.ok(buildPipelineService.findRecordById(id));
    }
}
