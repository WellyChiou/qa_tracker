import { ref } from 'vue'
import { apiService } from './useApi'

const currentUser = ref(null)
const isAuthenticated = ref(false)

export function useAuth() {
  const checkAuth = async () => {
    try {
      const user = await apiService.getCurrentUser()
      if (user && user.authenticated) {
        currentUser.value = user
        isAuthenticated.value = true
        return true
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
      const result = await apiService.login(username, password)
      await checkAuth()
      return result
    } catch (error) {
      throw error
    }
  }

  const logout = async () => {
    try {
      await apiService.logout()
      currentUser.value = null
      isAuthenticated.value = false
    } catch (error) {
      console.error('登出失敗:', error)
    }
  }

  return {
    currentUser,
    isAuthenticated,
    checkAuth,
    login,
    logout
  }
}

