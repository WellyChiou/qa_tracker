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
        <div class="modal-panel" @click.stop style="max-width: 700px;">
          <div class="modal-header">
            <h2 class="modal-title">{{ editingActivity ? '編輯活動' : '新增活動' }}</h2>
            <button class="btn-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveActivity">
              <div class="form-group">
                <label>標題 <span class="required">*</span></label>
                <input type="text" v-model="formData.title" required class="form-input" />
              </div>
              <div class="form-group">
                <label>描述</label>
                <textarea v-model="formData.description" rows="4" class="form-input"></textarea>
              </div>
              <div class="form-group">
                <label>活動日期 <span class="required">*</span></label>
                <input type="date" v-model="formData.activityDate" required class="form-input" />
              </div>
              <div class="form-group">
                <label>活動時間</label>
                <input type="text" v-model="formData.activityTime" placeholder="例如：10:00am" class="form-input" />
              </div>
              <div class="form-group">
                <label>地點</label>
                <input type="text" v-model="formData.location" class="form-input" />
              </div>
              <div class="form-group">
                <label class="checkbox-label">
                  <input type="checkbox" v-model="formData.isActive" class="checkbox-input" />
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

const activitiesList = ref([])
const showModal = ref(false)
const editingActivity = ref(null)
const formData = ref({
  title: '',
  description: '',
  activityDate: '',
  activityTime: '',
  location: '',
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
    alert('載入活動失敗: ' + error.message)
  }
}

const openAddModal = () => {
  editingActivity.value = null
  formData.value = {
    title: '',
    description: '',
    activityDate: '',
    activityTime: '',
    location: '',
    isActive: true
  }
  showModal.value = true
}

const editActivity = (activity) => {
  editingActivity.value = activity
  formData.value = {
    title: activity.title || '',
    description: activity.description || '',
    activityDate: activity.activityDate || '',
    activityTime: activity.activityTime || '',
    location: activity.location || '',
    isActive: activity.isActive !== undefined ? activity.isActive : true
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingActivity.value = null
}

const saveActivity = async () => {
  try {
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
        alert('儲存成功')
        closeModal()
        loadActivities()
      } else {
        alert('儲存失敗: ' + (data.message || '未知錯誤'))
      }
    }
  } catch (error) {
    console.error('儲存活動失敗:', error)
    alert('儲存活動失敗: ' + error.message)
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
        alert('刪除成功')
        loadActivities()
      }
    }
  } catch (error) {
    console.error('刪除活動失敗:', error)
    alert('刪除活動失敗: ' + error.message)
  }
}

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
.admin-activities {
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
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
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
