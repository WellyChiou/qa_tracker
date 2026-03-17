import { createApiClient } from '@shared/utils/apiClient'

const personalApiClient = createApiClient({
  accessTokenKey: 'personal_access_token',
  credentials: 'include',
  getApiBaseUrl() {
    return import.meta.env.VITE_API_BASE_URL || (
      import.meta.env.DEV
        ? `${window.location.protocol}//${window.location.hostname}:8080/api`
        : `${window.location.protocol}//${window.location.hostname}/api`
    )
  },
  shouldAttachToken(url) {
    const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/register')
    return !isAuthEndpoint
  }
})

export async function apiRequest(url, options = {}) {
  return personalApiClient.request(url, options)
}
