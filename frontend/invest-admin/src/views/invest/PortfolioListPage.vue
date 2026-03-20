<template>
  <AdminLayout>
    <div class="portfolio-list-page">
      <div class="page-header">
        <div>
          <h2>持股中心</h2>
          <p>管理持股成本、股數、現價與未實現損益。</p>
        </div>
        <button class="btn btn-primary" @click="openCreateModal">+ 新增持股</button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前筆數</span>
          <strong>{{ pagination.totalElements }}</strong>
          <p>持股資料總筆數。</p>
        </article>
        <article class="overview-card">
          <span>本頁總市值</span>
          <strong>{{ formatMoney(pageMarketValue) }}</strong>
          <p>本頁持股市值合計。</p>
        </article>
        <article class="overview-card">
          <span>本頁未實現損益</span>
          <strong :class="pagePnl >= 0 ? 'text-up' : 'text-down'">{{ formatSignedMoney(pagePnl) }}</strong>
          <p>僅統計本頁資料。</p>
        </article>
      </section>

      <details class="filters filters--collapsible" open>
        <summary>
          <div class="filters__title">
            <h3>查詢條件</h3>
          </div>
        </summary>
        <div class="filters__content">
          <div class="filter-grid">
            <div class="filter-group">
              <label>使用者</label>
              <input v-model="filters.userId" type="text" placeholder="留空使用目前登入者" />
            </div>
            <div class="filter-group">
              <label>股票</label>
              <select v-model="filters.stockId">
                <option value="">全部</option>
                <option v-for="stock in stockOptions" :key="stock.id" :value="stock.id">
                  {{ stock.ticker }} - {{ stock.name }}
                </option>
              </select>
            </div>
            <div class="filter-group">
              <label>狀態</label>
              <select v-model="filters.isActive">
                <option value="">全部</option>
                <option :value="true">啟用</option>
                <option :value="false">停用</option>
              </select>
            </div>
            <div class="filter-group actions">
              <button class="btn btn-secondary" @click="resetFilters">清除條件</button>
            </div>
          </div>
        </div>
      </details>

      <div class="card surface-card table-wrap">
        <div v-if="rows.length === 0" class="empty-state">
          {{ pagination.totalElements === 0 ? '尚無持股資料' : '沒有符合條件的資料' }}
        </div>
        <div v-else>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>股票</th>
                <th>使用者</th>
                <th>平均成本</th>
                <th>股數</th>
                <th>總成本</th>
                <th>現價</th>
                <th>市值</th>
                <th>未實現損益</th>
                <th>報酬率</th>
                <th>最新風險等級</th>
                <th>最新建議</th>
                <th>狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td>{{ row.id }}</td>
                <td>{{ row.ticker }}<br /><small>{{ row.stockName }}</small></td>
                <td>{{ row.userId }}</td>
                <td>{{ formatMoney(row.avgCost) }}</td>
                <td>{{ formatQty(row.quantity) }}</td>
                <td>{{ formatMoney(row.totalCost) }}</td>
                <td>{{ formatMoney(row.currentPrice) }}</td>
                <td>{{ formatMoney(row.marketValue) }}</td>
                <td :class="Number(row.unrealizedProfitLoss) >= 0 ? 'text-up' : 'text-down'">{{ formatSignedMoney(row.unrealizedProfitLoss) }}</td>
                <td :class="Number(row.unrealizedProfitLossPercent) >= 0 ? 'text-up' : 'text-down'">{{ formatPercent(row.unrealizedProfitLossPercent) }}</td>
                <td>{{ formatRiskLevel(row.latestRiskLevel) }}</td>
                <td>{{ formatRecommendation(row.latestRecommendation) }}</td>
                <td>
                  <span :class="row.isActive ? 'status-active' : 'status-inactive'">{{ row.isActive ? '啟用' : '停用' }}</span>
                </td>
                <td>
                  <div class="table-actions">
                    <button class="btn btn-primary" @click="openDetail(row.id)">明細</button>
                    <button class="btn btn-secondary" @click="openEditModal(row)">編輯</button>
                    <button class="btn btn-danger" @click="deleteRow(row)">刪除</button>
                  </div>
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
            page-size-id="portfolio-page-size"
            @first="goFirstPage"
            @previous="goPreviousPage"
            @next="goNextPage"
            @last="goLastPage"
            @jump="goJumpPage"
          />
        </div>
      </div>

      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-panel" @click.stop>
          <div class="modal-header">
            <h3 class="modal-title">{{ editingId ? '編輯持股' : '新增持股' }}</h3>
            <button class="btn-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="filter-group">
                <label>使用者 ID</label>
                <input v-model="form.userId" type="text" placeholder="留空會帶入目前登入者" />
              </div>
              <div class="filter-group">
                <label>股票</label>
                <select v-model="form.stockId">
                  <option disabled value="">請選擇股票</option>
                  <option v-for="stock in stockOptions" :key="stock.id" :value="stock.id">
                    {{ stock.ticker }} - {{ stock.name }}
                  </option>
                </select>
              </div>
              <div class="filter-group">
                <label>平均成本</label>
                <input v-model.number="form.avgCost" type="number" step="0.0001" min="0" />
              </div>
              <div class="filter-group">
                <label>股數</label>
                <input v-model.number="form.quantity" type="number" step="0.0001" min="0" />
              </div>
              <div class="filter-group">
                <label>備註</label>
                <input v-model="form.note" type="text" placeholder="可留空" />
              </div>
              <div class="filter-group">
                <label>狀態</label>
                <select v-model="form.isActive">
                  <option :value="true">啟用</option>
                  <option :value="false">停用</option>
                </select>
              </div>
            </div>
            <div class="hint">`totalCost` 依規則固定由 `avgCost * quantity` 計算（四捨五入到小數第 2 位）。</div>
            <div class="modal-actions">
              <button class="btn btn-primary" @click="submitForm">儲存</button>
              <button class="btn btn-secondary" @click="closeModal">取消</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from '@shared/composables/useToast'
import AdminLayout from '@/components/AdminLayout.vue'
import { investApiService } from '@/composables/useInvestApi'

const router = useRouter()
const rows = ref([])
const stockOptions = ref([])
const showModal = ref(false)
const editingId = ref(null)
const jumpPage = ref(1)

const filters = reactive({
  userId: '',
  stockId: '',
  isActive: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const form = reactive({
  userId: '',
  stockId: '',
  avgCost: 0,
  quantity: 0,
  note: '',
  isActive: true
})

const pageMarketValue = computed(() => rows.value.reduce((sum, row) => sum + Number(row.marketValue || 0), 0))
const pagePnl = computed(() => rows.value.reduce((sum, row) => sum + Number(row.unrealizedProfitLoss || 0), 0))

const buildParams = () => {
  const params = {
    page: pagination.page - 1,
    size: pagination.size
  }
  if (filters.userId) params.userId = filters.userId
  if (filters.stockId) params.stockId = filters.stockId
  if (filters.isActive !== '') params.isActive = filters.isActive
  return params
}

const loadStockOptions = async () => {
  try {
    stockOptions.value = await investApiService.getStockOptions()
  } catch (error) {
    toast.error(`載入股票選項失敗: ${error.message || '未知錯誤'}`)
  }
}

const loadRows = async () => {
  try {
    const data = await investApiService.getPortfoliosPaged(buildParams())
    rows.value = data?.content || []
    pagination.totalElements = Number(data?.totalElements || 0)
    pagination.totalPages = Number(data?.totalPages || 1)
    if (pagination.page > pagination.totalPages) {
      pagination.page = pagination.totalPages || 1
    }
  } catch (error) {
    toast.error(`載入持股資料失敗: ${error.message || '未知錯誤'}`)
  }
}

const resetFilters = () => {
  filters.userId = ''
  filters.stockId = ''
  filters.isActive = ''
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

const openDetail = (portfolioId) => {
  router.push(`/portfolios/${portfolioId}`)
}

const openCreateModal = () => {
  editingId.value = null
  Object.assign(form, {
    userId: '',
    stockId: '',
    avgCost: 0,
    quantity: 0,
    note: '',
    isActive: true
  })
  showModal.value = true
}

const openEditModal = (row) => {
  editingId.value = row.id
  Object.assign(form, {
    userId: row.userId,
    stockId: row.stockId,
    avgCost: Number(row.avgCost || 0),
    quantity: Number(row.quantity || 0),
    note: row.note || '',
    isActive: !!row.isActive
  })
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const submitForm = async () => {
  try {
    const payload = {
      userId: form.userId,
      stockId: Number(form.stockId),
      avgCost: Number(form.avgCost),
      quantity: Number(form.quantity),
      note: form.note,
      isActive: form.isActive
    }

    if (editingId.value) {
      await investApiService.updatePortfolio(editingId.value, payload)
      toast.success('持股更新成功')
    } else {
      await investApiService.createPortfolio(payload)
      toast.success('持股新增成功')
    }

    closeModal()
    loadRows()
  } catch (error) {
    toast.error(`儲存持股失敗: ${error.message || '未知錯誤'}`)
  }
}

const deleteRow = async (row) => {
  if (!confirm(`確定要刪除持股 ${row.ticker} 嗎？`)) {
    return
  }

  try {
    await investApiService.deletePortfolio(row.id)
    toast.success('持股刪除成功')
    loadRows()
  } catch (error) {
    toast.error(`刪除持股失敗: ${error.message || '未知錯誤'}`)
  }
}

const formatMoney = (value) => `$${Number(value || 0).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const formatSignedMoney = (value) => {
  const number = Number(value || 0)
  const sign = number >= 0 ? '+' : '-'
  return `${sign}${formatMoney(Math.abs(number))}`
}
const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`
const formatQty = (value) => Number(value || 0).toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 4 })
const formatRiskLevel = (code) => {
  if (!code) return '尚未分析'
  const map = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '極高'
  }
  return map[code] || code
}
const formatRecommendation = (code) => {
  if (!code) return '尚未分析'
  const map = {
    HOLD: '續抱',
    WATCH: '觀察',
    REDUCE: '減碼',
    STOP_LOSS_CHECK: '停損檢查'
  }
  return map[code] || code
}

watch(() => [filters.userId, filters.stockId, filters.isActive], () => {
  pagination.page = 1
  loadRows()
})

watch(() => pagination.size, () => {
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
})

watch(() => pagination.page, (page) => {
  jumpPage.value = page
})

onMounted(async () => {
  await loadStockOptions()
  await loadRows()
})
</script>

<style scoped>
.portfolio-list-page { display: flex; flex-direction: column; gap: 14px; }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.page-header h2 { margin: 0; font-size: 1.7rem; }
.page-header p { margin: 6px 0 0; color: var(--text-secondary); }
.overview-strip { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 12px; }
.overview-card { border: 1px solid var(--border-color); border-radius: 14px; padding: 14px; background: #fff; }
.overview-card--accent { background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(6, 182, 212, 0.1)); }
.overview-card span { color: var(--text-secondary); font-size: 0.85rem; }
.overview-card strong { display: block; margin: 6px 0; font-size: 1.2rem; }
.overview-card p { margin: 0; color: var(--text-secondary); font-size: 0.86rem; }
.filters__title h3 { margin: 0; }
.filter-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; }
.filter-group label { margin-bottom: 4px; font-size: 0.85rem; }
.filter-group.actions { display: flex; align-items: flex-end; }
.table-wrap { padding: 14px; overflow-x: auto; }
.table-actions { display: flex; gap: 6px; }
.empty-state { text-align: center; color: var(--text-secondary); padding: 18px 10px; }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 10px; }
.modal-actions { margin-top: 16px; display: flex; justify-content: flex-end; gap: 8px; }
.hint { margin-top: 10px; color: var(--text-secondary); font-size: 0.85rem; }
.text-up { color: #0f766e; }
.text-down { color: #b91c1c; }
</style>
