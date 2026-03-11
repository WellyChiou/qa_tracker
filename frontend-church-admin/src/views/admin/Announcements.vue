<template>
  <AdminLayout>
    <div class="admin-announcements">
      <div class="page-header">
        <div>
          <h2>公告管理</h2>
          <p>維護前台重要通知、置頂資訊與公告有效期間。</p>
        </div>
        <button @click="openAddModal" class="btn btn-primary">+ 新增公告</button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前公告</span>
          <strong>{{ totalRecords }}</strong>
          <p>後台目前可管理的公告總筆數。</p>
        </article>
        <article class="overview-card">
          <span>當前頁面</span>
          <strong>{{ announcementsList.length }}</strong>
          <p>這一頁實際載入的資料筆數。</p>
        </article>
        <article class="overview-card">
          <span>篩選狀態</span>
          <strong>{{ filters.title || filters.category || filters.startDate || filters.endDate || filters.isPinned !== '' || filters.isActive !== '' ? '已套用' : '全部' }}</strong>
          <p>可用標題、分類、日期與狀態快速縮小範圍。</p>
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
            <label>發布日期（開始）</label>
            <input
              type="date"
              v-model="filters.startDate"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>發布日期（結束）</label>
            <input
              type="date"
              v-model="filters.endDate"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>置頂狀態</label>
            <select v-model="filters.isPinned">
              <option value="">全部</option>
              <option :value="true">置頂</option>
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

      <div class="announcements-list card surface-card">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ announcementsList.length === 0 ? '尚無公告資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>公告列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>標題</th>
                <th>分類</th>
                <th>發布日期</th>
                <th>到期日期</th>
                <th>置頂</th>
                <th>啟用狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="announcement in announcementsList" :key="announcement.id">
                <td>{{ announcement.title }}</td>
                <td>{{ announcement.category || '-' }}</td>
                <td>{{ formatDate(announcement.publishDate) }}</td>
                <td>{{ formatDate(announcement.expireDate) || '永不過期' }}</td>
                <td>
                  <span v-if="announcement.isPinned" class="badge badge--accent">📌 置頂</span>
                  <span v-else class="muted">-</span>
                </td>
                <td>
                  <span :class="announcement.isActive ? 'status-active' : 'status-inactive'">
                    {{ announcement.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td><div class="table-actions"><button @click="editAnnouncement(announcement)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                  <button @click="deleteAnnouncement(announcement.id)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
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
        <div class="modal-panel" @click.stop>
          <div class="modal-header">
            <h2 class="modal-title">{{ editingAnnouncement ? '編輯公告' : '新增公告' }}</h2>
            <button class="btn-close" @click="closeModal">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveAnnouncement">
              <!-- 基本資訊區塊 -->
              <div class="form-section">
                <h3 class="section-title">基本資訊</h3>
                <div class="form-group">
                  <label>標題 <span class="required">*</span></label>
                  <input type="text" v-model="formData.title" required class="form-input" />
                </div>
                <div class="form-group">
                  <label>內容</label>
                  <textarea v-model="formData.content" rows="6" placeholder="公告詳細內容..." class="form-input form-textarea"></textarea>
                </div>
                <div class="form-group">
                  <label>分類</label>
                  <input type="text" v-model="formData.category" placeholder="例如：一般、重要、活動、通知" class="form-input" />
                </div>
              </div>

              <!-- 時間資訊區塊 -->
              <div class="form-section">
                <h3 class="section-title">時間資訊</h3>
                <div class="form-row">
                  <div class="form-group">
                    <label>發布日期</label>
                    <input type="date" v-model="formData.publishDate" class="form-input" />
                    <small class="form-hint">留空則使用今天</small>
                  </div>
                  <div class="form-group">
                    <label>到期日期</label>
                    <input type="date" v-model="formData.expireDate" class="form-input" />
                    <small class="form-hint">留空則永不過期</small>
                  </div>
                </div>
              </div>

              <!-- 設定區塊 -->
              <div class="form-section">
                <h3 class="section-title">設定</h3>
                <div class="form-group">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="formData.isPinned" class="checkbox-input" />
                    <span>置頂此公告</span>
                  </label>
                </div>
                <div class="form-group">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="formData.isActive" class="checkbox-input" />
                    <span>啟用此公告</span>
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
import { toast } from '@shared/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { apiRequest } from '@/utils/api'

const announcementsList = ref([])
const showModal = ref(false)
const editingAnnouncement = ref(null)
const formData = ref({
  title: '',
  content: '',
  category: '',
  publishDate: '',
  expireDate: '',
  isPinned: false,
  isActive: true
})

// 查詢條件
const filters = ref({
  title: '',
  category: '',
  startDate: '',
  endDate: '',
  isPinned: '',
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
  loadAnnouncements()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadAnnouncements()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadAnnouncements()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadAnnouncements()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadAnnouncements()
  } else {
    jumpPage.value = currentPage.value
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    title: '',
    category: '',
    startDate: '',
    endDate: '',
    isPinned: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 過濾後的列表（用於顯示）
const filteredList = computed(() => {
  if (!announcementsList.value || !Array.isArray(announcementsList.value)) {
    return []
  }
  return announcementsList.value
})

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => [filters.value.title, filters.value.category, filters.value.startDate, filters.value.endDate, filters.value.isPinned, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadAnnouncements()
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadAnnouncements()
})

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString('zh-TW')
}

const loadAnnouncements = async () => {
  try {
    const url = `/church/admin/announcements?page=${currentPage.value - 1}&size=${recordsPerPage.value}`
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(url, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 處理多種可能的數據結構
      let announcementsData = []
      if (data.success && data.data) {
        announcementsData = data.data
      } else if (data.data) {
        announcementsData = data.data
      } else if (data.content) {
        announcementsData = data.content
      } else if (Array.isArray(data)) {
        announcementsData = data
      }
      
      announcementsList.value = Array.isArray(announcementsData) ? announcementsData : []
      
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
        totalRecords.value = announcementsList.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
      }
      toast.success(`載入成功，共 ${totalRecords.value} 筆公告`)
    } else {
      // 確保即使 API 失敗，列表也是空陣列
      announcementsList.value = []
      toast.error('載入公告失敗')
    }
  } catch (error) {
    console.error('載入公告失敗:', error)
    toast.error('載入公告失敗: ' + error.message)
    // 確保即使發生錯誤，列表也是空陣列
    announcementsList.value = []
  }
}

const openAddModal = () => {
  editingAnnouncement.value = null
  formData.value = {
    title: '',
    content: '',
    category: '',
    publishDate: '',
    expireDate: '',
    isPinned: false,
    isActive: true
  }
  showModal.value = true
}

const editAnnouncement = (announcement) => {
  editingAnnouncement.value = announcement
  formData.value = {
    title: announcement.title || '',
    content: announcement.content || '',
    category: announcement.category || '',
    publishDate: announcement.publishDate || '',
    expireDate: announcement.expireDate || '',
    isPinned: announcement.isPinned !== undefined ? announcement.isPinned : false,
    isActive: announcement.isActive !== undefined ? announcement.isActive : true
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingAnnouncement.value = null
}

const saveAnnouncement = async () => {
  try {
    const url = editingAnnouncement.value 
      ? `/church/admin/announcements/${editingAnnouncement.value.id}`
      : '/church/admin/announcements'
    const method = editingAnnouncement.value ? 'PUT' : 'POST'
    
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(formData.value)
    })
    
    if (data) {
      if (data.success) {
        toast.success('儲存成功')
        closeModal()
        loadAnnouncements()
      } else {
        toast.error('儲存失敗: ' + (data.message || '未知錯誤'))
      }
    }
  } catch (error) {
    console.error('儲存公告失敗:', error)
    toast.error('儲存公告失敗: ' + error.message)
  }
}

const deleteAnnouncement = async (id) => {
  if (!confirm('確定要刪除這個公告嗎？')) return
  
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/announcements/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (data) {
      if (data.success) {
        toast.success('刪除成功')
        loadAnnouncements()
      }
    }
  } catch (error) {
    console.error('刪除公告失敗:', error)
    toast.error('刪除公告失敗: ' + error.message)
  }
}

onMounted(() => {
  loadAnnouncements()
})
</script>

<style scoped>
.admin-announcements{
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
.admin-announcements .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-announcements .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-announcements .page-header p,
.admin-announcements .subtitle,
.admin-announcements .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-announcements .table-container,
.admin-announcements .list-container,
.admin-announcements .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-announcements .table-container{ padding:0; }

/* Inline helpers */
.admin-announcements .hint,
.admin-announcements .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-announcements .actions,
.admin-announcements .header-actions{
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
