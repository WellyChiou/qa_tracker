<template>
  <div class="login-container">
    <div class="login-atmosphere" aria-hidden="true">
      <span class="login-glow login-glow--blue"></span>
      <span class="login-glow login-glow--emerald"></span>
      <span class="login-glow login-glow--mist"></span>
    </div>

    <div class="login-shell">
      <section class="login-intro">
        <div class="login-brand">
          <span class="login-brand__dot"></span>
          <span class="login-brand__name">Aurora Church Admin</span>
        </div>
        <span class="login-kicker">Church Admin</span>
        <h1>教會後台管理系統</h1>
        <p>
          給教會管理者與授權同工使用，集中處理內容維運、服事排程、簽到作業與權限設定。
        </p>

        <div class="intro-grid">
          <article class="intro-card">
            <strong>內容管理</strong>
            <span>公告、主日信息與網站內容可在同一處維護。</span>
          </article>
          <article class="intro-card">
            <strong>同工與權限</strong>
            <span>角色、帳號、權限與後台入口保持一致。</span>
          </article>
          <article class="intro-card">
            <strong>現場流程</strong>
            <span>服事表、簽到與排程任務可以直接掌握。</span>
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
          <div class="system-status-card">
            <div class="system-status">
              <span class="status-icon">●</span>
              <span class="status-label">系統狀態</span>
              <span :class="['status-text', systemStatus === 'online' ? 'status-online' : systemStatus === 'checking' ? 'status-checking' : 'status-offline']">
                {{ systemStatus === 'online' ? '運行中' : systemStatus === 'checking' ? '檢查中...' : '離線' }}
              </span>
            </div>
            <p class="system-status-hint">登入前會先確認後端服務可用，避免進入後台後才發現服務異常。</p>
          </div>

          <div class="form-group">
            <label for="username">用戶名</label>
            <input
              id="username"
              v-model="username"
              type="text"
              class="login-input"
              required
              placeholder="例如：church_admin"
              autocomplete="username"
            />
          </div>

          <div class="form-group">
            <label for="password">密碼</label>
            <input
              id="password"
              v-model="password"
              type="password"
              class="login-input"
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
          <p>若登入後無法看到功能選單，通常是角色權限尚未配置完成。</p>
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
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect
      ? route.query.redirect
      : '/'
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
  width:min(1140px, 100%);
  display:grid;
  grid-template-columns:minmax(0, .92fr) minmax(400px, 480px);
  gap:2.2rem;
  align-items:center;
}

.login-intro{
  padding:1rem 1rem 1rem 0;
  max-width:32rem;
}

.login-brand{
  display:flex;
  align-items:center;
  gap:.65rem;
  margin-bottom:1rem;
}

.login-brand__dot{
  width:12px;
  height:12px;
  border-radius:999px;
  background:linear-gradient(135deg, #2563eb, #14b8a6);
  box-shadow:0 0 0 6px rgba(37,99,235,.08);
}

.login-brand__name{
  color:rgba(15,23,42,.68);
  font-size:.9rem;
  font-weight:800;
  letter-spacing:.06em;
  text-transform:uppercase;
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
  margin:1rem 0 .9rem;
  max-width:none;
  font-size:clamp(2.2rem, 4vw, 3.35rem);
  line-height:1.02;
  letter-spacing:-.04em;
  color:var(--text);
}

.login-intro p{
  margin:0;
  max-width:31rem;
  color:var(--muted);
  font-size:1rem;
  line-height:1.82;
  font-weight:600;
}

.intro-grid{
  margin-top:1.75rem;
  display:grid;
  grid-template-columns:1fr;
  gap:.85rem;
}

.intro-card{
  padding:1rem 1.05rem;
  border-radius:18px;
  background:rgba(255,255,255,.7);
  border:1px solid rgba(255,255,255,.56);
  box-shadow:0 12px 28px rgba(15,23,42,.06);
}

.intro-card strong{
  display:block;
  margin-bottom:.22rem;
  color:var(--text);
  font-size:.96rem;
}

.intro-card span{
  color:var(--muted);
  font-weight:600;
  line-height:1.6;
}

.login-card{
  width:min(480px, 100%);
  background:linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.84));
  border:1px solid rgba(255,255,255,.68);
  border-radius:34px;
  box-shadow:0 24px 60px rgba(15,23,42,.14);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  padding:30px 28px 24px;
}

.card-top h2{
  margin:1rem 0 .6rem;
  font-size:2.15rem;
  font-weight:900;
  letter-spacing:-0.04em;
  color:var(--text);
}

.card-top p{
  margin:0;
  color:var(--muted);
  font-weight:600;
  font-size:14px;
  line-height:1.75;
}

.login-form{
  margin-top:20px;
  display:flex;
  flex-direction:column;
  gap:14px;
}

.system-status-card{
  padding:14px;
  border-radius:20px;
  border:1px solid rgba(2,6,23,.08);
  background:linear-gradient(180deg, rgba(248,250,252,.92), rgba(255,255,255,.75));
}

.system-status{
  display:flex;
  align-items:center;
  gap:8px;
}
.status-icon{font-size:12px}
.status-label{ color:rgba(15,23,42,.60); font-weight:800; font-size:12px; }
.status-text{ font-weight:900; font-size:12px; }
.status-online{ color:var(--success); }
.status-checking{ color:var(--warning); }
.status-offline{ color:#b91c1c; }

.system-status-hint{
  margin:.55rem 0 0;
  color:rgba(15,23,42,.58);
  font-size:12px;
  line-height:1.7;
  font-weight:700;
}

.form-group{ display:flex; flex-direction:column; gap:9px; }
.login-input{
  border-radius:18px;
  padding:14px 14px;
  background:rgba(255,255,255,.92);
  border:1px solid rgba(2,6,23,.10);
  box-shadow:inset 0 1px 0 rgba(255,255,255,.4);
}

.login-input::placeholder{
  color:rgba(15,23,42,.34);
}

.login-button{
  margin-top:8px;
  border:none;
  width:100%;
  padding:14px 16px;
  border-radius:18px;
  background:linear-gradient(135deg, var(--primary), #0f5fe0);
  color:white;
  font-weight:900;
  font-size:14px;
  letter-spacing:.02em;
  cursor:pointer;
  transition:transform .12s ease, background .12s ease, box-shadow .12s ease;
  box-shadow:0 14px 30px rgba(37,99,235,.24);
}
.login-button:hover{ transform:translateY(-1px); background:var(--primary-600); }
.login-button:active{ transform:translateY(0); box-shadow:none; }

.error-message{
  margin-top:2px;
  padding:12px 14px;
  border-radius:16px;
  border:1px solid rgba(239,68,68,.20);
  background:rgba(239,68,68,.08);
  color:#b91c1c;
  font-weight:800;
  font-size:13px;
  line-height:1.7;
}

.login-footer{
  margin-top:18px;
  padding-top:16px;
  border-top:1px solid rgba(2,6,23,.08);
  color:rgba(15,23,42,.56);
  font-size:12px;
  font-weight:700;
  display:grid;
  gap:.45rem;
}

@media (max-width: 960px){
  .login-shell{
    grid-template-columns:1fr;
    gap:1.6rem;
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
    padding:18px 14px;
  }

  .login-card{
    border-radius:26px;
    padding:22px 18px 18px;
  }

  .login-intro h1{
    font-size:clamp(2.35rem, 11vw, 3.5rem);
  }

  .login-intro p{
    font-size:.96rem;
  }
}
</style>
