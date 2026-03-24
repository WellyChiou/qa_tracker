<template>
  <nav class="navbar">
    <div class="navbar-shell">
      <router-link to="/" class="navbar-title">
        <span class="brand-chip">Invest</span>
        <div class="brand-copy">
          <h1>Invest Admin</h1>
          <p>持股分析與監控後台</p>
        </div>
      </router-link>

      <div class="navbar-menu" v-if="menus.length > 0">
        <div v-for="(menu, index) in menus" :key="menu.id || menu.menuCode" class="menu-item-wrapper">
          <router-link
            v-if="isDirectMenu(menu)"
            :to="normalizeMenuUrl(menu.url)"
            class="menu-item"
          >
            {{ menu.icon || '' }} {{ menu.menuName }}
          </router-link>
          <div
            v-else-if="hasChildren(menu)"
            class="menu-group-wrapper"
          >
            <button
              type="button"
              class="menu-item has-submenu"
              :class="{ active: activeSubmenuIndex === index }"
              @click.stop="toggleSubmenu(index)"
            >
              {{ menu.icon || '' }} {{ menu.menuName }}
              <span class="arrow">▼</span>
            </button>
            <div v-if="activeSubmenuIndex === index" class="submenu" @click.stop>
              <router-link
                v-for="child in sortedChildren(menu)"
                :key="child.id || child.menuCode"
                :to="normalizeMenuUrl(child.url)"
                class="submenu-item"
                :class="{ active: isSubmenuActive(child.url) }"
                @click.stop="closeSubmenu"
              >
                <span class="submenu-icon">{{ child.icon || '•' }}</span>
                {{ child.menuName }}
              </router-link>
            </div>
          </div>
          <span v-else class="menu-item menu-item--muted">
            {{ menu.icon || '' }} {{ menu.menuName }}
          </span>
        </div>
      </div>

      <div v-if="currentUser" class="user-info">
        <span class="user-name">{{ currentUser.displayName || currentUser.username || currentUser.email || '使用者' }}</span>
        <button class="logout-btn" @click="handleLogout">登出</button>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const route = useRoute()
const { currentUser, logout } = useAuth()
const activeSubmenuIndex = ref(null)

const menus = computed(() => {
  const menus = currentUser.value?.menus
  if (!Array.isArray(menus)) {
    return []
  }

  return menus
    .slice()
    .sort((a, b) => Number(a?.orderIndex || 0) - Number(b?.orderIndex || 0))
    .filter(menu => isDirectMenu(menu) || hasChildren(menu))
})

const normalizeMenuUrl = (url) => {
  if (!url || url === '#') {
    return '/'
  }

  if (url.startsWith('/invest-admin')) {
    const trimmed = url.replace('/invest-admin', '')
    return trimmed || '/'
  }

  if (url.startsWith('/invest/')) {
    return url.replace('/invest', '') || '/'
  }

  return url
}

const isDirectMenu = (menu) => !!(menu?.url && menu.url !== '#')

const sortedChildren = (menu) => {
  const children = Array.isArray(menu?.children) ? menu.children : []
  return children
    .filter(child => child?.url && child.url !== '#')
    .slice()
    .sort((a, b) => Number(a?.orderIndex || 0) - Number(b?.orderIndex || 0))
}

const hasChildren = (menu) => sortedChildren(menu).length > 0

const isSubmenuActive = (url) => {
  const path = normalizeMenuUrl(url)
  if (path === '/') {
    return route.path === '/'
  }
  return route.path === path || route.path.startsWith(`${path}/`)
}

const toggleSubmenu = (index) => {
  activeSubmenuIndex.value = activeSubmenuIndex.value === index ? null : index
}

const closeSubmenu = () => {
  activeSubmenuIndex.value = null
}

const handleClickOutside = (event) => {
  const target = event.target
  if (!(target instanceof Element)) {
    return
  }

  if (!target.closest('.menu-item-wrapper')) {
    closeSubmenu()
  }
}

const handleLogout = async () => {
  closeSubmenu()
  await logout()
  router.push('/login')
}

watch(
  () => route.path,
  () => {
    closeSubmenu()
  }
)

onMounted(() => {
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
  max-width: 1400px;
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
  gap: 0.85rem;
  text-decoration: none;
  color: inherit;
}

.brand-chip {
  background: linear-gradient(135deg, #0f172a, #2563eb);
  color: #fff;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 800;
  padding: 0.35rem 0.65rem;
}

.brand-copy h1 {
  margin: 0;
  font-size: 1rem;
}

.brand-copy p {
  margin: 0;
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.navbar-menu {
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.menu-item-wrapper,
.menu-group-wrapper {
  position: relative;
}

.menu-item {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  text-decoration: none;
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 0.4rem 0.75rem;
  font-size: 0.86rem;
  font-weight: 600;
  background: #fff;
}

.menu-item.has-submenu {
  cursor: pointer;
  font-family: inherit;
}

.menu-item.has-submenu.active {
  background: rgba(37, 99, 235, 0.12);
  border-color: rgba(37, 99, 235, 0.3);
}

.menu-item.router-link-active {
  background: rgba(37, 99, 235, 0.12);
  border-color: rgba(37, 99, 235, 0.3);
}

.menu-item--muted {
  color: var(--text-secondary);
  background: rgba(15, 23, 42, 0.04);
}

.arrow {
  margin-left: 0.2rem;
  font-size: 0.68rem;
}

.submenu {
  position: absolute;
  top: calc(100% + 0.45rem);
  right: 0;
  min-width: 210px;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.45rem;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-md);
  background: #fff;
  z-index: 120;
}

.submenu-item {
  text-decoration: none;
  color: var(--text-primary);
  border-radius: 10px;
  padding: 0.45rem 0.55rem;
  font-size: 0.84rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.submenu-item:hover {
  background: rgba(15, 23, 42, 0.05);
}

.submenu-item.active {
  background: rgba(37, 99, 235, 0.12);
  color: #1e40af;
}

.submenu-icon {
  width: 1rem;
  display: inline-flex;
  justify-content: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.user-name {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.logout-btn {
  border: 1px solid #fecaca;
  color: #b91c1c;
  background: #fff;
  border-radius: 999px;
  padding: 0.35rem 0.7rem;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 960px) {
  .navbar-shell {
    flex-direction: column;
    align-items: stretch;
    gap: 0.65rem;
  }

  .navbar-menu {
    width: 100%;
  }

  .submenu {
    left: 0;
    right: auto;
  }

  .user-info {
    justify-content: flex-end;
  }
}
</style>
