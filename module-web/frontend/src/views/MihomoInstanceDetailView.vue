<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { mihomoApi } from '../api/mihomo'
import { buildPipelineApi } from '../api/build-pipeline'
import type { BuildRecord } from '../api/build-pipeline'
import { ArrowLeft } from '@element-plus/icons-vue'
import ForwardingRuleTab from '../components/ForwardingRuleTab.vue'

const route = useRoute()
const router = useRouter()
const instanceId = route.params.id as string
const activeTab = ref('info')

const instance = ref<any>(null)
const loading = ref(false)

const historyRecords = ref<BuildRecord[]>([])
const historyLoading = ref(false)

async function loadHistory() {
  historyLoading.value = true
  try {
    // 获取所有构建流程，筛选出目标为当前实例的
    const { data: pipelines } = await buildPipelineApi.list()
    const targetPipelines = pipelines.filter(p => p.targetInstanceId === instanceId)
    // 获取每个流程的构建记录
    const allRecords: BuildRecord[] = []
    for (const pipeline of targetPipelines) {
      try {
        const { data: records } = await buildPipelineApi.getRecords(pipeline.id)
        allRecords.push(...records)
      } catch (e) {
        console.error(`获取流程 ${pipeline.id} 的记录失败:`, e)
      }
    }
    // 按开始时间倒序排列
    allRecords.sort((a, b) => new Date(b.startedAt).getTime() - new Date(a.startedAt).getTime())
    historyRecords.value = allRecords
  } catch (error) {
    console.error('获取推送历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

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
  loadHistory()
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
        <ForwardingRuleTab v-if="instance" :instance-id="instanceId" />
      </el-tab-pane>
      <el-tab-pane label="推送历史" name="history">
        <el-table :data="historyRecords" v-loading="historyLoading" stripe>
          <el-table-column prop="startedAt" label="推送时间" width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
                {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="errorMessage" label="错误信息" />
        </el-table>
        <el-empty v-if="historyRecords.length === 0 && !historyLoading" description="暂无推送记录" />
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
