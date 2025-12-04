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
 * @returns {Promise<Response>}
 */
export async function apiRequest(url, options = {}, loadingMessage = '載入中...') {
  const apiUrl = url.startsWith('http') ? url : `${getApiBaseUrl()}${url.startsWith('/') ? url : `/${url}`}`
  
  // 顯示 loading
  if (loadingCallbacks.show) {
    loadingCallbacks.show(loadingMessage)
  }
  
  try {
    const response = await fetch(apiUrl, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      },
      credentials: 'omit' // 不發送 cookies，避免認證相關的重定向
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

