<template>
  <div class="dashboard">
    <TopNavbar />

    <div class="container">
      <section class="page-head card">
        <div>
          <span class="hero-kicker">Control Surface</span>
          <h2>個人控制台</h2>
          <p>直接進入常用模組，並快速確認服務健康狀態。</p>
        </div>

        <div class="status-pills">
          <div class="pill">
            <span>後端</span>
            <strong :class="backendStatus">{{ backendStatusText }}</strong>
          </div>
          <div class="pill">
            <span>認證</span>
            <strong :class="authStatus">{{ authStatusText }}</strong>
          </div>
        </div>
      </section>

      <section class="status-card">
        <div class="section-head">
          <div>
            <span class="section-kicker">Quick Routes</span>
            <h3>快速訪問</h3>
          </div>
          <p>常用功能直接進入，減少切換成本。</p>
        </div>

        <div class="menu-grid">
          <router-link
            v-for="menuItem in dashboardMenus"
            :key="menuItem.url"
            :to="normalizeMenuUrl(menuItem.url)"
            class="menu-card"
          >
            <div class="menu-icon">{{ menuItem.icon || '◦' }}</div>
            <div class="menu-title">{{ menuItem.menuName }}</div>
            <div class="menu-desc">前往 {{ menuItem.menuName }} 模組</div>
          </router-link>
          <div v-if="dashboardMenus.length === 0" class="no-menu">
            暫無可用入口
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopNavbar from '@/components/TopNavbar.vue'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()

const { currentUser } = useAuth()
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

const flattenDashboardMenus = (menuList = []) => {
  const items = []
  if (!Array.isArray(menuList)) {
    return items
  }
  menuList.forEach(menu => {
    if (!menu) return
    const normalizedUrl = normalizeMenuUrl(menu.url)
    if (menu.showInDashboard !== false && normalizedUrl && normalizedUrl !== '#') {
      items.push({ ...menu, url: normalizedUrl })
    }
    if (Array.isArray(menu.children)) {
      menu.children.forEach(child => {
        if (!child) return
        const childUrl = normalizeMenuUrl(child.url)
        if (child.showInDashboard !== false && childUrl && childUrl !== '#') {
          items.push({ ...child, url: childUrl })
        }
      })
    }
  })
  return items
}

const dashboardMenus = computed(() => flattenDashboardMenus(currentUser.value?.menus))

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

onMounted(async () => {
  checkBackendStatus()
  authStatus.value = 'success'
  authStatusText.value = '✓ 已登入'
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 18%, rgba(37, 99, 235, 0.12), transparent 26%),
    radial-gradient(circle at 82% 20%, rgba(34, 197, 94, 0.1), transparent 28%),
    linear-gradient(180deg, #f7faff 0%, #f0f6ff 100%);
  color: var(--text-primary);
  position: relative;
}

.container {
  max-width: 1200px;
  margin: 1.25rem auto 0;
  padding: 0 1rem 2rem;
  position: relative;
  z-index: 1;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.25rem 1.35rem;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.hero-kicker,
.section-kicker {
  display: inline-flex;
  align-items: center;
  padding: 0.36rem 0.64rem;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  border: 1px solid rgba(37, 99, 235, 0.16);
  font-size: 0.7rem;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #1d4ed8;
}

.page-head h2 {
  margin: 0.5rem 0 0.3rem;
  font-size: clamp(1.8rem, 3vw, 2.4rem);
  letter-spacing: -0.03em;
}

.page-head p {
  margin: 0;
  color: rgba(15, 23, 42, 0.62);
  font-weight: 600;
}

.status-pills {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.pill {
  padding: 0.65rem 0.85rem;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 14px;
  min-width: 120px;
}

.pill span {
  display: block;
  font-size: 0.78rem;
  color: rgba(15, 23, 42, 0.58);
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.pill strong {
  display: block;
  margin-top: 0.25rem;
  font-size: 1rem;
  letter-spacing: -0.01em;
}

.pill strong.success {
  color: #059669;
}

.pill strong.error {
  color: #dc2626;
}

.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.15rem;
}

.section-head h3 {
  margin: 0.7rem 0 0;
  font-size: 1.8rem;
  letter-spacing: -0.04em;
}

.section-head p {
  margin: 0;
  color: rgba(15, 23, 42, 0.58);
  font-weight: 600;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.menu-card {
  padding: 1.15rem;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(18px);
  border-radius: 24px;
  text-align: left;
  text-decoration: none;
  color: var(--text-primary);
  transition: var(--transition);
  border: 1px solid rgba(148, 163, 184, 0.16);
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  position: relative;
}

a.menu-card {
  text-decoration: none;
}

.menu-card:hover {
  background: rgba(255, 255, 255, 0.98);
  transform: translateY(-4px);
  box-shadow: 0 22px 36px rgba(15, 23, 42, 0.12);
}

.menu-icon {
  width: 3rem;
  height: 3rem;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(14, 165, 233, 0.12));
  color: #1d4ed8;
  font-size: 1.45rem;
}

.menu-title {
  font-size: 1.08rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.menu-desc {
  color: rgba(15, 23, 42, 0.55);
  font-size: 0.86rem;
  font-weight: 600;
}

.no-menu {
  grid-column: 1/-1;
  text-align: center;
  color: rgba(15, 23, 42, 0.52);
  padding: 2rem;
  font-size: 1rem;
  font-weight: 700;
}

@media (max-width: 960px) {
  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .status-pills {
    width: 100%;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .pill {
    flex: 1 1 140px;
  }
}

@media (max-width: 640px) {
  .container {
    padding: 0 0.85rem 1.2rem;
  }

  .page-head {
    padding: 1rem 1.1rem;
    border-radius: 18px;
  }
}
</style>
