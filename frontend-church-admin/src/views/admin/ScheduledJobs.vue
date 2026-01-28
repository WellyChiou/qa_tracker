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
              <th class="col-id">ID</th>
              <th class="col-name">任務名稱</th>
              <th class="col-cron">Cron 表達式</th>
              <th class="col-desc">描述</th>
              <th class="col-enabled">啟用狀態</th>
              <th class="col-exec">執行狀態</th>
              <th class="col-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading" class="skeleton-row">
              <td colspan="7">
                <div class="skeleton" style="height:14px; width:42%; margin:6px auto;"></div>
              </td>
            </tr>
            <tr v-else-if="jobs.length === 0">
              <td colspan="7" style="text-align: center; padding: 20px;">尚無任務</td>
            </tr>
            <tr v-for="job in jobs" :key="job.id" >
              <td class="col-id">{{ job.id }}</td>
              <td class="col-name">{{ job.jobName }}</td>
              <td class="col-cron">
                <div class="cron-wrap">
                  <span class="badge badge--cron" :title="cronTooltip(job)">
                    {{ formatCronExpression(job.cronExpression) }}
                  </span>
                  <code class="cron-code" :title="job.cronExpression">{{ job.cronExpression }}</code>
                </div>
              </td>
              <td class="col-desc">
                <TruncatedText :text="job.description" />
              </td>
              <td class="col-enabled">
                <span :class="job.enabled ? 'status-active' : 'status-inactive'">
                  {{ job.enabled ? '啟用' : '停用' }}
                </span>
              </td>
              <td class="col-exec">
                <div v-if="jobExecutions[job.id]" class="exec-cell">
                  <span class="status-icon-only" :class="getStatusClass(jobExecutions[job.id].status)" :title="getStatusText(jobExecutions[job.id].status)">
                    <svg v-if="jobExecutions[job.id].status === 'SUCCESS'" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5"/></svg>
                    <svg v-else-if="jobExecutions[job.id].status === 'FAILED'" viewBox="0 0 24 24"><path d="M12 9v4m0 4h.01"/><path d="M10.29 3.86l-8.17 14.14A2 2 0 0 0 3.83 21h16.34a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/></svg>
                    <svg v-else-if="jobExecutions[job.id].status === 'CANCELLED'" viewBox="0 0 24 24"><path d="M18 6L6 18M6 6l12 12"/></svg>
                    <svg v-else-if="jobExecutions[job.id].status === 'RUNNING'" viewBox="0 0 24 24"><path d="M12 2a10 10 0 1 0 10 10"/><path d="M12 6v6l4 2"/></svg>
                    <svg v-else viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                  </span>
                  <button 
                    class="btn-sm btn-view" 
                    @click="viewExecutionHistory(job.id)"

                  >
                    查看記錄
                  </button>
                </div>
                <div v-else class="exec-cell exec-cell--empty">
                  <span class="status-icon-only status--none" title="未執行"><svg viewBox="0 0 24 24"><path d="M5 12h14"/></svg></span>
                </div>
              </td>
              <td class="col-actions actions">
                <div class="action-grid">
<button class="btn-sm btn-execute" @click="onExecute(job.id)" :disabled="executingJobId === job.id || !isActionAllowed(job.id, 'execute')">
                  <span v-if="executingJobId === job.id" class="spinner" aria-hidden="true"></span>
                  {{ executingJobId === job.id ? '執行中...' : '立即執行' }}
                </button>
                <button class="btn-sm btn-toggle" @click="onToggle(job.id, !job.enabled)" :disabled="!isActionAllowed(job.id, 'toggle')">
                  {{ job.enabled ? '停用' : '啟用' }}
                </button>
                <button class="btn-sm btn-edit" @click="editJob(job)"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                <button class="btn-sm btn-delete" @click="deleteJob(job.id)"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button>
                </div>
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
  <div class="modal-card modal-card--wide" @click.stop>
    <div class="modal-head">
      <div>
        <div class="modal-title">執行記錄</div>
        <div class="modal-sub">
          {{ selectedJobName }} <span class="dot">•</span> 共 {{ executionHistory.length }} 筆
        </div>
      </div>
      <button class="modal-close" type="button" @click="showExecutionModal = false">✕</button>
    </div>

    <div class="modal-body">
      <div v-if="executionHistory.length === 0" class="empty-state">
        尚無執行記錄
      </div>

      <div v-else class="table-wrap">
        <table class="table exec-table">
          <thead>
            <tr>
              <th class="col-created">執行時間</th>
              <th class="col-status">狀態</th>
              <th class="col-start">開始時間</th>
              <th class="col-end">完成時間</th>
              <th class="col-msg">結果 / 錯誤</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="execution in executionHistory" :key="execution.id">
              <td class="mono">{{ formatDateTime(execution.createdAt) }}</td>
              <td>
                <span class="status-pill" :class="getStatusClass(execution.status)">
                  <span class="status-icon" aria-hidden="true">
                    <svg v-if="execution.status === 'SUCCESS'" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5"/></svg>
                    <svg v-else-if="execution.status === 'FAILED'" viewBox="0 0 24 24"><path d="M18 6L6 18M6 6l12 12"/></svg>
                    <svg v-else-if="execution.status === 'RUNNING'" viewBox="0 0 24 24"><path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83"/></svg>
                    <svg v-else viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                  </span>
                  {{ getStatusText(execution.status) }}
                </span>
              </td>
              <td class="mono">{{ execution.startedAt ? formatDateTime(execution.startedAt) : '-' }}</td>
              <td class="mono">{{ execution.completedAt ? formatDateTime(execution.completedAt) : '-' }}</td>
              <td>
                <div v-if="execution.resultMessage" class="msg msg--ok">
                  {{ execution.resultMessage }}
                </div>
                <div v-if="execution.errorMessage" class="msg msg--err">
                  {{ execution.errorMessage }}
                </div>
                <span v-if="!execution.resultMessage && !execution.errorMessage" class="muted">-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
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
              <input class="input" v-model="form.jobName" placeholder="例如：週二服事人員通知" required />
            </div>
            <div class="form-group">
              <label>任務類別（完整類名） <span class="required">*</span></label>
              <input class="input mono-input" v-model="form.jobClass" placeholder="例如：com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler$WeeklyServiceNotificationJob" required />
              <small class="help">
                必須是已註冊的 Job 執行器類別
              </small>
            </div>
            <div class="form-group">
              <label>Cron 表達式 <span class="required">*</span></label>
              <input class="input mono-input" v-model="form.cronExpression" placeholder="例如：0 0 10 ? * TUE" required />
              <small class="help">
                格式：秒 分 時 日 月 星期<br>
                範例：0 0 10 ? * TUE (每週二 10:00) | 0 0 7 * * ? (每天 7:00) | 0 0 12 * * MON-FRI (週一到週五 12:00)
              </small>
            </div>
            <div class="form-group">
              <label>描述</label>
              <textarea class="textarea" v-model="form.description" rows="3" placeholder="任務描述..."></textarea>
            </div>
            <div class="form-group">
              <label class="check-row">
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
import { toast } from '@shared/composables/useToast'
import { ref, onMounted, onUnmounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import TruncatedText from '@/components/TruncatedText.vue'
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

// UI helpers
const actionThrottle = ref({}) // prevent double click spam


const isActionAllowed = (jobId, actionKey, cooldownMs = 800) => {
  const key = `${jobId}:${actionKey}`
  const now = Date.now()
  const last = actionThrottle.value[key] || 0
  return now - last >= cooldownMs
}

const markAction = (jobId, actionKey) => {
  const key = `${jobId}:${actionKey}`
  actionThrottle.value[key] = Date.now()
}

const onExecute = (jobId) => {
  if (!isActionAllowed(jobId, 'execute')) return
  markAction(jobId, 'execute')
  executeJob(jobId)
}

const onToggle = (jobId, nextEnabled) => {
  if (!isActionAllowed(jobId, 'toggle')) return
  markAction(jobId, 'toggle')
  toggleJob(jobId, nextEnabled)
}

const cronTooltip = (job) => {
  const pretty = formatCronExpression(job.cronExpression)
  return `${pretty}\n${job.cronExpression}`
}

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
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/scheduled-jobs', {
      method: 'GET'
    })
    
    if (data) {
      jobs.value = Array.isArray(data) ? data : (data.jobs || [])
      // 載入每個任務的最新執行狀態
      for (const job of jobs.value) {
        await loadLatestExecution(job.id)
      }
      toast.success(`載入成功，共 ${jobs.value.length} 個任務`)
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
    // apiRequest 現在會自動返回解析後的資料
    const execution = await apiRequest(`/church/scheduled-jobs/${jobId}/executions/latest`, {
      method: 'GET'
    })
    
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
      // apiRequest 現在會自動返回解析後的資料
      const execution = await apiRequest(`/church/scheduled-jobs/${jobId}/executions/latest`, {
        method: 'GET'
      })
      
      if (execution) {
        jobExecutions.value[jobId] = execution
        // 如果執行完成，停止輪詢
        if (execution.status === 'SUCCESS' || execution.status === 'FAILED') {
          stopPolling(jobId)
        }
      } else {
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
    // apiRequest 現在會自動返回解析後的資料
    const data = editingJob.value
      ? await apiRequest(`/church/scheduled-jobs/${editingJob.value.id}`, {
          method: 'PUT',
          body: JSON.stringify(form.value)
        })
      : await apiRequest('/church/scheduled-jobs', {
          method: 'POST',
          body: JSON.stringify(form.value)
        })
    
    if (data !== null) {
      await loadJobs()
      closeModal()
    } else {
      throw new Error('儲存失敗')
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
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/scheduled-jobs/${id}`, {
      method: 'DELETE'
    })
    
    if (data !== null) {
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
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/scheduled-jobs/${id}/toggle?enabled=${enabled}`, {
      method: 'PUT'
    })
    
    if (data !== null) {
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
    // apiRequest 現在會自動返回解析後的資料
    const result = await apiRequest(`/church/scheduled-jobs/${id}/execute`, {
      method: 'POST'
    })
    
    if (result !== null) {
      toast.success(result?.message || '任務已開始執行')
      // 重新載入執行狀態
      await loadLatestExecution(id)
      // 開始輪詢
      startPolling(id)
    } else {
      throw new Error('執行失敗')
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
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/scheduled-jobs/${jobId}/executions`, {
      method: 'GET'
    })
    
    if (data) {
      executionHistory.value = Array.isArray(data) ? data : (data.executions || [])
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
  overflow-x:auto;
  overflow-y:visible;
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


/* --- Scheduled jobs: description clamp & nicer execution modal --- */

/* Table layout + column widths (desktop-first) */
:deep(.table){ table-layout: fixed; width: 100%; }

/* Fixed columns (th/td 100% sync) - 參考個人網站，使用百分比寬度 */
:deep(.table th.col-id), :deep(.table td.col-id){ width: 5%; min-width: 60px; text-align: left; }
:deep(.table th.col-name), :deep(.table td.col-name){ width: 15%; min-width: 150px; }
:deep(.table th.col-cron), :deep(.table td.col-cron){ width: 15%; min-width: 170px; }
:deep(.table th.col-desc), :deep(.table td.col-desc){ width: 24%; min-width: 250px; }
:deep(.table th.col-enabled), :deep(.table td.col-enabled){ width: 8%; min-width: 100px; text-align: center; }
:deep(.table th.col-exec), :deep(.table td.col-exec){ width: 18%; min-width: 180px; text-align: center; }
:deep(.table th.col-actions), :deep(.table td.col-actions){ width: 15%; min-width: 220px; }

/* 描述欄位樣式已移至 TruncatedText 元件 */

/* Actions: two buttons per row */
.action-grid{
  display:grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 10px;
}
.action-grid .btn-sm{ width:100%; white-space:nowrap; }
@media (max-width: 640px){
  .action-grid{ grid-template-columns: 1fr; }
}

.exec-cell{ display:flex; align-items:center; justify-content:center; gap:10px; }
.exec-cell--empty{ justify-content:center; }
.exec-cell .status-icon-only{ flex: 0 0 auto; }

/* Responsive width tuning */
@media (max-width: 900px){
  :deep(.table th.col-id), :deep(.table td.col-id){ width: 56px; min-width: 56px; }
  :deep(.table th.col-name), :deep(.table td.col-name){ width: 150px; min-width: 150px; }
  :deep(.table th.col-cron), :deep(.table td.col-cron){ width: 180px; min-width: 180px; }
  :deep(.table th.col-enabled), :deep(.table td.col-enabled){ width: 104px; min-width: 104px; }
  :deep(.table th.col-exec), :deep(.table td.col-exec){ width: 180px; min-width: 180px; }
  :deep(.table th.col-actions), :deep(.table td.col-actions){ width: 230px; min-width: 230px; }
  :deep(.table th.col-desc), :deep(.table td.col-desc){ min-width: 400px; }
}

@media (max-width: 640px){
  :deep(.table th.col-id), :deep(.table td.col-id){ width: 52px; min-width: 52px; }
  :deep(.table th.col-name), :deep(.table td.col-name){ width: 140px; min-width: 140px; }
  :deep(.table th.col-cron), :deep(.table td.col-cron){ width: 180px; min-width: 180px; }
  :deep(.table th.col-enabled), :deep(.table td.col-enabled){ width: 96px; min-width: 96px; }
  :deep(.table th.col-exec), :deep(.table td.col-exec){ width: 160px; min-width: 160px; }
  :deep(.table th.col-actions), :deep(.table td.col-actions){ width: 220px; min-width: 220px; }

  :deep(.table th.col-desc), :deep(.table td.col-desc){ min-width: 320px; }

  .exec-cell{ flex-direction: column; gap: 6px; }
  .exec-cell .btn-view{ width: 100%; padding: 6px 10px; }
}

/* Cron: badge + code */
.cron-wrap{ display:flex; flex-direction:column; gap:6px; }
.badge{
  display:inline-flex;
  align-items:center;
  padding:6px 10px;
  border-radius:999px;
  border:1px solid rgba(2,6,23,.10);
  background: rgba(2,6,23,.02);
  font-weight: 900;
  font-size: 13px;
  color: rgba(15,23,42,.92);
  width: fit-content;
}
.badge--cron{ border-color: rgba(59,130,246,.18); background: rgba(59,130,246,.06); }
.cron-code{
  font-size: 12px;
  color: rgba(15,23,42,.55);
  background: rgba(2,6,23,.03);
  padding: 3px 8px;
  border-radius: 10px;
  border: 1px solid rgba(2,6,23,.08);
  width: fit-content;
}

/* Status pill + icons */
.status-pill{
  display:inline-flex;
  align-items:center;
  gap:8px;
  padding:6px 10px;
  border-radius:999px;
  border:1px solid rgba(2,6,23,.10);
  background: rgba(2,6,23,.02);
  font-weight: 900;
  font-size: 13px;
}

.status-icon-only{
  display:inline-flex;
  align-items:center;
  justify-content:center;
  width:30px;
  height:30px;
  border-radius:999px;
  border:1px solid rgba(2,6,23,.12);
  background: rgba(2,6,23,.03);
}
.status-icon-only svg{
  width:18px;
  height:18px;
  fill:none;
  stroke: currentColor;
  stroke-width:2.2;
  stroke-linecap:round;
  stroke-linejoin:round;
}
.status-icon-only.status--none{
  color:#94a3b8;
}
.status-success{ color:#16a34a; border-color: rgba(22,163,74,.25); background: rgba(22,163,74,.10); }
.status-failed{ color:#ef4444; border-color: rgba(239,68,68,.25); background: rgba(239,68,68,.10); }
.status-running{ color:#2563eb; border-color: rgba(37,99,235,.25); background: rgba(37,99,235,.10); }
.status-pending{ color:#f59e0b; border-color: rgba(245,158,11,.25); background: rgba(245,158,11,.10); }
.status-inactive{ color:#64748b; border-color: rgba(100,116,139,.22); background: rgba(100,116,139,.08); }
.status-icon svg{ width:16px; height:16px; fill:none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }

/* Small spinner for execute */
.spinner{
  width: 14px;
  height: 14px;
  border-radius: 999px;
  border: 2px solid rgba(255,255,255,.35);
  border-top-color: rgba(255,255,255,.95);
  display:inline-block;
  animation: spin .9s linear infinite;
  margin-right: 8px;
}
@keyframes spin{ to{ transform: rotate(360deg); } }

.modal-card--wide{
  width:min(980px, calc(100vw - 36px));
}

.empty-state{
  text-align:center;
  padding: 28px 10px;
  color: var(--muted);
  font-weight: 800;
}

.exec-table{ width:100%; }
.exec-table th.col-created,
.exec-table th.col-start,
.exec-table th.col-end{
  min-width: 170px; min-width: 170px;
}
.exec-table th.col-status{ min-width: 96px; }
.exec-table th.col-msg{ min-width: 340px; }

.mono{
  font-variant-numeric: tabular-nums;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12.5px;
  color: rgba(15,23,42,.78);
}

.dot{ margin:0 6px; color: rgba(15,23,42,.35); }

.msg{
  border:1px solid rgba(2,6,23,.10);
  background: rgba(2,6,23,.02);
  border-radius: 12px;
  padding: 10px 12px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.4;
  font-size: 13px;
}
.msg + .msg{ margin-top: 8px; }
.msg--ok{
  border-color: rgba(34,197,94,.22);
  background: rgba(34,197,94,.06);
}
.msg--err{
  border-color: rgba(239,68,68,.22);
  background: rgba(239,68,68,.06);
}



/* --- Add/Edit modal form (套版) --- */
.form-group{ margin-bottom: 14px; }
.form-group label{
  display:flex;
  align-items:baseline;
  gap:8px;
  margin-bottom:8px;
  font-weight:900;
  color: rgba(15,23,42,.82);
  letter-spacing:-0.01em;
}
.required{ color:#ef4444; font-weight:900; }

.input, .textarea{
  width:100%;
  border:1px solid rgba(2,6,23,.14);
  background: rgba(255,255,255,.92);
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 14px;
  font-weight: 700;
  color: rgba(15,23,42,.92);
  transition: box-shadow .12s ease, border-color .12s ease, transform .12s ease;
}
.textarea{ resize: vertical; min-height: 92px; line-height: 1.45; }

.mono-input{
  font-variant-numeric: tabular-nums;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12.5px;
}

.help{
  display:block;
  margin-top:6px;
  color: rgba(15,23,42,.55);
  font-size: 12.5px;
  font-weight: 700;
  line-height: 1.45;
}

.check-row{
  display:flex;
  align-items:center;
  gap:10px;
  padding: 10px 12px;
  border: 1px dashed rgba(2,6,23,.14);
  border-radius: 14px;
  background: rgba(2,6,23,.02);
  font-weight: 800;
  color: rgba(15,23,42,.78);
}
.check-row input[type="checkbox"]{
  width: 18px;
  height: 18px;
  accent-color: rgba(37,99,235,.9);
}

/* Responsive tweaks */
@media (max-width: 1024px){
  :deep(.table th.col-actions), :deep(.table td.col-actions){ width: 300px; min-width: 300px; }
  :deep(.table th.col-exec), :deep(.table td.col-exec){ width: 160px; min-width: 160px; }
  :deep(.table th.col-enabled), :deep(.table td.col-enabled){ width: 130px; min-width: 130px; }
}
@media (max-width: 640px){
  :deep(.table th.col-actions), :deep(.table td.col-actions){ width: 220px; min-width: 220px; }
  :deep(.table th.col-exec), :deep(.table td.col-exec){ width: 130px; min-width: 130px; }
  :deep(.table th.col-enabled), :deep(.table td.col-enabled){ width: 110px; min-width: 110px; }
  .action-grid{ grid-template-columns: 1fr; }
  .exec-cell{ flex-direction: column; }
}


@media (max-width: 900px){
  :deep(.table th.col-desc), :deep(.table td.col-desc){ min-width: 360px; }
}
@media (max-width: 640px){
  :deep(.table th.col-desc), :deep(.table td.col-desc){ min-width: 280px; }
}
</style>