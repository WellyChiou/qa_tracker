<template>
  <AdminLayout>
    <div class="system-scheduler-page">
      <div class="page-header">
        <div>
          <h2>排程管理</h2>
          <p>沿用平台基礎排程管理思路，統一管理 invest 既有 jobs 的 Run Now 與執行紀錄。</p>
        </div>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前任務</span>
          <strong>{{ jobs.length }}</strong>
          <p>已接入統一排程管理入口的 invest jobs。</p>
        </article>
        <article class="overview-card">
          <span>啟用中</span>
          <strong>{{ jobs.filter(j => j.enabled).length }}</strong>
          <p>依現行設定顯示（第一版以顯示為主）。</p>
        </article>
        <article class="overview-card">
          <span>可 Run Now</span>
          <strong>{{ canRunScheduler ? jobs.length : 0 }}</strong>
          <p>需具備 `INVEST_SYS_SCHEDULER_RUN` 權限。</p>
        </article>
      </section>

      <section class="card surface-card">
        <div v-if="loading" class="empty-state">載入排程任務中...</div>
        <div v-else-if="jobs.length === 0" class="empty-state">目前沒有可管理的排程任務。</div>
        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Job Code</th>
                <th>任務名稱</th>
                <th>啟用狀態</th>
                <th>排程設定</th>
                <th>最近執行</th>
                <th>最近狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="job in jobs" :key="job.jobCode">
                <td><code>{{ job.jobCode }}</code></td>
                <td>
                  <div class="title-cell">
                    <strong>{{ job.jobName }}</strong>
                    <small>{{ job.description || '-' }}</small>
                  </div>
                </td>
                <td>
                  <span :class="job.enabled ? 'status-active' : 'status-inactive'">
                    {{ job.enabled ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>
                  <div class="schedule-cell">
                    <span class="badge">{{ formatScheduleType(job.scheduleType) }}</span>
                    <code>{{ job.scheduleExpression || '-' }}</code>
                  </div>
                </td>
                <td>{{ formatDateTime(job.lastRunAt) }}</td>
                <td>
                  <span :class="statusClass(job.lastRunStatus)">
                    {{ formatStatus(job.lastRunStatus) }}
                  </span>
                </td>
                <td>
                  <div class="actions">
                    <button
                      class="btn btn-primary"
                      :disabled="!canRunScheduler || runningJobCode === job.jobCode"
                      @click="runNow(job)"
                    >
                      {{ runningJobCode === job.jobCode ? '執行中...' : 'Run Now' }}
                    </button>
                    <button class="btn btn-secondary" @click="openLogs(job)">
                      查看 Logs
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <div v-if="showLogsModal" class="modal-overlay" @click="closeLogsModal">
        <div class="modal-panel modal-panel--wide" @click.stop>
          <div class="modal-header">
            <h3 class="modal-title">執行紀錄 - {{ selectedJob?.jobName || '-' }}</h3>
            <button class="btn-close" @click="closeLogsModal">×</button>
          </div>
          <div class="modal-body">
            <div v-if="logsLoading" class="empty-state">載入紀錄中...</div>
            <div v-else-if="logs.length === 0" class="empty-state">尚無執行紀錄。</div>
            <div v-else class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>狀態</th>
                    <th>開始時間</th>
                    <th>完成時間</th>
                    <th>批次資訊</th>
                    <th>訊息</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in logs" :key="row.id">
                    <td>{{ row.id }}</td>
                    <td>
                      <span :class="statusClass(row.status)">{{ formatStatus(row.status) }}</span>
                    </td>
                    <td>{{ formatDateTime(row.startedAt) }}</td>
                    <td>{{ formatDateTime(row.finishedAt) }}</td>
                    <td>
                      <div class="batch-cell">
                        <small>來源：{{ row.sourceTable || '-' }}</small>
                        <small v-if="row.batchId">batch：{{ row.batchId }}</small>
                        <small v-if="row.runMode">runMode：{{ row.runMode }}</small>
                        <small v-if="row.totalCount !== null && row.totalCount !== undefined">
                          total/success/fail：{{ row.totalCount }}/{{ row.successCount || 0 }}/{{ row.failCount || 0 }}
                        </small>
                      </div>
                    </td>
                    <td>{{ row.message || '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <PaginationControls
              v-model:pageSize="logsPagination.size"
              v-model:jumpPage="logsJumpPage"
              :total-records="logsPagination.totalElements"
              :current-page="logsPagination.page"
              :total-pages="logsPagination.totalPages"
              :page-size-options="[10, 20, 50]"
              page-size-id="system-scheduler-log-page-size"
              @first="goLogsFirstPage"
              @previous="goLogsPreviousPage"
              @next="goLogsNextPage"
              @last="goLogsLastPage"
              @jump="goLogsJumpPage"
            />
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
const jobs = ref([])
const runningJobCode = ref('')

const showLogsModal = ref(false)
const selectedJob = ref(null)
const logsLoading = ref(false)
const logs = ref([])
const logsJumpPage = ref(1)
const logsPagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const { currentUser } = useAuth()

const canRunScheduler = computed(() => {
  const permissions = currentUser.value?.permissions
  return Array.isArray(permissions) && permissions.includes('INVEST_SYS_SCHEDULER_RUN')
})

const loadJobs = async () => {
  loading.value = true
  try {
    const data = await investApiService.getSystemSchedulerJobs()
    jobs.value = Array.isArray(data) ? data : []
  } catch (error) {
    toast.error(`載入排程任務失敗: ${error.message || '未知錯誤'}`)
  } finally {
    loading.value = false
  }
}

const runNow = async (job) => {
  if (!canRunScheduler.value) {
    toast.error('你沒有排程執行權限')
    return
  }
  runningJobCode.value = job.jobCode
  try {
    const result = await investApiService.runSystemSchedulerJobNow(job.jobCode)
    toast.success(result?.message || `${job.jobName} 執行完成`)
    await loadJobs()
    if (showLogsModal.value && selectedJob.value?.jobCode === job.jobCode) {
      await loadLogs()
    }
  } catch (error) {
    toast.error(`執行失敗: ${error.message || '未知錯誤'}`)
  } finally {
    runningJobCode.value = ''
  }
}

const openLogs = async (job) => {
  selectedJob.value = job
  showLogsModal.value = true
  logsPagination.page = 1
  logsJumpPage.value = 1
  await loadLogs()
}

const closeLogsModal = () => {
  showLogsModal.value = false
  selectedJob.value = null
  logs.value = []
}

const loadLogs = async () => {
  if (!selectedJob.value?.jobCode) {
    return
  }
  logsLoading.value = true
  try {
    const params = {
      page: logsPagination.page - 1,
      size: logsPagination.size
    }
    const data = await investApiService.getSystemSchedulerJobLogsPaged(selectedJob.value.jobCode, params)
    logs.value = data?.content || []
    logsPagination.totalElements = Number(data?.totalElements || 0)
    logsPagination.totalPages = Number(data?.totalPages || 1)
    logsJumpPage.value = logsPagination.page
  } catch (error) {
    toast.error(`載入執行紀錄失敗: ${error.message || '未知錯誤'}`)
  } finally {
    logsLoading.value = false
  }
}

const formatStatus = (code) => {
  const map = {
    SUCCESS: '成功',
    FAILED: '失敗',
    RUNNING: '執行中',
    PARTIAL_FAILED: '部分失敗'
  }
  return map[code] || code || '尚無資料'
}

const statusClass = (code) => {
  if (code === 'SUCCESS') return 'status-active'
  if (code === 'FAILED' || code === 'PARTIAL_FAILED') return 'status-inactive'
  if (code === 'RUNNING') return 'status-pending'
  return 'status-unknown'
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-TW', { hour12: false })
}

const formatScheduleType = (type) => {
  const map = {
    CRON: 'CRON',
    FIXED_DELAY: 'FIXED_DELAY',
    MANUAL: 'MANUAL'
  }
  return map[type] || type || '-'
}

const goLogsFirstPage = () => { logsPagination.page = 1 }
const goLogsPreviousPage = () => { if (logsPagination.page > 1) logsPagination.page -= 1 }
const goLogsNextPage = () => { if (logsPagination.page < logsPagination.totalPages) logsPagination.page += 1 }
const goLogsLastPage = () => { logsPagination.page = logsPagination.totalPages }
const goLogsJumpPage = () => {
  const page = Number(logsJumpPage.value || 1)
  if (page < 1 || page > logsPagination.totalPages) return
  logsPagination.page = page
}

watch(
  () => [logsPagination.page, logsPagination.size],
  async () => {
    if (showLogsModal.value) {
      await loadLogs()
    }
  }
)

onMounted(async () => {
  await loadJobs()
})
</script>

<style scoped>
.system-scheduler-page { display: flex; flex-direction: column; gap: 14px; }
.page-header h2 { margin: 0; font-size: 1.75rem; }
.page-header p { margin: 6px 0 0; color: var(--text-secondary); }
.overview-strip { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.overview-card { border: 1px solid var(--border-color); border-radius: 14px; padding: 14px; background: #fff; }
.overview-card span { color: var(--text-secondary); font-size: .85rem; }
.overview-card strong { display: block; margin: 6px 0; font-size: 1.2rem; }
.overview-card p { margin: 0; color: var(--text-secondary); font-size: .86rem; }
.overview-card--accent { background: linear-gradient(135deg, rgba(37,99,235,.12), rgba(6,182,212,.1)); }
.table-wrap { overflow-x: auto; }
.title-cell { display: flex; flex-direction: column; gap: 4px; }
.title-cell small { color: var(--text-secondary); }
.schedule-cell { display: flex; flex-direction: column; gap: 4px; min-width: 240px; }
.schedule-cell code { white-space: pre-wrap; word-break: break-word; }
.badge { display: inline-block; border: 1px solid var(--border-color); border-radius: 999px; padding: 2px 8px; font-size: .75rem; color: var(--text-secondary); width: fit-content; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.status-active { color: #0b8f52; font-weight: 600; }
.status-inactive { color: #d93025; font-weight: 600; }
.status-pending { color: #a06a00; font-weight: 600; }
.status-unknown { color: var(--text-secondary); }
.batch-cell { display: flex; flex-direction: column; gap: 2px; }
.batch-cell small { color: var(--text-secondary); }
.modal-panel--wide { width: min(1200px, 94vw); max-height: 88vh; overflow: hidden; display: flex; flex-direction: column; }
.modal-body { overflow: auto; }
</style>
