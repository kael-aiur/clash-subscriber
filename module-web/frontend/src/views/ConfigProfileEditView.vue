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

// 规则组引用，包含代理对象映射
interface RuleGroupRefWithMapping {
  ruleGroupId: string
  priority: number
  proxyObjectMappings: Record<string, string>
}

const form = ref<{
  name: string
  description: string
  subscriptionIds: string[]
  proxyGroups: (ProxyGroupConfig & { mode: string })[]
  ruleGroups: RuleGroupRefWithMapping[]
  basicConfig: ClashBasicConfig
  authUsername: string
  authPassword: string
}>({
  name: '',
  description: '',
  subscriptionIds: [],
  proxyGroups: [],
  ruleGroups: [],
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
        ruleGroups: (data.ruleGroups || []).map(rg => ({
          ruleGroupId: rg.ruleGroupId,
          priority: rg.priority,
          proxyObjectMappings: rg.proxyObjectMappings || {},
        })),
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
    excludeKeywords: [],
    includeAll: true,
    url: '',
    interval: 300,
    mode: 'all',
  })
}

const removeProxyGroup = (index: number) => {
  form.value.proxyGroups.splice(index, 1)
}

// 规则组相关方法
const availableRuleGroups = computed(() => {
  const selectedIds = new Set(form.value.ruleGroups.map(rg => rg.ruleGroupId))
  return ruleGroups.value.filter(rg => !selectedIds.has(rg.id))
})

const addRuleGroup = (ruleGroupId: string) => {
  const ruleGroup = ruleGroups.value.find(rg => rg.id === ruleGroupId)
  if (!ruleGroup) return

  // 初始化代理对象映射，默认使用 sourceName
  const proxyObjectMappings: Record<string, string> = {}
  if (ruleGroup.proxyObjects) {
    for (const obj of ruleGroup.proxyObjects) {
      proxyObjectMappings[obj.id] = obj.sourceName
    }
  }

  form.value.ruleGroups.push({
    ruleGroupId,
    priority: form.value.ruleGroups.length,
    proxyObjectMappings,
  })
}

const removeRuleGroup = (index: number) => {
  form.value.ruleGroups.splice(index, 1)
  // 重新设置优先级
  form.value.ruleGroups.forEach((rg, i) => {
    rg.priority = i
  })
}

const moveRuleGroupUp = (index: number) => {
  if (index <= 0) return
  const temp = form.value.ruleGroups[index]
  form.value.ruleGroups[index] = form.value.ruleGroups[index - 1]
  form.value.ruleGroups[index - 1] = temp
  // 重新设置优先级
  form.value.ruleGroups.forEach((rg, i) => {
    rg.priority = i
  })
}

const moveRuleGroupDown = (index: number) => {
  if (index >= form.value.ruleGroups.length - 1) return
  const temp = form.value.ruleGroups[index]
  form.value.ruleGroups[index] = form.value.ruleGroups[index + 1]
  form.value.ruleGroups[index + 1] = temp
  // 重新设置优先级
  form.value.ruleGroups.forEach((rg, i) => {
    rg.priority = i
  })
}

const getRuleGroupById = (id: string): RuleGroup | undefined => {
  return ruleGroups.value.find(rg => rg.id === id)
}

const getProxyGroupNames = (): string[] => {
  return form.value.proxyGroups.map(g => g.name).filter(name => name)
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
        excludeKeywords: g.excludeKeywords,
        includeAll: g.mode === 'all',
        url: g.url,
        interval: g.interval,
      })),
      ruleGroups: form.value.ruleGroups.map(rg => ({
        ruleGroupId: rg.ruleGroupId,
        priority: rg.priority,
        proxyObjectMappings: rg.proxyObjectMappings,
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
  } catch (error: any) {
    const message = error.response?.data?.error || '保存失败'
    ElMessage.error(message)
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
          <el-row :gutter="10" style="margin-top: 10px">
            <el-col :span="24">
              <div class="exclude-keywords-label">排除关键词（包含这些关键词的节点将被排除）：</div>
              <el-select
                v-model="group.excludeKeywords"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入关键词后回车，如：剩余、到期、流量、余额"
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
          <div class="card-header">
            <span>规则组配置</span>
            <el-select
              v-if="availableRuleGroups.length > 0"
              placeholder="添加规则组"
              style="width: 300px"
              @change="addRuleGroup"
              value=""
            >
              <el-option
                v-for="rg in availableRuleGroups"
                :key="rg.id"
                :label="rg.name"
                :value="rg.id"
              />
            </el-select>
          </div>
        </template>

        <div v-if="form.ruleGroups.length === 0" class="empty-tip">
          暂无规则组，请从上方下拉框添加
        </div>

        <div v-for="(rgRef, index) in form.ruleGroups" :key="rgRef.ruleGroupId" class="rule-group-item">
          <div class="rule-group-header">
            <div class="rule-group-info">
              <span class="rule-group-priority">优先级 {{ index + 1 }}</span>
              <span class="rule-group-name">{{ getRuleGroupById(rgRef.ruleGroupId)?.name || '未知规则组' }}</span>
              <el-tag size="small" type="info">{{ getRuleGroupById(rgRef.ruleGroupId)?.rules?.length || 0 }} 条规则</el-tag>
            </div>
            <div class="rule-group-actions">
              <el-button size="small" :disabled="index === 0" @click="moveRuleGroupUp(index)">
                <el-icon><Top /></el-icon>
              </el-button>
              <el-button size="small" :disabled="index === form.ruleGroups.length - 1" @click="moveRuleGroupDown(index)">
                <el-icon><Bottom /></el-icon>
              </el-button>
              <el-button size="small" type="danger" @click="removeRuleGroup(index)">删除</el-button>
            </div>
          </div>

          <!-- 代理对象映射配置 -->
          <div v-if="getRuleGroupById(rgRef.ruleGroupId)?.proxyObjects?.length" class="proxy-object-mappings">
            <div class="mapping-title">代理对象映射：</div>
            <div
              v-for="proxyObj in getRuleGroupById(rgRef.ruleGroupId)?.proxyObjects"
              :key="proxyObj.id"
              class="mapping-item"
            >
              <span class="mapping-source">{{ proxyObj.sourceName }}</span>
              <el-icon><Right /></el-icon>
              <el-select
                v-model="rgRef.proxyObjectMappings[proxyObj.id]"
                placeholder="选择代理组"
                style="width: 200px"
                filterable
              >
                <el-option
                  v-for="groupName in getProxyGroupNames()"
                  :key="groupName"
                  :label="groupName"
                  :value="groupName"
                />
              </el-select>
            </div>
          </div>
        </div>
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
            <el-input-number v-model="form.basicConfig.port" :min="0" :max="65535" />
            <span class="port-tip">设置为 0 表示不启用</span>
          </el-form-item>
          <el-form-item label="SOCKS5 端口">
            <el-input-number v-model="form.basicConfig.socksPort" :min="0" :max="65535" />
            <span class="port-tip">设置为 0 表示不启用</span>
          </el-form-item>
          <el-form-item label="重定向端口">
            <el-input-number v-model="form.basicConfig.redirPort" :min="0" :max="65535" />
            <span class="port-tip">设置为 0 表示不启用</span>
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
.empty-tip {
  text-align: center;
  color: #909399;
  padding: 20px;
}
.rule-group-item {
  margin-bottom: 15px;
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background-color: #fafafa;
}
.rule-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.rule-group-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rule-group-priority {
  font-weight: bold;
  color: #409eff;
}
.rule-group-name {
  font-size: 14px;
}
.rule-group-actions {
  display: flex;
  gap: 5px;
}
.proxy-object-mappings {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #dcdfe6;
}
.mapping-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.mapping-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.mapping-source {
  min-width: 120px;
  font-size: 13px;
  color: #303133;
}
.port-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
.exclude-keywords-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 5px;
}
</style>
