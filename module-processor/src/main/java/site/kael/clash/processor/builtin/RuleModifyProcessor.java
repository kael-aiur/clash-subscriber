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
 * 规则修改处理器：支持添加、删除和替换规则
 * 从 context 中读取 ruleModify 配置：
 * - add: 要添加的规则列表（追加到末尾）
 * - remove: 要删除的规则列表（精确匹配）
 * - replace: 要替换的规则映射（旧规则 -> 新规则）
 */
@Component
public class RuleModifyProcessor implements ConfigProcessor {

    private static final Logger log = LoggerFactory.getLogger(RuleModifyProcessor.class);

    @Override
    public String getName() {
        return "rule-modify";
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ClashConfig process(ClashConfig input, ProcessingContext context) {
        // 创建新的 ClashConfig，不修改输入
        ClashConfig output = copyConfig(input);

        Object ruleModifyObj = context.getVariable("ruleModify");
        if (!(ruleModifyObj instanceof Map<?, ?> ruleModify)) {
            log.info("未找到 ruleModify 变量，跳过规则修改");
            context.addLog("未找到 ruleModify 变量，跳过规则修改");
            return output;
        }

        List<Object> rules = output.getRules();
        int addCount = 0;
        int removeCount = 0;
        int replaceCount = 0;

        // 删除规则
        Object removeObj = ruleModify.get("remove");
        if (removeObj instanceof List<?> removeList) {
            for (Object rule : removeList) {
                if (rules.remove(rule)) {
                    removeCount++;
                }
            }
        }

        // 替换规则
        Object replaceObj = ruleModify.get("replace");
        if (replaceObj instanceof Map<?, ?> replaceMap) {
            for (Map.Entry<?, ?> entry : replaceMap.entrySet()) {
                String oldRule = entry.getKey().toString();
                String newRule = entry.getValue().toString();
                int index = rules.indexOf(oldRule);
                if (index >= 0) {
                    rules.set(index, newRule);
                    replaceCount++;
                }
            }
        }

        // 添加规则
        Object addObj = ruleModify.get("add");
        if (addObj instanceof List<?> addList) {
            for (Object rule : addList) {
                rules.add(rule);
                addCount++;
            }
        }

        String message = String.format("规则修改完成：添加 %d 条，删除 %d 条，替换 %d 条，当前规则总数 %d",
                addCount, removeCount, replaceCount, rules.size());
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
