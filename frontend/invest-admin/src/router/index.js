import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { createAuthGuard } from '@shared/utils/authGuard'

const routes = [
  {
    path: '/login',
    name: 'InvestAdminLogin',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    name: 'InvestAdminDashboard',
    component: () => import('@/views/invest/InvestDashboardPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_DASHBOARD_VIEW' }
  },
  {
    path: '/portfolios',
    name: 'InvestAdminPortfolios',
    component: () => import('@/views/invest/PortfolioListPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_PORTFOLIO_VIEW' }
  },
  {
    path: '/portfolios/:id',
    name: 'InvestAdminPortfolioDetail',
    component: () => import('@/views/invest/PortfolioDetailPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_PORTFOLIO_VIEW' }
  },
  {
    path: '/stocks',
    name: 'InvestAdminStocks',
    component: () => import('@/views/invest/StockListPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_STOCK_VIEW' }
  },
  {
    path: '/stock-price-dailies',
    name: 'InvestAdminStockPriceDailies',
    component: () => import('@/views/invest/StockPriceDailyPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_STOCK_PRICE_DAILY_VIEW' }
  },
  {
    path: '/daily-reports',
    name: 'InvestAdminDailyReports',
    component: () => import('@/views/invest/DailyReportPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_DAILY_REPORT_VIEW' }
  },
  {
    path: '/alerts',
    name: 'InvestAdminAlerts',
    component: () => import('@/views/invest/AlertListPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_ALERT_EVENT_VIEW' }
  },
  {
    path: '/system/users',
    name: 'InvestAdminSystemUsers',
    component: () => import('@/views/system/SystemUsersPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_SYS_USERS_VIEW' }
  },
  {
    path: '/system/roles',
    name: 'InvestAdminSystemRoles',
    component: () => import('@/views/system/SystemRolesPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_SYS_ROLES_VIEW' }
  },
  {
    path: '/system/permissions',
    name: 'InvestAdminSystemPermissions',
    component: () => import('@/views/system/SystemPermissionsPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_SYS_PERMISSIONS_VIEW' }
  },
  {
    path: '/system/menus',
    name: 'InvestAdminSystemMenus',
    component: () => import('@/views/system/SystemMenusPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_SYS_MENUS_VIEW' }
  },
  {
    path: '/system/url-permissions',
    name: 'InvestAdminSystemUrlPermissions',
    component: () => import('@/views/system/SystemUrlPermissionsPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_SYS_URL_PERMISSIONS_VIEW' }
  },
  {
    path: '/system/maintenance',
    name: 'InvestAdminSystemMaintenance',
    component: () => import('@/views/system/SystemMaintenancePage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_SYS_MAINTENANCE_VIEW' }
  },
  {
    path: '/system/scheduler',
    name: 'InvestAdminSystemScheduler',
    component: () => import('@/views/system/SystemSchedulerPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_SYS_SCHEDULER_VIEW' }
  },
  {
    path: '/system/line-groups',
    name: 'InvestAdminSystemLineGroups',
    component: () => import('@/views/system/SystemLineGroupsPage.vue'),
    meta: { requiresAuth: true, requiredPermission: 'INVEST_SYS_LINE_GROUPS_VIEW' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory('/invest-admin/'),
  routes
})

const { checkAuth, currentUser } = useAuth()
router.beforeEach(
  createAuthGuard({
    checkAuth,
    currentUser,
    loginRouteName: 'InvestAdminLogin',
    authenticatedRedirect: { name: 'InvestAdminDashboard' },
    strictPermissions: true
  })
)

export default router
