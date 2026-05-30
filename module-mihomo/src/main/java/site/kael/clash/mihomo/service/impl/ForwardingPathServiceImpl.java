package site.kael.clash.mihomo.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.kael.clash.mihomo.model.ForwardingPathResult;
import site.kael.clash.mihomo.service.ForwardingPathService;

import java.util.*;

@Service
public class ForwardingPathServiceImpl implements ForwardingPathService {

    private static final Logger log = LoggerFactory.getLogger(ForwardingPathServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SuppressWarnings("unchecked")
    public ForwardingPathResult resolveForwardingPath(String rulesJson, String proxiesJson, String domain) {
        try {
            // 解析 rules API 响应: {"rules": [{"type":"DomainSuffix","payload":"...","proxy":"..."}, ...]}
            Map<String, Object> rulesResponse = objectMapper.readValue(rulesJson, new TypeReference<>() {});
            List<Map<String, Object>> rulesList = (List<Map<String, Object>>) rulesResponse.getOrDefault("rules", List.of());
            log.debug("解析到 {} 条规则", rulesList.size());

            // 解析 proxies API 响应: {"proxies": {"GroupName": {"type":"Selector","all":["node1","node2"]}, ...}}
            Map<String, Object> proxiesResponse = objectMapper.readValue(proxiesJson, new TypeReference<>() {});
            Map<String, Map<String, Object>> proxiesMap = (Map<String, Map<String, Object>>) proxiesResponse.getOrDefault("proxies", Map.of());
            log.debug("解析到 {} 个代理/代理组", proxiesMap.size());

            // 匹配域名对应的规则
            Map<String, Object> matchedRule = matchRule(rulesList, domain);
            log.debug("域名 '{}' 匹配规则: {}", domain, matchedRule);
            if (matchedRule == null) {
                return new ForwardingPathResult(List.of(), List.of());
            }

            String targetGroupName = (String) matchedRule.get("proxy");
            String ruleType = (String) matchedRule.get("type");
            String rulePayload = (String) matchedRule.get("payload");
            String ruleLabel = (rulePayload != null ? ruleType + " " + rulePayload : ruleType)
                    + " → " + targetGroupName;
            log.debug("目标代理组: {}", targetGroupName);

            // 构建流程图数据
            List<ForwardingPathResult.Node> nodes = new ArrayList<>();
            List<ForwardingPathResult.Edge> edges = new ArrayList<>();
            int[] edgeCounter = {0};

            // 1. 域名节点
            nodes.add(new ForwardingPathResult.Node("domain", "domain", Map.of("label", domain)));

            // 2. 规则节点
            String ruleNodeId = "rule-0";
            nodes.add(new ForwardingPathResult.Node(ruleNodeId, "rule", Map.of("label", ruleLabel)));
            edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, "domain", ruleNodeId));

            // 3. 目标代理组节点
            buildGroupNodes(targetGroupName, proxiesMap, ruleNodeId, nodes, edges, edgeCounter);

            log.debug("构建流程图完成: {} 个节点, {} 条边", nodes.size(), edges.size());
            return new ForwardingPathResult(nodes, edges);
        } catch (Exception e) {
            log.error("解析转发路径失败: {}", e.getMessage(), e);
            return new ForwardingPathResult(List.of(), List.of());
        }
    }

    /**
     * 匹配域名对应的规则
     * Mihomo 规则格式: {"type": "DomainSuffix", "payload": "google.com", "proxy": "Proxy"}
     */
    private Map<String, Object> matchRule(List<Map<String, Object>> rules, String domain) {
        for (Map<String, Object> rule : rules) {
            String type = (String) rule.get("type");
            String payload = (String) rule.get("payload");

            if (type == null) continue;

            switch (type) {
                case "Domain":
                    if (domain.equals(payload)) return rule;
                    break;
                case "DomainSuffix":
                    // Mihomo 的 DomainSuffix 按域名层级匹配：
                    // "le.com" 匹配 "le.com" 和 "api.le.com"，不匹配 "google.com"
                    if (domain.equals(payload) || domain.endsWith("." + payload)) return rule;
                    break;
                case "DomainKeyword":
                    if (domain.contains(payload)) return rule;
                    break;
                case "Match":
                    return rule;  // 兜底规则，直接匹配
                // IP-CIDR, GeoIP 等不支持域名匹配，跳过
            }
        }
        return null;
    }

    /**
     * 递归构建代理组及其子节点，展示当前选中的代理路径
     * Mihomo 代理组格式: {"type": "Selector", "now": "当前选中代理", "all": ["node1", "node2", ...]}
     */
    @SuppressWarnings("unchecked")
    private void buildGroupNodes(
            String groupName,
            Map<String, Map<String, Object>> proxiesMap,
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

        Map<String, Object> proxyInfo = proxiesMap.get(groupName);
        if (proxyInfo == null) {
            // 代理不存在，可能是自定义节点名称
            String proxyId = "proxy-" + groupName.replaceAll("[^a-zA-Z0-9-]", "_");
            nodes.add(new ForwardingPathResult.Node(proxyId, "proxy", Map.of("label", groupName)));
            edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, parentNodeId, proxyId));
            return;
        }

        String type = (String) proxyInfo.getOrDefault("type", "Unknown");
        String groupId = "group-" + groupName.replaceAll("[^a-zA-Z0-9-]", "_");

        // 检查是否已添加过该代理组（避免循环引用）
        boolean alreadyExists = nodes.stream().anyMatch(n -> n.getId().equals(groupId));
        if (alreadyExists) {
            edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, parentNodeId, groupId));
            return;
        }

        nodes.add(new ForwardingPathResult.Node(groupId, "proxyGroup",
                Map.of("label", groupName, "groupType", type)));
        edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, parentNodeId, groupId));

        // 展开当前选中的代理路径
        String now = (String) proxyInfo.get("now");
        if (now != null) {
            // 有 now 字段（Selector/URLTest/Fallback 类型），展示当前选中的节点
            if (proxiesMap.containsKey(now)) {
                // 选中的代理本身也是代理组，递归展开
                buildGroupNodes(now, proxiesMap, groupId, nodes, edges, edgeCounter);
            } else {
                // 选中的是普通代理节点
                String proxyId = "proxy-" + now.replaceAll("[^a-zA-Z0-9-]", "_");
                nodes.add(new ForwardingPathResult.Node(proxyId, "proxy", Map.of("label", now)));
                edges.add(new ForwardingPathResult.Edge("e-" + edgeCounter[0]++, groupId, proxyId));
            }
        }
        // 无 now 字段的类型（LoadBalance/Direct/Reject/Compatible/Pass）不再展开子节点
    }
}
