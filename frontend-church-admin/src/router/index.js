import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const routes = [
  {
    path: '/login',
    name: 'AdminLogin',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/service-schedule',
    name: 'AdminServiceSchedule',
    component: () => import('@/views/admin/ServiceSchedule.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/persons',
    name: 'AdminPersons',
    component: () => import('@/views/admin/Persons.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/groups',
    name: 'AdminGroups',
    component: () => import('@/views/admin/Groups.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/attendance-rate',
    name: 'AdminAttendanceRate',
    component: () => import('@/views/admin/AttendanceRate.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/positions',
    name: 'AdminPositions',
    component: () => import('@/views/admin/Positions.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/users',
    name: 'AdminUsers',
    component: () => import('@/views/admin/Users.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/roles',
    name: 'AdminRoles',
    component: () => import('@/views/admin/Roles.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/permissions',
    name: 'AdminPermissions',
    component: () => import('@/views/admin/Permissions.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/menus',
    name: 'AdminMenus',
    component: () => import('@/views/admin/Menus.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/url-permissions',
    name: 'AdminUrlPermissions',
    component: () => import('@/views/admin/UrlPermissions.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/church-info',
    name: 'AdminChurchInfo',
    component: () => import('@/views/admin/ChurchInfo.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/about-info',
    name: 'AdminAboutInfo',
    component: () => import('@/views/admin/AboutInfo.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/activities',
    name: 'AdminActivities',
    component: () => import('@/views/admin/Activities.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/contact-submissions',
    name: 'AdminContactSubmissions',
    component: () => import('@/views/admin/ContactSubmissions.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/scheduled-jobs',
    name: 'AdminScheduledJobs',
    component: () => import('@/views/admin/ScheduledJobs.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/sunday-messages',
    name: 'AdminSundayMessages',
    component: () => import('@/views/admin/SundayMessages.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/prayer-requests',
    name: 'AdminPrayerRequests',
    component: () => import('@/views/admin/PrayerRequests.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/announcements',
    name: 'AdminAnnouncements',
    component: () => import('@/views/admin/Announcements.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/maintenance',
    name: 'AdminMaintenance',
    component: () => import('@/views/admin/Maintenance.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/checkin/admin/sessions',
    name: 'CheckinSessionList',
    component: () => import('@/views/checkin/SessionList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/checkin/admin/sessions/:id',
    name: 'CheckinSessionDetail',
    component: () => import('@/views/checkin/SessionDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/checkin/admin/manual',
    name: 'CheckinAdminManual',
    component: () => import('@/views/checkin/ManualView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:code',
    name: 'Checkin',
    component: () => import('@/views/checkin/CheckinView.vue'),
    meta: { requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory('/church-admin/'),
  routes
})

// 路由守衛：保護後台路由
router.beforeEach(async (to, from, next) => {
  const { checkAuth } = useAuth()
  
  // 登入頁面不需要認證
  if (to.path === '/login') {
    next()
    return
  }
  
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

