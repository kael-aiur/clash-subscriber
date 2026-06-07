<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { authSession, clearAuthSession } from '@/auth/session'

const router = useRouter()
const route = useRoute()

const menuItems = [
  {
    path: '/subscriptions',
    label: '订阅管理',
    icon: 'Link',
    children: [
      { path: '/node-tags', label: '标签管理', icon: 'PriceTag' },
    ],
  },
  {
    path: '/config-profiles',
    label: '配置管理',
    icon: 'Files',
    children: [
      { path: '/scripts', label: '脚本管理', icon: 'Document' },
      { path: '/rule-groups', label: '规则管理', icon: 'List' },
    ],
  },
  { path: '/build-pipelines', label: '配置构建', icon: 'SetUp' },
  { path: '/mihomo-instances', label: '实例管理', icon: 'Monitor' },
]

const isAuthPage = computed(() => route.path === '/auth')
const isFullscreen = computed(() => route.meta.fullscreen === true)
const currentTitle = computed(() => {
  // 优先使用路由 meta 中的 title
  if (route.meta.title) {
    return route.meta.title as string
  }
  // 查找当前路由对应的菜单项（包括子菜单）
  for (const item of menuItems) {
    if (item.path === route.path) {
      return item.label
    }
    if (item.children) {
      const child = item.children.find(c => c.path === route.path)
      if (child) {
        return child.label
      }
    }
  }
  return 'Clash 订阅管理中心'
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
        <template v-for="item in menuItems" :key="item.path">
          <!-- 有子菜单的项 -->
          <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.label }}</span>
            </el-menu-item>
          </el-sub-menu>
          <!-- 没有子菜单的项 -->
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </template>
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
      <div :class="['content', { 'content-fullscreen': isFullscreen }]">
        <router-view />
      </div>
    </div>
  </div>
</template>
