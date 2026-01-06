<template>
  <AdminLayout>
    <div class="admin-church-info">
      <div class="page-header">
        <h2>教會資訊管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增資訊</button>
      </div>

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

      <div class="church-info-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ churchInfoList.length === 0 ? '尚無教會資訊資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>教會資訊列表 (共 {{ totalRecords }} 筆)</h3>
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
              <tr v-for="info in churchInfoList" :key="info.id">
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
          <div class="pagination">
            <div class="pagination-left">
              <label for="pageSize" class="pagination-label">顯示筆數：</label>
              <select id="pageSize" v-model.number="recordsPerPage" class="page-size-select">
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
                <option :value="100">100</option>
              </select>
              <span class="pagination-info">共 {{ totalRecords }} 筆 (第 {{ currentPage }}/{{ totalPages }} 頁)</span>
            </div>
            <div class="pagination-right">
              <button class="btn-secondary" @click="firstPage" :disabled="currentPage === 1" title="第一頁">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7"/>
                </svg>
              </button>
              <button class="btn-secondary" @click="previousPage" :disabled="currentPage === 1">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
                </svg>
                上一頁
              </button>
              <div class="page-jump">
                <span class="pagination-label">到第</span>
                <input type="number" v-model.number="jumpPage" min="1" :max="totalPages" class="page-input" @keyup.enter="jumpToPage" />
                <span class="pagination-label">頁</span>
              </div>
              <button class="btn-secondary" @click="nextPage" :disabled="currentPage === totalPages">
                下一頁
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </button>
              <button class="btn-secondary" @click="lastPage" :disabled="currentPage === totalPages" title="最後一頁">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7"/>
                </svg>
              </button>
            </div>
          </div>
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
import { toast } from '@/composables/useToast'
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

// 監聽查詢條件變化，重置到第一頁
watch(() => [filters.value.infoKey, filters.value.infoValue, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
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
    const response = await apiRequest(url, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        churchInfoList.value = data.data || data.content || []
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
      }
    }
  } catch (error) {
    console.error('載入教會資訊失敗:', error)
    toast.error('載入教會資訊失敗: ' + error.message)
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
    const response = await apiRequest('/church/admin/church-info', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify(formData.value)
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success) {
        toast.success('儲存成功')
        closeModal()
        loadChurchInfo()
      } else {
        toast.error('儲存失敗: ' + (data.message || '未知錯誤'))
      }
    } else {
      const error = await response.json()
      toast.error('儲存失敗: ' + (error.message || '未知錯誤'))
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
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-church-info .page-header p,
.admin-church-info .subtitle,
.admin-church-info .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
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
}
</style>
