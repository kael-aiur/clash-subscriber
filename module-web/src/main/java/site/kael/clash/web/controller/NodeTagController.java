package site.kael.clash.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.kael.clash.web.model.NodeTag;
import site.kael.clash.web.service.NodeTagService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/node-tags")
public class NodeTagController {

    private static final Logger log = LoggerFactory.getLogger(NodeTagController.class);

    private final NodeTagService nodeTagService;

    public NodeTagController(NodeTagService nodeTagService) {
        this.nodeTagService = nodeTagService;
    }

    @GetMapping
    public ResponseEntity<List<NodeTag>> findAll() {
        log.debug("查询所有节点标签");
        return ResponseEntity.ok(nodeTagService.findAll());
    }

    @PostMapping
    public ResponseEntity<NodeTag> create(@RequestBody NodeTag nodeTag) {
        log.info("创建节点标签: name={}", nodeTag.getName());
        NodeTag created = nodeTagService.create(nodeTag);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NodeTag> findById(@PathVariable String id) {
        log.debug("查询节点标签: id={}", id);
        return ResponseEntity.ok(nodeTagService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NodeTag> update(@PathVariable String id, @RequestBody NodeTag nodeTag) {
        log.info("更新节点标签: id={}", id);
        nodeTag.setId(id);
        NodeTag updated = nodeTagService.update(nodeTag);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        log.info("删除节点标签: id={}", id);
        nodeTagService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 导出全部标签为 JSON 文件
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() throws Exception {
        log.info("导出节点标签");
        List<NodeTag> tags = nodeTagService.exportAll();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        byte[] data = mapper.writeValueAsBytes(tags);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=node-tags.json")
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(data.length)
                .body(data);
    }

    /**
     * 从 JSON 文件导入标签
     */
    @PostMapping("/import")
    public ResponseEntity<ImportResult> importTags(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("导入节点标签: filename={}, size={}", file.getOriginalFilename(), file.getSize());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        List<NodeTag> tags = mapper.readValue(file.getInputStream(),
                mapper.getTypeFactory().constructCollectionType(List.class, NodeTag.class));

        int count = nodeTagService.importAll(tags);
        return ResponseEntity.ok(new ImportResult(count));
    }

    public record ImportResult(int count) {}
}
