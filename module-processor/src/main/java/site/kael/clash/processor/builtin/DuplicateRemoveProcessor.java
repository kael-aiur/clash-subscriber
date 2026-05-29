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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 去重处理器：根据节点名称移除重复的代理节点，保留首次出现的节点
 */
@Component
public class DuplicateRemoveProcessor implements ConfigProcessor {

    private static final Logger log = LoggerFactory.getLogger(DuplicateRemoveProcessor.class);

    @Override
    public String getName() {
        return "duplicate-remove";
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public ClashConfig process(ClashConfig input, ProcessingContext context) {
        // 创建新的 ClashConfig，不修改输入
        ClashConfig output = copyConfig(input);

        List<ProxyNode> originalProxies = output.getProxies();
        List<ProxyNode> uniqueProxies = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        for (ProxyNode node : originalProxies) {
            if (seenNames.add(node.getName())) {
                uniqueProxies.add(node);
            }
        }

        int removedCount = originalProxies.size() - uniqueProxies.size();
        output.setProxies(uniqueProxies);

        String message = String.format("去重完成：原始节点数 %d，移除重复节点 %d，剩余节点 %d",
                originalProxies.size(), removedCount, uniqueProxies.size());
        log.info(message);
        context.addLog(message);

        return output;
    }

    /**
     * 深拷贝 ClashConfig，创建新的列表和 Map，但共享 ProxyNode 引用
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
