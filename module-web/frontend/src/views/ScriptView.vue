<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scriptApi } from '@/api/script'

const scriptNames = ref<string[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加脚本')
const viewDialogVisible = ref(false)
const viewContent = ref('')
const viewName = ref('')

const form = ref({
  name: '',
  content: '',
})

const loadScripts = async () => {
  loading.value = true
  try {
    const res = await scriptApi.list()
    scriptNames.value = res.data
  } catch {
    ElMessage.error('加载脚本列表失败')
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  dialogTitle.value = '添加脚本'
  form.value = { name: '', content: '' }
  dialogVisible.value = true
}

const openEditDialog = async (name: string) => {
  dialogTitle.value = '编辑脚本'
  try {
    const res = await scriptApi.get(name)
    form.value = { name, content: res.data }
    dialogVisible.value = true
  } catch {
    ElMessage.error('获取脚本内容失败')
  }
}

const handleView = async (name: string) => {
  try {
    const res = await scriptApi.get(name)
    viewName.value = name
    viewContent.value = res.data
    viewDialogVisible.value = true
  } catch {
    ElMessage.error('获取脚本内容失败')
  }
}

const handleSubmit = async () => {
  if (!form.value.name || !form.value.content) {
    ElMessage.warning('请填写脚本名称和内容')
    return
  }

  try {
    await scriptApi.save(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadScripts()
  } catch {
    ElMessage.error('保存失败')
  }
}

const handleDelete = (name: string) => {
  ElMessageBox.confirm(`确定删除脚本「${name}」？`, '确认删除', {
    type: 'warning',
  }).then(async () => {
    try {
      await scriptApi.delete(name)
      ElMessage.success('删除成功')
      await loadScripts()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(loadScripts)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>脚本管理</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        添加脚本
      </el-button>
    </div>

    <el-table :data="scriptNames.map(name => ({ name }))" v-loading="loading" border stripe>
      <el-table-column prop="name" label="脚本名称" min-width="300" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="info" @click="handleView(row.name)">
            <el-icon><View /></el-icon>
            查看
          </el-button>
          <el-button size="small" @click="openEditDialog(row.name)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.name)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="脚本名称" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="20"
            placeholder="输入脚本内容..."
            style="font-family: monospace;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看内容对话框 -->
    <el-dialog v-model="viewDialogVisible" :title="`查看脚本: ${viewName}`" width="700px">
      <el-input
        v-model="viewContent"
        type="textarea"
        :rows="25"
        readonly
        style="font-family: monospace;"
      />
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
