package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import java.util.Collections;
import site.kael.clash.common.util.YamlUtil;
import site.kael.clash.processor.engine.ScriptEngine;
import site.kael.clash.subscription.service.SubscriptionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 脚本管理 REST 控制器
 * <p>
 * 提供 JS 脚本文件的列表、读取、保存和删除功能。
 * 脚本存储在 data/scripts/{name}.js。
 */
@RestController
@RequestMapping("/api/scripts")
public class ScriptController {

    private static final Logger log = LoggerFactory.getLogger(ScriptController.class);

    private final Path scriptsDir;
    private final SubscriptionService subscriptionService;
    private final ScriptEngine scriptEngine;

    public ScriptController(@Value("${data.path:data}") String dataPath,
                            SubscriptionService subscriptionService,
                            ScriptEngine scriptEngine) {
        this.scriptsDir = Path.of(dataPath, "scripts");
        this.subscriptionService = subscriptionService;
        this.scriptEngine = scriptEngine;
    }

    /**
     * 列出所有脚本文件名（不含 .js 后缀）
     */
    @GetMapping
    public ResponseEntity<List<String>> listScripts() {
        log.debug("列出所有脚本");
        try {
            Files.createDirectories(scriptsDir);
            try (Stream<Path> stream = Files.list(scriptsDir)) {
                List<String> scripts = stream
                        .filter(p -> p.toString().endsWith(".js"))
                        .map(p -> p.getFileName().toString().replace(".js", ""))
                        .sorted()
                        .collect(Collectors.toList());
                return ResponseEntity.ok(scripts);
            }
        } catch (IOException e) {
            throw new BusinessException("列出脚本文件失败: " + e.getMessage());
        }
    }

    /**
     * 保存脚本（body 包含 name 和 content 字段）
     */
    @PostMapping
    public ResponseEntity<Void> saveScript(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String content = body.get("content");
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "脚本名称不能为空");
        }
        if (content == null) {
            throw new BusinessException(400, "脚本内容不能为空");
        }
        log.info("保存脚本: name={}", name);
        try {
            Files.createDirectories(scriptsDir);
            Path scriptPath = scriptsDir.resolve(name + ".js");
            Files.writeString(scriptPath, content, StandardCharsets.UTF_8);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            throw new BusinessException("保存脚本文件失败: " + e.getMessage());
        }
    }

    /**
     * 读取脚本内容
     */
    @GetMapping("/{name}")
    public ResponseEntity<String> readScript(@PathVariable String name) {
        log.debug("读取脚本: name={}", name);
        Path scriptPath = scriptsDir.resolve(name + ".js");
        if (!Files.exists(scriptPath)) {
            throw new BusinessException(404, "脚本不存在: " + name);
        }
        try {
            String content = Files.readString(scriptPath, StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            throw new BusinessException("读取脚本文件失败: " + e.getMessage());
        }
    }

    /**
     * 删除脚本
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteScript(@PathVariable String name) {
        log.info("删除脚本: name={}", name);
        Path scriptPath = scriptsDir.resolve(name + ".js");
        if (!Files.exists(scriptPath)) {
            throw new BusinessException(404, "脚本不存在: " + name);
        }
        try {
            Files.delete(scriptPath);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            throw new BusinessException("删除脚本文件失败: " + e.getMessage());
        }
    }

    /**
     * 试运行脚本：获取订阅源配置后执行脚本，返回结果摘要
     */
    @PostMapping("/try-run")
    public ResponseEntity<Map<String, Object>> tryRun(@RequestBody Map<String, String> body) {
        String scriptContent = body.get("scriptContent");
        String subscriptionId = body.get("subscriptionId");

        if (scriptContent == null || scriptContent.isBlank()) {
            throw new BusinessException(400, "脚本内容不能为空");
        }
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new BusinessException(400, "请选择订阅源");
        }

        log.info("试运行脚本: subscriptionId={}", subscriptionId);

        try {
            ClashConfig config = subscriptionService.fetch(subscriptionId);
            int proxiesBefore = config.getProxies().size();
            int groupsBefore = config.getProxyGroups().size();
            int rulesBefore = config.getRules().size();

            ClashConfig result = scriptEngine.execute(scriptContent, config, "try-run");

            int proxiesAfter = result.getProxies().size();
            int groupsAfter = result.getProxyGroups().size();
            int rulesAfter = result.getRules().size();

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("proxiesBefore", proxiesBefore);
            summary.put("proxiesAfter", proxiesAfter);
            summary.put("groupsBefore", groupsBefore);
            summary.put("groupsAfter", groupsAfter);
            summary.put("rulesBefore", rulesBefore);
            summary.put("rulesAfter", rulesAfter);

            Map<String, Object> inputSummary = buildConfigSummary(config);
            String inputYaml = serializeToYaml(config.getRaw());

            Map<String, Object> outputSummary = buildConfigSummary(result);
            String outputYaml = serializeToYaml(result.getRaw());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("summary", summary);
            response.put("config", result.getRaw());
            response.put("inputSummary", inputSummary);
            response.put("inputYaml", inputYaml);
            response.put("outputSummary", outputSummary);
            response.put("outputYaml", outputYaml);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("试运行失败: {}", e.getMessage());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 预览订阅源配置：获取订阅源的完整配置摘要和 YAML
     */
    @PostMapping("/preview-subscription")
    public ResponseEntity<Map<String, Object>> previewSubscription(@RequestBody Map<String, String> body) {
        String subscriptionId = body.get("subscriptionId");
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new BusinessException(400, "请选择订阅源");
        }

        log.info("预览订阅配置: subscriptionId={}", subscriptionId);

        try {
            ClashConfig config = subscriptionService.fetch(subscriptionId);
            Map<String, Object> summary = buildConfigSummary(config);
            String yaml = serializeToYaml(config.getRaw());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("summary", summary);
            response.put("yaml", yaml);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("预览订阅配置失败: {}", e.getMessage());
            throw new BusinessException("获取订阅配置失败: " + e.getMessage());
        }
    }

    private Map<String, Object> buildConfigSummary(ClashConfig config) {
        List<ProxyNode> proxies = config.getProxies() != null ? config.getProxies() : Collections.emptyList();
        Map<String, Object> groups = config.getProxyGroups() != null ? config.getProxyGroups() : Collections.emptyMap();
        List<Object> rules = config.getRules() != null ? config.getRules() : Collections.emptyList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("nodeCount", proxies.size());
        summary.put("proxyGroupCount", groups.size());
        summary.put("ruleCount", rules.size());
        summary.put("nodeNames", proxies.stream().limit(5).map(ProxyNode::getName).collect(Collectors.toList()));
        summary.put("proxyGroupNames", groups.keySet().stream().limit(5).collect(Collectors.toList()));
        return summary;
    }

    private String serializeToYaml(Map<String, Object> raw) {
        return YamlUtil.dump(raw);
    }
}
