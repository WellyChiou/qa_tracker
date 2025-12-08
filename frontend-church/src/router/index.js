import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/About.vue')
  },
  {
    path: '/activities',
    name: 'Activities',
    component: () => import('@/views/Activities.vue')
  },
  {
    path: '/contact',
    name: 'Contact',
    component: () => import('@/views/Contact.vue')
  },
  {
    path: '/service-schedule',
    name: 'ServiceSchedule',
    component: () => import('@/views/ServiceSchedule.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/service-schedule',
    name: 'AdminServiceSchedule',
    component: () => import('@/views/admin/ServiceSchedule.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/persons',
    name: 'AdminPersons',
    component: () => import('@/views/admin/Persons.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/positions',
    name: 'AdminPositions',
    component: () => import('@/views/admin/Positions.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/admin/Users.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/roles',
    name: 'AdminRoles',
    component: () => import('@/views/admin/Roles.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/permissions',
    name: 'AdminPermissions',
    component: () => import('@/views/admin/Permissions.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/menus',
    name: 'AdminMenus',
    component: () => import('@/views/admin/Menus.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory('/church/'),
  routes
})

// 路由守衛：保護後台路由
router.beforeEach(async (to, from, next) => {
  const { checkAuth } = useAuth()
  
  // 檢查是否需要認證
  if (to.meta.requiresAuth) {
    const isAuthenticated = await checkAuth()
    if (!isAuthenticated) {
      // 未登入，導向登入頁面
      next({ path: '/login', query: { redirect: to.fullPath } })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router

