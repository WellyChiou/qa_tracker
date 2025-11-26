<template>
  <nav class="navbar">
    <router-link to="/" class="navbar-title">
      <h1>📊 系統儀表板</h1>
    </router-link>
    <div class="navbar-menu">
      <div v-for="(menu, index) in menus" :key="index" class="menu-item-wrapper">
        <router-link
          v-if="menu.url && menu.url !== '#'"
          :to="normalizeMenuUrl(menu.url)"
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
          <Transition name="submenu">
            <div v-if="activeSubmenu === index" class="submenu" @click.stop>
              <template v-for="child in menu.children" :key="child?.id || child">
                <router-link
                  v-if="child && child.url && child.url !== '#'"
                  :to="normalizeMenuUrl(child.url)"
                  class="submenu-item"
                  :class="{ active: $route.path.startsWith(normalizeMenuUrl(child.url)) }"
                  @click="handleSubmenuClick(index)"
                >
                  <span class="submenu-icon">{{ child.icon || '📄' }}</span>
                  {{ child.menuName }}
                </router-link>
              </template>
            </div>
          </Transition>
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
</template>

<script setup>
import { ref, onMounted, watch, computed, Transition } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiService } from '@/composables/useApi'

const router = useRouter()
const route = useRoute()
const { currentUser, logout: authLogout } = useAuth()

const menus = ref([])
const activeSubmenu = ref(null)

// 正規化菜單 URL：移除 .html 後綴，轉換為 Vue Router 路徑
const normalizeMenuUrl = (url) => {
  if (!url || url === '#') return url
  // 移除 .html 後綴
  return url.replace(/\.html$/, '')
}

// 檢查當前路由是否屬於某個菜單的子項
const isChildOfMenu = (menu) => {
  if (!menu || !menu.children || menu.children.length === 0) return false
  const currentPath = route.path
  return menu.children.some(child => {
    if (!child || !child.url) return false
    const childUrl = normalizeMenuUrl(child.url)
    return childUrl && currentPath.startsWith(childUrl)
  })
}

// 標記用戶是否正在手動操作子菜單
let isUserClicking = false
let clickTimeout = null

// 監聽路由變化，自動展開包含當前路由的父菜單
watch(() => route.path, () => {
  // 如果用戶正在手動操作，不自動展開或關閉
  if (isUserClicking) {
    return
  }
  
  // 自動展開包含當前路由的父菜單
  let foundActiveMenu = false
  menus.value.forEach((menu, index) => {
    if (isChildOfMenu(menu)) {
      if (activeSubmenu.value !== index) {
        activeSubmenu.value = index
      }
      foundActiveMenu = true
    }
  })
  
  // 如果當前路由不屬於任何子菜單，關閉所有子菜單
  if (!foundActiveMenu) {
    activeSubmenu.value = null
  }
}, { immediate: false })

const toggleSubmenu = (index) => {
  // 清除之前的超時
  if (clickTimeout) {
    clearTimeout(clickTimeout)
  }
  
  // 標記這是用戶手動操作
  isUserClicking = true
  
  // 直接切換子菜單狀態
  const wasOpen = activeSubmenu.value === index
  if (wasOpen) {
    activeSubmenu.value = null
    console.log('關閉子菜單:', index)
  } else {
    activeSubmenu.value = index
    console.log('打開子菜單:', index, 'activeSubmenu.value:', activeSubmenu.value)
  }
  
  // 500ms 後重置標記，允許路由監聽器工作
  clickTimeout = setTimeout(() => {
    isUserClicking = false
  }, 500)
}

// 點擊子菜單項時的處理（點擊後關閉子菜單）
const handleSubmenuClick = (menuIndex) => {
  // 標記這是用戶點擊子菜單項
  isUserClicking = true
  
  // 延遲關閉子菜單，讓路由先變化
  setTimeout(() => {
    activeSubmenu.value = null
    // 300ms 後重置標記
    setTimeout(() => {
      isUserClicking = false
    }, 300)
  }, 100)
}

const handleLogout = async () => {
  await authLogout()
  router.push('/login')
}

const loadMenus = async () => {
  try {
    if (currentUser.value && currentUser.value.menus) {
      menus.value = currentUser.value.menus
    } else {
      menus.value = await apiService.getMenus()
    }
    console.log('載入的菜單:', menus.value)
    // 檢查是否有子菜單
    menus.value.forEach((menu, index) => {
      if (menu.children && menu.children.length > 0) {
        console.log(`菜單 ${index} (${menu.menuName}) 有 ${menu.children.length} 個子項:`, menu.children)
      }
    })
    // 加載菜單後，檢查是否需要自動展開（只有在不是用戶手動操作的情況下）
    if (!isUserClicking) {
      menus.value.forEach((menu, index) => {
        if (isChildOfMenu(menu)) {
          activeSubmenu.value = index
        }
      })
    }
  } catch (error) {
    console.error('載入菜單失敗:', error)
  }
}

onMounted(async () => {
  await loadMenus()
})
</script>

<style scoped>
.navbar {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.95) 0%, rgba(118, 75, 162, 0.95) 100%);
  backdrop-filter: blur(20px);
  padding: var(--spacing-lg) var(--spacing-xl);
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow-lg);
  position: relative;
  z-index: 100;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.navbar-title {
  text-decoration: none;
  color: white;
  transition: var(--transition);
}

.navbar-title:hover {
  transform: scale(1.05);
}

.navbar-title h1 {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
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
  padding: 0.75rem 1.25rem;
  background: rgba(255, 255, 255, 0.15);
  border-radius: var(--border-radius);
  text-decoration: none;
  color: white;
  transition: var(--transition);
  border: 1px solid rgba(255, 255, 255, 0.25);
  display: inline-block;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.95rem;
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

.menu-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.menu-item:hover::before {
  left: 100%;
}

a.menu-item {
  text-decoration: none;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: rgba(255, 255, 255, 0.4);
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
  margin-top: var(--spacing-sm);
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border-radius: var(--border-radius-lg);
  min-width: 220px;
  box-shadow: var(--shadow-xl);
  z-index: 10000;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.submenu-enter-active {
  animation: slideDown 0.3s;
}

.submenu-leave-active {
  animation: slideUp 0.2s;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideUp {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-10px);
  }
}

.submenu-item {
  display: block;
  padding: var(--spacing-md) var(--spacing-lg);
  color: var(--text-primary);
  text-decoration: none;
  border-bottom: 1px solid var(--border-color);
  transition: var(--transition);
  font-weight: 500;
}

a.submenu-item {
  text-decoration: none;
}

.submenu-item:last-child {
  border-bottom: none;
}

.submenu-item:hover {
  background: rgba(102, 126, 234, 0.1);
  color: var(--primary-color);
  padding-left: calc(var(--spacing-lg) + 4px);
}

.submenu-item.active {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.15) 0%, rgba(102, 126, 234, 0.05) 100%);
  color: var(--primary-color);
  font-weight: 700;
  border-left: 4px solid var(--primary-color);
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
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, rgba(220, 53, 69, 0.9) 0%, rgba(185, 28, 28, 0.9) 100%);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--border-radius);
  color: white;
  cursor: pointer;
  transition: var(--transition);
  font-weight: 600;
  font-size: 0.95rem;
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(10px);
}

.logout-btn:hover {
  background: linear-gradient(135deg, rgba(220, 53, 69, 1) 0%, rgba(185, 28, 28, 1) 100%);
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}
</style>

