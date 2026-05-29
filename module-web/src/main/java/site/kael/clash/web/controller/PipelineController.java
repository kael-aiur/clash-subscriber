package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.processor.model.PipelineConfig;
import site.kael.clash.processor.repository.PipelineConfigRepository;
import site.kael.clash.processor.service.PipelineService;

import java.util.List;
import java.util.UUID;

/**
 * Pipeline 配置管理 REST 控制器
 * <p>
 * 提供 Pipeline 配置的 CRUD 操作及执行功能。
 */
@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    private final PipelineConfigRepository pipelineConfigRepository;
    private final PipelineService pipelineService;

    public PipelineController(PipelineConfigRepository pipelineConfigRepository,
                              PipelineService pipelineService) {
        this.pipelineConfigRepository = pipelineConfigRepository;
        this.pipelineService = pipelineService;
    }

    /**
     * 查询所有 Pipeline 配置
     */
    @GetMapping
    public ResponseEntity<List<PipelineConfig>> findAll() {
        log.debug("查询所有 Pipeline 配置");
        return ResponseEntity.ok(pipelineConfigRepository.findAll());
    }

    /**
     * 创建 Pipeline 配置（若无 ID 则自动生成）
     */
    @PostMapping
    public ResponseEntity<PipelineConfig> create(@RequestBody PipelineConfig pipelineConfig) {
        log.info("创建 Pipeline 配置: name={}", pipelineConfig.getName());
        if (pipelineConfig.getId() == null || pipelineConfig.getId().isBlank()) {
            pipelineConfig.setId(UUID.randomUUID().toString());
        }
        PipelineConfig saved = pipelineConfigRepository.save(pipelineConfig);
        return ResponseEntity.ok(saved);
    }

    /**
     * 根据 ID 查询 Pipeline 配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<PipelineConfig> findById(@PathVariable String id) {
        log.debug("查询 Pipeline 配置: id={}", id);
        return pipelineConfigRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new BusinessException(404, "Pipeline 配置不存在: " + id));
    }

    /**
     * 更新 Pipeline 配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<PipelineConfig> update(@PathVariable String id,
                                                 @RequestBody PipelineConfig pipelineConfig) {
        log.info("更新 Pipeline 配置: id={}", id);
        pipelineConfig.setId(id);
        PipelineConfig saved = pipelineConfigRepository.save(pipelineConfig);
        return ResponseEntity.ok(saved);
    }

    /**
     * 删除 Pipeline 配置
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        log.info("删除 Pipeline 配置: id={}", id);
        pipelineConfigRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 执行 Pipeline，对输入的 ClashConfig 进行处理
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<ClashConfig> execute(@PathVariable String id,
                                               @RequestBody ClashConfig input) {
        log.info("执行 Pipeline: id={}", id);
        PipelineConfig pipeline = pipelineConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Pipeline 配置不存在: " + id));
        ClashConfig result = pipelineService.execute(pipeline, input);
        return ResponseEntity.ok(result);
    }
}
