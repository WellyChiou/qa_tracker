<template>
  <AdminLayout>
    <div class="admin-sunday-messages">
      <div class="page-header">
        <h2>主日信息管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增主日信息</button>
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
      </section>

      <div class="messages-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ messagesList.length === 0 ? '尚無主日信息資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>主日信息列表 (共 {{ filteredList.length }} 筆)</h3>
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
              <tr v-for="message in paginatedList" :key="message.id">
                <td>{{ formatDate(message.serviceDate) }}</td>
                <td>{{ message.serviceType === 'SATURDAY' ? '週六晚崇' : '週日早崇' }}</td>
                <td>{{ message.title || '-' }}</td>
                <td>{{ message.speaker || '-' }}</td>
                <td>
                  <span :class="message.isActive ? 'status-active' : 'status-inactive'">
                    {{ message.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>
                  <button @click="editMessage(message)" class="btn btn-edit">編輯</button>
                  <button @click="deleteMessage(message.id)" class="btn btn-delete">刪除</button>
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

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...messagesList.value]
  
  if (filters.value.title) {
    filtered = filtered.filter(message => 
      message.title?.toLowerCase().includes(filters.value.title.toLowerCase())
    )
  }
  
  if (filters.value.startDate) {
    filtered = filtered.filter(message => {
      if (!message.serviceDate) return false
      return new Date(message.serviceDate) >= new Date(filters.value.startDate)
    })
  }
  
  if (filters.value.endDate) {
    filtered = filtered.filter(message => {
      if (!message.serviceDate) return false
      return new Date(message.serviceDate) <= new Date(filters.value.endDate)
    })
  }
  
  if (filters.value.serviceType) {
    filtered = filtered.filter(message => message.serviceType === filters.value.serviceType)
  }
  
  if (filters.value.isActive !== '') {
    filtered = filtered.filter(message => message.isActive === filters.value.isActive)
  }
  
  return filtered.sort((a, b) => {
    if (!a.serviceDate && !b.serviceDate) return 0
    if (!a.serviceDate) return 1
    if (!b.serviceDate) return -1
    return new Date(b.serviceDate) - new Date(a.serviceDate)
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
    serviceType: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => [filters.value.title, filters.value.startDate, filters.value.endDate, filters.value.serviceType, filters.value.isActive], () => {
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

const loadMessages = async () => {
  try {
    const response = await apiRequest('/church/admin/sunday-messages', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        messagesList.value = data.data
      }
    }
  } catch (error) {
    console.error('載入主日信息失敗:', error)
    alert('載入主日信息失敗: ' + error.message)
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
    alert('只能上傳圖片文件')
    return
  }

  // 驗證文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    alert('文件大小不能超過 5MB')
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
        alert('圖片上傳失敗: ' + (error.message || '未知錯誤'))
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
        alert('儲存成功')
        closeModal()
        loadMessages()
      } else {
        alert('儲存失敗: ' + (data.message || '未知錯誤'))
      }
    }
  } catch (error) {
    console.error('儲存主日信息失敗:', error)
    alert('儲存主日信息失敗: ' + error.message)
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
        alert('刪除成功')
        loadMessages()
      }
    }
  } catch (error) {
    console.error('刪除主日信息失敗:', error)
    alert('刪除主日信息失敗: ' + error.message)
  }
}


onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.admin-sunday-messages {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h2 {
  font-size: 1.8rem;
  color: #333;
  margin: 0;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 600;
  transition: all 0.2s;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5568d3;
}

.btn-edit {
  background: #48bb78;
  color: white;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
}

.btn-edit:hover {
  background: #38a169;
}

.btn-delete {
  background: #f56565;
  color: white;
  padding: 0.5rem 1rem;
}

.btn-delete:hover {
  background: #e53e3e;
}

.btn-secondary {
  background: #e2e8f0;
  color: #4a5568;
}

.btn-secondary:hover {
  background: #cbd5e0;
}

.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  color: #666;
}

.info-table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f7fafc;
}

th, td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

th {
  font-weight: 600;
  color: #4a5568;
  border-bottom: 2px solid #e2e8f0;
}

.status-active {
  color: #48bb78;
  font-weight: 600;
}

.status-inactive {
  color: #f56565;
  font-weight: 600;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-panel {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  max-height: 90vh;
  overflow-y: auto;
  width: 90%;
  max-width: 1100px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px 12px 0 0;
}

.modal-header .modal-title {
  color: white;
  font-size: 1.75rem;
  font-weight: 700;
}

.modal-header .btn-close {
  color: white;
  background: rgba(255, 255, 255, 0.2);
  transition: all 0.2s;
}

.modal-header .btn-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

.modal-title {
  font-size: 1.5rem;
  color: #333;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  padding: 0.5rem;
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.2s;
}

.btn-close:hover {
  background: rgba(0, 0, 0, 0.05);
}

.w-6 {
  width: 1.5rem;
  height: 1.5rem;
}

.modal-body {
  padding: 2rem;
}

.form-section {
  margin-bottom: 2.5rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid #e2e8f0;
}

.form-section:last-of-type {
  border-bottom: none;
  margin-bottom: 1.5rem;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #4a5568;
  margin: 0 0 1.5rem 0;
  padding-bottom: 0.75rem;
  border-bottom: 2px solid #667eea;
  display: inline-block;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}

.form-group {
  margin-bottom: 2rem;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  margin-bottom: 0.75rem;
  font-weight: 600;
  color: #4a5568;
  font-size: 0.95rem;
}

.required {
  color: #f56565;
}

.form-input {
  width: 100%;
  padding: 1rem;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.2s;
  background: #fff;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input:hover {
  border-color: #cbd5e0;
}

.form-textarea {
  resize: vertical;
  min-height: 200px;
  font-family: inherit;
  line-height: 1.6;
}

.form-hint {
  display: block;
  color: #718096;
  font-size: 0.875rem;
  margin-top: 0.5rem;
  font-style: italic;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.checkbox-input {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
}

.image-upload-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.btn-upload {
  padding: 1rem 1.5rem;
  white-space: nowrap;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.2s;
  align-self: flex-start;
}

.btn-upload:hover:not(:disabled) {
  background: #5568d3;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.3);
}

.btn-upload:disabled {
  background: #cbd5e0;
  cursor: not-allowed;
  transform: none;
}

.image-preview-actions {
  padding: 1rem;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: center;
}

.btn-remove-image {
  padding: 0.5rem 1rem;
  background: #e53e3e;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.btn-remove-image:hover {
  background: #c53030;
}

.image-preview-container {
  margin-top: 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  max-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f7fafc;
}

.image-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  display: block;
}

.image-error-message {
  color: #e53e3e;
  padding: 1rem;
  text-align: center;
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2.5rem;
  padding-top: 2rem;
  border-top: 2px solid #e2e8f0;
}

.form-actions .btn {
  min-width: 120px;
  padding: 0.875rem 2rem;
  font-size: 1rem;
}

/* 查詢條件樣式 */
.filters {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.filters h3 {
  margin: 0 0 1rem 0;
  font-size: 1.2rem;
  color: #333;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  align-items: end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-group label {
  font-weight: 600;
  color: #4a5568;
  font-size: 0.9rem;
}

.filter-group select,
.filter-group input {
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.95rem;
}

.filter-group select:focus,
.filter-group input:focus {
  outline: none;
  border-color: #667eea;
}

.table-header {
  padding: 1rem;
  border-bottom: 1px solid #e2e8f0;
}

.table-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #4a5568;
}

/* 分頁樣式 */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-top: 1px solid #e2e8f0;
  background: #f7fafc;
}

.pagination-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.pagination-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.pagination-label {
  font-size: 0.9rem;
  color: #4a5568;
}

.page-size-select {
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.9rem;
}

.pagination-info {
  font-size: 0.9rem;
  color: #718096;
}

.page-jump {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.page-input {
  width: 60px;
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  text-align: center;
  font-size: 0.9rem;
}

.page-input:focus {
  outline: none;
  border-color: #667eea;
}

.btn-secondary {
  background: #e2e8f0;
  color: #4a5568;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s;
}

.btn-secondary:hover:not(:disabled) {
  background: #cbd5e0;
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.w-5 {
  width: 1.25rem;
  height: 1.25rem;
}
</style>

