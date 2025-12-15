/**
 * API 請求工具
 * 提供統一的 API 請求方法
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 
  (import.meta.env.DEV 
    ? `${window.location.protocol}//${window.location.hostname}:8080/api`
    : `${window.location.protocol}//${window.location.hostname}/api`)

/**
 * 獲取 Access Token（從 localStorage）
 */
function getAccessToken() {
  return localStorage.getItem('personal_access_token')
}

/**
 * 執行 API 請求
 * @param {string} url - API 端點（相對於 /api）
 * @param {RequestInit} options - Fetch 選項
 * @returns {Promise<Response>}
 */
export async function apiRequest(url, options = {}) {
  const apiUrl = url.startsWith('http') ? url : `${API_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`
  
  // 判斷是否需要 JWT Token
  const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/register')
  const needsToken = !isAuthEndpoint
  
  // 準備 headers
  const isFormData = options.body instanceof FormData
  const headers = {}
  
  // 只有非 FormData 請求才設置 Content-Type
  if (!isFormData) {
    headers['Content-Type'] = 'application/json'
  }
  
  // 合併用戶提供的 headers
  if (options.headers) {
    Object.assign(headers, options.headers)
  }
  
  // 如果需要 Token，添加到 Authorization header
  if (needsToken) {
    const token = getAccessToken()
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
  }
  
  const response = await fetch(apiUrl, {
    ...options,
    headers,
    credentials: options.credentials || 'include'
  })
  
  return response
}

