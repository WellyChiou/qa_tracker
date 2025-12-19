<template>
  <div class="admin-layout">
    <nav class="admin-navbar">
      <div class="navbar-container">
        <router-link to="/" class="title-link">
          <img src="/images/logo.png" alt="極光教會 Logo" class="logo-image" />
          <span class="logo-text">極光教會-PLC</span>
        </router-link>
        <button
          class="mobile-menu-btn"
          type="button"
          @click="mobileMenuOpen = !mobileMenuOpen"
          aria-label="開啟選單"
        >
          ☰
        </button>
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
    <!-- Mobile Drawer -->
    <div
      v-if="isMobile && mobileMenuOpen"
      class="mobile-drawer-overlay"
      @click.self="mobileMenuOpen = false"
    >
      <div class="mobile-drawer">
        <div class="mobile-drawer-top">
          <div class="mobile-brand">
            <img src="/images/logo.png" alt="極光教會 Logo" class="logo-image" />
            <span class="logo-text">極光教會-PLC</span>
          </div>
          <button class="mobile-close" type="button" @click="mobileMenuOpen = false">×</button>
        </div>

        <div class="mobile-user">
          <div class="mobile-user-name">{{ currentUser?.displayName || currentUser?.username }}</div>
          <button @click="handleLogout" class="logout-button">登出</button>
        </div>

        <div class="mobile-menu">
          <template v-for="menu in adminMenus" :key="'m-' + menu.id">
            <div v-if="menu.children && menu.children.length > 0" class="mobile-menu-group">
              <button
                class="mobile-menu-item"
                type="button"
                @click.stop="toggleMenu(menu.id)"
                :class="{ active: expandedMenus.includes(menu.id) }"
              >
                <span class="menu-icon">{{ menu.icon }}</span>
                <span class="menu-text">{{ menu.menuName }}</span>
                <span class="arrow" :class="{ open: expandedMenus.includes(menu.id) }">▼</span>
              </button>

              <div v-if="expandedMenus.includes(menu.id)" class="mobile-submenu">
                <router-link
                  v-for="child in menu.children"
                  :key="'m-' + child.id"
                  :to="child.url"
                  class="mobile-submenu-item"
                  :class="{ active: $route.path === child.url }"
                  @click="handleMobileSubmenuClick(menu.id, $event)"
                >
                  <span class="submenu-icon">{{ child.icon }}</span>
                  {{ child.menuName }}
                </router-link>
              </div>
            </div>

            <router-link
              v-else
              :to="menu.url"
              class="mobile-menu-item"
              :class="{ active: $route.path === menu.url }"
              @click="closeMobileMenu"
            >
              <span class="menu-icon">{{ menu.icon }}</span>
              <span class="menu-text">{{ menu.menuName }}</span>
            </router-link>
          </template>
        </div>
      </div>
    </div>

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

const mobileMenuOpen = ref(false)
const isMobile = ref(typeof window !== 'undefined' ? window.innerWidth <= 860 : false)

const closeMobileMenu = () => {
  mobileMenuOpen.value = false
}

const handleMobileSubmenuClick = (menuId, event) => {
  handleSubmenuClick(menuId, event)
  mobileMenuOpen.value = false
}

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
let _resizeHandler = null

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

  const handleResize = () => {
    isMobile.value = window.innerWidth <= 860
    if (!isMobile.value) {
      mobileMenuOpen.value = false
    }
  }

  window.addEventListener('resize', handleResize, { passive: true })
  handleResize()

  // 保存到 closure，供 unmounted 移除
  _resizeHandler = handleResize
})

onUnmounted(() => {
  if (clickTimeout) {
    clearTimeout(clickTimeout)
  }
  if (_resizeHandler) {
    window.removeEventListener('resize', _resizeHandler)
    _resizeHandler = null
  }
})
</script>

<style scoped>
.admin-layout{
  min-height:100vh;
  background:var(--bg);
}

/* Top bar */
.admin-navbar{
  position:sticky;
  top:0;
  z-index:50;
  background:var(--nav-bg);
  border-bottom:1px solid var(--nav-border);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.navbar-container{
  max-width:1100px;
  margin:0 auto;
  padding:12px 16px;
  display:flex;
  align-items:center;
  gap:14px;
}

.title-link{
  display:flex;
  align-items:center;
  gap:10px;
  padding:8px 10px;
  border-radius:14px;
  border:1px solid rgba(2,6,23,.08);
  background:rgba(255,255,255,.7);
  box-shadow:0 6px 18px rgba(2,6,23,.06);
}

.logo-image{
  width:30px;
  height:30px;
  object-fit:contain;
  border-radius:10px;
}
.logo-text{
  font-weight:900;
  letter-spacing:-0.02em;
  font-size:14px;
  color:var(--text);
}

/* Menu */
.navbar-menu{
  flex:1;
  display:flex;
  align-items:center;
  gap:8px;
  flex-wrap:wrap;
}

.menu-item-wrapper{ position:relative; }

.menu-item{
  display:inline-flex;
  align-items:center;
  gap:8px;
  padding:8px 10px;
  border-radius:14px;
  border:1px solid transparent;
  background:transparent;
  cursor:pointer;
  color:rgba(15,23,42,.82);
  font-weight:800;
  font-size:13px;
  transition:background .12s ease, border-color .12s ease, transform .12s ease;
  user-select:none;
}
.menu-item:hover{
  background:rgba(2,6,23,.04);
  border-color:rgba(2,6,23,.08);
  transform:translateY(-1px);
}
.menu-item.active{
  background:rgba(37,99,235,.10);
  border-color:rgba(37,99,235,.22);
  color:var(--primary);
}

.menu-icon{
  width:18px;
  text-align:center;
  opacity:.9;
}
.menu-text{ white-space:nowrap; }
.arrow{
  margin-left:2px;
  font-size:10px;
  opacity:.7;
}

/* Submenu */
.submenu{
  position:absolute;
  top:calc(100% + 8px);
  left:0;
  min-width:220px;
  padding:10px;
  border-radius:16px;
  border:1px solid rgba(2,6,23,.10);
  background:rgba(255,255,255,.92);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: var(--shadow);
}

.submenu-item{
  display:flex;
  align-items:center;
  gap:10px;
  padding:10px 10px;
  border-radius:14px;
  font-weight:800;
  font-size:13px;
  color:rgba(15,23,42,.82);
  border:1px solid transparent;
  transition:background .12s ease, border-color .12s ease, transform .12s ease;
}
.submenu-item:hover{
  background:rgba(2,6,23,.04);
  border-color:rgba(2,6,23,.08);
  transform:translateY(-1px);
}
.submenu-item.active{
  background:rgba(37,99,235,.10);
  border-color:rgba(37,99,235,.22);
  color:var(--primary);
}
.submenu-icon{
  width:18px;
  text-align:center;
  opacity:.85;
}

/* User */
.navbar-user{
  display:flex;
  align-items:center;
  gap:10px;
}
.user-info{
  display:flex;
  align-items:center;
  gap:10px;
  padding:6px 8px;
  border-radius:14px;
  border:1px solid rgba(2,6,23,.08);
  background:rgba(255,255,255,.7);
}
.user-name{
  font-weight:900;
  font-size:13px;
  color:rgba(15,23,42,.85);
}
.logout-button{
  border:none;
  background:rgba(239,68,68,.10);
  color:#b91c1c;
  font-weight:900;
  font-size:12px;
  padding:8px 10px;
  border-radius:12px;
  cursor:pointer;
  transition:transform .12s ease, background .12s ease;
}
.logout-button:hover{ transform:translateY(-1px); background:rgba(239,68,68,.14); }

/* Main content */
.admin-main{ width:100%; }
.admin-content{
  max-width:1100px;
  margin:0 auto;
  padding:18px 16px 32px;
}


/* Mobile menu toggle */
.mobile-menu-btn{
  display:none;
  align-items:center;
  justify-content:center;
  width:40px;
  height:40px;
  border-radius:14px;
  border:1px solid rgba(2,6,23,.10);
  background:rgba(255,255,255,.75);
  box-shadow:0 6px 18px rgba(2,6,23,.06);
  cursor:pointer;
  font-size:18px;
  font-weight:900;
  color:rgba(15,23,42,.85);
}

/* Mobile Drawer */
.mobile-drawer-overlay{
  position:fixed;
  inset:0;
  background:rgba(2,6,23,.45);
  z-index:80;
  display:flex;
  align-items:stretch;
}

.mobile-drawer{
  width:min(86vw, 360px);
  height:100%;
  background:rgba(255,255,255,.98);
  border-right:1px solid rgba(2,6,23,.10);
  box-shadow: 0 20px 60px rgba(2,6,23,.30);
  padding:14px 12px 18px;
  overflow:auto;
}

.mobile-drawer-top{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:10px;
  padding:6px 6px 12px;
  border-bottom:1px solid rgba(2,6,23,.08);
}
.mobile-brand{
  display:flex;
  align-items:center;
  gap:10px;
}
.mobile-close{
  width:36px;
  height:36px;
  border-radius:12px;
  border:1px solid rgba(2,6,23,.10);
  background:rgba(2,6,23,.04);
  cursor:pointer;
  font-size:20px;
  line-height:1;
}

.mobile-user{
  margin:12px 6px 10px;
  padding:10px 10px;
  border-radius:16px;
  border:1px solid rgba(2,6,23,.08);
  background:rgba(2,6,23,.02);
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:10px;
}
.mobile-user-name{
  font-weight:900;
  color:rgba(15,23,42,.85);
  font-size:13px;
}

.mobile-menu{
  padding:8px 6px 0;
  display:flex;
  flex-direction:column;
  gap:8px;
}

.mobile-menu-item{
  width:100%;
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:10px;
  padding:12px 12px;
  border-radius:16px;
  border:1px solid rgba(2,6,23,.08);
  background:rgba(255,255,255,.85);
  font-weight:900;
  font-size:13px;
  color:rgba(15,23,42,.84);
  cursor:pointer;
}

.mobile-menu-item.active{
  background:rgba(37,99,235,.10);
  border-color:rgba(37,99,235,.22);
  color:var(--primary);
}

.mobile-submenu{
  margin-top:6px;
  margin-left:8px;
  padding-left:10px;
  border-left:2px solid rgba(2,6,23,.08);
  display:flex;
  flex-direction:column;
  gap:6px;
}

.mobile-submenu-item{
  display:flex;
  align-items:center;
  gap:10px;
  padding:10px 12px;
  border-radius:14px;
  border:1px solid rgba(2,6,23,.08);
  background:rgba(255,255,255,.80);
  font-weight:800;
  font-size:13px;
  color:rgba(15,23,42,.80);
}
.mobile-submenu-item.active{
  background:rgba(37,99,235,.10);
  border-color:rgba(37,99,235,.22);
  color:var(--primary);
}

.arrow.open{ transform:rotate(180deg); }

/* Responsive */
@media (max-width: 860px){
  .navbar-container{ align-items:center; }
  .navbar-menu{ display:none; }
  .mobile-menu-btn{ display:inline-flex; }
  .navbar-user{ margin-left:auto; }
  .navbar-user .user-name{ display:none; }
  .user-info{ padding:6px 6px; }
  .logout-button{ padding:8px 10px; }
}
</style>
