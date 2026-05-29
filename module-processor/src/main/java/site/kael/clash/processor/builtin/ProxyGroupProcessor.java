package site.kael.clash.processor.builtin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.processor.api.ConfigProcessor;
import site.kael.clash.processor.api.ProcessingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代理组重组处理器：根据配置替换代理组
 * 从 context 中读取 proxyGroupConfig 配置：
 * - groups: 代理组列表，每个组包含 name、type、proxies
 */
@Component
public class ProxyGroupProcessor implements ConfigProcessor {

    private static final Logger log = LoggerFactory.getLogger(ProxyGroupProcessor.class);

    @Override
    public String getName() {
        return "proxy-group";
    }

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ClashConfig process(ClashConfig input, ProcessingContext context) {
        // 创建新的 ClashConfig，不修改输入
        ClashConfig output = copyConfig(input);

        Object proxyGroupConfigObj = context.getVariable("proxyGroupConfig");
        if (!(proxyGroupConfigObj instanceof Map<?, ?> proxyGroupConfig)) {
            log.info("未找到 proxyGroupConfig 变量，跳过代理组重组");
            context.addLog("未找到 proxyGroupConfig 变量，跳过代理组重组");
            return output;
        }

        Object groupsObj = proxyGroupConfig.get("groups");
        if (!(groupsObj instanceof List<?> groupsList)) {
            log.info("proxyGroupConfig 中未找到 groups 列表，跳过代理组重组");
            context.addLog("proxyGroupConfig 中未找到 groups 列表，跳过代理组重组");
            return output;
        }

        // 重建代理组
        Map<String, Object> newProxyGroups = new HashMap<>();

        for (Object groupObj : groupsList) {
            if (!(groupObj instanceof Map<?, ?> groupMap)) {
                continue;
            }

            String name = (String) groupMap.get("name");
            String type = (String) groupMap.get("type");
            List<String> proxies = (List<String>) groupMap.get("proxies");

            if (name == null || type == null) {
                continue;
            }

            Map<String, Object> groupData = new HashMap<>();
            groupData.put("type", type);
            if (proxies != null) {
                groupData.put("proxies", new ArrayList<>(proxies));
            }

            // 复制其他字段（如 url、interval 等）
            for (Map.Entry<?, ?> entry : groupMap.entrySet()) {
                String key = entry.getKey().toString();
                if (!"name".equals(key) && !"type".equals(key) && !"proxies".equals(key)) {
                    groupData.put(key, entry.getValue());
                }
            }

            newProxyGroups.put(name, groupData);
        }

        output.setProxyGroups(newProxyGroups);

        String message = String.format("代理组重组完成：设置 %d 个代理组", newProxyGroups.size());
        log.info(message);
        context.addLog(message);

        return output;
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
