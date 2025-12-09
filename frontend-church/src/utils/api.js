/**
 * API 配置工具
 * 確保在 HTTPS 環境下使用正確的協議
 */

// Loading 回調函數
let loadingCallbacks = {
  show: null,
  hide: null
}

/**
 * 設置 Loading 回調函數
 * @param {Function} show - 顯示 loading 的函數
 * @param {Function} hide - 隱藏 loading 的函數
 */
export function setLoadingCallbacks(show, hide) {
  loadingCallbacks.show = show
  loadingCallbacks.hide = hide
}

/**
 * 獲取 API 基礎 URL
 * @returns {string} API 基礎 URL
 */
export function getApiBaseUrl() {
  if (import.meta.env.DEV) {
    // 開發環境：使用當前協議和端口 8080
    return `${window.location.protocol}//${window.location.hostname}:8080/api`
  } else {
    // 生產環境：使用相對路徑（推薦）或當前協議
    // 使用相對路徑可以自動匹配當前頁面的協議（HTTP/HTTPS）
    // 這樣可以避免 Mixed Content 錯誤
    return '/api'
    
    // 備選方案：如果必須使用絕對路徑，確保使用正確的協議
    // const protocol = window.location.protocol === 'https:' ? 'https:' : 'https:'
    // return `${protocol}//${window.location.hostname}/api`
  }
}

/**
 * 獲取 Access Token（從 localStorage）
 */
export function getAccessToken() {
  return localStorage.getItem('church_access_token')
}

/**
 * 獲取 Refresh Token（從 localStorage）
 */
export function getRefreshToken() {
  return localStorage.getItem('church_refresh_token')
}

/**
 * 設置 Access Token 和 Refresh Token（保存到 localStorage）
 */
export function setTokens(accessToken, refreshToken) {
  if (accessToken) {
    localStorage.setItem('church_access_token', accessToken)
  } else {
    localStorage.removeItem('church_access_token')
  }
  if (refreshToken) {
    localStorage.setItem('church_refresh_token', refreshToken)
  } else {
    localStorage.removeItem('church_refresh_token')
  }
}

/**
 * 清除所有 Token
 */
export function clearTokens() {
  localStorage.removeItem('church_access_token')
  localStorage.removeItem('church_refresh_token')
}

/**
 * 刷新 Access Token
 */
export async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('沒有 Refresh Token')
  }

  try {
    const response = await fetch(`${getApiBaseUrl()}/church/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ refreshToken }),
      credentials: 'omit'
    })

    if (!response.ok) {
      throw new Error('刷新 Token 失敗')
    }

    const data = await response.json()
    if (data.accessToken) {
      localStorage.setItem('church_access_token', data.accessToken)
      return data.accessToken
    }
    throw new Error('刷新 Token 響應無效')
  } catch (error) {
    clearTokens()
    throw error
  }
}

/**
 * 執行 API 請求，自動處理錯誤和 Loading
 * @param {string} url - API 端點（相對於 /api）
 * @param {RequestInit} options - Fetch 選項
 * @param {string} loadingMessage - Loading 顯示的文字（可選）
 * @param {boolean} needsAuth - 是否需要認證（可選，預設自動判斷）
 * @returns {Promise<Response>}
 */
export async function apiRequest(url, options = {}, loadingMessage = '載入中...', needsAuth = false) {
  const apiUrl = url.startsWith('http') ? url : `${getApiBaseUrl()}${url.startsWith('/') ? url : `/${url}`}`
  
  // 顯示 loading
  if (loadingCallbacks.show) {
    loadingCallbacks.show(loadingMessage)
  }
  
  try {
    // 判斷是否需要 JWT Token
    const isAuthEndpoint = url.includes('/church/auth/login') || url.includes('/church/auth/register')
    const needsToken = !isAuthEndpoint && (needsAuth || url.includes('/church/'))
    
    // 準備 headers
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    }
    
    // 如果需要 Token，添加到 Authorization header
    if (needsToken) {
      const token = getAccessToken()
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
    }
    
    let response = await fetch(apiUrl, {
      ...options,
      headers,
      credentials: 'omit' // 不再使用 session cookies
    })
    
    // 處理 401 未授權錯誤（Token 無效或過期）
    if (response.status === 401 && needsToken) {
      // 嘗試使用 Refresh Token 刷新 Access Token（如果有 Refresh Token）
      const refreshToken = getRefreshToken()
      if (refreshToken) {
        try {
          const newAccessToken = await refreshAccessToken()
          // 使用新的 Access Token 重試請求
          headers['Authorization'] = `Bearer ${newAccessToken}`
          response = await fetch(apiUrl, {
            ...options,
            headers,
            credentials: 'omit'
          })
        } catch (error) {
          // 刷新失敗，清除所有 Token
          clearTokens()
          // 如果是認證相關的請求，不拋出異常，讓調用者處理
          if (url.includes('/church/auth/')) {
            return response
          }
          // 其他請求，可以選擇重定向到登入頁或拋出異常
          throw new Error('認證已過期，請重新登入')
        }
      } else {
        // 沒有 Refresh Token，清除 Access Token
        clearTokens()
      }
    }
    
    // 對於 401/403，不拋出異常，讓調用者處理
    // 對於其他錯誤，拋出異常
    if (!response.ok && response.status !== 401 && response.status !== 403) {
      const errorText = await response.text().catch(() => '無法讀取錯誤信息')
      throw new Error(`API 請求失敗: ${response.status} ${response.statusText} - ${errorText}`)
    }
    
    return response
  } finally {
    // 隱藏 loading
    if (loadingCallbacks.hide) {
      loadingCallbacks.hide()
    }
  }
}

