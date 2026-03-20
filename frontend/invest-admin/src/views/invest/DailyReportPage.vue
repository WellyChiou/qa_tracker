<template>
  <AdminLayout>
    <div class="daily-report-page">
      <div class="page-header">
        <div>
          <h2>每日報告</h2>
          <p>查看每日持股快照與風險摘要，並可手動觸發當前使用者的日報批次。</p>
        </div>
        <button v-if="canRunBatchJob" class="btn btn-primary" :disabled="runningBatch" @click="runNow">
          {{ runningBatch ? '執行中...' : '手動執行今日批次' }}
        </button>
      </div>

      <details class="filters filters--collapsible" open>
        <summary>
          <div class="filters__title">
            <h3>查詢條件</h3>
          </div>
        </summary>
        <div class="filters__content">
          <div class="filter-grid">
            <div class="filter-group">
              <label>起始日期</label>
              <input v-model="filters.reportDateFrom" type="date" />
            </div>
            <div class="filter-group">
              <label>結束日期</label>
              <input v-model="filters.reportDateTo" type="date" />
            </div>
            <div class="filter-group">
              <label>狀態</label>
              <select v-model="filters.status">
                <option value="">全部</option>
                <option value="SUCCESS">成功</option>
                <option value="FAILED">失敗</option>
              </select>
            </div>
            <div class="filter-group actions">
              <button class="btn btn-secondary" @click="resetFilters">清除條件</button>
            </div>
          </div>
        </div>
      </details>

      <section class="overview-strip" v-if="selectedSummary">
        <article class="overview-card overview-card--accent">
          <span>報告日期</span>
          <strong>{{ selectedSummary.reportDate || '-' }}</strong>
          <p>資料基準交易日：{{ selectedSummary.dataAsOfTradeDate || '-' }}</p>
        </article>
        <article class="overview-card">
          <span>持股 / 快照</span>
          <strong>{{ selectedSummary.holdingCount || 0 }} / {{ selectedSummary.snapshotCount || 0 }}</strong>
          <p>已建立快照筆數可小於持股筆數。</p>
        </article>
        <article class="overview-card">
          <span>總市值 / 總成本</span>
          <strong>{{ formatMoney(selectedSummary.totalMarketValue) }}</strong>
          <p>成本 {{ formatMoney(selectedSummary.totalCost) }}</p>
        </article>
        <article class="overview-card">
          <span>總損益</span>
          <strong :class="Number(selectedSummary.totalPnL) >= 0 ? 'text-up' : 'text-down'">
            {{ formatSignedMoney(selectedSummary.totalPnL) }}
          </strong>
          <p>{{ formatPercent(selectedSummary.totalPnLPercent) }}</p>
        </article>
      </section>

      <section class="card surface-card" v-if="selectedSummary">
        <div class="section-header">
          <h3>高風險持股</h3>
        </div>
        <div v-if="(selectedSummary.topRiskHoldings || []).length === 0" class="empty-state">
          此報告沒有高風險持股資料。
        </div>
        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>股票</th>
                <th>風險分數</th>
                <th>風險等級</th>
                <th>建議</th>
                <th>摘要</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in selectedSummary.topRiskHoldings" :key="`${item.portfolioId}-${item.ticker}`">
                <td>{{ item.ticker }}<br /><small>{{ item.stockName }}</small></td>
                <td>{{ item.riskScore }}</td>
                <td>{{ formatRiskLevel(item.riskLevel) }}</td>
                <td>{{ formatRecommendation(item.recommendation) }}</td>
                <td>{{ item.summary }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="card surface-card" v-if="selectedSummary">
        <div class="section-header">
          <h3>建議分布摘要</h3>
        </div>
        <div class="distribution-grid">
          <article
            v-for="item in (selectedSummary.recommendationDistribution || [])"
            :key="`dist-${item.key}`"
            class="dist-card"
          >
            <span>{{ formatRecommendation(item.key) }}</span>
            <strong>{{ item.count || 0 }}</strong>
          </article>
        </div>
      </section>

      <section class="card surface-card" v-if="selectedSummary">
        <div class="section-header">
          <h3>重要提醒</h3>
        </div>
        <ul class="reminders">
          <li v-for="(item, idx) in selectedSummary.noviceReminders || []" :key="`reminder-${idx}`">{{ item }}</li>
        </ul>
        <p class="disclaimer">{{ selectedSummary.disclaimer }}</p>
      </section>

      <section class="card surface-card table-wrap">
        <div class="section-header">
          <h3>報告列表</h3>
        </div>
        <div v-if="rows.length === 0" class="empty-state">
          {{ pagination.totalElements === 0 ? '尚無每日報告資料' : '沒有符合條件的資料' }}
        </div>
        <div v-else>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>報告日期</th>
                <th>資料基準交易日</th>
                <th>狀態</th>
                <th>持股數</th>
                <th>高風險</th>
                <th>極高風險</th>
                <th>建立時間</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td>{{ row.id }}</td>
                <td>{{ row.reportDate }}</td>
                <td>{{ row.dataAsOfTradeDate || '-' }}</td>
                <td>{{ row.status }}</td>
                <td>{{ row.holdingCount || 0 }}</td>
                <td>{{ row.highRiskCount || 0 }}</td>
                <td>{{ row.criticalRiskCount || 0 }}</td>
                <td>{{ formatDateTime(row.createdAt) }}</td>
                <td>
                  <button class="btn btn-secondary" @click="selectReport(row.id)">查看</button>
                </td>
              </tr>
            </tbody>
          </table>

          <PaginationControls
            v-model:pageSize="pagination.size"
            v-model:jumpPage="jumpPage"
            :total-records="pagination.totalElements"
            :current-page="pagination.page"
            :total-pages="pagination.totalPages"
            :page-size-options="[10, 20, 50]"
            page-size-id="daily-report-page-size"
            @first="goFirstPage"
            @previous="goPreviousPage"
            @next="goNextPage"
            @last="goLastPage"
            @jump="goJumpPage"
          />
        </div>
      </section>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { toast } from '@shared/composables/useToast'
import AdminLayout from '@/components/AdminLayout.vue'
import { investApiService } from '@/composables/useInvestApi'
import { useAuth } from '@/composables/useAuth'

const rows = ref([])
const selectedReport = ref(null)
const runningBatch = ref(false)
const jumpPage = ref(1)
const { currentUser } = useAuth()

const filters = reactive({
  reportDateFrom: '',
  reportDateTo: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const selectedSummary = computed(() => selectedReport.value?.summary || null)
const canRunBatchJob = computed(() => {
  const permissions = currentUser.value?.permissions
  return Array.isArray(permissions) && permissions.includes('INVEST_JOB_RUN_DAILY_REPORT')
})

const buildParams = () => {
  const params = {
    page: pagination.page - 1,
    size: pagination.size,
    reportType: 'PORTFOLIO_RISK_DAILY'
  }
  if (filters.reportDateFrom) params.reportDateFrom = filters.reportDateFrom
  if (filters.reportDateTo) params.reportDateTo = filters.reportDateTo
  if (filters.status) params.status = filters.status
  return params
}

const loadRows = async () => {
  const data = await investApiService.getDailyReportsPaged(buildParams())
  rows.value = data?.content || []
  pagination.totalElements = Number(data?.totalElements || 0)
  pagination.totalPages = Number(data?.totalPages || 1)
  if (pagination.page > pagination.totalPages) {
    pagination.page = pagination.totalPages || 1
  }
}

const loadLatest = async () => {
  try {
    selectedReport.value = await investApiService.getLatestDailyReport('PORTFOLIO_RISK_DAILY')
  } catch {
    selectedReport.value = null
  }
}

const selectReport = async (id) => {
  try {
    selectedReport.value = await investApiService.getDailyReportById(id)
  } catch (error) {
    toast.error(`載入報告明細失敗: ${error.message || '未知錯誤'}`)
  }
}

const resetFilters = () => {
  filters.reportDateFrom = ''
  filters.reportDateTo = ''
  filters.status = ''
}

const goPage = (targetPage) => {
  const page = Math.max(1, Math.min(targetPage, pagination.totalPages || 1))
  pagination.page = page
  jumpPage.value = page
  loadRows()
}

const goFirstPage = () => goPage(1)
const goPreviousPage = () => goPage(pagination.page - 1)
const goNextPage = () => goPage(pagination.page + 1)
const goLastPage = () => goPage(pagination.totalPages)
const goJumpPage = () => goPage(Number(jumpPage.value || 1))

const runNow = async () => {
  runningBatch.value = true
  try {
    const result = await investApiService.runDailyPortfolioRiskReport()
    toast.success(result?.message || '每日報告批次執行完成')
    await Promise.all([loadRows(), loadLatest()])
  } catch (error) {
    toast.error(`執行每日報告批次失敗: ${error.message || '未知錯誤'}`)
  } finally {
    runningBatch.value = false
  }
}

const formatMoney = (value) => `$${Number(value || 0).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const formatSignedMoney = (value) => {
  const number = Number(value || 0)
  const sign = number >= 0 ? '+' : '-'
  return `${sign}$${Math.abs(number).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`

const formatDateTime = (value) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

const formatRiskLevel = (code) => {
  const map = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '極高'
  }
  return map[code] || code || '-'
}

const formatRecommendation = (code) => {
  const map = {
    HOLD: '續抱',
    WATCH: '觀察',
    REDUCE: '減碼',
    STOP_LOSS_CHECK: '停損檢查'
  }
  return map[code] || code || '-'
}

watch(
  () => [filters.reportDateFrom, filters.reportDateTo, filters.status],
  () => {
    pagination.page = 1
    jumpPage.value = 1
    loadRows()
  }
)

watch(
  () => pagination.size,
  () => {
    pagination.page = 1
    jumpPage.value = 1
    loadRows()
  }
)

onMounted(async () => {
  try {
    await Promise.all([loadRows(), loadLatest()])
  } catch (error) {
    toast.error(`載入每日報告頁失敗: ${error.message || '未知錯誤'}`)
  }
})
</script>

<style scoped>
.daily-report-page { display: flex; flex-direction: column; gap: 14px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.page-header h2 { margin: 0; font-size: 1.75rem; }
.page-header p { margin: 6px 0 0; color: var(--text-secondary); }
.filter-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; }
.filter-group.actions { display: flex; align-items: flex-end; }
.overview-strip { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.overview-card { border: 1px solid var(--border-color); border-radius: 14px; padding: 14px; background: #fff; }
.overview-card span { color: var(--text-secondary); font-size: 0.85rem; }
.overview-card strong { display: block; margin: 6px 0; font-size: 1.2rem; }
.overview-card p { margin: 0; color: var(--text-secondary); font-size: 0.86rem; }
.overview-card--accent { background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(6, 182, 212, 0.1)); }
.section-header { margin-bottom: 8px; }
.section-header h3 { margin: 0; }
.distribution-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 8px; }
.dist-card { border: 1px solid var(--border-color); border-radius: 10px; padding: 10px; background: #fff; }
.dist-card span { color: var(--text-secondary); font-size: 0.8rem; display: block; }
.dist-card strong { display: block; margin-top: 4px; font-size: 1.1rem; }
.reminders { margin: 0; padding-left: 18px; color: #334155; }
.disclaimer { margin: 10px 0 0; color: #475569; font-size: 0.85rem; }
.empty-state { color: var(--text-secondary); text-align: center; padding: 12px; }
.text-up { color: #0f766e; }
.text-down { color: #b91c1c; }
@media (max-width: 768px) {
  .page-header { flex-direction: column; align-items: stretch; }
}
</style>
