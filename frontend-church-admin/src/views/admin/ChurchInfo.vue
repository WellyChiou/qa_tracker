<template>
  <AdminLayout>
    <div class="admin-church-info">
      <div class="page-header">
        <h2>教會資訊管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增資訊</button>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
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
      </section>

      <div class="church-info-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ churchInfoList.length === 0 ? '尚無教會資訊資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="info-table">
          <div class="table-header">
            <h3>教會資訊列表 (共 {{ filteredList.length }} 筆)</h3>
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
              <tr v-for="info in paginatedList" :key="info.id">
                <td>{{ info.infoKey }}</td>
                <td class="info-value">{{ info.infoValue || '-' }}</td>
                <td>{{ info.displayOrder }}</td>
                <td>
                  <span :class="info.isActive ? 'status-active' : 'status-inactive'">
                    {{ info.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>
                  <button @click="editInfo(info)" class="btn btn-edit">編輯</button>
                  <button @click="deleteInfo(info.id)" class="btn btn-delete">刪除</button>
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

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...churchInfoList.value]
  
  if (filters.value.infoKey) {
    filtered = filtered.filter(info => 
      info.infoKey?.toLowerCase().includes(filters.value.infoKey.toLowerCase())
    )
  }
  
  if (filters.value.infoValue) {
    filtered = filtered.filter(info => 
      (info.infoValue || '').toLowerCase().includes(filters.value.infoValue.toLowerCase())
    )
  }
  
  if (filters.value.isActive !== '') {
    filtered = filtered.filter(info => info.isActive === filters.value.isActive)
  }
  
  return filtered.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
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

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const loadChurchInfo = async () => {
  try {
    const response = await apiRequest('/church/admin/church-info', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        churchInfoList.value = data.data
      }
    }
  } catch (error) {
    console.error('載入教會資訊失敗:', error)
    alert('載入教會資訊失敗: ' + error.message)
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
        alert('儲存成功')
        closeModal()
        loadChurchInfo()
      } else {
        alert('儲存失敗: ' + (data.message || '未知錯誤'))
      }
    } else {
      const error = await response.json()
      alert('儲存失敗: ' + (error.message || '未知錯誤'))
    }
  } catch (error) {
    console.error('儲存教會資訊失敗:', error)
    alert('儲存教會資訊失敗: ' + error.message)
  }
}

const deleteInfo = async (id) => {
  if (!confirm('確定要刪除這筆教會資訊嗎？')) {
    return
  }
  
  try {
    // 注意：這裡需要後端提供刪除 API，目前先提示
    alert('刪除功能需要後端 API 支援，請聯繫開發人員')
  } catch (error) {
    console.error('刪除教會資訊失敗:', error)
    alert('刪除教會資訊失敗: ' + error.message)
  }
}

onMounted(() => {
  loadChurchInfo()
})
</script>

<style scoped>
.admin-church-info {
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

th {
  padding: 1rem;
  text-align: left;
  font-weight: 600;
  color: #4a5568;
  border-bottom: 2px solid #e2e8f0;
}

td {
  padding: 1rem;
  border-bottom: 1px solid #e2e8f0;
}

.info-value {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e2e8f0;
}

.modal-title {
  font-size: 1.5rem;
  color: #333;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 2rem;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s;
}

.btn-close:hover {
  background: #f7fafc;
}

.modal-body {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #4a5568;
}

.required {
  color: #f56565;
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.95rem;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.form-hint {
  display: block;
  margin-top: 0.25rem;
  color: #718096;
  font-size: 0.85rem;
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

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
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
