import { createApiClient } from './apiClient'
import { createChurchScheduledJobsApi } from './churchScheduledJobs'

export function createChurchApiModule() {
  const client = createApiClient({
    accessTokenKey: 'church_access_token',
    refreshTokenKey: 'church_refresh_token',
    authExpiredMessage: '認證已過期，請重新登入',
    credentials: 'omit',
    getApiBaseUrl() {
      if (import.meta.env.DEV) {
        return `${window.location.protocol}//${window.location.hostname}:8080/api`
      }
      return '/api'
    },
    isAuthRelatedRequest(url) {
      return url.includes('/church/auth/')
    },
    refreshEndpoint: '/church/auth/refresh',
    shouldAttachToken(url, needsAuth) {
      const isAuthEndpoint = url.includes('/church/auth/login') || url.includes('/church/auth/register')
      return !isAuthEndpoint && (needsAuth || url.includes('/church/'))
    }
  })

  const apiRequest = async (url, options = {}, loadingMessage = '載入中...', needsAuth = false) => {
    return client.request(url, options, {
      loadingMessage,
      needsAuth,
      showLoading: true
    })
  }

  return {
    apiRequest,
    clearTokens: client.clearTokens,
    getAccessToken: client.getAccessToken,
    getApiBaseUrl: client.getApiBaseUrl,
    getRefreshToken: client.getRefreshToken,
    refreshAccessToken: client.refreshAccessToken,
    setLoadingCallbacks: client.setLoadingCallbacks,
    setTokens: client.setTokens,
    ...createChurchScheduledJobsApi(apiRequest)
  }
}
