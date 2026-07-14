import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { hasRole } from '@/utils/auth'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/index.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/search/index.vue')
  },
  {
    path: '/hotel/:id',
    name: 'HotelDetail',
    component: () => import('@/views/hotel/detail.vue')
  },
  {
    path: '/order/confirm',
    name: 'OrderConfirm',
    component: () => import('@/views/order/confirm.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order/pay/:id',
    name: 'OrderPay',
    component: () => import('@/views/order/pay.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/user/login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/user/register.vue'),
    meta: { guest: true }
  },
  {
    path: '/user/center',
    name: 'UserCenter',
    component: () => import('@/views/user/center.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user/orders',
    name: 'UserOrders',
    component: () => import('@/views/user/orders.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      {
        path: 'hotels',
        name: 'AdminHotels',
        component: () => import('@/views/admin/Hotels.vue')
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/Orders.vue')
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = getToken()
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.role === 'admin' && !hasRole('admin')) {
    next({ name: 'Home' })
  } else if (to.meta.guest && token) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
