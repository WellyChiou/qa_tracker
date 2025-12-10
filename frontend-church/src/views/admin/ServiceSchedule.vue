<template>
  <AdminLayout>
    <div class="admin-service-schedule">
      <div class="page-header">
        <h2>服事表管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增服事表</button>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
        <div class="filter-grid">
          <div class="filter-group">
            <label>年份</label>
            <select v-model="filters.year" class="form-input">
              <option value="">全部</option>
              <option v-for="year in availableYears" :key="year" :value="year">{{ year }}年</option>
            </select>
          </div>
        </div>
      </section>

      <div class="schedule-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ schedules.length === 0 ? '尚無服事表' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="schedule-table-wrapper">
          <div class="table-header">
            <h3>服事表列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
          <table class="schedule-table">
            <thead>
              <tr>
                <th>年度</th>
                <th>日期範圍</th>
                <th>建立時間</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="schedule in paginatedList" :key="schedule.year">
                <td>{{ schedule.year }}年</td>
                <td>
                  <span v-if="schedule.startDate && schedule.endDate">
                {{ formatDate(schedule.startDate) }} ~ {{ formatDate(schedule.endDate) }}
                  </span>
                  <span v-else>-</span>
                </td>
                <td>
                  <span v-if="schedule.createdAt">{{ formatDateTime(schedule.createdAt) }}</span>
                  <span v-else>-</span>
                </td>
                <td>
              <button @click="editSchedule(schedule.year)" class="btn btn-edit">編輯</button>
              <button @click="deleteSchedule(schedule.year)" class="btn btn-delete">刪除</button>
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

      <!-- 服事表 Modal（重用現有的 ServiceScheduleModal） -->
      <ServiceScheduleModal
        v-if="showModal"
        :show="showModal"
        :mode="modalMode"
        :schedule-year="currentScheduleYear"
        :position-config="positionConfig"
        @close="closeModal"
        @saved="handleSaved"
        @updated="handleUpdated"
      />
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import ServiceScheduleModal from '@/components/ServiceScheduleModal.vue'
import { apiRequest } from '@/utils/api'

const schedules = ref([])
const showModal = ref(false)
const modalMode = ref('add')
const currentScheduleYear = ref(null)
const positionConfig = ref({})

// 查詢條件
const filters = ref({
  year: ''
})

// 計算可用的年份列表
const availableYears = computed(() => {
  const years = new Set()
  schedules.value.forEach(schedule => {
    if (schedule.year) {
      years.add(schedule.year)
    }
  })
  return Array.from(years).sort((a, b) => b - a) // 降序排列
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...schedules.value]
  
  if (filters.value.year) {
    filtered = filtered.filter(schedule => schedule.year === parseInt(filters.value.year))
  }
  
  return filtered.sort((a, b) => {
    // 按年度降序排列
    if (a.year && b.year) {
      return b.year - a.year
    }
    if (!a.year && !b.year) return 0
    if (!a.year) return 1
    if (!b.year) return -1
    return 0
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


// 監聽查詢條件變化，重置到第一頁
watch(() => filters.value.year, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const loadSchedules = async () => {
  try {
    const response = await apiRequest('/church/service-schedules', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      schedules.value = await response.json()
    }
  } catch (error) {
    console.error('載入服事表失敗:', error)
  }
}

const loadPositionConfig = async () => {
  try {
    const response = await apiRequest('/church/positions/config/full', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      // 後端返回格式：{ "config": {...}, "message": "..." }
      const config = data.config || {}
      
      // 按 sortOrder 排序崗位配置
      const sortedConfig = {}
      const sortedEntries = Object.entries(config).sort((a, b) => {
        const sortOrderA = a[1].sortOrder || 0
        const sortOrderB = b[1].sortOrder || 0
        return sortOrderA - sortOrderB
      })
      
      for (const [posCode, posData] of sortedEntries) {
        sortedConfig[posCode] = posData
      }
      
      positionConfig.value = sortedConfig
    }
  } catch (error) {
    console.error('載入崗位配置失敗:', error)
  }
}

const openAddModal = () => {
  modalMode.value = 'add'
  currentScheduleYear.value = null
  showModal.value = true
}

const editSchedule = (year) => {
  modalMode.value = 'edit'
  currentScheduleYear.value = year
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  currentScheduleYear.value = null
}

const handleSaved = () => {
  loadSchedules()
  closeModal()
}

const handleUpdated = () => {
  loadSchedules()
  closeModal()
}

const deleteSchedule = async (year) => {
  if (!confirm(`確定要刪除 ${year} 年的服事表嗎？`)) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/service-schedules/${year}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadSchedules()
    } else {
      alert('刪除失敗')
    }
  } catch (error) {
    console.error('刪除服事表失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  loadSchedules()
  loadPositionConfig()
})
</script>

<style scoped>
.admin-service-schedule {
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

.schedule-list {
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

.schedule-table-wrapper {
  overflow-x: auto;
  overflow-y: auto;
  max-height: 70vh;
  position: relative;
}

.schedule-table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  position: sticky;
  top: 0;
  z-index: 10;
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
  box-shadow: 0 2px 2px -1px rgba(0, 0, 0, 0.1);
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

/* 查詢條件和分頁樣式 */
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

