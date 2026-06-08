<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ruleGroupApi, parseRule, wrapProxyObjectId } from '@/api/ruleGroup'
import type { RuleGroup, ParsedRule } from '@/api/ruleGroup'

const route = useRoute()
const router = useRouter()
const ruleGroup = ref<RuleGroup | null>(null)
const loading = ref(false)
const saving = ref(false)

// 编辑状态
const editingName = ref('')
const editingDescription = ref('')

// 代理对象编辑
const proxyDialogVisible = ref(false)
const proxyDialogTitle = ref('添加代理对象')
const editingProxyIndex = ref<number | null>(null)
const proxyForm = ref({ id: '', sourceName: '', description: '' })

// 规则编辑
const ruleDialogVisible = ref(false)
const ruleDialogTitle = ref('添加规则')
const editingRuleIndex = ref<number | null>(null)
const ruleForm = ref({ type: '', match: '', proxyObjectId: '' })

// 常见规则类型
const commonRuleTypes = [
  'DOMAIN', 'DOMAIN-SUFFIX', 'DOMAIN-KEYWORD',
  'IP-CIDR', 'IP-CIDR6', 'GEOIP',
  'SRC-IP-CIDR', 'SRC-PORT', 'DST-PORT',
  'PROCESS-NAME', 'MATCH',
]

const ruleTypeLabel = (type: string): string => {
  const map: Record<string, string> = {
    'DOMAIN': '精确匹配', 'DOMAIN-SUFFIX': '后缀匹配', 'DOMAIN-KEYWORD': '关键词匹配',
    'IP-CIDR': 'IP 段', 'IP-CIDR6': 'IPv6 段', 'GEOIP': '地理位置',
    'MATCH': '兜底规则', 'FINAL': '兜底规则',
  }
  return map[type] || type
}

const loadRuleGroup = async () => {
  const id = route.params.id as string
  loading.value = true
  try {
    const res = await ruleGroupApi.get(id)
    ruleGroup.value = res.data
    editingName.value = res.data.name
    editingDescription.value = res.data.description || ''
  } catch {
    ElMessage.error('加载规则组失败')
    router.push({ name: 'rule-groups' })
  } finally {
    loading.value = false
  }
}

// 解析规则列表
const parsedRules = computed<ParsedRule[]>(() => {
  if (!ruleGroup.value) return []
  return (ruleGroup.value.rules ?? []).map((rule, i) => {
    const parsed = parseRule(rule, ruleGroup.value!.proxyObjects ?? [])
    parsed.index = i + 1
    return parsed
  })
})

// ---- 基本信息保存 ----
const handleSaveBasicInfo = async () => {
  if (!ruleGroup.value) return
  saving.value = true
  try {
    await ruleGroupApi.update(ruleGroup.value.id, {
      name: editingName.value,
      description: editingDescription.value,
    })
    ruleGroup.value.name = editingName.value
    ruleGroup.value.description = editingDescription.value
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ---- 代理对象管理 ----
const openAddProxyDialog = () => {
  proxyDialogTitle.value = '添加代理对象'
  editingProxyIndex.value = null
  proxyForm.value = { id: '', sourceName: '', description: '' }
  proxyDialogVisible.value = true
}

const openEditProxyDialog = (index: number) => {
  const obj = ruleGroup.value!.proxyObjects![index]
  proxyDialogTitle.value = '编辑代理对象'
  editingProxyIndex.value = index
  proxyForm.value = { id: obj.id, sourceName: obj.sourceName, description: obj.description || '' }
  proxyDialogVisible.value = true
}

const handleSaveProxy = async () => {
  if (!ruleGroup.value) return
  if (!proxyForm.value.sourceName) {
    ElMessage.warning('请填写源名称')
    return
  }

  const proxyObjects = [...(ruleGroup.value.proxyObjects ?? [])]
  if (editingProxyIndex.value !== null) {
    proxyObjects[editingProxyIndex.value] = {
      ...proxyObjects[editingProxyIndex.value],
      sourceName: proxyForm.value.sourceName,
      description: proxyForm.value.description,
    }
  } else {
    proxyObjects.push({
      id: proxyForm.value.id || '',
      sourceName: proxyForm.value.sourceName,
      description: proxyForm.value.description,
    })
  }

  try {
    const res = await ruleGroupApi.update(ruleGroup.value.id, { proxyObjects })
    ruleGroup.value = res.data
    proxyDialogVisible.value = false
    ElMessage.success(editingProxyIndex.value !== null ? '更新成功' : '添加成功')
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDeleteProxy = (index: number) => {
  const obj = ruleGroup.value!.proxyObjects![index]
  ElMessageBox.confirm(
    `确定删除代理对象「${obj.sourceName} (${obj.id})」？规则中引用此对象的占位符不会自动移除。`,
    '确认删除',
    { type: 'warning' }
  ).then(async () => {
    try {
      const proxyObjects = (ruleGroup.value!.proxyObjects ?? []).filter((_, i) => i !== index)
      const res = await ruleGroupApi.update(ruleGroup.value!.id, { proxyObjects })
      ruleGroup.value = res.data
      ElMessage.success('删除成功')
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// ---- 规则管理 ----
const openAddRuleDialog = () => {
  ruleDialogTitle.value = '添加规则'
  editingRuleIndex.value = null
  ruleForm.value = { type: 'DOMAIN-SUFFIX', match: '', proxyObjectId: '' }
  ruleDialogVisible.value = true
}

const openEditRuleDialog = (index: number) => {
  const ruleStr = ruleGroup.value!.rules![index]
  const parsed = parseRule(ruleStr, ruleGroup.value!.proxyObjects ?? [])
  ruleDialogTitle.value = '编辑规则'
  editingRuleIndex.value = index
  ruleForm.value = {
    type: parsed.type,
    match: parsed.match,
    proxyObjectId: parsed.proxyObjectId || '',
  }
  ruleDialogVisible.value = true
}

const handleSaveRule = async () => {
  if (!ruleGroup.value) return
  if (!ruleForm.value.type) {
    ElMessage.warning('请选择规则类型')
    return
  }
  if (!ruleForm.value.proxyObjectId) {
    ElMessage.warning('请选择代理对象')
    return
  }

  let ruleStr: string
  const proxyRef = wrapProxyObjectId(ruleForm.value.proxyObjectId)
  if (ruleForm.value.type === 'MATCH' || ruleForm.value.type === 'FINAL') {
    ruleStr = `${ruleForm.value.type},${proxyRef}`
  } else {
    if (!ruleForm.value.match) {
      ElMessage.warning('请填写匹配值')
      return
    }
    ruleStr = `${ruleForm.value.type},${ruleForm.value.match},${proxyRef}`
  }

  const rules = [...(ruleGroup.value.rules ?? [])]
  if (editingRuleIndex.value !== null) {
    rules[editingRuleIndex.value] = ruleStr
  } else {
    rules.push(ruleStr)
  }

  try {
    const res = await ruleGroupApi.update(ruleGroup.value.id, { rules })
    ruleGroup.value = res.data
    ruleDialogVisible.value = false
    ElMessage.success(editingRuleIndex.value !== null ? '更新成功' : '添加成功')
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDeleteRule = (index: number) => {
  ElMessageBox.confirm('确定删除此规则？', '确认删除', { type: 'warning' }
  ).then(async () => {
    try {
      const rules = (ruleGroup.value!.rules ?? []).filter((_, i) => i !== index)
      const res = await ruleGroupApi.update(ruleGroup.value!.id, { rules })
      ruleGroup.value = res.data
      ElMessage.success('删除成功')
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleMoveRule = async (index: number, direction: 'up' | 'down') => {
  if (!ruleGroup.value) return
  const rules = [...(ruleGroup.value.rules ?? [])]
  const targetIndex = direction === 'up' ? index - 1 : index + 1
  if (targetIndex < 0 || targetIndex >= rules.length) return
  [rules[index], rules[targetIndex]] = [rules[targetIndex], rules[index]]
  try {
    const res = await ruleGroupApi.update(ruleGroup.value.id, { rules })
    ruleGroup.value = res.data
  } catch {
    ElMessage.error('操作失败')
  }
}

// 代理对象选项（用于规则编辑下拉框）
const proxyOptions = computed(() => {
  if (!ruleGroup.value) return []
  return (ruleGroup.value.proxyObjects ?? []).map(obj => ({
    value: obj.id,
    label: `${obj.sourceName} (${obj.id})`,
  }))
})

onMounted(loadRuleGroup)
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px;">
        <el-button @click="router.push({ name: 'rule-groups' })" text>
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h2 style="margin: 0;">{{ ruleGroup?.name || '规则组详情' }}</h2>
        <el-tag v-if="ruleGroup?.sourceSubscriptionId" type="info" size="small">来自订阅</el-tag>
      </div>
    </div>

    <div v-if="ruleGroup">
      <!-- 来源提示 -->
      <el-alert
        v-if="ruleGroup.sourceSubscriptionId"
        title="此规则组来源于订阅，重新提取将覆盖所有手动修改"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 16px;"
      />

      <!-- 基本信息 -->
      <el-card shadow="never" style="margin-bottom: 16px;">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 600;">基本信息</span>
            <el-button type="primary" size="small" @click="handleSaveBasicInfo" :loading="saving">保存</el-button>
          </div>
        </template>
        <el-form label-width="80px">
          <el-form-item label="名称">
            <el-input v-model="editingName" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="editingDescription" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 代理对象 -->
      <el-card shadow="never" style="margin-bottom: 16px;">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 600;">代理对象 ({{ (ruleGroup.proxyObjects ?? []).length }})</span>
            <el-button type="primary" size="small" @click="openAddProxyDialog">
              <el-icon><Plus /></el-icon> 添加
            </el-button>
          </div>
        </template>
        <el-table :data="ruleGroup.proxyObjects ?? []" border stripe size="small">
          <el-table-column prop="id" label="ID" width="160" />
          <el-table-column prop="sourceName" label="源名称" min-width="200" />
          <el-table-column prop="description" label="描述" min-width="200">
            <template #default="{ row }">{{ row.description || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ $index }">
              <el-button size="small" type="primary" @click="openEditProxyDialog($index)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteProxy($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 规则列表 -->
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: 600;">规则列表 ({{ (ruleGroup.rules ?? []).length }})</span>
            <el-button type="primary" size="small" @click="openAddRuleDialog">
              <el-icon><Plus /></el-icon> 添加
            </el-button>
          </div>
        </template>
        <el-table :data="parsedRules" border stripe size="small">
          <el-table-column prop="index" label="#" width="50" />
          <el-table-column label="类型" width="220">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type }}</el-tag>
              <span style="margin-left: 4px; font-size: 12px; color: #909399;">{{ ruleTypeLabel(row.type) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="match" label="匹配值" min-width="200">
            <template #default="{ row }">{{ row.match || '-' }}</template>
          </el-table-column>
          <el-table-column label="代理对象" min-width="200">
            <template #default="{ row }">
              <span>{{ row.proxyDisplay }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ $index }">
              <el-button size="small" :disabled="$index === 0" @click="handleMoveRule($index, 'up')">上移</el-button>
              <el-button size="small" :disabled="$index === parsedRules.length - 1" @click="handleMoveRule($index, 'down')">下移</el-button>
              <el-button size="small" type="primary" @click="openEditRuleDialog($index)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteRule($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 代理对象编辑弹窗 -->
    <el-dialog v-model="proxyDialogVisible" :title="proxyDialogTitle" width="480px">
      <el-form label-width="80px">
        <el-form-item v-if="editingProxyIndex === null" label="ID">
          <el-input v-model="proxyForm.id" placeholder="留空则自动生成" />
        </el-form-item>
        <el-form-item v-else label="ID">
          <el-input :model-value="proxyForm.id" disabled />
        </el-form-item>
        <el-form-item label="源名称" required>
          <el-input v-model="proxyForm.sourceName" placeholder="如：美国节点、自动选择" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="proxyForm.description" placeholder="可选描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="proxyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveProxy">确定</el-button>
      </template>
    </el-dialog>

    <!-- 规则编辑弹窗 -->
    <el-dialog v-model="ruleDialogVisible" :title="ruleDialogTitle" width="520px">
      <el-form label-width="80px">
        <el-form-item label="类型" required>
          <el-select v-model="ruleForm.type" filterable allow-create placeholder="选择或输入规则类型" style="width: 100%;">
            <el-option v-for="t in commonRuleTypes" :key="t" :label="`${t} (${ruleTypeLabel(t)})`" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="ruleForm.type !== 'MATCH' && ruleForm.type !== 'FINAL'" label="匹配值" required>
          <el-input v-model="ruleForm.match" placeholder="如：google.com、CN" />
        </el-form-item>
        <el-form-item label="代理对象" required>
          <el-select v-model="ruleForm.proxyObjectId" placeholder="选择代理对象" style="width: 100%;">
            <el-option
              v-for="opt in proxyOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
