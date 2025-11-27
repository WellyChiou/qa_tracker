<template>
  <div class="dashboard">
    <TopNavbar />

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
            :to="normalizeMenuUrl(menuItem.url)"
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
import TopNavbar from '@/components/TopNavbar.vue'
import { apiService } from '@/composables/useApi'

const router = useRouter()

const menus = ref([])
const backendStatus = ref('')
const backendStatusText = ref('檢查中...')
const authStatus = ref('')
const authStatusText = ref('檢查中...')

// 正規化菜單 URL：移除 .html 後綴，轉換為 Vue Router 路徑
const normalizeMenuUrl = (url) => {
  if (!url || url === '#') return url
  // 移除 .html 後綴
  return url.replace(/\.html$/, '')
}

const dashboardMenus = computed(() => {
  const items = []
  menus.value.forEach(menu => {
    if (menu.showInDashboard !== false && menu.url && menu.url !== '#') {
      items.push({ ...menu, url: normalizeMenuUrl(menu.url) })
    }
    if (menu.children) {
      menu.children.forEach(child => {
        if (child.showInDashboard !== false && child.url && child.url !== '#') {
          items.push({ ...child, url: normalizeMenuUrl(child.url) })
        }
      })
    }
  })
  return items
})

const checkBackendStatus = async () => {
  try {
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 
      (import.meta.env.DEV 
        ? `${window.location.protocol}//${window.location.hostname}:8080/api`
        : `${window.location.protocol}//${window.location.hostname}/api`)
    const response = await fetch(`${apiBaseUrl}/hello`)
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
    menus.value = await apiService.getMenus()
  } catch (error) {
    console.error('載入菜單失敗:', error)
  }
}

onMounted(async () => {
  checkBackendStatus()
  authStatus.value = 'success'
  authStatusText.value = '✓ 已登入'
  await loadMenus()
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-attachment: fixed;
  color: white;
  position: relative;
}

.dashboard::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 50%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(118, 75, 162, 0.3) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.container {
  max-width: 1200px;
  margin: var(--spacing-2xl) auto;
  padding: 0 var(--spacing-xl);
  position: relative;
  z-index: 1;
}

.status-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  border-radius: var(--border-radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-xl);
  margin-bottom: var(--spacing-xl);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: var(--transition);
}

.status-card:hover {
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4);
}

.status-card h2 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: var(--spacing-xl);
  text-align: center;
  background: linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-lg);
  margin-top: var(--spacing-lg);
}

.status-item {
  padding: var(--spacing-xl);
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: var(--border-radius-lg);
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: var(--transition);
  position: relative;
  overflow: hidden;
}

.status-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.5) 0%, transparent 100%);
}

.status-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  background: rgba(255, 255, 255, 0.2);
}

.status-item strong {
  display: block;
  margin-bottom: var(--spacing-md);
  font-size: 0.95rem;
  opacity: 0.95;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-value {
  font-size: 1.3rem;
  font-weight: 700;
}

.status-value.success {
  color: #4ade80;
  text-shadow: 0 2px 10px rgba(74, 222, 128, 0.3);
}

.status-value.error {
  color: #f87171;
  text-shadow: 0 2px 10px rgba(248, 113, 113, 0.3);
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--spacing-lg);
  margin-top: var(--spacing-lg);
}

.menu-card {
  padding: var(--spacing-xl);
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  border-radius: var(--border-radius-lg);
  text-align: center;
  text-decoration: none;
  color: white;
  transition: var(--transition);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: block;
  position: relative;
  overflow: hidden;
}

.menu-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.5) 0%, transparent 100%);
}

a.menu-card {
  text-decoration: none;
}

.menu-card:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-6px) scale(1.02);
  box-shadow: var(--shadow-xl);
}

.menu-icon {
  font-size: 3.5rem;
  margin-bottom: var(--spacing-md);
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
  transition: var(--transition);
}

.menu-card:hover .menu-icon {
  transform: scale(1.1) rotate(5deg);
}

.menu-title {
  font-size: 1.1rem;
  font-weight: 600;
}

.no-menu {
  grid-column: 1/-1;
  text-align: center;
  opacity: 0.8;
  padding: var(--spacing-2xl);
  font-size: 1.1rem;
}
</style>

