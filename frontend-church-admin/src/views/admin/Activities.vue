<template>
  <AdminLayout>
    <div class="admin-activities">
      <div class="page-header">
        <h2>活動管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增活動</button>
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

      <div class="activities-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ activitiesList.length === 0 ? '尚無活動資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>活動列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>標題</th>
                <th>日期</th>
                <th>時間</th>
                <th>地點</th>
                <th>啟用狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="activity in activitiesList" :key="activity.id">
                <td>{{ activity.title }}</td>
                <td>{{ formatDate(activity.activityDate) }}</td>
                <td>{{ formatTimeRange(activity.startTime, activity.endTime) }}</td>
                <td>{{ activity.location || '-' }}</td>
                <td>
                  <span :class="activity.isActive ? 'status-active' : 'status-inactive'">
                    {{ activity.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td><div class="table-actions"><button @click="editActivity(activity)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                  <button @click="deleteActivity(activity.id)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
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
            <h2 class="modal-title">{{ editingActivity ? '編輯活動' : '新增活動' }}</h2>
            <button class="btn-close" @click="closeModal">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveActivity">
              <!-- 基本資訊區塊 -->
              <div class="form-section">
                <h3 class="section-title">基本資訊</h3>
                <div class="form-group">
                  <label>標題 <span class="required">*</span></label>
                  <input type="text" v-model="formData.title" required class="form-input" />
                </div>
                <div class="form-group">
                  <label>描述</label>
                  <textarea v-model="formData.description" rows="6" placeholder="活動詳細描述..." class="form-input form-textarea"></textarea>
                </div>
              </div>

              <!-- 時間地點資訊區塊 -->
              <div class="form-section">
                <h3 class="section-title">時間地點</h3>
                <div class="form-row">
                  <div class="form-group">
                    <label>活動日期 <span class="required">*</span></label>
                    <input type="date" v-model="formData.activityDate" required class="form-input" />
                  </div>
                  <div class="form-group">
                    <label>開始時間</label>
                    <input type="text" v-model="formData.startTime" placeholder="例如：10:00am 或 13:00" class="form-input" />
                  </div>
                  <div class="form-group">
                    <label>結束時間</label>
                    <input type="text" v-model="formData.endTime" placeholder="例如：12:00pm 或 16:00" class="form-input" />
                  </div>
                </div>
                <div class="form-group">
                  <label>地點</label>
                  <input type="text" v-model="formData.location" placeholder="活動地點" class="form-input" />
                </div>
              </div>

              <!-- 內容資訊區塊 -->
              <div class="form-section">
                <h3 class="section-title">內容資訊</h3>
                <div class="form-group">
                  <label>活動圖片</label>
                  <div class="image-upload-section">
                    <button type="button" @click="triggerFileUpload" class="btn btn-upload">
                      選擇圖片
                    </button>
                    <input type="file" ref="fileInput" @change="handleFileUpload" accept="image/*" style="display: none;" />
                    <small class="form-hint">點擊「選擇圖片」上傳活動圖片（支援 JPG、PNG、GIF、WebP，最大 5MB）</small>
                  </div>
                  <!-- 圖片預覽 -->
                  <div v-if="previewImageUrl || formData.imageUrl" class="image-preview-container">
                    <img v-show="!imageError" :src="previewImageUrl || formData.imageUrl" alt="預覽圖片" @error="imageError = true" class="image-preview" />
                    <p v-if="imageError" class="image-error-message">圖片載入失敗或無效</p>
                    <div v-if="(previewImageUrl || formData.imageUrl) && !imageError" class="image-preview-actions">
                      <button type="button" @click="removeImage" class="btn btn-remove-image">移除圖片</button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 設定區塊 -->
              <div class="form-section">
                <h3 class="section-title">設定</h3>
                <div class="form-group">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="formData.isActive" class="checkbox-input" />
                    <span>啟用此活動</span>
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

const activitiesList = ref([])
const showModal = ref(false)
const editingActivity = ref(null)
const fileInput = ref(null)
const uploading = ref(false)
const imageError = ref(false)
const selectedFile = ref(null)
const previewImageUrl = ref(null)
const formData = ref({
  title: '',
  description: '',
  activityDate: '',
  startTime: '',
  endTime: '',
  location: '',
  imageUrl: '',
  isActive: true
})

// 查詢條件
const filters = ref({
  title: '',
  startDate: '',
  endDate: '',
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
  loadActivities()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadActivities()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadActivities()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadActivities()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadActivities()
  } else {
    jumpPage.value = currentPage.value
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    title: '',
    startDate: '',
    endDate: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
  loadActivities()
}

// 過濾後的列表（用於顯示）
const filteredList = computed(() => {
  if (!activitiesList.value || !Array.isArray(activitiesList.value)) {
    return []
  }
  return activitiesList.value
})

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => [filters.value.title, filters.value.startDate, filters.value.endDate, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadActivities()
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadActivities()
})

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString('zh-TW')
}

const formatTimeRange = (startTime, endTime) => {
  if (startTime && endTime) {
    return `${startTime} ~ ${endTime}`
  } else if (startTime) {
    return startTime
  } else if (endTime) {
    return endTime
  }
  return '-'
}

const loadActivities = async () => {
  try {
    const params = new URLSearchParams()
    if (filters.value.title) {
      params.append('title', filters.value.title)
    }
    if (filters.value.startDate) {
      params.append('startDate', filters.value.startDate)
    }
    if (filters.value.endDate) {
      params.append('endDate', filters.value.endDate)
    }
    if (filters.value.isActive !== '') {
      params.append('isActive', filters.value.isActive === true || filters.value.isActive === 'true')
    }
    params.append('page', (currentPage.value - 1).toString())
    params.append('size', recordsPerPage.value.toString())
    
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/activities?${params.toString()}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 處理多種可能的數據結構
      let activitiesData = []
      if (data.success && data.data) {
        activitiesData = data.data
      } else if (data.data) {
        activitiesData = data.data
      } else if (data.content) {
        activitiesData = data.content
      } else if (Array.isArray(data)) {
        activitiesData = data
      }
      
      activitiesList.value = Array.isArray(activitiesData) ? activitiesData : []
      
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
        totalRecords.value = activitiesList.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
      }
      toast.success(`載入成功，共 ${totalRecords.value} 筆活動`)
    } else {
      // 確保即使 API 失敗，列表也是空陣列
      activitiesList.value = []
      toast.error('載入活動失敗')
    }
  } catch (error) {
    console.error('載入活動失敗:', error)
    toast.error('載入活動失敗: ' + error.message)
    // 確保即使發生錯誤，列表也是空陣列
    activitiesList.value = []
  }
}

const openAddModal = () => {
  editingActivity.value = null
  imageError.value = false
  selectedFile.value = null
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
  previewImageUrl.value = null
  formData.value = {
    title: '',
    description: '',
    activityDate: '',
    startTime: '',
    endTime: '',
    location: '',
    imageUrl: '',
    isActive: true
  }
  showModal.value = true
}

const editActivity = (activity) => {
  editingActivity.value = activity
  imageError.value = false
  selectedFile.value = null
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
  previewImageUrl.value = null
  formData.value = {
    title: activity.title || '',
    description: activity.description || '',
    activityDate: activity.activityDate || '',
    startTime: activity.startTime || '',
    endTime: activity.endTime || '',
    location: activity.location || '',
    imageUrl: activity.imageUrl || '',
    isActive: activity.isActive !== undefined ? activity.isActive : true
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingActivity.value = null
  imageError.value = false
  selectedFile.value = null
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
  previewImageUrl.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const saveActivity = async () => {
  try {
    // 如果有新選中的文件，先上傳圖片
    if (selectedFile.value) {
      uploading.value = true
      try {
        const uploadFormData = new FormData()
        uploadFormData.append('file', selectedFile.value)
        uploadFormData.append('type', 'activities')

        // apiRequest 現在會自動返回解析後的資料
        const uploadData = await apiRequest('/church/admin/upload/image', {
          method: 'POST',
          body: uploadFormData
        })

        if (uploadData) {
          if (uploadData.success && uploadData.url) {
            formData.value.imageUrl = uploadData.url
          } else {
            throw new Error(uploadData.message || '圖片上傳失敗')
          }
        } else {
          throw new Error('圖片上傳失敗')
        }
      } catch (error) {
        console.error('圖片上傳失敗:', error)
        toast.error('圖片上傳失敗: ' + (error.message || '未知錯誤'))
        uploading.value = false
        return
      } finally {
        uploading.value = false
      }

      // 清理臨時狀態
      if (previewImageUrl.value) {
        URL.revokeObjectURL(previewImageUrl.value)
      }
      selectedFile.value = null
      previewImageUrl.value = null
    }

    // 儲存活動數據
    const url = editingActivity.value 
      ? `/church/admin/activities/${editingActivity.value.id}`
      : '/church/admin/activities'
    const method = editingActivity.value ? 'PUT' : 'POST'
    
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
        loadActivities()
      } else {
        toast.error('儲存失敗: ' + (data.message || '未知錯誤'))
      }
    }
  } catch (error) {
    console.error('儲存活動失敗:', error)
    toast.error('儲存活動失敗: ' + error.message)
  }
}

const deleteActivity = async (id) => {
  if (!confirm('確定要刪除這個活動嗎？')) return
  
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/activities/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (data) {
      if (data.success !== false) {
        toast.success('刪除成功')
        loadActivities()
      }
    }
  } catch (error) {
    console.error('刪除活動失敗:', error)
    toast.error('刪除活動失敗: ' + error.message)
  }
}

const triggerFileUpload = () => {
  if (fileInput.value) {
    fileInput.value.click()
  }
}

const handleFileUpload = (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // 驗證文件類型
  if (!file.type.startsWith('image/')) {
    toast.info('只能上傳圖片文件')
    return
  }

  // 驗證文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    toast.info('文件大小不能超過 5MB')
    return
  }

  // 清理舊的預覽 URL
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }

  // 創建本地預覽
  selectedFile.value = file
  previewImageUrl.value = URL.createObjectURL(file)
  imageError.value = false

  // 清空文件輸入，以便可以再次選擇同一文件
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const removeImage = () => {
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
  selectedFile.value = null
  previewImageUrl.value = null
  formData.value.imageUrl = ''
  imageError.value = false
}

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
.admin-activities{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-activities .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-activities .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-activities .page-header p,
.admin-activities .subtitle,
.admin-activities .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-activities .table-container,
.admin-activities .list-container,
.admin-activities .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-activities .table-container{ padding:0; }

/* Inline helpers */
.admin-activities .hint,
.admin-activities .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-activities .actions,
.admin-activities .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
