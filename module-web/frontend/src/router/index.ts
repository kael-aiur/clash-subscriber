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
      path: '/scheduled-tasks',
      name: 'scheduled-tasks',
      component: () => import('@/views/ScheduledTaskView.vue'),
    },
    {
      path: '/scripts',
      name: 'scripts',
      component: () => import('@/views/ScriptView.vue'),
    },
  ],
})

export default router
