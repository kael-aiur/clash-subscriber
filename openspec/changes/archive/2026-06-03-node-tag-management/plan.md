# 节点标签管理 Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development
> to implement this plan task-by-task.

**Goal:** 实现全局节点标签管理功能，支持按节点名匹配规则进行分类，在订阅详情页用标签替代硬编码的 emoji 映射进行节点分组展示。

**Architecture:** 后端在 module-web 中新增 NodeTag 实体（JSON 文件存储），提供 RESTful CRUD API。前端新增标签管理页面，并重写订阅详情页的节点分组逻辑，从后端获取标签配置后按优先级匹配节点名进行分组。

**Tech Stack:** Java 21, Spring Boot, Jackson, Vue 3, TypeScript, Element Plus, Axios

---

## Task 1: NodeTag 后端模型与存储

- [ ] **Step 1.1:** 创建 `NodeTag` 实体类

  文件: `module-web/src/main/java/site/kael/clash/web/model/NodeTag.java`

  ```java
  package site.kael.clash.web.model;

  import java.time.LocalDateTime;
  import java.util.ArrayList;
  import java.util.List;

  public class NodeTag {
      private String id;
      private String name;
      private int priority;
      private List<String> patterns = new ArrayList<>();
      private LocalDateTime createdAt;
      private LocalDateTime updatedAt;

      public NodeTag() {
          this.createdAt = LocalDateTime.now();
          this.updatedAt = LocalDateTime.now();
      }

      // 所有字段的 getter/setter（单行风格，参照 Subscription.java）
  }
  ```

  参照: `module-subscription/src/main/java/site/kael/clash/subscription/model/Subscription.java`

- [ ] **Step 1.2:** 创建 `NodeTagRepository` 接口

  文件: `module-web/src/main/java/site/kael/clash/web/repository/NodeTagRepository.java`

  ```java
  package site.kael.clash.web.repository;

  import site.kael.clash.web.model.NodeTag;
  import java.util.List;
  import java.util.Optional;

  public interface NodeTagRepository {
      NodeTag save(NodeTag nodeTag);
      Optional<NodeTag> findById(String id);
      List<NodeTag> findAll();
      void deleteById(String id);
  }
  ```

- [ ] **Step 1.3:** 创建 `JsonFileNodeTagRepository` 实现

  文件: `module-web/src/main/java/site/kael/clash/web/repository/JsonFileNodeTagRepository.java`

  参照 `JsonFileSubscriptionRepository.java` 的完整模式:
  - `@Repository` 注解
  - 构造函数注入 `@Value("${data.path:data}") String dataPath`
  - 构造函数中 `new File(dataPath + "/node-tags").mkdirs()`
  - 使用 `ObjectMapper` + `JavaTimeModule`
  - `findAll()` 遍历 `data/node-tags/` 目录下的 `.json` 文件
  - `save()` 写入 `data/node-tags/{id}.json`
  - `deleteById()` 删除文件

  参照: `module-subscription/src/main/java/site/kael/clash/subscription/repository/JsonFileSubscriptionRepository.java`

- [ ] **Step 1.4:** 编译验证

  运行: `mvn compile -pl module-web`

  **提交点:** `feat(node-tag): 添加 NodeTag 模型和仓库层`

---

## Task 2: NodeTag 服务层

- [ ] **Step 2.1:** 创建 `NodeTagService` 接口

  文件: `module-web/src/main/java/site/kael/clash/web/service/NodeTagService.java`

  ```java
  package site.kael.clash.web.service;

  import site.kael.clash.web.model.NodeTag;
  import java.util.List;

  public interface NodeTagService {
      NodeTag create(NodeTag nodeTag);
      NodeTag update(NodeTag nodeTag);
      List<NodeTag> findAll();  // 按 priority 升序返回
      NodeTag findById(String id);
      void deleteById(String id);
  }
  ```

- [ ] **Step 2.2:** 创建 `NodeTagServiceImpl` 实现

  文件: `module-web/src/main/java/site/kael/clash/web/service/impl/NodeTagServiceImpl.java`

  关键实现:
  - `@Service` 注解
  - 构造函数注入 `NodeTagRepository`
  - `create()`: 使用 `IdGenerator.generate()` 生成 ID，设置时间戳
  - `update()`: 查找现有记录，合并字段，更新 `updatedAt`
  - `findAll()`: 从仓库获取后按 `priority` 升序排序
  - `findById()` / `deleteById()`: 委托给仓库，不存在时抛 `BusinessException`

  参照: `module-subscription/src/main/java/site/kael/clash/subscription/service/impl/SubscriptionServiceImpl.java` 的 CRUD 部分

- [ ] **Step 2.3:** 编译验证

  运行: `mvn compile -pl module-web`

  **提交点:** `feat(node-tag): 添加 NodeTag 服务层`

---

## Task 3: NodeTag REST API

- [ ] **Step 3.1:** 创建 `NodeTagController`

  文件: `module-web/src/main/java/site/kael/clash/web/controller/NodeTagController.java`

  ```java
  @RestController
  @RequestMapping("/api/node-tags")
  public class NodeTagController {
      private final NodeTagService nodeTagService;

      public NodeTagController(NodeTagService nodeTagService) {
          this.nodeTagService = nodeTagService;
      }

      @GetMapping
      public ResponseEntity<List<NodeTag>> findAll() { ... }

      @PostMapping
      public ResponseEntity<NodeTag> create(@RequestBody NodeTag nodeTag) { ... }

      @GetMapping("/{id}")
      public ResponseEntity<NodeTag> findById(@PathVariable String id) { ... }

      @PutMapping("/{id}")
      public ResponseEntity<NodeTag> update(@PathVariable String id,
                                            @RequestBody NodeTag nodeTag) { ... }

      @DeleteMapping("/{id}")
      public ResponseEntity<Void> deleteById(@PathVariable String id) { ... }
  }
  ```

  参照: `module-web/src/main/java/site/kael/clash/web/controller/SubscriptionController.java`

- [ ] **Step 3.2:** 编译验证

  运行: `mvn compile -pl module-web`

- [ ] **Step 3.3:** 启动应用并手动测试 API

  运行: `mvn spring-boot:run -pl module-web`

  测试:
  ```bash
  # 创建标签
  curl -X POST http://localhost:8080/api/node-tags \
    -H "Content-Type: application/json" \
    -d '{"name":"美国","priority":1,"patterns":["美国","US","[US]"]}'

  # 查询列表
  curl http://localhost:8080/api/node-tags

  # 更新
  curl -X PUT http://localhost:8080/api/node-tags/{id} \
    -H "Content-Type: application/json" \
    -d '{"name":"美国","priority":1,"patterns":["美国","US","🇺🇸","[US]"]}'

  # 删除
  curl -X DELETE http://localhost:8080/api/node-tags/{id}
  ```

  **提交点:** `feat(node-tag): 添加 NodeTag REST API`

---

## Task 4: 前端 API 与类型定义

- [ ] **Step 4.1:** 创建 `nodeTag.ts` API 文件

  文件: `module-web/frontend/src/api/nodeTag.ts`

  ```typescript
  import api from './index'

  export interface NodeTag {
    id: string
    name: string
    priority: number
    patterns: string[]
    createdAt?: string
    updatedAt?: string
  }

  export const nodeTagApi = {
    list() {
      return api.get<NodeTag[]>('/node-tags')
    },
    create(data: Partial<NodeTag>) {
      return api.post<NodeTag>('/node-tags', data)
    },
    get(id: string) {
      return api.get<NodeTag>(`/node-tags/${id}`)
    },
    update(id: string, data: Partial<NodeTag>) {
      return api.put<NodeTag>(`/node-tags/${id}`, data)
    },
    delete(id: string) {
      return api.delete(`/node-tags/${id}`)
    },
  }
  ```

  参照: `module-web/frontend/src/api/subscription.ts`

  **提交点:** `feat(node-tag): 添加前端 NodeTag API 和类型定义`

---

## Task 5: 订阅详情页节点分组改造

- [ ] **Step 5.1:** 读取 SubscriptionView.vue 完整内容，定位 `FLAG_REGION_MAP` 和 `regionGroups` 的精确位置

  文件: `module-web/frontend/src/views/SubscriptionView.vue`

  需要找到:
  - `FLAG_REGION_MAP` 常量定义（约第 35-49 行）
  - `RegionGroup` 接口定义
  - `regionGroups` computed 属性（约第 69-91 行）
  - 模板中使用 `regionGroups` 的位置

- [ ] **Step 5.2:** 在 SubscriptionView.vue 中导入 nodeTagApi 并加载标签

  在 `<script setup>` 中:
  - 导入 `import { nodeTagApi } from '@/api/nodeTag'`
  - 添加 `const nodeTags = ref<NodeTag[]>([])`
  - 在 `onMounted` 中加载标签列表: `const res = await nodeTagApi.list(); nodeTags.value = res.data`

- [ ] **Step 5.3:** 删除 `FLAG_REGION_MAP` 常量，重写 `regionGroups` computed

  新的 `regionGroups` 逻辑:
  ```typescript
  const regionGroups = computed(() => {
    const groups = new Map<string, { region: string; nodes: ProxyNode[]; count: number }>()
    const otherGroup = { region: '其他', nodes: [] as ProxyNode[], count: 0 }

    for (const node of filteredProxies.value) {
      let matched = false
      for (const tag of nodeTags.value) {  // 已按 priority 排序
        if (tag.patterns.some(p => node.name.includes(p))) {
          const group = groups.get(tag.name) || { region: tag.name, nodes: [], count: 0 }
          group.nodes.push(node)
          group.count++
          groups.set(tag.name, group)
          matched = true
          break
        }
      }
      if (!matched) {
        otherGroup.nodes.push(node)
        otherGroup.count++
      }
    }

    const result = Array.from(groups.values())
    if (otherGroup.count > 0) result.push(otherGroup)
    return result
  })
  ```

- [ ] **Step 5.4:** 检查模板中 `regionGroups` 的使用方式，确保 `RegionGroup` 接口或类型兼容

  如果模板中引用了 `flag` 字段，需要移除或调整（标签没有 flag emoji）。

- [ ] **Step 5.5:** 前端构建验证

  运行: `cd module-web/frontend && npm run build`

  **提交点:** `feat(node-tag): 订阅详情页使用标签进行节点分组`

---

## Task 6: 标签管理页面

- [ ] **Step 6.1:** 创建 `NodeTagManageView.vue`

  文件: `module-web/frontend/src/views/NodeTagManageView.vue`

  页面结构:
  - 顶部操作栏: "新增标签"按钮
  - 表格: 名称 | 优先级 | 匹配规则 | 操作（编辑/删除）
  - 匹配规则列: 用 `el-tag` 组件展示每个 pattern
  - 新增/编辑弹窗 (`el-dialog`):
    - 标签名输入框
    - 优先级数字输入框
    - 匹配规则: 支持添加/删除多个字符串（用 `el-tag` + 输入框）

  参照现有 View 的风格（如 SubscriptionView.vue 的表格和弹窗模式）

- [ ] **Step 6.2:** 在路由中添加标签管理页面

  文件: `module-web/frontend/src/router/index.ts`

  添加路由:
  ```typescript
  { path: '/node-tags', name: 'node-tags', component: () => import('@/views/NodeTagManageView.vue') }
  ```

- [ ] **Step 6.3:** 在侧边栏导航中添加入口

  文件: `module-web/frontend/src/App.vue`

  在 `menuItems` 数组中添加:
  ```typescript
  { path: '/node-tags', label: '标签管理', icon: 'PriceTag' }
  ```

- [ ] **Step 6.4:** 前端构建验证

  运行: `cd module-web/frontend && npm run build`

  **提交点:** `feat(node-tag): 添加标签管理页面`

---

## Task 7: 集成验证

- [ ] **Step 7.1:** 启动完整应用，端到端测试

  运行: `mvn spring-boot:run -pl module-web`

  测试流程:
  1. 访问标签管理页面，创建几个标签（美国、香港、日本等）
  2. 返回订阅源管理，打开某个订阅的详情
  3. 在"代理节点"Tab 中验证节点按标签分组展示
  4. 验证"其他"兜底逻辑正常
  5. 编辑标签（修改匹配规则），返回详情页验证分组更新

- [ ] **Step 7.2:** 运行后端测试

  运行: `mvn test`

  **提交点:** `feat(node-tag): 集成验证完成`
