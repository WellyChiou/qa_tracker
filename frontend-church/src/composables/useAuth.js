import { ref } from 'vue'
import { apiRequest } from '@/utils/api'

const currentUser = ref(null)
const isAuthenticated = ref(false)

export function useAuth() {
  const checkAuth = async () => {
    try {
      const response = await apiRequest('/church/auth/current-user', {
        method: 'GET',
        credentials: 'include'
      })
      
      if (response.ok) {
        const user = await response.json()
        if (user && user.authenticated) {
          currentUser.value = user
          isAuthenticated.value = true
          return true
        }
      }
    } catch (error) {
      console.error('認證檢查失敗:', error)
    }
    currentUser.value = null
    isAuthenticated.value = false
    return false
  }

  const login = async (username, password) => {
    try {
      const response = await apiRequest('/church/auth/login', {
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({ username, password })
      })
      
      if (!response.ok) {
        const error = await response.json()
        throw new Error(error.message || '登入失敗')
      }
      
      const result = await response.json()
      await checkAuth()
      return result
    } catch (error) {
      throw error
    }
  }

  const logout = async () => {
    try {
      await apiRequest('/church/auth/logout', {
        method: 'POST',
        credentials: 'include'
      })
      currentUser.value = null
      isAuthenticated.value = false
    } catch (error) {
      console.error('登出失敗:', error)
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

