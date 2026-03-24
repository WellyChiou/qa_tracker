<template>
  <AdminLayout>
    <div class="admin-church-info">
      <div class="page-header">
        <div>
          <h2>教會資訊管理</h2>
          <p>管理公開站會用到的基本資訊與顯示順序。</p>
        </div>
        <button @click="openAddModal" class="btn btn-primary">新增資訊</button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前資訊</span>
          <strong>{{ totalRecords }}</strong>
          <p>目前已建立的教會資訊資料總數。</p>
        </article>
        <article class="overview-card">
          <span>當前頁面</span>
          <strong>{{ filteredList.length }}</strong>
          <p>這一頁實際符合條件的資訊資料。</p>
        </article>
        <article class="overview-card">
          <span>查詢狀態</span>
          <strong>{{ filters.infoKey || filters.infoValue || filters.isActive !== '' ? '已套用' : '全部' }}</strong>
          <p>可透過鍵值、內容與狀態快速收斂教會資訊。</p>
        </article>
      </section>

      <!-- 查詢條件 -->
      <details class="filters filters--collapsible" open>
        <summary>
          <div class="filters__title">
            <h3>查詢條件</h3>
            <span class="filters__badge">點擊可收合</span>
          </div>
          <div class="filters__chev" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 9l6 6 6-6"/>
            </svg>
          </div>
        </summary>
        <div class="filters__content">
        <div class="filter-grid">
          <div class="filter-group">
            <label>資訊鍵值</label>
            <input
              type="text"
              v-model="filters.infoKey"
              placeholder="輸入資訊鍵值"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>資訊內容關鍵字</label>
            <input
              type="text"
              v-model="filters.infoValue"
              placeholder="輸入資訊內容關鍵字"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>啟用狀態</label>
            <select v-model="filters.isActive">
              <option value="">全部</option>
              <option :value="true">啟用</option>
              <option :value="false">停用</option>
            </select>
          </div>
          <div class="filter-group">
            <button @click="resetFilters" class="btn btn-secondary">清除條件</button>
          </div>
        </div>
        </div>
      </details>

      <div class="church-info-list card surface-card">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ churchInfoList.length === 0 ? '尚無教會資訊資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>資訊列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>資訊鍵值</th>
                <th>資訊內容</th>
                <th>顯示順序</th>
                <th>啟用狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="info in filteredList" :key="info.id">
                <td>{{ info.infoKey }}</td>
                <td class="info-value">{{ info.infoValue || '-' }}</td>
                <td>{{ info.displayOrder }}</td>
                <td>
                  <span :class="info.isActive ? 'status-active' : 'status-inactive'">
                    {{ info.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td><div class="table-actions"><button @click="editInfo(info)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                  <button @click="deleteInfo(info.id)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
              </tr>
            </tbody>
          </table>
          
          <!-- 分頁 -->
          <PaginationControls
            v-model:pageSize="recordsPerPage"
            v-model:jumpPage="jumpPage"
            :total-records="totalRecords"
            :current-page="currentPage"
            :total-pages="totalPages"
            @first="firstPage"
            @previous="previousPage"
            @next="nextPage"
            @last="lastPage"
            @jump="jumpToPage"
          />
        </div>
      </div>

      <!-- 新增/編輯 Modal -->
      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-panel" @click.stop style="max-width: 600px;">
          <div class="modal-header">
            <h2 class="modal-title">{{ editingInfo ? '編輯教會資訊' : '新增教會資訊' }}</h2>
            <button class="btn-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveInfo">
              <div class="form-group">
                <label>資訊鍵值 <span class="required">*</span></label>
                <input
                  type="text"
                  v-model="formData.infoKey"
                  :disabled="editingInfo"
                  required
                  placeholder="例如：address, phone, email"
                  class="form-input"
                />
                <small class="form-hint">唯一識別碼，用於程式碼中引用</small>
              </div>
              <div class="form-group">
                <label>資訊內容 <span class="required">*</span></label>
                <textarea
                  v-model="formData.infoValue"
                  required
                  rows="4"
                  placeholder="輸入資訊內容"
                  class="form-input"
                ></textarea>
              </div>
              <div class="form-group">
                <label>顯示順序</label>
                <input
                  type="number"
                  v-model.number="formData.displayOrder"
                  min="0"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    v-model="formData.isActive"
                    class="checkbox-input"
                  />
                  <span>啟用</span>
                </label>
              </div>
              <div class="form-actions">
                <button type="submit" class="btn btn-primary">儲存</button>
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
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { apiRequest } from '@/utils/api'

const churchInfoList = ref([])
const showModal = ref(false)
const editingInfo = ref(null)
const formData = ref({
  infoKey: '',
  infoValue: '',
  displayOrder: 0,
  isActive: true,
  infoType: 'text'
})

// 查詢條件
const filters = ref({
  infoKey: '',
  infoValue: '',
  isActive: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)
const totalRecords = ref(0)
const totalPages = ref(1)


// 第一頁
const firstPage = () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadChurchInfo()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadChurchInfo()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadChurchInfo()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadChurchInfo()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadChurchInfo()
  } else {
    jumpPage.value = currentPage.value
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    infoKey: '',
    infoValue: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 過濾後的列表（用於顯示）
const filteredList = computed(() => {
  if (!churchInfoList.value || !Array.isArray(churchInfoList.value)) {
    return []
  }
  return churchInfoList.value
})

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => [filters.value.infoKey, filters.value.infoValue, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadChurchInfo()
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadChurchInfo()
})

const loadChurchInfo = async () => {
  try {
    const url = `/church/admin/church-info?page=${currentPage.value - 1}&size=${recordsPerPage.value}`
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(url, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 處理多種可能的數據結構
      let infoData = []
      if (data.success && data.data) {
        infoData = data.data
      } else if (data.data) {
        infoData = data.data
      } else if (data.content) {
        infoData = data.content
      } else if (Array.isArray(data)) {
        infoData = data
      }
      
      churchInfoList.value = Array.isArray(infoData) ? infoData : []
      
      // 更新分頁信息
      if (data.totalElements !== undefined) {
        totalRecords.value = data.totalElements
        totalPages.value = data.totalPages || 1
        // 確保 currentPage 不超過 totalPages
        if (currentPage.value > totalPages.value) {
          currentPage.value = totalPages.value
          jumpPage.value = totalPages.value
        }
        // 同步 jumpPage 與 currentPage
        jumpPage.value = currentPage.value
      } else {
        totalRecords.value = churchInfoList.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
      }
      toast.success(`載入成功，共 ${totalRecords.value} 筆資訊`)
    } else {
      // 確保即使 API 失敗，列表也是空陣列
      churchInfoList.value = []
      toast.error('載入教會資訊失敗')
    }
  } catch (error) {
    console.error('載入教會資訊失敗:', error)
    toast.error('載入教會資訊失敗: ' + error.message)
    // 確保即使發生錯誤，列表也是空陣列
    churchInfoList.value = []
  }
}

const openAddModal = () => {
  editingInfo.value = null
  formData.value = {
    infoKey: '',
    infoValue: '',
    displayOrder: 0,
    isActive: true,
    infoType: 'text'
  }
  showModal.value = true
}

const editInfo = (info) => {
  editingInfo.value = info
  formData.value = {
    infoKey: info.infoKey,
    infoValue: info.infoValue || '',
    displayOrder: info.displayOrder || 0,
    isActive: info.isActive !== undefined ? info.isActive : true,
    infoType: info.infoType || 'text'
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingInfo.value = null
  formData.value = {
    infoKey: '',
    infoValue: '',
    displayOrder: 0,
    isActive: true,
    infoType: 'text'
  }
}

const saveInfo = async () => {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/admin/church-info', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify(formData.value)
    })
    
    if (data) {
      if (data.success) {
        toast.success('儲存成功')
        closeModal()
        loadChurchInfo()
      } else {
        toast.error('儲存失敗: ' + (data.message || '未知錯誤'))
      }
    }
  } catch (error) {
    console.error('儲存教會資訊失敗:', error)
    toast.error('儲存教會資訊失敗: ' + error.message)
  }
}

const deleteInfo = async (id) => {
  if (!confirm('確定要刪除這筆教會資訊嗎？')) {
    return
  }
  
  try {
    // 注意：這裡需要後端提供刪除 API，目前先提示
    toast.info('刪除功能需要後端 API 支援，請聯繫開發人員')
  } catch (error) {
    console.error('刪除教會資訊失敗:', error)
    toast.error('刪除教會資訊失敗: ' + error.message)
  }
}

onMounted(() => {
  loadChurchInfo()
})
</script>

<style scoped>
.admin-church-info{
  display:flex;
  flex-direction:column;
  gap:14px;
}

.overview-strip{
  display:grid;
  grid-template-columns:repeat(3, minmax(0, 1fr));
  gap:12px;
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
  color:white;
}

.overview-card--accent p{
  color:rgba(255,255,255,.76);
}

.surface-card{
  padding:16px;
}

/* Header */
.admin-church-info .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-church-info .page-header h2{
  font-size:18px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-church-info .page-header p,
.admin-church-info .subtitle,
.admin-church-info .description{
  color:var(--muted);
  font-weight:700;
  font-size:13px;
  margin-top:6px;
}

.overview-strip{
  gap:10px;
}

.overview-card{
  padding:13px;
  border-radius:14px;
}

.overview-card span{
  font-size:11px;
  letter-spacing:.08em;
}

.overview-card strong{
  margin-top:6px;
  font-size:22px;
}

.overview-card p{
  margin:6px 0 0;
  font-size:12px;
}

.surface-card{
  padding:14px;
}
/* Lists / table wrap */
.admin-church-info .table-container,
.admin-church-info .list-container,
.admin-church-info .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-church-info .table-container{ padding:0; }

/* Inline helpers */
.admin-church-info .hint,
.admin-church-info .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-church-info .actions,
.admin-church-info .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
  .overview-strip{
    grid-template-columns:1fr;
  }
}
</style>
