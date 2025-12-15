<template>
  <div class="admin-page">
    <TopNavbar />
    <header class="header">
      <div class="header-top">
        <h1>🔧 系統維護</h1>
      </div>
    </header>

    <main class="main-content">
      <div class="admin-maintenance">

      <!-- 標籤頁 -->
      <div class="tabs">
        <button 
          @click="activeTab = 'settings'" 
          :class="['tab-button', { active: activeTab === 'settings' }]"
        >
          系統參數
        </button>
        <button 
          @click="activeTab = 'backups'" 
          :class="['tab-button', { active: activeTab === 'backups' }]"
        >
          備份管理
        </button>
      </div>

      <!-- 系統參數標籤頁 -->
      <div v-if="activeTab === 'settings'" class="tab-content">
        <div class="settings-section">
          <div class="settings-actions">
            <button @click="showCreateModal = true" class="btn btn-primary">+ 新增參數</button>
            <button @click="refreshConfig" :disabled="refreshingConfig" class="btn btn-secondary">
              {{ refreshingConfig ? '刷新中...' : '刷新配置' }}
            </button>
            <button @click="loadSettings" class="btn btn-secondary">重新載入</button>
          </div>
          <div v-for="category in categories" :key="category" class="category-group">
            <h3 class="category-title">{{ getCategoryName(category) }}</h3>
            <div class="settings-grid">
              <div 
                v-for="setting in getSettingsByCategory(category)" 
                :key="setting.id"
                class="setting-item"
              >
                <div class="setting-header">
                  <label class="setting-label">
                    {{ setting.description || setting.settingKey }}
                    <span v-if="!setting.isEditable" class="readonly-badge">唯讀</span>
                  </label>
                </div>
                <div class="setting-input-group">
                  <input
                    v-if="setting.settingType === 'string' || setting.settingType === 'number'"
                    :type="setting.settingType === 'number' ? 'number' : 'text'"
                    v-model="setting.settingValue"
                    :disabled="!setting.isEditable || savingSettings.has(setting.settingKey)"
                    class="form-input"
                    @blur="saveSetting(setting)"
                  />
                  <select
                    v-else-if="setting.settingType === 'boolean'"
                    v-model="setting.settingValue"
                    :disabled="!setting.isEditable || savingSettings.has(setting.settingKey)"
                    class="form-input"
                    @change="saveSetting(setting)"
                  >
                    <option value="true">是</option>
                    <option value="false">否</option>
                  </select>
                  <textarea
                    v-else-if="setting.settingType === 'json'"
                    v-model="setting.settingValue"
                    :disabled="!setting.isEditable || savingSettings.has(setting.settingKey)"
                    class="form-input"
                    rows="3"
                    @blur="saveSetting(setting)"
                  ></textarea>
                  <span v-if="savingSettings.has(setting.settingKey)" class="saving-indicator">儲存中...</span>
                  <span v-if="savedSettings.has(setting.settingKey)" class="saved-indicator">✓ 已儲存</span>
                </div>
                <div v-if="setting.settingKey" class="setting-key">
                  <code>{{ setting.settingKey }}</code>
                  <button 
                    v-if="setting.isEditable" 
                    @click="deleteSetting(setting.settingKey)" 
                    class="btn-delete-setting"
                    title="刪除此參數"
                  >
                    🗑️
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 新增參數對話框 -->
      <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
        <div class="modal-content">
          <h3>新增系統參數</h3>
          <form @submit.prevent="createSetting">
            <div class="form-group">
              <label>參數鍵 (setting_key) *</label>
              <input v-model="newSetting.settingKey" type="text" required class="form-input" placeholder="例如: system.new-feature" />
            </div>
            <div class="form-group">
              <label>參數值 (setting_value)</label>
              <input v-model="newSetting.settingValue" type="text" class="form-input" placeholder="參數值" />
            </div>
            <div class="form-group">
              <label>參數類型 (setting_type) *</label>
              <select v-model="newSetting.settingType" required class="form-input">
                <option value="string">string</option>
                <option value="number">number</option>
                <option value="boolean">boolean</option>
                <option value="json">json</option>
              </select>
            </div>
            <div class="form-group">
              <label>分類 (category) *</label>
              <select v-model="newSetting.category" required class="form-input">
                <option value="system">system</option>
                <option value="linebot">linebot</option>
                <option value="jwt">jwt</option>
                <option value="backup">backup</option>
              </select>
            </div>
            <div class="form-group">
              <label>說明 (description)</label>
              <input v-model="newSetting.description" type="text" class="form-input" placeholder="參數說明" />
            </div>
            <div class="form-group">
              <label>
                <input v-model="newSetting.isEditable" type="checkbox" />
                可編輯
              </label>
            </div>
            <div class="modal-actions">
              <button type="submit" :disabled="creatingSetting" class="btn btn-primary">
                {{ creatingSetting ? '創建中...' : '創建' }}
              </button>
              <button type="button" @click="showCreateModal = false" class="btn btn-secondary">取消</button>
            </div>
          </form>
        </div>
      </div>

      <!-- 備份管理標籤頁 -->
      <div v-if="activeTab === 'backups'" class="tab-content">
        <div class="backups-section">
          <div class="backup-actions">
            <button @click="createBackup" :disabled="creatingBackup" class="btn btn-primary">
              {{ creatingBackup ? '備份中...' : '+ 手動備份' }}
            </button>
            <button @click="loadBackups" class="btn btn-secondary">重新整理</button>
          </div>

          <div v-if="backups.length === 0" class="empty-state">
            <p>尚無備份檔案</p>
          </div>
          <div v-else class="backups-table">
            <table>
              <thead>
                <tr>
                  <th>檔案名稱</th>
                  <th>資料庫</th>
                  <th>大小</th>
                  <th>備份時間</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="backup in backups" :key="backup.filename">
                  <td>{{ backup.filename }}</td>
                  <td>
                    <span class="database-badge" :class="backup.database">
                      {{ backup.database }}
                    </span>
                  </td>
                  <td>{{ backup.sizeFormatted }}</td>
                  <td>{{ formatDate(backup.modified) }}</td>
                  <td>
                    <button @click="downloadBackup(backup.relativePath || backup.filename)" class="btn btn-download">下載</button>
                    <button @click="deleteBackup(backup.relativePath || backup.filename)" class="btn btn-delete">刪除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      </div>
    </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import TopNavbar from '@/components/TopNavbar.vue'
import { apiService } from '@/composables/useApi'

const route = useRoute()
const activeTab = ref(route.query.tab || 'settings')
const settings = ref([])
const backups = ref([])
const creatingBackup = ref(false)
const refreshingConfig = ref(false)
const savingSettings = ref(new Set())
const savedSettings = ref(new Set())
const showCreateModal = ref(false)
const creatingSetting = ref(false)
const newSetting = ref({
  settingKey: '',
  settingValue: '',
  settingType: 'string',
  category: 'system',
  description: '',
  isEditable: true
})

// 顯示通知的輔助函數（使用簡單的 alert）
const showNotification = (message, type = 'info') => {
  if (type === 'error') {
    alert('錯誤: ' + message)
  } else if (type === 'success') {
    alert('成功: ' + message)
  } else {
    alert(message)
  }
}

const categories = computed(() => {
  const cats = new Set(settings.value.map(s => s.category))
  return Array.from(cats).sort()
})

const getCategoryName = (category) => {
  const names = {
    backup: '備份設定',
    system: '系統設定',
    linebot: 'LINE Bot 設定',
    jwt: 'JWT Token 設定'
  }
  return names[category] || category
}

const getSettingsByCategory = (category) => {
  return settings.value.filter(s => s.category === category)
}

const loadSettings = async () => {
  try {
    const data = await apiService.request('/personal/admin/system-settings', {
      method: 'GET'
    })
    settings.value = data.settings || []
  } catch (err) {
    showNotification('載入系統參數失敗: ' + err.message, 'error')
  }
}

const saveSetting = async (setting) => {
  if (!setting.isEditable) return
  
  savingSettings.value.add(setting.settingKey)
  savedSettings.value.delete(setting.settingKey)
  
  try {
    await apiService.request(`/personal/admin/system-settings/${setting.settingKey}`, {
      method: 'PUT',
      body: JSON.stringify({
        settingValue: setting.settingValue
      })
    })
    
    savedSettings.value.add(setting.settingKey)
    setTimeout(() => {
      savedSettings.value.delete(setting.settingKey)
    }, 2000)
    showNotification('設定已儲存', 'success')
  } catch (err) {
    showNotification('儲存失敗: ' + err.message, 'error')
  } finally {
    savingSettings.value.delete(setting.settingKey)
  }
}

const refreshConfig = async () => {
  refreshingConfig.value = true
  
  try {
    const data = await apiService.request('/personal/admin/system-settings/refresh', {
      method: 'POST'
    })
    
    if (data.success) {
      showNotification('配置刷新成功，新的配置已生效', 'success')
      loadSettings()
    } else {
      showNotification(data.message || '配置刷新失敗', 'error')
    }
  } catch (err) {
    showNotification('配置刷新失敗: ' + err.message, 'error')
  } finally {
    refreshingConfig.value = false
  }
}

const loadBackups = async () => {
  try {
    const data = await apiService.request('/personal/admin/backups', {
      method: 'GET'
    })
    backups.value = data.backups || []
  } catch (err) {
    showNotification('載入備份列表失敗: ' + err.message, 'error')
  }
}

const createBackup = async () => {
  if (!confirm('確定要立即執行備份嗎？這可能需要一些時間。')) {
    return
  }
  
  creatingBackup.value = true
  
  try {
    const data = await apiService.request('/personal/admin/backups/create', {
      method: 'POST'
    })
    
    if (data.success) {
      showNotification('備份創建成功', 'success')
      loadBackups()
    } else {
      showNotification(data.message || '備份創建失敗', 'error')
    }
  } catch (err) {
    showNotification('備份創建失敗: ' + err.message, 'error')
  } finally {
    creatingBackup.value = false
  }
}

const downloadBackup = async (relativePath) => {
  try {
    // 從相對路徑中提取檔案名稱
    const filename = relativePath.split('/').pop() || relativePath
    // 下載需要使用 fetch 直接處理 blob
    const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 
      (import.meta.env.DEV 
        ? `${window.location.protocol}//${window.location.hostname}:8080/api`
        : `${window.location.protocol}//${window.location.hostname}/api`)
    const token = localStorage.getItem('personal_access_token')
    const response = await fetch(`${API_BASE_URL}/personal/admin/backups/download?path=${encodeURIComponent(relativePath)}`, {
      method: 'GET',
      headers: token ? { 'Authorization': `Bearer ${token}` } : {},
      credentials: 'include'
    })
    
    if (response.ok) {
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      showNotification('備份檔案下載開始', 'success')
    }
  } catch (err) {
    showNotification('下載失敗: ' + err.message, 'error')
  }
}

const deleteBackup = async (relativePath) => {
  // 從相對路徑中提取檔案名稱用於確認訊息
  const filename = relativePath.split('/').pop() || relativePath
  if (!confirm(`確定要刪除備份檔案 "${filename}" 嗎？`)) {
    return
  }
  
  try {
    const data = await apiService.request(`/personal/admin/backups/delete?path=${encodeURIComponent(relativePath)}`, {
      method: 'DELETE'
    })
    
    if (data.success) {
      showNotification('備份檔案刪除成功', 'success')
      loadBackups()
    } else {
      showNotification(data.message || '刪除失敗', 'error')
    }
  } catch (err) {
    showNotification('刪除失敗: ' + err.message, 'error')
  }
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const createSetting = async () => {
  creatingSetting.value = true
  
  try {
    const data = await apiService.request('/personal/admin/system-settings', {
      method: 'POST',
      body: JSON.stringify(newSetting.value)
    })
    
    if (data.success) {
      showNotification('參數創建成功', 'success')
      showCreateModal.value = false
      // 重置表單
      newSetting.value = {
        settingKey: '',
        settingValue: '',
        settingType: 'string',
        category: 'system',
        description: '',
        isEditable: true
      }
      loadSettings()
    } else {
      showNotification(data.message || '創建失敗', 'error')
    }
  } catch (err) {
    showNotification('創建失敗: ' + err.message, 'error')
  } finally {
    creatingSetting.value = false
  }
}

const deleteSetting = async (settingKey) => {
  if (!confirm(`確定要刪除參數 "${settingKey}" 嗎？此操作無法復原。`)) {
    return
  }
  
  try {
    const data = await apiService.request(`/personal/admin/system-settings/${settingKey}`, {
      method: 'DELETE'
    })
    
    if (data.success) {
      showNotification('參數刪除成功', 'success')
      loadSettings()
    } else {
      showNotification(data.message || '刪除失敗', 'error')
    }
  } catch (err) {
    showNotification('刪除失敗: ' + err.message, 'error')
  }
}

// 監聽標籤切換
watch(activeTab, (newTab) => {
  if (newTab === 'backups') {
    loadBackups()
  }
})

onMounted(() => {
  loadSettings()
  if (activeTab.value === 'backups') {
    loadBackups()
  }
})
</script>

<style scoped>
.admin-maintenance {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h2 {
  margin: 0;
  font-size: 1.8rem;
  color: #333;
}

.tabs {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  border-bottom: 2px solid #e0e0e0;
}

.tab-button {
  padding: 0.75rem 1.5rem;
  border: none;
  background: none;
  font-size: 1rem;
  font-weight: 600;
  color: #666;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.2s;
}

.tab-button:hover {
  color: #667eea;
}

.tab-button.active {
  color: #667eea;
  border-bottom-color: #667eea;
}

.tab-content {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 系統參數樣式 */
.settings-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e0e0e0;
}

.category-group {
  margin-bottom: 2rem;
}

.category-title {
  font-size: 1.3rem;
  color: #333;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #e0e0e0;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 1.5rem;
}

.setting-item {
  padding: 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #f9fafb;
}

.setting-header {
  margin-bottom: 0.5rem;
}

.setting-label {
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.readonly-badge {
  background: #f0f0f0;
  color: #666;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
}

.setting-input-group {
  position: relative;
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.form-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.setting-key {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: #666;
}

.setting-key code {
  background: #e5e7eb;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
}

.saving-indicator,
.saved-indicator {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.85rem;
}

.saving-indicator {
  color: #667eea;
}

.saved-indicator {
  color: #10b981;
}

/* 備份管理樣式 */
.backup-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.backups-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f5f5f5;
}

th, td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  font-weight: 600;
  color: #333;
}

.database-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 600;
}

.database-badge.qa_tracker {
  background: #667eea;
  color: white;
}

.database-badge.church {
  background: #10b981;
  color: white;
}

.btn-download {
  background: #667eea;
  color: white;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
  font-size: 0.9rem;
}

.btn-download:hover {
  background: #5568d3;
}

.btn-delete {
  background: #ef4444;
  color: white;
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
}

.btn-delete:hover {
  background: #dc2626;
}

.error-message {
  background: #fee2e2;
  color: #ef4444;
  padding: 0.75rem;
  border-radius: 8px;
  margin-top: 1rem;
  border: 1px solid #ef4444;
}

.btn-delete-setting {
  background: #ef4444;
  color: white;
  border: none;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  margin-left: 0.5rem;
  transition: background 0.2s;
}

.btn-delete-setting:hover {
  background: #dc2626;
}

/* 新增參數對話框樣式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  color: #333;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
  justify-content: flex-end;
}
</style>

