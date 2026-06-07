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
      path: '/scripts/edit/:name',
      name: 'ScriptEditor',
      component: () => import('@/views/ScriptEditorView.vue'),
      meta: { title: '编辑脚本', fullscreen: true },
    },
    {
      path: '/build-pipelines',
      name: 'build-pipelines',
      component: () => import('@/views/BuildPipelineView.vue'),
    },
    {
      path: '/build-records/:id',
      name: 'build-record-detail',
      component: () => import('@/views/BuildRecordDetailView.vue'),
      meta: { title: '构建记录详情' },
    },
    {
      path: '/node-tags',
      name: 'node-tags',
      component: () => import('@/views/NodeTagManageView.vue'),
    },
    {
      path: '/rule-groups',
      name: 'rule-groups',
      component: () => import('@/views/RuleGroupListView.vue'),
    },
    {
      path: '/rule-groups/:id',
      name: 'rule-group-detail',
      component: () => import('@/views/RuleGroupDetailView.vue'),
      meta: { title: '规则组详情' },
    },
    {
      path: '/config-profiles',
      name: 'config-profiles',
      component: () => import('@/views/ConfigProfileListView.vue'),
    },
    {
      path: '/config-profiles/:id',
      name: 'config-profile-edit',
      component: () => import('@/views/ConfigProfileEditView.vue'),
      meta: { title: '编辑配置' },
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
