import { useAuthCore } from '@shared/composables/useAuthCore'
import { investApiService, clearTokens } from './useInvestApi'

const authCore = useAuthCore({
  fetchCurrentUser: () => investApiService.getCurrentUser(),
  loginFn: (username, password) => investApiService.login(username, password),
  logoutFn: () => investApiService.logout(),
  clearTokens,
  cacheDuration: 5000
})

export function useAuth() {
  return authCore
}
