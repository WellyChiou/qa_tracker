<template>
  <AdminLayout>
    <div class="admin-activities">
      <div class="page-header">
        <h2>活動管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增活動</button>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
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
      </section>

      <div class="activities-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ activitiesList.length === 0 ? '尚無活動資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>活動列表 (共 {{ filteredList.length }} 筆)</h3>
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
              <tr v-for="activity in paginatedList" :key="activity.id">
                <td>{{ activity.title }}</td>
                <td>{{ formatDate(activity.activityDate) }}</td>
                <td>{{ activity.activityTime || '-' }}</td>
                <td>{{ activity.location || '-' }}</td>
                <td>
                  <span :class="activity.isActive ? 'status-active' : 'status-inactive'">
                    {{ activity.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>
                  <button @click="editActivity(activity)" class="btn btn-edit">編輯</button>
                  <button @click="deleteActivity(activity.id)" class="btn btn-delete">刪除</button>
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
                    <label>活動時間</label>
                    <input type="text" v-model="formData.activityTime" placeholder="例如：10:00am" class="form-input" />
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
import { toast } from '@/composables/useToast'
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
  activityTime: '',
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

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...activitiesList.value]
  
  if (filters.value.title) {
    filtered = filtered.filter(activity => 
      activity.title?.toLowerCase().includes(filters.value.title.toLowerCase())
    )
  }
  
  if (filters.value.startDate) {
    filtered = filtered.filter(activity => {
      if (!activity.activityDate) return false
      return new Date(activity.activityDate) >= new Date(filters.value.startDate)
    })
  }
  
  if (filters.value.endDate) {
    filtered = filtered.filter(activity => {
      if (!activity.activityDate) return false
      return new Date(activity.activityDate) <= new Date(filters.value.endDate)
    })
  }
  
  if (filters.value.isActive !== '') {
    filtered = filtered.filter(activity => activity.isActive === filters.value.isActive)
  }
  
  return filtered.sort((a, b) => {
    if (!a.activityDate && !b.activityDate) return 0
    if (!a.activityDate) return 1
    if (!b.activityDate) return -1
    return new Date(b.activityDate) - new Date(a.activityDate)
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
    startDate: '',
    endDate: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => [filters.value.title, filters.value.startDate, filters.value.endDate, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
})

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString('zh-TW')
}

const loadActivities = async () => {
  try {
    const response = await apiRequest('/church/admin/activities', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        activitiesList.value = data.data
      }
    }
  } catch (error) {
    console.error('載入活動失敗:', error)
    toast.error('載入活動失敗: ' + error.message)
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
    activityTime: '',
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
    activityTime: activity.activityTime || '',
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

        const uploadResponse = await apiRequest('/church/admin/upload/image', {
          method: 'POST',
          body: uploadFormData
        })

        if (uploadResponse.ok) {
          const uploadData = await uploadResponse.json()
          if (uploadData.success && uploadData.url) {
            formData.value.imageUrl = uploadData.url
          } else {
            throw new Error(uploadData.message || '圖片上傳失敗')
          }
        } else {
          const errorData = await uploadResponse.json().catch(() => ({ message: '圖片上傳失敗' }))
          throw new Error(errorData.message || '圖片上傳失敗')
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
    const response = await apiRequest(`/church/admin/activities/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success) {
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
