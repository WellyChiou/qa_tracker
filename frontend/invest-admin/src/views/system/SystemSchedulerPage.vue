<template>
  <AdminLayout>
    <div class="admin-page">
      <header class="header">
        <div class="header-top">
          <div>
            <h1>⏰ 排程管理</h1>
            <p class="header-subtitle">沿用 church baseline 的操作節奏，集中管理 invest 排程任務與執行紀錄。</p>
          </div>
          <button
            class="btn btn-primary"
            :disabled="!canManageScheduler || hiddenJobs.length === 0"
            :title="hiddenJobs.length === 0 ? '目前沒有可新增的隱藏任務' : ''"
            @click="openCreateModal"
          >
            新增任務
          </button>
        </div>
      </header>

      <main class="main-content">
        <section class="overview-strip">
          <article class="overview-card overview-card--accent">
            <span>目前任務</span>
            <strong>{{ activeJobs.length }}</strong>
            <p>目前可見的排程任務數量。</p>
          </article>
          <article class="overview-card">
            <span>啟用中</span>
            <strong>{{ activeJobs.filter(job => job.enabled).length }}</strong>
            <p>停用後將跳過自動排程與立即執行。</p>
          </article>
          <article class="overview-card">
            <span>最近失敗</span>
            <strong>{{ activeJobs.filter(job => ['FAILED', 'PARTIAL_FAILED'].includes(latestStatus(job))).length }}</strong>
            <p>可點擊「查看記錄」查看失敗原因與股票明細。</p>
          </article>
        </section>

        <div class="card">
          <div class="card__body">
            <div v-if="loading" class="empty-state">載入排程任務中...</div>
            <div v-else-if="activeJobs.length === 0" class="empty-state">目前沒有可管理的排程任務。</div>
            <div v-else class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th class="col-id">ID</th>
                    <th class="col-name">任務名稱</th>
                    <th class="col-cron">CRON / 排程設定</th>
                    <th class="col-desc">描述</th>
                    <th class="col-enabled">啟用狀態</th>
                    <th class="col-exec">執行狀態</th>
                    <th class="col-actions">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="job in activeJobs" :key="job.jobCode">
                    <td class="col-id">{{ displayJobId(job) }}</td>
                    <td class="col-name">
                      <div class="title-cell">
                        <strong>{{ job.jobName }}</strong>
                        <small><code>{{ job.jobCode }}</code></small>
                      </div>
                    </td>
                    <td class="col-cron">
                      <div class="cron-wrap">
                        <span class="badge badge--cron" :title="cronTooltip(job)">
                          {{ formatCronExpression(job.scheduleExpression, job.scheduleType) }}
                        </span>
                        <code class="cron-code">{{ job.scheduleExpression || '-' }}</code>
                      </div>
                    </td>
                    <td class="col-desc">
                      <div class="desc-cell">{{ job.description || '-' }}</div>
                    </td>
                    <td class="col-enabled">
                      <span :class="job.enabled ? 'status-active' : 'status-inactive'">
                        {{ job.enabled ? '啟用' : '停用' }}
                      </span>
                    </td>
                    <td class="col-exec">
                      <div class="exec-cell" v-if="latestExecution(job)">
                        <span
                          class="status-icon-only"
                          :class="getStatusClass(latestExecution(job).status)"
                          :title="getStatusText(latestExecution(job).status)"
                        >
                          <svg v-if="latestExecution(job).status === 'SUCCESS'" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5"/></svg>
                          <svg v-else-if="latestExecution(job).status === 'FAILED'" viewBox="0 0 24 24"><path d="M12 9v4m0 4h.01"/><path d="M10.29 3.86l-8.17 14.14A2 2 0 0 0 3.83 21h16.34a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/></svg>
                          <svg v-else-if="latestExecution(job).status === 'PARTIAL_FAILED'" viewBox="0 0 24 24"><path d="M12 9v4m0 4h.01"/><path d="M10.29 3.86l-8.17 14.14A2 2 0 0 0 3.83 21h16.34a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/></svg>
                          <svg v-else-if="latestExecution(job).status === 'RUNNING'" viewBox="0 0 24 24"><path d="M12 2a10 10 0 1 0 10 10"/><path d="M12 6v6l4 2"/></svg>
                          <svg v-else viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                        </span>
                        <small>{{ getStatusText(latestExecution(job).status) }}</small>
                      </div>
                      <div v-else class="exec-cell exec-cell--empty">
                        <span class="status-icon-only status--none" title="未執行"><svg viewBox="0 0 24 24"><path d="M5 12h14"/></svg></span>
                      </div>
                    </td>
                    <td class="col-actions actions">
                      <div class="action-grid">
                        <button
                          class="btn-sm btn-execute"
                          :disabled="!canRunScheduler || !job.enabled || !job.allowRunNow || runningJobCode === job.jobCode"
                          @click="executeJob(job)"
                        >
                          <span v-if="runningJobCode === job.jobCode" class="spinner" aria-hidden="true"></span>
                          {{ runningJobCode === job.jobCode ? '執行中...' : '立即執行' }}
                        </button>
                        <button class="btn-sm btn-view" :disabled="!canViewScheduler" @click="openExecutions(job)">查看記錄</button>
                        <button
                          class="btn-sm btn-toggle"
                          :disabled="!canManageScheduler || togglingJobCode === job.jobCode"
                          @click="toggleJob(job)"
                        >
                          {{ job.enabled ? '停用' : '啟用' }}
                        </button>
                        <button
                          class="btn-sm btn-edit"
                          :disabled="!canManageScheduler"
                          @click="openEditModal(job)"
                        >
                          編輯
                        </button>
                        <button
                          class="btn-sm btn-delete"
                          :disabled="!canManageScheduler || deletingJobCode === job.jobCode"
                          @click="deleteJob(job)"
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
      </main>

      <div v-if="showExecutionModal" class="modal-overlay" @click="closeExecutionModal">
        <div class="modal-card modal-card--wide" @click.stop>
          <div class="modal-head">
            <div>
              <div class="modal-title">執行記錄</div>
              <div class="modal-sub">
                {{ selectedJob?.jobName || '-' }} <span class="dot">•</span> 共 {{ executionPagination.totalElements }} 筆
              </div>
            </div>
            <button class="modal-close" type="button" @click="closeExecutionModal">✕</button>
          </div>

          <div class="modal-body">
            <div v-if="executionLoading" class="empty-state">載入執行記錄中...</div>
            <div v-else-if="executionRows.length === 0" class="empty-state">尚無執行記錄</div>

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
                  <tr v-for="execution in executionRows" :key="execution.executionId || execution.referenceLogId">
                    <td class="mono">{{ formatDateTime(execution.startedAt) }}</td>
                    <td>
                      <span class="status-pill" :class="getStatusClass(execution.status)">
                        <span class="status-icon" aria-hidden="true">
                          <svg v-if="execution.status === 'SUCCESS'" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5"/></svg>
                          <svg v-else-if="execution.status === 'FAILED'" viewBox="0 0 24 24"><path d="M18 6L6 18M6 6l12 12"/></svg>
                          <svg v-else-if="execution.status === 'PARTIAL_FAILED'" viewBox="0 0 24 24"><path d="M12 9v4m0 4h.01"/><path d="M10.29 3.86l-8.17 14.14A2 2 0 0 0 3.83 21h16.34a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/></svg>
                          <svg v-else-if="execution.status === 'RUNNING'" viewBox="0 0 24 24"><path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83"/></svg>
                          <svg v-else viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                        </span>
                        {{ getStatusText(execution.status) }}
                      </span>
                    </td>
                    <td class="mono">{{ formatDateTime(execution.startedAt) }}</td>
                    <td class="mono">{{ formatDateTime(execution.completedAt) }}</td>
                    <td>
                      <div v-if="execution.resultMessage" class="msg msg--ok">{{ execution.resultMessage }}</div>
                      <div v-if="execution.errorMessage" class="msg msg--err">{{ execution.errorMessage }}</div>
                      <div v-if="execution.failedItems?.length" class="log-failed">
                        <small>失敗股票明細：</small>
                        <ul>
                          <li v-for="(item, idx) in execution.failedItems" :key="`${execution.executionId}-failed-${idx}`">
                            <code>{{ item.ticker || '-' }}</code>：{{ formatReason(item.reason) }}
                          </li>
                        </ul>
                      </div>
                      <div class="meta-row" v-if="execution.referenceLogId || execution.batchId || execution.runMode">
                        <small v-if="execution.referenceLogId">referenceLogId：{{ execution.referenceLogId }}</small>
                        <small v-if="execution.batchId">batch：{{ execution.batchId }}</small>
                        <small v-if="execution.runMode">runMode：{{ execution.runMode }}</small>
                      </div>
                      <span v-if="!execution.resultMessage && !execution.errorMessage && !execution.failedItems?.length" class="muted">-</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <PaginationControls
              v-model:pageSize="executionPagination.size"
              v-model:jumpPage="executionJumpPage"
              :total-records="executionPagination.totalElements"
              :current-page="executionPagination.page"
              :total-pages="executionPagination.totalPages"
              :page-size-options="[10, 20, 50]"
              page-size-id="invest-system-scheduler-execution-page-size"
              @first="goExecutionFirstPage"
              @previous="goExecutionPreviousPage"
              @next="goExecutionNextPage"
              @last="goExecutionLastPage"
              @jump="goExecutionJumpPage"
            />
          </div>

          <div class="modal-foot">
            <button type="button" class="btn btn-secondary" @click="closeExecutionModal">關閉</button>
          </div>
        </div>
      </div>

      <div v-if="showEditModal" class="modal-overlay" @click="closeEditModal">
        <div class="modal-card" @click.stop>
          <div class="modal-head">
            <div>
              <div class="modal-title">{{ editMode === 'create' ? '新增排程任務' : '編輯排程任務' }}</div>
              <div class="modal-sub">設定任務名稱、排程設定與啟停狀態。</div>
            </div>
            <button class="modal-close" type="button" @click="closeEditModal">✕</button>
          </div>

          <div class="modal-body">
            <form @submit.prevent="submitForm">
              <div class="form-group">
                <label>Job Code <span class="required">*</span></label>
                <select v-if="editMode === 'create'" v-model="jobForm.jobCode" required>
                  <option value="" disabled>請選擇 Job</option>
                  <option v-for="job in hiddenJobs" :key="`hidden-${job.jobCode}`" :value="job.jobCode">
                    {{ job.jobCode }} - {{ job.jobName }}
                  </option>
                </select>
                <input v-else v-model="jobForm.jobCode" type="text" disabled />
              </div>

              <div class="form-group">
                <label>任務名稱 <span class="required">*</span></label>
                <input v-model="jobForm.jobName" type="text" maxlength="120" required />
              </div>

              <div class="form-group">
                <label>排程設定</label>
                <input
                  v-model="jobForm.scheduleExpression"
                  type="text"
                  :disabled="jobForm.scheduleEditable === false"
                />
                <small class="help" v-if="jobForm.scheduleEditable === false">
                  第一版先保留唯讀顯示（對齊目前 invest scheduler 實作），避免誤解為已支援動態 cron 即時生效。
                </small>
              </div>

              <div class="form-group">
                <label>描述</label>
                <textarea v-model="jobForm.description" rows="3" maxlength="500" />
              </div>

              <div class="form-group form-group-inline">
                <label class="check-row">
                  <input v-model="jobForm.enabled" type="checkbox" />
                  啟用此任務
                </label>
                <label class="check-row">
                  <input v-model="jobForm.allowRunNow" type="checkbox" />
                  允許立即執行
                </label>
              </div>

              <div class="modal-foot">
                <button type="submit" class="btn btn-primary" :disabled="savingForm">
                  {{ savingForm ? '儲存中...' : '儲存' }}
                </button>
                <button type="button" class="btn btn-secondary" @click="closeEditModal">取消</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { toast } from '@shared/composables/useToast'
import AdminLayout from '@/components/AdminLayout.vue'
import { investApiService } from '@/composables/useInvestApi'
import { useAuth } from '@/composables/useAuth'

const loading = ref(false)
const allJobs = ref([])
const runningJobCode = ref('')
const togglingJobCode = ref('')
const deletingJobCode = ref('')

const showEditModal = ref(false)
const editMode = ref('edit')
const savingForm = ref(false)
const jobForm = reactive({
  jobCode: '',
  jobName: '',
  description: '',
  enabled: true,
  scheduleType: '',
  scheduleExpression: '',
  scheduleEditable: false,
  allowRunNow: true
})

const showExecutionModal = ref(false)
const selectedJob = ref(null)
const executionLoading = ref(false)
const executionRows = ref([])
const executionJumpPage = ref(1)
const executionPagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const jobExecutions = ref({})

const { currentUser } = useAuth()

const permissionSet = computed(() => new Set(currentUser.value?.permissions || []))
const canRunScheduler = computed(() => permissionSet.value.has('INVEST_SYS_SCHEDULER_RUN'))
const canManageScheduler = computed(() => permissionSet.value.has('INVEST_SYS_SCHEDULER_MANAGE'))
const canViewScheduler = computed(() => (
  permissionSet.value.has('INVEST_SYS_SCHEDULER_VIEW') || canRunScheduler.value || canManageScheduler.value
))

const activeJobs = computed(() => allJobs.value.filter(job => job.active !== false))
const hiddenJobs = computed(() => allJobs.value.filter(job => job.active === false))

const isNotFoundError = (error) => {
  const message = String(error?.message || '')
  const status = String(error?.status || '')
  return message.includes('404') || status === '404'
}

const displayJobId = (job) => {
  if (job?.configId !== null && job?.configId !== undefined) return job.configId
  return '-'
}

const latestExecution = (job) => {
  return jobExecutions.value[job.jobCode] || null
}

const latestStatus = (job) => latestExecution(job)?.status || ''

const loadJobs = async () => {
  loading.value = true
  try {
    const data = await investApiService.getSystemSchedulerJobs({ includeInactive: true })
    const list = Array.isArray(data) ? data : []
    allJobs.value = list

    const executionMap = {}
    await Promise.all(list.map(async (job) => {
      executionMap[job.jobCode] = await fetchLatestExecution(job.jobCode)
    }))
    jobExecutions.value = executionMap
  } catch (error) {
    toast.error(`載入排程任務失敗: ${error.message || '未知錯誤'}`)
  } finally {
    loading.value = false
  }
}

const fetchLatestExecution = async (jobCode) => {
  try {
    return await investApiService.getSystemSchedulerJobLatestExecution(jobCode)
  } catch (error) {
    if (!isNotFoundError(error)) {
      console.error(`載入 ${jobCode} 最新 execution 失敗`, error)
    }
    return null
  }
}

const executeJob = async (job) => {
  if (!canRunScheduler.value) {
    toast.error('你沒有排程執行權限')
    return
  }
  if (!job.enabled || !job.allowRunNow) {
    toast.error('此任務目前不允許執行')
    return
  }

  runningJobCode.value = job.jobCode
  try {
    const result = await investApiService.executeSystemSchedulerJob(job.jobCode)
    toast.success(result?.message || `${job.jobName} 已觸發執行`)

    await loadJobs()
    if (showExecutionModal.value && selectedJob.value?.jobCode === job.jobCode) {
      await loadExecutions()
    }
  } catch (error) {
    toast.error(`執行失敗: ${error.message || '未知錯誤'}`)
  } finally {
    runningJobCode.value = ''
  }
}

const openCreateModal = () => {
  if (!canManageScheduler.value) {
    toast.error('你沒有排程管理權限')
    return
  }
  if (hiddenJobs.value.length === 0) {
    toast.error('目前沒有可新增的隱藏任務')
    return
  }
  editMode.value = 'create'
  fillFormFromJob(hiddenJobs.value[0])
  showEditModal.value = true
}

const openEditModal = (job) => {
  if (!canManageScheduler.value) {
    toast.error('你沒有排程管理權限')
    return
  }
  editMode.value = 'edit'
  fillFormFromJob(job)
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
}

const fillFormFromJob = (job) => {
  jobForm.jobCode = job?.jobCode || ''
  jobForm.jobName = job?.jobName || ''
  jobForm.description = job?.description || ''
  jobForm.enabled = job?.enabled !== false
  jobForm.scheduleType = job?.scheduleType || ''
  jobForm.scheduleExpression = job?.scheduleExpression || ''
  jobForm.scheduleEditable = job?.scheduleEditable === true
  jobForm.allowRunNow = job?.allowRunNow !== false
}

const submitForm = async () => {
  if (!jobForm.jobCode) {
    toast.error('請先選擇 Job Code')
    return
  }

  const payload = {
    jobCode: jobForm.jobCode,
    jobName: jobForm.jobName,
    description: jobForm.description,
    enabled: Boolean(jobForm.enabled),
    scheduleExpression: jobForm.scheduleExpression,
    allowRunNow: Boolean(jobForm.allowRunNow)
  }

  savingForm.value = true
  try {
    if (editMode.value === 'create') {
      await investApiService.createSystemSchedulerJob(payload)
      toast.success('新增排程任務成功')
    } else {
      await investApiService.updateSystemSchedulerJob(jobForm.jobCode, payload)
      toast.success('更新排程任務成功')
    }
    showEditModal.value = false
    await loadJobs()
  } catch (error) {
    toast.error(`儲存失敗: ${error.message || '未知錯誤'}`)
  } finally {
    savingForm.value = false
  }
}

const toggleJob = async (job) => {
  if (!canManageScheduler.value) {
    toast.error('你沒有排程管理權限')
    return
  }
  togglingJobCode.value = job.jobCode
  try {
    await investApiService.toggleSystemSchedulerJob(job.jobCode, !job.enabled)
    toast.success(`${job.jobName} 已${job.enabled ? '停用' : '啟用'}`)
    await loadJobs()
  } catch (error) {
    toast.error(`切換失敗: ${error.message || '未知錯誤'}`)
  } finally {
    togglingJobCode.value = ''
  }
}

const deleteJob = async (job) => {
  if (!canManageScheduler.value) {
    toast.error('你沒有排程管理權限')
    return
  }

  const ok = window.confirm(`確認刪除任務「${job.jobName}」？刪除後會從列表隱藏。`)
  if (!ok) return

  deletingJobCode.value = job.jobCode
  try {
    await investApiService.deleteSystemSchedulerJob(job.jobCode)
    toast.success('任務已刪除')
    await loadJobs()
  } catch (error) {
    toast.error(`刪除失敗: ${error.message || '未知錯誤'}`)
  } finally {
    deletingJobCode.value = ''
  }
}

const openExecutions = async (job) => {
  if (!canViewScheduler.value) {
    toast.error('你沒有排程查看權限')
    return
  }
  selectedJob.value = job
  executionPagination.page = 1
  executionJumpPage.value = 1
  showExecutionModal.value = true
  await loadExecutions()
}

const closeExecutionModal = () => {
  showExecutionModal.value = false
  selectedJob.value = null
  executionRows.value = []
}

const loadExecutions = async () => {
  if (!selectedJob.value?.jobCode) return

  executionLoading.value = true
  try {
    const params = {
      page: executionPagination.page - 1,
      size: executionPagination.size
    }

    const [executionData, legacyLogData] = await Promise.all([
      investApiService.getSystemSchedulerJobExecutionsPaged(selectedJob.value.jobCode, params),
      investApiService.getSystemSchedulerJobLogsPaged(selectedJob.value.jobCode, params)
    ])

    const legacyMap = new Map((legacyLogData?.content || []).map(row => [row.id, row]))

    executionRows.value = (executionData?.content || []).map(row => {
      const legacy = legacyMap.get(row.executionId) || null
      return {
        ...row,
        failedItems: legacy?.failedItems || [],
        runMode: legacy?.runMode || null,
        batchId: row.batchId || legacy?.batchId || null
      }
    })

    executionPagination.totalElements = Number(executionData?.totalElements || 0)
    executionPagination.totalPages = Number(executionData?.totalPages || 1)
    executionJumpPage.value = executionPagination.page
  } catch (error) {
    toast.error(`載入執行記錄失敗: ${error.message || '未知錯誤'}`)
  } finally {
    executionLoading.value = false
  }
}

const getStatusClass = (status) => {
  switch (status) {
    case 'SUCCESS':
      return 'status-success'
    case 'FAILED':
      return 'status-failed'
    case 'PARTIAL_FAILED':
      return 'status-partial-failed'
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
    case 'PARTIAL_FAILED':
      return '部分失敗'
    case 'RUNNING':
      return '執行中'
    case 'PENDING':
      return '等待執行'
    default:
      return '未執行'
  }
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

const formatReason = (value) => {
  if (!value) return '未知原因'
  return String(value).replace(/^回補失敗：/, '').replace(/^更新失敗：/, '')
}

const formatCronExpression = (expression, scheduleType) => {
  if (!expression) return '-'
  if (scheduleType !== 'CRON') {
    if (scheduleType === 'MANUAL_ONLY') return '手動觸發（MANUAL_ONLY）'
    if (scheduleType === 'MANUAL') return '手動觸發（MANUAL）'
    return expression
  }

  const parts = expression.trim().split(/\s+/)
  if (parts.length < 6) return expression

  const [, minute, hour, day, month, weekday] = parts
  const timeStr = `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`

  if (day === '*' && month === '*' && (weekday === '*' || weekday === '?')) {
    return `每天 ${timeStr}`
  }
  if (day === '?' && month === '*' && weekday !== '*' && weekday !== '?') {
    const map = {
      MON: '週一', TUE: '週二', WED: '週三', THU: '週四', FRI: '週五', SAT: '週六', SUN: '週日'
    }
    if (weekday.includes('-')) {
      const [start, end] = weekday.split('-')
      return `${map[start] || start}至${map[end] || end} ${timeStr}`
    }
    if (weekday.includes(',')) {
      const days = weekday.split(',').map(w => map[w] || w).join('、')
      return `每${days} ${timeStr}`
    }
    return `${map[weekday] || weekday} ${timeStr}`
  }
  if (day !== '*' && day !== '?' && month === '*' && (weekday === '*' || weekday === '?')) {
    return `每月 ${day} 日 ${timeStr}`
  }

  return expression
}

const cronTooltip = (job) => {
  const scheduleType = job?.scheduleType || '-'
  const expression = job?.scheduleExpression || '-'
  const pretty = formatCronExpression(expression, scheduleType)
  return `${pretty}\n${scheduleType}\n${expression}`
}

const goExecutionFirstPage = () => {
  executionPagination.page = 1
}
const goExecutionPreviousPage = () => {
  if (executionPagination.page > 1) executionPagination.page -= 1
}
const goExecutionNextPage = () => {
  if (executionPagination.page < executionPagination.totalPages) executionPagination.page += 1
}
const goExecutionLastPage = () => {
  executionPagination.page = executionPagination.totalPages
}
const goExecutionJumpPage = () => {
  const page = Number(executionJumpPage.value || 1)
  if (page < 1 || page > executionPagination.totalPages) return
  executionPagination.page = page
}

watch(
  () => [executionPagination.page, executionPagination.size],
  async () => {
    if (showExecutionModal.value) {
      await loadExecutions()
    }
  }
)

onMounted(async () => {
  await loadJobs()
})
</script>

<style scoped>
.admin-page{
  display:flex;
  flex-direction:column;
  gap:14px;
}

.header-top{
  display:flex;
  align-items:flex-start;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
}

.header-top h1{
  font-size:28px;
  line-height:1.1;
  font-weight:900;
  letter-spacing:-0.03em;
}

.header-subtitle{
  margin-top:6px;
  color:var(--text-secondary);
  font-size:14px;
  font-weight:700;
}

.overview-strip{
  display:grid;
  grid-template-columns:repeat(3, minmax(0, 1fr));
  gap:12px;
  margin-bottom:14px;
}

.overview-card{
  padding:16px;
  border-radius:20px;
  border:1px solid rgba(2,6,23,.08);
  background:rgba(255,255,255,.88);
  box-shadow:var(--shadow-sm);
}

.overview-card span{
  display:block;
  color:rgba(2,6,23,.56);
  font-size:12px;
  font-weight:900;
  letter-spacing:.12em;
  text-transform:uppercase;
}

.overview-card strong{
  display:block;
  margin-top:8px;
  font-size:28px;
  line-height:1;
  letter-spacing:-0.04em;
}

.overview-card p{
  margin:8px 0 0;
  color:rgba(2,6,23,.62);
  font-size:13px;
  line-height:1.6;
  font-weight:700;
}

.overview-card--accent{
  background:linear-gradient(140deg, rgba(15,23,42,.96), rgba(29,78,216,.92));
}

.overview-card--accent span,
.overview-card--accent strong,
.overview-card--accent p{
  color:#fff;
}

.overview-card--accent p{
  color:rgba(255,255,255,.76);
}

.table-wrap{ overflow-x:auto; }
:deep(.table){ table-layout:fixed; width:100%; }
:deep(.table th.col-id), :deep(.table td.col-id){ width:6%; min-width:70px; }
:deep(.table th.col-name), :deep(.table td.col-name){ width:16%; min-width:170px; }
:deep(.table th.col-cron), :deep(.table td.col-cron){ width:18%; min-width:190px; }
:deep(.table th.col-desc), :deep(.table td.col-desc){ width:20%; min-width:200px; }
:deep(.table th.col-enabled), :deep(.table td.col-enabled){ width:10%; min-width:100px; text-align:center; }
:deep(.table th.col-exec), :deep(.table td.col-exec){ width:13%; min-width:140px; text-align:center; }
:deep(.table th.col-actions), :deep(.table td.col-actions){ width:17%; min-width:240px; }

.title-cell{ display:flex; flex-direction:column; gap:4px; }
.title-cell small{ color:var(--text-secondary); }
.desc-cell{
  overflow:hidden;
  text-overflow:ellipsis;
  display:-webkit-box;
  -webkit-box-orient:vertical;
  -webkit-line-clamp:2;
  line-height:1.5;
  color:var(--text-secondary);
}

.cron-wrap{ display:flex; flex-direction:column; gap:6px; }
.badge{
  display:inline-flex;
  align-items:center;
  padding:6px 10px;
  border-radius:999px;
  border:1px solid rgba(2,6,23,.10);
  background:rgba(2,6,23,.02);
  font-weight:900;
  font-size:13px;
  color:rgba(15,23,42,.92);
  width:fit-content;
}
.badge--cron{ border-color:rgba(59,130,246,.18); background:rgba(59,130,246,.06); }
.cron-code{
  font-size:12px;
  color:rgba(15,23,42,.55);
  background:rgba(2,6,23,.03);
  padding:3px 8px;
  border-radius:10px;
  border:1px solid rgba(2,6,23,.08);
  width:fit-content;
  max-width:100%;
  overflow:hidden;
  text-overflow:ellipsis;
}

.exec-cell{ display:flex; align-items:center; justify-content:center; gap:8px; flex-direction:column; }
.exec-cell small{ color:var(--text-secondary); font-size:12px; font-weight:700; }
.exec-cell--empty{ justify-content:center; }

.status-icon-only{
  display:inline-flex;
  align-items:center;
  justify-content:center;
  width:30px;
  height:30px;
  border-radius:999px;
  border:1px solid rgba(2,6,23,.12);
  background:rgba(2,6,23,.03);
}
.status-icon-only svg{
  width:18px;
  height:18px;
  fill:none;
  stroke:currentColor;
  stroke-width:2.2;
  stroke-linecap:round;
  stroke-linejoin:round;
}
.status-icon-only.status--none{ color:#94a3b8; }

.actions{ display:flex; align-items:center; }
.action-grid{
  display:grid;
  grid-template-columns:repeat(2, 1fr);
  gap:8px 10px;
  width:100%;
}
.action-grid .btn-sm{ width:100%; white-space:nowrap; }

.btn-sm{
  height:34px;
  border-radius:10px;
  border:1px solid rgba(2,6,23,.12);
  background:#fff;
  color:var(--text-primary);
  font-size:13px;
  font-weight:800;
  padding:0 10px;
  cursor:pointer;
  display:inline-flex;
  align-items:center;
  justify-content:center;
  gap:6px;
}

.btn-sm:disabled{
  cursor:not-allowed;
  opacity:.55;
}

.btn-execute{
  background:linear-gradient(140deg, rgba(15,23,42,.96), rgba(29,78,216,.92));
  color:#fff;
  border-color:transparent;
}
.btn-view{
  color:#1d4ed8;
  border-color:rgba(37,99,235,.28);
  background:rgba(59,130,246,.08);
}
.btn-toggle{
  color:#0f766e;
  border-color:rgba(13,148,136,.28);
  background:rgba(20,184,166,.08);
}
.btn-edit{
  color:#1d4ed8;
  border-color:rgba(59,130,246,.30);
  background:#eff6ff;
}
.btn-delete{
  color:#b42318;
  border-color:#f3b6b6;
  background:#fff5f5;
}

.spinner{
  width:14px;
  height:14px;
  border-radius:999px;
  border:2px solid rgba(255,255,255,.35);
  border-top-color:rgba(255,255,255,.95);
  display:inline-block;
  animation:spin .9s linear infinite;
}
@keyframes spin{ to{ transform:rotate(360deg); } }

.status-active{ color:#0b8f52; font-weight:700; }
.status-inactive{ color:#d93025; font-weight:700; }
.status-unknown{ color:var(--text-secondary); font-weight:700; }

.status-pill{
  display:inline-flex;
  align-items:center;
  gap:8px;
  padding:6px 10px;
  border-radius:999px;
  border:1px solid rgba(2,6,23,.10);
  background:rgba(2,6,23,.02);
  font-weight:900;
  font-size:13px;
}
.status-icon svg{
  width:16px;
  height:16px;
  fill:none;
  stroke:currentColor;
  stroke-width:2;
  stroke-linecap:round;
  stroke-linejoin:round;
}
.status-success{ color:#16a34a; border-color:rgba(22,163,74,.25); background:rgba(22,163,74,.10); }
.status-failed{ color:#ef4444; border-color:rgba(239,68,68,.25); background:rgba(239,68,68,.10); }
.status-partial-failed{ color:#dc2626; border-color:rgba(220,38,38,.25); background:rgba(220,38,38,.10); }
.status-running{ color:#2563eb; border-color:rgba(37,99,235,.25); background:rgba(37,99,235,.10); }
.status-pending{ color:#f59e0b; border-color:rgba(245,158,11,.25); background:rgba(245,158,11,.10); }

.mono{ font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
.msg{ margin-bottom:4px; line-height:1.5; }
.msg--ok{ color:#15803d; }
.msg--err{ color:#dc2626; }
.meta-row{ display:flex; flex-wrap:wrap; gap:10px; margin-top:4px; }
.meta-row small{ color:var(--text-secondary); }

.log-failed small{ color:#d93025; font-weight:700; }
.log-failed ul{ margin:4px 0 0; padding-left:16px; color:var(--text-secondary); }
.log-failed li{ margin-bottom:3px; }

.form-group{ display:flex; flex-direction:column; gap:6px; margin-bottom:12px; }
.form-group select,
.form-group input,
.form-group textarea{
  border:1px solid var(--border-color);
  border-radius:10px;
  padding:8px 10px;
  font-size:.95rem;
  background:#fff;
}
.form-group-inline{
  flex-direction:row;
  align-items:center;
  gap:16px;
  flex-wrap:wrap;
}
.check-row{ display:flex; align-items:center; gap:8px; }
.check-row input{ width:16px; height:16px; }
.required{ color:#d93025; }
.help{ color:var(--text-secondary); font-size:.82rem; }

.modal-card--wide{ width:min(1200px, 94vw); max-height:88vh; overflow:hidden; display:flex; flex-direction:column; }
.modal-body{ overflow:auto; }

@media (max-width: 900px){
  .overview-strip{ grid-template-columns:1fr; }
  .action-grid{ grid-template-columns:1fr; }
  :deep(.table th.col-actions), :deep(.table td.col-actions){ min-width:210px; }
}
</style>
