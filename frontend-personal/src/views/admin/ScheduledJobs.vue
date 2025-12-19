<template>
  <div class="admin-page">
    <TopNavbar />
    <header class="header">
      <div class="header-top">
        <h1>⏰ 定時任務管理</h1>
        <button class="btn btn-primary" @click="showAddModal = true">
          <i class="fas fa-plus me-2"></i>新增任務
        </button>
      </div>
    </header>

    <main class="main-content">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>任務名稱</th>
            <th>Cron 表達式</th>
            <th>描述</th>
            <th>啟用狀態</th>
            <th>執行狀態</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" style="text-align: center; padding: 20px;">載入中...</td>
          </tr>
          <tr v-else-if="jobs.length === 0">
            <td colspan="7" style="text-align: center; padding: 20px;">尚無任務</td>
          </tr>
          <tr v-for="job in jobs" :key="job.id">
            <td>{{ job.id }}</td>
            <td>{{ job.jobName }}</td>
            <td>
              <div style="font-weight: 500; color: #1e293b; margin-bottom: 0.25rem;">
                {{ formatCronExpression(job.cronExpression) }}
              </div>
              <code style="font-size: 0.75em; color: #94a3b8; background: #f1f5f9; padding: 0.125rem 0.375rem; border-radius: 0.25rem;">
                {{ job.cronExpression }}
              </code>
            </td>
            <td :title="job.description || '-'">
              {{ job.description || '-' }}
            </td>
            <td>
              <span :class="job.enabled ? 'status-active' : 'status-inactive'">
                {{ job.enabled ? '啟用' : '停用' }}
              </span>
            </td>
            <td>
              <div v-if="jobExecutions[job.id]">
                <span :class="getStatusClass(jobExecutions[job.id].status)">
                  {{ getStatusText(jobExecutions[job.id].status) }}
                </span>
                <button 
                  class="btn-sm btn-view" 
                  @click="viewExecutionHistory(job.id)"
                  style="margin-left: 0.5rem;"
                >
                  查看記錄
                </button>
              </div>
              <span v-else style="color: #94a3b8;">未執行</span>
            </td>
            <td class="actions">
              <button class="btn-sm btn-execute" @click="executeJob(job.id)" :disabled="executingJobId === job.id">
                {{ executingJobId === job.id ? '執行中...' : '立即執行' }}
              </button>
              <button class="btn-sm btn-toggle" @click="toggleJob(job.id, !job.enabled)">
                {{ job.enabled ? '停用' : '啟用' }}
              </button>
              <button class="btn-sm btn-edit" @click="editJob(job)">編輯</button>
              <button class="btn-sm btn-delete" @click="deleteJob(job.id)">刪除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </main>

    <!-- 執行記錄模態框 -->
    <div v-if="showExecutionModal" class="modal-overlay" @click="showExecutionModal = false">
      <div class="modal-panel execution-modal-content" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">執行記錄 - {{ selectedJobName }}</h2>
          <button class="btn-close" @click="showExecutionModal = false">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div v-if="executionHistory.length === 0" style="text-align: center; padding: 2rem; color: #94a3b8;">
            尚無執行記錄
          </div>
          <div v-else class="execution-table-container">
            <table class="execution-table">
              <thead>
                <tr>
                  <th class="execution-time-col">執行時間</th>
                  <th class="execution-status-col">狀態</th>
                  <th class="execution-time-col">開始時間</th>
                  <th class="execution-time-col">完成時間</th>
                  <th class="execution-result-col">結果/錯誤</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="execution in executionHistory" :key="execution.id">
                  <td>{{ formatDateTime(execution.createdAt) }}</td>
                  <td>
                    <span :class="getStatusClass(execution.status)">
                      {{ getStatusText(execution.status) }}
                    </span>
                  </td>
                  <td>{{ execution.startedAt ? formatDateTime(execution.startedAt) : '-' }}</td>
                  <td>{{ execution.completedAt ? formatDateTime(execution.completedAt) : '-' }}</td>
                  <td>
                    <div v-if="execution.resultMessage" class="result-message">
                      {{ execution.resultMessage }}
                    </div>
                    <div v-if="execution.errorMessage" class="error-message">
                      {{ execution.errorMessage }}
                    </div>
                    <span v-if="!execution.resultMessage && !execution.errorMessage" style="color: #94a3b8;">-</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="showExecutionModal = false">關閉</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/編輯模態框 -->
    <div v-if="showAddModal || editingJob" class="modal-overlay" @click="closeModal">
      <div class="modal-panel" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">{{ editingJob ? '編輯定時任務' : '新增定時任務' }}</h2>
          <button class="btn-close" @click="closeModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit" class="form-container">
            <div class="form-group">
              <label class="form-label">任務名稱 <span class="text-danger">*</span></label>
              <input v-model="form.jobName" class="form-input" placeholder="例如：週二服事人員通知" required />
            </div>
            <div class="form-group">
              <label class="form-label">任務類別（完整類名） <span class="text-danger">*</span></label>
              <input v-model="form.jobClass" class="form-input" placeholder="例如：com.example.helloworld.scheduler.personal.DatabaseBackupScheduler$DatabaseBackupJob" required />
              <small class="form-hint">
                必須是已註冊的 Job 執行器類別
              </small>
            </div>
            <div class="form-group">
              <label class="form-label">Cron 表達式 <span class="text-danger">*</span></label>
              <input v-model="form.cronExpression" class="form-input" placeholder="例如：0 0 10 ? * TUE" required />
              <small class="form-hint">
                格式：秒 分 時 日 月 星期<br>
                範例：0 0 10 ? * TUE (每週二 10:00) | 0 0 7 * * ? (每天 7:00) | 0 0 12 * * MON-FRI (週一到週五 12:00)
              </small>
            </div>
            <div class="form-group">
              <label class="form-label">描述</label>
              <textarea v-model="form.description" rows="3" class="form-input" placeholder="任務描述..."></textarea>
            </div>
            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="form.enabled" class="checkbox-input" />
                啟用此任務
              </label>
            </div>
            <div class="form-actions">
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <i class="fas fa-save me-2"></i>
                {{ saving ? '儲存中...' : '儲存' }}
              </button>
              <button type="button" class="btn btn-secondary" @click="closeModal">
                <i class="fas fa-times me-2"></i>取消
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import TopNavbar from '@/components/TopNavbar.vue'
import { apiService } from '@/composables/useApi'

const jobs = ref([])
const loading = ref(false)
const saving = ref(false)
const executingJobId = ref(null)
const showAddModal = ref(false)
const editingJob = ref(null)
const jobExecutions = ref({})
const showExecutionModal = ref(false)
const executionHistory = ref([])
const selectedJobName = ref('')
const pollingIntervals = ref({})

const form = ref({
  jobName: '',
  jobClass: '',
  cronExpression: '',
  description: '',
  enabled: true
})

const loadJobs = async () => {
  loading.value = true
  try {
    jobs.value = await apiService.getScheduledJobs()
    
    // 載入每個任務的最新執行狀態
    for (const job of jobs.value) {
      await loadLatestExecution(job.id)
    }
  } catch (error) {
    console.error('載入任務失敗:', error)
    alert('載入任務失敗: ' + error.message)
  } finally {
    loading.value = false
  }
}

const loadLatestExecution = async (jobId) => {
  try {
    const execution = await apiService.getLatestJobExecution(jobId)
    
    if (execution) {
      jobExecutions.value[jobId] = execution
      // 如果正在執行，開始輪詢
      if (execution.status === 'RUNNING' || execution.status === 'PENDING') {
        startPolling(jobId)
      }
    } else {
      jobExecutions.value[jobId] = null
    }
  } catch (error) {
    // 如果是 404 錯誤（沒有執行記錄），這是正常的
    if (error.message && (error.message.includes('404') || error.message.includes('Not Found'))) {
      jobExecutions.value[jobId] = null
    } else {
      console.error('載入執行狀態失敗:', error)
    }
  }
}

const startPolling = (jobId) => {
  // 清除舊的輪詢
  if (pollingIntervals.value[jobId]) {
    clearInterval(pollingIntervals.value[jobId])
  }
  
  // 開始新的輪詢（每2秒檢查一次）
  pollingIntervals.value[jobId] = setInterval(async () => {
    try {
      const execution = await apiService.getLatestJobExecution(jobId)
      
      if (execution) {
        jobExecutions.value[jobId] = execution
        // 如果執行完成，停止輪詢
        if (execution.status === 'SUCCESS' || execution.status === 'FAILED') {
          stopPolling(jobId)
        }
      } else {
        jobExecutions.value[jobId] = null
      }
    } catch (error) {
      // 如果是 404 錯誤，停止輪詢
      if (error.message && (error.message.includes('404') || error.message.includes('Not Found'))) {
        jobExecutions.value[jobId] = null
        stopPolling(jobId)
      } else {
        console.error('輪詢執行狀態失敗:', error)
      }
    }
  }, 2000)
}

const stopPolling = (jobId) => {
  if (pollingIntervals.value[jobId]) {
    clearInterval(pollingIntervals.value[jobId])
    delete pollingIntervals.value[jobId]
  }
}

const editJob = (job) => {
  editingJob.value = job
  form.value = {
    jobName: job.jobName,
    jobClass: job.jobClass,
    cronExpression: job.cronExpression,
    description: job.description || '',
    enabled: job.enabled
  }
  showAddModal.value = true
}

const closeModal = () => {
  showAddModal.value = false
  editingJob.value = null
  form.value = {
    jobName: '',
    jobClass: '',
    cronExpression: '',
    description: '',
    enabled: true
  }
}

const handleSubmit = async () => {
  saving.value = true
  try {
    if (editingJob.value) {
      await apiService.updateScheduledJob(editingJob.value.id, form.value)
    } else {
      await apiService.createScheduledJob(form.value)
    }
    
    await loadJobs()
    closeModal()
  } catch (error) {
    console.error('儲存任務失敗:', error)
    alert('儲存失敗: ' + error.message)
  } finally {
    saving.value = false
  }
}

const deleteJob = async (id) => {
  if (!confirm('確定要刪除此任務嗎？')) {
    return
  }
  
  try {
    await apiService.deleteScheduledJob(id)
    await loadJobs()
  } catch (error) {
    console.error('刪除任務失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

const toggleJob = async (id, enabled) => {
  try {
    await apiService.toggleScheduledJob(id, enabled)
    await loadJobs()
  } catch (error) {
    console.error('切換任務狀態失敗:', error)
    alert('切換狀態失敗: ' + error.message)
  }
}

const executeJob = async (id) => {
  executingJobId.value = id
  try {
    const response = await apiService.executeScheduledJob(id)
    
    alert(response?.message || '任務已開始執行')
    // 重新載入執行狀態
    await loadLatestExecution(id)
    // 開始輪詢
    startPolling(id)
  } catch (error) {
    console.error('執行任務失敗:', error)
    alert('執行失敗: ' + (error.message || '未知錯誤'))
  } finally {
    executingJobId.value = null
  }
}

const viewExecutionHistory = async (jobId) => {
  const job = jobs.value.find(j => j.id === jobId)
  selectedJobName.value = job ? job.jobName : '未知任務'
  showExecutionModal.value = true
  
  try {
    executionHistory.value = await apiService.getJobExecutions(jobId)
  } catch (error) {
    console.error('載入執行記錄失敗:', error)
    alert('載入執行記錄失敗: ' + error.message)
    executionHistory.value = []
  }
}

const getStatusClass = (status) => {
  switch (status) {
    case 'SUCCESS':
      return 'status-success'
    case 'FAILED':
      return 'status-failed'
    case 'RUNNING':
      return 'status-running'
    case 'PENDING':
      return 'status-pending'
    default:
      return 'status-inactive'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'SUCCESS':
      return '執行成功'
    case 'FAILED':
      return '執行失敗'
    case 'RUNNING':
      return '執行中'
    case 'PENDING':
      return '等待執行'
    default:
      return status
  }
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const formatCronExpression = (cronExpr) => {
  if (!cronExpr) return '-'
  
  // 移除多餘的空格並分割
  const parts = cronExpr.trim().split(/\s+/)
  if (parts.length < 6) {
    return cronExpr // 如果格式不對，直接返回原值
  }
  
  const [second, minute, hour, day, month, weekday] = parts
  
  // 星期映射
  const weekdayMap = {
    'SUN': '週日',
    'MON': '週一',
    'TUE': '週二',
    'WED': '週三',
    'THU': '週四',
    'FRI': '週五',
    'SAT': '週六',
    '0': '週日',
    '1': '週一',
    '2': '週二',
    '3': '週三',
    '4': '週四',
    '5': '週五',
    '6': '週六',
    '7': '週日'
  }
  
  // 月份映射
  const monthMap = {
    'JAN': '1月',
    'FEB': '2月',
    'MAR': '3月',
    'APR': '4月',
    'MAY': '5月',
    'JUN': '6月',
    'JUL': '7月',
    'AUG': '8月',
    'SEP': '9月',
    'OCT': '10月',
    'NOV': '11月',
    'DEC': '12月'
  }
  
  // 解析時間
  const timeStr = `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`
  
  // 解析星期
  let weekdayStr = ''
  if (weekday === '?' || weekday === '*') {
    weekdayStr = ''
  } else if (weekday.includes(',')) {
    const weekdays = weekday.split(',').map(w => weekdayMap[w.trim()] || w.trim()).join('、')
    weekdayStr = weekdays
  } else if (weekday.includes('-')) {
    const [start, end] = weekday.split('-')
    weekdayStr = `${weekdayMap[start.trim()] || start.trim()}到${weekdayMap[end.trim()] || end.trim()}`
  } else {
    weekdayStr = weekdayMap[weekday] || weekday
  }
  
  // 解析日期
  let dayStr = ''
  if (day === '?' || day === '*') {
    dayStr = ''
  } else if (day.includes(',')) {
    dayStr = `每月${day.split(',').join('、')}日`
  } else if (day.includes('-')) {
    const [start, end] = day.split('-')
    dayStr = `每月${start}日到${end}日`
  } else if (day === 'L') {
    dayStr = '每月最後一天'
  } else if (day.startsWith('L-')) {
    const dayNum = day.substring(2)
    dayStr = `每月倒數第${dayNum}天`
  } else {
    dayStr = `每月${day}日`
  }
  
  // 解析月份
  let monthStr = ''
  if (month === '*') {
    monthStr = ''
  } else if (month.includes(',')) {
    const months = month.split(',').map(m => monthMap[m.trim()] || m.trim()).join('、')
    monthStr = months
  } else if (month.includes('-')) {
    const [start, end] = month.split('-')
    monthStr = `${monthMap[start.trim()] || start.trim()}到${monthMap[end.trim()] || end.trim()}`
  } else {
    monthStr = monthMap[month] || month
  }
  
  // 組合描述
  let description = ''
  
  // 每天特定時間
  if (day === '*' && month === '*' && (weekday === '*' || weekday === '?')) {
    if (second === '0' && minute === '0') {
      description = `每天 ${timeStr}`
    } else {
      description = `每天 ${timeStr}:${second.padStart(2, '0')}`
    }
  }
  // 每週特定時間
  else if (day === '?' && month === '*' && weekday !== '*' && weekday !== '?') {
    if (weekdayStr.includes('、')) {
      description = `每${weekdayStr} ${timeStr}`
    } else {
      description = `${weekdayStr} ${timeStr}`
    }
  }
  // 每月特定日期和時間
  else if (day !== '*' && day !== '?' && month === '*' && (weekday === '*' || weekday === '?')) {
    description = `${dayStr} ${timeStr}`
  }
  // 特定月份
  else if (month !== '*') {
    description = `${monthStr}${dayStr ? ' ' + dayStr : ''}${weekdayStr ? ' ' + weekdayStr : ''} ${timeStr}`
  }
  // 其他複雜情況
  else {
    const parts = []
    if (monthStr) parts.push(monthStr)
    if (dayStr) parts.push(dayStr)
    if (weekdayStr) parts.push(weekdayStr)
    parts.push(timeStr)
    description = parts.join(' ')
  }
  
  return description || cronExpr
}

onMounted(() => {
  loadJobs()
})

// 組件卸載時清除所有輪詢
onUnmounted(() => {
  Object.keys(pollingIntervals.value).forEach(jobId => {
    stopPolling(jobId)
  })
})
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 2rem;
}

.header {
  background: white;
  padding: 2rem;
  margin-bottom: 2rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.header-top {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h1 {
  font-size: 1.8rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 2rem;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  table-layout: fixed;
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
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #374151;
}

.data-table th:nth-child(1) { width: 5%; }
.data-table th:nth-child(2) { width: 20%; }
.data-table th:nth-child(3) { width: 20%; }
.data-table th:nth-child(4) { width: 20%; }
.data-table th:nth-child(5) { width: 8%; }
.data-table th:nth-child(6) { width: 12%; }
.data-table th:nth-child(7) { width: 15%; }

.data-table td {
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.data-table td:nth-child(4) {
  max-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  position: relative;
  cursor: help;
}

.data-table td:nth-child(4):hover {
  white-space: normal;
  overflow: visible;
  z-index: 10;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  padding: 1rem;
  border-radius: 0.5rem;
  max-width: 400px;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.btn {
  padding: 0.6rem 1.2rem;
  border-radius: 6px;
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
  background: #fff;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background: #f9fafb;
  color: #111827;
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

.btn-execute {
  background: #10b981;
}

.btn-execute:hover:not(:disabled) {
  background: #059669;
}

.btn-toggle {
  background: #f59e0b;
}

.btn-toggle:hover {
  background: #d97706;
}

.btn-view {
  background: #3b82f6;
}

.btn-view:hover {
  background: #2563eb;
}

.btn:disabled, .btn-sm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.status-active, .status-success { color: #10b981; font-weight: 600; }
.status-inactive, .status-failed { color: #ef4444; font-weight: 600; }
.status-running { color: #f59e0b; font-weight: 600; animation: pulse 2s infinite; }
.status-pending { color: #6b7280; font-weight: 600; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-panel {
  width: 100%;
  max-width: 600px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  margin: 2rem;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s;
}

.execution-modal-content {
  max-width: 1200px !important;
  width: 90% !important;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.btn-close {
  background: transparent;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  background: #f3f4f6;
  color: #111827;
}

.modal-body {
  padding: 1.5rem;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-group {
  margin-bottom: 0;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
  font-size: 0.9rem;
}

.form-input {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
  transition: all 0.2s;
  background: white;
  color: #1f2937;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.checkbox-group {
  display: flex;
  align-items: center;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-weight: 500;
  color: #374151;
  gap: 0.5rem;
}

.checkbox-input {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
  accent-color: #667eea;
}

.form-hint {
  display: block;
  margin-top: 0.25rem;
  color: #6b7280;
  font-size: 0.85rem;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid #e5e7eb;
  justify-content: flex-end;
}

.text-danger { color: #ef4444; }
.me-2 { margin-right: 0.5rem; }

/* 執行記錄樣式 */
.execution-table-container {
  max-height: 500px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.execution-table {
  width: 100%;
  border-collapse: collapse;
}

.execution-table th, .execution-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.execution-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
  position: sticky;
  top: 0;
}

.result-message {
  color: #059669;
  background: #ecfdf5;
  padding: 0.5rem;
  border-radius: 4px;
  font-family: monospace;
  white-space: pre-wrap;
}

.error-message {
  color: #dc2626;
  background: #fef2f2;
  padding: 0.5rem;
  border-radius: 4px;
  font-family: monospace;
  white-space: pre-wrap;
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
