<template>
  <AdminLayout>
    <div class="stock-price-daily-page">
      <div class="page-header">
        <div>
          <h2>每日行情管理</h2>
          <p>Step 1 先提供 CRUD 與 seed 驗證，後續再擴充匯入/覆蓋/重算。</p>
        </div>
        <button class="btn btn-primary" @click="openCreateModal">+ 新增行情</button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>總筆數</span>
          <strong>{{ pagination.totalElements }}</strong>
          <p>每日行情資料總筆數。</p>
        </article>
        <article class="overview-card">
          <span>本頁成交量合計</span>
          <strong>{{ formatVolume(pageVolume) }}</strong>
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
              <label>股票</label>
              <select v-model="filters.stockId">
                <option value="">全部</option>
                <option v-for="stock in stockOptions" :key="stock.id" :value="stock.id">
                  {{ stock.ticker }} - {{ stock.name }}
                </option>
              </select>
            </div>
            <div class="filter-group">
              <label>代碼</label>
              <input v-model="filters.ticker" type="text" placeholder="輸入 ticker" />
            </div>
            <div class="filter-group">
              <label>日期起</label>
              <input v-model="filters.tradeDateFrom" type="date" />
            </div>
            <div class="filter-group">
              <label>日期迄</label>
              <input v-model="filters.tradeDateTo" type="date" />
            </div>
            <div class="filter-group actions">
              <button class="btn btn-secondary" @click="resetFilters">清除條件</button>
            </div>
          </div>
        </div>
      </details>

      <div class="card surface-card table-wrap">
        <div v-if="rows.length === 0" class="empty-state">
          {{ pagination.totalElements === 0 ? '尚無每日行情資料' : '沒有符合條件的資料' }}
        </div>
        <div v-else>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>日期</th>
                <th>股票</th>
                <th>開</th>
                <th>高</th>
                <th>低</th>
                <th>收</th>
                <th>漲跌</th>
                <th>漲跌%</th>
                <th>成交量</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td>{{ row.id }}</td>
                <td>{{ row.tradeDate }}</td>
                <td>{{ row.ticker }}<br /><small>{{ row.stockName }}</small></td>
                <td>{{ formatPrice(row.openPrice) }}</td>
                <td>{{ formatPrice(row.highPrice) }}</td>
                <td>{{ formatPrice(row.lowPrice) }}</td>
                <td>{{ formatPrice(row.closePrice) }}</td>
                <td>{{ formatSignedPrice(row.changeAmount) }}</td>
                <td>{{ formatPercent(row.changePercent) }}</td>
                <td>{{ formatVolume(row.volume) }}</td>
                <td>
                  <div class="table-actions">
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
            page-size-id="daily-page-size"
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
            <h3 class="modal-title">{{ editingId ? '編輯每日行情' : '新增每日行情' }}</h3>
            <button class="btn-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
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
                <label>交易日期</label>
                <input v-model="form.tradeDate" type="date" />
              </div>
              <div class="filter-group">
                <label>開盤價</label>
                <input v-model.number="form.openPrice" type="number" step="0.0001" min="0" />
              </div>
              <div class="filter-group">
                <label>最高價</label>
                <input v-model.number="form.highPrice" type="number" step="0.0001" min="0" />
              </div>
              <div class="filter-group">
                <label>最低價</label>
                <input v-model.number="form.lowPrice" type="number" step="0.0001" min="0" />
              </div>
              <div class="filter-group">
                <label>收盤價</label>
                <input v-model.number="form.closePrice" type="number" step="0.0001" min="0" />
              </div>
              <div class="filter-group">
                <label>成交量</label>
                <input v-model.number="form.volume" type="number" min="0" />
              </div>
              <div class="filter-group">
                <label>漲跌金額</label>
                <input v-model.number="form.changeAmount" type="number" step="0.0001" />
              </div>
              <div class="filter-group">
                <label>漲跌幅 (%)</label>
                <input v-model.number="form.changePercent" type="number" step="0.0001" />
              </div>
            </div>
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
import { toast } from '@shared/composables/useToast'
import AdminLayout from '@/components/AdminLayout.vue'
import { investApiService } from '@/composables/useInvestApi'

const rows = ref([])
const stockOptions = ref([])
const showModal = ref(false)
const editingId = ref(null)
const jumpPage = ref(1)

const filters = reactive({
  stockId: '',
  ticker: '',
  tradeDateFrom: '',
  tradeDateTo: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const form = reactive({
  stockId: '',
  tradeDate: '',
  openPrice: 0,
  highPrice: 0,
  lowPrice: 0,
  closePrice: 0,
  volume: 0,
  changeAmount: 0,
  changePercent: 0
})

const pageVolume = computed(() => rows.value.reduce((sum, row) => sum + Number(row.volume || 0), 0))

const buildParams = () => {
  const params = {
    page: pagination.page - 1,
    size: pagination.size
  }
  if (filters.stockId) params.stockId = filters.stockId
  if (filters.ticker) params.ticker = filters.ticker
  if (filters.tradeDateFrom) params.tradeDateFrom = filters.tradeDateFrom
  if (filters.tradeDateTo) params.tradeDateTo = filters.tradeDateTo
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
    const data = await investApiService.getStockPriceDailiesPaged(buildParams())
    rows.value = data?.content || []
    pagination.totalElements = Number(data?.totalElements || 0)
    pagination.totalPages = Number(data?.totalPages || 1)
    if (pagination.page > pagination.totalPages) {
      pagination.page = pagination.totalPages || 1
    }
  } catch (error) {
    toast.error(`載入每日行情失敗: ${error.message || '未知錯誤'}`)
  }
}

const resetFilters = () => {
  filters.stockId = ''
  filters.ticker = ''
  filters.tradeDateFrom = ''
  filters.tradeDateTo = ''
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

const openCreateModal = () => {
  editingId.value = null
  Object.assign(form, {
    stockId: '',
    tradeDate: '',
    openPrice: 0,
    highPrice: 0,
    lowPrice: 0,
    closePrice: 0,
    volume: 0,
    changeAmount: 0,
    changePercent: 0
  })
  showModal.value = true
}

const openEditModal = (row) => {
  editingId.value = row.id
  Object.assign(form, {
    stockId: row.stockId,
    tradeDate: row.tradeDate,
    openPrice: Number(row.openPrice || 0),
    highPrice: Number(row.highPrice || 0),
    lowPrice: Number(row.lowPrice || 0),
    closePrice: Number(row.closePrice || 0),
    volume: Number(row.volume || 0),
    changeAmount: Number(row.changeAmount || 0),
    changePercent: Number(row.changePercent || 0)
  })
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const submitForm = async () => {
  try {
    const payload = {
      stockId: Number(form.stockId),
      tradeDate: form.tradeDate,
      openPrice: Number(form.openPrice),
      highPrice: Number(form.highPrice),
      lowPrice: Number(form.lowPrice),
      closePrice: Number(form.closePrice),
      volume: Number(form.volume),
      changeAmount: Number(form.changeAmount),
      changePercent: Number(form.changePercent)
    }

    if (editingId.value) {
      await investApiService.updateStockPriceDaily(editingId.value, payload)
      toast.success('每日行情更新成功')
    } else {
      await investApiService.createStockPriceDaily(payload)
      toast.success('每日行情新增成功')
    }

    closeModal()
    loadRows()
  } catch (error) {
    toast.error(`儲存每日行情失敗: ${error.message || '未知錯誤'}`)
  }
}

const deleteRow = async (row) => {
  if (!confirm(`確定要刪除 ${row.ticker} ${row.tradeDate} 的行情資料嗎？`)) {
    return
  }

  try {
    await investApiService.deleteStockPriceDaily(row.id)
    toast.success('每日行情刪除成功')
    loadRows()
  } catch (error) {
    toast.error(`刪除每日行情失敗: ${error.message || '未知錯誤'}`)
  }
}

const formatPrice = (value) => Number(value || 0).toFixed(4)
const formatSignedPrice = (value) => {
  const number = Number(value || 0)
  const sign = number >= 0 ? '+' : '-'
  return `${sign}${Math.abs(number).toFixed(4)}`
}
const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`
const formatVolume = (value) => Number(value || 0).toLocaleString('zh-TW')

watch(() => [filters.stockId, filters.ticker, filters.tradeDateFrom, filters.tradeDateTo], () => {
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
.stock-price-daily-page { display: flex; flex-direction: column; gap: 14px; }
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
</style>
