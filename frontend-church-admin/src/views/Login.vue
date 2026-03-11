<template>
  <div class="login-container">
    <div class="login-atmosphere" aria-hidden="true">
      <span class="login-glow login-glow--blue"></span>
      <span class="login-glow login-glow--emerald"></span>
      <span class="login-glow login-glow--mist"></span>
    </div>

    <div class="login-shell">
      <section class="login-intro">
        <span class="login-kicker">Church Admin</span>
        <h1>把內容維運、同工資料與現場流程集中在同一個後台。</h1>
        <p>
          這個入口是給教會管理者使用，登入後可直接管理公告、活動、服事表、點名與排程作業。
        </p>

        <div class="intro-grid">
          <article class="intro-card">
            <strong>內容維運</strong>
            <span>公告、主日信息、前台選單與教會介紹集中更新。</span>
          </article>
          <article class="intro-card">
            <strong>同工管理</strong>
            <span>人員、角色、權限與服事排班同一套資料來源。</span>
          </article>
          <article class="intro-card">
            <strong>現場作業</strong>
            <span>聚會點名、手動補登與任務排程可從後台直接掌握。</span>
          </article>
        </div>
      </section>

      <div class="login-card">
        <div class="card-top">
          <span class="card-kicker">Secure Access</span>
          <h2>教會系統登入</h2>
          <p>請使用有授權的帳號登入管理系統。</p>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="system-status">
            <span class="status-icon">●</span>
            <span class="status-label">系統狀態</span>
            <span :class="['status-text', systemStatus === 'online' ? 'status-online' : systemStatus === 'checking' ? 'status-checking' : 'status-offline']">
              {{ systemStatus === 'online' ? '運行中' : systemStatus === 'checking' ? '檢查中...' : '離線' }}
            </span>
          </div>

          <div class="form-group">
            <label for="username">用戶名</label>
            <input
              id="username"
              v-model="username"
              type="text"
              required
              placeholder="請輸入用戶名"
              autocomplete="username"
            />
          </div>

          <div class="form-group">
            <label for="password">密碼</label>
            <input
              id="password"
              v-model="password"
              type="password"
              required
              placeholder="請輸入密碼"
              autocomplete="current-password"
            />
          </div>

          <div v-if="error" class="error-message">{{ error }}</div>

          <button type="submit" :disabled="loading" class="login-button">
            {{ loading ? '登入中...' : '登入後台' }}
          </button>
        </form>

        <div class="login-footer">
          <p>只有具備權限的教會管理者可登入此系統。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const route = useRoute()
const { login, checkAuth } = useAuth()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const systemStatus = ref('checking') // 'checking', 'online', 'offline'

const checkSystemStatus = async () => {
  try {
    // 嘗試訪問後端健康檢查 API
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 
      (import.meta.env.DEV 
        ? `${window.location.protocol}//${window.location.hostname}:8080/api`
        : `${window.location.protocol}//${window.location.hostname}/api`)
    const response = await fetch(`${apiBaseUrl}/hello`, {
      method: 'GET',
      credentials: 'include',
      signal: AbortSignal.timeout(3000) // 3秒超時
    })
    if (response.ok) {
      systemStatus.value = 'online'
    } else {
      systemStatus.value = 'offline'
    }
  } catch (err) {
    systemStatus.value = 'offline'
  }
}

let statusInterval = null

onMounted(() => {
  checkSystemStatus()
  // 每30秒檢查一次系統狀態
  statusInterval = setInterval(checkSystemStatus, 30000)
})

onUnmounted(() => {
  if (statusInterval) {
    clearInterval(statusInterval)
  }
})

const handleLogin = async () => {
  error.value = ''
  loading.value = true
  
  try {
    await login(username.value, password.value)
    
    // 確保認證狀態已更新
    await new Promise(resolve => setTimeout(resolve, 100))
    
    // 驗證認證狀態（強制刷新，不使用緩存）
    let isAuthenticated = false
    for (let i = 0; i < 3; i++) {
      isAuthenticated = await checkAuth(true) // 強制刷新
      if (isAuthenticated) {
        break
      }
      await new Promise(resolve => setTimeout(resolve, 200))
    }
    
    if (!isAuthenticated) {
      throw new Error('認證狀態驗證失敗，請重新登入')
    }
    
    // 登入成功後，前往後台首頁
    const redirect = route.query.redirect || '/admin'
    router.push(redirect)
  } catch (err) {
    error.value = err.message || '登入失敗，請檢查用戶名和密碼'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container{
  min-height:100vh;
  position: relative;
  overflow: hidden;
  display:flex;
  align-items:center;
  justify-content:center;
  padding:32px 20px;
  background:
    radial-gradient(1200px 600px at 20% 0%, rgba(37,99,235,.18), rgba(37,99,235,0)),
    radial-gradient(900px 500px at 80% 20%, rgba(16,185,129,.12), rgba(16,185,129,0)),
    var(--bg);
}

.login-atmosphere{
  position:absolute;
  inset:0;
  pointer-events:none;
}

.login-glow{
  position:absolute;
  border-radius:999px;
  filter:blur(18px);
}

.login-glow--blue{
  top:-8rem;
  left:-6rem;
  width:26rem;
  height:26rem;
  background:radial-gradient(circle, rgba(59,130,246,.30) 0%, rgba(59,130,246,0) 70%);
}

.login-glow--emerald{
  right:-10rem;
  bottom:-8rem;
  width:28rem;
  height:28rem;
  background:radial-gradient(circle, rgba(16,185,129,.24) 0%, rgba(16,185,129,0) 72%);
}

.login-glow--mist{
  top:22%;
  right:18%;
  width:18rem;
  height:18rem;
  background:radial-gradient(circle, rgba(255,255,255,.44) 0%, rgba(255,255,255,0) 70%);
}

.login-shell{
  position:relative;
  z-index:1;
  width:min(1160px, 100%);
  display:grid;
  grid-template-columns:minmax(0, 1.15fr) minmax(360px, 430px);
  gap:2rem;
  align-items:center;
}

.login-intro{
  padding:1.5rem 1rem 1.5rem 0;
}

.login-kicker,
.card-kicker{
  display:inline-flex;
  align-items:center;
  justify-content:center;
  padding:0.45rem 0.78rem;
  border-radius:999px;
  background:rgba(255,255,255,.7);
  border:1px solid rgba(37,99,235,.14);
  color:#1d4ed8;
  font-size:.76rem;
  font-weight:900;
  letter-spacing:.14em;
  text-transform:uppercase;
}

.login-intro h1{
  margin:1rem 0 1rem;
  max-width:12ch;
  font-size:clamp(2.6rem, 5vw, 4.8rem);
  line-height:.94;
  letter-spacing:-.05em;
  color:var(--text);
}

.login-intro p{
  margin:0;
  max-width:42rem;
  color:var(--muted);
  font-size:1.02rem;
  line-height:1.8;
  font-weight:600;
}

.intro-grid{
  margin-top:2rem;
  display:grid;
  gap:1rem;
}

.intro-card{
  padding:1.1rem 1.15rem;
  border-radius:22px;
  background:rgba(255,255,255,.72);
  border:1px solid rgba(255,255,255,.54);
  box-shadow:0 16px 34px rgba(15,23,42,.08);
}

.intro-card strong{
  display:block;
  margin-bottom:.35rem;
  color:var(--text);
  font-size:1rem;
}

.intro-card span{
  color:var(--muted);
  font-weight:600;
  line-height:1.6;
}

.login-card{
  width:min(460px, 100%);
  background:rgba(255,255,255,.82);
  border:1px solid rgba(2,6,23,.10);
  border-radius:30px;
  box-shadow: var(--shadow);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  padding:24px 22px;
}

.card-top h2{
  margin:.95rem 0 .55rem;
  font-size:2rem;
  font-weight:900;
  letter-spacing:-0.04em;
  color:var(--text);
}

.card-top p{
  margin:0;
  color:var(--muted);
  font-weight:600;
  font-size:14px;
}

.login-form{ margin-top:16px; display:flex; flex-direction:column; gap:12px; }

.system-status{
  display:flex;
  align-items:center;
  gap:8px;
  padding:10px 12px;
  border-radius:16px;
  border:1px solid rgba(2,6,23,.08);
  background:rgba(255,255,255,.7);
}
.status-icon{font-size:12px}
.status-label{ color:rgba(15,23,42,.60); font-weight:800; font-size:12px; }
.status-text{ font-weight:900; font-size:12px; }
.status-online{ color:var(--success); }
.status-checking{ color:var(--warning); }
.status-offline{ color:#b91c1c; }

.form-group{ display:flex; flex-direction:column; gap:8px; }
.form-label{ font-size:13px; font-weight:900; color:rgba(15,23,42,.72); }
.login-input{
  border-radius:16px;
  padding:12px 12px;
}

.login-button{
  margin-top:4px;
  border:none;
  width:100%;
  padding:12px 14px;
  border-radius:16px;
  background:var(--primary);
  color:white;
  font-weight:900;
  font-size:14px;
  cursor:pointer;
  transition:transform .12s ease, background .12s ease, box-shadow .12s ease;
  box-shadow:0 10px 24px rgba(37,99,235,.22);
}
.login-button:hover{ transform:translateY(-1px); background:var(--primary-600); }
.login-button:active{ transform:translateY(0); box-shadow:none; }

.error-message{
  margin-top:8px;
  padding:10px 12px;
  border-radius:16px;
  border:1px solid rgba(239,68,68,.20);
  background:rgba(239,68,68,.08);
  color:#b91c1c;
  font-weight:800;
  font-size:13px;
}

.login-footer{
  margin-top:16px;
  color:rgba(15,23,42,.56);
  font-size:12px;
  font-weight:700;
}

@media (max-width: 960px){
  .login-shell{
    grid-template-columns:1fr;
  }

  .login-intro{
    padding:0;
  }

  .login-intro h1{
    max-width:none;
  }

  .login-card{
    width:100%;
  }
}

@media (max-width: 640px){
  .login-container{
    padding:20px 14px;
  }

  .login-card{
    border-radius:24px;
    padding:20px 18px;
  }
}
</style>
