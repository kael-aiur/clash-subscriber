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
        <!-- 实例信息内容 -->
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
