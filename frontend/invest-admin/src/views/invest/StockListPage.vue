<template>
  <AdminLayout>
    <div class="stock-list-page">
      <div class="page-header">
        <div>
          <h2>股票主資料管理</h2>
          <p>管理股票基本資訊（market / ticker / name / industry）。</p>
        </div>
        <button class="btn btn-primary" @click="openCreateModal">+ 新增股票</button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>總筆數</span>
          <strong>{{ pagination.totalElements }}</strong>
          <p>股票主資料總數。</p>
        </article>
        <article class="overview-card">
          <span>啟用中</span>
          <strong>{{ activeCount }}</strong>
          <p>目前仍啟用的股票筆數。</p>
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
              <label>市場</label>
              <select v-model="filters.market">
                <option value="">全部</option>
                <option value="TW">TW</option>
                <option value="US">US</option>
              </select>
            </div>
            <div class="filter-group">
              <label>代碼</label>
              <input v-model="filters.ticker" type="text" placeholder="輸入 ticker" />
            </div>
            <div class="filter-group">
              <label>名稱</label>
              <input v-model="filters.name" type="text" placeholder="輸入名稱" />
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
          {{ pagination.totalElements === 0 ? '尚無股票資料' : '沒有符合條件的資料' }}
        </div>
        <div v-else>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>市場</th>
                <th>代碼</th>
                <th>名稱</th>
                <th>產業</th>
                <th>狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td>{{ row.id }}</td>
                <td>{{ row.market }}</td>
                <td>{{ row.ticker }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.industry || '-' }}</td>
                <td>
                  <span :class="row.isActive ? 'status-active' : 'status-inactive'">
                    {{ row.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
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
            page-size-id="stock-page-size"
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
            <h3 class="modal-title">{{ editingId ? '編輯股票' : '新增股票' }}</h3>
            <button class="btn-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="filter-group">
                <label>市場</label>
                <select v-model="form.market">
                  <option value="TW">TW</option>
                  <option value="US">US</option>
                </select>
              </div>
              <div class="filter-group">
                <label>代碼</label>
                <input v-model="form.ticker" type="text" placeholder="例如 2330 / AAPL" />
              </div>
              <div class="filter-group">
                <label>名稱</label>
                <input v-model="form.name" type="text" placeholder="股票名稱" />
              </div>
              <div class="filter-group">
                <label>產業</label>
                <input v-model="form.industry" type="text" placeholder="可留空" />
              </div>
              <div class="filter-group">
                <label>狀態</label>
                <select v-model="form.isActive">
                  <option :value="true">啟用</option>
                  <option :value="false">停用</option>
                </select>
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
const showModal = ref(false)
const editingId = ref(null)
const jumpPage = ref(1)

const filters = reactive({
  market: '',
  ticker: '',
  name: '',
  isActive: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const form = reactive({
  market: 'TW',
  ticker: '',
  name: '',
  industry: '',
  isActive: true
})

const activeCount = computed(() => rows.value.filter(row => row.isActive).length)

const buildParams = () => {
  const params = {
    page: pagination.page - 1,
    size: pagination.size
  }
  if (filters.market) params.market = filters.market
  if (filters.ticker) params.ticker = filters.ticker
  if (filters.name) params.name = filters.name
  if (filters.isActive !== '') params.isActive = filters.isActive
  return params
}

const loadRows = async () => {
  try {
    const data = await investApiService.getStocksPaged(buildParams())
    rows.value = data?.content || []
    pagination.totalElements = Number(data?.totalElements || 0)
    pagination.totalPages = Number(data?.totalPages || 1)
    if (pagination.page > pagination.totalPages) {
      pagination.page = pagination.totalPages || 1
    }
  } catch (error) {
    toast.error(`載入股票資料失敗: ${error.message || '未知錯誤'}`)
  }
}

const resetFilters = () => {
  filters.market = ''
  filters.ticker = ''
  filters.name = ''
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

const openCreateModal = () => {
  editingId.value = null
  Object.assign(form, {
    market: 'TW',
    ticker: '',
    name: '',
    industry: '',
    isActive: true
  })
  showModal.value = true
}

const openEditModal = (row) => {
  editingId.value = row.id
  Object.assign(form, {
    market: row.market,
    ticker: row.ticker,
    name: row.name,
    industry: row.industry || '',
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
      market: form.market,
      ticker: form.ticker,
      name: form.name,
      industry: form.industry,
      isActive: form.isActive
    }

    if (editingId.value) {
      await investApiService.updateStock(editingId.value, payload)
      toast.success('股票更新成功')
    } else {
      await investApiService.createStock(payload)
      toast.success('股票新增成功')
    }

    closeModal()
    loadRows()
  } catch (error) {
    toast.error(`儲存股票失敗: ${error.message || '未知錯誤'}`)
  }
}

const deleteRow = async (row) => {
  if (!confirm(`確定要刪除 ${row.ticker} 嗎？`)) {
    return
  }

  try {
    await investApiService.deleteStock(row.id)
    toast.success('股票刪除成功')
    loadRows()
  } catch (error) {
    toast.error(`刪除股票失敗: ${error.message || '未知錯誤'}`)
  }
}

watch(() => [filters.market, filters.ticker, filters.name, filters.isActive], () => {
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

onMounted(loadRows)
</script>

<style scoped>
.stock-list-page { display: flex; flex-direction: column; gap: 14px; }
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
.table-wrap { padding: 14px; }
.table-actions { display: flex; gap: 6px; }
.empty-state { text-align: center; color: var(--text-secondary); padding: 18px 10px; }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 10px; }
.modal-actions { margin-top: 16px; display: flex; justify-content: flex-end; gap: 8px; }
</style>
