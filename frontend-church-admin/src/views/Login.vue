<template>
  <div class="login-container">
    <div class="login-card">
      <h1>教會系統登入</h1>
      <p class="login-subtitle">請登入以管理教會系統</p>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="system-status">
          <span class="status-icon">🔍</span>
          <span class="status-label">系統狀態：</span>
          <span :class="['status-text', systemStatus === 'online' ? 'status-online' : systemStatus === 'checking' ? 'status-checking' : 'status-offline']">
            {{ systemStatus === 'online' ? '✓ 運行中' : systemStatus === 'checking' ? '檢查中...' : '✗ 離線' }}
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
          {{ loading ? '登入中...' : '登入' }}
        </button>
      </form>
      <div class="login-footer">
        <p>只有有權限的用戶才能登入管理系統</p>
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
  display:flex;
  align-items:center;
  justify-content:center;
  padding:24px 16px;
  background:
    radial-gradient(1200px 600px at 20% 0%, rgba(37,99,235,.18), rgba(37,99,235,0)),
    radial-gradient(900px 500px at 80% 20%, rgba(16,185,129,.12), rgba(16,185,129,0)),
    var(--bg);
}

.login-card{
  width:min(460px, 100%);
  background:rgba(255,255,255,.82);
  border:1px solid rgba(2,6,23,.10);
  border-radius:22px;
  box-shadow: var(--shadow);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  padding:22px 20px;
}

h1{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
  color:var(--text);
}

.login-subtitle{
  margin-top:6px;
  color:var(--muted);
  font-weight:700;
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
</style>
