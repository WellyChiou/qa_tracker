<template>
  <AdminLayout>
    <div class="admin-sunday-messages">
      <div class="page-header">
        <h2>主日信息管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增主日信息</button>
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
            <label>聚會時間</label>
            <select v-model="filters.serviceType">
              <option value="">全部</option>
              <option value="SATURDAY">週六晚崇</option>
              <option value="SUNDAY">週日早崇</option>
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

      <div class="messages-list">
        <div v-if="messagesList.length === 0" class="empty-state">
          <p>尚無主日信息資料</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>主日信息列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>日期</th>
                <th>聚會時間</th>
                <th>標題</th>
                <th>講員</th>
                <th>啟用狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="message in messagesList" :key="message.id">
                <td>{{ formatDate(message.serviceDate) }}</td>
                <td>{{ message.serviceType === 'SATURDAY' ? '週六晚崇' : '週日早崇' }}</td>
                <td>{{ message.title || '-' }}</td>
                <td>{{ message.speaker || '-' }}</td>
                <td>
                  <span :class="message.isActive ? 'status-active' : 'status-inactive'">
                    {{ message.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td><div class="table-actions"><button @click="editMessage(message)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                  <button @click="deleteMessage(message.id)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
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
            <h2 class="modal-title">{{ editingMessage ? '編輯主日信息' : '新增主日信息' }}</h2>
            <button class="btn-close" @click="closeModal">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveMessage">
              <!-- 基本資訊區塊 -->
              <div class="form-section">
                <h3 class="section-title">基本資訊</h3>
                <div class="form-row">
                  <div class="form-group">
                    <label>主日日期 <span class="required">*</span></label>
                    <input type="date" v-model="formData.serviceDate" required class="form-input" />
                  </div>
                  <div class="form-group">
                    <label>聚會時間 <span class="required">*</span></label>
                    <select v-model="formData.serviceType" required class="form-input">
                      <option value="SUNDAY">週日早崇</option>
                      <option value="SATURDAY">週六晚崇</option>
                    </select>
                  </div>
                </div>
                <div class="form-group">
                  <label>標題/講題</label>
                  <input type="text" v-model="formData.title" placeholder="例如：在光中的奇蹟" class="form-input" />
                </div>
              </div>

              <!-- 講道資訊區塊 -->
              <div class="form-section">
                <h3 class="section-title">講道資訊</h3>
                <div class="form-row">
                  <div class="form-group">
                    <label>經文</label>
                    <input type="text" v-model="formData.scripture" placeholder="例如：約翰福音 1:1-18" class="form-input" />
                  </div>
                  <div class="form-group">
                    <label>講員</label>
                    <input type="text" v-model="formData.speaker" placeholder="講員姓名" class="form-input" />
                  </div>
                </div>
              </div>

              <!-- 內容資訊區塊 -->
              <div class="form-section">
                <h3 class="section-title">內容資訊</h3>
                <div class="form-group">
                  <label>DM圖片</label>
                  <div class="image-upload-section">
                    <button type="button" @click="triggerFileUpload" class="btn btn-upload">
                      選擇圖片
                    </button>
                    <input type="file" ref="fileInput" @change="handleFileUpload" accept="image/*" style="display: none;" />
                    <small class="form-hint">點擊「選擇圖片」上傳DM圖片（支援 JPG、PNG、GIF、WebP，最大 5MB）</small>
                  </div>
                  <div v-if="previewImageUrl || formData.imageUrl" class="image-preview-container">
                    <img v-show="!imageError" :src="previewImageUrl || formData.imageUrl" @error="imageError = true" class="image-preview" alt="DM圖片預覽" />
                    <p v-if="imageError" class="image-error-message">圖片載入失敗或無效</p>
                    <div v-if="(previewImageUrl || formData.imageUrl) && !imageError" class="image-preview-actions">
                      <button type="button" @click="removeImage" class="btn btn-remove-image">移除圖片</button>
                    </div>
                  </div>
                </div>
                <div class="form-group">
                  <label>內容/解析文字</label>
                  <textarea v-model="formData.content" rows="10" placeholder="從DM圖片解析出的文字資訊..." class="form-input form-textarea"></textarea>
                </div>
              </div>

              <!-- 設定區塊 -->
              <div class="form-section">
                <h3 class="section-title">設定</h3>
                <div class="form-group">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="formData.isActive" class="checkbox-input" />
                    <span>啟用此主日信息</span>
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

const messagesList = ref([])
const showModal = ref(false)
const editingMessage = ref(null)
const fileInput = ref(null)
const uploading = ref(false)
const imageError = ref(false)
const selectedFile = ref(null)
const previewImageUrl = ref(null)
const formData = ref({
  serviceDate: '',
  serviceType: 'SUNDAY',
  imageUrl: '',
  title: '',
  scripture: '',
  speaker: '',
  content: '',
  isActive: true
})

// 查詢條件
const filters = ref({
  title: '',
  startDate: '',
  endDate: '',
  serviceType: '',
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
  loadMessages()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadMessages()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadMessages()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadMessages()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadMessages()
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
    serviceType: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
  loadMessages()
}

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => [filters.value.title, filters.value.startDate, filters.value.endDate, filters.value.serviceType, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadMessages()
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadMessages()
})

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString('zh-TW')
}

const loadMessages = async () => {
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
    if (filters.value.serviceType) {
      params.append('serviceType', filters.value.serviceType)
    }
    if (filters.value.isActive !== '') {
      params.append('isActive', filters.value.isActive === true || filters.value.isActive === 'true')
    }
    params.append('page', (currentPage.value - 1).toString())
    params.append('size', recordsPerPage.value.toString())
    
    const response = await apiRequest(`/church/admin/sunday-messages?${params.toString()}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        messagesList.value = data.data || data.content || []
        
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
          totalRecords.value = messagesList.value.length
          totalPages.value = 1
          currentPage.value = 1
          jumpPage.value = 1
        }
      }
    }
  } catch (error) {
    console.error('載入主日信息失敗:', error)
    toast.error('載入主日信息失敗: ' + error.message)
  }
}

const openAddModal = () => {
  editingMessage.value = null
  imageError.value = false
  selectedFile.value = null
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
  previewImageUrl.value = null
  formData.value = {
    serviceDate: '',
    serviceType: 'SUNDAY',
    imageUrl: '',
    title: '',
    scripture: '',
    speaker: '',
    content: '',
    isActive: true
  }
  showModal.value = true
}

const editMessage = (message) => {
  editingMessage.value = message
  imageError.value = false
  selectedFile.value = null
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
  previewImageUrl.value = null
  formData.value = {
    serviceDate: message.serviceDate || '',
    serviceType: message.serviceType || 'SUNDAY',
    imageUrl: message.imageUrl || '',
    title: message.title || '',
    scripture: message.scripture || '',
    speaker: message.speaker || '',
    content: message.content || '',
    isActive: message.isActive !== undefined ? message.isActive : true
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingMessage.value = null
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

const saveMessage = async () => {
  try {
    // 如果有新選中的文件，先上傳圖片
    if (selectedFile.value) {
      uploading.value = true
      try {
        const uploadFormData = new FormData()
        uploadFormData.append('file', selectedFile.value)
        uploadFormData.append('type', 'sunday-messages')

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

    // 儲存主日信息數據
    const url = editingMessage.value 
      ? `/church/admin/sunday-messages/${editingMessage.value.id}`
      : '/church/admin/sunday-messages'
    const method = editingMessage.value ? 'PUT' : 'POST'
    
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
        loadMessages()
      } else {
        toast.error('儲存失敗: ' + (data.message || '未知錯誤'))
      }
    }
  } catch (error) {
    console.error('儲存主日信息失敗:', error)
    toast.error('儲存主日信息失敗: ' + error.message)
  }
}

const deleteMessage = async (id) => {
  if (!confirm('確定要刪除這個主日信息嗎？')) return
  
  try {
    const response = await apiRequest(`/church/admin/sunday-messages/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success) {
        toast.success('刪除成功')
        loadMessages()
      }
    }
  } catch (error) {
    console.error('刪除主日信息失敗:', error)
    toast.error('刪除主日信息失敗: ' + error.message)
  }
}


onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.admin-sunday-messages{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-sunday-messages .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-sunday-messages .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-sunday-messages .page-header p,
.admin-sunday-messages .subtitle,
.admin-sunday-messages .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-sunday-messages .table-container,
.admin-sunday-messages .list-container,
.admin-sunday-messages .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-sunday-messages .table-container{ padding:0; }

/* Inline helpers */
.admin-sunday-messages .hint,
.admin-sunday-messages .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-sunday-messages .actions,
.admin-sunday-messages .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
