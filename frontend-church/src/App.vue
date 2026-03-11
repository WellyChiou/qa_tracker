<template>
  <div id="app" class="site-shell">
    <div class="site-atmosphere" aria-hidden="true">
      <span class="orb orb--gold"></span>
      <span class="orb orb--sage"></span>
      <span class="orb orb--mist"></span>
    </div>

    <header class="site-chrome">
      <div class="container chrome-frame">
        <router-link to="/" class="brand-mark" @click="closeMenu">
          <span class="brand-mark__logo">
            <img src="/images/logo.png" alt="極光教會 Logo" />
          </span>
          <span class="brand-mark__text">
            <strong>極光教會</strong>
            <small>Power Light Church</small>
          </span>
        </router-link>

        <nav class="nav" :class="{ 'nav--open': isMenuOpen }" ref="navRef">
          <template v-for="menu in displayMenus" :key="menu.id">
            <div
              v-if="menu.children && menu.children.length > 0"
              class="nav-dropdown"
              :class="{ 'nav-dropdown--open': openDropdownId === menu.id }"
            >
              <button
                class="nav-dropdown__trigger"
                :class="{ 'nav-dropdown__trigger--active': openDropdownId === menu.id }"
                @click="toggleDropdown(menu.id)"
              >
                {{ menu.menuName }}
                <span class="nav-dropdown__arrow">▼</span>
              </button>
              <div class="nav-dropdown__menu" @click.stop>
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

            <router-link
              v-else-if="menu.url && menu.url !== '#'"
              :to="menu.url"
              class="nav-link"
              @click="closeMenu"
            >
              {{ menu.menuName }}
            </router-link>
          </template>
        </nav>

        <div class="chrome-actions">
          <div class="status-pill">
            <span class="status-pill__dot"></span>
            本週聚會持續開放
          </div>

          <button class="burger" type="button" @click="toggleMenu" aria-label="Toggle menu">
            <span></span><span></span><span></span>
          </button>
        </div>
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
        <div class="footer__panel">
          <div>
            <div class="footer__title">極光教會 · Power Light Church</div>
            <div class="footer__copy">願我們在基督的愛與真理中，一同成長。</div>
          </div>
          <div class="footer__meta">
            <span>公開網站</span>
            <span>教會生活</span>
            <span>{{ currentYear }}</span>
          </div>
        </div>
      </div>
    </footer>

    <LoadingSpinner />
    <ToastHost />
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useLoading } from '@/composables/useLoading'
import { apiRequest } from '@/utils/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import ToastHost from '@shared/components/ToastHost.vue'

const route = useRoute()
const navRef = ref(null)

// Mobile (<=768px): disable route transitions / reveal
const isMobile = ref(false)
const updateIsMobile = () => { isMobile.value = window.innerWidth <= 768 }
updateIsMobile()
window.addEventListener('resize', updateIsMobile)
const handleClickOutside = (event) => {
  if (!navRef.value) return
  if (!navRef.value.contains(event.target)) {
    openDropdownId.value = null
  }
}

onUnmounted(() => {
  window.removeEventListener('resize', updateIsMobile)
  document.removeEventListener('click', handleClickOutside)
})
const frontendMenus = ref([])
const isMenuOpen = ref(false)
const openDropdownId = ref(null)
const currentYear = new Date().getFullYear()

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
  openDropdownId.value = openDropdownId.value === menuId ? null : menuId
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

const displayMenus = computed(() => frontendMenus.value.length > 0 ? frontendMenus.value : defaultMenus)

const loadFrontendMenus = async () => {
  try {
    const data = await apiRequest('/church/menus/frontend', { method: 'GET' }, '', false)
    frontendMenus.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('載入前台菜單時發生異常:', error)
    frontendMenus.value = []
  }
}

watch(
  () => route.path,
  () => {
    closeMenu()
    openDropdownId.value = null
  }
)

onMounted(() => {
  loadFrontendMenus()
  document.addEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.site-shell {
  position: relative;
  min-height: 100vh;
}

.site-atmosphere {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(8px);
  opacity: 0.9;
}

.orb--gold {
  top: 4rem;
  right: -8rem;
  width: 24rem;
  height: 24rem;
  background: radial-gradient(circle, rgba(244, 180, 0, 0.26) 0%, rgba(244, 180, 0, 0) 72%);
}

.orb--sage {
  top: 10rem;
  left: -10rem;
  width: 28rem;
  height: 28rem;
  background: radial-gradient(circle, rgba(31, 157, 106, 0.18) 0%, rgba(31, 157, 106, 0) 72%);
}

.orb--mist {
  bottom: -10rem;
  left: 30%;
  width: 26rem;
  height: 26rem;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.6) 0%, rgba(255, 255, 255, 0) 70%);
}

.site-chrome,
.main,
.footer {
  position: relative;
  z-index: 1;
}

.site-chrome {
  padding: 0.85rem 0 0;
}

.chrome-frame {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding: 0.78rem 0.95rem;
  border-radius: 14px;
  border: 1px solid rgba(16, 24, 40, 0.08);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
  box-shadow: 0 12px 24px rgba(16, 24, 40, 0.05);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 0.85rem;
  min-width: 0;
}

.brand-mark__logo {
  width: 2.65rem;
  height: 2.65rem;
  display: grid;
  place-items: center;
  border-radius: 0.8rem;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(245, 236, 216, 0.92));
  border: 1px solid rgba(16, 24, 40, 0.08);
  box-shadow: 0 8px 16px rgba(16, 24, 40, 0.06);
}

.brand-mark__logo img {
  width: 1.85rem;
  height: 1.85rem;
  object-fit: contain;
}

.brand-mark__text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-mark__text strong {
  font-size: 0.96rem;
  letter-spacing: -0.02em;
  color: #17212f;
}

.brand-mark__text small {
  font-size: 0.66rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: rgba(23, 33, 47, 0.56);
}

.chrome-actions {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.62rem 0.86rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(16, 24, 40, 0.08);
  color: rgba(23, 33, 47, 0.72);
  font-size: 0.76rem;
  font-weight: 700;
}

.status-pill__dot {
  width: 0.55rem;
  height: 0.55rem;
  border-radius: 999px;
  background: #1f9d6a;
  box-shadow: 0 0 0 0.3rem rgba(31, 157, 106, 0.14);
}

.burger {
  display: none;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 12px;
  border: 1px solid rgba(16, 24, 40, 0.08);
  background: rgba(255, 255, 255, 0.84);
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 0.28rem;
}

.burger span {
  width: 1.2rem;
  height: 2px;
  border-radius: 999px;
  background: #17212f;
}

.nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  flex-wrap: wrap;
  flex: 1;
  padding: 0;
  border: none;
  background: transparent;
  backdrop-filter: none;
  box-shadow: none;
}

.nav-link,
.nav-dropdown__trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  min-height: 2.45rem;
  padding: 0.62rem 0.82rem;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  color: rgba(23, 33, 47, 0.8);
  font-weight: 800;
  font-size: 0.84rem;
  transition: transform 160ms ease, background 160ms ease, border-color 160ms ease, color 160ms ease, box-shadow 160ms ease;
}

.nav-link:hover,
.nav-dropdown__trigger:hover,
.nav-dropdown__trigger--active {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.82);
  border-color: rgba(16, 24, 40, 0.08);
  color: #17212f;
  box-shadow: 0 8px 16px rgba(16, 24, 40, 0.06);
  text-decoration: none;
}

.nav-dropdown {
  position: relative;
}

.nav-dropdown__arrow {
  font-size: 0.65rem;
  opacity: 0.75;
}

.nav-dropdown__menu {
  position: absolute;
  top: calc(100% + 0.6rem);
  left: 50%;
  transform: translateX(-50%);
  min-width: 12rem;
  padding: 0.45rem;
  border-radius: 12px;
  border: 1px solid rgba(16, 24, 40, 0.08);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 28px rgba(16, 24, 40, 0.1);
  opacity: 0;
  visibility: hidden;
  transition: opacity 160ms ease, transform 160ms ease, visibility 160ms ease;
}

.nav-dropdown--open .nav-dropdown__menu {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(0);
}

.nav-dropdown__item {
  display: block;
  padding: 0.7rem 0.8rem;
  border-radius: 10px;
  color: rgba(23, 33, 47, 0.78);
  font-weight: 700;
  font-size: 0.82rem;
}

.nav-dropdown__item:hover {
  background: rgba(31, 157, 106, 0.08);
  color: #166a48;
  text-decoration: none;
}

.main {
  flex: 1;
  padding-top: 1.25rem;
}

.footer {
  padding: 1.3rem 0 1.7rem;
}

.footer__panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.1rem;
  border-radius: 14px;
  border: 1px solid rgba(16, 24, 40, 0.08);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(14px);
}

.footer__title {
  font-weight: 900;
  letter-spacing: -0.02em;
  color: #17212f;
}

.footer__copy {
  margin-top: 0.35rem;
  color: rgba(23, 33, 47, 0.62);
  font-size: 0.82rem;
}

.footer__meta {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.footer__meta span {
  padding: 0.36rem 0.58rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(16, 24, 40, 0.08);
  color: rgba(23, 33, 47, 0.62);
  font-size: 0.68rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

@media (max-width: 900px) {
  .status-pill {
    display: none;
  }

  .burger {
    display: inline-flex;
  }

  .nav {
    display: none;
    flex-direction: column;
    align-items: stretch;
    justify-content: flex-start;
    position: absolute;
    top: calc(100% + 0.5rem);
    left: 1rem;
    right: 1rem;
    padding: 0.85rem;
    border-radius: 14px;
    border: 1px solid rgba(16, 24, 40, 0.08);
    background: rgba(255, 255, 255, 0.96);
    box-shadow: 0 16px 28px rgba(16, 24, 40, 0.1);
  }

  .nav--open {
    display: flex;
  }

  .nav-link,
  .nav-dropdown__trigger {
    width: 100%;
    justify-content: space-between;
  }

  .nav-dropdown__menu {
    position: static;
    transform: none;
    min-width: 0;
    margin-top: 0.55rem;
    opacity: 1;
    visibility: visible;
    display: none;
  }

  .nav-dropdown--open .nav-dropdown__menu {
    display: block;
    transform: none;
  }

  .footer__panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .footer__meta {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .chrome-frame {
    padding: 0.85rem;
    border-radius: 20px;
  }

  .brand-mark__text small {
    display: none;
  }

  .brand-mark__logo {
    width: 2.75rem;
    height: 2.75rem;
  }

  .chrome-frame {
    position: relative;
    align-items: center;
  }
}
</style>
