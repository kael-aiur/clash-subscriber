# 代理节点按地区分组展示 - 实施计划

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将订阅源详情页的代理节点从平铺表格改为按国旗 emoji 地区分组的折叠面板，支持搜索过滤。

**Architecture:** 纯前端实现，仅修改 `SubscriptionView.vue`。在 `<script setup>` 中新增国旗映射表、分组 computed 和搜索过滤逻辑；在模板中将代理节点 tab 从 `el-table` 改为 `el-collapse` + `el-table` 结构。参照规则标签页已有的分组展开/折叠模式。

**Tech Stack:** Vue 3 Composition API, Element Plus, TypeScript

---

### Task 1: 国旗映射表与类型定义

**Files:**
- Modify: `module-web/frontend/src/views/SubscriptionView.vue:30-32`（在现有 ref 声明区域之后添加）

- [ ] **Step 1: 添加 FLAG_REGION_MAP 常量和 RegionGroup 接口**

在 `<script setup>` 中的 `rawYamlContent` ref 声明之后（约第 31 行），添加以下代码：

```typescript
// 国旗 emoji → 中文地区名映射
const FLAG_REGION_MAP: Record<string, string> = {
  '🇭🇰': '香港', '🇲🇴': '澳门', '🇹🇼': '台湾',
  '🇨🇳': '中国',
  '🇯🇵': '日本', '🇰🇷': '韩国',
  '🇸🇬': '新加坡', '🇲🇾': '马来西亚', '🇹🇭': '泰国', '🇻🇳': '越南',
  '🇵🇭': '菲律宾', '🇮🇩': '印度尼西亚',
  '🇺🇸': '美国', '🇨🇦': '加拿大',
  '🇬🇧': '英国', '🇩🇪': '德国', '🇫🇷': '法国', '🇳🇱': '荷兰',
  '🇷🇺': '俄罗斯',
  '🇦🇺': '澳大利亚', '🇳🇿': '新西兰',
  '🇮🇳': '印度', '🇵🇰': '巴基斯坦',
  '🇧🇷': '巴西', '🇦🇷': '阿根廷', '🇨🇱': '智利',
  '🇹🇷': '土耳其', '🇮🇱': '以色列', '🇦🇪': '阿联酋',
  '🇿🇦': '南非', '🇪🇬': '埃及', '🇳🇬': '尼日利亚', '🇰🇪': '肯尼亚',
}

interface RegionGroup {
  region: string
  flag: string
  nodes: ProxyNode[]
  count: number
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd module-web/frontend && npx vue-tsc --noEmit 2>&1 | head -20`
Expected: 无新增类型错误

- [ ] **Step 3: 提交**

```bash
git add module-web/frontend/src/views/SubscriptionView.vue
git commit -m "feat(web): 添加国旗映射表和地区分组类型定义"
```

---

### Task 2: 搜索过滤与分组逻辑

**Files:**
- Modify: `module-web/frontend/src/views/SubscriptionView.vue`（在 `rawYamlContent` ref 之后、`form` ref 之前添加）

- [ ] **Step 1: 添加代理节点搜索 ref 和过滤 computed**

在 Task 1 添加的代码之后，添加：

```typescript
// 代理节点地区分组
const proxySearchKeyword = ref('')
const expandedRegions = ref<string[]>([])

const filteredProxies = computed(() => {
  const keyword = proxySearchKeyword.value.trim().toLowerCase()
  const proxies = detailData.value?.proxies || []
  if (!keyword) return proxies
  return proxies.filter(node => node.name.toLowerCase().includes(keyword))
})

const regionGroups = computed<RegionGroup[]>(() => {
  const groups = new Map<string, RegionGroup>()

  for (const node of filteredProxies.value) {
    // 匹配节点名称开头的国旗 emoji
    const match = node.name.match(/^(\p{Emoji_Presentation})/u)
    const flag = match?.[1] ?? ''
    const region = flag ? (FLAG_REGION_MAP[flag] || '其他') : '其他'
    const key = region

    if (!groups.has(key)) {
      groups.set(key, { region, flag: key === '其他' ? '' : flag, nodes: [], count: 0 })
    }
    const group = groups.get(key)!
    group.nodes.push(node)
    group.count++
  }

  return Array.from(groups.values()).sort((a, b) => {
    if (a.region === '其他') return 1
    if (b.region === '其他') return -1
    return b.count - a.count
  })
})
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd module-web/frontend && npx vue-tsc --noEmit 2>&1 | head -20`
Expected: 无新增类型错误

- [ ] **Step 3: 提交**

```bash
git add module-web/frontend/src/views/SubscriptionView.vue
git commit -m "feat(web): 添加代理节点搜索过滤和地区分组逻辑"
```

---

### Task 3: UI 模板改造

**Files:**
- Modify: `module-web/frontend/src/views/SubscriptionView.vue:538-546`（代理节点 tab 模板区域）

- [ ] **Step 1: 替换代理节点 tab 模板**

将现有的代理节点 tab（第 538-546 行）：

```vue
<!-- 代理节点 -->
<el-tab-pane :label="`代理节点 (${detailData?.proxies?.length || 0})`" name="proxies">
  <el-table :data="detailData?.proxies || []" border stripe max-height="500">
    <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
    <el-table-column prop="type" label="类型" width="100" />
    <el-table-column prop="server" label="服务器" min-width="150" />
    <el-table-column prop="port" label="端口" width="80" />
  </el-table>
</el-tab-pane>
```

替换为：

```vue
<!-- 代理节点（按地区分组） -->
<el-tab-pane :label="`代理节点 (${detailData?.proxies?.length || 0})`" name="proxies">
  <el-input
    v-model="proxySearchKeyword"
    placeholder="搜索节点名称..."
    clearable
    style="margin-bottom: 12px;"
  />
  <template v-if="regionGroups.length > 0">
    <el-collapse v-model="expandedRegions">
      <el-collapse-item
        v-for="group in regionGroups"
        :key="group.region"
        :name="group.region"
      >
        <template #title>
          <span style="display: flex; align-items: center; gap: 8px;">
            <span style="font-weight: 600;">{{ group.flag }} {{ group.region }}</span>
            <el-tag size="small" type="info">{{ group.count }} 个节点</el-tag>
          </span>
        </template>
        <el-table :data="group.nodes" border stripe size="small">
          <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column prop="server" label="服务器" min-width="150" />
          <el-table-column prop="port" label="端口" width="80" />
        </el-table>
      </el-collapse-item>
    </el-collapse>
  </template>
  <el-empty v-else description="暂无代理节点" />
</el-tab-pane>
```

- [ ] **Step 2: 验证前端构建**

Run: `cd module-web/frontend && npm run build 2>&1 | tail -10`
Expected: 构建成功，无错误

- [ ] **Step 3: 提交**

```bash
git add module-web/frontend/src/views/SubscriptionView.vue
git commit -m "feat(web): 代理节点标签页改为按地区分组的折叠面板"
```

---

### Task 4: 构建验证

**Files:**
- Modify: `module-web/frontend/src/views/SubscriptionView.vue`（如有构建错误需修复）

- [ ] **Step 1: 运行完整前端构建**

Run: `cd module-web/frontend && npm run build`
Expected: 构建成功，无类型错误，无 lint 警告

- [ ] **Step 2: 最终提交（如有修复）**

如有修复，提交修复：

```bash
git add module-web/frontend/src/views/SubscriptionView.vue
git commit -m "fix(web): 修复代理节点分组构建问题"
```

如无修复，跳过此步骤。
