import { createApiClient } from '@shared/utils/apiClient'

const personalApiClient = createApiClient({
  accessTokenKey: 'personal_access_token',
  refreshTokenKey: 'personal_refresh_token',
  authExpiredMessage: '認證已過期，請重新登入',
  credentials: 'omit',
  getApiBaseUrl() {
    return import.meta.env.VITE_API_BASE_URL || (
      import.meta.env.DEV
        ? `${window.location.protocol}//${window.location.hostname}:8080/api/personal`
        : `${window.location.protocol}//${window.location.hostname}/api/personal`
    )
  },
  isAuthRelatedRequest(url) {
    return url.includes('/auth/')
  },
  refreshEndpoint: '/auth/refresh',
  rejectRedirected: true,
  shouldAttachToken(url) {
    const isAuthEndpoint = url.includes('/auth/login')
      || url.includes('/auth/register')
      || url.includes('/auth/refresh')
    return !isAuthEndpoint
  }
})

export const setLoadingCallbacks = personalApiClient.setLoadingCallbacks
export const setTokens = personalApiClient.setTokens
export const clearTokens = personalApiClient.clearTokens

class ApiService {
  async request(url, options = {}) {
    return personalApiClient.request(url, options, {
      loadingMessage: options.loadingMessage || '載入中...',
      showLoading: options.showLoading !== false
    })
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

  // System Settings API (Replaces Config API)
  async getConfig(configKey) {
    // 兼容舊方法名，但在底層改用新的 system-settings API
    return this.request(`/system-settings/${configKey}/value`, { showLoading: false })
  }

  async saveConfig(configKey, data) {
    // 兼容舊方法，轉發給 updateSystemSetting
    const value = typeof data === 'object' && data.value ? data.value : data
    return this.updateSystemSetting(configKey, value)
  }

  // System Settings API (Admin Maintenance)
  async getSystemSettings() {
    return this.request('/system-settings')
  }
  
  async getSettingsByCategory(category) {
    return this.request(`/system-settings/category/${category}`)
  }

  async updateSystemSetting(key, value) {
    return this.request(`/system-settings/${key}`, {
      method: 'PUT',
      body: JSON.stringify({ settingValue: value })
    })
  }
  
  async createSystemSetting(setting) {
      return this.request('/system-settings', {
          method: 'POST',
          body: JSON.stringify(setting)
      })
  }
  
  async deleteSystemSetting(key) {
      return this.request(`/system-settings/${key}`, {
          method: 'DELETE'
      })
  }

  async refreshSystemSettings() {
    return this.request('/system-settings/refresh', {
      method: 'POST'
    })
  }

  // Backup API (Admin Maintenance)
  async getBackups() {
    return this.request('/backups')
  }

  async createBackup() {
    return this.request('/backups/create', {
      method: 'POST'
    })
  }

  async deleteBackup(relativePath) {
    return this.request(`/backups/delete?path=${encodeURIComponent(relativePath)}`, {
      method: 'DELETE'
    })
  }

  // Note: Download backup typically requires direct link or blob handling, 
  // currently handled in component but could be moved here if returning blob.

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

  // Admin - Users API
  async getUsers() {
    return this.request('/users')
  }

  async getUsersPaged(params = {}) {
    const queryParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        queryParams.append(key, value)
      }
    })
    const query = queryParams.toString()
    return this.request(query ? `/users/paged?${query}` : '/users/paged')
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

  // Admin - Line Groups API
  async getLineGroups() {
    return this.request('/line-groups')
  }

  async getLineGroupById(groupId) {
    return this.request(`/line-groups/${groupId}`)
  }

  async createLineGroup(group) {
    return this.request('/line-groups', {
      method: 'POST',
      body: JSON.stringify(group)
    })
  }

  async updateLineGroup(groupId, group) {
    return this.request(`/line-groups/${groupId}`, {
      method: 'PUT',
      body: JSON.stringify(group)
    })
  }

  async deleteLineGroup(groupId) {
    return this.request(`/line-groups/${groupId}`, { method: 'DELETE' })
  }

  // Admin - Line Group Members API
  async getLineGroupMembers(groupId) {
    return this.request(`/line-groups/${groupId}/members`)
  }

  async addLineGroupMember(groupId, member) {
    return this.request(`/line-groups/${groupId}/members`, {
      method: 'POST',
      body: JSON.stringify(member)
    })
  }

  async updateLineGroupMember(groupId, memberId, member) {
    return this.request(`/line-groups/${groupId}/members/${memberId}`, {
      method: 'PUT',
      body: JSON.stringify(member)
    })
  }

  async deleteLineGroupMember(groupId, memberId) {
    return this.request(`/line-groups/${groupId}/members/${memberId}`, { method: 'DELETE' })
  }

  // Admin - Roles API
  async getRoles() {
    return this.request('/roles')
  }

  async getRolesPaged(params = {}) {
    const queryParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        queryParams.append(key, value)
      }
    })
    const query = queryParams.toString()
    return this.request(query ? `/roles/paged?${query}` : '/roles/paged')
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

  async getPermissionsPaged(params = {}) {
    const queryParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        queryParams.append(key, value)
      }
    })
    const query = queryParams.toString()
    return this.request(query ? `/permissions/paged?${query}` : '/permissions/paged')
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

  async getMenuItemsPaged(params = {}) {
    const queryParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        queryParams.append(key, value)
      }
    })
    const query = queryParams.toString()
    return this.request(query ? `/menus/paged?${query}` : '/menus/paged')
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

  async getUrlPermissionsPaged(params = {}) {
    const queryParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        queryParams.append(key, value)
      }
    })
    const query = queryParams.toString()
    return this.request(query ? `/url-permissions/paged?${query}` : '/url-permissions/paged')
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
