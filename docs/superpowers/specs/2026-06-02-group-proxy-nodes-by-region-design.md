# 代理节点按地区分组展示 - 设计文档

## 概述

优化订阅源详情页面的代理节点标签页，将平铺的节点表格改为按地区分组展示，支持点击展开/折叠，提升大量节点时的浏览体验。

## 背景

当前代理节点以 `el-table` 平铺展示（名称、类型、服务器、端口），节点多时浏览混乱。规则标签页已实现双层分组（策略 → 规则类型）并支持展开/折叠，用户希望代理节点采用类似的交互模式。

大部分节点名称包含国旗 emoji（如 `🇭🇰 香港 | 00`），为按地区分组提供了可靠的数据基础。

## 设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 分组层级 | 单层（仅地区） | 需求明确为"按地区分组"，双层增加复杂度无明显收益 |
| 地区识别 | 国旗 emoji + 映射表 | 国旗是最可靠的标识，映射表确保中文显示 |
| 无标识节点 | 归入"其他" | 不丢失节点，用户可在"其他"中找到 |
| 组内展示 | 表格（与当前一致） | 保持用户熟悉的交互模式 |
| 实现方案 | 纯前端 + 映射表 | 展示层优化，不涉及后端变更 |

## 实现方案

### 国旗 emoji → 地区映射表

在 `SubscriptionView.vue` 中维护常量映射对象，覆盖 20-30 个常见国旗 emoji：

```typescript
const FLAG_REGION_MAP: Record<string, string> = {
  '🇭🇰': '香港', '🇲🇴': '澳门', '🇹🇼': '台湾',
  '🇯🇵': '日本', '🇰🇷': '韩国', '🇸🇬': '新加坡',
  '🇺🇸': '美国', '🇬🇧': '英国', '🇩🇪': '德国', '🇫🇷': '法国',
  '🇦🇺': '澳大利亚', '🇨🇦': '加拿大', '🇮🇳': '印度',
  '🇷🇺': '俄罗斯', '🇧🇷': '巴西', '🇳🇱': '荷兰',
  '🇹🇭': '泰国', '🇻🇳': '越南', '🇵🇭': '菲律宾',
  '🇲🇾': '马来西亚', '🇮🇩': '印度尼西亚', '🇹🇷': '土耳其',
  '🇦🇷': '阿根廷', '🇨🇱': '智利', '🇿🇦': '南非',
  '🇪🇬': '埃及', '🇳🇬': '尼日利亚', '🇰🇪': '肯尼亚',
}
```

### 分组算法

```typescript
const regionGroups = computed(() => {
  const groups = new Map<string, RegionGroup>()
  const filtered = filteredProxies.value  // 搜索过滤后的节点

  for (const node of filtered) {
    const match = node.name.match(/^(\p{Emoji_Presentation})/u)
    const flag = match?.[1]
    const region = flag ? (FLAG_REGION_MAP[flag] || '其他') : '其他'
    const key = region

    if (!groups.has(key)) {
      groups.set(key, { region, flag: flag || '', nodes: [], count: 0 })
    }
    const group = groups.get(key)!
    group.nodes.push(node)
    group.count++
  }

  return Array.from(groups.values())
    .sort((a, b) => {
      if (a.region === '其他') return 1
      if (b.region === '其他') return -1
      return b.count - a.count  // 按数量降序
    })
})
```

### 数据结构

```typescript
interface RegionGroup {
  region: string       // 地区名（如"香港"）
  flag: string         // 国旗 emoji（如"🇭🇰"），"其他"组为空字符串
  nodes: ProxyNode[]   // 该地区下的节点列表
  count: number        // 节点数量
}
```

### UI 交互

#### 代理节点标签页模板

```vue
<el-tab-pane :label="`代理节点 (${detailData?.proxies?.length || 0})`" name="proxies">
  <!-- 搜索框 -->
  <el-input v-model="proxySearchKeyword" placeholder="搜索节点名称..."
    clearable style="margin-bottom: 12px;" />

  <!-- 地区分组折叠面板 -->
  <el-collapse v-model="expandedRegions" style="margin-top: 12px;">
    <el-collapse-item v-for="group in regionGroups" :key="group.region"
                      :name="group.region">
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
</el-tab-pane>
```

#### 搜索过滤

```typescript
const proxySearchKeyword = ref('')

const filteredProxies = computed(() => {
  const keyword = proxySearchKeyword.value.trim().toLowerCase()
  if (!keyword) return detailData.value?.proxies || []
  return (detailData.value?.proxies || []).filter(
    node => node.name.toLowerCase().includes(keyword)
  )
})
```

#### 展开/折叠状态

```typescript
const expandedRegions = ref<string[]>([])  // 默认全部折叠
```

### 边缘情况

| 场景 | 处理方式 |
|------|----------|
| 节点名称为空或异常 | 归入"其他"组 |
| 搜索无结果 | 空分组列表，显示空状态 |
| 代理节点列表为空 | 隐藏分组，显示空表格 |
| 国旗 emoji 不在映射表中 | 归入"其他"组 |

## 文件变更

仅修改 `module-web/frontend/src/views/SubscriptionView.vue`：

1. **新增**：`FLAG_REGION_MAP` 常量（国旗 → 地区映射）
2. **新增**：`RegionGroup` 接口定义
3. **新增**：`proxySearchKeyword` ref
4. **新增**：`filteredProxies` computed（搜索过滤）
5. **新增**：`regionGroups` computed（分组逻辑）
6. **新增**：`expandedRegions` ref（展开状态）
7. **修改**：代理节点 tab 模板（el-table → el-collapse + el-table）

## 不涉及的变更

- 后端 API 无变更
- `ProxyNode` 类型定义无变更
- 无新增组件文件
- 无新增依赖
