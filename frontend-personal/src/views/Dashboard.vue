<template>
  <div class="dashboard">
    <TopNavbar />

    <div class="container">
      <section class="hero-panel">
        <div class="hero-copy">
          <span class="hero-kicker">Control Surface</span>
          <h2>把個人系統的關鍵操作，壓縮成一個易讀且即時的工作台。</h2>
          <p>
            你可以先確認服務健康狀態，再快速切進常用功能，不需要先翻找選單。
          </p>
        </div>

        <div class="hero-stats">
          <article class="status-item status-item--spotlight">
            <strong>前端</strong>
            <div class="status-value success">在線</div>
            <small>Vue / Nginx</small>
          </article>
          <article class="status-item">
            <strong>後端</strong>
            <div class="status-value" :class="backendStatus">
              {{ backendStatusText }}
            </div>
            <small>Spring Boot API</small>
          </article>
          <article class="status-item">
            <strong>資料庫</strong>
            <div class="status-value success">正常</div>
            <small>MySQL</small>
          </article>
          <article class="status-item">
            <strong>認證</strong>
            <div class="status-value" :class="authStatus">
              {{ authStatusText }}
            </div>
            <small>目前工作區狀態</small>
          </article>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopNavbar from '@/components/TopNavbar.vue'
import { apiService } from '@/composables/useApi'
import { toast } from '@shared/composables/useToast'

const router = useRouter()

const menus = ref([])
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

const dashboardMenus = computed(() => {
  const items = []
  menus.value.forEach(menu => {
    if (menu.showInDashboard !== false && menu.url && menu.url !== '#') {
      items.push({ ...menu, url: normalizeMenuUrl(menu.url) })
    }
    if (menu.children) {
      menu.children.forEach(child => {
        if (child.showInDashboard !== false && child.url && child.url !== '#') {
          items.push({ ...child, url: normalizeMenuUrl(child.url) })
        }
      })
    }
  })
  return items
})

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

const loadMenus = async () => {
  try {
    menus.value = await apiService.getMenus()
    // Dashboard 的菜單載入不需要提示（避免打擾）
  } catch (error) {
    console.error('載入菜單失敗:', error)
    toast.error('載入菜單失敗')
  }
}

onMounted(async () => {
  checkBackendStatus()
  authStatus.value = 'success'
  authStatusText.value = '✓ 已登入'
  await loadMenus()
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 18%, rgba(14, 165, 233, 0.16), transparent 28%),
    radial-gradient(circle at 82% 20%, rgba(37, 99, 235, 0.16), transparent 30%),
    linear-gradient(180deg, #eff6ff 0%, #f8fafc 100%);
  color: var(--text-primary);
  position: relative;
}

.dashboard::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0));
  pointer-events: none;
  z-index: 0;
}

.container {
  max-width: 1280px;
  margin: 1.35rem auto 0;
  padding: 0 1rem 2rem;
  position: relative;
  z-index: 1;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 1.2rem;
  margin-bottom: 1.2rem;
}

.hero-copy,
.status-card {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(24px);
  border-radius: 30px;
  padding: 1.5rem;
  box-shadow: 0 28px 54px rgba(15, 23, 42, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.7);
  transition: var(--transition);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 320px;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.95), rgba(37, 99, 235, 0.92)),
    rgba(15, 23, 42, 0.9);
  color: white;
}

.hero-kicker,
.section-kicker {
  display: inline-flex;
  align-items: center;
  padding: 0.42rem 0.7rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 0.74rem;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero-copy h2 {
  margin: 1rem 0 0.8rem;
  max-width: 14ch;
  font-size: clamp(2.2rem, 4vw, 3.7rem);
  line-height: 0.95;
  letter-spacing: -0.05em;
}

.hero-copy p {
  margin: 0;
  max-width: 44rem;
  color: rgba(255, 255, 255, 0.78);
  font-size: 1rem;
  line-height: 1.8;
  font-weight: 600;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.status-item {
  padding: 1.15rem;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(14px);
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  transition: var(--transition);
  position: relative;
  overflow: hidden;
  min-height: 148px;
}

.status-item--spotlight {
  background: linear-gradient(145deg, rgba(14, 165, 233, 0.12), rgba(255, 255, 255, 0.94));
}

.status-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 22px 36px rgba(15, 23, 42, 0.12);
}

.status-value {
  margin: 0.8rem 0 0.45rem;
  font-size: 1.7rem;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.status-value.success {
  color: #059669;
}

.status-value.error {
  color: #dc2626;
}

.status-item strong,
.status-item small {
  display: block;
}

.status-item strong {
  color: rgba(15, 23, 42, 0.62);
  font-size: 0.8rem;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.status-item small {
  color: rgba(15, 23, 42, 0.48);
  font-size: 0.82rem;
  font-weight: 700;
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
  .hero-panel {
    grid-template-columns: 1fr;
  }

  .hero-copy h2 {
    max-width: none;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .container {
    padding: 0 0.85rem 1.2rem;
  }

  .hero-copy,
  .status-card {
    padding: 1.15rem;
    border-radius: 24px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }
}
</style>
