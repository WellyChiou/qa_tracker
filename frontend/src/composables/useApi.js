// 支持環境變量配置，開發環境使用默認值
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 
  `${window.location.protocol}//${window.location.hostname}:8080/api`

class ApiService {
  async request(url, options = {}) {
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
  }

  // Auth API
  async login(username, password) {
    return this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    })
  }

  async logout() {
    return this.request('/auth/logout', { method: 'POST' })
  }

  async getCurrentUser() {
    return this.request('/auth/current-user')
  }

  async getMenus() {
    return this.request('/menus')
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
}

export const apiService = new ApiService()

