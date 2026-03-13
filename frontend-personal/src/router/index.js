import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { createAuthGuard } from '@shared/utils/authGuard'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/expenses',
    name: 'Expenses',
    component: () => import('@/views/Expenses.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/tracker',
    name: 'Tracker',
    component: () => import('@/views/Tracker.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    redirect: '/admin/users'
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
  },
  {
    path: '/admin/url-permissions',
    name: 'AdminUrlPermissions',
    component: () => import('@/views/admin/UrlPermissions.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/scheduled-jobs',
    name: 'AdminScheduledJobs',
    component: () => import('@/views/admin/ScheduledJobs.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/maintenance',
    name: 'AdminMaintenance',
    component: () => import('@/views/admin/Maintenance.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/line-groups',
    name: 'AdminLineGroups',
    component: () => import('@/views/admin/LineGroups.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory('/personal/'),
  routes
})

const { checkAuth, currentUser } = useAuth()
router.beforeEach(
  createAuthGuard({
    checkAuth,
    currentUser,
    loginRouteName: 'Login',
    authenticatedRedirect: { name: 'Dashboard' }
  })
)

export default router
