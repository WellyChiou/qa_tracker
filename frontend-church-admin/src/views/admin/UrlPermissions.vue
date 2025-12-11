<template>
  <AdminLayout>
    <div class="admin-url-permissions">
      <div class="page-header">
        <h2>URL 權限管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增 URL 權限</button>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
        <div class="filter-grid">
          <div class="filter-group">
            <label>URL 模式</label>
            <input
              type="text"
              v-model="filters.urlPattern"
              placeholder="輸入 URL 模式"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>HTTP 方法</label>
            <select v-model="filters.httpMethod">
              <option value="">全部</option>
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
            </select>
          </div>
          <div class="filter-group">
            <label>是否公開</label>
            <select v-model="filters.isPublic">
              <option value="">全部</option>
              <option :value="true">公開</option>
              <option :value="false">需認證</option>
            </select>
          </div>
          <div class="filter-group">
            <label>需要權限</label>
            <input
              type="text"
              v-model="filters.requiredPermission"
              placeholder="輸入權限代碼"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>是否啟用</label>
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

      <div class="url-permissions-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ permissions.length === 0 ? '尚無 URL 權限資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="url-permissions-table">
          <div class="table-header">
            <h3>URL 權限列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>URL 模式</th>
                <th>HTTP 方法</th>
                <th>是否公開</th>
                <th>需要角色</th>
                <th>需要權限</th>
                <th>是否啟用</th>
                <th>排序</th>
                <th>描述</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="permission in paginatedList" :key="permission.id">
                <td>{{ permission.urlPattern }}</td>
                <td>{{ permission.httpMethod || '全部' }}</td>
                <td>{{ permission.isPublic ? '是' : '否' }}</td>
                <td>{{ permission.requiredRole || '-' }}</td>
                <td>{{ permission.requiredPermission || '-' }}</td>
                <td>{{ permission.isActive ? '是' : '否' }}</td>
                <td>{{ permission.orderIndex }}</td>
                <td>{{ permission.description || '-' }}</td>
                <td>
                  <button @click="editPermission(permission.id)" class="btn btn-edit">編輯</button>
                  <button @click="deletePermission(permission.id)" class="btn btn-delete">刪除</button>
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
    </div>
    
    <UrlPermissionModal
      :show="showModal"
      :permission="selectedPermission"
      @close="closeModal"
      @saved="handleSaved"
    />
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import UrlPermissionModal from '@/components/UrlPermissionModal.vue'
import { apiRequest } from '@/utils/api'

const permissions = ref([])
const showModal = ref(false)
const selectedPermission = ref(null)

// 查詢條件
const filters = ref({
  urlPattern: '',
  httpMethod: '',
  isPublic: '',
  requiredPermission: '',
  isActive: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...permissions.value]
  
  if (filters.value.urlPattern) {
    filtered = filtered.filter(perm => 
      perm.urlPattern?.toLowerCase().includes(filters.value.urlPattern.toLowerCase())
    )
  }
  
  if (filters.value.httpMethod) {
    filtered = filtered.filter(perm => 
      (perm.httpMethod || '').toLowerCase() === filters.value.httpMethod.toLowerCase()
    )
  }
  
  if (filters.value.isPublic !== '') {
    filtered = filtered.filter(perm => perm.isPublic === filters.value.isPublic)
  }
  
  if (filters.value.requiredPermission) {
    filtered = filtered.filter(perm => 
      (perm.requiredPermission || '').toLowerCase().includes(filters.value.requiredPermission.toLowerCase())
    )
  }
  
  if (filters.value.isActive !== '') {
    filtered = filtered.filter(perm => perm.isActive === filters.value.isActive)
  }
  
  return filtered.sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0))
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
    urlPattern: '',
    httpMethod: '',
    isPublic: '',
    requiredPermission: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => [
  filters.value.urlPattern, 
  filters.value.httpMethod, 
  filters.value.isPublic, 
  filters.value.requiredPermission, 
  filters.value.isActive
], () => {
  currentPage.value = 1
  jumpPage.value = 1
})

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const loadPermissions = async () => {
  try {
    const response = await apiRequest('/church/admin/url-permissions', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      permissions.value = data.permissions || data || []
    }
  } catch (error) {
    console.error('載入 URL 權限失敗:', error)
  }
}

const openAddModal = () => {
  selectedPermission.value = null
  showModal.value = true
}

const editPermission = async (id) => {
  try {
    const response = await apiRequest(`/church/admin/url-permissions/${id}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      selectedPermission.value = data.permission || data
      showModal.value = true
    } else {
      alert('載入 URL 權限資料失敗')
    }
  } catch (error) {
    console.error('載入 URL 權限資料失敗:', error)
    alert('載入 URL 權限資料失敗: ' + error.message)
  }
}

const closeModal = () => {
  showModal.value = false
  selectedPermission.value = null
}

const handleSaved = () => {
  loadPermissions()
  closeModal()
}

const deletePermission = async (id) => {
  if (!confirm('確定要刪除此 URL 權限嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/admin/url-permissions/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadPermissions()
    } else {
      alert('刪除失敗')
    }
  } catch (error) {
    console.error('刪除 URL 權限失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadPermissions()
})
</script>

<style scoped>
.admin-url-permissions {
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
  margin: 0;
  font-size: 1.8rem;
  color: #333;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.url-permissions-list {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.url-permissions-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f5f5f5;
}

th, td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  font-weight: 600;
  color: #333;
}

tbody tr:hover {
  background: #f9f9f9;
}

.btn-edit {
  background: #667eea;
  color: white;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.btn-edit:hover {
  background: #5568d3;
}

.btn-delete {
  background: #ef4444;
  color: white;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.btn-delete:hover {
  background: #dc2626;
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

