<template>
  <div class="admin-layout">
    <nav class="admin-navbar">
      <div class="navbar-container">
        <router-link to="/admin" class="title-link">
          <img src="/images/logo.png" alt="極光教會 Logo" class="logo-image" />
          <span class="logo-text">極光教會-PLC</span>
        </router-link>
        <div class="navbar-menu">
          <template v-for="menu in adminMenus" :key="menu.id">
            <div v-if="menu.children && menu.children.length > 0" class="menu-item-wrapper">
              <div
                class="menu-item has-submenu"
                @click.stop="toggleMenu(menu.id)"
                :class="{ active: expandedMenus.includes(menu.id) }"
              >
                <span class="menu-icon">{{ menu.icon }}</span>
                <span class="menu-text">{{ menu.menuName }}</span>
                <span class="arrow">▼</span>
              </div>
              <div v-if="expandedMenus.includes(menu.id)" class="submenu" @click.stop>
                <router-link
                  v-for="child in menu.children"
                  :key="child.id"
                  :to="child.url"
                  class="submenu-item"
                  :class="{ active: $route.path === child.url }"
                  @click="handleSubmenuClick(menu.id, $event)"
                >
                  <span class="submenu-icon">{{ child.icon }}</span>
                  {{ child.menuName }}
                </router-link>
              </div>
            </div>
            <router-link
              v-else
              :to="menu.url"
              class="menu-item"
              :class="{ active: $route.path === menu.url }"
            >
              <span class="menu-icon">{{ menu.icon }}</span>
              <span class="menu-text">{{ menu.menuName }}</span>
            </router-link>
          </template>
        </div>
        <div class="navbar-user">
          <div class="user-info">
            <span class="user-name">{{ currentUser?.displayName || currentUser?.username }}</span>
            <button @click="handleLogout" class="logout-button">登出</button>
          </div>
        </div>
      </div>
    </nav>
    <main class="admin-main">
      <div class="admin-content">
        <slot />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiRequest } from '@/utils/api'

const router = useRouter()
const route = useRoute()
const { currentUser, logout } = useAuth()

const adminMenus = ref([])
const expandedMenus = ref([])

// 模組級變量：記錄被用戶手動關閉的子菜單 ID（永久記錄，直到用戶手動展開）
// 使用 sessionStorage 確保在組件重新創建時不會丟失
const getManuallyClosedMenus = () => {
  try {
    const stored = sessionStorage.getItem('manuallyClosedMenus')
    return stored ? new Set(JSON.parse(stored)) : new Set()
  } catch {
    return new Set()
  }
}

const saveManuallyClosedMenus = (set) => {
  try {
    sessionStorage.setItem('manuallyClosedMenus', JSON.stringify(Array.from(set)))
  } catch (e) {
    console.error('保存 manuallyClosedMenus 失敗:', e)
  }
}

let manuallyClosedMenus = getManuallyClosedMenus()

// 標記用戶是否正在手動操作子菜單
let isUserClicking = false
let justNavigatedFromSubmenu = false
let clickTimeout = null

const pageTitle = computed(() => {
  // 先查找子菜單
  for (const menu of adminMenus.value) {
    if (menu.children) {
      const child = menu.children.find(c => c.url === route.path)
      if (child) {
        return child.menuName
      }
    }
  }
  // 再查找主菜單
  const menu = adminMenus.value.find(m => m.url === route.path)
  return menu ? menu.menuName : '管理系統'
})

// 檢查當前路由是否屬於某個菜單的子項
const isChildOfMenu = (menu) => {
  if (!menu || !menu.children || menu.children.length === 0) return false
  const currentPath = route.path
  return menu.children.some(child => {
    if (!child || !child.url) return false
    return child.url && currentPath === child.url
  })
}

const toggleMenu = (menuId) => {
  // 清除之前的超時
  if (clickTimeout) {
    clearTimeout(clickTimeout)
  }
  
  // 標記這是用戶手動操作
  isUserClicking = true
  
  const index = expandedMenus.value.indexOf(menuId)
  const wasOpen = index > -1
  
  if (wasOpen) {
    expandedMenus.value.splice(index, 1)
    // 記錄用戶手動關閉的子菜單
    manuallyClosedMenus.add(menuId)
    saveManuallyClosedMenus(manuallyClosedMenus)
  } else {
    expandedMenus.value.push(menuId)
    // 當用戶手動展開子菜單時，從關閉記錄中移除，允許正常的自動展開邏輯
    manuallyClosedMenus.delete(menuId)
    saveManuallyClosedMenus(manuallyClosedMenus)
  }
  
  // 延遲重置標記
  clickTimeout = setTimeout(() => {
    isUserClicking = false
  }, 1000)
}

const handleSubmenuClick = (menuId, event) => {
  // 阻止事件冒泡
  if (event) {
    event.stopPropagation()
  }
  
  // 記錄被手動關閉的子菜單（永久記錄，直到用戶手動展開）
  manuallyClosedMenus.add(menuId)
  saveManuallyClosedMenus(manuallyClosedMenus)
  
  // 立即設置標記，在路由變化之前
  isUserClicking = true
  justNavigatedFromSubmenu = true
  
  // 立即關閉父菜單
  const index = expandedMenus.value.indexOf(menuId)
  if (index > -1) {
    expandedMenus.value.splice(index, 1)
  }
  
  // 清除之前的超時
  if (clickTimeout) {
    clearTimeout(clickTimeout)
  }
  
  // 延遲重置標記，防止路由變化後自動重新展開
  clickTimeout = setTimeout(() => {
    isUserClicking = false
    justNavigatedFromSubmenu = false
  }, 3000) // 3秒後重置標記，給路由導航和頁面渲染足夠時間
}

const loadAdminMenus = async () => {
  try {
    const response = await apiRequest('/church/menus/admin', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const menus = await response.json()
      console.log('載入的後台菜單:', menus)
      adminMenus.value = menus || []
      console.log('設置後的 adminMenus:', adminMenus.value)
      
      // 從 sessionStorage 恢復 manuallyClosedMenus（防止組件重新創建時丟失）
      manuallyClosedMenus = getManuallyClosedMenus()
      
      // 首先，無論什麼情況，都要確保被手動關閉的子菜單保持關閉狀態
      // 強制關閉所有在 manuallyClosedMenus 中的子菜單
      if (manuallyClosedMenus.size > 0) {
        manuallyClosedMenus.forEach((menuId) => {
          const index = expandedMenus.value.indexOf(menuId)
          if (index > -1) {
            expandedMenus.value.splice(index, 1)
          }
        })
      }
      
      // 檢查當前路由是否屬於被手動關閉的子菜單
      let isCurrentRouteInClosedSubmenu = false
      for (const menu of adminMenus.value) {
        if (manuallyClosedMenus.has(menu.id) && isChildOfMenu(menu)) {
          isCurrentRouteInClosedSubmenu = true
          break
        }
      }
      
      // 如果當前路由屬於被手動關閉的子菜單，絕對不自動展開
      if (isCurrentRouteInClosedSubmenu) {
        return
      }
      
      // 加載菜單後，檢查是否需要自動展開（只有在不是用戶手動操作且不是剛剛從子菜單導航的情況下）
      // 並且跳過被手動關閉的子菜單
      if (!isUserClicking && !justNavigatedFromSubmenu) {
        const currentPath = route.path
        for (const menu of adminMenus.value) {
          if (menu.children) {
            const hasActiveChild = menu.children.some(c => c.url === currentPath)
            // 只有在子菜單不在 manuallyClosedMenus 中時才自動展開
            if (hasActiveChild && !manuallyClosedMenus.has(menu.id) && !expandedMenus.value.includes(menu.id)) {
              expandedMenus.value.push(menu.id)
            }
          }
        }
      }
    }
  } catch (error) {
    console.error('載入後台菜單失敗:', error)
  }
}

const handleLogout = async () => {
  await logout()
  router.push('/login')
}

// 監聽路由變化，自動展開包含當前路由的父菜單
watch(() => route.path, (newPath, oldPath) => {
  // 首先，無論什麼情況，都要確保被手動關閉的子菜單保持關閉狀態
  manuallyClosedMenus.forEach((menuId) => {
    const index = expandedMenus.value.indexOf(menuId)
    if (index > -1) {
      expandedMenus.value.splice(index, 1)
    }
  })
  
  // 檢查當前路由是否屬於被手動關閉的子菜單
  let isCurrentRouteInClosedSubmenu = false
  for (const menu of adminMenus.value) {
    if (manuallyClosedMenus.has(menu.id) && isChildOfMenu(menu)) {
      isCurrentRouteInClosedSubmenu = true
      break
    }
  }
  
  // 如果當前路由屬於被手動關閉的子菜單，絕對不自動展開
  if (isCurrentRouteInClosedSubmenu) {
    return
  }
  
  // 如果用戶正在手動操作或剛剛從子菜單導航，不自動展開或關閉
  if (isUserClicking || justNavigatedFromSubmenu) {
    return
  }
  
  // 如果 oldPath 存在且不是根路徑，說明這是導航操作，不自動展開
  if (oldPath !== undefined && oldPath !== null && oldPath !== '/') {
    return
  }
  
  // 只有在頁面首次加載時（oldPath 為 undefined 或 null）才自動展開
  // 自動展開包含當前路由的父菜單（僅在首次加載時），但跳過被手動關閉的子菜單
  let foundActiveMenu = false
  for (const menu of adminMenus.value) {
    if (menu.children) {
      const hasActiveChild = menu.children.some(c => c.url === newPath)
      // 只有在子菜單不在 manuallyClosedMenus 中時才自動展開
      if (hasActiveChild && !manuallyClosedMenus.has(menu.id)) {
        if (!expandedMenus.value.includes(menu.id)) {
          expandedMenus.value.push(menu.id)
        }
        foundActiveMenu = true
      }
    }
  }
  
  // 如果當前路由不屬於任何子菜單，關閉所有子菜單（但保留被手動關閉的記錄）
  if (!foundActiveMenu) {
    // 只有在子菜單當前打開時才關閉
    for (const menu of adminMenus.value) {
      if (menu.children && expandedMenus.value.includes(menu.id)) {
        expandedMenus.value.splice(expandedMenus.value.indexOf(menu.id), 1)
      }
    }
  }
}, { immediate: false })

onMounted(() => {
  loadAdminMenus()
})

onUnmounted(() => {
  if (clickTimeout) {
    clearTimeout(clickTimeout)
  }
})
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f5f5f5;
}

.admin-navbar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 1rem 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;
}

.title-link {
  text-decoration: none;
  color: white;
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
  font-weight: 700;
  color: white;
}

.navbar-menu {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex: 1;
  justify-content: center;
  flex-wrap: wrap;
}

.menu-item-wrapper {
  position: relative;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  text-decoration: none;
  color: white;
  transition: all 0.2s;
  border: 1px solid rgba(255, 255, 255, 0.25);
  cursor: pointer;
  font-weight: 600;
  font-size: 0.95rem;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

.menu-item.active {
  background: rgba(255, 255, 255, 0.3);
  font-weight: 700;
}

.menu-item.has-submenu .arrow {
  transition: transform 0.3s;
  margin-left: 0.25rem;
  font-size: 0.7rem;
}

.menu-item.has-submenu.active .arrow {
  transform: rotate(180deg);
}

.menu-icon {
  font-size: 1.1rem;
}

.menu-text {
  white-space: nowrap;
}

.submenu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 0.5rem;
  background: white;
  border-radius: 8px;
  min-width: 200px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.submenu-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  color: #333;
  text-decoration: none;
  border-bottom: 1px solid #e0e0e0;
  transition: all 0.2s;
  font-weight: 500;
}

.submenu-item:last-child {
  border-bottom: none;
}

.submenu-item:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  padding-left: 1.25rem;
}

.submenu-item.active {
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
  font-weight: 700;
  border-left: 4px solid #667eea;
}

.submenu-icon {
  font-size: 1rem;
}

.navbar-user {
  flex-shrink: 0;
}

.user-info {
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
  transition: background 0.2s;
  font-weight: 600;
}

.logout-button:hover {
  background: rgba(255, 255, 255, 0.3);
}

.admin-main {
  min-height: calc(100vh - 80px);
}

.admin-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}
</style>

