package site.kael.clash.processor.builtin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.api.ConfigProcessor;
import site.kael.clash.processor.api.ProcessingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点合并处理器：将多个订阅源的节点合并到主配置中
 * 同时合并代理组：同名组的代理列表会合并，新组直接添加
 */
@Component
public class NodeMergeProcessor implements ConfigProcessor {

    private static final Logger log = LoggerFactory.getLogger(NodeMergeProcessor.class);

    @Override
    public String getName() {
        return "node-merge";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ClashConfig process(ClashConfig input, ProcessingContext context) {
        // 创建新的 ClashConfig，不修改输入
        ClashConfig output = copyConfig(input);

        Object mergeConfigsObj = context.getVariable("mergeConfigs");
        if (!(mergeConfigsObj instanceof List<?> mergeConfigsList)) {
            log.info("未找到 mergeConfigs 变量，跳过合并");
            context.addLog("未找到 mergeConfigs 变量，跳过合并");
            return output;
        }

        int totalProxiesMerged = 0;

        for (Object obj : mergeConfigsList) {
            if (!(obj instanceof ClashConfig mergeConfig)) {
                continue;
            }

            // 合并代理节点
            List<ProxyNode> mergeProxies = mergeConfig.getProxies();
            output.getProxies().addAll(mergeProxies);
            totalProxiesMerged += mergeProxies.size();

            // 合并代理组
            mergeProxyGroups(output, mergeConfig.getProxyGroups());
        }

        String message = String.format("节点合并完成：合并了 %d 个配置源，新增 %d 个节点，当前总节点数 %d",
                mergeConfigsList.size(), totalProxiesMerged, output.getProxies().size());
        log.info(message);
        context.addLog(message);

        return output;
    }

    /**
     * 合并代理组：同名组合并代理列表，新组直接添加
     */
    @SuppressWarnings("unchecked")
    private void mergeProxyGroups(ClashConfig output, Map<String, Object> mergeGroups) {
        Map<String, Object> outputGroups = output.getProxyGroups();

        for (Map.Entry<String, Object> entry : mergeGroups.entrySet()) {
            String groupName = entry.getKey();
            Object mergeGroupValue = entry.getValue();

            if (outputGroups.containsKey(groupName)) {
                // 同名组：合并代理列表
                Object existingGroupValue = outputGroups.get(groupName);
                if (existingGroupValue instanceof Map<?, ?> existingGroup
                        && mergeGroupValue instanceof Map<?, ?> mergeGroup) {

                    Object existingProxies = existingGroup.get("proxies");
                    Object mergeProxies = mergeGroup.get("proxies");

                    if (existingProxies instanceof List<?> existingList
                            && mergeProxies instanceof List<?> mergeList) {
                        List<Object> merged = new ArrayList<>(existingList);
                        merged.addAll(mergeList);

                        Map<String, Object> updatedGroup = new HashMap<>((Map<String, Object>) existingGroup);
                        updatedGroup.put("proxies", merged);
                        outputGroups.put(groupName, updatedGroup);
                    }
                }
            } else {
                // 新组：直接添加
                outputGroups.put(groupName, mergeGroupValue);
            }
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
