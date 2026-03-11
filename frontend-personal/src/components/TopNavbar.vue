<template>
  <nav class="navbar">
    <div class="navbar-shell">
      <router-link to="/" class="navbar-title">
        <span class="brand-chip">Ops</span>
        <div class="brand-copy">
          <h1>個人系統控制台</h1>
          <p>記帳、任務與設定集中管理</p>
        </div>
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
          <span v-else class="menu-item menu-item--muted">
            {{ menu.icon || '' }} {{ menu.menuName }}
          </span>
        </div>
      </div>

      <div v-if="currentUser" class="user-info">
        <div class="user-menu-wrapper">
          <button class="user-name-btn" @click="toggleUserMenu">
            <span class="user-avatar">
              {{ (currentUser.displayName || currentUser.username || 'U').slice(0, 1).toUpperCase() }}
            </span>
            <span class="user-copy">
              <strong>{{ currentUser.displayName || currentUser.username || currentUser.email || '用戶' }}</strong>
              <small>已登入工作區</small>
            </span>
            <i class="fas fa-chevron-down ms-1" :class="{ rotate: showUserMenu }"></i>
          </button>

          <div v-if="showUserMenu" class="user-menu">
            <button class="user-menu-item" @click="openProfileModal">
              <i class="fas fa-user-cog me-2"></i>
              個人資料設定
            </button>
            <hr class="user-menu-divider">
            <button class="user-menu-item logout-btn" @click="handleLogout">
              <i class="fas fa-sign-out-alt me-2"></i>
              登出
            </button>
          </div>
        </div>
      </div>
    </div>
  </nav>

  <ProfileModal :show="showProfileModal" @close="closeProfileModal" />
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiService } from '@/composables/useApi'
import ProfileModal from './ProfileModal.vue'

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
const showUserMenu = ref(false)
const showProfileModal = ref(false)

const handleClickOutside = (event) => {
  const target = event.target
  const menuWrapper = target.closest('.menu-item-wrapper')
  const submenu = target.closest('.submenu')
  const menuItem = target.closest('.menu-item.has-submenu')
  const navbarMenu = target.closest('.navbar-menu')
  const userMenuWrapper = target.closest('.user-menu-wrapper')

  if (!menuWrapper && !submenu && !menuItem && !navbarMenu) {
    activeSubmenu.value = null
  }

  if (!userMenuWrapper && showUserMenu.value) {
    showUserMenu.value = false
  }
}

const normalizeMenuUrl = (url) => {
  if (!url || url === '#') return url
  return url.replace(/\.html$/, '')
}

const isChildOfMenu = (menu) => {
  if (!menu || !menu.children || menu.children.length === 0) return false
  const currentPath = route.path
  return menu.children.some(child => {
    if (!child || !child.url) return false
    const childUrl = normalizeMenuUrl(child.url)
    return childUrl && currentPath.startsWith(childUrl)
  })
}

let isUserClicking = false
let justNavigatedFromSubmenu = false
let clickTimeout = null

watch(() => route.path, (newPath, oldPath) => {
  menus.value.forEach((menu, index) => {
    if (manuallyClosedSubmenus.has(index)) {
      activeSubmenu.value = null
    }
  })

  let isCurrentRouteInClosedSubmenu = false
  menus.value.forEach((menu, index) => {
    if (manuallyClosedSubmenus.has(index) && isChildOfMenu(menu)) {
      isCurrentRouteInClosedSubmenu = true
    }
  })

  if (isCurrentRouteInClosedSubmenu || isUserClicking || justNavigatedFromSubmenu) {
    return
  }

  if (oldPath !== undefined && oldPath !== null && oldPath !== '/') {
    return
  }

  let foundActiveMenu = false
  menus.value.forEach((menu, index) => {
    if (isChildOfMenu(menu) && !manuallyClosedSubmenus.has(index)) {
      if (activeSubmenu.value !== index) {
        activeSubmenu.value = index
      }
      foundActiveMenu = true
    }
  })

  if (!foundActiveMenu && activeSubmenu.value !== null) {
    activeSubmenu.value = null
  }
}, { immediate: false })

const toggleSubmenu = (index) => {
  if (clickTimeout) {
    clearTimeout(clickTimeout)
  }

  isUserClicking = true

  const wasOpen = activeSubmenu.value === index
  if (wasOpen) {
    activeSubmenu.value = null
    manuallyClosedSubmenus.add(index)
    saveManuallyClosedSubmenus(manuallyClosedSubmenus)
  } else {
    activeSubmenu.value = index
    manuallyClosedSubmenus.delete(index)
    saveManuallyClosedSubmenus(manuallyClosedSubmenus)
  }

  clickTimeout = setTimeout(() => {
    isUserClicking = false
  }, 1000)
}

const handleSubmenuClick = (menuIndex, event) => {
  if (event) {
    event.stopPropagation()
  }

  manuallyClosedSubmenus.add(menuIndex)
  saveManuallyClosedSubmenus(manuallyClosedSubmenus)

  isUserClicking = true
  justNavigatedFromSubmenu = true
  activeSubmenu.value = null

  if (clickTimeout) {
    clearTimeout(clickTimeout)
  }

  clickTimeout = setTimeout(() => {
    isUserClicking = false
    justNavigatedFromSubmenu = false
  }, 2000)
}

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const closeUserMenu = () => {
  showUserMenu.value = false
}

const openProfileModal = () => {
  closeUserMenu()
  showProfileModal.value = true
}

const closeProfileModal = () => {
  showProfileModal.value = false
}

const handleLogout = async () => {
  closeUserMenu()
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

    manuallyClosedSubmenus = getManuallyClosedSubmenus()

    if (manuallyClosedSubmenus.size > 0) {
      manuallyClosedSubmenus.forEach(() => {
        activeSubmenu.value = null
      })
    }

    let isCurrentRouteInClosedSubmenu = false
    manuallyClosedSubmenus.forEach((index) => {
      const menu = menus.value[index]
      if (menu && isChildOfMenu(menu)) {
        isCurrentRouteInClosedSubmenu = true
      }
    })

    if (isCurrentRouteInClosedSubmenu) {
      return
    }

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
        }
      }
    })

    if (foundInClosedSubmenu) {
      return
    }

    if (!isUserClicking && !justNavigatedFromSubmenu) {
      menus.value.forEach((menu, index) => {
        if (isChildOfMenu(menu) && !manuallyClosedSubmenus.has(index)) {
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
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 0;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(14px);
}

.navbar-shell {
  max-width: 1180px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding: 0.75rem 1rem;
}

.navbar-title {
  display: flex;
  align-items: center;
  gap: 1rem;
  min-width: 0;
  text-decoration: none;
  color: var(--text-primary);
}

.brand-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  min-height: 48px;
  padding: 0.55rem;
  border-radius: 14px;
  background: linear-gradient(145deg, #0f172a, #1d4ed8);
  color: #f8fafc;
  font-size: 0.66rem;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  box-shadow: 0 8px 18px rgba(29, 78, 216, 0.14);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.brand-copy h1 {
  margin: 0;
  font-size: clamp(1.06rem, 1.55vw, 1.42rem);
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: #0f172a;
}

.brand-copy p {
  margin: 0;
  color: rgba(15, 23, 42, 0.62);
  font-size: 0.78rem;
  font-weight: 600;
}

.navbar-menu {
  flex: 1;
  display: flex;
  gap: 0.35rem;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
}

.menu-item-wrapper {
  position: relative;
  overflow: visible;
  z-index: 1;
  display: inline-block;
}

.menu-item {
  padding: 0.62rem 0.82rem;
  background: rgba(248, 250, 252, 0.94);
  border-radius: 10px;
  text-decoration: none;
  color: rgba(15, 23, 42, 0.76);
  transition: background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  border: 1px solid rgba(148, 163, 184, 0.22);
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  cursor: pointer;
  font-weight: 800;
  font-size: 0.84rem;
  backdrop-filter: none;
  position: relative;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 1);
  transform: none;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.05);
  border-color: rgba(37, 99, 235, 0.18);
}

.menu-item--muted {
  cursor: default;
  opacity: 0.62;
}

.menu-item.has-submenu.active .arrow {
  transform: rotate(180deg);
}

.arrow {
  display: inline-block;
  transition: transform 0.3s;
  margin-left: 0.25rem;
  font-size: 0.62rem;
}

.submenu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: var(--spacing-sm);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(20px);
  border-radius: 14px;
  min-width: 220px;
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.12);
  z-index: 10000 !important;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.18);
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
}

.submenu-item {
  display: block;
  padding: 0.8rem 0.9rem;
  color: var(--text-primary);
  text-decoration: none;
  border-bottom: 1px solid rgba(226, 232, 240, 0.72);
  transition: var(--transition);
  font-weight: 700;
  font-size: 0.84rem;
}

.submenu-item:last-child {
  border-bottom: none;
}

.submenu-item:hover {
  background: rgba(59, 130, 246, 0.08);
  color: var(--primary-color);
  padding-left: calc(1.1rem + 4px);
}

.submenu-item.active {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.14) 0%, rgba(59, 130, 246, 0.04) 100%);
  color: var(--primary-color);
  font-weight: 800;
  border-left: 4px solid var(--primary-color);
}

.submenu-icon {
  margin-right: 0.5rem;
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.user-menu-wrapper {
  position: relative;
}

.user-name-btn {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.5rem 0.68rem;
  background: rgba(248, 250, 252, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 10px;
  color: #0f172a;
  cursor: pointer;
  transition: var(--transition);
  font-size: 0.84rem;
  box-shadow: none;
  backdrop-filter: none;
}

.user-name-btn:hover {
  background: rgba(255, 255, 255, 1);
  transform: none;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
}

.user-avatar {
  width: 2rem;
  height: 2rem;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: linear-gradient(135deg, #1d4ed8, #0891b2);
  color: #fff;
  font-weight: 900;
}

.user-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.1;
}

.user-copy strong {
  font-size: 0.82rem;
  color: #0f172a;
}

.user-copy small {
  color: rgba(15, 23, 42, 0.58);
  font-size: 0.68rem;
  font-weight: 700;
}

.rotate {
  transform: rotate(180deg);
}

.user-menu {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  min-width: 200px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 14px;
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.14);
  border: 1px solid rgba(148, 163, 184, 0.18);
  z-index: 1000;
  overflow: hidden;
}

.user-menu-item {
  display: flex;
  align-items: center;
  width: 100%;
  background: none;
  border: none;
  text-align: left;
  padding: 0.78rem 0.9rem;
  color: #334155;
  text-decoration: none;
  transition: var(--transition);
  cursor: pointer;
  font-weight: 700;
  font-size: 0.84rem;
}

.user-menu-item:hover {
  background: #f8fafc;
  color: #0f172a;
}

.user-menu-divider {
  margin: 0.25rem 0;
  border: 0;
  border-top: 1px solid #e9ecef;
}

.logout-btn {
  color: #dc2626;
}

.logout-btn:hover {
  background: #fee2e2;
  color: #991b1b;
}

@media (max-width: 1120px) {
  .navbar-shell {
    flex-direction: column;
    align-items: stretch;
  }

  .navbar-menu {
    justify-content: flex-start;
  }

  .user-info {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .navbar {
    padding: 0.85rem 0.85rem 0;
  }

  .brand-chip {
    min-width: 56px;
    min-height: 56px;
    border-radius: 16px;
    font-size: 0.64rem;
  }

  .brand-copy p {
    font-size: 0.82rem;
  }

  .menu-item {
    padding: 0.66rem 0.82rem;
    font-size: 0.82rem;
  }

  .user-name-btn {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
