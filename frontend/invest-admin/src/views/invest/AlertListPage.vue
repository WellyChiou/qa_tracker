<template>
  <AdminLayout>
    <div class="alert-list-page">
      <div class="page-header">
        <div>
          <h2>警示事件</h2>
          <p>僅監控你的持股，顯示停損、跌幅與短時異常下跌警示。</p>
        </div>
        <button
          v-if="canRunAlertPolling"
          class="btn btn-primary"
          :disabled="runningPolling"
          @click="runAlertPollingNow"
        >
          {{ runningPolling ? '執行中...' : '手動執行警示輪詢' }}
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
              <label>持股</label>
              <select v-model="filters.portfolioId">
                <option value="">全部</option>
                <option v-for="item in portfolioOptions" :key="item.id" :value="item.id">
                  {{ item.ticker }} - {{ item.stockName }}
                </option>
              </select>
            </div>
            <div class="filter-group">
              <label>觸發類型</label>
              <select v-model="filters.triggerType">
                <option value="">全部</option>
                <option value="STOP_LOSS">跌破停損</option>
                <option value="DROP_PERCENT">跌幅超標</option>
                <option value="ABNORMAL_DROP">短時異常下跌</option>
              </select>
            </div>
            <div class="filter-group actions">
              <button class="btn btn-secondary" @click="resetFilters">清除條件</button>
            </div>
          </div>
        </div>
      </details>

      <section class="card surface-card">
        <div class="section-header">
          <h3>警示事件列表</h3>
        </div>
        <div v-if="rows.length === 0" class="empty-state">
          {{ pagination.totalElements === 0 ? '目前沒有警示事件' : '沒有符合條件的警示事件' }}
        </div>
        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>觸發時間</th>
                <th>股票</th>
                <th>類型</th>
                <th>嚴重度</th>
                <th>數值</th>
                <th>訊息</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td>{{ formatDateTime(row.triggeredAt) }}</td>
                <td>{{ row.ticker }}<br /><small>{{ row.stockName }}</small></td>
                <td>{{ formatTriggerType(row.triggerType) }}</td>
                <td>
                  <span :class="row.severity === 'HIGH' ? 'severity-high' : 'severity-normal'">
                    {{ row.severity === 'HIGH' ? '高' : '一般' }}
                  </span>
                </td>
                <td>{{ formatTriggerValue(row.triggerType, row.triggerValue) }}</td>
                <td>{{ row.message }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <PaginationControls
          v-model:pageSize="pagination.size"
          v-model:jumpPage="jumpPage"
          :total-records="pagination.totalElements"
          :current-page="pagination.page"
          :total-pages="pagination.totalPages"
          :page-size-options="[10, 20, 50]"
          page-size-id="alerts-page-size"
          @first="goFirstPage"
          @previous="goPreviousPage"
          @next="goNextPage"
          @last="goLastPage"
          @jump="goJumpPage"
        />
      </section>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { investApiService } from '@/composables/useInvestApi'
import { useAuth } from '@/composables/useAuth'
import { toast } from '@shared/composables/useToast'

const rows = ref([])
const portfolioOptions = ref([])
const runningPolling = ref(false)
const jumpPage = ref(1)
const { currentUser } = useAuth()

const filters = reactive({
  portfolioId: '',
  triggerType: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const canRunAlertPolling = computed(() => {
  const permissions = currentUser.value?.permissions
  return Array.isArray(permissions) && permissions.includes('INVEST_JOB_RUN_ALERT_POLLING')
})

const buildParams = () => {
  const params = {
    page: pagination.page - 1,
    size: pagination.size
  }
  if (filters.portfolioId) params.portfolioId = Number(filters.portfolioId)
  if (filters.triggerType) params.triggerType = filters.triggerType
  return params
}

const loadPortfolioOptions = async () => {
  const data = await investApiService.getPortfoliosAll({ isActive: true })
  portfolioOptions.value = Array.isArray(data) ? data : []
}

const loadRows = async () => {
  const data = await investApiService.getPortfolioAlertEventsPaged(buildParams())
  rows.value = data?.content || []
  pagination.totalElements = Number(data?.totalElements || 0)
  pagination.totalPages = Number(data?.totalPages || 1)
  jumpPage.value = pagination.page
}

const runAlertPollingNow = async () => {
  runningPolling.value = true
  try {
    const result = await investApiService.runAlertPolling()
    toast.success(result?.message || '警示輪詢完成')
    await loadRows()
  } catch (error) {
    toast.error(`警示輪詢失敗: ${error.message || '未知錯誤'}`)
  } finally {
    runningPolling.value = false
  }
}

const resetFilters = () => {
  filters.portfolioId = ''
  filters.triggerType = ''
  pagination.page = 1
}

const goFirstPage = () => { pagination.page = 1 }
const goPreviousPage = () => { if (pagination.page > 1) pagination.page -= 1 }
const goNextPage = () => { if (pagination.page < pagination.totalPages) pagination.page += 1 }
const goLastPage = () => { pagination.page = pagination.totalPages }
const goJumpPage = () => {
  const page = Number(jumpPage.value || 1)
  if (page < 1 || page > pagination.totalPages) return
  pagination.page = page
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return value.replace('T', ' ')
}

const formatTriggerType = (code) => {
  const map = {
    STOP_LOSS: '跌破停損',
    DROP_PERCENT: '跌幅超標',
    ABNORMAL_DROP: '短時異常下跌'
  }
  return map[code] || code || '-'
}

const formatTriggerValue = (type, value) => {
  const num = Number(value || 0)
  if (type === 'DROP_PERCENT' || type === 'ABNORMAL_DROP') {
    return `${num.toFixed(2)}%`
  }
  return `${num.toFixed(4)}`
}

watch(
  () => [filters.portfolioId, filters.triggerType],
  () => {
    pagination.page = 1
    loadRows()
  }
)

watch(
  () => [pagination.page, pagination.size],
  () => {
    loadRows()
  }
)

onMounted(async () => {
  try {
    await loadPortfolioOptions()
    await loadRows()
  } catch (error) {
    toast.error(`載入警示事件失敗: ${error.message || '未知錯誤'}`)
  }
})
</script>

<style scoped>
.alert-list-page { display: flex; flex-direction: column; gap: 14px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.page-header h2 { margin: 0; font-size: 1.7rem; }
.page-header p { margin: 6px 0 0; color: var(--text-secondary); }
.filters { border: 1px solid var(--border-color); border-radius: 14px; background: #fff; }
.filters summary { cursor: pointer; list-style: none; }
.filters summary::-webkit-details-marker { display: none; }
.filters__title { padding: 12px 14px; }
.filters__title h3 { margin: 0; }
.filters__content { padding: 0 14px 14px; }
.filter-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 10px; align-items: end; }
.filter-group label { display: block; margin-bottom: 5px; color: var(--text-secondary); font-size: 0.86rem; }
.filter-group select { width: 100%; }
.table-wrap { overflow-x: auto; }
.empty-state { text-align: center; color: var(--text-secondary); padding: 16px; }
.severity-high { color: #b91c1c; font-weight: 700; }
.severity-normal { color: #0f172a; font-weight: 600; }
.pagination { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-top: 12px; }
.pagination-left, .pagination-right { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.pagination-label, .pagination-info { color: var(--text-secondary); font-size: 0.88rem; }
.page-size-select { min-width: 90px; }
.page-jump { display: flex; align-items: center; gap: 8px; }
.page-input { width: 68px; text-align: center; }
</style>
