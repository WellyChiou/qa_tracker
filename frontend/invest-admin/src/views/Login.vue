<template>
  <div class="login-container">
    <div class="login-grid">
      <section class="login-intro">
        <div class="intro-brand">
          <span class="intro-dot"></span>
          <span class="intro-name">Invest Admin</span>
        </div>
        <h1>登入 Invest 後台</h1>
        <p>僅提供授權人員使用，系統依據 invest ACL 載入可用菜單與 API 權限。</p>
      </section>

      <section class="login-card">
        <div class="card-top">
          <span class="card-kicker">Secure Access</span>
          <h2>登入系統</h2>
          <p>請輸入 invest 帳號與密碼。</p>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="system-status">
            <span class="status-icon">●</span>
            <span class="status-label">系統狀態</span>
            <span :class="['status-text', systemStatus === 'online' ? 'status-online' : systemStatus === 'checking' ? 'status-checking' : 'status-offline']">
              {{ systemStatus === 'online' ? '在線' : systemStatus === 'checking' ? '檢查中' : '離線' }}
            </span>
          </div>

          <div class="form-group">
            <label for="username">用戶名</label>
            <input id="username" v-model="username" type="text" required placeholder="例如：admin" />
          </div>

          <div class="form-group">
            <label for="password">密碼</label>
            <input id="password" v-model="password" type="password" required placeholder="請輸入密碼" />
          </div>

          <div v-if="error" class="error-message">{{ error }}</div>

          <button type="submit" :disabled="loading" class="login-button">
            {{ loading ? '登入中...' : '進入後台' }}
          </button>
        </form>
      </section>
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
const systemStatus = ref('checking')

const checkSystemStatus = async () => {
  try {
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || (
      import.meta.env.DEV
        ? `${window.location.protocol}//${window.location.hostname}:8080/api/invest`
        : `${window.location.protocol}//${window.location.hostname}/api/invest`
    )

    const response = await fetch(`${apiBaseUrl}/hello`, {
      method: 'GET',
      credentials: 'omit',
      signal: AbortSignal.timeout(3000)
    })

    systemStatus.value = response.ok ? 'online' : 'offline'
  } catch (err) {
    systemStatus.value = 'offline'
  }
}

let statusInterval = null

onMounted(() => {
  checkSystemStatus()
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
    await new Promise(resolve => setTimeout(resolve, 100))

    let isAuthenticated = false
    for (let i = 0; i < 3; i += 1) {
      isAuthenticated = await checkAuth(true)
      if (isAuthenticated) {
        break
      }
      await new Promise(resolve => setTimeout(resolve, 200))
    }

    if (!isAuthenticated) {
      throw new Error('認證狀態驗證失敗，請重新登入')
    }

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
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  background:
    radial-gradient(circle at 15% 20%, rgba(37, 99, 235, 0.18), transparent 28%),
    radial-gradient(circle at 80% 25%, rgba(6, 182, 212, 0.16), transparent 30%),
    linear-gradient(180deg, #eff6ff 0%, #e0f2fe 48%, #f8fafc 100%);
}

.login-grid {
  width: 100%;
  max-width: 960px;
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(340px, 420px);
  gap: 1.5rem;
}

.login-intro {
  padding: 1rem 0.5rem;
}

.intro-brand {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  margin-bottom: 0.9rem;
}

.intro-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #1d4ed8, #06b6d4);
}

.intro-name {
  font-size: 0.85rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #334155;
  font-weight: 700;
}

.login-intro h1 {
  margin: 0;
  font-size: clamp(1.8rem, 2.8vw, 2.35rem);
  color: #0f172a;
}

.login-intro p {
  margin: 0.9rem 0 0;
  color: #475569;
  line-height: 1.6;
}

.login-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 20px 38px rgba(15, 23, 42, 0.14);
  padding: 1.4rem;
}

.card-kicker {
  font-size: 0.74rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #1d4ed8;
  font-weight: 700;
}

.card-top h2 {
  margin: 0.4rem 0 0;
  font-size: 1.35rem;
}

.card-top p {
  margin: 0.4rem 0 0;
  color: #64748b;
  font-size: 0.92rem;
}

.login-form {
  margin-top: 1.15rem;
  display: grid;
  gap: 0.9rem;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #334155;
}

.status-icon {
  color: #1d4ed8;
}

.status-online {
  color: #15803d;
  font-weight: 700;
}

.status-checking {
  color: #ca8a04;
  font-weight: 700;
}

.status-offline {
  color: #b91c1c;
  font-weight: 700;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-group label {
  font-size: 0.82rem;
  font-weight: 700;
  color: #334155;
}

.form-group input {
  height: 42px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 0 0.8rem;
  font-size: 0.95rem;
}

.form-group input:focus {
  outline: none;
  border-color: #1d4ed8;
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.16);
}

.error-message {
  border: 1px solid #fecaca;
  color: #b91c1c;
  background: #fee2e2;
  border-radius: 10px;
  padding: 0.6rem 0.75rem;
  font-size: 0.88rem;
}

.login-button {
  margin-top: 0.25rem;
  height: 44px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #1d4ed8, #06b6d4);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.login-button:disabled {
  opacity: 0.72;
  cursor: not-allowed;
}

@media (max-width: 920px) {
  .login-grid {
    grid-template-columns: 1fr;
  }

  .login-intro {
    padding: 0;
  }
}
</style>
