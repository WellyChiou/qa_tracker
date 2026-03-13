import { useAuthCore } from '@shared/composables/useAuthCore'
import { apiService, clearTokens } from './useApi'

const authCore = useAuthCore({
  fetchCurrentUser: () => apiService.getCurrentUser(),
  loginFn: (username, password) => apiService.login(username, password),
  logoutFn: () => apiService.logout(),
  clearTokens,
  cacheDuration: 5000
})

export function useAuth() {
  return authCore
}
