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
.login-container {
  min-height: calc(100vh - 200px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  background: white;
  border-radius: 12px;
  padding: 2.5rem;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 420px;
}

.login-card h1 {
  text-align: center;
  margin-bottom: 0.5rem;
  color: #333;
  font-size: 2rem;
  font-weight: 700;
}

.login-subtitle {
  text-align: center;
  color: #666;
  margin-bottom: 2rem;
  font-size: 0.95rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 8px;
  border: 1px solid rgba(102, 126, 234, 0.2);
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.status-icon {
  font-size: 1.1rem;
}

.status-label {
  color: #333;
  font-weight: 600;
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
  gap: 0.5rem;
}

.form-group label {
  color: #333;
  font-weight: 600;
  font-size: 0.95rem;
}

.form-group input {
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.error-message {
  color: #ef4444;
  background: #fee2e2;
  padding: 12px;
  border-radius: 8px;
  text-align: center;
  font-weight: 600;
  border: 1px solid #ef4444;
}

.login-button {
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.login-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.login-footer {
  margin-top: 1.5rem;
  text-align: center;
  color: #666;
  font-size: 0.85rem;
}
</style>

