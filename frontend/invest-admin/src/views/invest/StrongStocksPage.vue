<template>
  <AdminLayout>
    <div class="strong-stocks-page">
      <div class="page-header">
        <div>
          <h2>強勢股分析</h2>
          <p>僅分析你的持股與預設觀察清單，提供可解釋的強度分數與觀察建議。</p>
        </div>
        <div class="header-actions">
          <select v-model="analysisScope">
            <option value="HOLDINGS_AND_WATCHLIST">持股 + 觀察清單</option>
            <option value="HOLDINGS_ONLY">僅持股</option>
          </select>
          <button
            v-if="canRunMarketAnalysis"
            class="btn btn-primary"
            :disabled="runningAnalysis"
            @click="runMarketAnalysisNow"
          >
            {{ runningAnalysis ? '分析中...' : '手動執行市場分析' }}
          </button>
        </div>
      </div>

      <section class="card surface-card watchlist-card">
        <div class="section-header">
          <h3>預設觀察清單</h3>
          <p v-if="defaultWatchlist">{{ defaultWatchlist.name }} / 共 {{ defaultWatchlist.activeItemCount || 0 }} 檔</p>
        </div>

        <div class="watchlist-toolbar">
          <select v-model="watchlistForm.stockId">
            <option value="">選擇股票</option>
            <option v-for="opt in stockOptions" :key="opt.id" :value="opt.id">
              {{ opt.ticker }} - {{ opt.name }}
            </option>
          </select>
          <input v-model="watchlistForm.note" type="text" placeholder="備註（可留空）" />
          <button class="btn btn-secondary" @click="addToWatchlist">新增標的</button>
        </div>

        <div v-if="watchlistItems.length === 0" class="empty-state">尚無觀察標的</div>
        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>股票</th>
                <th>市場</th>
                <th>備註</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in watchlistItems" :key="item.id">
                <td>{{ item.ticker }} - {{ item.stockName }}</td>
                <td>{{ item.market }}</td>
                <td>{{ item.note || '-' }}</td>
                <td>
                  <button class="btn btn-danger" @click="removeFromWatchlist(item)">移除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

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
              <label>強度等級</label>
              <select v-model="filters.strengthLevel">
                <option value="">全部</option>
                <option value="STRONG">強勢</option>
                <option value="GOOD">偏強</option>
                <option value="NEUTRAL">中性</option>
                <option value="WEAK">偏弱</option>
              </select>
            </div>
            <div class="filter-group">
              <label>來源範圍</label>
              <select v-model="filters.universeType">
                <option value="">全部</option>
                <option value="HOLDING">持股</option>
                <option value="WATCHLIST">觀察清單</option>
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
          <h3>強勢分析列表</h3>
        </div>

        <div v-if="rows.length === 0" class="empty-state">
          {{ pagination.totalElements === 0 ? '目前沒有強勢分析資料，可先執行市場分析。' : '沒有符合條件的資料。' }}
        </div>

        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>交易日</th>
                <th>股票</th>
                <th>範圍</th>
                <th>分數</th>
                <th>等級</th>
                <th>建議</th>
                <th>品質</th>
                <th>摘要</th>
                <th>因子（前 3）</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="row in rows" :key="row.id">
                <tr>
                  <td>{{ row.tradeDate }}</td>
                  <td>{{ row.ticker }}<br /><small>{{ row.stockName }}</small></td>
                  <td>{{ formatUniverse(row.universeType) }}</td>
                  <td>{{ row.strengthScore ?? '-' }}</td>
                  <td>{{ formatStrengthLevel(row.strengthLevel) }}</td>
                  <td>{{ formatRecommendation(row.recommendation) }}</td>
                  <td>{{ formatDataQuality(row.dataQuality) }}</td>
                  <td>{{ row.summary }}</td>
                  <td>
                    <ul class="mini-list">
                      <li v-for="factor in row.topFactors || []" :key="`${row.id}-${factor.factorCode}`">
                        {{ factor.factorName }} (+{{ factor.scoreContribution }})
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
                  <td colspan="10">
                    <div class="detail-panel">
                      <div class="detail-head">
                        <strong>因子明細</strong>
                        <button
                          v-if="(detailRows[row.id].factors || []).length > 3"
                          class="btn btn-secondary"
                          @click="toggleFactorsExpand(row.id)"
                        >
                          {{ expandedFactors[row.id] ? '只顯示前 3 筆' : '顯示全部' }}
                        </button>
                      </div>
                      <ul class="factor-list">
                        <li v-for="factor in visibleFactors(row.id)" :key="`${row.id}-${factor.factorCode}-${factor.factorName}`">
                          <div class="factor-title">
                            <strong>{{ factor.factorName }}</strong>
                            <span>+{{ factor.scoreContribution }}</span>
                          </div>
                          <p>原始值：{{ factor.rawValue || '-' }} / 門檻：{{ factor.threshold || '-' }}</p>
                          <p>{{ factor.explanation }}</p>
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
          page-size-id="strength-page-size"
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
const stockOptions = ref([])
const defaultWatchlist = ref(null)
const watchlistItems = ref([])
const jumpPage = ref(1)
const runningAnalysis = ref(false)
const analysisScope = ref('HOLDINGS_AND_WATCHLIST')
const detailRows = reactive({})
const expandedFactors = reactive({})

const watchlistForm = reactive({
  stockId: '',
  note: ''
})

const filters = reactive({
  tradeDate: '',
  strengthLevel: '',
  universeType: '',
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
  if (filters.strengthLevel) params.strengthLevel = filters.strengthLevel
  if (filters.universeType) params.universeType = filters.universeType
  if (filters.ticker) params.ticker = filters.ticker
  return params
}

const loadStockOptions = async () => {
  const data = await investApiService.getStockOptions()
  stockOptions.value = Array.isArray(data) ? data : []
}

const loadDefaultWatchlist = async () => {
  defaultWatchlist.value = await investApiService.getDefaultWatchlist()
}

const loadWatchlistItems = async () => {
  const data = await investApiService.getDefaultWatchlistItemsPaged({ page: 0, size: 200 })
  watchlistItems.value = data?.content || []
}

const loadRows = async () => {
  const data = await investApiService.getStrengthSnapshotsPaged(buildParams())
  rows.value = data?.content || []
  pagination.totalElements = Number(data?.totalElements || 0)
  pagination.totalPages = Number(data?.totalPages || 1)
  jumpPage.value = pagination.page
}

const runMarketAnalysisNow = async () => {
  runningAnalysis.value = true
  try {
    const result = await investApiService.runMarketAnalysis(analysisScope.value)
    toast.success(result?.message || '市場分析完成')
    await loadRows()
  } catch (error) {
    toast.error(`市場分析失敗: ${error.message || '未知錯誤'}`)
  } finally {
    runningAnalysis.value = false
  }
}

const addToWatchlist = async () => {
  if (!watchlistForm.stockId) {
    toast.error('請先選擇股票')
    return
  }

  try {
    await investApiService.addDefaultWatchlistItem({
      stockId: Number(watchlistForm.stockId),
      note: watchlistForm.note || null
    })
    watchlistForm.stockId = ''
    watchlistForm.note = ''
    await loadDefaultWatchlist()
    await loadWatchlistItems()
    toast.success('已加入預設觀察清單')
  } catch (error) {
    toast.error(`加入觀察清單失敗: ${error.message || '未知錯誤'}`)
  }
}

const removeFromWatchlist = async (item) => {
  try {
    await investApiService.removeDefaultWatchlistItem(item.id)
    await loadDefaultWatchlist()
    await loadWatchlistItems()
    toast.success('已從觀察清單移除')
  } catch (error) {
    toast.error(`移除失敗: ${error.message || '未知錯誤'}`)
  }
}

const toggleDetail = async (row) => {
  if (detailRows[row.id]) {
    delete detailRows[row.id]
    return
  }

  try {
    detailRows[row.id] = await investApiService.getStrengthSnapshotById(row.id)
  } catch (error) {
    toast.error(`載入明細失敗: ${error.message || '未知錯誤'}`)
  }
}

const toggleFactorsExpand = (rowId) => {
  expandedFactors[rowId] = !expandedFactors[rowId]
}

const visibleFactors = (rowId) => {
  const factors = detailRows[rowId]?.factors || []
  if (expandedFactors[rowId]) {
    return factors
  }
  return factors.slice(0, 3)
}

const resetFilters = () => {
  filters.tradeDate = ''
  filters.strengthLevel = ''
  filters.universeType = ''
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

const formatUniverse = (code) => {
  const map = {
    HOLDING: '持股',
    WATCHLIST: '觀察清單'
  }
  return map[code] || code || '-'
}

const formatStrengthLevel = (code) => {
  const map = {
    STRONG: '強勢',
    GOOD: '偏強',
    NEUTRAL: '中性',
    WEAK: '偏弱'
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

const formatDataQuality = (code) => {
  const map = {
    GOOD: '良好',
    PARTIAL: '部分缺漏',
    STALE: '資料過舊',
    INSUFFICIENT: '資料不足'
  }
  return map[code] || code || '-'
}

watch(
  () => [filters.tradeDate, filters.strengthLevel, filters.universeType, filters.ticker],
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
    await Promise.all([
      loadStockOptions(),
      loadDefaultWatchlist(),
      loadWatchlistItems(),
      loadRows()
    ])
  } catch (error) {
    toast.error(`載入強勢股頁失敗: ${error.message || '未知錯誤'}`)
  }
})
</script>

<style scoped>
.strong-stocks-page { display: flex; flex-direction: column; gap: 14px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.page-header h2 { margin: 0; font-size: 1.7rem; }
.page-header p { margin: 6px 0 0; color: var(--text-secondary); }
.header-actions { display: flex; align-items: center; gap: 8px; }
.watchlist-card { padding: 14px; }
.section-header h3 { margin: 0; }
.section-header p { margin: 6px 0 0; color: var(--text-secondary); }
.watchlist-toolbar { display: grid; grid-template-columns: 1fr 1fr auto; gap: 10px; margin-top: 12px; }
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
.detail-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.factor-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.factor-list li { border: 1px solid var(--border-color); border-radius: 8px; padding: 8px; }
.factor-title { display: flex; justify-content: space-between; align-items: center; }
.factor-list p { margin: 6px 0 0; color: #475569; }
.disclaimer { margin-top: 8px; color: #475569; font-size: 0.86rem; }
.empty-state { text-align: center; color: var(--text-secondary); padding: 14px; }
@media (max-width: 960px) {
  .page-header { flex-direction: column; align-items: stretch; }
  .header-actions { flex-wrap: wrap; }
  .watchlist-toolbar { grid-template-columns: 1fr; }
}
</style>
