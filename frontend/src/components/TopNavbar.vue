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
          class="menu-item-wrapper"
        >
          <div
            class="menu-item has-submenu"
            @click.stop="toggleSubmenu(index, $event)"
            :class="{ active: activeSubmenu === index }"
          >
            {{ menu.icon || '' }} {{ menu.menuName }}
            <span class="arrow">▼</span>
          </div>
          <div v-if="activeSubmenu === index" class="submenu" @click.stop>
            <template v-for="child in menu.children" :key="child?.id || child">
              <router-link
                v-if="child && child.url && child.url !== '#'"
                :to="normalizeMenuUrl(child.url)"
                class="submenu-item"
                :class="{ active: $route.path.startsWith(normalizeMenuUrl(child.url)) }"
                @click.stop="handleSubmenuClick(index, $event)"
              >
                <span class="submenu-icon">{{ child.icon || '📄' }}</span>
                {{ child.menuName }}
              </router-link>
            </template>
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
</template>

<script setup>
import { ref, onMounted, watch, computed, Transition } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiService } from '@/composables/useApi'

// 模組級變量：記錄被用戶手動關閉的子菜單索引（永久記錄，直到用戶手動展開）
// 使用 sessionStorage 確保在組件重新創建時不會丟失
const getManuallyClosedSubmenus = () => {
  try {
    const stored = sessionStorage.getItem('manuallyClosedSubmenus')
    return stored ? new Set(JSON.parse(stored)) : new Set()
  } catch {
    return new Set()
  }
}

const saveManuallyClosedSubmenus = (set) => {
  try {
    sessionStorage.setItem('manuallyClosedSubmenus', JSON.stringify(Array.from(set)))
  } catch (e) {
    console.error('保存 manuallyClosedSubmenus 失敗:', e)
  }
}

let manuallyClosedSubmenus = getManuallyClosedSubmenus()

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
let justNavigatedFromSubmenu = false // 標記是否剛剛從子菜單導航
let clickTimeout = null

// 監聽路由變化，自動展開包含當前路由的父菜單
watch(() => route.path, (newPath, oldPath) => {
  console.log('路由變化:', oldPath, '->', newPath, 'isUserClicking:', isUserClicking, 'justNavigatedFromSubmenu:', justNavigatedFromSubmenu, 'manuallyClosedSubmenus:', Array.from(manuallyClosedSubmenus))
  
  // 首先，無論什麼情況，都要確保被手動關閉的子菜單保持關閉狀態
  menus.value.forEach((menu, index) => {
    if (manuallyClosedSubmenus.has(index)) {
      // 強制關閉子菜單，無論當前狀態如何
      activeSubmenu.value = null
      console.log('路由變化，強制關閉被用戶手動關閉的子菜單（優先檢查）:', index, 'manuallyClosedSubmenus:', Array.from(manuallyClosedSubmenus))
    }
  })
  
  // 檢查當前路由是否屬於被手動關閉的子菜單
  let isCurrentRouteInClosedSubmenu = false
  menus.value.forEach((menu, index) => {
    if (manuallyClosedSubmenus.has(index) && isChildOfMenu(menu)) {
      isCurrentRouteInClosedSubmenu = true
      console.log('路由變化，檢測到當前路由屬於被手動關閉的子菜單:', index)
    }
  })
  
  // 如果當前路由屬於被手動關閉的子菜單，絕對不自動展開
  if (isCurrentRouteInClosedSubmenu) {
    console.log('路由變化，跳過自動展開（當前路由屬於被手動關閉的子菜單）')
    return
  }
  
  // 如果用戶正在手動操作或剛剛從子菜單導航，不自動展開或關閉
  if (isUserClicking || justNavigatedFromSubmenu) {
    console.log('路由變化，但用戶正在操作或剛剛從子菜單導航，跳過自動展開')
    return
  }
  
  // 如果 oldPath 存在且不是根路徑，說明這是導航操作，不自動展開
  if (oldPath !== undefined && oldPath !== null && oldPath !== '/') {
    console.log('路由變化，這是導航操作（oldPath:', oldPath, '），不自動展開子菜單')
    return
  }
  
  // 只有在頁面首次加載時（oldPath 為 undefined 或 null）才自動展開
  // 自動展開包含當前路由的父菜單（僅在首次加載時），但跳過被手動關閉的子菜單
  let foundActiveMenu = false
  menus.value.forEach((menu, index) => {
    if (isChildOfMenu(menu) && !manuallyClosedSubmenus.has(index)) {
      // 只有在子菜單當前關閉時才自動展開
      if (activeSubmenu.value !== index) {
        activeSubmenu.value = index
        console.log('首次加載，自動展開子菜單:', index)
      }
      foundActiveMenu = true
    }
  })
  
  // 如果當前路由不屬於任何子菜單，關閉所有子菜單
  if (!foundActiveMenu) {
    // 只有在子菜單當前打開時才關閉
    if (activeSubmenu.value !== null) {
      console.log('路由變化，關閉所有子菜單')
      activeSubmenu.value = null
    }
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
    // 記錄用戶手動關閉的子菜單
    manuallyClosedSubmenus.add(index)
    saveManuallyClosedSubmenus(manuallyClosedSubmenus)
    console.log('關閉子菜單:', index, 'activeSubmenu.value:', activeSubmenu.value, 'manuallyClosedSubmenus:', Array.from(manuallyClosedSubmenus))
  } else {
    activeSubmenu.value = index
    // 當用戶手動展開子菜單時，從關閉記錄中移除，允許正常的自動展開邏輯
    manuallyClosedSubmenus.delete(index)
    saveManuallyClosedSubmenus(manuallyClosedSubmenus)
    console.log('打開子菜單:', index, 'activeSubmenu.value:', activeSubmenu.value, 'manuallyClosedSubmenus:', Array.from(manuallyClosedSubmenus), 'type:', typeof activeSubmenu.value, 'type of index:', typeof index)
    // 檢查菜單是否有子項
    const menu = menus.value[index]
    console.log('菜單:', menu?.menuName, '子項數量:', menu?.children?.length, '子項:', menu?.children)
  }
  
  // 延長到 1000ms 後重置標記，確保子菜單有足夠時間顯示
  clickTimeout = setTimeout(() => {
    isUserClicking = false
    console.log('重置 isUserClicking 標記，當前 activeSubmenu.value:', activeSubmenu.value)
  }, 1000)
}

// 點擊子菜單項時的處理（點擊後關閉子菜單）
const handleSubmenuClick = (menuIndex, event) => {
  // 阻止事件冒泡
  if (event) {
    event.stopPropagation()
  }
  
  // 記錄被手動關閉的子菜單（永久記錄，直到用戶手動展開）
  manuallyClosedSubmenus.add(menuIndex)
  saveManuallyClosedSubmenus(manuallyClosedSubmenus)
  
  // 立即設置標記，在路由變化之前
  isUserClicking = true
  justNavigatedFromSubmenu = true
  console.log('點擊子菜單項，設置標記 - isUserClicking:', isUserClicking, 'justNavigatedFromSubmenu:', justNavigatedFromSubmenu, 'manuallyClosedSubmenus:', Array.from(manuallyClosedSubmenus))
  
  // 立即關閉子菜單
  activeSubmenu.value = null
  console.log('點擊子菜單項，關閉子菜單，activeSubmenu.value:', activeSubmenu.value)
  
  // 延長標記時間，防止 watch 自動重新打開
  if (clickTimeout) {
    clearTimeout(clickTimeout)
  }
  
  // 使用更長的時間，確保路由變化完成後也不自動展開
  clickTimeout = setTimeout(() => {
    isUserClicking = false
    justNavigatedFromSubmenu = false
    console.log('重置 isUserClicking 和 justNavigatedFromSubmenu 標記（子菜單點擊後）')
  }, 2000) // 2秒後重置標記
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
    // 從 sessionStorage 恢復 manuallyClosedSubmenus（防止組件重新創建時丟失）
    manuallyClosedSubmenus = getManuallyClosedSubmenus()
    
    // 首先，無論什麼情況，都要確保被手動關閉的子菜單保持關閉狀態
    console.log('loadMenus 開始執行，manuallyClosedSubmenus:', Array.from(manuallyClosedSubmenus), 'manuallyClosedSubmenus.size:', manuallyClosedSubmenus.size, 'isUserClicking:', isUserClicking, 'justNavigatedFromSubmenu:', justNavigatedFromSubmenu, '當前路由:', route.path)
    
    // 強制關閉所有在 manuallyClosedSubmenus 中的子菜單
    if (manuallyClosedSubmenus.size > 0) {
      manuallyClosedSubmenus.forEach((index) => {
        activeSubmenu.value = null
        console.log('載入菜單後，強制關閉被用戶手動關閉的子菜單:', index)
      })
    }
    
    // 檢查當前路由是否屬於被手動關閉的子菜單
    let isCurrentRouteInClosedSubmenu = false
    manuallyClosedSubmenus.forEach((index) => {
      const menu = menus.value[index]
      if (menu && isChildOfMenu(menu)) {
        isCurrentRouteInClosedSubmenu = true
        console.log('載入菜單後，檢測到當前路由屬於被手動關閉的子菜單:', index, '當前路由:', route.path, '菜單:', menu.menuName)
      }
    })
    
    console.log('isCurrentRouteInClosedSubmenu:', isCurrentRouteInClosedSubmenu)
    
    // 如果當前路由屬於被手動關閉的子菜單，絕對不自動展開
    if (isCurrentRouteInClosedSubmenu) {
      console.log('載入菜單，跳過自動展開（當前路由屬於被手動關閉的子菜單）')
      return
    }
    
    // 額外檢查：如果 manuallyClosedSubmenus 中有任何索引，並且該菜單有子項，就檢查當前路由
    // 這是一個額外的安全檢查
    let foundInClosedSubmenu = false
    manuallyClosedSubmenus.forEach((index) => {
      const menu = menus.value[index]
      if (menu && menu.children && menu.children.length > 0) {
        const currentPath = route.path
        const isInSubmenu = menu.children.some(child => {
          if (!child || !child.url) return false
          const childUrl = normalizeMenuUrl(child.url)
          return childUrl && currentPath.startsWith(childUrl)
        })
        if (isInSubmenu) {
          foundInClosedSubmenu = true
          console.log('載入菜單，額外檢查發現當前路由屬於被手動關閉的子菜單:', index)
        }
      }
    })
    
    if (foundInClosedSubmenu) {
      console.log('載入菜單，額外檢查確認，跳過自動展開')
      return
    }
    
    // 加載菜單後，檢查是否需要自動展開（只有在不是用戶手動操作且不是剛剛從子菜單導航的情況下）
    // 並且跳過被手動關閉的子菜單
    console.log('準備檢查是否自動展開，isUserClicking:', isUserClicking, 'justNavigatedFromSubmenu:', justNavigatedFromSubmenu)
    if (!isUserClicking && !justNavigatedFromSubmenu) {
      menus.value.forEach((menu, index) => {
        // 雙重檢查：確保不在 manuallyClosedSubmenus 中，並且是當前路由的子菜單
        if (isChildOfMenu(menu) && !manuallyClosedSubmenus.has(index)) {
          // 再次確認：如果這個索引在 manuallyClosedSubmenus 中，絕對不展開
          if (!manuallyClosedSubmenus.has(index)) {
            activeSubmenu.value = index
            console.log('載入菜單後，自動展開子菜單:', index)
          } else {
            console.log('載入菜單後，跳過自動展開（子菜單在 manuallyClosedSubmenus 中）:', index)
          }
        }
      })
    } else {
      console.log('載入菜單，但跳過自動展開 - isUserClicking:', isUserClicking, 'justNavigatedFromSubmenu:', justNavigatedFromSubmenu, 'manuallyClosedSubmenus:', Array.from(manuallyClosedSubmenus))
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
  overflow: visible; /* 確保子菜單可以顯示 */
  z-index: 1;
  display: inline-block; /* 確保包裝器不會影響佈局 */
}

.menu-item {
  padding: 0.75rem 1.25rem;
  background: rgba(255, 255, 255, 0.15);
  border-radius: var(--border-radius);
  text-decoration: none;
  color: white;
  transition: background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease; /* 明確指定過渡屬性，避免閃爍 */
  border: 1px solid rgba(255, 255, 255, 0.25);
  display: inline-block;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.95rem;
  backdrop-filter: blur(10px);
  position: relative;
  overflow: visible; /* 改為 visible，讓子菜單可以顯示 */
  will-change: transform; /* 優化動畫性能 */
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
  pointer-events: none; /* 防止影響點擊事件 */
  z-index: -1; /* 確保在內容下方 */
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
  z-index: 10000 !important;
  overflow: hidden;
  border: 1px solid var(--border-color);
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
}

/* 移除 Transition 相關的 CSS，因為我們不再使用 Transition 組件 */

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

