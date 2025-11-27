// 支持環境變量配置，開發環境使用默認值
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 
  `${window.location.protocol}//${window.location.hostname}:8080/api`

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

class ApiService {
  async request(url, options = {}) {
    const showLoader = options.showLoading !== false // 默認顯示 loading
    
    try {
      if (showLoader && loadingCallbacks.show) {
        loadingCallbacks.show(options.loadingMessage || '載入中...')
      }
      
      const response = await fetch(`${API_BASE_URL}${url}`, {
        credentials: 'include',
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...options.headers
        }
      })
      
      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || `請求失敗: ${response.status}`)
      }
      
      if (response.status === 204) return null
      return await response.json()
    } finally {
      if (showLoader && loadingCallbacks.hide) {
        loadingCallbacks.hide()
      }
    }
  }

  // Auth API
  async login(username, password) {
    return this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
      loadingMessage: '登入中...'
    })
  }

  async logout() {
    return this.request('/auth/logout', { 
      method: 'POST',
      loadingMessage: '登出中...'
    })
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

  // Config API
  async getConfig(configKey) {
    return this.request(`/config/${configKey}`, { showLoading: false })
  }

  async saveConfig(configKey, data) {
    return this.request(`/config/${configKey}`, {
      method: 'POST',
      body: JSON.stringify(data)
    })
  }

  async getInProgressCount() {
    return this.request('/records/stats/in-progress', { showLoading: false })
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

  // Config API
  async getConfig(configKey) {
    return this.request(`/config/${configKey}`)
  }

  async saveConfig(configKey, value, description) {
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
    return this.request('/line/test/push', {
      method: 'POST',
      params: { userId, message }
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

