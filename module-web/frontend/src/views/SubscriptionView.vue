<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { subscriptionApi } from '@/api/subscription'
import type { Subscription, ClashConfig, ProxyGroup } from '@/api/subscription'
import MaskableText from '@/components/MaskableText.vue'

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

const form = ref<Partial<Subscription>>({
  name: '',
  url: '',
  userAgent: '',
  headers: {},
})

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

const openDialog = (sub?: Subscription) => {
  if (sub) {
    dialogTitle.value = '编辑订阅源'
    editingId.value = sub.id
    form.value = { ...sub }
    headerPairs.value = Object.entries(sub.headers || {}).map(([key, value]) => ({ key, value }))
  } else {
    dialogTitle.value = '添加订阅源'
    editingId.value = null
    form.value = { name: '', url: '', userAgent: '', headers: {} }
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
  if (!form.value.name || !form.value.url) {
    ElMessage.warning('请填写名称和 URL')
    return
  }

  // 构建 headers
  const headers: Record<string, string> = {}
  for (const pair of headerPairs.value) {
    if (pair.key && pair.value) {
      headers[pair.key] = pair.value
    }
  }
  form.value.headers = headers

  try {
    if (editingId.value) {
      await subscriptionApi.update(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await subscriptionApi.create(form.value)
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

onMounted(loadSubscriptions)
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
      <el-table-column label="URL" min-width="300">
        <template #default="{ row }">
          <MaskableText :text="row.url" />
        </template>
      </el-table-column>
      <el-table-column label="最后获取时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.lastFetchedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="handleFetch(row)">
            <el-icon><Refresh /></el-icon>
            获取
          </el-button>
          <el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="输入订阅源名称" />
        </el-form-item>
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
    <el-drawer v-model="detailDrawerVisible" :title="`订阅详情 - ${detailSub?.name || ''}`" size="70%">
      <div v-loading="detailLoading">
        <el-tabs v-model="activeTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="配置名称">{{ detailData?.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="订阅 URL">{{ detailSub?.url || '-' }}</el-descriptions-item>
              <el-descriptions-item label="User-Agent">{{ detailSub?.userAgent || '-' }}</el-descriptions-item>
              <el-descriptions-item label="最后获取时间">{{ formatDate(detailSub?.lastFetchedAt) }}</el-descriptions-item>
              <el-descriptions-item label="自定义 Headers">
                <template v-if="detailSub?.headers && Object.keys(detailSub.headers).length">
                  <el-tag v-for="(value, key) in detailSub.headers" :key="key" style="margin: 2px;">{{ key }}: {{ value }}</el-tag>
                </template>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="代理节点数">{{ detailData?.proxies?.length || 0 }}</el-descriptions-item>
              <el-descriptions-item label="节点组数">{{ Object.keys(detailData?.proxyGroups || {}).length }}</el-descriptions-item>
              <el-descriptions-item label="规则数">{{ detailData?.rules?.length || 0 }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 代理节点 -->
          <el-tab-pane :label="`代理节点 (${detailData?.proxies?.length || 0})`" name="proxies">
            <el-table :data="detailData?.proxies || []" border stripe max-height="500">
              <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
              <el-table-column prop="type" label="类型" width="100" />
              <el-table-column prop="server" label="服务器" min-width="150" />
              <el-table-column prop="port" label="端口" width="80" />
            </el-table>
          </el-tab-pane>

          <!-- 节点组 -->
          <el-tab-pane :label="`节点组 (${proxyGroupList.length})`" name="groups">
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
          </el-tab-pane>

          <!-- 规则 -->
          <el-tab-pane :label="`规则 (${detailData?.rules?.length || 0})`" name="rules">
            <el-input v-model="ruleSearch" placeholder="搜索规则（匹配值或策略）" clearable style="margin-bottom: 12px;" />
            <el-table :data="filteredRules" border stripe max-height="500">
              <el-table-column prop="index" label="#" width="60" />
              <el-table-column prop="type" label="类型" width="160" />
              <el-table-column prop="match" label="匹配值" min-width="300" show-overflow-tooltip />
              <el-table-column prop="policy" label="策略" width="150" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>
