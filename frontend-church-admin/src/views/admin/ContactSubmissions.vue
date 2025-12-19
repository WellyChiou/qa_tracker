<template>
  <AdminLayout>
    <div class="admin-contact-submissions">
      <div class="page-header">
        <h2>聯絡表單管理</h2>
        <div class="stats">
          <span class="stat-item">未讀：{{ stats.unread || 0 }}</span>
          <span class="stat-item">已讀：{{ stats.read || 0 }}</span>
        </div>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
        <div class="filter-grid">
          <div class="filter-group">
            <label>姓名</label>
            <input
              type="text"
              v-model="filters.name"
              placeholder="輸入姓名"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>電子郵件</label>
            <input
              type="text"
              v-model="filters.email"
              placeholder="輸入電子郵件"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>狀態</label>
            <select v-model="filters.isRead">
              <option value="">全部</option>
              <option :value="false">未讀</option>
              <option :value="true">已讀</option>
            </select>
          </div>
          <div class="filter-group">
            <label>開始日期</label>
            <input
              type="date"
              v-model="filters.startDate"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>結束日期</label>
            <input
              type="date"
              v-model="filters.endDate"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <button @click="resetFilters" class="btn btn-secondary">清除條件</button>
          </div>
        </div>
      </section>

      <div class="submissions-list">
        <div v-if="filteredList.length === 0 && !loading" class="empty-state">
          <p>{{ submissionsList.length === 0 ? '尚無聯絡表單記錄' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else-if="loading" class="empty-state">
          <p>載入中...</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>聯絡表單列表 (共 {{ totalElements }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>姓名</th>
                <th>電子郵件</th>
                <th>電話</th>
                <th>訊息預覽</th>
                <th>提交時間</th>
                <th>狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="submission in filteredList" :key="submission.id" :class="{ 'unread': !submission.isRead }">
                <td>{{ submission.name }}</td>
                <td>{{ submission.email }}</td>
                <td>{{ submission.phone || '-' }}</td>
                <td class="message-preview">{{ (submission.message || '').substring(0, 50) }}{{ (submission.message || '').length > 50 ? '...' : '' }}</td>
                <td>{{ formatDateTime(submission.submittedAt || submission.createdAt) }}</td>
                <td>
                  <span :class="submission.isRead ? 'status-read' : 'status-unread'">
                    {{ submission.isRead ? '已讀' : '未讀' }}
                  </span>
                </td>
                <td>
                  <button @click="viewSubmission(submission)" class="btn btn-view">查看</button>
                  <button v-if="!submission.isRead" @click="markAsRead(submission.id)" class="btn btn-mark-read">標記已讀</button>
                  <button @click="deleteSubmission(submission.id)" class="btn btn-delete">刪除</button>
                </td>
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
              <span class="pagination-info">共 {{ totalElements }} 筆 (第 {{ currentPage + 1 }}/{{ totalPages }} 頁)</span>
            </div>
            <div class="pagination-right">
              <button class="btn-secondary" @click="currentPage--" :disabled="currentPage === 0">
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
              <button class="btn-secondary" @click="currentPage++" :disabled="currentPage >= totalPages - 1">
                下一頁
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 查看詳情 Modal -->
      <div v-if="showViewModal && selectedSubmission" class="modal-overlay" @click="closeViewModal">
        <div class="modal-panel" @click.stop style="max-width: 600px;">
          <div class="modal-header">
            <h2 class="modal-title">聯絡表單詳情</h2>
            <button class="btn-close" @click="closeViewModal">×</button>
          </div>
          <div class="modal-body">
            <div class="detail-item">
              <label>姓名：</label>
              <span>{{ selectedSubmission.name }}</span>
            </div>
            <div class="detail-item">
              <label>電子郵件：</label>
              <span>{{ selectedSubmission.email }}</span>
            </div>
            <div class="detail-item">
              <label>電話：</label>
              <span>{{ selectedSubmission.phone || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>提交時間：</label>
              <span>{{ formatDateTime(selectedSubmission.submittedAt) }}</span>
            </div>
            <div class="detail-item full-width">
              <label>訊息內容：</label>
              <div class="message-content">{{ selectedSubmission.message }}</div>
            </div>
            <div class="form-actions">
              <button v-if="!selectedSubmission.isRead" @click="markAsRead(selectedSubmission.id)" class="btn btn-primary">標記已讀</button>
              <button type="button" class="btn btn-secondary" @click="closeViewModal">關閉</button>
            </div>
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

const submissionsList = ref([])
const showViewModal = ref(false)
const selectedSubmission = ref(null)
const stats = ref({ unread: 0, read: 0 })
const loading = ref(false)

// 查詢條件
const filters = ref({
  name: '',
  email: '',
  isRead: '',
  startDate: '',
  endDate: ''
})

// 分頁（後端分頁，從0開始）
const currentPage = ref(0)
const recordsPerPage = ref(20)
const jumpPage = ref(1)
const totalElements = ref(0)
const totalPages = ref(0)

const formatDateTime = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-TW')
}

// 過濾後的列表（前端過濾）
const filteredList = computed(() => {
  let filtered = [...submissionsList.value]
  
  if (filters.value.name) {
    filtered = filtered.filter(sub => 
      sub.name?.toLowerCase().includes(filters.value.name.toLowerCase())
    )
  }
  
  if (filters.value.email) {
    filtered = filtered.filter(sub => 
      sub.email?.toLowerCase().includes(filters.value.email.toLowerCase())
    )
  }
  
  if (filters.value.isRead !== '') {
    filtered = filtered.filter(sub => sub.isRead === filters.value.isRead)
  }
  
  if (filters.value.startDate) {
    filtered = filtered.filter(sub => {
      const date = sub.submittedAt || sub.createdAt
      if (!date) return false
      return new Date(date) >= new Date(filters.value.startDate)
    })
  }
  
  if (filters.value.endDate) {
    filtered = filtered.filter(sub => {
      const date = sub.submittedAt || sub.createdAt
      if (!date) return false
      return new Date(date) <= new Date(filters.value.endDate + 'T23:59:59')
    })
  }
  
  return filtered.sort((a, b) => {
    const dateA = new Date(a.submittedAt || a.createdAt || 0)
    const dateB = new Date(b.submittedAt || b.createdAt || 0)
    return dateB - dateA
  })
})

// 跳轉到指定頁
const jumpToPage = () => {
  const page = jumpPage.value - 1 // 轉換為從0開始
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    loadSubmissions()
  } else {
    jumpPage.value = currentPage.value + 1
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    name: '',
    email: '',
    isRead: '',
    startDate: '',
    endDate: ''
  }
  currentPage.value = 0
  jumpPage.value = 1
  loadSubmissions()
}

const loadSubmissions = async () => {
  loading.value = true
  try {
    // 構建查詢參數
    const params = new URLSearchParams()
    params.append('page', currentPage.value.toString())
    params.append('size', recordsPerPage.value.toString())
    if (filters.value.isRead !== '') {
      params.append('isRead', filters.value.isRead.toString())
    }
    
    const [submissionsRes, statsRes] = await Promise.all([
      apiRequest(`/church/admin/contact-submissions?${params.toString()}`, {
        method: 'GET',
        credentials: 'include'
      }),
      apiRequest('/church/admin/contact-submissions/stats', {
        method: 'GET',
        credentials: 'include'
      })
    ])
    
    if (submissionsRes.ok) {
      const data = await submissionsRes.json()
      if (data.success && data.data) {
        submissionsList.value = data.data
        totalElements.value = data.totalElements || data.data.length
        totalPages.value = data.totalPages || 1
        jumpPage.value = currentPage.value + 1
      }
    }
    
    if (statsRes.ok) {
      const statsData = await statsRes.json()
      if (statsData.success && statsData.data) {
        stats.value = statsData.data
      }
    }
  } catch (error) {
    console.error('載入聯絡表單失敗:', error)
    toast.error('載入聯絡表單失敗: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 監聽分頁變化
watch([currentPage, recordsPerPage], () => {
  loadSubmissions()
})

const viewSubmission = (submission) => {
  selectedSubmission.value = submission
  showViewModal.value = true
}

const closeViewModal = () => {
  showViewModal.value = false
  selectedSubmission.value = null
}

const markAsRead = async (id) => {
  try {
    const response = await apiRequest(`/church/admin/contact-submissions/${id}/read`, {
      method: 'PUT',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success) {
        toast.success('標記已讀成功')
        closeViewModal()
        loadSubmissions()
        // 重新載入統計
        const statsRes = await apiRequest('/church/admin/contact-submissions/stats', {
          method: 'GET',
          credentials: 'include'
        })
        if (statsRes.ok) {
          const statsData = await statsRes.json()
          if (statsData.success && statsData.data) {
            stats.value = statsData.data
          }
        }
      }
    }
  } catch (error) {
    console.error('標記已讀失敗:', error)
    toast.error('標記已讀失敗: ' + error.message)
  }
}

const deleteSubmission = async (id) => {
  if (!confirm('確定要刪除這筆聯絡表單記錄嗎？')) return
  
  try {
    const response = await apiRequest(`/church/admin/contact-submissions/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success) {
        toast.success('刪除成功')
        loadSubmissions()
      }
    }
  } catch (error) {
    console.error('刪除聯絡表單失敗:', error)
    toast.error('刪除聯絡表單失敗: ' + error.message)
  }
}

onMounted(() => {
  loadSubmissions()
})
</script>

<style scoped>
.admin-contact-submissions{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-contact-submissions .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-contact-submissions .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-contact-submissions .page-header p,
.admin-contact-submissions .subtitle,
.admin-contact-submissions .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-contact-submissions .table-container,
.admin-contact-submissions .list-container,
.admin-contact-submissions .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-contact-submissions .table-container{ padding:0; }

/* Inline helpers */
.admin-contact-submissions .hint,
.admin-contact-submissions .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-contact-submissions .actions,
.admin-contact-submissions .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
