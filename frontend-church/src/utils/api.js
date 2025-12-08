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
    // 判斷是否需要發送 credentials（用於設置和接收 session cookies）
    // 登入 API 需要 credentials 來設置 session，但不需要認證
    // 如果明確指定了 needsAuth 參數，使用該參數；否則自動判斷
    let shouldSendCredentials = needsAuth
    if (!shouldSendCredentials) {
      // 登入相關 API 需要 credentials（設置 session cookie）
      if (url.includes('/church/auth')) {
        shouldSendCredentials = true
      }
      // 需要認證的後台 API 也需要 credentials
      else if (url.includes('/church/menus/admin') ||
               (url.includes('/church/menus') && !url.includes('/church/menus/frontend')) || // 菜單管理需要認證（除了前台菜單）
               url.includes('/church/admin') ||
               (url.includes('/church/service-schedules') && (options.method === 'POST' || options.method === 'PUT' || options.method === 'DELETE')) ||
               (url.includes('/church/persons') && (options.method === 'POST' || options.method === 'PUT' || options.method === 'DELETE')) ||
               (url.includes('/church/positions') && (options.method === 'POST' || options.method === 'PUT' || options.method === 'DELETE'))) {
        shouldSendCredentials = true
      }
    }
    
    const response = await fetch(apiUrl, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      },
      credentials: shouldSendCredentials ? 'include' : 'omit' // 需要認證或登入的請求發送 cookies
    })
    
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

