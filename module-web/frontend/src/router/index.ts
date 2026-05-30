import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
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

export default router
