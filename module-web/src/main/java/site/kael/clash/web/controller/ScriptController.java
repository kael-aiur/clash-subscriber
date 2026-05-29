package site.kael.clash.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.kael.clash.common.exception.BusinessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public ScriptController(@Value("${data.path:data}") String dataPath) {
        this.scriptsDir = Path.of(dataPath, "scripts");
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
}
