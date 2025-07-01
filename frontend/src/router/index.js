import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/useAuthStore'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/auth',
      name: 'Auth',
      component: () => import('@/views/Auth.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/device-list',
      name: 'DeviceList',
      component: () => import('@/views/DeviceList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/device-map',
      name: 'DeviceMap',
      component: () => import('@/views/DeviceMap.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/worker-list',
      name: 'WorkerList',
      component: () => import('@/views/WorkerList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/maintenance-records',
      name: 'MaintenanceRecords',
      component: () => import('@/views/MaintenanceRecords.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/maintenance-success',
      name: 'MaintenanceSuccess',
      component: () => import('@/views/MaintenanceSuccess.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/maintenance-upload/:id',
      name: 'MaintenanceUpload',
      component: () => import('@/views/MaintenanceUpload.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFound.vue')
    }
  ]
})


// 路由守卫:登录拦截
router.beforeEach((to, from, next) => {
  const store = useAuthStore()
  if (to.meta.requiresAuth && !store.isAuthenticated) {
    next('/auth')
  } else {
    next()
  }

})

export default router 