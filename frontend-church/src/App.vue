<template>
  <div id="app">
    <!-- 前台 navbar -->
    <nav class="navbar">
      <div class="nav-container">
        <router-link to="/" class="brand" @click="isNavOpen = false">
          <img src="/images/logo.png" alt="極光教會 Logo" class="brand-logo" />
          <span class="brand-text">極光教會-PLC</span>
        </router-link>

        <button
          class="nav-toggle"
          type="button"
          :aria-expanded="String(isNavOpen)"
          aria-label="切換導覽選單"
          @click="isNavOpen = !isNavOpen"
        >
          <span class="nav-toggle-bar" />
          <span class="nav-toggle-bar" />
          <span class="nav-toggle-bar" />
        </button>

        <ul class="nav-menu nav-menu--desktop">
          <li v-for="menu in displayMenus" :key="menu.id">
            <router-link :to="menu.url || '#'" class="nav-link">{{ menu.menuName }}</router-link>
          </li>
        </ul>
      </div>

      <div class="nav-mobile" v-show="isNavOpen">
        <ul class="nav-menu nav-menu--mobile">
          <li v-for="menu in displayMenus" :key="'m-' + menu.id">
            <router-link :to="menu.url || '#'" class="nav-link" @click="isNavOpen = false">{{ menu.menuName }}</router-link>
          </li>
        </ul>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
    <!-- 前台 footer -->
    <footer class="footer">
      <div class="footer-container">
        <p>&copy; 2026 極光教會網站. 版權所有.</p>
      </div>
    </footer>
    <LoadingSpinner />
  </div>
</template>

<script setup>

import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useLoading } from '@/composables/useLoading'
import { apiRequest } from '@/utils/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const route = useRoute()
const frontendMenus = ref([])
const isNavOpen = ref(false)

// 計算要顯示的菜單（如果有子菜單，只顯示子菜單；否則顯示根菜單）
const displayMenus = computed(() => {
  const menus = []
  for (const menu of frontendMenus.value) {
    // 如果菜單有子菜單，只顯示子菜單（不顯示母菜單）
    if (menu.children && menu.children.length > 0) {
      menus.push(...menu.children)
    } else if (menu.url && menu.url !== '#') {
      // 如果沒有子菜單且 URL 不是 '#'，顯示根菜單本身
      menus.push(menu)
    }
  }
  return menus
})

// 初始化 loading 系統（註冊回調到 API 服務）
useLoading()

// 預設菜單（僅在 API 請求失敗時使用）
const defaultMenus = [
  { id: 1, menuName: '首頁', url: '/' },
  { id: 2, menuName: '關於我們', url: '/about' },
  { id: 3, menuName: '活動', url: '/activities' },
  { id: 4, menuName: '服事表', url: '/service-schedule' },
  { id: 5, menuName: '聯絡我們', url: '/contact' }
]

// 載入前台菜單
const loadFrontendMenus = async () => {
  try {
    const response = await apiRequest('/church/menus/frontend', {
      method: 'GET'
    })
    
    console.log('前台菜單 API 響應狀態:', response.status, response.ok)
    
    if (response.ok) {
      const menus = await response.json()
      console.log('前台菜單數據:', menus)
      
      // 如果後端返回了有效的菜單數據，使用後端的菜單
      if (menus && Array.isArray(menus)) {
        if (menus.length > 0) {
          console.log('使用後端菜單，共', menus.length, '項')
          frontendMenus.value = menus
          return
        } else {
          console.warn('後端返回空數組，但資料庫可能有設定，檢查資料庫配置')
          // 空數組不代表失敗，可能是資料庫中沒有前台菜單，不應該使用預設菜單
          frontendMenus.value = []
          return
        }
      } else {
        console.warn('後端返回的數據格式不正確:', menus)
      }
    } else {
      console.error('前台菜單 API 請求失敗，狀態碼:', response.status)
    }
    
    // 只有在 API 請求失敗（非 200 狀態）時才使用預設菜單
    console.warn('API 請求失敗，使用預設前台菜單')
    frontendMenus.value = defaultMenus
  } catch (error) {
    console.error('載入前台菜單時發生異常:', error)
    // 只有在發生異常時才使用預設菜單
    frontendMenus.value = defaultMenus
  }
}

// 監聽路由變化，載入前台菜單
watch(() => route.path, () => {
    loadFrontendMenus()
}, { immediate: true })

</script>

<style scoped>
.navbar{
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255,255,255,0.78);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}

.nav-container{
  max-width: var(--container);
  margin: 0 auto;
  padding: 0.9rem 1.25rem;
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap: 1rem;
}

.brand{
  display:flex;
  align-items:center;
  gap: 0.75rem;
  text-decoration:none;
  color: var(--text);
  min-width: 0;
}

.brand-logo{
  width: 38px;
  height: 38px;
  object-fit: contain;
  border-radius: 12px;
  background: white;
  border: 1px solid var(--border);
  padding: 6px;
}

.brand-text{
  font-weight: 800;
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.nav-toggle{
  display:none;
  align-items:center;
  justify-content:center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: var(--surface);
  cursor:pointer;
}

.nav-toggle:hover{ box-shadow: var(--shadow-sm); }

.nav-toggle-bar{
  display:block;
  width: 18px;
  height: 2px;
  background: var(--text);
  margin: 2px 0;
  border-radius: 99px;
}

.nav-menu{
  list-style:none;
  display:flex;
  align-items:center;
  gap: 0.35rem;
  margin:0;
  padding:0;
}

.nav-link{
  display:inline-flex;
  align-items:center;
  padding: 0.55rem 0.85rem;
  border-radius: 14px;
  color: var(--text);
  text-decoration:none;
  font-weight: 600;
  opacity: 0.9;
  transition: background 0.15s ease, transform 0.15s ease, opacity 0.15s ease;
}

.nav-link:hover{
  background: rgba(102,126,234,0.10);
  opacity: 1;
}

.nav-link.router-link-active{
  background: rgba(102,126,234,0.14);
  box-shadow: inset 0 0 0 1px rgba(102,126,234,0.18);
}

.nav-mobile{
  display:none;
  border-top: 1px solid var(--border);
  background: rgba(255,255,255,0.9);
}

.nav-menu--mobile{
  flex-direction: column;
  align-items: stretch;
  padding: 0.75rem 1.25rem 1.25rem;
  gap: 0.25rem;
}

.nav-menu--mobile .nav-link{
  width: 100%;
  justify-content: space-between;
  padding: 0.8rem 0.9rem;
}

.main-content{
  width: 100%;
}

.footer{
  margin-top: 3rem;
  padding: 2.5rem 0;
  border-top: 1px solid var(--border);
  background: var(--surface);
}

.footer-container{
  max-width: var(--container);
  margin: 0 auto;
  padding: 0 1.25rem;
  text-align:center;
  color: var(--muted);
  font-size: 0.95rem;
}

@media (max-width: 860px){
  .nav-toggle{ display:flex; }
  .nav-menu--desktop{ display:none; }
  .nav-mobile{ display:block; }
  .brand-text{ font-size: 0.98rem; }
}
</style>
