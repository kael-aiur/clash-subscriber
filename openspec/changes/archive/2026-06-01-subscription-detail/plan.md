# 订阅源详情功能 Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 在订阅源管理页面新增详情抽屉，展示代理节点、节点组、规则的完整信息。

**Architecture:** 纯前端变更，在现有 SubscriptionView.vue 中添加详情抽屉组件，复用后端 fetch 接口数据。补充 TypeScript 类型定义以支持模板中的类型安全访问。

**Tech Stack:** Vue 3, TypeScript, Element Plus (el-drawer, el-tabs, el-table, el-tag, el-descriptions)

---

## Task 1: 前端类型定义

- [ ] **Step 1:** 编辑 `module-web/frontend/src/api/subscription.ts`，添加 `ProxyNode` 接口：
```typescript
export interface ProxyNode {
  name: string
  type: string
  server: string
  port: number
  [key: string]: unknown
}
```

- [ ] **Step 2:** 同文件添加 `ProxyGroup` 接口：
```typescript
export interface ProxyGroup {
  name: string
  type: string
  proxies: string[]
  url?: string
  interval?: number
  [key: string]: unknown
}
```

- [ ] **Step 3:** 修改 `ClashConfig` 接口类型：
```typescript
export interface ClashConfig {
  name?: string
  raw?: Record<string, unknown>
  proxies?: ProxyNode[]
  proxyGroups?: Record<string, ProxyGroup>
  rules?: string[]
}
```

**Commit:** `feat(web): 补充 ProxyNode 和 ProxyGroup TypeScript 类型定义`

---

## Task 2: 详情抽屉基础框架

- [ ] **Step 1:** 在 `SubscriptionView.vue` 的 `script setup` 中添加状态变量：
```typescript
const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<ClashConfig | null>(null)
const detailSubName = ref('')
const activeTab = ref('basic')
```

- [ ] **Step 2:** 添加 `openDetail` 方法，调用 fetch 接口并打开抽屉：
```typescript
const openDetail = async (sub: Subscription) => {
  detailSubName.value = sub.name
  detailLoading.value = true
  detailDrawerVisible.value = true
  try {
    const res = await subscriptionApi.fetch(sub.id)
    detailData.value = res.data
  } catch {
    ElMessage.error('获取订阅详情失败')
    detailDrawerVisible.value = false
  } finally {
    detailLoading.value = false
  }
}
```

- [ ] **Step 3:** 在操作列添加"详情"按钮：
```html
<el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
```

- [ ] **Step 4:** 添加抽屉骨架和四个 tab 的基本结构：
```html
<el-drawer v-model="detailDrawerVisible" :title="`订阅详情 - ${detailSubName}`" size="70%">
  <div v-loading="detailLoading">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="basic">...</el-tab-pane>
      <el-tab-pane label="代理节点" name="proxies">...</el-tab-pane>
      <el-tab-pane label="节点组" name="groups">...</el-tab-pane>
      <el-tab-pane label="规则" name="rules">...</el-tab-pane>
    </el-tabs>
  </div>
</el-drawer>
```

**Commit:** `feat(web): 添加订阅详情抽屉基础框架`

---

## Task 3: 基本信息标签页

- [ ] **Step 1:** 在"基本信息" tab-pane 中使用 el-descriptions 展示数据：
```html
<el-descriptions :column="1" border>
  <el-descriptions-item label="配置名称">{{ detailData?.name || '-' }}</el-descriptions-item>
  <el-descriptions-item label="代理节点数">{{ detailData?.proxies?.length || 0 }}</el-descriptions-item>
  <el-descriptions-item label="节点组数">{{ Object.keys(detailData?.proxyGroups || {}).length }}</el-descriptions-item>
  <el-descriptions-item label="规则数">{{ detailData?.rules?.length || 0 }}</el-descriptions-item>
</el-descriptions>
```

**Commit:** `feat(web): 实现基本信息标签页`

---

## Task 4: 代理节点标签页

- [ ] **Step 1:** 在"代理节点" tab-pane 中添加 el-table，列为名称、类型、服务器、端口：
```html
<el-table :data="detailData?.proxies || []" border stripe max-height="500">
  <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
  <el-table-column prop="type" label="类型" width="100" />
  <el-table-column prop="server" label="服务器" min-width="150" />
  <el-table-column prop="port" label="端口" width="80" />
</el-table>
```

- [ ] **Step 2:** 修改 tab label 绑定动态数量：`:label="'代理节点 (' + (detailData?.proxies?.length || 0) + ')'`

**Commit:** `feat(web): 实现代理节点标签页`

---

## Task 5: 节点组标签页

- [ ] **Step 1:** 添加计算属性将 proxyGroups Record 转为数组：
```typescript
const proxyGroupList = computed(() => {
  if (!detailData.value?.proxyGroups) return []
  return Object.entries(detailData.value.proxyGroups).map(([key, group]) => ({
    key,
    ...group as ProxyGroup
  }))
})
```

- [ ] **Step 2:** 在"节点组" tab-pane 中添加带 expand 的 el-table：
```html
<el-table :data="proxyGroupList" border stripe>
  <el-table-column type="expand">
    <template #default="{ row }">
      <div style="padding: 10px 20px;">
        <el-tag v-for="proxy in row.proxies" :key="proxy" style="margin: 2px;">{{ proxy }}</el-tag>
      </div>
    </template>
  </el-table-column>
  <el-table-column prop="name" label="组名" min-width="200" />
  <el-table-column prop="type" label="类型" width="120" />
  <el-table-column label="节点数" width="100">
    <template #default="{ row }">{{ row.proxies?.length || 0 }}</template>
  </el-table-column>
  <el-table-column label="测速间隔" width="120">
    <template #default="{ row }">{{ row.interval ? row.interval + 's' : '-' }}</template>
  </el-table-column>
</el-table>
```

- [ ] **Step 3:** 修改 tab label 绑定动态数量。

**Commit:** `feat(web): 实现节点组标签页`

---

## Task 6: 规则标签页

- [ ] **Step 1:** 添加规则搜索关键词状态和过滤计算属性：
```typescript
const ruleSearch = ref('')
const filteredRules = computed(() => {
  if (!detailData.value?.rules) return []
  const list = detailData.value.rules.map((rule, index) => {
    const parts = typeof rule === 'string' ? rule.split(',') : []
    return { index: index + 1, type: parts[0] || '', match: parts[1] || '', policy: parts[2] || '' }
  })
  if (!ruleSearch.value) return list
  const kw = ruleSearch.value.toLowerCase()
  return list.filter(r => r.match.toLowerCase().includes(kw) || r.policy.toLowerCase().includes(kw))
})
```

- [ ] **Step 2:** 在"规则" tab-pane 添加搜索框和表格：
```html
<el-input v-model="ruleSearch" placeholder="搜索规则（匹配值或策略）" clearable style="margin-bottom: 12px;" />
<el-table :data="filteredRules" border stripe max-height="500">
  <el-table-column prop="index" label="#" width="60" />
  <el-table-column prop="type" label="类型" width="160" />
  <el-table-column prop="match" label="匹配值" min-width="300" show-overflow-tooltip />
  <el-table-column prop="policy" label="策略" width="150" />
</el-table>
```

- [ ] **Step 3:** 修改 tab label 绑定动态数量。

**Commit:** `feat(web): 实现规则标签页`

---

## Task 7: 集成验证

- [ ] **Step 1:** 运行 `cd module-web/frontend && npm run build` 确认无编译错误
- [ ] **Step 2:** 启动开发服务器验证：列表"详情"按钮、抽屉打开、四个标签页数据展示、规则搜索、节点组展开

**Commit:** 无（验证步骤，不产生代码变更）
