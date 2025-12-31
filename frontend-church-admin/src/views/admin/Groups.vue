<template>
  <AdminLayout>
    <div class="admin-groups">
      <div class="page-header">
        <h2>小組管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增小組</button>
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
              <label>小組名稱</label>
              <input
                type="text"
                v-model="filters.groupName"
                placeholder="輸入小組名稱"
                class="form-input"
                @keyup.enter="load"
              />
            </div>
            <div class="filter-group">
              <label>區分</label>
              <input
                type="text"
                v-model="filters.category"
                placeholder="輸入區分"
                class="form-input"
                @keyup.enter="load"
              />
            </div>
            <div class="filter-group">
              <label>聚會地點</label>
              <input
                type="text"
                v-model="filters.meetingLocation"
                placeholder="輸入聚會地點"
                class="form-input"
                @keyup.enter="load"
              />
            </div>
            <div class="filter-group">
              <label>狀態</label>
              <select v-model="filters.status" class="form-input">
                <option value="">全部</option>
                <option value="active">啟用</option>
                <option value="inactive">停用</option>
              </select>
            </div>
            <div class="filter-group">
              <button @click="resetFilters" class="btn btn-secondary">清除條件</button>
            </div>
          </div>
        </div>
      </details>

      <div class="groups-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ groups.length === 0 ? '尚無小組資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="groups-table">
          <div class="table-header">
            <h3>小組列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>小組名稱</th>
                <th>描述</th>
                <th>成員數量</th>
                <th>狀態</th>
                <th>建立時間</th>
                <th>更新時間</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="group in paginatedList" :key="group.id">
                <td>{{ group.groupName || '-' }}</td>
                <td class="col-desc" :title="group.description || '-'">{{ group.description || '-' }}</td>
                <td>{{ group.memberCount || 0 }}</td>
                <td>
                  <span :class="['status-badge', group.isActive ? 'status-active' : 'status-inactive']">
                    {{ group.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>{{ formatDateTime(group.createdAt) }}</td>
                <td>{{ formatDateTime(group.updatedAt) }}</td>
                <td>
                  <div class="table-actions">
                    <button @click="editGroup(group.id)" class="btn btn-edit">
                      <span class="btn__icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M12 20h9"/>
                          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/>
                        </svg>
                      </span>
                      <span>編輯</span>
                    </button>
                    <button @click="deleteGroup(group.id)" class="btn btn-delete">
                      <span class="btn__icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <polyline points="3 6 5 6 21 6"/>
                          <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                          <path d="M10 11v6"/>
                          <path d="M14 11v6"/>
                          <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                        </svg>
                      </span>
                      <span>刪除</span>
                    </button>
                  </div>
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
              <span class="pagination-info">共 {{ totalRecords }} 筆 (第 {{ currentPage }}/{{ totalPages }} 頁)</span>
            </div>
            <div class="pagination-right">
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
            </div>
          </div>
        </div>
      </div>

      <!-- 新增/編輯小組 Modal -->
      <GroupModal
        v-if="showModal"
        :show="showModal"
        :group="editingGroup"
        @close="closeModal"
        @saved="handleSaved"
      />
    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import GroupModal from '@/components/GroupModal.vue'
import { apiRequest } from '@/utils/api'

const groups = ref([])
const showModal = ref(false)
const editingGroup = ref(null)

// 查詢條件
const filters = ref({
  groupName: '',
  category: '',
  meetingLocation: '',
  status: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)
const totalRecords = ref(0)
const totalPages = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...groups.value]
  
  if (filters.value.groupName) {
    filtered = filtered.filter(group => 
      (group.groupName || '').toLowerCase().includes(filters.value.groupName.toLowerCase())
    )
  }
  
  if (filters.value.category) {
    filtered = filtered.filter(group => 
      (group.category || '').toLowerCase().includes(filters.value.category.toLowerCase())
    )
  }
  
  if (filters.value.meetingLocation) {
    filtered = filtered.filter(group => 
      (group.meetingLocation || '').toLowerCase().includes(filters.value.meetingLocation.toLowerCase())
    )
  }
  
  if (filters.value.status) {
    if (filters.value.status === 'active') {
      filtered = filtered.filter(group => group.isActive === true)
    } else if (filters.value.status === 'inactive') {
      filtered = filtered.filter(group => group.isActive === false)
    }
  }
  
  return filtered
})

// 注意：分頁現在由後端處理，但前端過濾仍然保留（過濾當前頁數據）
// 為了更好的體驗，建議將過濾邏輯移到後端
const paginatedList = computed(() => {
  // 暫時保留前端過濾，只過濾當前頁的數據
  return filteredList.value
})

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    loadGroups()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    loadGroups()
  }
}

// 跳轉到指定頁
const jumpToPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    currentPage.value = jumpPage.value
    loadGroups()
  } else {
    jumpPage.value = currentPage.value
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    groupName: '',
    category: '',
    meetingLocation: '',
    status: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => [filters.value.groupName, filters.value.category, filters.value.meetingLocation, filters.value.status], () => {
  currentPage.value = 1
  jumpPage.value = 1
  // 注意：前端過濾仍然保留，但分頁數據來自後端
  // 為了更好的體驗，建議將過濾邏輯移到後端
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadGroups()
})

const loadGroups = async () => {
  try {
    const url = `/church/groups?page=${currentPage.value - 1}&size=${recordsPerPage.value}`
    const response = await apiRequest(url, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      groups.value = data.groups || data.content || data || []
      // 更新分頁信息
      if (data.totalElements !== undefined) {
        totalRecords.value = data.totalElements
        totalPages.value = data.totalPages || 1
      } else {
        totalRecords.value = groups.value.length
        totalPages.value = 1
      }
      // 成員數量已經在後端查詢時包含，不需要逐一查詢
    }
  } catch (error) {
    console.error('載入小組失敗:', error)
    toast.error('載入小組失敗', '錯誤')
  }
}


const openAddModal = () => {
  editingGroup.value = null
  showModal.value = true
}

const editGroup = async (id) => {
  try {
    const response = await apiRequest(`/church/groups/${id}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      editingGroup.value = data.group
      showModal.value = true
    }
  } catch (error) {
    console.error('載入小組詳情失敗:', error)
    toast.error('載入小組詳情失敗', '錯誤')
  }
}

const closeModal = () => {
  showModal.value = false
  editingGroup.value = null
}

const handleSaved = () => {
  loadGroups()
  closeModal()
  toast.success('操作成功', '成功')
}

const deleteGroup = async (id) => {
  if (!confirm('確定要刪除這個小組嗎？刪除後將移除所有成員關聯。')) {
    return
  }
  
  try {
    await apiRequest(`/church/groups/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    }, '刪除中...', true)
    
    toast.success('刪除成功', '成功')
    loadGroups()
  } catch (error) {
    console.error('刪除小組失敗:', error)
    toast.error('刪除小組失敗', '錯誤')
  }
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

onMounted(() => {
  loadGroups()
})
</script>

<style scoped>
.admin-groups {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.groups-table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.table-header {
  padding: 16px 20px;
  background: #f5f5f5;
  border-bottom: 1px solid #ddd;
}

.table-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

thead {
  background: #f9f9f9;
}

th, td {
  padding: 12px 20px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

th {
  font-weight: 600;
  color: #333;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: #d4edda;
  color: #155724;
}

.status-inactive {
  background: #f8d7da;
  color: #721c24;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid #eee;
}

.pagination-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-size-select {
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.page-input {
  width: 60px;
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  text-align: center;
}

.pagination-label {
  font-size: 14px;
  color: #666;
}

/* 描述列：hover 顯示完整內容（參考定時任務管理） */
td.col-desc {
  max-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  position: relative;
  cursor: help;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

td.col-desc:hover {
  white-space: normal;
  overflow: visible;
  z-index: 10;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  padding: 1rem;
  border-radius: 0.5rem;
  max-width: 400px;
  word-wrap: break-word;
  overflow-wrap: break-word;
}
</style>

