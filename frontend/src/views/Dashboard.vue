<template>
  <div class="dashboard">
    <nav class="navbar">
      <h1>📊 系統儀表板</h1>
      <div class="navbar-menu">
        <div v-for="(menu, index) in menus" :key="index" class="menu-item-wrapper">
          <router-link
            v-if="menu.url && menu.url !== '#'"
            :to="menu.url"
            class="menu-item"
          >
            {{ menu.icon || '' }} {{ menu.menuName }}
          </router-link>
          <div
            v-else-if="menu.children && menu.children.length > 0"
            class="menu-item has-submenu"
            @click="toggleSubmenu(index)"
            :class="{ active: activeSubmenu === index }"
          >
            {{ menu.icon || '' }} {{ menu.menuName }}
            <span class="arrow">▼</span>
            <div v-if="activeSubmenu === index" class="submenu">
              <router-link
                v-for="child in menu.children"
                :key="child.id"
                v-if="child.url && child.url !== '#'"
                :to="child.url"
                class="submenu-item"
              >
                <span class="submenu-icon">{{ child.icon || '📄' }}</span>
                {{ child.menuName }}
              </router-link>
            </div>
          </div>
          <span v-else class="menu-item" style="cursor: default; opacity: 0.7;">
            {{ menu.icon || '' }} {{ menu.menuName }}
          </span>
        </div>
      </div>
      <div v-if="currentUser" class="user-info">
        <span>{{ currentUser.displayName || currentUser.username || currentUser.email || '用戶' }}</span>
        <button class="logout-btn" @click="handleLogout">登出</button>
      </div>
    </nav>

    <div class="container">
      <div class="status-card">
        <h2>🔍 系統狀態</h2>
        <div class="status-grid">
          <div class="status-item">
            <strong>前端 (Vue/Nginx)</strong>
            <div class="status-value success">✓ 運行中</div>
          </div>
          <div class="status-item">
            <strong>後端 (Java Spring Boot)</strong>
            <div class="status-value" :class="backendStatus">
              {{ backendStatusText }}
            </div>
          </div>
          <div class="status-item">
            <strong>資料庫 (MySQL)</strong>
            <div class="status-value success">已連接</div>
          </div>
          <div class="status-item">
            <strong>認證狀態</strong>
            <div class="status-value" :class="authStatus">
              {{ authStatusText }}
            </div>
          </div>
        </div>
      </div>

      <div class="status-card">
        <h2>🚀 快速訪問</h2>
        <div class="menu-grid">
          <router-link
            v-for="menuItem in dashboardMenus"
            :key="menuItem.url"
            :to="menuItem.url"
            class="menu-card"
          >
            <div class="menu-icon">{{ menuItem.icon || '📄' }}</div>
            <div class="menu-title">{{ menuItem.menuName }}</div>
          </router-link>
          <div v-if="dashboardMenus.length === 0" class="no-menu">
            暫無可用菜單
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiService } from '@/composables/useApi'

const router = useRouter()
const { currentUser, logout: authLogout } = useAuth()

const menus = ref([])
const activeSubmenu = ref(null)
const backendStatus = ref('')
const backendStatusText = ref('檢查中...')
const authStatus = ref('')
const authStatusText = ref('檢查中...')

const dashboardMenus = computed(() => {
  const items = []
  menus.value.forEach(menu => {
    if (menu.showInDashboard !== false && menu.url && menu.url !== '#') {
      items.push(menu)
    }
    if (menu.children) {
      menu.children.forEach(child => {
        if (child.showInDashboard !== false && child.url && child.url !== '#') {
          items.push(child)
        }
      })
    }
  })
  return items
})

const toggleSubmenu = (index) => {
  activeSubmenu.value = activeSubmenu.value === index ? null : index
}

const handleLogout = async () => {
  await authLogout()
  router.push('/login')
}

const checkBackendStatus = async () => {
  try {
    const response = await fetch(`${window.location.protocol}//${window.location.hostname}:8080/api/hello`)
    if (response.ok) {
      backendStatus.value = 'success'
      backendStatusText.value = '✓ 運行中'
    } else {
      throw new Error('後端無響應')
    }
  } catch (error) {
    backendStatus.value = 'error'
    backendStatusText.value = '✗ 無法連接'
  }
}

const loadMenus = async () => {
  try {
    if (currentUser.value && currentUser.value.menus) {
      menus.value = currentUser.value.menus
    } else {
      menus.value = await apiService.getMenus()
    }
  } catch (error) {
    console.error('載入菜單失敗:', error)
  }
}

onMounted(async () => {
  checkBackendStatus()
  
  if (currentUser.value && currentUser.value.authenticated) {
    authStatus.value = 'success'
    authStatusText.value = '✓ 已登入'
    await loadMenus()
  } else {
    authStatus.value = 'error'
    authStatusText.value = '✗ 未登入'
    setTimeout(() => {
      router.push('/login')
    }, 1000)
  }
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.navbar {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 100;
}

.navbar h1 {
  font-size: 1.5rem;
  margin: 0;
}

.navbar-menu {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.menu-item-wrapper {
  position: relative;
}

.menu-item {
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  text-decoration: none;
  color: white;
  transition: all 0.3s;
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: inline-block;
  cursor: pointer;
}

a.menu-item {
  text-decoration: none;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.menu-item.has-submenu {
  cursor: pointer;
}

.menu-item.has-submenu.active .arrow {
  transform: rotate(180deg);
}

.arrow {
  display: inline-block;
  transition: transform 0.3s;
  margin-left: 0.25rem;
}

.submenu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 0.5rem;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  min-width: 200px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  z-index: 10000;
  overflow: hidden;
}

.submenu-item {
  display: block;
  padding: 0.75rem 1rem;
  color: #333;
  text-decoration: none;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
}

a.submenu-item {
  text-decoration: none;
}

.submenu-item:last-child {
  border-bottom: none;
}

.submenu-item:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.submenu-icon {
  margin-right: 0.5rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.logout-btn {
  padding: 0.5rem 1rem;
  background: rgba(220, 53, 69, 0.8);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
}

.logout-btn:hover {
  background: rgba(220, 53, 69, 1);
}

.container {
  max-width: 1200px;
  margin: 2rem auto;
  padding: 0 2rem;
}

.status-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
  margin-bottom: 2rem;
}

.status-card h2 {
  font-size: 2rem;
  margin-bottom: 1.5rem;
  text-align: center;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.status-item {
  padding: 1rem;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  text-align: center;
}

.status-item strong {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  opacity: 0.9;
}

.status-value {
  font-size: 1.2rem;
  font-weight: bold;
}

.status-value.success {
  color: #4ade80;
}

.status-value.error {
  color: #f87171;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.menu-card {
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  text-align: center;
  text-decoration: none;
  color: white;
  transition: all 0.3s;
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: block;
}

a.menu-card {
  text-decoration: none;
}

.menu-card:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.menu-icon {
  font-size: 3rem;
  margin-bottom: 0.5rem;
}

.menu-title {
  font-size: 1.1rem;
  font-weight: 500;
}

.no-menu {
  grid-column: 1/-1;
  text-align: center;
  opacity: 0.7;
  padding: 2rem;
}
</style>

