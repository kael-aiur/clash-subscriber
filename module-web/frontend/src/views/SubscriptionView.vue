<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { subscriptionApi } from '@/api/subscription'
import type { Subscription, SubscriptionType, ClashConfig, ProxyNode, ProxyGroup } from '@/api/subscription'
import { nodeTagApi } from '@/api/nodeTag'
import type { NodeTag } from '@/api/nodeTag'
import { ruleGroupApi } from '@/api/ruleGroup'
import MaskableText from '@/components/MaskableText.vue'
import TreeNode from '@/components/TreeNode.vue'

const router = useRouter()
const subscriptions = ref<Subscription[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加订阅源')
const editingId = ref<string | null>(null)
const fetchResultVisible = ref(false)
const fetchResult = ref<ClashConfig | null>(null)
const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<ClashConfig | null>(null)
const detailSub = ref<Subscription | null>(null)
const activeTab = ref('basic')
const ruleSearch = ref('')
const ruleTypeFilter = ref('')
const ruleViewMode = ref<'grouped' | 'table'>('grouped')
const ruleExpandedPolicies = ref<string[]>([])
const ruleExpandedTypes = ref<string[]>([]) // 格式: "policy::type"
const probeDomain = ref('')
const probeResult = ref<DomainProbeResult | null>(null)

// 配置关系标签页
const selectedGroup = ref<string | null>(null)
const groupDetailPanelVisible = ref(false)
const groupRuleTypeFilter = ref('')
const rawYamlVisible = ref(false)
const rawYamlContent = ref('')

// 节点标签
const nodeTags = ref<NodeTag[]>([])

interface RegionGroup {
  region: string
  nodes: ProxyNode[]
  count: number
}

interface DomainProbeResult {
  domain: string
  rule: { index: number; type: string; match: string; policy: string }
  nodes: string[]
  specialPolicy: string | null
}

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
  const otherGroup: RegionGroup = { region: '其他', nodes: [], count: 0 }

  for (const node of filteredProxies.value) {
    let matched = false
    for (const tag of nodeTags.value) {
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

const form = ref<Partial<Subscription>>({
  name: '',
  type: 'remote',
  url: '',
  content: '',
  userAgent: '',
  headers: {},
})

const subscriptionTypeOptions: Array<{ label: string; value: SubscriptionType }> = [
  { label: '远程订阅', value: 'remote' },
  { label: '本地订阅', value: 'local' },
]

const headerPairs = ref<Array<{ key: string; value: string }>>([])

const loadSubscriptions = async () => {
  loading.value = true
  try {
    const res = await subscriptionApi.list()
    subscriptions.value = res.data
  } catch {
    ElMessage.error('加载订阅源列表失败')
  } finally {
    loading.value = false
  }
}

const openDialog = async (sub?: Subscription) => {
  if (sub) {
    dialogTitle.value = '编辑订阅源'
    editingId.value = sub.id
    form.value = { ...sub, content: '' }
    headerPairs.value = Object.entries(sub.headers || {}).map(([key, value]) => ({ key, value }))
    if (sub.type === 'local') {
      try {
        const res = await subscriptionApi.getContent(sub.id)
        form.value.content = res.data.content
      } catch {
        ElMessage.warning('读取本地配置失败，请重新粘贴配置')
        form.value.content = ''
      }
    }
  } else {
    dialogTitle.value = '添加订阅源'
    editingId.value = null
    form.value = { name: '', type: 'remote', url: '', content: '', userAgent: '', headers: {} }
    headerPairs.value = []
  }
  dialogVisible.value = true
}

const addHeader = () => {
  headerPairs.value.push({ key: '', value: '' })
}

const removeHeader = (index: number) => {
  headerPairs.value.splice(index, 1)
}

const handleSubmit = async () => {
  if (!form.value.name?.trim()) {
    ElMessage.warning('请填写订阅源名称')
    return
  }
  if (form.value.type === 'remote' && !form.value.url?.trim()) {
    ElMessage.warning('请填写订阅链接')
    return
  }
  if (form.value.type === 'local' && !form.value.content?.trim()) {
    ElMessage.warning('请输入 Clash 配置内容')
    return
  }

  // 构建 headers
  const headers: Record<string, string> = {}
  for (const pair of headerPairs.value) {
    if (pair.key && pair.value) {
      headers[pair.key] = pair.value
    }
  }

  const payload: Partial<Subscription> = {
    id: editingId.value || undefined,
    name: form.value.name.trim(),
    type: form.value.type,
    content: form.value.type === 'local' ? form.value.content : undefined,
    url: form.value.type === 'remote' ? form.value.url?.trim() : undefined,
    userAgent: form.value.type === 'remote' ? form.value.userAgent : undefined,
    headers: form.value.type === 'remote' ? headers : {},
  }

  try {
    if (editingId.value) {
      await subscriptionApi.update(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await subscriptionApi.create(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadSubscriptions()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (sub: Subscription) => {
  ElMessageBox.confirm(`确定删除订阅源「${sub.name}」？`, '确认删除', {
    type: 'warning',
  }).then(async () => {
    try {
      await subscriptionApi.delete(sub.id)
      ElMessage.success('删除成功')
      await loadSubscriptions()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleFetch = async (sub: Subscription) => {
  try {
    const res = await subscriptionApi.fetch(sub.id)
    fetchResult.value = res.data
    fetchResultVisible.value = true
    await loadSubscriptions()
  } catch {
    ElMessage.error('获取订阅失败')
  }
}

const formatDate = (date?: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const openDetail = async (sub: Subscription) => {
  detailSub.value = sub
  detailLoading.value = true
  detailDrawerVisible.value = true
  activeTab.value = 'basic'
  ruleSearch.value = ''
  ruleTypeFilter.value = ''
  ruleViewMode.value = 'grouped'
  ruleExpandedPolicies.value = []
  probeDomain.value = ''
  probeResult.value = null
  selectedGroup.value = null
  groupDetailPanelVisible.value = false
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

const proxyGroupList = computed(() => {
  if (!detailData.value?.proxyGroups) return []
  return Object.entries(detailData.value.proxyGroups).map(([key, group]) => ({
    key,
    ...group as ProxyGroup,
  }))
})

// 规则解析：将字符串规则拆分为结构化对象
const parsedRules = computed(() => {
  if (!detailData.value?.rules) return []
  return detailData.value.rules.map((rule, index) => {
    const parts = typeof rule === 'string' ? rule.split(',') : []
    const type = parts[0] || ''
    const isFinalRule = type === 'MATCH' || type === 'FINAL'
    return {
      index: index + 1,
      type,
      match: isFinalRule ? '' : parts[1] || '',
      policy: isFinalRule ? parts[1] || '' : parts[2] || '',
    }
  })
})

// 规则类型列表（用于筛选下拉框）
const ruleTypes = computed(() => {
  const types = new Set(parsedRules.value.map(r => r.type).filter(Boolean))
  return Array.from(types).sort()
})

// 带搜索和类型筛选的规则列表
const filteredRules = computed(() => {
  let list = parsedRules.value
  if (ruleTypeFilter.value) {
    list = list.filter(r => r.type === ruleTypeFilter.value)
  }
  if (ruleSearch.value) {
    const kw = ruleSearch.value.toLowerCase()
    list = list.filter(r => r.match.toLowerCase().includes(kw) || r.policy.toLowerCase().includes(kw))
  }
  return list
})

const matchDomainRule = (domain: string, rule: { type: string; match: string; policy: string }) => {
  const normalizedDomain = domain.toLowerCase()
  const normalizedMatch = rule.match.toLowerCase()

  switch (rule.type) {
    case 'DOMAIN':
      return normalizedDomain === normalizedMatch
    case 'DOMAIN-SUFFIX':
      return normalizedDomain === normalizedMatch || normalizedDomain.endsWith(`.${normalizedMatch}`)
    case 'DOMAIN-KEYWORD':
      return normalizedDomain.includes(normalizedMatch)
    case 'MATCH':
    case 'FINAL':
      return true
    default:
      return false
  }
}

const resolvePolicyNodes = (policy: string, visited = new Set<string>()): { nodes: string[]; specialPolicy: string | null } => {
  if (policy === 'DIRECT' || policy === 'REJECT') {
    return { nodes: [], specialPolicy: policy }
  }
  if (!detailData.value?.proxyGroups || visited.has(policy)) {
    return { nodes: [policy], specialPolicy: null }
  }

  const group = detailData.value.proxyGroups[policy] as ProxyGroup | undefined
  if (!group) {
    return { nodes: [policy], specialPolicy: null }
  }

  visited.add(policy)
  const nodes: string[] = []
  let specialPolicy: string | null = null
  for (const member of group.proxies || []) {
    const resolved = resolvePolicyNodes(member, new Set(visited))
    nodes.push(...resolved.nodes)
    if (resolved.specialPolicy && !specialPolicy) {
      specialPolicy = resolved.specialPolicy
    }
  }
  return { nodes: Array.from(new Set(nodes)), specialPolicy }
}

const handleProbeDomain = () => {
  const domain = probeDomain.value.trim()
  if (!domain) {
    ElMessage.warning('请输入要探测的域名')
    return
  }

  const matchedRule = parsedRules.value.find(rule => matchDomainRule(domain, rule))
  if (!matchedRule) {
    probeResult.value = null
    ElMessage.warning('没有匹配到规则')
    return
  }

  const resolved = resolvePolicyNodes(matchedRule.policy)
  probeResult.value = {
    domain,
    rule: matchedRule,
    nodes: resolved.nodes,
    specialPolicy: resolved.specialPolicy,
  }
}

// 规则类型中文名称
const ruleTypeLabel = (type: string): string => {
  const map: Record<string, string> = {
    'DOMAIN': '精确匹配',
    'DOMAIN-SUFFIX': '后缀匹配',
    'DOMAIN-KEYWORD': '关键词匹配',
    'IP-CIDR': 'IP 段匹配',
    'IP-CIDR6': 'IPv6 段匹配',
    'GEOIP': '地理位置 IP',
    'SRC-IP-CIDR': '来源 IP 段',
    'SRC-PORT': '来源端口',
    'DST-PORT': '目标端口',
    'PROCESS-NAME': '进程名称',
    'MATCH': '兜底规则',
  }
  return map[type] || type
}

// 按策略分组，组内再按类型分组
const rulesByPolicy = computed(() => {
  const policyMap = new Map<string, Map<string, string[]>>()
  for (const rule of filteredRules.value) {
    const policy = rule.policy || 'UNKNOWN'
    if (!policyMap.has(policy)) policyMap.set(policy, new Map())
    const typeMap = policyMap.get(policy)!
    if (!typeMap.has(rule.type)) typeMap.set(rule.type, [])
    typeMap.get(rule.type)!.push(rule.match)
  }
  return Array.from(policyMap.entries())
    .sort((a, b) => {
      const countA = Array.from(a[1].values()).reduce((s, arr) => s + arr.length, 0)
      const countB = Array.from(b[1].values()).reduce((s, arr) => s + arr.length, 0)
      return countB - countA
    })
    .map(([policy, typeMap]) => {
      const types = Array.from(typeMap.entries())
        .sort((a, b) => b[1].length - a[1].length)
        .map(([type, matches]) => ({ type, matches, count: matches.length }))
      const totalCount = types.reduce((s, t) => s + t.count, 0)
      return { policy, types, count: totalCount }
    })
})

// ---- 配置关系标签页 ----

// 根节点选择：优先 GLOBAL，否则第一个 select 组
const rootGroupName = computed(() => {
  const groups = detailData.value?.proxyGroups
  if (!groups) return null
  if (groups['GLOBAL']) return 'GLOBAL'
  const first = Object.entries(groups).find(([, g]) => (g as ProxyGroup).type === 'select')
  return first ? first[0] : Object.keys(groups)[0] || null
})

// 构建树形数据：从根节点递归构建
interface TreeNode {
  name: string
  type: string
  members: string[]
  children: TreeNode[]
  isSpecial: boolean // DIRECT / REJECT
}

const buildTree = (groupName: string, visited: Set<string>): TreeNode | null => {
  if (visited.has(groupName)) return null // 防止循环引用
  visited.add(groupName)

  // 特殊策略
  if (groupName === 'DIRECT' || groupName === 'REJECT') {
    return { name: groupName, type: 'special', members: [], children: [], isSpecial: true }
  }

  const groups = detailData.value?.proxyGroups
  if (!groups || !groups[groupName]) return null

  const group = groups[groupName] as ProxyGroup
  const children: TreeNode[] = []
  const members = group.proxies || []

  for (const member of members) {
    if (groups[member]) {
      // 是子代理组
      const child = buildTree(member, new Set(visited))
      if (child) children.push(child)
    }
  }

  return { name: groupName, type: group.type, members, children, isSpecial: false }
}

const treeRoot = computed(() => {
  if (!rootGroupName.value) return null
  return buildTree(rootGroupName.value, new Set())
})

// 选中组的详情
const selectedGroupDetail = computed(() => {
  if (!selectedGroup.value || !detailData.value?.proxyGroups) return null
  return detailData.value.proxyGroups[selectedGroup.value] as ProxyGroup | null
})

// 选中组的关联规则
const selectedGroupRules = computed(() => {
  if (!selectedGroup.value) return []
  let rules = parsedRules.value.filter(r => r.policy === selectedGroup.value)
  if (groupRuleTypeFilter.value) {
    rules = rules.filter(r => r.type === groupRuleTypeFilter.value)
  }
  return rules
})

// 选中组关联规则的类型列表
const selectedGroupRuleTypes = computed(() => {
  if (!selectedGroup.value) return []
  const types = new Set(parsedRules.value.filter(r => r.policy === selectedGroup.value).map(r => r.type).filter(Boolean))
  return Array.from(types).sort()
})

// 判断成员是代理组还是代理节点
const isGroupMember = (memberName: string) => {
  return !!(detailData.value?.proxyGroups && detailData.value.proxyGroups[memberName])
}

// 点击选择代理组
const selectGroup = (groupName: string) => {
  if (groupName === 'DIRECT' || groupName === 'REJECT') return
  selectedGroup.value = groupName
  groupDetailPanelVisible.value = true
  groupRuleTypeFilter.value = ''
}

// 从规则列表跳转到代理组
const jumpToGroup = (groupName: string) => {
  activeTab.value = 'relation'
  nextTick(() => selectGroup(groupName))
}

// 查看原始 YAML 配置
const showRawYaml = () => {
  if (!detailData.value?.raw) return
  rawYamlContent.value = yamlStringify(detailData.value.raw)
  rawYamlVisible.value = true
}

// 查看代理组原始配置
const showGroupRawYaml = (groupName: string) => {
  if (!detailData.value?.proxyGroups || !detailData.value.proxyGroups[groupName]) return
  const group = detailData.value.proxyGroups[groupName]
  rawYamlContent.value = yamlStringify(group)
  rawYamlVisible.value = true
}

// 简单的 YAML 序列化
const yamlStringify = (obj: unknown, indent = 0): string => {
  const prefix = '  '.repeat(indent)
  if (Array.isArray(obj)) {
    return obj.map(item => {
      if (typeof item === 'object' && item !== null) {
        const entries = Object.entries(item)
        if (entries.length === 0) return prefix + '- {}'
        const first = entries[0]
        let result = prefix + `- ${first[0]}: ${yamlValue(first[1], indent + 1)}`
        for (let i = 1; i < entries.length; i++) {
          result += '\n' + prefix + `  ${entries[i][0]}: ${yamlValue(entries[i][1], indent + 1)}`
        }
        return result
      }
      return prefix + `- ${yamlValue(item, 0)}`
    }).join('\n')
  }
  if (typeof obj === 'object' && obj !== null) {
    return Object.entries(obj).map(([k, v]) => prefix + `${k}: ${yamlValue(v, indent + 1)}`).join('\n')
  }
  return String(obj)
}

const yamlValue = (val: unknown, indent: number): string => {
  if (Array.isArray(val)) {
    if (val.length === 0) return '[]'
    return '\n' + yamlStringify(val, indent)
  }
  if (typeof val === 'object' && val !== null) {
    const entries = Object.entries(val)
    if (entries.length === 0) return '{}'
    return '\n' + entries.map(([k, v]) => '  '.repeat(indent) + `${k}: ${yamlValue(v, indent + 1)}`).join('\n')
  }
  if (typeof val === 'string' && (val.includes(',') || val.includes(':') || val.includes('#'))) {
    return `"${val}"`
  }
  return String(val ?? '')
}

// 组类型颜色
const groupTypeColor = (type: string): string => {
  const map: Record<string, string> = {
    'select': '#409EFF',
    'url-test': '#67C23A',
    'urltest': '#67C23A',
    'fallback': '#E6A23C',
    'load-balance': '#9B59B6',
  }
  return map[type?.toLowerCase()] || '#909399'
}

// 特殊策略颜色
const policyColor = (policy: string): string => {
  if (policy === 'DIRECT') return '#67C23A'
  if (policy === 'REJECT') return '#F56C6C'
  return ''
}

// 切换规则策略分组展开/折叠
const toggleRulePolicy = (policy: string) => {
  const idx = ruleExpandedPolicies.value.indexOf(policy)
  if (idx >= 0) {
    ruleExpandedPolicies.value.splice(idx, 1)
  } else {
    ruleExpandedPolicies.value.push(policy)
  }
}

// 切换规则类型展开/折叠
const toggleRuleType = (policy: string, type: string) => {
  const key = `${policy}::${type}`
  const idx = ruleExpandedTypes.value.indexOf(key)
  if (idx >= 0) {
    ruleExpandedTypes.value.splice(idx, 1)
  } else {
    ruleExpandedTypes.value.push(key)
  }
}

const isRuleTypeExpanded = (policy: string, type: string) => {
  return ruleExpandedTypes.value.includes(`${policy}::${type}`)
}

const loadNodeTags = async () => {
  try {
    const res = await nodeTagApi.list()
    nodeTags.value = res.data
  } catch {
    // 标签加载失败不影响订阅功能
  }
}

const handleExtractRuleGroup = async (sub: Subscription) => {
  try {
    const res = await ruleGroupApi.extract(sub.id)
    ElMessage.success('提取成功')
    router.push({ name: 'rule-group-detail', params: { id: res.data.id } })
  } catch {
    ElMessage.error('提取失败')
  }
}

onMounted(() => {
  loadSubscriptions()
  loadNodeTags()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h2>订阅源管理</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        添加订阅源
      </el-button>
    </div>

    <el-table :data="subscriptions" v-loading="loading" border stripe>
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'local' ? 'warning' : 'primary'">
            {{ row.type === 'local' ? '本地' : '远程' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" min-width="280">
        <template #default="{ row }">
          <span v-if="row.type === 'local'" class="local-source">本地 Clash 配置</span>
          <MaskableText v-else :text="row.url" />
        </template>
      </el-table-column>
      <el-table-column label="最后获取时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.lastFetchedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="380" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="handleFetch(row)">
            <el-icon><Refresh /></el-icon>
            获取
          </el-button>
          <el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
          <el-button size="small" type="warning" @click="handleExtractRuleGroup(row)">提取规则组</el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form label-width="100px">
        <el-form-item label="订阅类型" required>
          <el-radio-group v-model="form.type">
            <el-radio-button
              v-for="option in subscriptionTypeOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="输入订阅源名称" />
        </el-form-item>

        <template v-if="form.type === 'remote'">
          <el-form-item label="URL" required>
            <el-input v-model="form.url" placeholder="输入订阅链接" />
          </el-form-item>
          <el-form-item label="User-Agent">
            <el-input v-model="form.userAgent" placeholder="自定义 User-Agent（可选）" />
          </el-form-item>
          <el-form-item label="自定义 Headers">
            <div style="width: 100%">
              <div v-for="(pair, index) in headerPairs" :key="index" style="display: flex; gap: 8px; margin-bottom: 8px;">
                <el-input v-model="pair.key" placeholder="Header 名称" style="flex: 1" />
                <el-input v-model="pair.value" placeholder="Header 值" style="flex: 1" />
                <el-button type="danger" :icon="'Delete'" circle @click="removeHeader(index)" />
              </div>
              <el-button size="small" @click="addHeader">添加 Header</el-button>
            </div>
          </el-form-item>
        </template>

        <el-form-item v-else label="配置内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="16"
            :autosize="{ minRows: 12, maxRows: 28 }"
            placeholder="粘贴完整的 Clash YAML 配置..."
            spellcheck="false"
            class="subscription-yaml-input"
          />
          <div class="config-hint">支持完整 Clash YAML 配置；保存时会校验内容格式。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 获取结果对话框 -->
    <el-dialog v-model="fetchResultVisible" title="获取结果" width="500px">
      <div v-if="fetchResult">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="配置名称">{{ fetchResult.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="代理节点数">{{ fetchResult.proxies?.length || 0 }}</el-descriptions-item>
          <el-descriptions-item label="代理组数">{{ Object.keys(fetchResult.proxyGroups || {}).length }}</el-descriptions-item>
          <el-descriptions-item label="规则数">{{ fetchResult.rules?.length || 0 }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="fetchResultVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" :title="`订阅详情 - ${detailSub?.name || ''}`" size="80%">
      <div v-loading="detailLoading">
        <el-tabs v-model="activeTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="配置名称">{{ detailData?.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="订阅类型">{{ detailSub?.type === 'local' ? '本地订阅' : '远程订阅' }}</el-descriptions-item>
              <el-descriptions-item label="订阅来源">{{ detailSub?.type === 'local' ? '保存的 Clash 配置' : (detailSub?.url || '-') }}</el-descriptions-item>
              <el-descriptions-item label="User-Agent">{{ detailSub?.type === 'local' ? '-' : (detailSub?.userAgent || '-') }}</el-descriptions-item>
              <el-descriptions-item label="最后获取时间">{{ formatDate(detailSub?.lastFetchedAt) }}</el-descriptions-item>
              <el-descriptions-item v-if="detailSub?.type !== 'local'" label="自定义 Headers">
                <template v-if="detailSub?.headers && Object.keys(detailSub.headers).length">
                  <el-tag v-for="(value, key) in detailSub.headers" :key="key" style="margin: 2px;">{{ key }}: {{ value }}</el-tag>
                </template>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="代理节点数">{{ detailData?.proxies?.length || 0 }}</el-descriptions-item>
              <el-descriptions-item label="节点组数">{{ Object.keys(detailData?.proxyGroups || {}).length }}</el-descriptions-item>
              <el-descriptions-item label="规则数">{{ detailData?.rules?.length || 0 }}</el-descriptions-item>
            </el-descriptions>
            <el-button style="margin-top: 12px;" @click="showRawYaml">查看原始配置</el-button>
          </el-tab-pane>

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
                      <span style="font-weight: 600;">{{ group.region }}</span>
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

          <!-- 节点组（增强版） -->
          <el-tab-pane :label="`节点组 (${proxyGroupList.length})`" name="groups">
            <el-table :data="proxyGroupList" border stripe>
              <el-table-column type="expand">
                <template #default="{ row }">
                  <div style="padding: 10px 20px;">
                    <template v-for="member in row.proxies" :key="member">
                      <el-tag
                        :type="member === 'DIRECT' ? 'success' : member === 'REJECT' ? 'danger' : 'info'"
                        style="margin: 2px;"
                        :effect="member === 'DIRECT' || member === 'REJECT' ? 'dark' : 'light'"
                      >
                        {{ member }}
                        <template v-if="isGroupMember(member)">
                          <span style="font-size: 11px; opacity: 0.7;"> [组]</span>
                        </template>
                      </el-tag>
                    </template>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="name" label="组名" min-width="200" />
              <el-table-column label="类型" width="140">
                <template #default="{ row }">
                  <el-tag :color="groupTypeColor(row.type)" effect="dark" size="small" style="color: #fff; border: none;">
                    {{ row.type }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="节点数" width="100">
                <template #default="{ row }">{{ row.proxies?.length || 0 }}</template>
              </el-table-column>
              <el-table-column label="测速间隔" width="120">
                <template #default="{ row }">{{ row.interval ? row.interval + 's' : '-' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 规则（按策略分组） -->
          <el-tab-pane :label="`规则 (${detailData?.rules?.length || 0})`" name="rules">
            <div style="display: flex; gap: 12px; margin-bottom: 12px;">
              <el-input v-model="ruleSearch" placeholder="搜索规则（匹配值或策略）" clearable style="flex: 1;" />
              <el-select v-model="ruleTypeFilter" placeholder="按类型筛选" clearable style="width: 180px;">
                <el-option v-for="t in ruleTypes" :key="t" :label="t" :value="t" />
              </el-select>
              <el-radio-group v-model="ruleViewMode" size="default">
                <el-radio-button value="grouped">分组</el-radio-button>
                <el-radio-button value="table">列表</el-radio-button>
              </el-radio-group>
            </div>

            <!-- 分组视图 -->
            <div v-if="ruleViewMode === 'grouped'" class="rule-groups">
              <div class="rule-group-summary">
                <el-tag
                  v-for="group in rulesByPolicy"
                  :key="group.policy"
                  :type="group.policy === 'DIRECT' ? 'success' : group.policy === 'REJECT' ? 'danger' : ''"
                  :effect="group.policy === 'DIRECT' || group.policy === 'REJECT' ? 'dark' : 'plain'"
                  style="margin: 2px; cursor: pointer;"
                  @click="toggleRulePolicy(group.policy)"
                >
                  {{ group.policy }} ({{ group.count }})
                </el-tag>
              </div>
              <el-collapse v-model="ruleExpandedPolicies" style="margin-top: 12px;">
                <el-collapse-item
                  v-for="group in rulesByPolicy"
                  :key="group.policy"
                  :name="group.policy"
                >
                  <template #title>
                    <span style="display: flex; align-items: center; gap: 8px;">
                      <span
                        :style="{ color: policyColor(group.policy), fontWeight: '600' }"
                        style="cursor: pointer;"
                        @click.stop="policyColor(group.policy) ? null : jumpToGroup(group.policy)"
                      >{{ group.policy }}</span>
                      <el-tag size="small" type="info">{{ group.count }} 条</el-tag>
                    </span>
                  </template>
                  <div class="rule-type-groups">
                    <div v-for="typeGroup in group.types" :key="typeGroup.type" class="rule-type-group">
                      <div class="rule-type-header" @click="toggleRuleType(group.policy, typeGroup.type)">
                        <el-icon class="rule-type-arrow" :class="{ 'is-expanded': isRuleTypeExpanded(group.policy, typeGroup.type) }"><ArrowRight /></el-icon>
                        <span class="rule-type-label">{{ ruleTypeLabel(typeGroup.type) }}</span>
                        <span class="rule-type-code">({{ typeGroup.type }})</span>
                        <el-tag size="small" type="info" style="margin-left: 8px;">{{ typeGroup.count }}</el-tag>
                      </div>
                      <div v-if="isRuleTypeExpanded(group.policy, typeGroup.type)" class="rule-type-matches">
                        <span v-for="(match, i) in typeGroup.matches" :key="i" class="rule-match-tag">{{ match }}</span>
                      </div>
                    </div>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>

            <!-- 列表视图 -->
            <div v-else>
              <el-table :data="filteredRules" border stripe max-height="500">
                <el-table-column prop="index" label="#" width="60" />
                <el-table-column prop="type" label="类型" width="160" />
                <el-table-column prop="match" label="匹配值" min-width="300" show-overflow-tooltip />
                <el-table-column label="策略" width="150">
                  <template #default="{ row }">
                    <span
                      :style="{ color: policyColor(row.policy), cursor: policyColor(row.policy) ? 'pointer' : 'default', fontWeight: policyColor(row.policy) ? 'bold' : 'normal' }"
                      @click="policyColor(row.policy) ? null : jumpToGroup(row.policy)"
                    >{{ row.policy }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <!-- 域名探测 -->
          <el-tab-pane label="域名探测" name="domain-probe">
            <div class="domain-probe">
              <div class="probe-form">
                <el-input
                  v-model="probeDomain"
                  placeholder="输入域名，如 github.com"
                  clearable
                  style="max-width: 420px;"
                  @keyup.enter="handleProbeDomain"
                >
                  <template #prepend>域名</template>
                </el-input>
                <el-button type="primary" @click="handleProbeDomain">探测使用节点</el-button>
              </div>

              <el-empty v-if="!probeResult" description="输入域名后查看命中规则和最终可用节点" />
              <div v-else class="probe-result">
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="探测域名">{{ probeResult.domain }}</el-descriptions-item>
                  <el-descriptions-item label="命中策略">{{ probeResult.rule.policy }}</el-descriptions-item>
                  <el-descriptions-item label="命中规则" :span="2">
                    {{ probeResult.rule.type }}{{ probeResult.rule.match ? `,${probeResult.rule.match}` : '' }},{{ probeResult.rule.policy }}
                  </el-descriptions-item>
                  <el-descriptions-item label="特殊策略" :span="2">
                    {{ probeResult.specialPolicy || '-' }}
                  </el-descriptions-item>
                </el-descriptions>

                <div class="probe-node-section">
                  <div class="section-title">使用节点 ({{ probeResult.nodes.length }})</div>
                  <div v-if="probeResult.nodes.length" class="member-list">
                    <el-tag v-for="node in probeResult.nodes" :key="node" type="info" style="margin: 2px;">
                      {{ node }}
                    </el-tag>
                  </div>
                  <div v-else style="color: #909399;">该域名走 {{ probeResult.specialPolicy || probeResult.rule.policy }}</div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 配置关系（合并节点组关系图 + 规则） -->
          <el-tab-pane label="配置关系" name="relation">
            <div v-if="!treeRoot" style="color: #909399; text-align: center; padding: 40px;">
              暂无代理组数据，请先获取订阅配置
            </div>
            <div v-else class="relation-container">
              <!-- 左侧：树形关系图 -->
              <div class="relation-tree">
                <div class="tree-title">代理组关系图</div>
                <div class="tree-content">
                  <template v-if="treeRoot">
                    <TreeNode :node="treeRoot" :depth="0" :selected="selectedGroup" @select="selectGroup" />
                  </template>
                </div>
              </div>

              <!-- 右侧：详情面板 -->
              <div class="relation-detail">
                <div v-if="!groupDetailPanelVisible" class="detail-empty">
                  <el-icon :size="48" color="#c0c4cc"><InfoFilled /></el-icon>
                  <p>点击左侧代理组查看详情</p>
                </div>
                <div v-else>
                  <div class="detail-header">
                    <span class="detail-title">{{ selectedGroup }}</span>
                    <el-tag :color="groupTypeColor(selectedGroupDetail?.type || '')" effect="dark" size="small" style="color: #fff; border: none; margin-left: 8px;">
                      {{ selectedGroupDetail?.type }}
                    </el-tag>
                    <el-button size="small" style="margin-left: auto;" @click="showGroupRawYaml(selectedGroup!)">查看原始配置</el-button>
                  </div>

                  <el-descriptions :column="2" border size="small" style="margin: 12px 0;">
                    <el-descriptions-item label="测速 URL">{{ selectedGroupDetail?.url || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="测速间隔">{{ selectedGroupDetail?.interval ? selectedGroupDetail.interval + 's' : '-' }}</el-descriptions-item>
                  </el-descriptions>

                  <!-- 成员列表 -->
                  <div class="detail-section">
                    <div class="section-title">成员列表</div>
                    <div class="member-list">
                      <template v-for="member in selectedGroupDetail?.proxies || []" :key="member">
                        <el-tag
                          v-if="isGroupMember(member)"
                          type="primary"
                          effect="plain"
                          style="margin: 2px; cursor: pointer;"
                          @click="selectGroup(member)"
                        >
                          {{ member }}
                          <el-icon style="margin-left: 2px;"><Right /></el-icon>
                        </el-tag>
                        <el-tag
                          v-else
                          :type="member === 'DIRECT' ? 'success' : member === 'REJECT' ? 'danger' : 'info'"
                          :effect="member === 'DIRECT' || member === 'REJECT' ? 'dark' : 'light'"
                          style="margin: 2px;"
                        >
                          {{ member }}
                        </el-tag>
                      </template>
                    </div>
                  </div>

                  <!-- 关联规则 -->
                  <div class="detail-section">
                    <div class="section-title">
                      关联规则 ({{ selectedGroupRules.length }})
                      <el-select v-if="selectedGroupRuleTypes.length > 1" v-model="groupRuleTypeFilter" placeholder="按类型筛选" clearable size="small" style="width: 160px; margin-left: 12px;">
                        <el-option v-for="t in selectedGroupRuleTypes" :key="t" :label="t" :value="t" />
                      </el-select>
                    </div>
                    <el-table v-if="selectedGroupRules.length" :data="selectedGroupRules" border size="small" max-height="300">
                      <el-table-column prop="index" label="#" width="60" />
                      <el-table-column prop="type" label="类型" width="160" />
                      <el-table-column prop="match" label="匹配值" min-width="200" show-overflow-tooltip />
                      <el-table-column prop="policy" label="策略" width="120" />
                    </el-table>
                    <div v-else style="color: #909399; padding: 12px 0;">无关联规则</div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <!-- 原始 YAML 弹窗 -->
    <el-dialog v-model="rawYamlVisible" title="原始 YAML 配置" width="700px">
      <pre style="max-height: 500px; overflow: auto; background: #f5f7fa; padding: 16px; border-radius: 4px; font-size: 13px; line-height: 1.5;">{{ rawYamlContent }}</pre>
      <template #footer>
        <el-button @click="rawYamlVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.local-source {
  color: #909399;
}

.subscription-yaml-input :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  line-height: 1.5;
}

.config-hint {
  width: 100%;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.relation-container {
  display: flex;
  gap: 16px;
  height: 600px;
}

.domain-probe {
  padding: 4px 0;
}

.probe-form {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.probe-result {
  max-width: 760px;
}

.probe-node-section {
  margin-top: 16px;
}

.relation-tree {
  width: 360px;
  flex-shrink: 0;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tree-title {
  padding: 10px 16px;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.tree-content {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.relation-detail {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: auto;
  padding: 16px;
}

.detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #c0c4cc;
}

.detail-empty p {
  margin-top: 12px;
  font-size: 14px;
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
}

.detail-section {
  margin-top: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.rule-groups {
  max-height: 550px;
  overflow: auto;
}

.rule-group-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.rule-type-groups {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rule-type-group {
  border-left: 3px solid #e4e7ed;
  padding-left: 8px;
  margin-left: 4px;
}

.rule-type-header {
  display: flex;
  align-items: center;
  padding: 6px 4px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  user-select: none;
}

.rule-type-header:hover {
  background: #f5f7fa;
}

.rule-type-arrow {
  font-size: 12px;
  color: #909399;
  transition: transform 0.2s;
  margin-right: 4px;
}

.rule-type-arrow.is-expanded {
  transform: rotate(90deg);
}

.rule-type-label {
  font-weight: 500;
  color: #303133;
}

.rule-type-code {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}

.rule-type-matches {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 4px 4px 8px 20px;
}

.rule-match-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #f0f2f5;
  border-radius: 3px;
  font-size: 12px;
  color: #606266;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
