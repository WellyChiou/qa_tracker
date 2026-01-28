<template>
  <AdminLayout>
    <div class="admin-maintenance">
      <div class="page-head">
        <div>
          <div class="page-title">系統維護</div>
          <div class="page-desc">系統參數、備份管理與維護工具。</div>
        </div>
      </div>

      <!-- 標籤頁 -->
      <div class="tabs-seg" role="tablist" aria-label="Maintenance tabs">
        <button 
          @click="activeTab = 'settings'" 
          :class="['tab', { 'is-active': activeTab === 'settings' }]"
          role="tab"
          :aria-selected="activeTab === 'settings'"
        >
          系統參數
        </button>
        <button 
          @click="activeTab = 'backups'" 
          :class="['tab', { 'is-active': activeTab === 'backups' }]"
          role="tab"
          :aria-selected="activeTab === 'backups'"
        >
          備份管理
        </button>
      </div>

      <!-- 系統參數標籤頁 -->
      <div v-if="activeTab === 'settings'" class="card"><div class="card__body">
        <div class="settings-section">
          <div class="section-actions">
            <button @click="refreshConfig" :disabled="refreshingConfig" class="btn btn-secondary">
              {{ refreshingConfig ? '刷新中...' : '刷新配置' }}
            </button>
            <button @click="loadSettings" class="btn btn-secondary">重新載入</button>
          </div>
          <div v-for="category in categories" :key="category" class="category-group">
            <div class="category-card">
              <div class="category-head">
                <h3 class="category-title">{{ getCategoryName(category) }}</h3>
              </div>
              <div class="category-body">
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
                </div>
              </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div></div>

      <!-- 備份管理標籤頁 -->
      <div v-if="activeTab === 'backups'" class="card"><div class="card__body">
        <div class="backups-section">
          <div class="section-actions">
            <button @click="createBackup" :disabled="creatingBackup" class="btn btn-primary">
              {{ creatingBackup ? '備份中...' : '+ 手動備份' }}
            </button>
            <button @click="loadBackups" class="btn btn-secondary">重新整理</button>
          </div>

          <div v-if="backups.length === 0" class="empty-state">
            <p>尚無備份檔案</p>
          </div>
          <div v-else class="table-wrap backups-table">
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
                    <div class="row-actions">
                      <button @click="downloadBackup(backup.relativePath || backup.filename)" class="btn btn-secondary btn-download">下載</button>
                      <button @click="deleteBackup(backup.relativePath || backup.filename)" class="btn btn-danger btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
	      </div></div>

	    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AdminLayout from '@/components/AdminLayout.vue'
import { toast } from '@shared/composables/useToast'
import { apiRequest, getApiBaseUrl, getAccessToken } from '@/utils/api'

const route = useRoute()
const activeTab = ref(route.query.tab || 'settings')
const settings = ref([])
const backups = ref([])
const creatingBackup = ref(false)
const refreshingConfig = ref(false)
const savingSettings = ref(new Set())
const savedSettings = ref(new Set())

// 通知組件引用
// 顯示通知（使用共用 Toast 系統）
const showNotification = (message, type = 'info', duration = 3000) => {
  const opts = duration > 0 ? { duration } : {}
  if (type === 'success') {
    toast.success(message, '成功', opts)
  } else if (type === 'error') {
    toast.error(message, '錯誤', opts)
  } else if (type === 'warning') {
    toast.warning(message, '提醒', opts)
  } else {
    toast.info(message, '提示', opts)
  }
}

const categories = computed(() => {
  if (!Array.isArray(settings.value) || settings.value.length === 0) {
    return []
  }
  const cats = new Set(settings.value.map(s => s?.category).filter(c => c != null))
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
  if (!Array.isArray(settings.value)) {
    return []
  }
  return settings.value.filter(s => s && s.category === category)
}

const loadSettings = async () => {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/admin/system-settings', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 後端返回 ApiResponse<List<SystemSetting>>，apiRequest 會返回 data 字段（即 List）
      // 所以 data 直接是數組，不需要檢查 data.settings
      settings.value = Array.isArray(data) ? data : (data.settings || data.data || [])
    } else {
      settings.value = []
    }
  } catch (err) {
    console.error('載入系統參數失敗:', err)
    showNotification('載入系統參數失敗: ' + err.message, 'error')
    settings.value = []
  }
}

const saveSetting = async (setting) => {
  if (!setting.isEditable) return
  
  savingSettings.value.add(setting.settingKey)
  savedSettings.value.delete(setting.settingKey)
  
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/system-settings/${setting.settingKey}`, {
      method: 'PUT',
      body: JSON.stringify({
        settingValue: setting.settingValue
      }),
      credentials: 'include'
    })
    
    if (data !== null) {
      // apiRequest 成功返回數據，表示儲存成功
      savedSettings.value.add(setting.settingKey)
      setTimeout(() => {
        savedSettings.value.delete(setting.settingKey)
      }, 2000)
      showNotification('設定已儲存', 'success')
    }
  } catch (err) {
    showNotification('儲存失敗: ' + err.message, 'error')
  } finally {
    savingSettings.value.delete(setting.settingKey)
  }
}

const refreshConfig = async () => {
  refreshingConfig.value = true
  
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/admin/system-settings/refresh', {
      method: 'POST',
      credentials: 'include'
    })
    
    if (data !== null) {
      // apiRequest 成功返回數據，表示刷新成功
      showNotification('配置刷新成功，新的配置已生效', 'success')
      loadSettings()
    } else {
      showNotification('配置刷新失敗', 'error')
    }
  } catch (err) {
    showNotification('配置刷新失敗: ' + err.message, 'error')
  } finally {
    refreshingConfig.value = false
  }
}

const loadBackups = async () => {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/admin/backups', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      backups.value = data.backups || []
    }
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
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/admin/backups/create', {
      method: 'POST',
      credentials: 'include'
    })
    
    if (data !== null) {
      // apiRequest 成功返回數據，表示創建成功
      showNotification('備份創建成功', 'success')
      loadBackups()
    } else {
      showNotification('備份創建失敗', 'error')
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
    // 下載檔案需要直接使用 fetch，因為返回的是 blob，不是 JSON
    const apiUrl = `${getApiBaseUrl()}/church/admin/backups/download?path=${encodeURIComponent(relativePath)}`
    const token = getAccessToken()
    const headers = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    
    const response = await fetch(apiUrl, {
      method: 'GET',
      headers,
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
    } else {
      showNotification('下載失敗', 'error')
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
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/backups/delete?path=${encodeURIComponent(relativePath)}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (data !== null) {
      // apiRequest 成功返回數據，表示刪除成功
      showNotification('備份檔案刪除成功', 'success')
      loadBackups()
    } else {
      showNotification('刪除失敗', 'error')
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
.admin-maintenance{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Tabs */
.tabs-seg{
  display:inline-flex;
  align-items:center;
  gap:6px;
  padding:6px;
  border-radius:999px;
  border:1px solid var(--border);
  background:rgba(2,6,23,.03);
  width:max-content;
}
.tabs-seg .tab{
  border:1px solid transparent;
  background:transparent;
  color:rgba(15,23,42,.78);
  font-weight:800;
  font-size:13px;
  padding:8px 12px;
  border-radius:999px;
  cursor:pointer;
  transition:background .12s ease, border-color .12s ease, transform .12s ease;
}
.tabs-seg .tab:hover{
  background:rgba(255,255,255,.70);
  border-color:rgba(2,6,23,.08);
}
.tabs-seg .tab.is-active{
  background:var(--surface);
  border-color:rgba(37,99,235,.20);
  box-shadow:var(--shadow-sm);
  color:rgba(15,23,42,.92);
}

/* Section actions */
.section-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
  justify-content:flex-end;
  margin-bottom:10px;
}

/* Category */
.category-group{ margin-top:10px; }
.category-card{
  border:1px solid var(--border);
  border-radius:var(--radius);
  background:rgba(255,255,255,.60);
  overflow:hidden;
}
.category-head{
  display:flex;
  align-items:center;
  justify-content:space-between;
  padding:12px 14px;
  background:rgba(2,6,23,.02);
  border-bottom:1px solid var(--border);
}
.category-title{
  font-size:13px;
  font-weight:900;
  letter-spacing:.02em;
  color:rgba(15,23,42,.72);
}
.category-body{ padding:12px 14px 14px; }

/* Settings grid */
.settings-grid{
  display:grid;
  grid-template-columns:repeat(auto-fit, minmax(260px, 1fr));
  gap:12px;
}
.setting-item{
  background:var(--surface);
  border:1px solid var(--border);
  border-radius:14px;
  padding:12px;
  box-shadow:0 1px 0 rgba(2,6,23,.03);
}
.setting-header{ display:flex; align-items:flex-start; justify-content:space-between; gap:10px; margin-bottom:8px; }
.setting-label{ color:rgba(15,23,42,.78); font-weight:900; font-size:13px; }
.readonly-badge{
  display:inline-flex;
  align-items:center;
  padding:3px 8px;
  border-radius:999px;
  border:1px solid rgba(2,6,23,.10);
  background:rgba(2,6,23,.04);
  font-size:11px;
  font-weight:800;
  color:rgba(15,23,42,.60);
  margin-left:8px;
}
.setting-input-group{ position:relative; }
.saving-indicator,
.saved-indicator{
  display:inline-flex;
  margin-top:6px;
  font-size:12px;
  font-weight:800;
}
.saving-indicator{ color:rgba(15,23,42,.55); }
.saved-indicator{ color:var(--success); }
.setting-key{ margin-top:8px; }
.setting-key code{
  font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,"Liberation Mono","Courier New",monospace;
  font-size:12px;
  color:rgba(15,23,42,.55);
  background:rgba(2,6,23,.04);
  border:1px solid rgba(2,6,23,.08);
  padding:3px 8px;
  border-radius:999px;
  display:inline-block;
}

/* Backups */
.empty-state{
  border:1px dashed rgba(2,6,23,.18);
  border-radius:var(--radius);
  padding:16px;
  background:rgba(255,255,255,.55);
  color:rgba(15,23,42,.65);
  font-weight:800;
}
.backups-table td:last-child{
  white-space:nowrap;
}
.database-badge{
  display:inline-flex;
  align-items:center;
  padding:4px 10px;
  border-radius:999px;
  border:1px solid rgba(2,6,23,.10);
  background:rgba(2,6,23,.04);
  font-size:12px;
  font-weight:900;
  color:rgba(15,23,42,.70);
}
.btn-download{ background:rgba(37,99,235,.08); border-color:rgba(37,99,235,.18); color:rgba(29,78,216,.95); }
.btn-download:hover{ background:rgba(37,99,235,.12); }
.btn-delete{ background:rgba(239,68,68,.10); border-color:rgba(239,68,68,.22); color:#b91c1c; }
.btn-delete:hover{ background:rgba(239,68,68,.14); }

@media (max-width: 640px){
  .section-actions{ justify-content:flex-start; }
  .settings-grid{ grid-template-columns:1fr; }
}
</style>
