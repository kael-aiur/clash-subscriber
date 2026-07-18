package site.kael.clash.processor.util;

import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.model.NodePolicy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 节点过滤器：统一关键词匹配逻辑，供「订阅源节点采纳」与「代理组节点筛选」复用。
 */
public class NodeFilter {

    /**
     * 节点名是否包含任一关键词（不区分大小写；keywords 为空或 null 时返回 false）。
     */
    public static boolean containsAnyKeyword(String nodeName, List<String> keywords) {
        if (nodeName == null || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String lowerName = nodeName.toLowerCase();
        return keywords.stream().anyMatch(k -> k != null && lowerName.contains(k.toLowerCase()));
    }

    /**
     * 按采纳规则判断单个节点是否被采纳。
     * keyword 模式下 matchKeywords 为空时回退为 all 模式。
     */
    public static boolean isAccepted(ProxyNode node, NodePolicy policy) {
        if (node == null || node.getName() == null) {
            return false;
        }
        NodePolicy p = policy != null ? policy : new NodePolicy();
        boolean keywordMode = NodePolicy.MODE_KEYWORD.equals(p.getMode())
                && p.getMatchKeywords() != null && !p.getMatchKeywords().isEmpty();
        if (keywordMode && !containsAnyKeyword(node.getName(), p.getMatchKeywords())) {
            return false;
        }
        return !containsAnyKeyword(node.getName(), p.getExcludeKeywords());
    }

    /**
     * 按采纳规则过滤节点列表，返回新列表。
     */
    public static List<ProxyNode> filter(List<ProxyNode> nodes, NodePolicy policy) {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
                .filter(n -> isAccepted(n, policy))
                .collect(Collectors.toList());
    }
}
