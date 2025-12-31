<template>
  <AdminLayout>
    <div class="admin-prayer-requests">
      <div class="page-header">
        <h2>代禱事項管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增代禱事項</button>
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
            <label>標題關鍵字</label>
            <input
              type="text"
              v-model="filters.title"
              placeholder="輸入標題關鍵字"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>分類</label>
            <input
              type="text"
              v-model="filters.category"
              placeholder="輸入分類"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>緊急狀態</label>
            <select v-model="filters.isUrgent">
              <option value="">全部</option>
              <option :value="true">緊急</option>
              <option :value="false">一般</option>
            </select>
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

      <div class="prayer-requests-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ prayerRequestsList.length === 0 ? '尚無代禱事項資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>代禱事項列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>標題</th>
                <th>分類</th>
                <th>緊急</th>
                <th>建立時間</th>
                <th>啟用狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="prayer in paginatedList" :key="prayer.id">
                <td>{{ prayer.title }}</td>
                <td>{{ prayer.category || '-' }}</td>
                <td>
                  <span v-if="prayer.isUrgent" class="badge badge--accent">🔥 緊急</span>
                  <span v-else class="muted">一般</span>
                </td>
                <td>{{ formatDateTime(prayer.createdAt) }}</td>
                <td>
                  <span :class="prayer.isActive ? 'status-active' : 'status-inactive'">
                    {{ prayer.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td><div class="table-actions"><button @click="editPrayerRequest(prayer)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                  <button @click="deletePrayerRequest(prayer.id)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
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
              <span class="pagination-info">共 {{ filteredList.length }} 筆 (第 {{ currentPage }}/{{ totalPages }} 頁)</span>
            </div>
            <div class="pagination-right">
              <button class="btn-secondary" @click="currentPage--" :disabled="currentPage === 1">
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
              <button class="btn-secondary" @click="currentPage++" :disabled="currentPage === totalPages">
                下一頁
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 新增/編輯 Modal -->
      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-panel" @click.stop>
          <div class="modal-header">
            <h2 class="modal-title">{{ editingPrayerRequest ? '編輯代禱事項' : '新增代禱事項' }}</h2>
            <button class="btn-close" @click="closeModal">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="savePrayerRequest">
              <!-- 基本資訊區塊 -->
              <div class="form-section">
                <h3 class="section-title">基本資訊</h3>
                <div class="form-group">
                  <label>標題 <span class="required">*</span></label>
                  <input type="text" v-model="formData.title" required class="form-input" />
                </div>
                <div class="form-group">
                  <label>內容</label>
                  <textarea v-model="formData.content" rows="6" placeholder="代禱事項詳細內容..." class="form-input form-textarea"></textarea>
                </div>
                <div class="form-group">
                  <label>分類</label>
                  <input type="text" v-model="formData.category" placeholder="例如：個人、家庭、教會、社區" class="form-input" />
                </div>
              </div>

              <!-- 設定區塊 -->
              <div class="form-section">
                <h3 class="section-title">設定</h3>
                <div class="form-group">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="formData.isUrgent" class="checkbox-input" />
                    <span>標記為緊急</span>
                  </label>
                </div>
                <div class="form-group">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="formData.isActive" class="checkbox-input" />
                    <span>啟用此代禱事項</span>
                  </label>
                </div>
              </div>

              <div class="form-actions">
                <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
                <button type="submit" class="btn btn-primary">儲存</button>
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

const prayerRequestsList = ref([])
const showModal = ref(false)
const editingPrayerRequest = ref(null)
const formData = ref({
  title: '',
  content: '',
  category: '',
  isUrgent: false,
  isActive: true
})

// 查詢條件
const filters = ref({
  title: '',
  category: '',
  isUrgent: '',
  isActive: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...prayerRequestsList.value]
  
  if (filters.value.title) {
    filtered = filtered.filter(prayer => 
      prayer.title?.toLowerCase().includes(filters.value.title.toLowerCase())
    )
  }
  
  if (filters.value.category) {
    filtered = filtered.filter(prayer => 
      prayer.category?.toLowerCase().includes(filters.value.category.toLowerCase())
    )
  }
  
  if (filters.value.isUrgent !== '') {
    filtered = filtered.filter(prayer => prayer.isUrgent === filters.value.isUrgent)
  }
  
  if (filters.value.isActive !== '') {
    filtered = filtered.filter(prayer => prayer.isActive === filters.value.isActive)
  }
  
  return filtered.sort((a, b) => {
    // 緊急的在前，然後按建立時間降序
    if (a.isUrgent && !b.isUrgent) return -1
    if (!a.isUrgent && b.isUrgent) return 1
    if (!a.createdAt && !b.createdAt) return 0
    if (!a.createdAt) return 1
    if (!b.createdAt) return -1
    return new Date(b.createdAt) - new Date(a.createdAt)
  })
})

// 分頁後的列表
const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * recordsPerPage.value
  return filteredList.value.slice(start, start + recordsPerPage.value)
})

// 總頁數
const totalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredList.value.length / recordsPerPage.value))
})

// 跳轉到指定頁
const jumpToPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    currentPage.value = jumpPage.value
  } else {
    jumpPage.value = currentPage.value
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    title: '',
    category: '',
    isUrgent: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => [filters.value.title, filters.value.category, filters.value.isUrgent, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
})

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const formatDateTime = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-TW')
}

const loadPrayerRequests = async () => {
  try {
    const response = await apiRequest('/church/admin/prayer-requests', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        prayerRequestsList.value = data.data
      }
    }
  } catch (error) {
    console.error('載入代禱事項失敗:', error)
    toast.error('載入代禱事項失敗: ' + error.message)
  }
}

const openAddModal = () => {
  editingPrayerRequest.value = null
  formData.value = {
    title: '',
    content: '',
    category: '',
    isUrgent: false,
    isActive: true
  }
  showModal.value = true
}

const editPrayerRequest = (prayer) => {
  editingPrayerRequest.value = prayer
  formData.value = {
    title: prayer.title || '',
    content: prayer.content || '',
    category: prayer.category || '',
    isUrgent: prayer.isUrgent !== undefined ? prayer.isUrgent : false,
    isActive: prayer.isActive !== undefined ? prayer.isActive : true
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingPrayerRequest.value = null
}

const savePrayerRequest = async () => {
  try {
    const url = editingPrayerRequest.value 
      ? `/church/admin/prayer-requests/${editingPrayerRequest.value.id}`
      : '/church/admin/prayer-requests'
    const method = editingPrayerRequest.value ? 'PUT' : 'POST'
    
    const response = await apiRequest(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(formData.value)
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success) {
        toast.success('儲存成功')
        closeModal()
        loadPrayerRequests()
      } else {
        toast.error('儲存失敗: ' + (data.message || '未知錯誤'))
      }
    }
  } catch (error) {
    console.error('儲存代禱事項失敗:', error)
    toast.error('儲存代禱事項失敗: ' + error.message)
  }
}

const deletePrayerRequest = async (id) => {
  if (!confirm('確定要刪除這個代禱事項嗎？')) return
  
  try {
    const response = await apiRequest(`/church/admin/prayer-requests/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success) {
        toast.success('刪除成功')
        loadPrayerRequests()
      }
    }
  } catch (error) {
    console.error('刪除代禱事項失敗:', error)
    toast.error('刪除代禱事項失敗: ' + error.message)
  }
}

onMounted(() => {
  loadPrayerRequests()
})
</script>

<style scoped>
.admin-prayer-requests{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-prayer-requests .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-prayer-requests .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-prayer-requests .page-header p,
.admin-prayer-requests .subtitle,
.admin-prayer-requests .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-prayer-requests .table-container,
.admin-prayer-requests .list-container,
.admin-prayer-requests .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-prayer-requests .table-container{ padding:0; }

/* Inline helpers */
.admin-prayer-requests .hint,
.admin-prayer-requests .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-prayer-requests .actions,
.admin-prayer-requests .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>

