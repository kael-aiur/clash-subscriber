<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { configProfileApi } from '@/api/config-profile'
import type { ConfigProfile } from '@/api/config-profile'

const router = useRouter()
const configProfiles = ref<ConfigProfile[]>([])
const loading = ref(false)

const loadConfigProfiles = async () => {
  loading.value = true
  try {
    const res = await configProfileApi.list()
    configProfiles.value = res.data
  } catch {
    ElMessage.error('加载配置列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  router.push({ name: 'config-profile-edit', params: { id: 'new' } })
}

const handleEdit = (profile: ConfigProfile) => {
  router.push({ name: 'config-profile-edit', params: { id: profile.id } })
}

const handleCopyLink = (profile: ConfigProfile) => {
  const url = `${window.location.origin}/api/config/${profile.name}`
  navigator.clipboard.writeText(url)
  ElMessage.success('链接已复制')
}

const handleDelete = (profile: ConfigProfile) => {
  ElMessageBox.confirm(`确定删除配置「${profile.name}」？`, '确认删除', {
    type: 'warning',
  }).then(async () => {
    try {
      await configProfileApi.delete(profile.id!)
      ElMessage.success('删除成功')
      await loadConfigProfiles()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const formatDate = (date?: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(loadConfigProfiles)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>配置管理</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建配置
      </el-button>
    </div>

    <el-table :data="configProfiles" v-loading="loading" border stripe>
      <el-table-column prop="name" label="配置名称" min-width="180" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="订阅源" width="100" align="center">
        <template #default="{ row }">{{ row.subscriptionIds?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="代理组" width="100" align="center">
        <template #default="{ row }">{{ row.proxyGroups?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="规则组" width="100" align="center">
        <template #default="{ row }">{{ row.ruleGroups?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" @click="handleCopyLink(row)">复制链接</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && configProfiles.length === 0" description="暂无配置，点击「新建配置」开始创建" />
  </div>
</template>
