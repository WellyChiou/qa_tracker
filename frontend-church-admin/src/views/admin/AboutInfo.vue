<template>
  <AdminLayout>
    <div class="admin-about-info">
      <div class="page-header">
        <h2>關於我們管理</h2>
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
            <label>區塊鍵值</label>
            <input
              type="text"
              v-model="filters.sectionKey"
              placeholder="輸入區塊鍵值"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>標題關鍵字</label>
            <input
              type="text"
              v-model="filters.title"
              placeholder="輸入標題關鍵字"
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

      <div class="about-info-list">
        <div v-if="aboutInfoList.length === 0" class="empty-state">
          <p>尚無關於我們資料</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>關於我們列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>區塊鍵值</th>
                <th>標題</th>
                <th>內容預覽</th>
                <th>顯示順序</th>
                <th>啟用狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="info in aboutInfoList" :key="info.id">
                <td>{{ info.sectionKey }}</td>
                <td>{{ info.title }}</td>
                <td class="content-preview">{{ (info.content || '').substring(0, 50) }}{{ (info.content || '').length > 50 ? '...' : '' }}</td>
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
        <div class="modal-panel" @click.stop style="max-width: 700px;">
          <div class="modal-header">
            <h2 class="modal-title">{{ editingInfo ? '編輯關於我們資訊' : '新增關於我們資訊' }}</h2>
            <button class="btn-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveInfo">
              <div class="form-group">
                <label>區塊鍵值 <span class="required">*</span></label>
                <input
                  type="text"
                  v-model="formData.sectionKey"
                  required
                  placeholder="例如：welcome, mission, vision"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label>標題 <span class="required">*</span></label>
                <input
                  type="text"
                  v-model="formData.title"
                  required
                  placeholder="輸入標題"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label>內容 <span class="required">*</span></label>
                <textarea
                  v-model="formData.content"
                  required
                  rows="8"
                  placeholder="輸入內容"
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

const aboutInfoList = ref([])
const showModal = ref(false)
const editingInfo = ref(null)
const formData = ref({
  sectionKey: '',
  title: '',
  content: '',
  displayOrder: 0,
  isActive: true
})

// 查詢條件
const filters = ref({
  sectionKey: '',
  title: '',
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
  loadAboutInfo()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadAboutInfo()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadAboutInfo()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadAboutInfo()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadAboutInfo()
  } else {
    jumpPage.value = currentPage.value
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    sectionKey: '',
    title: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
  loadAboutInfo()
}

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => [filters.value.sectionKey, filters.value.title, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadAboutInfo()
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadAboutInfo()
})

const loadAboutInfo = async () => {
  try {
    const params = new URLSearchParams()
    if (filters.value.sectionKey) {
      params.append('sectionKey', filters.value.sectionKey)
    }
    if (filters.value.title) {
      params.append('title', filters.value.title)
    }
    if (filters.value.isActive !== '') {
      params.append('isActive', filters.value.isActive === true || filters.value.isActive === 'true')
    }
    params.append('page', (currentPage.value - 1).toString())
    params.append('size', recordsPerPage.value.toString())
    
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/about-info?${params.toString()}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 處理 ApiResponse 格式或直接返回的資料
      let responseData = data
      if (data.success && data.data) {
        responseData = data.data
      } else if (data.data) {
        responseData = data.data
      }
      
      // 處理分頁資料
      let content = []
      if (responseData.content) {
        content = responseData.content
      } else if (Array.isArray(responseData)) {
        content = responseData
      } else if (responseData.data) {
        content = responseData.data
      }
      
      aboutInfoList.value = Array.isArray(content) ? content : []
      
      // 更新分頁信息
      if (responseData.totalElements !== undefined) {
        totalRecords.value = responseData.totalElements
        totalPages.value = responseData.totalPages || 1
        // 確保 currentPage 不超過 totalPages
        if (currentPage.value > totalPages.value) {
          currentPage.value = totalPages.value
          jumpPage.value = totalPages.value
        }
        // 同步 jumpPage 與 currentPage
        jumpPage.value = currentPage.value
      } else if (data.totalElements !== undefined) {
        totalRecords.value = data.totalElements
        totalPages.value = data.totalPages || 1
        if (currentPage.value > totalPages.value) {
          currentPage.value = totalPages.value
          jumpPage.value = totalPages.value
        }
        jumpPage.value = currentPage.value
      } else {
        totalRecords.value = aboutInfoList.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
      }
      toast.success(`載入成功，共 ${totalRecords.value} 筆資訊`)
    } else {
      aboutInfoList.value = []
      toast.error('載入關於我們資訊失敗')
    }
  } catch (error) {
    console.error('載入關於我們資訊失敗:', error)
    toast.error('載入關於我們資訊失敗: ' + error.message)
    toast.error('載入關於我們資訊失敗: ' + error.message)
  }
}

const openAddModal = () => {
  editingInfo.value = null
  formData.value = {
    sectionKey: '',
    title: '',
    content: '',
    displayOrder: 0,
    isActive: true
  }
  showModal.value = true
}

const editInfo = (info) => {
  editingInfo.value = info
  formData.value = {
    sectionKey: info.sectionKey || '',
    title: info.title || '',
    content: info.content || '',
    displayOrder: info.displayOrder || 0,
    isActive: info.isActive !== undefined ? info.isActive : true
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingInfo.value = null
  formData.value = {
    sectionKey: '',
    title: '',
    content: '',
    displayOrder: 0,
    isActive: true
  }
}

const saveInfo = async () => {
  try {
    const url = editingInfo.value 
      ? `/church/admin/about-info/${editingInfo.value.id}`
      : '/church/admin/about-info'
    const method = editingInfo.value ? 'PUT' : 'POST'
    
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(url, {
      method,
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
        loadAboutInfo()
      } else {
        toast.error('儲存失敗: ' + (data.message || '未知錯誤'))
      }
    }
  } catch (error) {
    console.error('儲存關於我們資訊失敗:', error)
    toast.error('儲存關於我們資訊失敗: ' + error.message)
  }
}

const deleteInfo = async (id) => {
  if (!confirm('確定要刪除這筆關於我們資訊嗎？')) {
    return
  }
  
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/about-info/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (data) {
      if (data.success) {
        toast.success('刪除成功')
        loadAboutInfo()
      } else {
        toast.error('刪除失敗: ' + (data.message || '未知錯誤'))
      }
    }
  } catch (error) {
    console.error('刪除關於我們資訊失敗:', error)
    toast.error('刪除關於我們資訊失敗: ' + error.message)
  }
}

onMounted(() => {
  loadAboutInfo()
})
</script>

<style scoped>
.admin-about-info{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-about-info .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-about-info .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-about-info .page-header p,
.admin-about-info .subtitle,
.admin-about-info .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-about-info .table-container,
.admin-about-info .list-container,
.admin-about-info .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-about-info .table-container{ padding:0; }

/* Inline helpers */
.admin-about-info .hint,
.admin-about-info .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-about-info .actions,
.admin-about-info .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
