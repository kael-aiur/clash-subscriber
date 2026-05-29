package site.kael.clash.processor.builtin;

import org.graalvm.polyglot.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.processor.api.ConfigProcessor;
import site.kael.clash.processor.api.ProcessingContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 脚本处理器：使用 GraalVM JS 引擎执行自定义 JavaScript 脚本
 * 脚本可以访问 config（ClashConfig）和 context（ProcessingContext）变量
 * 脚本执行后应将处理结果赋值给 config 变量
 */
@Component
public class ScriptProcessor implements ConfigProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScriptProcessor.class);

    @Value("${data.path:data}")
    private String dataPath;

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

        // 创建输入的拷贝，避免修改原始配置
        ClashConfig configCopy = copyConfig(input);

        // 加载脚本文件
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

        // 使用 GraalVM JS 引擎执行脚本
        log.info("执行脚本: {}", scriptName);
        context.addLog("开始执行脚本: " + scriptName);

        try (Context jsContext = Context.create("js")) {
            org.graalvm.polyglot.Value bindings = jsContext.getBindings("js");
            bindings.putMember("config", configCopy);
            bindings.putMember("context", context);

            jsContext.eval("js", scriptContent);

            // 从脚本中获取处理结果
            org.graalvm.polyglot.Value resultValue = bindings.getMember("config");
            ClashConfig result = resultValue.as(ClashConfig.class);

            String message = String.format("脚本执行完成: %s", scriptName);
            log.info(message);
            context.addLog(message);

            return result;
        } catch (Exception e) {
            String errorMsg = String.format("脚本执行失败: %s, %s", scriptName, e.getMessage());
            log.error(errorMsg, e);
            throw new BusinessException(errorMsg);
        }
    }

    /**
     * 深拷贝 ClashConfig
     */
    private ClashConfig copyConfig(ClashConfig input) {
        ClashConfig copy = new ClashConfig(input.getName());
        copy.setProxies(new ArrayList<>(input.getProxies()));
        copy.setProxyGroups(new HashMap<>(input.getProxyGroups()));
        copy.setRules(new ArrayList<>(input.getRules()));
        copy.setRaw(new HashMap<>(input.getRaw()));
        return copy;
    }
}
