<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { authSession, clearAuthSession } from '@/auth/session'

const router = useRouter()
const route = useRoute()

const menuItems = [
  { path: '/subscriptions', label: '订阅源管理', icon: 'Link' },
  { path: '/mihomo-instances', label: 'Mihomo 实例', icon: 'Monitor' },
  { path: '/build-pipelines', label: '构建流程', icon: 'SetUp' },
  { path: '/scripts', label: '脚本管理', icon: 'Document' },
  { path: '/node-tags', label: '标签管理', icon: 'PriceTag' },
]

const isAuthPage = computed(() => route.path === '/auth')
const currentTitle = computed(() => {
  const item = menuItems.find(m => m.path === route.path)
  return item?.label || 'Clash 订阅管理中心'
})

const handleMenuSelect = (path: string) => {
  router.push(path)
}

const handleLogout = async () => {
  try {
    await authApi.logout()
    ElMessage.success('已退出登录')
  } catch {
    ElMessage.warning('退出登录请求失败，已清除本地登录状态')
  } finally {
    clearAuthSession()
    await router.replace('/auth')
  }
}
</script>

<template>
  <router-view v-if="isAuthPage" />
  <div v-else class="layout">
    <div class="sidebar">
      <div class="logo">Clash 订阅中心</div>
      <el-menu
        :default-active="route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        @select="handleMenuSelect"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </div>
    <div class="main">
      <div class="header">
        <span>{{ currentTitle }}</span>
        <div class="header-actions">
          <span class="header-user">{{ authSession.username }}</span>
          <el-button size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>
