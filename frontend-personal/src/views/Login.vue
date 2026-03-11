<template>
  <div class="login-container">
    <div class="login-atmosphere" aria-hidden="true">
      <span class="login-glow login-glow--blue"></span>
      <span class="login-glow login-glow--cyan"></span>
    </div>

    <div class="login-grid">
      <section class="login-intro">
        <div class="intro-brand">
          <span class="intro-dot"></span>
          <span class="intro-name">Personal Console</span>
        </div>
        <h1>登入個人工作台</h1>
        <p>專注帳務、排程與設定的個人入口，維持輕量、快速、隱私可靠的日常作業。</p>

        <div class="intro-points">
          <div class="intro-point">
            <span class="point-badge">01</span>
            <div>
              <strong>快速進入</strong>
              <span>常用操作集中，少一步導覽。</span>
            </div>
          </div>
          <div class="intro-point">
            <span class="point-badge">02</span>
            <div>
              <strong>穩定連線</strong>
              <span>登入前先檢查後端狀態，避免誤判。</span>
            </div>
          </div>
        </div>
      </section>

      <section class="login-card">
        <div class="card-top">
          <span class="card-kicker">Secure Access</span>
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
            <div class="password-field">
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                required
                placeholder="請輸入密碼"
                @keydown="handleCaps"
                @keyup="handleCaps"
                @focus="handleCaps"
              />
              <button
                type="button"
                class="toggle-visibility"
                @click="showPassword = !showPassword"
                :aria-label="showPassword ? '隱藏密碼' : '顯示密碼'"
                :title="showPassword ? '隱藏密碼' : '顯示密碼'"
              >
                <span v-if="showPassword">🙈</span>
                <span v-else>👁</span>
              </button>
            </div>
            <p v-if="capsLockOn" class="caps-hint">Caps Lock 已開啟，請注意大小寫。</p>
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
const showPassword = ref(false)
const capsLockOn = ref(false)

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

const handleCaps = (event) => {
  capsLockOn.value = event.getModifierState && event.getModifierState('CapsLock')
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 1.8rem;
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
  max-width: 1040px;
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(380px, 430px);
  gap: 1.6rem;
  position: relative;
  z-index: 1;
}

.login-intro {
  padding: 1.25rem 1rem 1.25rem 0;
}

.intro-brand{
  display:flex;
  align-items:center;
  gap:.7rem;
  margin-bottom:.9rem;
}

.intro-dot{
  width:11px;
  height:11px;
  border-radius:999px;
  background:linear-gradient(135deg, #0f172a, #2563eb);
  box-shadow:0 0 0 6px rgba(15,23,42,.06);
}

.intro-name{
  color:rgba(15,23,42,.64);
  font-size:.9rem;
  font-weight:800;
  letter-spacing:.08em;
  text-transform:uppercase;
}

.login-intro h1 {
  margin: .4rem 0 .9rem;
  max-width: 16ch;
  font-size: clamp(2rem, 3.8vw, 3rem);
  line-height: 1.05;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.login-intro p {
  max-width: 32rem;
  margin: 0;
  color: rgba(15, 23, 42, 0.64);
  font-size: 0.98rem;
  line-height: 1.8;
  font-weight: 600;
}

.intro-points {
  margin-top: 1.25rem;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.75rem;
}

.intro-point {
  padding: 0.9rem 1rem;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 0.65rem;
}

.point-badge {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #0f172a 0%, #2563eb 100%);
  color: #fff;
  font-weight: 800;
  letter-spacing: 0.02em;
  font-size: 0.9rem;
}

.intro-point strong {
  display: block;
  color: #0f172a;
  font-size: 0.95rem;
  margin-bottom: 0.2rem;
}

.intro-point span {
  color: rgba(15, 23, 42, 0.6);
  font-weight: 600;
  font-size: 0.9rem;
}

.login-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(18px);
  border-radius: 28px;
  padding: 2.1rem;
  box-shadow: 0 28px 56px rgba(15, 23, 42, 0.12);
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.12);
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
  color: rgba(15, 23, 42, 0.6);
  font-weight: 600;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1.2rem;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.8rem 1rem;
  background: rgba(248, 250, 252, 0.92);
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.14);
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

.password-field {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.form-group label {
  color: var(--text-primary);
  font-weight: 700;
  font-size: 0.9rem;
}

.form-group input {
  padding: 15px 18px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 16px;
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

.toggle-visibility {
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-primary);
  padding: 8px 10px;
  min-width: 40px;
  height: 42px;
  border-radius: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: var(--transition);
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.toggle-visibility:hover {
  background: rgba(2, 6, 23, 0.04);
  border-color: rgba(2, 6, 23, 0.16);
}

.caps-hint {
  margin: 0.3rem 0 0;
  color: #b45309;
  font-size: 12px;
  font-weight: 800;
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
  padding: 15px;
  background: linear-gradient(135deg, #0f172a 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 16px;
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
