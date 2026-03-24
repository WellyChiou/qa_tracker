<template>
  <AdminLayout>
    <div class="system-maintenance-page">
      <div class="page-head">
        <div>
          <div class="page-title">系統維護</div>
          <div class="page-desc">集中管理 invest 系統參數、資料庫備份與 LINE 測試訊息。</div>
        </div>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>系統參數</span>
          <strong>{{ settings.length }}</strong>
          <p>目前已載入設定項目數量。</p>
        </article>
        <article class="overview-card">
          <span>備份檔案</span>
          <strong>{{ backups.length }}</strong>
          <p>目前已載入 invest 備份檔案數量。</p>
          <p class="overview-meta">
            排程：{{ backupSchedulerJob ? (backupSchedulerJob.enabled ? '啟用' : '停用') : '-' }} / {{ backupSchedulerJob?.scheduleExpression || '-' }}
          </p>
          <p class="overview-meta">
            最後執行：{{ formatDate(backupSchedulerJob?.lastRunAt) }}（{{ backupSchedulerJob?.lastRunStatus || '未執行' }}）
          </p>
        </article>
        <article class="overview-card">
          <span>編輯權限</span>
          <strong>{{ canEditMaintenance ? '可編輯' : '唯讀' }}</strong>
          <p>{{ canEditMaintenance ? '可儲存設定與執行維護動作。' : '目前帳號僅能查看。' }}</p>
        </article>
      </section>

      <div class="tabs-seg" role="tablist" aria-label="Maintenance tabs">
        <button
          class="tab"
          :class="{ 'is-active': activeTab === 'settings' }"
          role="tab"
          :aria-selected="activeTab === 'settings'"
          @click="activeTab = 'settings'"
        >
          系統參數
        </button>
        <button
          v-if="canViewStrategy"
          class="tab"
          :class="{ 'is-active': activeTab === 'strategy' }"
          role="tab"
          :aria-selected="activeTab === 'strategy'"
          @click="activeTab = 'strategy'"
        >
          策略設定
        </button>
        <button
          class="tab"
          :class="{ 'is-active': activeTab === 'backups' }"
          role="tab"
          :aria-selected="activeTab === 'backups'"
          @click="activeTab = 'backups'"
        >
          備份管理
        </button>
      </div>

      <div v-if="activeTab === 'settings'" class="card">
        <div class="card__body">
          <div class="settings-section">
            <div class="section-actions">
              <button class="btn btn-secondary" :disabled="refreshingConfig || !canEditMaintenance" @click="refreshConfig">
                {{ refreshingConfig ? '刷新中...' : '刷新設定' }}
              </button>
              <button class="btn btn-secondary" @click="loadSettings">重新載入</button>
            </div>

            <div v-for="category in categories" :key="category" class="category-group">
              <div class="category-card">
                <div class="category-head">
                  <h3 class="category-title">{{ getCategoryName(category) }}</h3>
                  <button
                    v-if="category === 'line.bot'"
                    class="btn btn-primary btn-sm"
                    :disabled="!canEditMaintenance"
                    @click="showTestMessageModal = true"
                  >
                    測試群組訊息
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
                        v-model="setting.settingValue"
                        :type="setting.settingType === 'number' ? 'number' : 'text'"
                        :disabled="!canEdit(setting)"
                        class="form-input"
                        @blur="saveSetting(setting)"
                      />

                      <select
                        v-else-if="setting.settingType === 'boolean'"
                        v-model="setting.settingValue"
                        :disabled="!canEdit(setting)"
                        class="form-input"
                        @change="saveSetting(setting)"
                      >
                        <option value="true">是</option>
                        <option value="false">否</option>
                      </select>

                      <textarea
                        v-else-if="setting.settingType === 'json'"
                        v-model="setting.settingValue"
                        :disabled="!canEdit(setting)"
                        class="form-input"
                        rows="3"
                        @blur="saveSetting(setting)"
                      />

                      <input
                        v-else
                        v-model="setting.settingValue"
                        type="text"
                        :disabled="!canEdit(setting)"
                        class="form-input"
                        @blur="saveSetting(setting)"
                      />

                      <span v-if="savingSettings.has(setting.settingKey)" class="saving-indicator">儲存中...</span>
                      <span v-if="savedSettings.has(setting.settingKey)" class="saved-indicator">✓ 已儲存</span>
                    </div>

                    <div class="setting-key">
                      <code>{{ setting.settingKey }}</code>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="categories.length === 0" class="empty-state">尚無系統參數資料</div>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'strategy'" class="card">
        <div class="card__body">
          <div class="strategy-section">
            <div class="section-actions">
              <button class="btn btn-secondary" :disabled="loadingStrategy" @click="loadStrategySettings">
                重新載入
              </button>
              <button class="btn btn-primary" :disabled="savingStrategy || !canEditStrategy" @click="saveStrategySettings">
                {{ savingStrategy ? '儲存中...' : '儲存策略設定' }}
              </button>
            </div>

            <div class="strategy-meta">
              <div class="strategy-meta-item">
                <span>策略版本</span>
                <strong>v{{ strategySettings.strategyVersion ?? '-' }}</strong>
              </div>
              <div class="strategy-meta-item">
                <span>最後更新</span>
                <strong>{{ formatDate(strategySettings.lastUpdatedAt) }}</strong>
              </div>
            </div>

            <div class="strategy-grid">
              <article class="strategy-card">
                <h3>Strength 分數區間</h3>
                <div class="strategy-fields">
                  <label>
                    STRONG 最低分
                    <input v-model.number="strategySettings.strength.strongMin" type="number" class="form-input" :disabled="!canEditStrategy" />
                  </label>
                  <label>
                    GOOD 最低分
                    <input v-model.number="strategySettings.strength.goodMin" type="number" class="form-input" :disabled="!canEditStrategy" />
                  </label>
                  <label>
                    WEAK 最高分
                    <input v-model.number="strategySettings.strength.weakMax" type="number" class="form-input" :disabled="!canEditStrategy" />
                  </label>
                </div>
              </article>

              <article class="strategy-card">
                <h3>Data Quality 門檻</h3>
                <div class="strategy-fields">
                  <label>
                    最小歷史天數
                    <input v-model.number="strategySettings.dataQuality.minHistoryDays" type="number" class="form-input" :disabled="!canEditStrategy" />
                  </label>
                  <label>
                    STALE 天數
                    <input v-model.number="strategySettings.dataQuality.staleDays" type="number" class="form-input" :disabled="!canEditStrategy" />
                  </label>
                </div>
              </article>

              <article class="strategy-card">
                <h3>Opportunity 條件</h3>
                <div class="strategy-fields">
                  <label>
                    OBSERVE 最低分
                    <input v-model.number="strategySettings.opportunity.observeMinScore" type="number" class="form-input" :disabled="!canEditStrategy" />
                  </label>
                  <label>
                    WAIT_PULLBACK 最低分
                    <input v-model.number="strategySettings.opportunity.waitPullbackMinScore" type="number" class="form-input" :disabled="!canEditStrategy" />
                  </label>
                  <label>
                    REEVALUATE 最低分
                    <input v-model.number="strategySettings.opportunity.reevaluateMinScore" type="number" class="form-input" :disabled="!canEditStrategy" />
                  </label>
                </div>
              </article>
            </div>

            <p class="strategy-tip">提示：只有策略值有實際變更時，系統才會自動升版。</p>
          </div>
        </div>
      </div>

      <div v-else class="card">
        <div class="card__body">
          <div class="backups-section">
            <div class="section-actions">
              <button class="btn btn-primary" :disabled="creatingBackup || !canEditMaintenance" @click="createBackup">
                {{ creatingBackup ? '備份中...' : '+ 手動備份' }}
              </button>
              <button class="btn btn-secondary" @click="loadBackups">重新整理</button>
            </div>

            <div v-if="backups.length === 0" class="empty-state">尚無備份檔案</div>

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
                  <tr v-for="backup in backups" :key="backup.relativePath || backup.filename">
                    <td>{{ backup.filename }}</td>
                    <td>
                      <span class="database-badge">{{ backup.database }}</span>
                    </td>
                    <td>{{ backup.sizeFormatted }}</td>
                    <td>{{ formatDate(backup.modified) }}</td>
                    <td>
                      <div class="row-actions">
                        <button class="btn btn-secondary btn-download" @click="downloadBackup(backup.relativePath || backup.filename)">
                          下載
                        </button>
                        <button
                          class="btn btn-danger btn-delete"
                          :disabled="!canEditMaintenance"
                          @click="deleteBackup(backup.relativePath || backup.filename)"
                        >
                          刪除
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showTestMessageModal" class="modal-overlay" @click.self="showTestMessageModal = false">
        <div class="modal-content">
          <h3>發送測試群組訊息</h3>
          <form @submit.prevent="sendTestMessage">
            <div class="form-group">
              <label>群組 ID（Group ID）*</label>
              <input
                v-model="testMessage.groupId"
                type="text"
                required
                class="form-input"
                placeholder="輸入 LINE 群組 ID（C...）"
              />
            </div>

            <div class="form-group">
              <label>訊息內容 *</label>
              <textarea
                v-model="testMessage.message"
                required
                class="form-input"
                rows="4"
                placeholder="輸入要發送的測試訊息..."
              />
            </div>

            <div class="modal-actions">
              <button type="submit" class="btn btn-primary" :disabled="sendingTestMessage || !canEditMaintenance">
                {{ sendingTestMessage ? '發送中...' : '發送' }}
              </button>
              <button type="button" class="btn btn-secondary" @click="showTestMessageModal = false">取消</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { toast } from '@shared/composables/useToast'
import { hasPermission } from '@shared/utils/permission'
import AdminLayout from '@/components/AdminLayout.vue'
import { useAuth } from '@/composables/useAuth'
import { getInvestAccessToken, getInvestApiBaseUrl, investApiService } from '@/composables/useInvestApi'

const { currentUser } = useAuth()

const activeTab = ref('settings')
const settings = ref([])
const backups = ref([])
const backupSchedulerJob = ref(null)
const loadingStrategy = ref(false)
const savingStrategy = ref(false)
const strategySettings = ref({
  strategyVersion: null,
  lastUpdatedAt: null,
  strength: {
    strongMin: 80,
    goodMin: 60,
    weakMax: 39
  },
  dataQuality: {
    minHistoryDays: 20,
    staleDays: 3
  },
  opportunity: {
    observeMinScore: 80,
    waitPullbackMinScore: 65,
    reevaluateMinScore: 45
  }
})

const creatingBackup = ref(false)
const refreshingConfig = ref(false)
const savingSettings = ref(new Set())
const savedSettings = ref(new Set())

const showTestMessageModal = ref(false)
const sendingTestMessage = ref(false)
const testMessage = ref({
  groupId: '',
  message: ''
})

const canEditMaintenance = computed(() => hasPermission(currentUser.value, 'INVEST_SYS_MAINTENANCE_EDIT'))
const canViewScheduler = computed(() =>
  hasPermission(currentUser.value, 'INVEST_SYS_SCHEDULER_VIEW')
  || hasPermission(currentUser.value, 'INVEST_SYS_SCHEDULER_RUN')
  || hasPermission(currentUser.value, 'INVEST_SYS_SCHEDULER_MANAGE')
)
const canViewStrategy = computed(() =>
  hasPermission(currentUser.value, 'INVEST_STRATEGY_VIEW') || hasPermission(currentUser.value, 'INVEST_STRATEGY_EDIT')
)
const canEditStrategy = computed(() => hasPermission(currentUser.value, 'INVEST_STRATEGY_EDIT'))

const categories = computed(() => {
  if (!Array.isArray(settings.value) || settings.value.length === 0) {
    return []
  }
  const unique = new Set(
    settings.value
      .filter(item => item?.category !== 'invest.strategy')
      .map(item => item?.category)
      .filter(Boolean)
  )
  return Array.from(unique).sort()
})

const getCategoryName = (category) => {
  const names = {
    backup: '備份設定',
    scheduler: '排程設定',
    'line.bot': 'LINE Bot 設定',
    'invest.analysis': '投資分析設定',
    system: '系統設定'
  }
  return names[category] || category
}

const getSettingsByCategory = (category) => {
  return settings.value.filter(
    setting => setting?.category === category && setting?.category !== 'invest.strategy'
  )
}

const canEdit = (setting) => {
  return canEditMaintenance.value && setting?.isEditable && !savingSettings.value.has(setting.settingKey)
}

const showNotification = (message, type = 'info') => {
  if (type === 'success') {
    toast.success(message)
  } else if (type === 'error') {
    toast.error(message)
  } else if (type === 'warning') {
    toast.warning(message)
  } else {
    toast.info(message)
  }
}

const loadSettings = async () => {
  try {
    const data = await investApiService.getSystemMaintenanceSettings()
    settings.value = Array.isArray(data) ? data : []
  } catch (error) {
    settings.value = []
    showNotification(`載入系統設定失敗：${error.message}`, 'error')
  }
}

const saveSetting = async (setting) => {
  if (!canEdit(setting)) {
    return
  }

  savingSettings.value.add(setting.settingKey)
  savedSettings.value.delete(setting.settingKey)

  try {
    await investApiService.updateSystemMaintenanceSetting(setting.settingKey, {
      settingValue: setting.settingValue
    })

    savedSettings.value.add(setting.settingKey)
    setTimeout(() => {
      savedSettings.value.delete(setting.settingKey)
    }, 2000)
    showNotification('設定已儲存', 'success')
  } catch (error) {
    showNotification(`儲存失敗：${error.message}`, 'error')
  } finally {
    savingSettings.value.delete(setting.settingKey)
  }
}

const refreshConfig = async () => {
  refreshingConfig.value = true
  try {
    await investApiService.refreshSystemMaintenanceSettings()
    await loadSettings()
    showNotification('設定快取已刷新', 'success')
  } catch (error) {
    showNotification(`刷新設定失敗：${error.message}`, 'error')
  } finally {
    refreshingConfig.value = false
  }
}

const loadBackups = async () => {
  try {
    const data = await investApiService.getSystemMaintenanceBackups()
    backups.value = Array.isArray(data?.backups) ? data.backups : []
  } catch (error) {
    backups.value = []
    showNotification(`載入備份清單失敗：${error.message}`, 'error')
  }
}

const loadBackupSchedulerJob = async () => {
  if (!canViewScheduler.value) {
    backupSchedulerJob.value = null
    return
  }
  try {
    const jobs = await investApiService.getSystemSchedulerJobs({ includeInactive: true })
    const list = Array.isArray(jobs) ? jobs : []
    backupSchedulerJob.value = list.find(job => job.jobCode === 'DATABASE_BACKUP') || null
  } catch (error) {
    backupSchedulerJob.value = null
    showNotification(`載入備份排程資訊失敗：${error.message}`, 'warning')
  }
}

const normalizeStrategyPayload = (payload) => ({
  strategyVersion: payload?.strategyVersion ?? null,
  lastUpdatedAt: payload?.lastUpdatedAt ?? null,
  strength: {
    strongMin: Number(payload?.strength?.strongMin ?? 80),
    goodMin: Number(payload?.strength?.goodMin ?? 60),
    weakMax: Number(payload?.strength?.weakMax ?? 39)
  },
  dataQuality: {
    minHistoryDays: Number(payload?.dataQuality?.minHistoryDays ?? 20),
    staleDays: Number(payload?.dataQuality?.staleDays ?? 3)
  },
  opportunity: {
    observeMinScore: Number(payload?.opportunity?.observeMinScore ?? 80),
    waitPullbackMinScore: Number(payload?.opportunity?.waitPullbackMinScore ?? 65),
    reevaluateMinScore: Number(payload?.opportunity?.reevaluateMinScore ?? 45)
  }
})

const loadStrategySettings = async () => {
  if (!canViewStrategy.value) {
    return
  }
  loadingStrategy.value = true
  try {
    const data = await investApiService.getSystemStrategySettings()
    strategySettings.value = normalizeStrategyPayload(data)
  } catch (error) {
    showNotification(`載入策略設定失敗：${error.message}`, 'error')
  } finally {
    loadingStrategy.value = false
  }
}

const saveStrategySettings = async () => {
  if (!canEditStrategy.value) {
    return
  }
  savingStrategy.value = true
  try {
    const payload = {
      strength: strategySettings.value.strength,
      dataQuality: strategySettings.value.dataQuality,
      opportunity: strategySettings.value.opportunity
    }
    const data = await investApiService.updateSystemStrategySettings(payload)
    strategySettings.value = normalizeStrategyPayload(data)
    showNotification('策略設定已更新', 'success')
  } catch (error) {
    showNotification(`更新策略設定失敗：${error.message}`, 'error')
  } finally {
    savingStrategy.value = false
  }
}

const createBackup = async () => {
  creatingBackup.value = true
  try {
    await investApiService.createSystemMaintenanceBackup()
    showNotification('備份建立成功', 'success')
    await loadBackups()
  } catch (error) {
    showNotification(`備份建立失敗：${error.message}`, 'error')
  } finally {
    creatingBackup.value = false
  }
}

const downloadBackup = async (relativePath) => {
  try {
    const apiUrl = `${getInvestApiBaseUrl()}/system/maintenance/backups/download?path=${encodeURIComponent(relativePath)}`
    const token = getInvestAccessToken()

    const headers = {}
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }

    const response = await fetch(apiUrl, {
      method: 'GET',
      headers
    })

    if (!response.ok) {
      let errorMessage = '下載備份失敗'
      try {
        const errorJson = await response.json()
        errorMessage = errorJson?.message || errorMessage
      } catch (_) {
        // ignore
      }
      throw new Error(errorMessage)
    }

    const blob = await response.blob()
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = relativePath.split('/').pop() || 'invest-backup.sql.gz'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)

    showNotification('備份下載成功', 'success')
  } catch (error) {
    showNotification(`下載備份失敗：${error.message}`, 'error')
  }
}

const deleteBackup = async (relativePath) => {
  if (!window.confirm('確定要刪除這份備份檔案嗎？')) {
    return
  }

  try {
    await investApiService.deleteSystemMaintenanceBackup(relativePath)
    showNotification('備份刪除成功', 'success')
    await loadBackups()
  } catch (error) {
    showNotification(`刪除備份失敗：${error.message}`, 'error')
  }
}

const sendTestMessage = async () => {
  sendingTestMessage.value = true
  try {
    await investApiService.sendSystemLineTestGroupPush({
      groupId: testMessage.value.groupId,
      message: testMessage.value.message
    })

    showNotification('測試訊息發送成功', 'success')
    showTestMessageModal.value = false
    testMessage.value.message = ''
  } catch (error) {
    showNotification(`測試訊息發送失敗：${error.message}`, 'error')
  } finally {
    sendingTestMessage.value = false
  }
}

const formatDate = (value) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  return date.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(async () => {
  if (activeTab.value === 'strategy' && !canViewStrategy.value) {
    activeTab.value = 'settings'
  }
  const jobs = [loadSettings(), loadBackups()]
  if (canViewScheduler.value) {
    jobs.push(loadBackupSchedulerJob())
  }
  if (canViewStrategy.value) {
    jobs.push(loadStrategySettings())
  }
  await Promise.all(jobs)
})
</script>

<style scoped>
.system-maintenance-page {
  display: grid;
  gap: 20px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.page-title {
  font-size: 30px;
  font-weight: 800;
  color: #0f172a;
}

.page-desc {
  margin-top: 4px;
  color: #475569;
  font-size: 15px;
}

.overview-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.overview-card {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(2, 6, 23, 0.08);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.overview-card--accent {
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%);
  color: #fff;
}

.overview-card span {
  display: block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .04em;
  opacity: .8;
}

.overview-card strong {
  display: block;
  margin-top: 6px;
  font-size: 30px;
  line-height: 1;
}

.overview-card p {
  margin-top: 8px;
  font-size: 13px;
  opacity: .88;
}

.overview-card .overview-meta {
  margin-top: 4px;
  font-size: 12px;
  opacity: .9;
  word-break: break-word;
}

.tabs-seg {
  display: inline-flex;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: #e2e8f0;
}

.tab {
  border: 0;
  border-radius: 999px;
  background: transparent;
  padding: 9px 16px;
  font-weight: 700;
  color: #334155;
  cursor: pointer;
}

.tab.is-active {
  background: #fff;
  color: #0f172a;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12);
}

.section-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-bottom: 14px;
}

.category-group {
  margin-bottom: 14px;
}

.category-card {
  border: 1px solid rgba(2, 6, 23, 0.08);
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.category-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.category-title {
  margin: 0;
  font-size: 17px;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 10px;
}

.setting-item {
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 12px;
  padding: 12px;
  background: #f8fafc;
}

.setting-label {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.readonly-badge {
  margin-left: 8px;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #475569;
}

.setting-input-group {
  margin-top: 8px;
}

.setting-key {
  margin-top: 8px;
  font-size: 12px;
  color: #64748b;
}

.strategy-section {
  display: grid;
  gap: 14px;
}

.strategy-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.strategy-meta-item {
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 12px;
  background: #f8fafc;
  padding: 12px;
}

.strategy-meta-item span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.strategy-meta-item strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 16px;
}

.strategy-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
}

.strategy-card {
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 12px;
  background: #fff;
  padding: 12px;
}

.strategy-card h3 {
  margin: 0 0 10px;
  font-size: 15px;
  color: #0f172a;
}

.strategy-fields {
  display: grid;
  gap: 10px;
}

.strategy-fields label {
  display: grid;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.strategy-tip {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.saving-indicator {
  margin-left: 8px;
  color: #ea580c;
  font-size: 12px;
}

.saved-indicator {
  margin-left: 8px;
  color: #16a34a;
  font-size: 12px;
}

.backups-table table {
  width: 100%;
  border-collapse: collapse;
}

.backups-table th,
.backups-table td {
  padding: 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  text-align: left;
}

.row-actions {
  display: flex;
  gap: 8px;
}

.database-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
  font-weight: 700;
  font-size: 12px;
}

.empty-state {
  padding: 18px;
  text-align: center;
  color: #64748b;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
}

.modal-content {
  width: min(560px, calc(100vw - 32px));
  background: #fff;
  border-radius: 14px;
  padding: 18px;
}

.form-group {
  margin-bottom: 12px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  background: #fff;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}

@media (max-width: 768px) {
  .tabs-seg {
    display: flex;
    width: 100%;
  }

  .tab {
    flex: 1;
    text-align: center;
  }
}
</style>
