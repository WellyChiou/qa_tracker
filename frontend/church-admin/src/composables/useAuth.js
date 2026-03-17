import { useAuthCore } from '@shared/composables/useAuthCore'
import { apiRequest, setTokens, clearTokens } from '@/utils/api'

const authCore = useAuthCore({
  fetchCurrentUser: () => apiRequest('/church/auth/current-user', { method: 'GET' }),
  loginFn: async (username, password) => {
    const result = await apiRequest('/church/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    })

    if (result && result.accessToken) {
      setTokens(result.accessToken, result.refreshToken || null)
    }

    return result
  },
  logoutFn: () => apiRequest('/church/auth/logout', { method: 'POST' }),
  clearTokens,
  cacheDuration: 5000
})

export function useAuth() {
  return authCore
}
