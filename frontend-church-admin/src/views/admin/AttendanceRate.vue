<template>
  <AdminLayout>
    <div class="admin-attendance-rate">
      <div class="page-header">
        <h2>出席率查詢</h2>
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
              <label>年度 <span class="required">*</span></label>
              <select v-model.number="filters.year" class="form-input" required>
                <option v-for="y in availableYears" :key="y" :value="y">{{ y }}年</option>
              </select>
            </div>
            <div class="filter-group">
              <label>查詢類型</label>
              <select v-model="filters.queryType" class="form-input">
                <option value="person">個人出席率</option>
                <option value="group">小組出席率</option>
                <option value="all">全部人員</option>
              </select>
            </div>
            <div class="filter-group" v-if="filters.queryType === 'person'">
              <label>人員</label>
              <select v-model.number="filters.personId" class="form-input">
                <option :value="null">請選擇人員</option>
                <option v-for="person in activePersons" :key="person.id" :value="person.id">
                  {{ person.personName || person.displayName }} ({{ person.memberNo || '-' }})
                </option>
              </select>
            </div>
            <div class="filter-group" v-if="filters.queryType === 'group'">
              <label>小組</label>
              <select v-model.number="filters.groupId" class="form-input">
                <option :value="null">請選擇小組</option>
                <option v-for="group in activeGroups" :key="group.id" :value="group.id">
                  {{ group.groupName }}
                </option>
              </select>
            </div>
            <div class="filter-group" v-if="filters.queryType === 'all'">
              <label>類別</label>
              <select v-model="filters.category" class="form-input">
                <option value="">全部類別</option>
                <option value="SATURDAY">週六晚崇</option>
                <option value="SUNDAY">週日早崇</option>
                <option value="WEEKDAY">小組</option>
                <option value="SPECIAL">活動</option>
              </select>
            </div>
            <div class="filter-group" v-if="filters.queryType === 'all' && filters.category === 'WEEKDAY'">
              <label>篩選小組</label>
              <select v-model.number="filters.groupFilterId" class="form-input">
                <option :value="null">全部小組</option>
                <option v-for="group in activeGroups" :key="group.id" :value="group.id">
                  {{ group.groupName }}
                </option>
              </select>
            </div>
            <div class="filter-group">
              <label>
                <input
                  type="checkbox"
                  v-model="filters.includeHistorical"
                  class="form-checkbox"
                />
                <span class="checkbox-label">包含歷史記錄</span>
              </label>
            </div>
            <div class="filter-group">
              <button @click="query" class="btn btn-primary" :disabled="loading || !canQuery">查詢</button>
              <button @click="resetFilters" class="btn btn-secondary">清除條件</button>
            </div>
          </div>
        </div>
      </details>

      <!-- 結果列表 -->
      <div class="attendance-results" v-if="results.length > 0">
        <div class="table-header">
          <h3>出席率統計 (共 {{ results.length }} 筆)</h3>
        </div>
        <table>
          <thead>
            <tr>
              <th>姓名</th>
              <th>顯示名稱</th>
              <th>會員編號</th>
              <th>類別/小組</th>
              <th>狀態</th>
              <th>總場次</th>
              <th>已簽到</th>
              <th>出席率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in paginatedList" :key="index" :class="{ 'historical-row': item.groupStatus === 'HISTORICAL' }">
              <td>{{ item.personName || '-' }}</td>
              <td>{{ item.displayName || '-' }}</td>
              <td>{{ item.memberNo || '-' }}</td>
              <td>{{ item.category || '-' }}</td>
              <td>
                <!-- 只有小組類型才顯示狀態，主日類型（週六晚崇、週日早崇、活動）不顯示 -->
                <span v-if="isGroupCategory(item.category) && item.groupStatus" 
                      :class="['status-badge', item.groupStatus === 'CURRENT' ? 'status-current' : 'status-historical']">
                  {{ item.groupStatus === 'CURRENT' ? '目前' : '歷史' }}
                </span>
                <span v-else-if="isGroupCategory(item.category)">-</span>
                <span v-else>-</span>
              </td>
              <td>{{ item.totalSessions || 0 }}</td>
              <td>{{ item.checkedInSessions || 0 }}</td>
              <td>
                <span :class="getAttendanceRateClass(item.attendanceRate)">
                  {{ formatAttendanceRate(item.attendanceRate) }}%
                </span>
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
            <span class="pagination-info">共 {{ results.length }} 筆 (第 {{ currentPage }}/{{ totalPages }} 頁)</span>
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

      <div v-else-if="!loading" class="empty-state">
        <p>請選擇查詢條件並點擊「查詢」按鈕</p>
      </div>

      <div v-if="loading" class="loading-state">
        <p>查詢中...</p>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@/composables/useToast'
import { ref, computed, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { apiRequest } from '@/utils/api'

const results = ref([])
const activePersons = ref([])
const activeGroups = ref([])
const loading = ref(false)

// 查詢條件
const filters = ref({
  year: new Date().getFullYear(),
  queryType: 'person',
  personId: null,
  groupId: null,
  category: '',
  groupFilterId: null, // 當類別為「小組」時，用於篩選特定小組
  includeHistorical: false
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)

// 可用年度列表（當前年度往前5年，往後1年）
const availableYears = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let i = currentYear - 5; i <= currentYear + 1; i++) {
    years.push(i)
  }
  return years.reverse()
})

// 是否可以查詢
const canQuery = computed(() => {
  if (!filters.value.year) return false
  if (filters.value.queryType === 'person' && !filters.value.personId) return false
  if (filters.value.queryType === 'group' && !filters.value.groupId) return false
  return true
})

// 分頁後的列表
const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * recordsPerPage.value
  return results.value.slice(start, start + recordsPerPage.value)
})

// 總頁數
const totalPages = computed(() => {
  return Math.max(1, Math.ceil(results.value.length / recordsPerPage.value))
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
    year: new Date().getFullYear(),
    queryType: 'person',
    personId: null,
    groupId: null,
    category: '',
    groupFilterId: null,
    includeHistorical: false
  }
  results.value = []
  currentPage.value = 1
  jumpPage.value = 1
}

const loadActivePersons = async () => {
  try {
    const response = await apiRequest('/church/persons/active', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      activePersons.value = data.persons || []
    }
  } catch (error) {
    console.error('載入人員列表失敗:', error)
  }
}

const loadActiveGroups = async () => {
  try {
    const response = await apiRequest('/church/groups/active', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      activeGroups.value = data.groups || []
    }
  } catch (error) {
    console.error('載入小組列表失敗:', error)
  }
}

const query = async () => {
  if (!canQuery.value) {
    toast.warning('請填寫完整的查詢條件', '提示')
    return
  }

  loading.value = true
  results.value = []
  currentPage.value = 1
  jumpPage.value = 1

  try {
    let url = ''
    const includeHistorical = filters.value.includeHistorical ? '&includeHistorical=true' : ''
    if (filters.value.queryType === 'person') {
      url = `/church/attendance/person/${filters.value.personId}?year=${filters.value.year}${includeHistorical}`
    } else if (filters.value.queryType === 'group') {
      url = `/church/attendance/group/${filters.value.groupId}?year=${filters.value.year}${includeHistorical}`
    } else {
      url = `/church/attendance/all?year=${filters.value.year}${includeHistorical}`
      if (filters.value.category) {
        url += `&category=${filters.value.category}`
      }
    }

    const response = await apiRequest(url, {
      method: 'GET',
      credentials: 'include'
    }, '查詢中...', true)
    
    if (response.ok) {
      const data = await response.json()
      let attendanceRates = data.attendanceRates || []
      
      // 如果類別為「小組」且選擇了特定小組，則過濾結果
      if (filters.value.queryType === 'all' && 
          filters.value.category === 'WEEKDAY' && 
          filters.value.groupFilterId) {
        // 獲取選擇的小組名稱
        const selectedGroup = activeGroups.value.find(g => g.id === filters.value.groupFilterId)
        if (selectedGroup) {
          // 只保留該小組的數據（category 欄位應該是小組名稱）
          attendanceRates = attendanceRates.filter(rate => rate.category === selectedGroup.groupName)
        }
      }
      
      results.value = attendanceRates
      toast.success(`查詢成功，共 ${results.value.length} 筆資料`, '查詢')
    } else {
      const errorData = await response.json()
      toast.error(errorData.error || '查詢失敗', '錯誤')
    }
  } catch (error) {
    console.error('查詢出席率失敗:', error)
    toast.error('查詢出席率失敗', '錯誤')
  } finally {
    loading.value = false
  }
}

const formatAttendanceRate = (rate) => {
  if (rate == null) return '0.00'
  return typeof rate === 'number' ? rate.toFixed(2) : rate.toString()
}

const getAttendanceRateClass = (rate) => {
  if (rate == null) return 'rate-low'
  const numRate = typeof rate === 'number' ? rate : parseFloat(rate.toString())
  if (numRate >= 80) return 'rate-high'
  if (numRate >= 60) return 'rate-medium'
  return 'rate-low'
}

// 判斷是否為小組類別（不是主日類型）
const isGroupCategory = (category) => {
  if (!category) return false
  // 主日類型：週六晚崇、週日早崇、活動
  const mainServiceCategories = ['週六晚崇', '週日早崇', '活動']
  return !mainServiceCategories.includes(category)
}

onMounted(() => {
  loadActivePersons()
  loadActiveGroups()
})
</script>

<style scoped>
.admin-attendance-rate {
  padding: 20px;
}

.page-header {
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

.loading-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.attendance-results {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-top: 20px;
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

.rate-high {
  color: #28a745;
  font-weight: 600;
}

.rate-medium {
  color: #ffc107;
  font-weight: 600;
}

.rate-low {
  color: #dc3545;
  font-weight: 600;
}

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-current {
  background-color: #d4edda;
  color: #155724;
}

.status-historical {
  background-color: #e2e3e5;
  color: #383d41;
}

.historical-row {
  opacity: 0.8;
  font-style: italic;
}

.form-checkbox {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
  margin-right: 0.5rem;
}

.checkbox-label {
  font-weight: normal;
  cursor: pointer;
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
</style>

