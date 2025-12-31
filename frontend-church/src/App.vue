<template>
  <div id="app">
    <div class="page-bg" aria-hidden="true"></div>
    <header class="topbar">
      <div class="container topbar__inner">
        <router-link to="/" class="brand" @click="closeMenu">
          <span class="brand__logo">
            <img src="/images/logo.png" alt="極光教會 Logo" />
          </span>
          <span class="brand__name">極光教會</span>
        </router-link>

        <button class="burger" type="button" @click="toggleMenu" aria-label="Toggle menu">
          <span></span><span></span><span></span>
        </button>

        <nav class="nav" :class="{ 'nav--open': isMenuOpen }">
          <template v-for="menu in frontendMenus" :key="menu.id">
            <!-- 有子菜單的父菜單：顯示下拉選單 -->
            <div 
              v-if="menu.children && menu.children.length > 0" 
              class="nav-dropdown"
              :class="{ 'nav-dropdown--open': openDropdownId === menu.id }"
              @mouseenter="!isMobile && (openDropdownId = menu.id)"
              @mouseleave="!isMobile && (openDropdownId = null)"
            >
              <button 
                class="nav-dropdown__trigger"
                :class="{ 'nav-dropdown__trigger--active': openDropdownId === menu.id }"
                @click="toggleDropdown(menu.id)"
                @mouseenter="!isMobile && (openDropdownId = menu.id)"
              >
                {{ menu.menuName }}
                <span class="nav-dropdown__arrow">▼</span>
              </button>
              <div class="nav-dropdown__menu">
                <router-link
                  v-for="child in menu.children"
                  :key="child.id"
                  :to="child.url || '#'"
                  @click="closeMenuAndDropdown"
                  class="nav-dropdown__item"
                >
                  {{ child.menuName }}
                </router-link>
              </div>
            </div>
            <!-- 沒有子菜單的普通菜單 -->
            <router-link
              v-else-if="menu.url && menu.url !== '#'"
              :to="menu.url"
              @click="closeMenu"
            >
              {{ menu.menuName }}
            </router-link>
          </template>
        </nav>
      </div>
    </header>

    <main class="main">
      <router-view v-slot="{ Component, route }">
        <transition :name="isMobile ? '' : 'page'" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </transition>
      </router-view>
    </main>

    <footer class="footer">
      <div class="container footer__inner">
        <div>
          <div class="text-sm"><strong>極光教會</strong> · Power Light Church</div>
          <div class="text-xs" style="opacity:.85">願我們在基督的愛與真理中，一同成長。</div>
        </div>
        <div class="text-xs" style="opacity:.75">© 2026 極光教會網站. 版權所有.</div>
      </div>
    </footer>

    <LoadingSpinner />
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useLoading } from '@/composables/useLoading'
import { apiRequest } from '@/utils/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const route = useRoute()

// Mobile (<=768px): disable route transitions / reveal
const isMobile = ref(false)
const updateIsMobile = () => { isMobile.value = window.innerWidth <= 768 }
updateIsMobile()
window.addEventListener('resize', updateIsMobile)
onUnmounted(() => window.removeEventListener('resize', updateIsMobile))
const frontendMenus = ref([])
const isMenuOpen = ref(false)
const openDropdownId = ref(null)

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
  if (!isMenuOpen.value) {
    openDropdownId.value = null
  }
}
const closeMenu = () => {
  isMenuOpen.value = false
  openDropdownId.value = null
}

const toggleDropdown = (menuId) => {
  if (isMobile.value) {
    // 手機版：切換下拉選單
    openDropdownId.value = openDropdownId.value === menuId ? null : menuId
  }
  // 桌面版：由 hover 控制，不需要在這裡處理
}

const closeMenuAndDropdown = () => {
  closeMenu()
  openDropdownId.value = null
}

// 初始化 loading 系統（註冊回調到 API 服務）
useLoading()

const defaultMenus = [
  { id: 1, menuName: '首頁', url: '/' },
  { id: 2, menuName: '關於我們', url: '/about' },
  { id: 3, menuName: '活動', url: '/activities' },
  { id: 4, menuName: '服事表', url: '/service-schedule' },
  { id: 5, menuName: '聯絡我們', url: '/contact' }
]

const loadFrontendMenus = async () => {
  try {
    const response = await apiRequest('/church/menus/frontend', { method: 'GET' })

    if (response.ok) {
      const menus = await response.json()
      if (menus && Array.isArray(menus)) {
        if (menus.length > 0) {
          frontendMenus.value = menus
          return
        }
        frontendMenus.value = []
        return
      }
    }

    frontendMenus.value = defaultMenus
  } catch (error) {
    console.error('載入前台菜單時發生異常:', error)
    frontendMenus.value = defaultMenus
  }
}

watch(
  () => route.path,
  () => {
    closeMenu()
    loadFrontendMenus()
  },
  { immediate: true }
)
</script>
