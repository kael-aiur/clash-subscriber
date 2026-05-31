import { createRouter, createWebHashHistory } from 'vue-router'
import { setUnauthorizedHandler } from '@/api'
import { authApi } from '@/api/auth'
import { clearAuthSession, updateAuthSession } from '@/auth/session'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/auth',
      name: 'auth',
      component: () => import('@/views/AuthView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      redirect: '/subscriptions',
    },
    {
      path: '/subscriptions',
      name: 'subscriptions',
      component: () => import('@/views/SubscriptionView.vue'),
    },
    {
      path: '/mihomo-instances',
      name: 'mihomo-instances',
      component: () => import('@/views/MihomoInstanceView.vue'),
    },
    {
      path: '/mihomo-instances/:id',
      name: 'MihomoInstanceDetail',
      component: () => import('@/views/MihomoInstanceDetailView.vue'),
      meta: { title: 'Mihomo 实例详情' },
    },
    {
      path: '/scheduled-tasks',
      name: 'scheduled-tasks',
      component: () => import('@/views/ScheduledTaskView.vue'),
    },
    {
      path: '/scripts',
      name: 'scripts',
      component: () => import('@/views/ScriptView.vue'),
    },
    {
      path: '/build-pipelines',
      name: 'build-pipelines',
      component: () => import('@/views/BuildPipelineView.vue'),
    },
  ],
})

setUnauthorizedHandler(() => {
  clearAuthSession()
  const current = router.currentRoute.value
  router.replace({ path: '/auth', query: { redirect: current.fullPath } })
})

router.beforeEach(async (to) => {
  if (to.meta.public) {
    return true
  }

  try {
    const status = await authApi.status()
    updateAuthSession(status)
    if (!status.initialized || !status.authenticated) {
      return { path: '/auth', query: { redirect: to.fullPath } }
    }
    return true
  } catch {
    clearAuthSession()
    return { path: '/auth', query: { redirect: to.fullPath } }
  }
})

export default router
