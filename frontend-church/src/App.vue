<template>
  <div id="app">
    <!-- 前台 navbar：只在非後台路由顯示 -->
    <nav v-if="!isAdminRoute" class="navbar">
      <div class="nav-container">
        <div class="nav-logo">
          <router-link to="/" class="logo-link">
            <img src="/images/logo.png" alt="極光教會 Logo" class="logo-image" />
            <span class="logo-text">極光教會-PLC</span>
          </router-link>
        </div>
        <ul class="nav-menu">
          <li v-for="menu in displayMenus" :key="menu.id">
            <router-link :to="menu.url || '#'">{{ menu.menuName }}</router-link>
          </li>
        </ul>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
    <!-- 前台 footer：只在非後台路由顯示 -->
    <footer v-if="!isAdminRoute" class="footer">
      <div class="footer-container">
        <p>&copy; 2026 極光教會網站. 版權所有.</p>
      </div>
    </footer>
    <LoadingSpinner />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useLoading } from '@/composables/useLoading'
import { apiRequest } from '@/utils/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const route = useRoute()
const frontendMenus = ref([])

// 計算要顯示的菜單（如果有子菜單，只顯示子菜單；否則顯示根菜單）
const displayMenus = computed(() => {
  const menus = []
  for (const menu of frontendMenus.value) {
    // 如果菜單有子菜單，只顯示子菜單（不顯示母菜單）
    if (menu.children && menu.children.length > 0) {
      menus.push(...menu.children)
    } else if (menu.url && menu.url !== '#') {
      // 如果沒有子菜單且 URL 不是 '#'，顯示根菜單本身
      menus.push(menu)
    }
  }
  return menus
})

// 判斷是否為後台路由或登入頁面（這些頁面不需要顯示前台的 navbar 和 footer）
const isAdminRoute = computed(() => {
  return route.path.startsWith('/admin') || route.path === '/login'
})

// 初始化 loading 系統（註冊回調到 API 服務）
useLoading()

// 預設菜單（僅在 API 請求失敗時使用）
const defaultMenus = [
  { id: 1, menuName: '首頁', url: '/' },
  { id: 2, menuName: '關於我們', url: '/about' },
  { id: 3, menuName: '活動', url: '/activities' },
  { id: 4, menuName: '服事安排', url: '/service-schedule' },
  { id: 5, menuName: '聯絡我們', url: '/contact' }
]

// 載入前台菜單
const loadFrontendMenus = async () => {
  // 如果是後台路由或登入頁面，不需要載入前台菜單
  if (isAdminRoute.value) {
    return
  }
  
  try {
    const response = await apiRequest('/church/menus/frontend', {
      method: 'GET'
    })
    
    console.log('前台菜單 API 響應狀態:', response.status, response.ok)
    
    if (response.ok) {
      const menus = await response.json()
      console.log('前台菜單數據:', menus)
      
      // 如果後端返回了有效的菜單數據，使用後端的菜單
      if (menus && Array.isArray(menus)) {
        if (menus.length > 0) {
          console.log('使用後端菜單，共', menus.length, '項')
          frontendMenus.value = menus
          return
        } else {
          console.warn('後端返回空數組，但資料庫可能有設定，檢查資料庫配置')
          // 空數組不代表失敗，可能是資料庫中沒有前台菜單，不應該使用預設菜單
          frontendMenus.value = []
          return
        }
      } else {
        console.warn('後端返回的數據格式不正確:', menus)
      }
    } else {
      console.error('前台菜單 API 請求失敗，狀態碼:', response.status)
    }
    
    // 只有在 API 請求失敗（非 200 狀態）時才使用預設菜單
    console.warn('API 請求失敗，使用預設前台菜單')
    frontendMenus.value = defaultMenus
  } catch (error) {
    console.error('載入前台菜單時發生異常:', error)
    // 只有在發生異常時才使用預設菜單
    frontendMenus.value = defaultMenus
  }
}

// 監聽路由變化，只在非後台路由時載入前台菜單
watch(() => route.path, (newPath) => {
  if (!isAdminRoute.value) {
    loadFrontendMenus()
  } else {
    // 進入後台路由時，清空前台菜單（節省記憶體）
    frontendMenus.value = []
  }
}, { immediate: true })

// 初始化時載入前台菜單（如果當前不是後台路由）
onMounted(async () => {
  if (!isAdminRoute.value) {
    await loadFrontendMenus()
  }
})
</script>

<style scoped>
.navbar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 1rem 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-logo a {
  font-size: 1.5rem;
  font-weight: bold;
  color: white;
  text-decoration: none;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-image {
  width: 50px;
  height: 50px;
  object-fit: contain;
  border-radius: 50%;
}

.logo-text {
  font-size: 1.5rem;
  font-weight: bold;
  color: white;
}

.nav-menu {
  display: flex;
  list-style: none;
  gap: 2rem;
  margin: 0;
  padding: 0;
}

.nav-menu a {
  color: white;
  text-decoration: none;
  font-weight: 500;
  transition: opacity 0.3s;
}

.nav-menu a:hover,
.nav-menu a.router-link-active {
  opacity: 0.8;
  text-decoration: underline;
}


.main-content {
  min-height: calc(100vh - 200px);
}

.footer {
  background: #2c3e50;
  color: white;
  padding: 2rem 0;
  margin-top: 4rem;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  text-align: center;
}
</style>

