# Mihomo 实例详情页实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Mihomo 实例增加详情页面，包含实例信息、转发规则流程图、推送历史三个标签页，用户可输入域名查询完整转发路径并以 Vue Flow 流程图展示。

**Architecture:** 后端主导架构。后端新增 `ForwardingPathService` 负责从 Mihomo API 获取配置、解析规则、匹配域名、构建 Vue Flow 格式的流程图数据。前端新增详情页面，使用 Vue Flow 渲染流程图，dagre 自动布局。

**Tech Stack:** Java 21, Spring Boot 3.2.5, SnakeYAML, OkHttp, Vue 3, TypeScript, Vue Flow, Element Plus, dagre

---

## Task 1: 后端 — MihomoHttpClient.getConfig()

**Files:**
- Modify: `module-mihomo/src/main/java/site/kael/clash/mihomo/client/MihomoHttpClient.java`

- [ ] **Step 1:** 在 `MihomoHttpClient` 中新增 `getConfig()` 方法

```java
/**
 * 从 Mihomo 实例获取当前运行配置
 *
 * @param apiUrl    Mihomo API 地址
 * @param apiSecret API 密钥（可为 null）
 * @return YAML 格式的配置字符串
 */
public String getConfig(String apiUrl, String apiSecret) {
    Request.Builder builder = new Request.Builder()
            .url(apiUrl + "/configs")
            .get();

    if (apiSecret != null && !apiSecret.isEmpty()) {
        builder.addHeader("Authorization", "Bearer " + apiSecret);
    }

    try (Response response = client.newCall(builder.build()).execute()) {
        if (!response.isSuccessful()) {
            String body = response.body() != null ? response.body().string() : "";
            throw new BusinessException("获取配置失败: HTTP " + response.code() + " - " + body);
        }
        return response.body() != null ? response.body().string() : "";
    } catch (IOException e) {
        throw new BusinessException("获取配置失败: " + e.getMessage());
    }
}
```

- [ ] **Step 2:** 编译验证

Run: `mvn compile -pl module-mihomo`
Expected: BUILD SUCCESS

- [ ] **Step 3:** 提交

```bash
git add module-mihomo/src/main/java/site/kael/clash/mihomo/client/MihomoHttpClient.java
git commit -m "feat(mihomo): 新增 MihomoHttpClient.getConfig() 方法"
```

---

## Task 2: 后端 — MihomoService.getConfig()

**Files:**
- Modify: `module-mihomo/src/main/java/site/kael/clash/mihomo/service/MihomoService.java`
- Modify: `module-mihomo/src/main/java/site/kael/clash/mihomo/service/impl/MihomoServiceImpl.java`

- [ ] **Step 1:** 在 `MihomoService` 接口中新增方法

```java
/**
 * 获取指定实例的当前运行配置
 *
 * @param instanceId 实例 ID
 * @return YAML 格式的配置字符串
 */
String getConfig(String instanceId);
```

- [ ] **Step 2:** 在 `MihomoServiceImpl` 中实现方法

```java
@Override
public String getConfig(String instanceId) {
    MihomoInstance instance = repository.findById(instanceId)
            .orElseThrow(() -> new BusinessException(404, "Mihomo 实例不存在: " + instanceId));
    return httpClient.getConfig(instance.getApiUrl(), instance.getApiSecret());
}
```

- [ ] **Step 3:** 编译验证

Run: `mvn compile -pl module-mihomo`
Expected: BUILD SUCCESS

- [ ] **Step 4:** 提交

```bash
git add module-mihomo/src/main/java/site/kael/clash/mihomo/service/MihomoService.java module-mihomo/src/main/java/site/kael/clash/mihomo/service/impl/MihomoServiceImpl.java
git commit -m "feat(mihomo): 新增 MihomoService.getConfig() 方法"
```

---

## Task 3: 后端 — ForwardingPathResult DTO

**Files:**
- Create: `module-mihomo/src/main/java/site/kael/clash/mihomo/model/ForwardingPathResult.java`

- [ ] **Step 1:** 创建 DTO 类

```java
package site.kael.clash.mihomo.model;

import java.util.List;
import java.util.Map;

/**
 * 转发路径查询结果，包含 Vue Flow 格式的节点和边
 */
public class ForwardingPathResult {

    private List<Node> nodes;
    private List<Edge> edges;

    public ForwardingPathResult(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<Node> getNodes() { return nodes; }
    public void setNodes(List<Node> nodes) { this.nodes = nodes; }
    public List<Edge> getEdges() { return edges; }
    public void setEdges(List<Edge> edges) { this.edges = edges; }

    /**
     * Vue Flow 节点
     */
    public static class Node {
        private String id;
        private String type;  // domain, rule, proxyGroup, proxy, target
        private Map<String, Object> data;
        private Position position;

        public Node(String id, String type, Map<String, Object> data) {
            this.id = id;
            this.type = type;
            this.data = data;
            this.position = new Position(0, 0);  // 布局由前端 dagre 计算
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        public Position getPosition() { return position; }
        public void setPosition(Position position) { this.position = position; }
    }

    /**
     * Vue Flow 边
     */
    public static class Edge {
        private String id;
        private String source;
        private String target;

        public Edge(String id, String source, String target) {
            this.id = id;
            this.source = source;
            this.target = target;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
    }

    /**
     * 节点位置（前端 dagre 会重新计算）
     */
    public static class Position {
        private double x;
        private double y;

        public Position(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
    }
}
```

- [ ] **Step 2:** 编译验证

Run: `mvn compile -pl module-mihomo`
Expected: BUILD SUCCESS

- [ ] **Step 3:** 提交

```bash
git add module-mihomo/src/main/java/site/kael/clash/mihomo/model/ForwardingPathResult.java
git commit -m "feat(mihomo): 新增 ForwardingPathResult DTO"
```

---

## Task 4: 后端 — ForwardingPathService 配置解析

**Files:**
- Create: `module-mihomo/src/main/java/site/kael/clash/mihomo/service/ForwardingPathService.java`
- Create: `module-mihomo/src/main/java/site/kael/clash/mihomo/service/impl/ForwardingPathServiceImpl.java`

- [ ] **Step 1:** 创建 `ForwardingPathService` 接口

```java
package site.kael.clash.mihomo.service;

import site.kael.clash.mihomo.model.ForwardingPathResult;

/**
 * 转发路径解析服务
 * <p>
 * 从 Mihomo 配置中解析规则匹配和代理组路由，构建转发路径流程图数据。
 */
public interface ForwardingPathService {

    /**
     * 查询指定域名的转发路径
     *
     * @param configYaml Mihomo YAML 配置
     * @param domain     用户输入的域名
     * @return Vue Flow 格式的流程图数据
     */
    ForwardingPathResult resolveForwardingPath(String configYaml, String domain);
}
```

- [ ] **Step 2:** 创建 `ForwardingPathServiceImpl` 框架

```java
package site.kael.clash.mihomo.service.impl;

import org.springframework.stereotype.Service;
import site.kael.clash.mihomo.model.ForwardingPathResult;
import site.kael.clash.mihomo.service.ForwardingPathService;

import java.util.*;

@Service
public class ForwardingPathServiceImpl implements ForwardingPathService {

    @Override
    public ForwardingPathResult resolveForwardingPath(String configYaml, String domain) {
        // TODO: 后续步骤实现
        return new ForwardingPathResult(List.of(), List.of());
    }
}
```

- [ ] **Step 3:** 编译验证

Run: `mvn compile -pl module-mihomo`
Expected: BUILD SUCCESS

- [ ] **Step 4:** 提交

```bash
git add module-mihomo/src/main/java/site/kael/clash/mihomo/service/ForwardingPathService.java module-mihomo/src/main/java/site/kael/clash/mihomo/service/impl/ForwardingPathServiceImpl.java
git commit -m "feat(mihomo): 新增 ForwardingPathService 接口和实现框架"
```

---

## Task 5: 后端 — ForwardingPathServiceImpl 规则解析和域名匹配

**Files:**
- Modify: `module-mihomo/src/main/java/site/kael/clash/mihomo/service/impl/ForwardingPathServiceImpl.java`

- [ ] **Step 1:** 实现配置解析和域名匹配逻辑

在 `ForwardingPathServiceImpl` 中添加以下方法：

```java
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
```

- [ ] **Step 2:** 更新 `resolveForwardingPath` 方法实现完整的解析流程

```java
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
```

- [ ] **Step 3:** 实现代理组节点递归构建方法

```java
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
```

- [ ] **Step 4:** 编译验证

Run: `mvn compile -pl module-mihomo`
Expected: BUILD SUCCESS

- [ ] **Step 5:** 提交

```bash
git add module-mihomo/src/main/java/site/kael/clash/mihomo/service/impl/ForwardingPathServiceImpl.java
git commit -m "feat(mihomo): 实现转发路径解析引擎"
```

---

## Task 6: 后端 — Controller 转发路径 API 端点

**Files:**
- Modify: `module-web/src/main/java/site/kael/clash/web/controller/MihomoInstanceController.java`

- [ ] **Step 1:** 在 Controller 中注入 `ForwardingPathService`

```java
private final MihomoService mihomoService;
private final ForwardingPathService forwardingPathService;

public MihomoInstanceController(MihomoService mihomoService, ForwardingPathService forwardingPathService) {
    this.mihomoService = mihomoService;
    this.forwardingPathService = forwardingPathService;
}
```

- [ ] **Step 2:** 新增转发路径查询端点

```java
/**
 * 查询域名的转发路径
 */
@GetMapping("/{id}/forwarding-path")
public ResponseEntity<ForwardingPathResult> getForwardingPath(
        @PathVariable String id,
        @RequestParam String domain) {
    log.debug("查询转发路径: id={}, domain={}", id, domain);
    String configYaml = mihomoService.getConfig(id);
    ForwardingPathResult result = forwardingPathService.resolveForwardingPath(configYaml, domain);
    return ResponseEntity.ok(result);
}
```

- [ ] **Step 3:** 新增获取配置端点

```java
/**
 * 获取实例当前配置
 */
@GetMapping("/{id}/config")
public ResponseEntity<String> getConfig(@PathVariable String id) {
    log.debug("获取实例配置: id={}", id);
    String configYaml = mihomoService.getConfig(id);
    return ResponseEntity.ok(configYaml);
}
```

- [ ] **Step 4:** 添加必要的 import

```java
import site.kael.clash.mihomo.model.ForwardingPathResult;
import site.kael.clash.mihomo.service.ForwardingPathService;
```

- [ ] **Step 5:** 编译验证

Run: `mvn compile -pl module-web`
Expected: BUILD SUCCESS

- [ ] **Step 6:** 提交

```bash
git add module-web/src/main/java/site/kael/clash/web/controller/MihomoInstanceController.java
git commit -m "feat(web): 新增转发路径和配置获取 API 端点"
```

---

## Task 7: 前端 — 安装依赖和 API 方法

**Files:**
- Modify: `module-web/frontend/package.json`
- Modify: `module-web/frontend/src/api/mihomo.ts`

- [ ] **Step 1:** 安装 Vue Flow 和 dagre 依赖

Run: `cd module-web/frontend && npm install @vue-flow/core @vue-flow/background @vue-flow/controls dagre @types/dagre`

Expected: 安装成功，package.json 中新增依赖

- [ ] **Step 2:** 在 `api/mihomo.ts` 中新增 API 方法

```typescript
/**
 * 查询域名的转发路径
 */
export function getForwardingPath(id: string, domain: string) {
  return api.get<ForwardingPathResult>(`/mihomo-instances/${id}/forwarding-path`, {
    params: { domain }
  })
}

/**
 * 获取实例当前配置
 */
export function getMihomoConfig(id: string) {
  return api.get<string>(`/mihomo-instances/${id}/config`)
}
```

- [ ] **Step 3:** 在 `api/mihomo.ts` 顶部添加类型定义

```typescript
export interface FlowNode {
  id: string
  type: 'domain' | 'rule' | 'proxyGroup' | 'proxy' | 'target'
  data: Record<string, any>
  position: { x: number; y: number }
}

export interface FlowEdge {
  id: string
  source: string
  target: string
}

export interface ForwardingPathResult {
  nodes: FlowNode[]
  edges: FlowEdge[]
}
```

- [ ] **Step 4:** 前端编译验证

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 5:** 提交

```bash
git add module-web/frontend/package.json module-web/frontend/package-lock.json module-web/frontend/src/api/mihomo.ts
git commit -m "feat(frontend): 安装 Vue Flow 依赖，新增转发路径 API"
```

---

## Task 8: 前端 — 路由和详情页框架

**Files:**
- Modify: `module-web/frontend/src/router/index.ts`
- Create: `module-web/frontend/src/views/MihomoInstanceDetailView.vue`

- [ ] **Step 1:** 在路由中新增详情页路由

在 `router/index.ts` 的 routes 数组中添加：

```typescript
{
  path: '/mihomo-instances/:id',
  name: 'MihomoInstanceDetail',
  component: () => import('../views/MihomoInstanceDetailView.vue'),
  meta: { title: 'Mihomo 实例详情' }
}
```

- [ ] **Step 2:** 创建详情页框架组件

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMihomoInstance } from '../api/mihomo'

const route = useRoute()
const router = useRouter()
const instanceId = route.params.id as string
const activeTab = ref('info')

const instance = ref<any>(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await getMihomoInstance(instanceId)
    instance.value = data
  } catch (error) {
    console.error('获取实例信息失败:', error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="mihomo-detail-view" v-loading="loading">
    <div class="page-header">
      <el-button @click="router.push('/mihomo-instances')" text>
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
      <h2>{{ instance?.name || '实例详情' }}</h2>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="实例信息" name="info">
        <!-- 实例信息内容 -->
      </el-tab-pane>
      <el-tab-pane label="转发规则" name="forwarding">
        <!-- 转发规则内容 -->
      </el-tab-pane>
      <el-tab-pane label="推送历史" name="history">
        <!-- 推送历史内容 -->
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.mihomo-detail-view {
  padding: 20px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
}
</style>
```

- [ ] **Step 3:** 前端编译验证

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 4:** 提交

```bash
git add module-web/frontend/src/router/index.ts module-web/frontend/src/views/MihomoInstanceDetailView.vue
git commit -m "feat(frontend): 新增 Mihomo 实例详情页路由和框架"
```

---

## Task 9: 前端 — 实例信息标签页

**Files:**
- Modify: `module-web/frontend/src/views/MihomoInstanceDetailView.vue`

- [ ] **Step 1:** 实现实例信息标签页内容

在 `el-tab-pane label="实例信息"` 中添加：

```vue
<el-tab-pane label="实例信息" name="info">
  <el-descriptions :column="2" border>
    <el-descriptions-item label="名称">{{ instance?.name }}</el-descriptions-item>
    <el-descriptions-item label="API URL">{{ instance?.apiUrl }}</el-descriptions-item>
    <el-descriptions-item label="API Secret">
      {{ instance?.apiSecret ? '******' : '未设置' }}
    </el-descriptions-item>
    <el-descriptions-item label="启用状态">
      <el-tag :type="instance?.enabled ? 'success' : 'info'">
        {{ instance?.enabled ? '已启用' : '已禁用' }}
      </el-tag>
    </el-descriptions-item>
    <el-descriptions-item label="健康状态">
      <el-tag :type="instance?.status === 'HEALTHY' ? 'success' : instance?.status === 'UNHEALTHY' ? 'danger' : 'warning'">
        {{ instance?.status === 'HEALTHY' ? '健康' : instance?.status === 'UNHEALTHY' ? '异常' : '未知' }}
      </el-tag>
    </el-descriptions-item>
    <el-descriptions-item label="最后检查">
      {{ instance?.lastHealthCheck || '未检查' }}
    </el-descriptions-item>
  </el-descriptions>
</el-tab-pane>
```

- [ ] **Step 2:** 前端编译验证

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 3:** 提交

```bash
git add module-web/frontend/src/views/MihomoInstanceDetailView.vue
git commit -m "feat(frontend): 实现实例信息标签页"
```

---

## Task 10: 前端 — ForwardingRuleTab 转发规则标签页

**Files:**
- Create: `module-web/frontend/src/components/ForwardingRuleTab.vue`
- Modify: `module-web/frontend/src/views/MihomoInstanceDetailView.vue`

- [ ] **Step 1:** 创建 ForwardingRuleTab 组件

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import dagre from 'dagre'
import { getForwardingPath } from '../api/mihomo'
import type { FlowNode, FlowEdge } from '../api/mihomo'

import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'

const props = defineProps<{
  instanceId: string
}>()

const domain = ref('')
const loading = ref(false)
const nodes = ref<FlowNode[]>([])
const edges = ref<FlowEdge[]>([])

const { fitView } = useVueFlow()

async function handleQuery() {
  if (!domain.value.trim()) return

  loading.value = true
  try {
    const { data } = await getForwardingPath(props.instanceId, domain.value.trim())
    // 使用 dagre 自动布局
    const layouted = applyDagreLayout(data.nodes, data.edges)
    nodes.value = layouted.nodes
    edges.value = layouted.edges
    setTimeout(() => fitView(), 100)
  } catch (error) {
    console.error('查询转发路径失败:', error)
  } finally {
    loading.value = false
  }
}

function applyDagreLayout(nodes: FlowNode[], edges: FlowEdge[]) {
  const g = new dagre.graphlib.Graph()
  g.setDefaultEdgeLabel(() => ({}))
  g.setGraph({ rankdir: 'LR', nodesep: 50, ranksep: 100 })

  for (const node of nodes) {
    g.setNode(node.id, { width: 180, height: 40 })
  }
  for (const edge of edges) {
    g.setEdge(edge.source, edge.target)
  }

  dagre.layout(g)

  return {
    nodes: nodes.map(node => {
      const pos = g.node(node.id)
      return { ...node, position: { x: pos.x - 90, y: pos.y - 20 } }
    }),
    edges
  }
}
</script>

<template>
  <div class="forwarding-rule-tab">
    <div class="query-bar">
      <el-input
        v-model="domain"
        placeholder="请输入域名，如 google.com"
        clearable
        @keyup.enter="handleQuery"
        style="width: 400px"
      >
        <template #prepend>域名</template>
      </el-input>
      <el-button type="primary" @click="handleQuery" :loading="loading">
        查询转发路径
      </el-button>
    </div>

    <div class="flow-container" v-loading="loading">
      <VueFlow
        v-if="nodes.length > 0"
        :nodes="nodes"
        :edges="edges"
        :default-viewport="{ zoom: 0.8, x: 0, y: 0 }"
        fit-view-on-init
      >
        <Background />
        <Controls />
      </VueFlow>
      <el-empty v-else-if="!loading" description="请输入域名查询转发路径" />
    </div>
  </div>
</template>

<style scoped>
.forwarding-rule-tab {
  height: 600px;
  display: flex;
  flex-direction: column;
}
.query-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.flow-container {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}
</style>
```

- [ ] **Step 2:** 在详情页中引入 ForwardingRuleTab

在 `MihomoInstanceDetailView.vue` 的 script setup 中添加 import：

```typescript
import ForwardingRuleTab from '../components/ForwardingRuleTab.vue'
```

在转发规则标签页中使用：

```vue
<el-tab-pane label="转发规则" name="forwarding">
  <ForwardingRuleTab v-if="instance" :instance-id="instanceId" />
</el-tab-pane>
```

- [ ] **Step 3:** 前端编译验证

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 4:** 提交

```bash
git add module-web/frontend/src/components/ForwardingRuleTab.vue module-web/frontend/src/views/MihomoInstanceDetailView.vue
git commit -m "feat(frontend): 实现转发规则标签页和 Vue Flow 流程图"
```

---

## Task 11: 前端 — 推送历史标签页

**Files:**
- Modify: `module-web/frontend/src/views/MihomoInstanceDetailView.vue`

- [ ] **Step 1:** 在 script setup 中添加推送历史数据获取

```typescript
import { getBuildRecords } from '../api/build-pipeline'

const historyRecords = ref<any[]>([])
const historyLoading = ref(false)

async function loadHistory() {
  historyLoading.value = true
  try {
    const { data } = await getBuildRecords()
    // 过滤出推送到当前实例的记录
    historyRecords.value = data.filter((r: any) => r.targetInstanceId === instanceId)
  } catch (error) {
    console.error('获取推送历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

onMounted(() => {
  // ... existing code ...
  loadHistory()
})
```

- [ ] **Step 2:** 实现推送历史标签页内容

```vue
<el-tab-pane label="推送历史" name="history">
  <el-table :data="historyRecords" v-loading="historyLoading" stripe>
    <el-table-column prop="startedAt" label="推送时间" width="180" />
    <el-table-column prop="status" label="状态" width="100">
      <template #default="{ row }">
        <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
          {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="errorMessage" label="错误信息" />
  </el-table>
  <el-empty v-if="historyRecords.length === 0 && !historyLoading" description="暂无推送记录" />
</el-tab-pane>
```

- [ ] **Step 3:** 前端编译验证

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 4:** 提交

```bash
git add module-web/frontend/src/views/MihomoInstanceDetailView.vue
git commit -m "feat(frontend): 实现推送历史标签页"
```

---

## Task 12: 前端 — 列表页跳转链接

**Files:**
- Modify: `module-web/frontend/src/views/MihomoInstanceView.vue`

- [ ] **Step 1:** 在实例列表的名称列添加跳转链接

找到实例名称的列，将其改为可点击的链接：

```vue
<el-table-column prop="name" label="名称">
  <template #default="{ row }">
    <router-link :to="`/mihomo-instances/${row.id}`" class="instance-link">
      {{ row.name }}
    </router-link>
  </template>
</el-table-column>
```

- [ ] **Step 2:** 添加链接样式

```css
.instance-link {
  color: #409eff;
  text-decoration: none;
}
.instance-link:hover {
  text-decoration: underline;
}
```

- [ ] **Step 3:** 前端编译验证

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 4:** 提交

```bash
git add module-web/frontend/src/views/MihomoInstanceView.vue
git commit -m "feat(frontend): 实例列表页名称添加跳转详情页链接"
```

---

## Task 13: 全栈验证

- [ ] **Step 1:** 后端编译

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 2:** 前端构建

Run: `cd module-web/frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 3:** 启动应用并手动测试

Run: `mvn spring-boot:run -pl module-web`

测试步骤：
1. 访问 `http://localhost:8080/#/mihomo-instances`
2. 点击某个实例名称，确认跳转到详情页
3. 查看实例信息标签页，确认信息展示正确
4. 切换到转发规则标签页，输入域名（如 `google.com`）
5. 点击查询，确认流程图正确展示
6. 切换到推送历史标签页，确认记录展示

- [ ] **Step 4:** 最终提交

```bash
git add .
git commit -m "feat: 完成 Mihomo 实例详情页功能"
```
