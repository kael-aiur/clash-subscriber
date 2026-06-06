<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { configProfileApi } from '@/api/config-profile'
import type { ConfigProfile, ProxyGroupConfig, ClashBasicConfig } from '@/api/config-profile'
import { subscriptionApi } from '@/api/subscription'
import type { Subscription } from '@/api/subscription'
import { ruleGroupApi } from '@/api/ruleGroup'
import type { RuleGroup } from '@/api/ruleGroup'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => route.params.id !== 'new')
const showBasicConfig = ref(false)
const saving = ref(false)

const form = ref<{
  name: string
  description: string
  subscriptionIds: string[]
  proxyGroups: (ProxyGroupConfig & { mode: string })[]
  ruleGroupIds: string[]
  basicConfig: ClashBasicConfig
  authUsername: string
  authPassword: string
}>({
  name: '',
  description: '',
  subscriptionIds: [],
  proxyGroups: [],
  ruleGroupIds: [],
  basicConfig: {
    mixedPort: 7890,
    port: 7891,
    socksPort: 7892,
    redirPort: 7893,
    allowLan: false,
    mode: 'rule',
    logLevel: 'info',
    externalController: '127.0.0.1:9090',
    secret: '',
  },
  authUsername: '',
  authPassword: '',
})

const subscriptions = ref<Subscription[]>([])
const ruleGroups = ref<RuleGroup[]>([])

onMounted(async () => {
  try {
    const [subRes, rgRes] = await Promise.all([
      subscriptionApi.list(),
      ruleGroupApi.list(),
    ])
    subscriptions.value = subRes.data
    ruleGroups.value = rgRes.data

    if (isEdit.value) {
      const res = await configProfileApi.get(route.params.id as string)
      const data = res.data
      form.value = {
        name: data.name || '',
        description: data.description || '',
        subscriptionIds: data.subscriptionIds || [],
        proxyGroups: (data.proxyGroups || []).map(g => ({
          ...g,
          mode: g.includeAll ? 'all' : (g.matchKeywords?.length > 0 ? 'keyword' : 'select'),
        })),
        ruleGroupIds: (data.ruleGroups || []).map(rg => rg.ruleGroupId),
        basicConfig: data.basicConfig || form.value.basicConfig,
        authUsername: data.authUsername || '',
        authPassword: data.authPassword || '',
      }
    }
  } catch {
    ElMessage.error('加载数据失败')
  }
})

const addProxyGroup = () => {
  form.value.proxyGroups.push({
    name: '',
    type: 'select',
    nodeNames: [],
    matchKeywords: [],
    includeAll: true,
    url: '',
    interval: 300,
    mode: 'all',
  })
}

const removeProxyGroup = (index: number) => {
  form.value.proxyGroups.splice(index, 1)
}

const handleSubmit = async () => {
  if (!form.value.name) {
    ElMessage.warning('请填写配置名称')
    return
  }

  saving.value = true
  try {
    const submitData: Partial<ConfigProfile> = {
      name: form.value.name,
      description: form.value.description,
      subscriptionIds: form.value.subscriptionIds,
      proxyGroups: form.value.proxyGroups.map(g => ({
        name: g.name,
        type: g.type,
        nodeNames: g.nodeNames,
        matchKeywords: g.matchKeywords,
        includeAll: g.mode === 'all',
        url: g.url,
        interval: g.interval,
      })),
      ruleGroups: form.value.ruleGroupIds.map((id, index) => ({
        ruleGroupId: id,
        priority: index,
      })),
      basicConfig: form.value.basicConfig,
      authUsername: form.value.authUsername || undefined,
      authPassword: form.value.authPassword || undefined,
    }

    if (isEdit.value) {
      await configProfileApi.update(route.params.id as string, submitData)
      ElMessage.success('更新成功')
    } else {
      await configProfileApi.create(submitData)
      ElMessage.success('创建成功')
    }
    router.push('/config-profiles')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleCancel = () => {
  router.push('/config-profiles')
}
</script>

<template>
  <div class="config-profile-edit">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑配置' : '新建配置' }}</h2>
    </div>

    <el-form :model="form" label-width="120px">
      <!-- 基本信息 -->
      <el-card class="section">
        <template #header>
          <span>基本信息</span>
        </template>
        <el-form-item label="配置名称" required>
          <el-input v-model="form.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
      </el-card>

      <!-- 订阅源选择 -->
      <el-card class="section">
        <template #header>
          <span>订阅源选择</span>
        </template>
        <el-form-item label="订阅源">
          <el-select v-model="form.subscriptionIds" multiple placeholder="请选择订阅源" style="width: 100%">
            <el-option
              v-for="sub in subscriptions"
              :key="sub.id"
              :label="sub.name"
              :value="sub.id"
            />
          </el-select>
        </el-form-item>
      </el-card>

      <!-- 代理组配置 -->
      <el-card class="section">
        <template #header>
          <div class="card-header">
            <span>代理组配置</span>
            <el-button size="small" @click="addProxyGroup">添加代理组</el-button>
          </div>
        </template>
        <div v-for="(group, index) in form.proxyGroups" :key="index" class="proxy-group-item">
          <el-row :gutter="10">
            <el-col :span="6">
              <el-input v-model="group.name" placeholder="代理组名称" />
            </el-col>
            <el-col :span="4">
              <el-select v-model="group.type" placeholder="类型">
                <el-option label="手动选择" value="select" />
                <el-option label="自动测试" value="url-test" />
                <el-option label="故障转移" value="fallback" />
                <el-option label="负载均衡" value="load-balance" />
              </el-select>
            </el-col>
            <el-col :span="10">
              <el-radio-group v-model="group.mode">
                <el-radio value="all">全部节点</el-radio>
                <el-radio value="keyword">关键词匹配</el-radio>
                <el-radio value="select">手动选择</el-radio>
              </el-radio-group>
            </el-col>
            <el-col :span="4">
              <el-button type="danger" size="small" @click="removeProxyGroup(index)">删除</el-button>
            </el-col>
          </el-row>
          <el-row v-if="group.mode === 'keyword'" :gutter="10" style="margin-top: 10px">
            <el-col :span="24">
              <el-select
                v-model="group.matchKeywords"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入关键词后回车"
                style="width: 100%"
              />
            </el-col>
          </el-row>
          <el-row v-if="group.mode === 'select'" :gutter="10" style="margin-top: 10px">
            <el-col :span="24">
              <el-select
                v-model="group.nodeNames"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入节点名称后回车"
                style="width: 100%"
              />
            </el-col>
          </el-row>
          <el-row v-if="group.type !== 'select'" :gutter="10" style="margin-top: 10px">
            <el-col :span="16">
              <el-input v-model="group.url" placeholder="健康检查 URL（如 http://www.gstatic.com/generate_204）" />
            </el-col>
            <el-col :span="8">
              <el-input-number v-model="group.interval" :min="30" :max="3600" placeholder="间隔（秒）" />
            </el-col>
          </el-row>
        </div>
        <el-empty v-if="form.proxyGroups.length === 0" description="暂无代理组，点击上方按钮添加" :image-size="60" />
      </el-card>

      <!-- 规则组配置 -->
      <el-card class="section">
        <template #header>
          <span>规则组配置</span>
        </template>
        <el-form-item label="规则组">
          <el-select v-model="form.ruleGroupIds" multiple placeholder="请选择规则组" style="width: 100%">
            <el-option
              v-for="rg in ruleGroups"
              :key="rg.id"
              :label="rg.name"
              :value="rg.id"
            />
          </el-select>
        </el-form-item>
      </el-card>

      <!-- 认证配置 -->
      <el-card class="section">
        <template #header>
          <span>认证配置</span>
        </template>
        <el-form-item label="用户名">
          <el-input v-model="form.authUsername" placeholder="留空则不启用认证" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.authPassword" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
      </el-card>

      <!-- 基础信息配置 -->
      <el-card class="section">
        <template #header>
          <div class="card-header">
            <span>基础信息配置</span>
            <el-button size="small" @click="showBasicConfig = !showBasicConfig">
              {{ showBasicConfig ? '收起' : '展开' }}
            </el-button>
          </div>
        </template>
        <template v-if="showBasicConfig">
          <el-form-item label="混合端口">
            <el-input-number v-model="form.basicConfig.mixedPort" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="HTTP 端口">
            <el-input-number v-model="form.basicConfig.port" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="SOCKS5 端口">
            <el-input-number v-model="form.basicConfig.socksPort" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="允许局域网">
            <el-switch v-model="form.basicConfig.allowLan" />
          </el-form-item>
          <el-form-item label="模式">
            <el-select v-model="form.basicConfig.mode">
              <el-option label="规则模式" value="rule" />
              <el-option label="全局模式" value="global" />
              <el-option label="直连模式" value="direct" />
            </el-select>
          </el-form-item>
          <el-form-item label="日志级别">
            <el-select v-model="form.basicConfig.logLevel">
              <el-option label="静默" value="silent" />
              <el-option label="错误" value="error" />
              <el-option label="警告" value="warning" />
              <el-option label="信息" value="info" />
              <el-option label="调试" value="debug" />
            </el-select>
          </el-form-item>
          <el-form-item label="外部控制">
            <el-input v-model="form.basicConfig.externalController" placeholder="127.0.0.1:9090" />
          </el-form-item>
          <el-form-item label="管理密钥">
            <el-input v-model="form.basicConfig.secret" placeholder="留空则无密钥" />
          </el-form-item>
        </template>
      </el-card>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
        <el-button @click="handleCancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.config-profile-edit {
  padding: 0;
}
.section {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.proxy-group-item {
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 4px;
}
</style>
