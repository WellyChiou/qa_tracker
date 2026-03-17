<template>
  <AdminLayout>
    <div class="admin-page">
    <header class="header">
      <div class="header-top">
        <h1>🔧 系統維護</h1>
      </div>
    </header>

    <main class="main-content">
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
            <!-- Added "New Setting" button from Remote -->
            <button @click="showCreateModal = true" class="btn btn-primary me-2">
               <i class="fas fa-plus me-2"></i>新增參數
            </button>
            <button @click="refreshConfig" :disabled="refreshingConfig" class="btn btn-secondary me-2">
              <i class="fas fa-sync-alt me-2" :class="{ 'fa-spin': refreshingConfig }"></i>
              {{ refreshingConfig ? '刷新中...' : '刷新配置' }}
            </button>
            <button @click="loadSettings" class="btn btn-secondary">
              <i class="fas fa-redo me-2"></i>重新載入
            </button>
          </div>
          <div v-for="category in categories" :key="category" class="category-group">
            <div class="category-header">
              <h3 class="category-title">{{ getCategoryName(category) }}</h3>
              
              <!-- 只有在 linebot 分類時顯示測試按鈕 -->
              <button 
                v-if="category === 'linebot'"
                @click="showTestMessageModal = true"
                class="btn btn-sm btn-primary ms-3"
              >
                <i class="fas fa-paper-plane me-1"></i> 測試群組訊息
              </button>
            </div>

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
                  <!-- Added Delete button from Remote -->
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

       <!-- 新增參數對話框 (From Remote) -->
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

      <!-- 測試訊息對話框 -->
      <div v-if="showTestMessageModal" class="modal-overlay" @click.self="showTestMessageModal = false">
        <div class="modal-content">
          <h3>發送測試群組訊息</h3>
          <form @submit.prevent="sendTestMessage">
            <div class="form-group">
              <label>群組 ID (Group ID) *</label>
              <input v-model="testMessage.groupId" type="text" required class="form-input" placeholder="輸入 LINE 群組 ID (C...)" />
              <small class="form-text text-muted">可以從 LINE Bot webhook 日誌中找到群組 ID</small>
            </div>
            <div class="form-group">
              <label>訊息內容 *</label>
              <textarea v-model="testMessage.message" required class="form-input" rows="4" placeholder="輸入要發送的測試訊息..."></textarea>
            </div>
            <div class="modal-actions">
              <button type="submit" :disabled="sendingTestMessage" class="btn btn-primary">
                <i class="fas fa-paper-plane me-2"></i>
                {{ sendingTestMessage ? '發送中...' : '發送' }}
              </button>
              <button type="button" @click="showTestMessageModal = false" class="btn btn-secondary">取消</button>
            </div>
          </form>
        </div>
      </div>

      <!-- 備份管理標籤頁 -->
      <div v-if="activeTab === 'backups'" class="tab-content">
        <div class="backups-section">
          <div class="backup-actions">
            <button @click="createBackup" :disabled="creatingBackup" class="btn btn-primary me-2">
              <i class="fas fa-save me-2"></i>
              {{ creatingBackup ? '備份中...' : '+ 手動備份' }}
            </button>
            <button @click="loadBackups" class="btn btn-secondary">
              <i class="fas fa-redo me-2"></i>重新整理
            </button>
          </div>

          <div v-if="backups.length === 0" class="empty-state">
            <p>尚無備份檔案</p>
          </div>
          <div v-else class="backups-table">
            <table class="data-table">
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
                    <div class="actions">
                      <button @click="downloadBackup(backup.relativePath || backup.filename)" class="btn-sm btn-edit me-2">下載</button>
                      <button @click="deleteBackup(backup.relativePath || backup.filename)" class="btn-sm btn-delete">刪除</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </main>

    <!-- 通知已移至全局 ToastHost -->
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue'
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { apiService } from '@/composables/useApi'
import { toast } from '@shared/composables/useToast'

const route = useRoute()
const activeTab = ref(route.query.tab || 'settings')
const settings = ref([])
const backups = ref([])
const creatingBackup = ref(false)
const refreshingConfig = ref(false)
const savingSettings = ref(new Set())
const savedSettings = ref(new Set())
// notification 已改用全局 toast 系統

// Remote additions for create
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

// Test Message Modal
const showTestMessageModal = ref(false)
const sendingTestMessage = ref(false)
const testMessage = ref({
  groupId: 'C421bdbec9e301e1d93fe9aceef84243c',
  message: ''
})

// showNotification 已改用全局 toast 系統

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
    const data = await apiService.getSystemSettings()
    settings.value = Array.isArray(data) ? data : (data?.settings || [])
  } catch (err) {
    toast.error('載入系統參數失敗: ' + err.message)
  }
}

const saveSetting = async (setting) => {
  if (!setting.isEditable) return
  
  savingSettings.value.add(setting.settingKey)
  savedSettings.value.delete(setting.settingKey)
  
  try {
    const response = await apiService.updateSystemSetting(setting.settingKey, setting.settingValue)
    // ApiResponse 成功時回傳 data（可能為 null 或 { success, message, setting }）
    if (response != null) {
      savedSettings.value.add(setting.settingKey)
      setTimeout(() => {
        savedSettings.value.delete(setting.settingKey)
      }, 2000)
      toast.success('設定已儲存')
    } else {
      toast.error(response?.message || '儲存失敗')
    }
  } catch (err) {
    toast.error('儲存失敗: ' + err.message)
  } finally {
    savingSettings.value.delete(setting.settingKey)
  }
}

const createSetting = async () => {
  creatingSetting.value = true
  
  try {
    const data = await apiService.createSystemSetting(newSetting.value)
    // ApiResponse 成功時回傳 data（創建成功為 SystemSetting 實體）
    if (data != null) {
      toast.success('參數創建成功')
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
      toast.error(data.message || '創建失敗')
    }
  } catch (err) {
    toast.error('創建失敗: ' + err.message)
  } finally {
    creatingSetting.value = false
  }
}

const sendTestMessage = async () => {
  if (!testMessage.value.groupId || !testMessage.value.message) {
    toast.error('請填寫群組 ID 和訊息內容')
    return
  }

  sendingTestMessage.value = true
  
  try {
    // 調用後端 API 發送測試訊息
    // 注意：這裡假設後端有對應的 API，如果沒有需要手動實現 fetch
    const token = localStorage.getItem('personal_access_token')
    const headers = {
      'Content-Type': 'application/json'
    }
    if (token) headers['Authorization'] = `Bearer ${token}`
    
    const response = await fetch('/api/personal/line/test/group-push', {
      method: 'POST',
      headers,
      body: JSON.stringify(testMessage.value)
    })
    
    const data = await response.json()
    
    if (response.ok && data.success) {
      toast.success('測試訊息發送成功')
      showTestMessageModal.value = false
      testMessage.value.message = '' // 清空訊息，保留 Group ID 方便下次測試
    } else {
      toast.error(data.message || '發送失敗')
    }
  } catch (err) {
    toast.error('發送失敗: ' + err.message)
  } finally {
    sendingTestMessage.value = false
  }
}

const deleteSetting = async (settingKey) => {
  if (!confirm(`確定要刪除參數 "${settingKey}" 嗎？此操作無法復原。`)) {
    return
  }
  
  try {
    const data = await apiService.deleteSystemSetting(settingKey)
    
    if (data.success) {
      toast.success('參數刪除成功')
      loadSettings()
    } else {
      toast.error(data.message || '刪除失敗')
    }
  } catch (err) {
    toast.error('刪除失敗: ' + err.message)
  }
}

const refreshConfig = async () => {
  refreshingConfig.value = true
  
  try {
    await apiService.refreshSystemSettings()
    toast.success('配置刷新成功，新的配置已生效')
    loadSettings()
  } catch (err) {
    toast.error('配置刷新失敗: ' + err.message)
  } finally {
    refreshingConfig.value = false
  }
}

const loadBackups = async () => {
  try {
    const data = await apiService.getBackups()
    if (data && data.backups) {
      backups.value = data.backups
    }
  } catch (err) {
    toast.error('載入備份列表失敗: ' + err.message)
  }
}

const createBackup = async () => {
  if (!confirm('確定要立即執行備份嗎？這可能需要一些時間。')) {
    return
  }
  
  creatingBackup.value = true
  
  try {
    const response = await apiService.createBackup()
    // ApiResponse 成功時 data 為 { success, message, output? }
    if (response && (response.success !== false)) {
      toast.success('備份創建成功')
      loadBackups()
    } else {
      toast.error(response?.message || '備份創建失敗')
    }
  } catch (err) {
    toast.error('備份創建失敗: ' + err.message)
  } finally {
    creatingBackup.value = false
  }
}

const downloadBackup = async (relativePath) => {
  try {
    const token = localStorage.getItem('personal_access_token')
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    
    const filename = relativePath.split('/').pop() || relativePath
    const url = `/api/personal/backups/download?path=${encodeURIComponent(relativePath)}`
    
    const response = await fetch(url, { headers })
    
    if (response.ok) {
      const blob = await response.blob()
      const downloadUrl = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = downloadUrl
      a.download = filename
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(downloadUrl)
      document.body.removeChild(a)
      toast.success('備份檔案下載開始')
    } else {
      toast.error('下載失敗')
    }
  } catch (err) {
    toast.error('下載失敗: ' + err.message)
  }
}

const deleteBackup = async (relativePath) => {
  const filename = relativePath.split('/').pop() || relativePath
  if (!confirm(`確定要刪除備份檔案 "${filename}" 嗎？`)) {
    return
  }
  
  try {
    await apiService.deleteBackup(relativePath)
    toast.success('備份檔案刪除成功')
    loadBackups()
  } catch (err) {
    toast.error('刪除失敗: ' + err.message)
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
.admin-page {
  min-height: 100vh;
  background: var(--bg-primary);
}

.header {
  padding: 2rem;
  background: var(--bg-card);
  box-shadow: var(--shadow);
  margin-bottom: 2rem;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1400px;
  margin: 0 auto;
}

.header h1 {
  margin: 0;
  font-size: 1.8rem;
  color: #333;
  font-weight: 600;
}

.main-content {
  padding: 0 2rem 2rem 2rem;
  max-width: 1400px;
  margin: 0 auto;
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
  background: rgba(102, 126, 234, 0.05);
  border-radius: 8px 8px 0 0;
}

.tab-button.active {
  color: #667eea;
  border-bottom-color: #667eea;
}

.tab-content {
  background: var(--bg-card);
  border-radius: var(--border-radius);
  padding: 2rem;
  box-shadow: var(--shadow-md);
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 系統參數樣式 */
.settings-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e0e0e0;
}

.category-group {
  margin-bottom: 2rem;
}

.form-text {
  display: block;
  margin-top: 0.25rem;
  font-size: 0.85rem;
  color: #6c757d;
}

.text-muted {
  color: #6c757d;
}

.ms-3 {
  margin-left: 1rem;
}

.me-1 {
  margin-right: 0.25rem;
}

.category-header {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.category-header .category-title {
  margin-bottom: 0;
  border-bottom: none;
  padding-bottom: 0;
}

.category-title {
  font-size: 1.3rem;
  color: #333;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 1.5rem;
}

.setting-item {
  padding: 1rem;
  background: #f9fafb;
  border: 1px solid #e0e0e0;
  border-radius: var(--border-radius);
  transition: transform 0.2s;
}

.setting-item:hover {
  background: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}

.setting-header {
  margin-bottom: 0.75rem;
}

.setting-label {
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.readonly-badge {
  background: #e5e7eb;
  color: #4b5563;
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
  border: 1px solid #d1d5db;
  border-radius: var(--border-radius);
  font-size: 1rem;
  transition: all 0.2s;
  background: var(--bg-card);
  color: #1f2937;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input:disabled {
  background: #f3f4f6;
  color: #9ca3af;
  cursor: not-allowed;
  border-color: #e5e7eb;
}

.setting-key {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.setting-key code {
  background: #f3f4f6;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  color: #4b5563;
}

.btn-delete-setting {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  padding: 4px;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.btn-delete-setting:hover {
  opacity: 1;
}

.saving-indicator,
.saved-indicator {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.85rem;
  font-weight: 600;
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

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #6b7280;
  background: #f9fafb;
  border-radius: var(--border-radius);
  border: 1px dashed #d1d5db;
}

.backups-table {
  overflow-x: auto;
  border: 1px solid #e0e0e0;
  border-radius: var(--border-radius);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: var(--bg-card);
}

.data-table th,
.data-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.data-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
  text-transform: uppercase;
  font-size: 0.85rem;
  letter-spacing: 0.05em;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.database-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.database-badge.qa_tracker {
  background: #dbeafe;
  color: #1e40af;
}

.database-badge.church {
  background: #d1fae5;
  color: #065f46;
}

.btn {
  padding: 0.6rem 1.2rem;
  border-radius: var(--border-radius);
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.95rem;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #5a67d8;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(102, 126, 234, 0.25);
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.btn-sm {
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  color: white;
  transition: all 0.2s;
}

.me-2 {
  margin-right: 0.5rem;
}

.notification {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  left: auto;
  padding: 1rem 1.5rem;
  border-radius: var(--border-radius);
  color: white;
  font-weight: 600;
  z-index: 10000;
  animation: slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  min-width: 300px;
}

.notification.success {
  background: #10b981;
}

.notification.error {
  background: #ef4444;
}

@keyframes slideIn {
  from {
    transform: translateY(100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* Modal Styles */
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
  background: var(--bg-card);
  border-radius: var(--border-radius);
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
/* ============================================
   Icon buttons: 編輯 / 刪除（參考教會後台風格）
   - 不影響 API / 邏輯，只統一視覺
   ============================================ */
.btn-edit, .btn-delete {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
  padding: 0.75rem 1.5rem;
  border-radius: 999px;
  border: 2px solid transparent;
  font-weight: 800;
  letter-spacing: 0.02em;
  cursor: pointer;
  user-select: none;
  transition: all 0.18s ease;
  background: transparent;
}

.btn-sm.btn-edit, .btn-sm.btn-delete {
  padding: 0.55rem 1.15rem;
  font-size: 0.9rem;
}

.btn-edit {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.btn-edit:hover {
  background: #dbeafe;
  border-color: #93c5fd;
  transform: translateY(-1px);
}

.btn-delete {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.btn-delete:hover {
  background: #fee2e2;
  border-color: #fca5a5;
  transform: translateY(-1px);
}

.btn-edit::before,
.btn-delete::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-repeat: no-repeat;
  background-position: center;
  background-size: 20px 20px;
  flex: 0 0 20px;
}

.btn-edit::before {
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http://www.w3.org/2000/svg%22%20viewBox%3D%220%200%2024%2024%22%20fill%3D%22none%22%20stroke%3D%22%231d4ed8%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cpath%20d%3D%22M12%2020h9%22/%3E%3Cpath%20d%3D%22M16.5%203.5a2.121%202.121%200%200%201%203%203L7%2019l-4%201%201-4%2012.5-12.5z%22/%3E%3C/svg%3E");
}

.btn-delete::before {
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http://www.w3.org/2000/svg%22%20viewBox%3D%220%200%2024%2024%22%20fill%3D%22none%22%20stroke%3D%22%23b91c1c%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cpolyline%20points%3D%223%206%205%206%2021%206%22/%3E%3Cpath%20d%3D%22M19%206l-1%2014a2%202%200%200%201-2%202H8a2%202%200%200%201-2-2L5%206%22/%3E%3Cpath%20d%3D%22M10%2011v6%22/%3E%3Cpath%20d%3D%22M14%2011v6%22/%3E%3Cpath%20d%3D%22M9%206V4a2%202%200%200%201%202-2h2a2%202%200%200%201%202%202v2%22/%3E%3C/svg%3E");
}

</style>
