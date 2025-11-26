/**
 * API 服務
 * 連接 Java 後端 API
 */

const API_BASE_URL = window.location.protocol + '//' + window.location.hostname + ':8080/api';

// 認證相關的 API 方法
async function login(username, password) {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify({ username, password })
  });
  if (!response.ok) throw new Error('登入失敗');
  return await response.json();
}

async function logout() {
  const response = await fetch(`${API_BASE_URL}/auth/logout`, {
    method: 'POST',
    credentials: 'include'
  });
  if (!response.ok) throw new Error('登出失敗');
  return await response.json();
}

async function getCurrentUser() {
  const response = await fetch(`${API_BASE_URL}/auth/current-user`, {
    credentials: 'include'
  });
  if (!response.ok) throw new Error('獲取用戶資訊失敗');
  return await response.json();
}

async function getMenus() {
  const response = await fetch(`${API_BASE_URL}/menus`, {
    credentials: 'include'
  });
  if (!response.ok) throw new Error('獲取菜單失敗');
  return await response.json();
}

class ApiService {
  // Records API
  async getRecords(params = {}) {
    const queryParams = new URLSearchParams();
    if (params.status !== undefined) queryParams.append('status', params.status);
    if (params.category !== undefined) queryParams.append('category', params.category);
    if (params.testPlan !== undefined) queryParams.append('testPlan', params.testPlan);
    if (params.bugFound !== undefined) queryParams.append('bugFound', params.bugFound);
    if (params.issueNumber) queryParams.append('issueNumber', params.issueNumber);
    if (params.keyword) queryParams.append('keyword', params.keyword);
    if (params.page !== undefined) queryParams.append('page', params.page);
    if (params.size !== undefined) queryParams.append('size', params.size);
    
    const response = await fetch(`${API_BASE_URL}/records?${queryParams}`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error('取得記錄失敗');
    return await response.json();
  }
  
  async getRecordById(id) {
    const response = await fetch(`${API_BASE_URL}/records/${id}`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error('取得記錄失敗');
    return await response.json();
  }
  
  async createRecord(record) {
    const response = await fetch(`${API_BASE_URL}/records`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(record)
    });
    if (!response.ok) throw new Error('建立記錄失敗');
    return await response.json();
  }
  
  async updateRecord(id, record) {
    const response = await fetch(`${API_BASE_URL}/records/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(record)
    });
    if (!response.ok) throw new Error('更新記錄失敗');
    return await response.json();
  }
  
  async deleteRecord(id) {
    const response = await fetch(`${API_BASE_URL}/records/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    });
    if (!response.ok) throw new Error('刪除記錄失敗');
    return await response.json();
  }
  
  async getRecordsForExport(params = {}) {
    const queryParams = new URLSearchParams();
    if (params.status !== undefined) queryParams.append('status', params.status);
    if (params.category !== undefined) queryParams.append('category', params.category);
    if (params.testPlan !== undefined) queryParams.append('testPlan', params.testPlan);
    if (params.bugFound !== undefined) queryParams.append('bugFound', params.bugFound);
    if (params.issueNumber) queryParams.append('issueNumber', params.issueNumber);
    if (params.keyword) queryParams.append('keyword', params.keyword);
    
    const response = await fetch(`${API_BASE_URL}/records/export?${queryParams}`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error('匯出記錄失敗');
    return await response.json();
  }
  
  async getInProgressCount() {
    const response = await fetch(`${API_BASE_URL}/records/stats/in-progress`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error('取得統計失敗');
    return await response.json();
  }
  
  // Expenses API
  async getExpenses(params = {}) {
    const queryParams = new URLSearchParams();
    if (params.year !== undefined) queryParams.append('year', params.year);
    if (params.month !== undefined) queryParams.append('month', params.month);
    if (params.member) queryParams.append('member', params.member);
    if (params.type) queryParams.append('type', params.type);
    if (params.mainCategory) queryParams.append('mainCategory', params.mainCategory);
    if (params.page !== undefined) queryParams.append('page', params.page);
    if (params.size !== undefined) queryParams.append('size', params.size);
    
    const response = await fetch(`${API_BASE_URL}/expenses?${queryParams}`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error('取得記帳記錄失敗');
    return await response.json();
  }
  
  async getAllExpenses(params = {}) {
    const queryParams = new URLSearchParams();
    if (params.year !== undefined) queryParams.append('year', params.year);
    if (params.month !== undefined) queryParams.append('month', params.month);
    if (params.member) queryParams.append('member', params.member);
    if (params.type) queryParams.append('type', params.type);
    if (params.mainCategory) queryParams.append('mainCategory', params.mainCategory);
    
    const response = await fetch(`${API_BASE_URL}/expenses/all?${queryParams}`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error('取得所有記帳記錄失敗');
    return await response.json();
  }
  
  async getExpenseById(id) {
    const response = await fetch(`${API_BASE_URL}/expenses/${id}`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error('取得記帳記錄失敗');
    return await response.json();
  }
  
  async createExpense(expense) {
    const response = await fetch(`${API_BASE_URL}/expenses`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(expense)
    });
    if (!response.ok) throw new Error('建立記帳記錄失敗');
    return await response.json();
  }
  
  async updateExpense(id, expense) {
    const response = await fetch(`${API_BASE_URL}/expenses/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(expense)
    });
    if (!response.ok) throw new Error('更新記帳記錄失敗');
    return await response.json();
  }
  
  async deleteExpense(id) {
    const response = await fetch(`${API_BASE_URL}/expenses/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    });
    if (!response.ok) throw new Error('刪除記帳記錄失敗');
  }
  
  // Assets API
  async getAssets() {
    const response = await fetch(`${API_BASE_URL}/assets`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error('取得資產失敗');
    return await response.json();
  }
  
  async getAssetById(id) {
    const response = await fetch(`${API_BASE_URL}/assets/${id}`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error('取得資產失敗');
    return await response.json();
  }
  
  async createAsset(asset) {
    const response = await fetch(`${API_BASE_URL}/assets`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(asset)
    });
    if (!response.ok) throw new Error('建立資產失敗');
    return await response.json();
  }
  
  async updateAsset(id, asset) {
    const response = await fetch(`${API_BASE_URL}/assets/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(asset)
    });
    if (!response.ok) throw new Error('更新資產失敗');
    return await response.json();
  }
  
  async deleteAsset(id) {
    const response = await fetch(`${API_BASE_URL}/assets/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    });
    if (!response.ok) throw new Error('刪除資產失敗');
  }
  
  // Exchange Rates API
  async getExchangeRate(date) {
    const response = await fetch(`${API_BASE_URL}/exchange-rates/${date}`, {
      credentials: 'include'
    });
    if (!response.ok) {
      if (response.status === 404) {
        return null; // 沒有找到匯率是正常情況
      }
      const errorText = await response.text();
      throw new Error(`取得匯率失敗: ${errorText}`);
    }
    return await response.json();
  }
  
  async getLatestExchangeRate(date) {
    const response = await fetch(`${API_BASE_URL}/exchange-rates/latest/${date}`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error('取得最新匯率失敗');
    return await response.json();
  }
  
  async saveExchangeRate(exchangeRate) {
    const response = await fetch(`${API_BASE_URL}/exchange-rates`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(exchangeRate)
    });
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`儲存匯率失敗: ${errorText}`);
    }
    return await response.json();
  }
  
  // Config API
  async getConfig(configKey) {
    const response = await fetch(`${API_BASE_URL}/config/${configKey}`, {
      credentials: 'include'
    });
    if (!response.ok) throw new Error(`取得設定失敗: ${configKey}`);
    return await response.json();
  }

  async saveConfig(configKey, value, description) {
    const response = await fetch(`${API_BASE_URL}/config/${configKey}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ value, description })
    });
    if (!response.ok) throw new Error(`儲存設定失敗: ${configKey}`);
    return await response.json();
  }
}

// 匯出單例
try {
    window.apiService = new ApiService();
    console.log('✅ ApiService 實例已創建');
    console.log('ApiService 實例:', window.apiService);
    console.log('ApiService 方法:', Object.getOwnPropertyNames(Object.getPrototypeOf(window.apiService)));
} catch (error) {
    console.error('❌ ApiService 實例創建失敗:', error);
    window.apiService = null;
}

