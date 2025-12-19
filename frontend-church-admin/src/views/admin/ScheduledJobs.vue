<template>
  <AdminLayout>
    <div class="admin-page">
      <header class="header">
        <div class="header-top">
          <h1>⏰ 定時任務管理</h1>
          <button class="btn btn-primary" @click="showAddModal = true">新增任務</button>
        </div>
      </header>

      <main class="main-content">
        <div class="card">
          <div class="card__body">
            <div class="table-wrap">
              <table class="table">
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
            </div>
          </div>
        </div>
      </main>

      <!-- 執行記錄模態框 -->
      <div v-if="showExecutionModal" class="modal-overlay" @click="showExecutionModal = false">
        <div class="modal-card" @click.stop>
          <h2>執行記錄 - {{ selectedJobName }}</h2>
          <div v-if="executionHistory.length === 0" style="text-align: center; padding: 2rem; color: #94a3b8;">
            尚無執行記錄
          </div>
          <div v-else class="table-wrap">
            <table class="table">
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
          <div class="modal-foot">
            <button type="button" class="btn btn-secondary" @click="showExecutionModal = false">關閉</button>
          </div>
        </div>
      </div>

      <!-- 新增/編輯模態框 -->
	      <div v-if="showAddModal || editingJob" class="modal-overlay" @click="closeModal">
	        <div class="modal-card" @click.stop>
	          <div class="modal-head">
	            <div>
	              <div class="modal-title">{{ editingJob ? '編輯定時任務' : '新增定時任務' }}</div>
	              <div class="modal-sub">設定任務名稱、Job 類別與 Cron 表達式。</div>
	            </div>
	            <button class="modal-close" type="button" @click="closeModal">✕</button>
	          </div>
	          <div class="modal-body">
          <form @submit.prevent="handleSubmit">
            <div class="form-group">
              <label>任務名稱 <span class="required">*</span></label>
              <input v-model="form.jobName" placeholder="例如：週二服事人員通知" required />
            </div>
            <div class="form-group">
              <label>任務類別（完整類名） <span class="required">*</span></label>
              <input v-model="form.jobClass" placeholder="例如：com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler$WeeklyServiceNotificationJob" required />
              <small style="color: #666; font-size: 0.875rem; margin-top: 0.25rem; display: block;">
                必須是已註冊的 Job 執行器類別
              </small>
            </div>
            <div class="form-group">
              <label>Cron 表達式 <span class="required">*</span></label>
              <input v-model="form.cronExpression" placeholder="例如：0 0 10 ? * TUE" required />
              <small style="color: #666; font-size: 0.875rem; margin-top: 0.25rem; display: block;">
                格式：秒 分 時 日 月 星期<br>
                範例：0 0 10 ? * TUE (每週二 10:00) | 0 0 7 * * ? (每天 7:00) | 0 0 12 * * MON-FRI (週一到週五 12:00)
              </small>
            </div>
            <div class="form-group">
              <label>描述</label>
              <textarea v-model="form.description" rows="3" placeholder="任務描述..."></textarea>
            </div>
            <div class="form-group">
              <label>
                <input type="checkbox" v-model="form.enabled" />
                啟用此任務
              </label>
            </div>
            <div class="modal-foot">
              <button type="submit" class="btn btn-primary" :disabled="saving">
                {{ saving ? '儲存中...' : '儲存' }}
              </button>
              <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
            </div>
	          </form>
	          </div>
	        </div>
	      </div>
	    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@/composables/useToast'
import { ref, onMounted, onUnmounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { apiRequest } from '@/utils/api'

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
    const response = await apiRequest('/church/scheduled-jobs', {
      method: 'GET'
    }, '載入任務中...', true)
    
    if (response.ok) {
      jobs.value = await response.json()
      // 載入每個任務的最新執行狀態
      for (const job of jobs.value) {
        await loadLatestExecution(job.id)
      }
    } else {
      throw new Error('載入任務失敗')
    }
  } catch (error) {
    console.error('載入任務失敗:', error)
    toast.error('載入任務失敗: ' + error.message)
  } finally {
    loading.value = false
  }
}

const loadLatestExecution = async (jobId) => {
  try {
    const response = await apiRequest(`/church/scheduled-jobs/${jobId}/executions/latest`, {
      method: 'GET'
    }, '', true)
    
    if (response.ok) {
      const execution = await response.json()
      if (execution) {
        jobExecutions.value[jobId] = execution
        // 如果正在執行，開始輪詢
        if (execution.status === 'RUNNING' || execution.status === 'PENDING') {
          startPolling(jobId)
        }
      } else {
        jobExecutions.value[jobId] = null
      }
    } else if (response.status === 404) {
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
      const response = await apiRequest(`/church/scheduled-jobs/${jobId}/executions/latest`, {
        method: 'GET'
      }, '', true)
      
      if (response.ok) {
        const execution = await response.json()
        if (execution) {
          jobExecutions.value[jobId] = execution
          // 如果執行完成，停止輪詢
          if (execution.status === 'SUCCESS' || execution.status === 'FAILED') {
            stopPolling(jobId)
          }
        } else {
          jobExecutions.value[jobId] = null
        }
      } else if (response.status === 404) {
        jobExecutions.value[jobId] = null
        stopPolling(jobId)
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
    const response = editingJob.value
      ? await apiRequest(`/church/scheduled-jobs/${editingJob.value.id}`, {
          method: 'PUT',
          body: JSON.stringify(form.value)
        }, '更新中...', true)
      : await apiRequest('/church/scheduled-jobs', {
          method: 'POST',
          body: JSON.stringify(form.value)
        }, '建立中...', true)
    
    if (response.ok) {
      await loadJobs()
      closeModal()
    } else {
      const errorText = await response.text()
      throw new Error(errorText || '儲存失敗')
    }
  } catch (error) {
    console.error('儲存任務失敗:', error)
    toast.error('儲存失敗: ' + error.message)
  } finally {
    saving.value = false
  }
}

const deleteJob = async (id) => {
  if (!confirm('確定要刪除此任務嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/scheduled-jobs/${id}`, {
      method: 'DELETE'
    }, '刪除中...', true)
    
    if (response.ok) {
      await loadJobs()
    } else {
      throw new Error('刪除失敗')
    }
  } catch (error) {
    console.error('刪除任務失敗:', error)
    toast.error('刪除失敗: ' + error.message)
  }
}

const toggleJob = async (id, enabled) => {
  try {
    const response = await apiRequest(`/church/scheduled-jobs/${id}/toggle?enabled=${enabled}`, {
      method: 'PUT'
    }, '更新中...', true)
    
    if (response.ok) {
      await loadJobs()
    } else {
      throw new Error('切換狀態失敗')
    }
  } catch (error) {
    console.error('切換任務狀態失敗:', error)
    toast.error('切換狀態失敗: ' + error.message)
  }
}

const executeJob = async (id) => {
  executingJobId.value = id
  try {
    const response = await apiRequest(`/church/scheduled-jobs/${id}/execute`, {
      method: 'POST'
    }, '執行中...', true)
    
    if (response.ok) {
      const result = await response.json()
      toast.success(result?.message || '任務已開始執行')
      // 重新載入執行狀態
      await loadLatestExecution(id)
      // 開始輪詢
      startPolling(id)
    } else {
      const errorText = await response.text()
      throw new Error(errorText || '執行失敗')
    }
  } catch (error) {
    console.error('執行任務失敗:', error)
    toast.error('執行失敗: ' + (error.message || '未知錯誤'))
  } finally {
    executingJobId.value = null
  }
}

const viewExecutionHistory = async (jobId) => {
  const job = jobs.value.find(j => j.id === jobId)
  selectedJobName.value = job ? job.jobName : '未知任務'
  showExecutionModal.value = true
  
  try {
    const response = await apiRequest(`/church/scheduled-jobs/${jobId}/executions`, {
      method: 'GET'
    }, '載入記錄中...', true)
    
    if (response.ok) {
      executionHistory.value = await response.json()
    } else {
      throw new Error('載入執行記錄失敗')
    }
  } catch (error) {
    console.error('載入執行記錄失敗:', error)
    toast.error('載入執行記錄失敗: ' + error.message)
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
.admin-page{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-page .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-page .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-page .page-header p,
.admin-page .subtitle,
.admin-page .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-page .table-container,
.admin-page .list-container,
.admin-page .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-page .table-container{ padding:0; }

/* Inline helpers */
.admin-page .hint,
.admin-page .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-page .actions,
.admin-page .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
