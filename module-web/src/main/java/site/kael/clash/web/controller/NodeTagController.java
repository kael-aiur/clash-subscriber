package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.web.model.NodeTag;
import site.kael.clash.web.service.NodeTagService;

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
}
