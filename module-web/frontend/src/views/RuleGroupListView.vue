<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ruleGroupApi } from '@/api/ruleGroup'
import type { RuleGroup } from '@/api/ruleGroup'

const router = useRouter()
const ruleGroups = ref<RuleGroup[]>([])
const loading = ref(false)
const createDialogVisible = ref(false)

const createForm = ref({
  name: '',
  description: '',
})

const loadRuleGroups = async () => {
  loading.value = true
  try {
    const res = await ruleGroupApi.list()
    ruleGroups.value = res.data
  } catch {
    ElMessage.error('加载规则组列表失败')
  } finally {
    loading.value = false
  }
}

const handleView = (rg: RuleGroup) => {
  router.push({ name: 'rule-group-detail', params: { id: rg.id } })
}

const handleDelete = (rg: RuleGroup) => {
  ElMessageBox.confirm(`确定删除规则组「${rg.name}」？`, '确认删除', {
    type: 'warning',
  }).then(async () => {
    try {
      await ruleGroupApi.delete(rg.id)
      ElMessage.success('删除成功')
      await loadRuleGroups()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleCreate = async () => {
  if (!createForm.value.name) {
    ElMessage.warning('请填写规则组名称')
    return
  }
  try {
    const res = await ruleGroupApi.create({
      name: createForm.value.name,
      description: createForm.value.description,
    })
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    createForm.value = { name: '', description: '' }
    router.push({ name: 'rule-group-detail', params: { id: res.data.id } })
  } catch {
    ElMessage.error('创建失败')
  }
}

const formatDate = (date?: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const openCreateDialog = () => {
  createForm.value = { name: '', description: '' }
  createDialogVisible.value = true
}

onMounted(loadRuleGroups)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>规则组管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        手动创建
      </el-button>
    </div>

    <el-table :data="ruleGroups" v-loading="loading" border stripe>
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column label="来源订阅" min-width="150">
        <template #default="{ row }">
          <el-tag v-if="row.sourceSubscriptionId" type="info" size="small">已关联</el-tag>
          <span v-else style="color: #909399;">手动创建</span>
        </template>
      </el-table-column>
      <el-table-column label="规则数" width="100" align="center">
        <template #default="{ row }">{{ row.rules?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="代理对象数" width="110" align="center">
        <template #default="{ row }">{{ row.proxyObjects?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="handleView(row)">查看</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && ruleGroups.length === 0" description="暂无规则组，可以从订阅中提取或手动创建" />

    <!-- 手动创建对话框 -->
    <el-dialog v-model="createDialogVisible" title="手动创建规则组" width="480px">
      <el-form label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" placeholder="输入规则组名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="可选描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>
