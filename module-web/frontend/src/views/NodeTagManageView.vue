<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { nodeTagApi } from '@/api/nodeTag'
import type { NodeTag } from '@/api/nodeTag'

const tags = ref<NodeTag[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增标签')
const editingId = ref<string | null>(null)

const form = ref({
  name: '',
  priority: 1,
  patterns: [] as string[],
})

const newPattern = ref('')

const loadTags = async () => {
  loading.value = true
  try {
    const res = await nodeTagApi.list()
    tags.value = res.data
  } catch {
    ElMessage.error('加载标签列表失败')
  } finally {
    loading.value = false
  }
}

const openDialog = (tag?: NodeTag) => {
  if (tag) {
    dialogTitle.value = '编辑标签'
    editingId.value = tag.id
    form.value = {
      name: tag.name,
      priority: tag.priority,
      patterns: [...tag.patterns],
    }
  } else {
    dialogTitle.value = '新增标签'
    editingId.value = null
    form.value = { name: '', priority: 1, patterns: [] }
  }
  newPattern.value = ''
  dialogVisible.value = true
}

const addPattern = () => {
  const val = newPattern.value.trim()
  if (!val) return
  if (form.value.patterns.includes(val)) {
    ElMessage.warning('该匹配规则已存在')
    return
  }
  form.value.patterns.push(val)
  newPattern.value = ''
}

const removePattern = (index: number) => {
  form.value.patterns.splice(index, 1)
}

const handleSubmit = async () => {
  if (!form.value.name) {
    ElMessage.warning('请填写标签名称')
    return
  }
  if (form.value.patterns.length === 0) {
    ElMessage.warning('请添加至少一个匹配规则')
    return
  }

  try {
    if (editingId.value) {
      await nodeTagApi.update(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await nodeTagApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadTags()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (tag: NodeTag) => {
  ElMessageBox.confirm(`确定删除标签「${tag.name}」？`, '确认删除', {
    type: 'warning',
  }).then(async () => {
    try {
      await nodeTagApi.delete(tag.id)
      ElMessage.success('删除成功')
      await loadTags()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(loadTags)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>节点标签管理</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        新增标签
      </el-button>
    </div>

    <el-table :data="tags" v-loading="loading" border stripe>
      <el-table-column prop="priority" label="优先级" width="100" sortable />
      <el-table-column prop="name" label="标签名" min-width="120" />
      <el-table-column label="匹配规则" min-width="300">
        <template #default="{ row }">
          <el-tag v-for="p in row.patterns" :key="p" size="small" style="margin: 2px;">{{ p }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form label-width="80px">
        <el-form-item label="标签名" required>
          <el-input v-model="form.name" placeholder="如：美国、香港、日本" />
        </el-form-item>
        <el-form-item label="优先级" required>
          <el-input-number v-model="form.priority" :min="1" :max="999" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">数字越小越优先</span>
        </el-form-item>
        <el-form-item label="匹配规则" required>
          <div style="width: 100%;">
            <div style="display: flex; gap: 8px; margin-bottom: 8px;">
              <el-input
                v-model="newPattern"
                placeholder="输入匹配字符串，如：美国、US、[US]"
                @keyup.enter="addPattern"
                style="flex: 1;"
              />
              <el-button @click="addPattern">添加</el-button>
            </div>
            <div v-if="form.patterns.length > 0" style="display: flex; flex-wrap: wrap; gap: 4px;">
              <el-tag
                v-for="(p, index) in form.patterns"
                :key="index"
                closable
                @close="removePattern(index)"
              >
                {{ p }}
              </el-tag>
            </div>
            <div v-else style="color: #909399; font-size: 12px;">节点名包含任意一个匹配字符串即归入此标签</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
