package site.kael.clash.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.processor.model.RuleGroup;
import site.kael.clash.processor.service.RuleGroupService;

import java.util.List;
import java.util.Map;

/**
 * 规则组管理 REST API
 */
@RestController
@RequestMapping("/api/rule-groups")
public class RuleGroupController {

    private final RuleGroupService ruleGroupService;

    public RuleGroupController(RuleGroupService ruleGroupService) {
        this.ruleGroupService = ruleGroupService;
    }

    @GetMapping
    public ResponseEntity<List<RuleGroup>> findAll() {
        return ResponseEntity.ok(ruleGroupService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuleGroup> findById(@PathVariable String id) {
        return ruleGroupService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RuleGroup> create(@RequestBody RuleGroup ruleGroup) {
        RuleGroup created = ruleGroupService.create(ruleGroup);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RuleGroup> update(@PathVariable String id, @RequestBody RuleGroup ruleGroup) {
        RuleGroup existing = ruleGroupService.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        // 合并更新：只覆盖请求中非 null 的字段
        if (ruleGroup.getName() != null) existing.setName(ruleGroup.getName());
        if (ruleGroup.getDescription() != null) existing.setDescription(ruleGroup.getDescription());
        if (ruleGroup.getRules() != null) existing.setRules(ruleGroup.getRules());
        if (ruleGroup.getProxyObjects() != null) existing.setProxyObjects(ruleGroup.getProxyObjects());
        RuleGroup updated = ruleGroupService.update(existing);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (ruleGroupService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ruleGroupService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extractFromSubscription(@RequestBody Map<String, String> request) {
        String subscriptionId = request.get("subscriptionId");
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "subscriptionId 不能为空"));
        }
        try {
            RuleGroup ruleGroup = ruleGroupService.extractFromSubscription(subscriptionId);
            return ResponseEntity.ok(ruleGroup);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
