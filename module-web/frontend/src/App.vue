<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'

const router = useRouter()
const route = useRoute()

const menuItems = [
  { path: '/subscriptions', label: '订阅源管理', icon: 'Link' },
  { path: '/mihomo-instances', label: 'Mihomo 实例', icon: 'Monitor' },
  { path: '/build-pipelines', label: '构建流程', icon: 'SetUp' },
  { path: '/scripts', label: '脚本管理', icon: 'Document' },
]

const currentTitle = computed(() => {
  const item = menuItems.find(m => m.path === route.path)
  return item?.label || 'Clash 订阅管理中心'
})

const handleMenuSelect = (path: string) => {
  router.push(path)
}
</script>

<template>
  <div class="layout">
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
      <div class="header">{{ currentTitle }}</div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>
