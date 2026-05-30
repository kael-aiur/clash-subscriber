package site.kael.clash.processor.builtin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.processor.api.ConfigProcessor;
import site.kael.clash.processor.api.ProcessingContext;
import site.kael.clash.processor.engine.ScriptEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

@Component
public class ScriptProcessor implements ConfigProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScriptProcessor.class);

    private final ScriptEngine scriptEngine;

    @Value("${data.path:data}")
    private String dataPath;

    public ScriptProcessor(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    @Override
    public String getName() {
        return "script";
    }

    @Override
    public int getOrder() {
        return 999;
    }

    @Override
    public ClashConfig process(ClashConfig input, ProcessingContext context) {
        Object scriptNameObj = context.getVariable("scriptName");
        if (!(scriptNameObj instanceof String scriptName) || scriptName.isBlank()) {
            throw new BusinessException("未指定脚本名称");
        }

        Path scriptPath = Paths.get(dataPath, "scripts", scriptName + ".js");
        if (!Files.exists(scriptPath)) {
            throw new BusinessException("脚本不存在: " + scriptName);
        }

        String scriptContent;
        try {
            scriptContent = Files.readString(scriptPath);
        } catch (IOException e) {
            throw new BusinessException("读取脚本失败: " + scriptName + ", " + e.getMessage());
        }

        ClashConfig configCopy = copyConfig(input);

        log.info("执行脚本: {}", scriptName);
        context.addLog("开始执行脚本: " + scriptName);

        ClashConfig result = scriptEngine.execute(scriptContent, configCopy, "");

        String message = String.format("脚本执行完成: %s", scriptName);
        log.info(message);
        context.addLog(message);

        return result;
    }

    private ClashConfig copyConfig(ClashConfig input) {
        ClashConfig copy = new ClashConfig(input.getName());
        copy.setProxies(new ArrayList<>(input.getProxies()));
        copy.setProxyGroups(new HashMap<>(input.getProxyGroups()));
        copy.setRules(new ArrayList<>(input.getRules()));
        copy.setRaw(new HashMap<>(input.getRaw()));
        return copy;
    }
}
