<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { mihomoApi } from '../api/mihomo'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const instanceId = route.params.id as string
const activeTab = ref('info')

const instance = ref<any>(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await mihomoApi.get(instanceId)
    instance.value = data
  } catch (error) {
    console.error('获取实例信息失败:', error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="mihomo-detail-view" v-loading="loading">
    <div class="page-header">
      <el-button @click="router.push('/mihomo-instances')" text>
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
      <h2>{{ instance?.name || '实例详情' }}</h2>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="实例信息" name="info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">{{ instance?.name }}</el-descriptions-item>
          <el-descriptions-item label="API URL">{{ instance?.apiUrl }}</el-descriptions-item>
          <el-descriptions-item label="API Secret">
            {{ instance?.apiSecret ? '******' : '未设置' }}
          </el-descriptions-item>
          <el-descriptions-item label="启用状态">
            <el-tag :type="instance?.enabled ? 'success' : 'info'">
              {{ instance?.enabled ? '已启用' : '已禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="健康状态">
            <el-tag :type="instance?.status === 'HEALTHY' ? 'success' : instance?.status === 'UNHEALTHY' ? 'danger' : 'warning'">
              {{ instance?.status === 'HEALTHY' ? '健康' : instance?.status === 'UNHEALTHY' ? '异常' : '未知' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最后检查">
            {{ instance?.lastHealthCheck || '未检查' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
      <el-tab-pane label="转发规则" name="forwarding">
        <!-- 转发规则内容 -->
      </el-tab-pane>
      <el-tab-pane label="推送历史" name="history">
        <!-- 推送历史内容 -->
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.mihomo-detail-view {
  padding: 20px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
}
</style>
