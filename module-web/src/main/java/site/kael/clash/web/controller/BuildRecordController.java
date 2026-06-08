package site.kael.clash.web.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.kael.clash.pipeline.model.BuildRecord;
import site.kael.clash.pipeline.service.BuildPipelineService;
import site.kael.clash.pipeline.service.BuildRecordService;

@RestController
@RequestMapping("/api/build-records")
public class BuildRecordController {

    private final BuildPipelineService buildPipelineService;
    private final BuildRecordService buildRecordService;

    public BuildRecordController(BuildPipelineService buildPipelineService,
            BuildRecordService buildRecordService) {
        this.buildPipelineService = buildPipelineService;
        this.buildRecordService = buildRecordService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildRecord> findById(@PathVariable String id) {
        return ResponseEntity.ok(buildPipelineService.findRecordById(id));
    }

    /**
     * 订阅构建进度 SSE 端点
     */
    @GetMapping(value = "/{id}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter progress(@PathVariable String id) {
        return buildRecordService.subscribeProgress(id);
    }
}
