package site.kael.clash.mihomo.service.impl;

import org.springframework.stereotype.Service;
import site.kael.clash.mihomo.model.ForwardingPathResult;
import site.kael.clash.mihomo.service.ForwardingPathService;

import java.util.*;

@Service
public class ForwardingPathServiceImpl implements ForwardingPathService {

    @Override
    @SuppressWarnings("unchecked")
    public ForwardingPathResult resolveForwardingPath(String configYaml, String domain) {
        Map<String, Object> config = parseConfig(configYaml);

        // 提取规则列表
        List<String> rules = (List<String>) config.getOrDefault("rules", List.of());

        // 提取代理组（YAML 中 proxy-groups 是数组格式）
        List<Map<String, Object>> proxyGroupList =
                (List<Map<String, Object>>) config.getOrDefault("proxy-groups", List.of());
        Map<String, Map<String, Object>> proxyGroups = new LinkedHashMap<>();
        for (Map<String, Object> group : proxyGroupList) {
            String name = (String) group.get("name");
            proxyGroups.put(name, group);
        }

        // 匹配域名对应的规则
        String matchedRule = matchRule(rules, domain);
        if (matchedRule == null) {
            return new ForwardingPathResult(List.of(), List.of());
        }

        String targetGroupName = extractTargetGroup(matchedRule);
        if (targetGroupName == null) {
            return new ForwardingPathResult(List.of(), List.of());
        }

        // 构建流程图数据
        List<ForwardingPathResult.Node> nodes = new ArrayList<>();
        List<ForwardingPathResult.Edge> edges = new ArrayList<>();
        int[] edgeCounter = {0};

        // 1. 域名节点
        nodes.add(new ForwardingPathResult.Node("domain", "domain", Map.of("label", domain)));

        // 2. 规则节点
        String ruleNodeId = "rule-0";
        nodes.add(new ForwardingPathResult.Node(ruleNodeId, "rule", Map.of("label", matchedRule)));
        edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, "domain", ruleNodeId));

        // 3. 目标代理组节点
        buildGroupNodes(targetGroupName, proxyGroups, ruleNodeId, nodes, edges, edgeCounter);

        return new ForwardingPathResult(nodes, edges);
    }

    /**
     * 解析 YAML 配置，提取 rules、proxy-groups、proxies
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String configYaml) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        return yaml.load(configYaml);
    }

    /**
     * 匹配域名对应的规则
     * 按优先级从上到下遍历，返回第一个匹配的规则
     */
    private String matchRule(List<String> rules, String domain) {
        for (String rule : rules) {
            String[] parts = rule.split(",");
            if (parts.length < 2) continue;

            String ruleType = parts[0];
            String ruleValue = parts[1];

            switch (ruleType) {
                case "DOMAIN":
                    if (domain.equals(ruleValue)) return rule;
                    break;
                case "DOMAIN-SUFFIX":
                    if (domain.endsWith(ruleValue) || domain.equals(ruleValue)) return rule;
                    break;
                case "DOMAIN-KEYWORD":
                    if (domain.contains(ruleValue)) return rule;
                    break;
                case "MATCH":
                    return rule;  // 兜底规则，直接匹配
                // IP-CIDR, GEOIP 等不支持域名匹配，跳过
            }
        }
        return null;
    }

    /**
     * 从规则字符串中提取目标代理组名称
     */
    private String extractTargetGroup(String rule) {
        String[] parts = rule.split(",");
        return parts.length >= 3 ? parts[parts.length - 1] : null;
    }

    /**
     * 递归构建代理组及其子节点
     */
    @SuppressWarnings("unchecked")
    private void buildGroupNodes(
            String groupName,
            Map<String, Map<String, Object>> proxyGroups,
            String parentNodeId,
            List<ForwardingPathResult.Node> nodes,
            List<ForwardingPathResult.Edge> edges,
            int[] edgeCounter) {

        // 处理 DIRECT 和 REJECT 特殊出口
        if ("DIRECT".equals(groupName) || "REJECT".equals(groupName)) {
            String targetId = "target-" + groupName.toLowerCase();
            nodes.add(new ForwardingPathResult.Node(targetId, "target", Map.of("label", groupName)));
            edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, parentNodeId, targetId));
            return;
        }

        Map<String, Object> group = proxyGroups.get(groupName);
        if (group == null) {
            // 代理组不存在，可能是代理节点名称
            String proxyId = "proxy-" + groupName.replaceAll("[^a-zA-Z0-9-]", "_");
            nodes.add(new ForwardingPathResult.Node(proxyId, "proxy", Map.of("label", groupName)));
            edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, parentNodeId, proxyId));
            return;
        }

        String groupType = (String) group.getOrDefault("type", "select");
        String groupId = "group-" + groupName.replaceAll("[^a-zA-Z0-9-]", "_");

        // 检查是否已添加过该代理组（避免循环引用）
        boolean alreadyExists = nodes.stream().anyMatch(n -> n.getId().equals(groupId));
        if (alreadyExists) {
            edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, parentNodeId, groupId));
            return;
        }

        nodes.add(new ForwardingPathResult.Node(groupId, "proxyGroup",
                Map.of("label", groupName, "groupType", groupType)));
        edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, parentNodeId, groupId));

        // 处理代理组内的代理列表
        List<String> proxies = (List<String>) group.getOrDefault("proxies", List.of());
        for (String proxyName : proxies) {
            // 判断是子代理组还是代理节点
            if (proxyGroups.containsKey(proxyName)) {
                buildGroupNodes(proxyName, proxyGroups, groupId, nodes, edges, edgeCounter);
            } else {
                // 代理节点
                String proxyId = "proxy-" + proxyName.replaceAll("[^a-zA-Z0-9-]", "_");
                nodes.add(new ForwardingPathResult.Node(proxyId, "proxy", Map.of("label", proxyName)));
                edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, groupId, proxyId));
            }
        }
    }
}
