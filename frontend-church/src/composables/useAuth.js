import { ref } from 'vue'
import { apiRequest, setTokens, clearTokens } from '@/utils/api'

const currentUser = ref(null)
const isAuthenticated = ref(false)

// 緩存機制：避免短時間內重複調用
let checkAuthPromise = null
let lastCheckTime = 0
const CACHE_DURATION = 5000 // 5秒內不重複調用

export function useAuth() {
  const checkAuth = async (force = false) => {
    // 如果已經有用戶信息且不是強制刷新，且距離上次檢查不到 5 秒，直接返回
    if (!force && currentUser.value && isAuthenticated.value) {
      const now = Date.now()
      if (now - lastCheckTime < CACHE_DURATION) {
        return true
      }
    }
    
    // 如果已經有正在進行的請求，等待它完成
    if (checkAuthPromise) {
      return checkAuthPromise
    }
    
    // 創建新的請求
    checkAuthPromise = (async () => {
    try {
      const response = await apiRequest('/church/auth/current-user', {
        method: 'GET'
      })
      
      if (response.ok) {
        const user = await response.json()
        if (user && user.authenticated) {
          currentUser.value = user
          isAuthenticated.value = true
            lastCheckTime = Date.now()
          return true
        }
      }
    } catch (error) {
      console.error('認證檢查失敗:', error)
    }
    currentUser.value = null
    isAuthenticated.value = false
      lastCheckTime = Date.now()
    return false
    })()
    
    try {
      return await checkAuthPromise
    } finally {
      // 清除 promise，允許下次調用
      checkAuthPromise = null
    }
  }

  const login = async (username, password) => {
    try {
      const response = await apiRequest('/church/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
      })
      
      if (!response.ok) {
        const error = await response.json()
        throw new Error(error.message || '登入失敗')
      }
      
      const result = await response.json()
      
      // 保存 Access Token 和 Refresh Token（如果有的話）
      if (result.accessToken) {
        setTokens(result.accessToken, result.refreshToken || null)
      }
      
      await checkAuth(true) // 登入後強制刷新認證狀態
      return result
    } catch (error) {
      throw error
    }
  }

  const logout = async () => {
    try {
      await apiRequest('/church/auth/logout', {
        method: 'POST'
      })
      // 清除 Token
      clearTokens()
      currentUser.value = null
      isAuthenticated.value = false
    } catch (error) {
      console.error('登出失敗:', error)
      // 即使 API 調用失敗，也清除本地 Token
      clearTokens()
      currentUser.value = null
      isAuthenticated.value = false
    }
  }

  const getUser = () => {
    return currentUser.value
  }

  return {
    currentUser,
    isAuthenticated,
    checkAuth,
    login,
    logout,
    getUser
  }
}

