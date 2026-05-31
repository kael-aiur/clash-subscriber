<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { updateAuthSession } from '@/auth/session'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const initialized = ref(true)

const setupForm = reactive({ username: '', password: '', confirmPassword: '' })
const loginForm = reactive({ username: '', password: '' })

const isSetupMode = computed(() => !initialized.value)
const title = computed(() => (isSetupMode.value ? '首次使用，创建管理员' : '管理员登录'))

const fallbackPath = '/subscriptions'

const getErrorMessage = (error: unknown, fallback: string) => {
  if (typeof error === 'object' && error !== null) {
    const response = 'response' in error ? error.response : undefined
    if (typeof response === 'object' && response !== null && 'data' in response) {
      const data = response.data
      if (typeof data === 'object' && data !== null && 'message' in data && typeof data.message === 'string') {
        return data.message
      }
    }
  }
  return fallback
}

const getRedirectPath = () => {
  const redirect = route.query.redirect
  if (typeof redirect !== 'string') {
    return fallbackPath
  }
  if (!redirect.startsWith('/') || redirect.startsWith('//') || redirect.startsWith('/auth')) {
    return fallbackPath
  }
  return redirect
}

const replaceWithFallback = async (path: string) => {
  try {
    await router.replace(path)
  } catch {
    if (path === fallbackPath) {
      ElMessage.error('跳转失败')
      return
    }
    try {
      await router.replace(fallbackPath)
    } catch {
      ElMessage.error('跳转失败')
    }
  }
}

const refreshStatus = async () => {
  try {
    const status = await authApi.status()
    initialized.value = status.initialized
    updateAuthSession(status)
    if (status.authenticated) {
      await replaceWithFallback(getRedirectPath())
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '获取认证状态失败'))
  }
}

const submitSetup = async () => {
  if (!setupForm.username.trim()) {
    ElMessage.error('用户名不能为空')
    return
  }
  if (setupForm.password.length < 8) {
    ElMessage.error('密码至少需要 8 位')
    return
  }
  if (setupForm.password !== setupForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    const username = setupForm.username.trim()
    await authApi.setup({ ...setupForm, username })
    ElMessage.success('管理员初始化成功，请登录')
    initialized.value = true
    loginForm.username = username
    loginForm.password = ''
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '初始化失败'))
  } finally {
    loading.value = false
  }
}

const submitLogin = async () => {
  if (!loginForm.username.trim() || !loginForm.password) {
    ElMessage.error('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const status = await authApi.login({ username: loginForm.username.trim(), password: loginForm.password })
    updateAuthSession(status)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '登录失败'))
    loading.value = false
    return
  }

  try {
    await replaceWithFallback(getRedirectPath())
  } finally {
    loading.value = false
  }
}

onMounted(refreshStatus)
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>Clash 订阅管理中心</h1>
      <p>{{ title }}</p>

      <el-form v-if="isSetupMode" label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="setupForm.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="setupForm.password" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="setupForm.confirmPassword" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="auth-submit" @click="submitSetup">创建管理员</el-button>
      </el-form>

      <el-form v-else label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="loginForm.password"
            type="password"
            autocomplete="current-password"
            show-password
            @keyup.enter="submitLogin"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="auth-submit" @click="submitLogin">登录</el-button>
      </el-form>
    </div>
  </div>
</template>
