<template>
  <div class="login-container">
    <div class="login-atmosphere" aria-hidden="true">
      <span class="login-glow login-glow--blue"></span>
      <span class="login-glow login-glow--cyan"></span>
    </div>

    <div class="login-grid">
      <section class="login-intro">
        <div class="intro-badge">Personal Workspace</div>
        <h1>把每天要處理的事情，收斂成一個乾淨的工作台。</h1>
        <p>
          這裡整合個人記帳、系統設定、排程與資料維護。登入後可以直接回到你常用的管理流程。
        </p>

        <div class="intro-points">
          <div class="intro-point">
            <strong>集中式導覽</strong>
            <span>常用操作從同一個入口進入，不再跳來跳去。</span>
          </div>
          <div class="intro-point">
            <strong>即時狀態</strong>
            <span>登入前先確認後端是否在線，降低誤判。</span>
          </div>
          <div class="intro-point">
            <strong>穩定工作流</strong>
            <span>保留既有認證與 API，不重寫邏輯只優化體驗。</span>
          </div>
        </div>
      </section>

      <section class="login-card">
        <div class="card-top">
          <span class="card-kicker">Secure Sign-in</span>
          <h2>登入系統</h2>
          <p>輸入帳號與密碼，進入個人控制台。</p>
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
            <input
              id="username"
              v-model="username"
              type="text"
              required
              placeholder="例如：admin"
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
            />
          </div>

          <div v-if="error" class="error-message">{{ error }}</div>

          <button type="submit" :disabled="loading" class="login-button">
            {{ loading ? '登入中...' : '進入控制台' }}
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
import { apiService } from '@/composables/useApi'

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
    
    // 確保認證狀態已更新，等待一小段時間讓 session cookie 設置完成
    await new Promise(resolve => setTimeout(resolve, 100))
    
    // 驗證認證狀態，最多重試 3 次
    let isAuthenticated = false
    for (let i = 0; i < 3; i++) {
      isAuthenticated = await checkAuth()
      if (isAuthenticated) {
        break
      }
      // 如果還沒認證，等待一下再重試
      await new Promise(resolve => setTimeout(resolve, 200))
    }
    
    if (!isAuthenticated) {
      throw new Error('認證狀態驗證失敗，請重新登入')
    }
    
    const redirect = route.query.redirect || '/'
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
  position: relative;
  overflow: hidden;
  padding: 2rem;
  background:
    radial-gradient(circle at 12% 18%, rgba(14, 165, 233, 0.18), transparent 28%),
    radial-gradient(circle at 82% 22%, rgba(37, 99, 235, 0.18), transparent 32%),
    linear-gradient(180deg, #eff6ff 0%, #e0f2fe 48%, #f8fafc 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.login-atmosphere {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.login-glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(12px);
}

.login-glow--blue {
  top: -8rem;
  left: -6rem;
  width: 22rem;
  height: 22rem;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.28) 0%, rgba(59, 130, 246, 0) 70%);
}

.login-glow--cyan {
  right: -8rem;
  bottom: -10rem;
  width: 24rem;
  height: 24rem;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.22) 0%, rgba(6, 182, 212, 0) 72%);
}

.login-grid {
  width: 100%;
  max-width: 1180px;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 440px);
  gap: 2rem;
  position: relative;
  z-index: 1;
}

.login-intro {
  padding: 2.5rem 1rem 2.5rem 0;
}

.intro-badge,
.card-kicker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.45rem 0.75rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(59, 130, 246, 0.14);
  color: #1d4ed8;
  font-size: 0.76rem;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.login-intro h1 {
  margin: 1rem 0 1rem;
  max-width: 12ch;
  font-size: clamp(2.5rem, 4.6vw, 4.6rem);
  line-height: 0.94;
  letter-spacing: -0.05em;
  color: #0f172a;
}

.login-intro p {
  max-width: 42rem;
  margin: 0;
  color: rgba(15, 23, 42, 0.66);
  font-size: 1.05rem;
  line-height: 1.8;
  font-weight: 600;
}

.intro-points {
  margin-top: 2rem;
  display: grid;
  gap: 1rem;
}

.intro-point {
  padding: 1.05rem 1.15rem;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.06);
}

.intro-point strong {
  display: block;
  color: #0f172a;
  font-size: 1rem;
  margin-bottom: 0.35rem;
}

.intro-point span {
  color: rgba(15, 23, 42, 0.6);
  font-weight: 600;
  font-size: 0.94rem;
}

.login-card {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(24px);
  border-radius: 32px;
  padding: 2rem;
  box-shadow: 0 32px 70px rgba(15, 23, 42, 0.14);
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.65);
  position: relative;
  align-self: center;
}

.card-top h2 {
  margin: 0.9rem 0 0.55rem;
  color: var(--text-primary);
  font-size: 2rem;
  letter-spacing: -0.04em;
}

.card-top p {
  margin: 0;
  color: rgba(15, 23, 42, 0.58);
  font-weight: 600;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
  margin-top: 1.5rem;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.85rem 1rem;
  background: rgba(248, 250, 252, 0.9);
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  font-size: 0.9rem;
}

.status-icon {
  font-size: 0.95rem;
}

.status-label {
  color: var(--text-primary);
  font-weight: 700;
}

.status-text {
  font-weight: 600;
  margin-left: auto;
}

.status-online {
  color: #10b981;
}

.status-checking {
  color: #f59e0b;
}

.status-offline {
  color: #ef4444;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.form-group label {
  color: var(--text-primary);
  font-weight: 700;
  font-size: 0.9rem;
}

.form-group input {
  padding: 15px 18px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 18px;
  font-size: 15px;
  transition: var(--transition);
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-primary);
}

.form-group input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.error-message {
  color: var(--error);
  background: var(--error-light);
  padding: 0.9rem 1rem;
  border-radius: 18px;
  text-align: center;
  font-weight: 700;
  border: 1px solid rgba(239, 68, 68, 0.22);
}

.login-button {
  padding: 16px;
  background: linear-gradient(135deg, #0f172a 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 18px;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition: var(--transition);
  box-shadow: 0 22px 32px rgba(37, 99, 235, 0.22);
}

.login-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 26px 38px rgba(37, 99, 235, 0.26);
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

@media (max-width: 960px) {
  .login-grid {
    grid-template-columns: 1fr;
  }

  .login-intro {
    padding: 0;
  }

  .login-intro h1 {
    max-width: none;
  }
}

@media (max-width: 640px) {
  .login-container {
    padding: 1rem;
  }

  .login-card {
    padding: 1.35rem;
    border-radius: 24px;
  }

  .login-intro h1 {
    font-size: 2.2rem;
  }
}
</style>
