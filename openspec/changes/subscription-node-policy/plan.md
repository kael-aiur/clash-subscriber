# 订阅源节点采纳规则 实施计划

> **For agentic workers:** 按本计划逐 Task 执行，步骤使用 `- [ ]` 复选框跟踪。每完成一个 Task 提交一次代码。

**Goal:** 让配置组合能为每个选中的订阅源单独配置节点采纳规则，在合并前过滤掉伪节点（到期/流量信息），从源头消除 Mihomo 推送时的 `duplicate name` 冲突。

**Architecture:** 采纳规则作为 `ConfigProfile` 的属性、按订阅源维度存储（`subscriptionRefs`）；在 `ConfigGeneratorServiceImpl` 取回每个订阅源后、交给 `NodeMergeProcessor` 之前，按规则过滤其 `proxies`；关键词匹配逻辑抽成共用的 `NodeFilter`，订阅源采纳与代理组筛选复用同一份。下游（merge、resolveProxyGroups、toYaml）基本不变。

**Tech Stack:** Java 21, Spring Boot, Maven 多模块（module-processor / module-web）, Vue 3 + Element Plus + TypeScript

---

## 文件结构

### 后端（module-processor）
- 新增 `src/main/java/site/kael/clash/processor/model/NodePolicy.java`
- 新增 `src/main/java/site/kael/clash/processor/model/SubscriptionRef.java`
- 修改 `src/main/java/site/kael/clash/processor/model/ConfigProfile.java`
- 新增 `src/main/java/site/kael/clash/processor/util/NodeFilter.java`
- 修改 `src/main/java/site/kael/clash/processor/service/impl/ConfigGeneratorServiceImpl.java`

### 前端（module-web/frontend）
- 修改 `src/api/config-profile.ts`
- 修改 `src/views/ConfigProfileEditView.vue`

### 测试（module-processor）
- 新增 `src/test/java/site/kael/clash/processor/util/NodeFilterTest.java`
- 新增/扩展 `src/test/java/site/kael/clash/processor/service/impl/ConfigGeneratorServiceImplTest.java`（若不存在则新建）

---

## Task 1: 数据模型

**Files:**
- Create: `module-processor/.../model/NodePolicy.java`
- Create: `module-processor/.../model/SubscriptionRef.java`
- Modify: `module-processor/.../model/ConfigProfile.java`

- [ ] **Step 1: 新增 `NodePolicy` 模型**

```java
package site.kael.clash.processor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点采纳规则：控制某订阅源的哪些节点进入最终配置。
 * mode=all    —— 采纳全部节点，排除命中 excludeKeywords 的节点
 * mode=keyword —— 仅采纳命中 matchKeywords 且未被 excludeKeywords 排除的节点；
 *                  matchKeywords 为空时回退为 all 模式
 */
public class NodePolicy {
    public static final String MODE_ALL = "all";
    public static final String MODE_KEYWORD = "keyword";

    private String mode = MODE_ALL;
    private List<String> excludeKeywords = new ArrayList<>();
    private List<String> matchKeywords = new ArrayList<>();

    public NodePolicy() {}

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public List<String> getExcludeKeywords() { return excludeKeywords; }
    public void setExcludeKeywords(List<String> excludeKeywords) { this.excludeKeywords = excludeKeywords; }
    public List<String> getMatchKeywords() { return matchKeywords; }
    public void setMatchKeywords(List<String> matchKeywords) { this.matchKeywords = matchKeywords; }
}
```

- [ ] **Step 2: 新增 `SubscriptionRef` 模型**

```java
package site.kael.clash.processor.model;

/** 配置组合中对一个订阅源的引用，附带该订阅源的节点采纳规则。 */
public class SubscriptionRef {
    private String subscriptionId;
    private NodePolicy nodePolicy = new NodePolicy();

    public SubscriptionRef() {}

    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }
    public NodePolicy getNodePolicy() { return nodePolicy; }
    public void setNodePolicy(NodePolicy nodePolicy) { this.nodePolicy = nodePolicy; }
}
```

- [ ] **Step 3: `ConfigProfile` 增加 `subscriptionRefs`，保留 `subscriptionIds` 兼容读取**

在现有 `subscriptionIds` 字段基础上新增 `subscriptionRefs`，并提供 `getEffectiveSubscriptionRefs()`：优先返回 `subscriptionRefs`；为空时把旧的 `subscriptionIds` 映射为「默认规则（all + 空排除词）」的 refs，保证老数据行为不变。

```java
private List<String> subscriptionIds = new ArrayList<>;   // 保留，用于兼容老数据
private List<SubscriptionRef> subscriptionRefs = new ArrayList<>();

public List<SubscriptionRef> getSubscriptionRefs() { return subscriptionRefs; }
public void setSubscriptionRefs(List<SubscriptionRef> subscriptionRefs) { this.subscriptionRefs = subscriptionRefs; }

/** 获取生效的订阅源引用：优先 subscriptionRefs，为空则从 subscriptionIds 映射为默认规则 */
public List<SubscriptionRef> getEffectiveSubscriptionRefs() {
    if (subscriptionRefs != null && !subscriptionRefs.isEmpty()) {
        return subscriptionRefs;
    }
    if (subscriptionIds == null || subscriptionIds.isEmpty()) {
        return new ArrayList<>();
    }
    List<SubscriptionRef> mapped = new ArrayList<>();
    for (String id : subscriptionIds) {
        SubscriptionRef ref = new SubscriptionRef();
        ref.setSubscriptionId(id);
        mapped.add(ref); // nodePolicy 默认 all + 空 excludeKeywords
    }
    return mapped;
}
```

- [ ] **Step 4: 编译验证**
```bash
mvn -q -pl module-processor compile
```
- [ ] **Step 5: 提交**
```bash
git add module-processor/src/main/java/site/kael/clash/processor/model/NodePolicy.java \
        module-processor/src/main/java/site/kael/clash/processor/model/SubscriptionRef.java \
        module-processor/src/main/java/site/kael/clash/processor/model/ConfigProfile.java
git commit -m "feat(processor): 新增订阅源节点采纳规则数据模型"
```

---

## Task 2: 共用节点过滤器 NodeFilter

**Files:**
- Create: `module-processor/.../util/NodeFilter.java`
- Modify: `module-processor/.../service/impl/ConfigGeneratorServiceImpl.java`（resolveProxyGroups 复用）
- Create: `module-processor/.../util/NodeFilterTest.java`

- [ ] **Step 1: 新增 `NodeFilter`，统一关键词匹配逻辑**

```java
package site.kael.clash.processor.util;

import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.model.NodePolicy;

import java.util.List;
import java.util.stream.Collectors;

public class NodeFilter {

    /** 节点名是否包含任一关键词（不区分大小写；keywords 为空返回 false） */
    public static boolean containsAnyKeyword(String nodeName, List<String> keywords) {
        if (nodeName == null || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String lowerName = nodeName.toLowerCase();
        return keywords.stream().anyMatch(k -> k != null && lowerName.contains(k.toLowerCase()));
    }

    /** 按采纳规则判断单个节点是否被采纳 */
    public static boolean isAccepted(ProxyNode node, NodePolicy policy) {
        if (node == null || node.getName() == null) {
            return false;
        }
        NodePolicy p = policy != null ? policy : new NodePolicy();
        // matchKeywords 为空时回退为 all 模式
        boolean keywordMode = NodePolicy.MODE_KEYWORD.equals(p.getMode())
                && p.getMatchKeywords() != null && !p.getMatchKeywords().isEmpty();
        if (keywordMode && !containsAnyKeyword(node.getName(), p.getMatchKeywords())) {
            return false;
        }
        return !containsAnyKeyword(node.getName(), p.getExcludeKeywords());
    }

    /** 按采纳规则过滤节点列表，返回新列表 */
    public static List<ProxyNode> filter(List<ProxyNode> nodes, NodePolicy policy) {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream().filter(n -> isAccepted(n, policy)).collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: `resolveProxyGroups` 复用 `NodeFilter.containsAnyKeyword`**

把 `ConfigGeneratorServiceImpl` 中的私有方法 `matchKeywords` / `matchAnyKeyword` 删除，`resolveProxyGroups` 内改为调用 `NodeFilter.containsAnyKeyword(...)`，行为保持不变。

- [ ] **Step 3: 编写 `NodeFilterTest`**

覆盖：默认规则（全部采纳）、排除关键词、关键词匹配、排除+匹配叠加、关键词为空回退 all、大小写不敏感、null/空入参。

```bash
mvn -q -pl module-processor test -Dtest=NodeFilterTest
```

- [ ] **Step 4: 提交**
```bash
git add module-processor/src/main/java/site/kael/clash/processor/util/NodeFilter.java \
        module-processor/src/main/java/site/kael/clash/processor/service/impl/ConfigGeneratorServiceImpl.java \
        module-processor/src/test/java/site/kael/clash/processor/util/NodeFilterTest.java
git commit -m "refactor(processor): 抽出共用 NodeFilter，代理组筛选复用"
```

---

## Task 3: 配置生成流程接入采纳规则

**Files:**
- Modify: `module-processor/.../service/impl/ConfigGeneratorServiceImpl.java`

- [ ] **Step 1: `generate` 改用 `getEffectiveSubscriptionRefs` 驱动**

```java
List<SubscriptionRef> refs = profile.getEffectiveSubscriptionRefs();
List<ClashConfig> subscriptionConfigs = fetchAndFilterSubscriptions(refs);
```

- [ ] **Step 2: 新增 `fetchAndFilterSubscriptions`，fetch 后按 nodePolicy 过滤**

```java
private List<ClashConfig> fetchAndFilterSubscriptions(List<SubscriptionRef> refs) {
    List<ClashConfig> configs = new ArrayList<>();
    for (SubscriptionRef ref : refs) {
        try {
            ClashConfig config = subscriptionService.fetch(ref.getSubscriptionId());
            if (config != null) {
                int raw = config.getProxies() != null ? config.getProxies().size() : 0;
                List<ProxyNode> filtered = NodeFilter.filter(config.getProxies(), ref.getNodePolicy());
                config.setProxies(filtered);
                configs.add(config);
                log.info("获取并过滤订阅源: subscriptionId={}, 原始={}, 过滤后={}",
                        ref.getSubscriptionId(), raw, filtered.size());
            }
        } catch (Exception e) {
            log.error("获取订阅源失败: subscriptionId={}", ref.getSubscriptionId(), e);
        }
    }
    return configs;
}
```

> 删除旧的 `fetchSubscriptions(List<String>)`（已被替代）。后续 `nodeMergeProcessor.process` / `resolveProxyGroups` / `toYaml` 无需改动——过滤后的干净节点自然流入 mergedConfig.proxies。

- [ ] **Step 3: 默认行为回归确认**

默认规则（all + 空排除词）下 `NodeFilter.filter` 返回全部节点，与改造前 `addAll` 全部节点的结果一致。

- [ ] **Step 4: 编写/扩展 `ConfigGeneratorServiceImplTest`**

新增用例：两个订阅源含同名伪节点「套餐到期：长期有效」，为其中一个配置 excludeKeywords=[到期]，断言生成 YAML 的 proxies 不再包含该伪节点、无重名。默认规则用例断言全量节点都被保留。

```bash
mvn -q -pl module-processor test -Dtest=ConfigGeneratorServiceImplTest
```

- [ ] **Step 5: 提交**
```bash
git add module-processor/src/main/java/site/kael/clash/processor/service/impl/ConfigGeneratorServiceImpl.java \
        module-processor/src/test/java/site/kael/clash/processor/service/impl/ConfigGeneratorServiceImplTest.java
git commit -m "feat(processor): 配置生成按订阅源采纳规则过滤节点"
```

---

## Task 4: 前端类型与编辑界面

**Files:**
- Modify: `module-web/frontend/src/api/config-profile.ts`
- Modify: `module-web/frontend/src/views/ConfigProfileEditView.vue`

- [ ] **Step 1: `api/config-profile.ts` 增类型，`ConfigProfile` 改用 `subscriptionRefs`**

```typescript
export interface NodePolicy {
  mode: 'all' | 'keyword'
  excludeKeywords: string[]
  matchKeywords: string[]
}
export interface SubscriptionRef {
  subscriptionId: string
  nodePolicy: NodePolicy
}
// ConfigProfile 中 subscriptionIds 保留兼容；新增 subscriptionRefs: SubscriptionRef[]
```

- [ ] **Step 2: 订阅源卡片改造为「订阅源 + 采纳规则」列表**

把现在的单一 `el-select multiple v-model="form.subscriptionIds"` 改为：先多选订阅源，选中后渲染成列表，每行一个订阅源 + mode 单选（全部节点 / 关键词匹配）+ 条件渲染的关键词输入 + 排除关键词输入。UI 样式复用代理组配置（`el-radio-group` + `el-select multiple filterable allow-create`）。

- [ ] **Step 3: 编辑态加载还原为表单结构**

`onMounted` 中读取 `data.subscriptionRefs`（为空则从 `subscriptionIds` 映射为默认规则），还原每行 mode 与关键词。

- [ ] **Step 4: 保存时序列化为 `subscriptionRefs`**

`handleSubmit` 中由表单结构生成 `subscriptionRefs` 提交；排除关键词输入框 placeholder 补示例（到期、剩余、流量、余额）。

- [ ] **Step 5: 前端类型检查**
```bash
cd module-web/frontend && npm run build
```

- [ ] **Step 6: 提交**
```bash
git add module-web/frontend/src/api/config-profile.ts module-web/frontend/src/views/ConfigProfileEditView.vue
git commit -m "feat(web): 配置编辑页支持订阅源节点采纳规则"
```

---

## Task 5: 验证

- [ ] **Step 1: 后端全量测试**
```bash
mvn -q test
```
- [ ] **Step 2: 默认规则回归** —— 用老数据（仅 subscriptionIds）生成配置，结果与升级前一致。
- [ ] **Step 3: 排除伪节点** —— 为订阅源配置排除关键词（到期/流量），生成配置 proxies 不含相应伪节点，推送到 Mihomo 不再 400。
- [ ] **Step 4: 老数据加载** —— 现有 `data/config-profiles/*.json` 可正常加载、编辑、保存、生成。

---

## 完成

全部 Task 完成后，运行 `openspec validate subscription-node-policy --strict` 确认无误，即可进入 verify / archive 流程。
