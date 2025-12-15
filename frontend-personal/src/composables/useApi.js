// 支持環境變量配置，開發環境使用默認值
// 生產環境：使用當前域名（Nginx 反向代理）
// 開發環境：使用 8080 端口
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 
  (import.meta.env.DEV 
    ? `${window.location.protocol}//${window.location.hostname}:8080/api`
    : `${window.location.protocol}//${window.location.hostname}/api`)

// 全局 loading 狀態管理
let loadingCount = 0
let loadingMessage = '載入中...'
let loadingCallbacks = {
  show: null,
  hide: null
}

// 設置 loading 回調函數（由 useLoading composable 調用）
export function setLoadingCallbacks(show, hide) {
  loadingCallbacks.show = show
  loadingCallbacks.hide = hide
}

/**
 * 獲取 Access Token（從 localStorage）
 */
function getAccessToken() {
  return localStorage.getItem('personal_access_token')
}

/**
 * 獲取 Refresh Token（從 localStorage）
 */
function getRefreshToken() {
  return localStorage.getItem('personal_refresh_token')
}

/**
 * 設置 Access Token 和 Refresh Token（保存到 localStorage）
 */
export function setTokens(accessToken, refreshToken) {
  if (accessToken) {
    localStorage.setItem('personal_access_token', accessToken)
  } else {
    localStorage.removeItem('personal_access_token')
  }
  if (refreshToken) {
    localStorage.setItem('personal_refresh_token', refreshToken)
  } else {
    localStorage.removeItem('personal_refresh_token')
  }
}

/**
 * 清除所有 Token
 */
function clearTokens() {
  localStorage.removeItem('personal_access_token')
  localStorage.removeItem('personal_refresh_token')
}

/**
 * 刷新 Access Token
 */
async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('沒有 Refresh Token')
  }

  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
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
      localStorage.setItem('personal_access_token', data.accessToken)
      return data.accessToken
    }
    throw new Error('刷新 Token 響應無效')
  } catch (error) {
    clearTokens()
    throw error
  }
}

class ApiService {
  async request(url, options = {}) {
    const showLoader = options.showLoading !== false // 默認顯示 loading
    
    try {
      if (showLoader && loadingCallbacks.show) {
        loadingCallbacks.show(options.loadingMessage || '載入中...')
      }
      
      // 判斷是否需要 JWT Token
      const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/register')
      const needsToken = !isAuthEndpoint
      
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
      
      let response = await fetch(`${API_BASE_URL}${url}`, {
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
            response = await fetch(`${API_BASE_URL}${url}`, {
              ...options,
              headers,
              credentials: 'omit'
            })
          } catch (error) {
            // 刷新失敗，清除所有 Token
            clearTokens()
            // 如果是認證相關的請求，不拋出異常，讓調用者處理
            if (url.includes('/auth/')) {
              // 繼續處理原始響應
            } else {
              // 其他請求，拋出異常
              throw new Error('認證已過期，請重新登入')
            }
          }
        } else {
          // 沒有 Refresh Token，清除 Access Token
          clearTokens()
        }
      }
      
      // 檢查是否是重定向（3xx 狀態碼）
      if (response.redirected) {
        throw new Error('請求被重定向，可能是認證失敗')
      }
      
      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || `請求失敗: ${response.status}`)
      }
      
      // 處理空響應（204 No Content）
      if (response.status === 204) {
        return null
      }
      
      // 嘗試解析 JSON，如果響應體為空則返回 null
      const text = await response.text()
      if (!text || text.trim().length === 0) {
        return null
      }
      
      try {
        return JSON.parse(text)
      } catch (e) {
        // 如果不是有效的 JSON，返回原始文本或 null
        console.warn('響應不是有效的 JSON:', text)
        return null
      }
    } finally {
      if (showLoader && loadingCallbacks.hide) {
        loadingCallbacks.hide()
      }
    }
  }

  // Auth API
  async login(username, password) {
    const result = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
      loadingMessage: '登入中...'
    })
    
    // 保存 Access Token 和 Refresh Token（如果有的話）
    if (result && result.accessToken) {
      setTokens(result.accessToken, result.refreshToken || null)
    }
    
    return result
  }

  async logout() {
    try {
      await this.request('/auth/logout', { 
        method: 'POST',
        loadingMessage: '登出中...'
      })
    } finally {
      // 即使 API 調用失敗，也清除本地 Token
      clearTokens()
    }
  }

  async getCurrentUser() {
    return this.request('/auth/current-user', { showLoading: false })
  }

  async getMenus() {
    return this.request('/menus', { showLoading: false })
  }

  // Records API
  async getRecords(params = {}) {
    const queryParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        queryParams.append(key, value)
      }
    })
    return this.request(`/records?${queryParams}`)
  }

  async getRecordById(id) {
    return this.request(`/records/${id}`)
  }

  async createRecord(record) {
    return this.request('/records', {
      method: 'POST',
      body: JSON.stringify(record)
    })
  }

  async updateRecord(id, record) {
    return this.request(`/records/${id}`, {
      method: 'PUT',
      body: JSON.stringify(record)
    })
  }

  async deleteRecord(id) {
    return this.request(`/records/${id}`, { method: 'DELETE' })
  }

  // Config API（已廢棄，請使用 SystemSettings API）
  // 使用 /personal/admin/system-settings/{key} 替代
  // @deprecated 請使用 SystemSettings API
  async getConfig(configKey) {
    console.warn('getConfig 已廢棄，請使用 SystemSettings API')
    return this.request(`/config/${configKey}`, { showLoading: false })
  }

  // @deprecated 請使用 SystemSettings API
  async saveConfig(configKey, data) {
    console.warn('saveConfig 已廢棄，請使用 SystemSettings API')
    return this.request(`/config/${configKey}`, {
      method: 'POST',
      body: JSON.stringify(data)
    })
  }

  async getInProgressCount() {
    return this.request('/records/stats/in-progress', { showLoading: false })
  }

  async getTotalRecordCount(year = new Date().getFullYear(), showLoading = true) {
    return this.request(`/records/stats/total?year=${year}`, { showLoading })
  }

  async getYearlyStats(year = new Date().getFullYear(), showLoading = true) {
    return this.request(`/records/stats/yearly?year=${year}`, { showLoading })
  }

  // Expenses API
  async getExpenses(params = {}) {
    const queryParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        queryParams.append(key, value)
      }
    })
    return this.request(`/expenses?${queryParams}`)
  }

  async getAllExpenses(params = {}) {
    const queryParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        queryParams.append(key, value)
      }
    })
    return this.request(`/expenses/all?${queryParams}`)
  }

  async getExpenseById(id) {
    return this.request(`/expenses/${id}`)
  }

  async createExpense(expense) {
    return this.request('/expenses', {
      method: 'POST',
      body: JSON.stringify(expense)
    })
  }

  async updateExpense(id, expense) {
    return this.request(`/expenses/${id}`, {
      method: 'PUT',
      body: JSON.stringify(expense)
    })
  }

  async deleteExpense(id) {
    return this.request(`/expenses/${id}`, { method: 'DELETE' })
  }

  // Assets API
  async getAssets() {
    return this.request('/assets')
  }

  async getAssetById(id) {
    return this.request(`/assets/${id}`)
  }

  async createAsset(asset) {
    return this.request('/assets', {
      method: 'POST',
      body: JSON.stringify(asset)
    })
  }

  async updateAsset(id, asset) {
    return this.request(`/assets/${id}`, {
      method: 'PUT',
      body: JSON.stringify(asset)
    })
  }

  async deleteAsset(id) {
    return this.request(`/assets/${id}`, { method: 'DELETE' })
  }

  // Exchange Rates API
  async getExchangeRate(date) {
    try {
      return await this.request(`/exchange-rates/${date}`)
    } catch (error) {
      if (error.message.includes('404')) {
        return null
      }
      throw error
    }
  }

  async getLatestExchangeRate(date) {
    return this.request(`/exchange-rates/latest/${date}`)
  }

  async saveExchangeRate(exchangeRate) {
    return this.request('/exchange-rates', {
      method: 'POST',
      body: JSON.stringify(exchangeRate)
    })
  }

  async autoFillExchangeRates(days = 7) {
    return this.request(`/exchange-rates/auto-fill?days=${days}`, {
      method: 'POST'
    })
  }

  // Config API（已廢棄，請使用 SystemSettings API）
  // 使用 /personal/admin/system-settings/{key} 替代
  // @deprecated 請使用 SystemSettings API
  async getConfig(configKey) {
    console.warn('getConfig 已廢棄，請使用 SystemSettings API')
    return this.request(`/config/${configKey}`)
  }

  // @deprecated 請使用 SystemSettings API
  async saveConfig(configKey, value, description) {
    console.warn('saveConfig 已廢棄，請使用 SystemSettings API')
    return this.request(`/config/${configKey}`, {
      method: 'POST',
      body: JSON.stringify({ value, description })
    })
  }

  // Admin - Users API
  async getUsers() {
    return this.request('/users')
  }

  async getUserByUid(uid) {
    return this.request(`/users/${uid}`)
  }

  async createUser(user) {
    return this.request('/users', {
      method: 'POST',
      body: JSON.stringify(user)
    })
  }

  async updateUser(uid, user) {
    return this.request(`/users/${uid}`, {
      method: 'PUT',
      body: JSON.stringify(user)
    })
  }

  async deleteUser(uid) {
    return this.request(`/users/${uid}`, { method: 'DELETE' })
  }

  async updateUserRoles(uid, roleIds) {
    return this.request(`/users/${uid}/roles`, {
      method: 'PUT',
      body: JSON.stringify({ roleIds })
    })
  }

  async updateUserPermissions(uid, permissionIds) {
    return this.request(`/users/${uid}/permissions`, {
      method: 'PUT',
      body: JSON.stringify({ permissionIds })
    })
  }

  // LINE Bot API
  async getUserLineStatus(uid) {
    return this.request(`/users/${uid}/line-status`)
  }

  async bindUserLineAccount(uid, lineUserId) {
    return this.request(`/users/${uid}/bind-line`, {
      method: 'POST',
      body: JSON.stringify({ lineUserId })
    })
  }

  async unbindUserLineAccount(uid) {
    return this.request(`/users/${uid}/unbind-line`, {
      method: 'POST'
    })
  }

  async sendLineTestMessage(userId, message) {
    // 使用 JSON body 方式發送
    return this.request('/line/test/push', {
      method: 'POST',
      body: JSON.stringify({ userId, message })
    })
  }

  // Admin - Roles API
  async getRoles() {
    return this.request('/roles')
  }

  async getRoleById(id) {
    return this.request(`/roles/${id}`)
  }

  async createRole(role) {
    return this.request('/roles', {
      method: 'POST',
      body: JSON.stringify(role)
    })
  }

  async updateRole(id, role) {
    return this.request(`/roles/${id}`, {
      method: 'PUT',
      body: JSON.stringify(role)
    })
  }

  async deleteRole(id) {
    return this.request(`/roles/${id}`, { method: 'DELETE' })
  }

  async updateRolePermissions(id, permissionIds) {
    return this.request(`/roles/${id}/permissions`, {
      method: 'PUT',
      body: JSON.stringify({ permissionIds })
    })
  }

  // Admin - Permissions API
  async getPermissions() {
    return this.request('/permissions')
  }

  async getPermissionById(id) {
    return this.request(`/permissions/${id}`)
  }

  async getPermissionsByResource(resource) {
    return this.request(`/permissions/resource/${resource}`)
  }

  async createPermission(permission) {
    return this.request('/permissions', {
      method: 'POST',
      body: JSON.stringify(permission)
    })
  }

  async updatePermission(id, permission) {
    return this.request(`/permissions/${id}`, {
      method: 'PUT',
      body: JSON.stringify(permission)
    })
  }

  async deletePermission(id) {
    return this.request(`/permissions/${id}`, { method: 'DELETE' })
  }

  // Admin - Menus API
  async getAllMenuItems() {
    return this.request('/menus/all')
  }

  async getMenuItemById(id) {
    return this.request(`/menus/${id}`)
  }

  async createMenuItem(menuItem) {
    return this.request('/menus', {
      method: 'POST',
      body: JSON.stringify(menuItem)
    })
  }

  async updateMenuItem(id, menuItem) {
    return this.request(`/menus/${id}`, {
      method: 'PUT',
      body: JSON.stringify(menuItem)
    })
  }

  async deleteMenuItem(id) {
    return this.request(`/menus/${id}`, { method: 'DELETE' })
  }

  // Admin - URL Permissions API
  async getUrlPermissions() {
    return this.request('/url-permissions')
  }

  async getUrlPermissionById(id) {
    return this.request(`/url-permissions/${id}`)
  }

  async createUrlPermission(permission) {
    return this.request('/url-permissions', {
      method: 'POST',
      body: JSON.stringify(permission)
    })
  }

  async updateUrlPermission(id, permission) {
    return this.request(`/url-permissions/${id}`, {
      method: 'PUT',
      body: JSON.stringify(permission)
    })
  }

  async deleteUrlPermission(id) {
    return this.request(`/url-permissions/${id}`, { method: 'DELETE' })
  }

  async getActiveUrlPermissions() {
    return this.request('/url-permissions/active')
  }

  // Scheduled Jobs API
  async getScheduledJobs() {
    return this.request('/scheduled-jobs')
  }

  async getScheduledJobById(id) {
    return this.request(`/scheduled-jobs/${id}`)
  }

  async createScheduledJob(job) {
    return this.request('/scheduled-jobs', {
      method: 'POST',
      body: JSON.stringify(job)
    })
  }

  async updateScheduledJob(id, job) {
    return this.request(`/scheduled-jobs/${id}`, {
      method: 'PUT',
      body: JSON.stringify(job)
    })
  }

  async deleteScheduledJob(id) {
    return this.request(`/scheduled-jobs/${id}`, { method: 'DELETE' })
  }

  async executeScheduledJob(id) {
    return this.request(`/scheduled-jobs/${id}/execute`, {
      method: 'POST'
    })
  }

  async toggleScheduledJob(id, enabled) {
    return this.request(`/scheduled-jobs/${id}/toggle?enabled=${enabled}`, {
      method: 'PUT'
    })
  }

  async getJobExecutions(jobId) {
    return this.request(`/scheduled-jobs/${jobId}/executions`)
  }

  async getLatestJobExecution(jobId) {
    return this.request(`/scheduled-jobs/${jobId}/executions/latest`)
  }

  async getJobExecutionById(executionId) {
    return this.request(`/scheduled-jobs/executions/${executionId}`)
  }
}

export const apiService = new ApiService()

