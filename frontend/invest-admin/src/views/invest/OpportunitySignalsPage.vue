<template>
  <AdminLayout>
    <div class="opportunity-page">
      <div class="page-header">
        <div>
          <h2>機會股觀察</h2>
          <p>僅輸出觀察型建議，不提供買進指令或保證式結論。</p>
        </div>
        <button
          v-if="canRunMarketAnalysis"
          class="btn btn-primary"
          :disabled="runningAnalysis"
          @click="runMarketAnalysisNow"
        >
          {{ runningAnalysis ? '分析中...' : '手動更新訊號' }}
        </button>
      </div>

      <details class="filters filters--collapsible" open>
        <summary>
          <div class="filters__title"><h3>查詢條件</h3></div>
        </summary>
        <div class="filters__content">
          <div class="filter-grid">
            <div class="filter-group">
              <label>交易日</label>
              <input v-model="filters.tradeDate" type="date" />
            </div>
            <div class="filter-group">
              <label>狀態</label>
              <select v-model="filters.status">
                <option value="">全部</option>
                <option value="ACTIVE">啟用中</option>
                <option value="EXPIRED">已過期</option>
              </select>
            </div>
            <div class="filter-group">
              <label>建議</label>
              <select v-model="filters.recommendation">
                <option value="">全部</option>
                <option value="OBSERVE">可觀察</option>
                <option value="WAIT_PULLBACK">等待回檔</option>
                <option value="REEVALUATE_WHEN_CONDITION_MET">條件成立再評估</option>
                <option value="NOT_SUITABLE_CHASE">不建議追價</option>
              </select>
            </div>
            <div class="filter-group">
              <label>訊號類型</label>
              <select v-model="filters.signalType">
                <option value="">全部</option>
                <option value="MOMENTUM_OBSERVE">強勢延續觀察</option>
                <option value="PULLBACK_WATCH">回檔觀察</option>
                <option value="BREAKOUT_REEVALUATE">條件成立再評估</option>
                <option value="HIGH_RISK_AVOID_CHASE">高風險不追價</option>
              </select>
            </div>
            <div class="filter-group">
              <label>代碼</label>
              <input v-model="filters.ticker" type="text" placeholder="輸入 ticker" />
            </div>
            <div class="filter-group actions">
              <button class="btn btn-secondary" @click="resetFilters">清除條件</button>
            </div>
          </div>
        </div>
      </details>

      <section class="card surface-card">
        <div class="section-header">
          <h3>機會訊號列表</h3>
        </div>

        <div v-if="rows.length === 0" class="empty-state">
          {{ pagination.totalElements === 0 ? '目前尚無機會訊號，可先執行市場分析。' : '沒有符合條件的資料。' }}
        </div>

        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>交易日</th>
                <th>股票</th>
                <th>訊號</th>
                <th>分數</th>
                <th>建議</th>
                <th>狀態</th>
                <th>摘要</th>
                <th>原因（前 3）</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="row in rows" :key="row.id">
                <tr>
                  <td>{{ row.tradeDate }}</td>
                  <td>{{ row.ticker }}<br /><small>{{ row.stockName }}</small></td>
                  <td>{{ formatSignalType(row.signalType) }}</td>
                  <td>{{ row.signalScore }}</td>
                  <td>{{ formatRecommendation(row.recommendation) }}</td>
                  <td>{{ formatStatus(row.status) }}</td>
                  <td>{{ row.summary }}</td>
                  <td>
                    <ul class="mini-list">
                      <li v-for="reason in row.topReasons || []" :key="`${row.id}-${reason.reasonTitle}`">
                        {{ reason.reasonTitle }}
                      </li>
                    </ul>
                  </td>
                  <td>
                    <button class="btn btn-secondary" @click="toggleDetail(row)">
                      {{ detailRows[row.id] ? '收合' : '明細' }}
                    </button>
                  </td>
                </tr>
                <tr v-if="detailRows[row.id]" class="detail-row">
                  <td colspan="9">
                    <div class="detail-panel">
                      <p><strong>條件說明：</strong>{{ detailRows[row.id].conditionText }}</p>
                      <ul class="reason-list">
                        <li v-for="reason in detailRows[row.id].reasons || []" :key="`${row.id}-${reason.reasonTitle}`">
                          <strong>{{ reason.reasonTitle }}</strong>
                          <p>{{ reason.reasonDetail }}</p>
                        </li>
                      </ul>
                      <p class="disclaimer">{{ detailRows[row.id].disclaimer }}</p>
                    </div>
                  </td>
                </tr>
              </template>
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
          page-size-id="opportunity-page-size"
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
import { toast } from '@shared/composables/useToast'
import { investApiService } from '@/composables/useInvestApi'
import { useAuth } from '@/composables/useAuth'
import AdminLayout from '@/components/AdminLayout.vue'

const rows = ref([])
const jumpPage = ref(1)
const runningAnalysis = ref(false)
const detailRows = reactive({})

const filters = reactive({
  tradeDate: '',
  status: '',
  recommendation: '',
  signalType: '',
  ticker: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const { currentUser } = useAuth()

const canRunMarketAnalysis = computed(() => {
  const permissions = currentUser.value?.permissions
  return Array.isArray(permissions) && permissions.includes('INVEST_JOB_RUN_MARKET_ANALYSIS')
})

const buildParams = () => {
  const params = {
    page: pagination.page - 1,
    size: pagination.size
  }
  if (filters.tradeDate) params.tradeDate = filters.tradeDate
  if (filters.status) params.status = filters.status
  if (filters.recommendation) params.recommendation = filters.recommendation
  if (filters.signalType) params.signalType = filters.signalType
  if (filters.ticker) params.ticker = filters.ticker
  return params
}

const loadRows = async () => {
  const data = await investApiService.getOpportunitySignalsPaged(buildParams())
  rows.value = data?.content || []
  pagination.totalElements = Number(data?.totalElements || 0)
  pagination.totalPages = Number(data?.totalPages || 1)
  jumpPage.value = pagination.page
}

const runMarketAnalysisNow = async () => {
  runningAnalysis.value = true
  try {
    const result = await investApiService.runMarketAnalysis('HOLDINGS_AND_WATCHLIST')
    toast.success(result?.message || '市場分析完成')
    await loadRows()
  } catch (error) {
    toast.error(`更新訊號失敗: ${error.message || '未知錯誤'}`)
  } finally {
    runningAnalysis.value = false
  }
}

const toggleDetail = async (row) => {
  if (detailRows[row.id]) {
    delete detailRows[row.id]
    return
  }

  try {
    detailRows[row.id] = await investApiService.getOpportunitySignalById(row.id)
  } catch (error) {
    toast.error(`載入明細失敗: ${error.message || '未知錯誤'}`)
  }
}

const resetFilters = () => {
  filters.tradeDate = ''
  filters.status = ''
  filters.recommendation = ''
  filters.signalType = ''
  filters.ticker = ''
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

const formatSignalType = (code) => {
  const map = {
    MOMENTUM_OBSERVE: '強勢延續觀察',
    PULLBACK_WATCH: '回檔觀察',
    BREAKOUT_REEVALUATE: '條件成立再評估',
    HIGH_RISK_AVOID_CHASE: '高風險不追價'
  }
  return map[code] || code || '-'
}

const formatRecommendation = (code) => {
  const map = {
    OBSERVE: '可觀察',
    WAIT_PULLBACK: '等待回檔',
    REEVALUATE_WHEN_CONDITION_MET: '條件成立再評估',
    NOT_SUITABLE_CHASE: '不建議追價'
  }
  return map[code] || code || '-'
}

const formatStatus = (code) => {
  const map = {
    ACTIVE: '啟用中',
    EXPIRED: '已過期'
  }
  return map[code] || code || '-'
}

watch(
  () => [filters.tradeDate, filters.status, filters.recommendation, filters.signalType, filters.ticker],
  async () => {
    pagination.page = 1
    await loadRows()
  }
)

watch(
  () => [pagination.page, pagination.size],
  async () => {
    await loadRows()
  }
)

onMounted(async () => {
  try {
    await loadRows()
  } catch (error) {
    toast.error(`載入機會訊號頁失敗: ${error.message || '未知錯誤'}`)
  }
})
</script>

<style scoped>
.opportunity-page { display: flex; flex-direction: column; gap: 14px; }
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
.table-wrap { overflow-x: auto; }
.mini-list { margin: 0; padding-left: 16px; }
.mini-list li { margin-bottom: 3px; }
.detail-row td { background: #f8fafc; }
.detail-panel { border: 1px solid var(--border-color); border-radius: 10px; padding: 10px; background: #fff; }
.reason-list { margin: 10px 0 0; padding-left: 18px; }
.reason-list li { margin-bottom: 8px; }
.reason-list p { margin: 4px 0 0; color: #475569; }
.disclaimer { margin-top: 8px; color: #475569; font-size: 0.86rem; }
.empty-state { text-align: center; color: var(--text-secondary); padding: 14px; }
@media (max-width: 960px) {
  .page-header { flex-direction: column; align-items: stretch; }
}
</style>
