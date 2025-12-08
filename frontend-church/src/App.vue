<template>
  <div id="app">
    <nav class="navbar">
      <div class="nav-container">
        <div class="nav-logo">
          <router-link to="/" class="logo-link">
            <img src="/images/logo.png" alt="極光教會 Logo" class="logo-image" />
            <span class="logo-text">極光教會-PLC</span>
          </router-link>
        </div>
        <ul class="nav-menu">
          <li v-for="menu in frontendMenus" :key="menu.id">
            <router-link :to="menu.url || '#'">{{ menu.menuName }}</router-link>
          </li>
          <li v-if="isAuthenticated">
            <router-link to="/admin" class="admin-link">管理後台</router-link>
          </li>
          <li v-if="!isAuthenticated">
            <router-link to="/login" class="login-link">登入</router-link>
          </li>
          <li v-else class="user-menu">
            <span class="user-name">{{ currentUser?.displayName || currentUser?.username }}</span>
            <button @click="handleLogout" class="logout-button">登出</button>
          </li>
        </ul>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
    <footer class="footer">
      <div class="footer-container">
        <p>&copy; 2026 極光教會網站. 版權所有.</p>
      </div>
    </footer>
    <LoadingSpinner />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLoading } from '@/composables/useLoading'
import { useAuth } from '@/composables/useAuth'
import { apiRequest } from '@/utils/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const router = useRouter()
const { currentUser, isAuthenticated, checkAuth, logout } = useAuth()
const frontendMenus = ref([])

// 初始化 loading 系統（註冊回調到 API 服務）
useLoading()

// 載入前台菜單
const loadFrontendMenus = async () => {
  try {
    const response = await apiRequest('/church/menus/frontend', {
      method: 'GET'
    })
    
    if (response.ok) {
      const menus = await response.json()
      frontendMenus.value = menus || []
    }
  } catch (error) {
    console.error('載入前台菜單失敗:', error)
    // 如果載入失敗，使用預設菜單
    frontendMenus.value = [
      { id: 1, menuName: '首頁', url: '/' },
      { id: 2, menuName: '關於我們', url: '/about' },
      { id: 3, menuName: '活動', url: '/activities' },
      { id: 4, menuName: '服事安排', url: '/service-schedule' },
      { id: 5, menuName: '聯絡我們', url: '/contact' }
    ]
  }
}

// 檢查認證狀態
onMounted(async () => {
  await checkAuth()
  await loadFrontendMenus()
})

// 處理登出
const handleLogout = async () => {
  await logout()
  router.push('/')
}
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

.admin-link {
  background: rgba(255, 255, 255, 0.2);
  padding: 0.5rem 1rem;
  border-radius: 6px;
  transition: background 0.3s;
}

.admin-link:hover {
  background: rgba(255, 255, 255, 0.3);
  text-decoration: none;
}

.login-link {
  background: rgba(255, 255, 255, 0.2);
  padding: 0.5rem 1rem;
  border-radius: 6px;
  transition: background 0.3s;
}

.login-link:hover {
  background: rgba(255, 255, 255, 0.3);
  text-decoration: none;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-name {
  font-size: 0.9rem;
  opacity: 0.9;
}

.logout-button {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.3s;
}

.logout-button:hover {
  background: rgba(255, 255, 255, 0.3);
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

