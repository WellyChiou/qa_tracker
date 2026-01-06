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
              <div class="searchable-select">
                <input
                  type="text"
                  v-model="personSearchText"
                  @focus="personDropdownOpen = true"
                  @input="personDropdownOpen = true"
                  @blur="handlePersonBlur"
                  :placeholder="selectedPersonText || '請輸入或選擇人員'"
                  class="form-input search-input"
                />
                <div v-if="personDropdownOpen && filteredPersons.length > 0" class="dropdown-menu">
                  <div
                    v-for="person in filteredPersons"
                    :key="person.id"
                    @mousedown.prevent="selectPerson(person)"
                    class="dropdown-item"
                  >
                    {{ person.personName || person.displayName }} ({{ person.memberNo || '-' }})
                  </div>
                </div>
                <div v-if="personDropdownOpen && filteredPersons.length === 0 && personSearchText" class="dropdown-menu">
                  <div class="dropdown-item no-results">沒有找到符合的人員</div>
                </div>
              </div>
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
              <button @click="executeQuery" class="btn btn-primary" :disabled="loading || !canQuery">查詢</button>
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
            <tr v-for="(item, index) in results" :key="index" :class="{ 'historical-row': item.groupStatus === 'HISTORICAL' }">
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
                <span 
                  :class="['attendance-rate-cell', 'clickable', getAttendanceRateClass(item.attendanceRate)]"
                  @click="handleAttendanceRateClick(item)"
                  title="點擊查看場次詳細資訊"
                >
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

      <div v-else-if="!loading" class="empty-state">
        <p>請選擇查詢條件並點擊「查詢」按鈕</p>
      </div>

      <div v-if="loading" class="loading-state">
        <p>查詢中...</p>
      </div>

      <!-- 場次詳細資訊 Modal -->
      <div v-if="showSessionModal" class="modal-overlay" @click="closeSessionModal">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h3>場次詳細資訊</h3>
            <button class="modal-close" @click="closeSessionModal">×</button>
          </div>
          <div class="modal-body">
            <div v-if="sessionDetailsLoading" class="modal-loading-state">
              <div class="loading-spinner"></div>
              <p>載入中...</p>
            </div>
            <div v-else-if="sessionDetails.length === 0" class="modal-empty-state">
              <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 12h6m-3-3v6m-9 1V7a2 2 0 012-2h10a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>
              </svg>
              <p>沒有場次資料</p>
            </div>
            <div v-else>
              <div class="session-info-header">
                <div class="info-item">
                  <span class="info-label">姓名</span>
                  <span class="info-value">{{ selectedItem?.personName || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">類別</span>
                  <span class="info-value">{{ selectedItem?.category || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">年度</span>
                  <span class="info-value">{{ filters.year }}年</span>
                </div>
                <div class="info-item">
                  <span class="info-label">總場次</span>
                  <span class="info-value">{{ sessionDetails.length }} 場</span>
                </div>
                <div class="info-item">
                  <span class="info-label">已簽到</span>
                  <span class="info-value checked-count">{{ sessionDetails.filter(s => s.checkedIn).length }} 場</span>
                </div>
                <div class="info-item">
                  <span class="info-label">未簽到</span>
                  <span class="info-value unchecked-count">{{ sessionDetails.filter(s => !s.checkedIn).length }} 場</span>
                </div>
              </div>
              <div class="filter-controls">
                <label class="toggle-switch">
                  <input type="checkbox" v-model="showOnlyUnchecked" />
                  <span class="toggle-slider"></span>
                  <span class="toggle-label">只顯示未簽到場次</span>
                </label>
              </div>
              <div class="session-table-wrapper" ref="tableWrapperRef">
                <table class="session-details-table" ref="tableRef">
                  <thead ref="theadRef">
                    <tr>
                      <th>日期</th>
                      <th>場次標題</th>
                      <th>場次代碼</th>
                      <th>狀態</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(session, index) in filteredSessionDetails" :key="index">
                      <td>{{ formatDate(session.sessionDate) }}</td>
                      <td>{{ session.sessionTitle || '-' }}</td>
                      <td>{{ session.sessionCode || '-' }}</td>
                      <td>
                        <span :class="['status-badge', session.checkedIn ? 'status-checked-in' : 'status-not-checked-in']">
                          {{ session.checkedIn ? '已簽到' : '未簽到' }}
                        </span>
                        <span v-if="session.canceled" class="status-canceled">（已取消）</span>
                      </td>
                    </tr>
                    <tr v-if="filteredSessionDetails.length === 0 && showOnlyUnchecked">
                      <td colspan="4" class="no-results-message">
                        <p>沒有未簽到的場次</p>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@/composables/useToast'
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { apiRequest } from '@/utils/api'

const results = ref([])
const activePersons = ref([])
const activeGroups = ref([])
const loading = ref(false)
const showSessionModal = ref(false)
const selectedItem = ref(null)
const sessionDetails = ref([])
const sessionDetailsLoading = ref(false)
const showOnlyUnchecked = ref(false)

// 表格引用
const tableWrapperRef = ref(null)
const tableRef = ref(null)
const theadRef = ref(null)
let stickyHeadCleanup = null

// 人員搜索相關
const personSearchText = ref('')
const personDropdownOpen = ref(false)

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
const totalRecords = ref(0)
const totalPages = ref(1)

// 可用年度列表（當前年度往前5年，往後1年）
const availableYears = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let i = currentYear - 5; i <= currentYear + 1; i++) {
    years.push(i)
  }
  return years.reverse()
})

// 過濾後的人員列表
const filteredPersons = computed(() => {
  if (!personSearchText.value) {
    return activePersons.value
  }
  const searchLower = personSearchText.value.toLowerCase()
  return activePersons.value.filter(person => {
    const name = (person.personName || person.displayName || '').toLowerCase()
    const memberNo = (person.memberNo || '').toLowerCase()
    return name.includes(searchLower) || memberNo.includes(searchLower)
  })
})

// 選中的人員顯示文字
const selectedPersonText = computed(() => {
  if (!filters.value.personId) return ''
  const person = activePersons.value.find(p => p.id === filters.value.personId)
  if (!person) return ''
  return `${person.personName || person.displayName} (${person.memberNo || '-'})`
})

// 選擇人員
const selectPerson = (person) => {
  filters.value.personId = person.id
  personSearchText.value = ''
  personDropdownOpen.value = false
}

// 處理人員輸入框失去焦點
const handlePersonBlur = () => {
  // 延遲關閉，以便點擊選項時能觸發
  setTimeout(() => {
    personDropdownOpen.value = false
    // 如果搜索框為空，清空選中的人員
    if (!personSearchText.value.trim()) {
      filters.value.personId = null
    } else if (filters.value.personId) {
      // 如果有選中人員，顯示選中的人員名稱
      personSearchText.value = selectedPersonText.value
    } else {
      // 如果搜索框有內容但沒有匹配到人員，清空搜索框
      personSearchText.value = ''
    }
  }, 200)
}

// 是否可以查詢
const canQuery = computed(() => {
  if (!filters.value.year) return false
  if (filters.value.queryType === 'person' && !filters.value.personId) return false
  if (filters.value.queryType === 'group' && !filters.value.groupId) return false
  return true
})

// 注意：分頁現在由後端處理，results 已經是當前頁的數據

// 第一頁
const firstPage = () => {
  currentPage.value = 1
  jumpPage.value = 1
  query()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    query()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    query()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  query()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    query()
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
  personSearchText.value = ''
  personDropdownOpen.value = false
  results.value = []
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  if (canQuery.value) {
    currentPage.value = 1
    jumpPage.value = 1
    query()
  }
})

const loadActivePersons = async () => {
  try {
    const response = await apiRequest('/church/persons/active', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      activePersons.value = data.persons || []
      // 如果已經選中了人員，更新搜索框顯示
      if (filters.value.personId && !personSearchText.value) {
        personSearchText.value = selectedPersonText.value
      }
    }
  } catch (error) {
    console.error('載入人員列表失敗:', error)
  }
}

// 監聽 personId 變化，更新搜索框顯示
watch(() => filters.value.personId, (newId) => {
  if (newId && activePersons.value.length > 0) {
    personSearchText.value = selectedPersonText.value
  } else if (!newId) {
    personSearchText.value = ''
  }
})

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

// 執行查詢（重置頁碼）
const executeQuery = () => {
  currentPage.value = 1
  jumpPage.value = 1
  query()
}

// 查詢函數（不重置頁碼，用於分頁操作）
const query = async () => {
  if (!canQuery.value) {
    toast.warning('請填寫完整的查詢條件', '提示')
    return
  }

  loading.value = true
  results.value = []

  try {
    let url = ''
    const includeHistorical = filters.value.includeHistorical ? '&includeHistorical=true' : ''
    const pageParam = `page=${currentPage.value - 1}&size=${recordsPerPage.value}`
    
    if (filters.value.queryType === 'person') {
      url = `/church/attendance/person/${filters.value.personId}?year=${filters.value.year}${includeHistorical}&${pageParam}`
    } else if (filters.value.queryType === 'group') {
      url = `/church/attendance/group/${filters.value.groupId}?year=${filters.value.year}${includeHistorical}&${pageParam}`
    } else {
      url = `/church/attendance/all?year=${filters.value.year}${includeHistorical}&${pageParam}`
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
      let attendanceRates = data.content || data.attendanceRates || []
      
      // 如果類別為「小組」且選擇了特定小組，則過濾結果（前端過濾，但這會影響分頁準確性）
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
        // 如果後端沒有返回分頁信息，使用結果長度（兼容舊格式）
        totalRecords.value = results.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
      }
      
      toast.success(`查詢成功，共 ${totalRecords.value} 筆資料`, '查詢')
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

// 處理出席率點擊
const handleAttendanceRateClick = async (item) => {
  selectedItem.value = item
  showSessionModal.value = true
  sessionDetails.value = []
  sessionDetailsLoading.value = true

  try {
    // 將類別名稱轉換為 API 需要的格式
    let category = item.category
    if (category === '週六晚崇') {
      category = 'SATURDAY'
    } else if (category === '週日早崇') {
      category = 'SUNDAY'
    } else if (category === '活動') {
      category = 'SPECIAL'
    }
    // 如果是小組類別，category 就是小組名稱，不需要轉換

    const url = `/church/attendance/person/${item.personId}/sessions?category=${encodeURIComponent(category)}&year=${filters.value.year}${filters.value.includeHistorical ? '&includeHistorical=true' : ''}`
    
    const response = await apiRequest(url, {
      method: 'GET',
      credentials: 'include'
    }, '載入場次詳細資訊中...', true)
    
    if (response.ok) {
      const data = await response.json()
      sessionDetails.value = data.sessionDetails || []
      // 數據加載完成後設置表頭凍結
      nextTick(() => {
        setupStickyHeader()
      })
    } else {
      const errorData = await response.json()
      toast.error(errorData.error || '載入場次詳細資訊失敗', '錯誤')
    }
  } catch (error) {
    console.error('載入場次詳細資訊失敗:', error)
    toast.error('載入場次詳細資訊失敗', '錯誤')
  } finally {
    sessionDetailsLoading.value = false
  }
}

// 設置表頭凍結
const setupStickyHeader = () => {
  // 清理舊的
  if (stickyHeadCleanup) {
    stickyHeadCleanup()
    stickyHeadCleanup = null
  }

  nextTick(() => {
    const wrap = tableWrapperRef.value
    const table = tableRef.value
    const thead = theadRef.value

    if (!wrap || !table || !thead) {
      return
    }

    // 建立浮動表頭容器
    const headWrap = document.createElement('div')
    headWrap.id = '__stickyHeadWrap'
    Object.assign(headWrap.style, {
      position: 'absolute',
      left: '0',
      top: '0',
      right: '0',
      zIndex: '99999',
      background: 'transparent',
      pointerEvents: 'none',
    })

    // 確保 wrap 可定位
    const wrapPos = getComputedStyle(wrap).position
    if (wrapPos === 'static') {
      wrap.style.position = 'relative'
    }

    // 複製一份表頭
    const headTable = document.createElement('table')
    headTable.setAttribute('aria-hidden', 'true')
    headTable.className = table.className
    Object.assign(headTable.style, {
      width: '100%',
      borderCollapse: getComputedStyle(table).borderCollapse,
      borderSpacing: getComputedStyle(table).borderSpacing,
    })

    const clonedThead = thead.cloneNode(true)
    // 確保複製的表頭有正確的樣式
    const clonedThs = clonedThead.querySelectorAll('th')
    clonedThs.forEach(th => {
      // 確保背景色正確
      th.style.background = '#f8f9fa'
      th.style.color = '#333'
      th.style.fontWeight = '600'
    })
    
    headTable.appendChild(clonedThead)
    headWrap.appendChild(headTable)
    wrap.appendChild(headWrap)

    // 原本 thead 隱藏（保留欄寬計算來源）
    thead.style.visibility = 'hidden'

    // 同步欄寬
    const syncWidths = () => {
      const srcThs = thead.querySelectorAll('th')
      const dstThs = clonedThead.querySelectorAll('th')

      // 表格水平滾動時，同步 left 偏移
      headTable.style.transform = `translateX(${-wrap.scrollLeft}px)`

      // 設定每個 th 固定寬度
      srcThs.forEach((th, i) => {
        const w = th.getBoundingClientRect().width
        if (dstThs[i]) {
          dstThs[i].style.width = `${w}px`
          // 確保樣式一致
          dstThs[i].style.padding = getComputedStyle(th).padding
          dstThs[i].style.borderBottom = getComputedStyle(th).borderBottom
          dstThs[i].style.borderRight = getComputedStyle(th).borderRight
          dstThs[i].style.borderLeft = getComputedStyle(th).borderLeft
        }
      })

      // headTable 寬度跟 table 一致
      const tableW = table.getBoundingClientRect().width
      headTable.style.width = `${tableW}px`
    }

    // 讓浮動表頭跟著 wrap 滾動
    const onScroll = () => {
      headWrap.style.transform = `translateY(${wrap.scrollTop}px)`
      syncWidths()
    }

    // 初始定位 & 欄寬同步
    syncWidths()
    onScroll()

    // 綁事件
    wrap.addEventListener('scroll', onScroll, { passive: true })
    window.addEventListener('resize', syncWidths)

    // 提供清除函數
    stickyHeadCleanup = () => {
      wrap.removeEventListener('scroll', onScroll)
      window.removeEventListener('resize', syncWidths)
      thead.style.visibility = ''
      headWrap.remove()
      stickyHeadCleanup = null
    }
  })
}

// 關閉 Modal
const closeSessionModal = () => {
  // 清理表頭凍結
  if (stickyHeadCleanup) {
    stickyHeadCleanup()
    stickyHeadCleanup = null
  }
  
  showSessionModal.value = false
  selectedItem.value = null
  sessionDetails.value = []
  showOnlyUnchecked.value = false
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-TW', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit',
    weekday: 'short'
  })
}

// 過濾後的場次詳細資訊
const filteredSessionDetails = computed(() => {
  if (!showOnlyUnchecked.value) {
    return sessionDetails.value
  }
  return sessionDetails.value.filter(session => !session.checkedIn)
})

// 監聽過濾結果變化，重新同步表頭
watch(filteredSessionDetails, () => {
  if (showSessionModal.value && !sessionDetailsLoading.value) {
    nextTick(() => {
      setupStickyHeader()
    })
  }
})

onMounted(() => {
  loadActivePersons()
  loadActiveGroups()
})

onUnmounted(() => {
  // 清理表頭凍結
  if (stickyHeadCleanup) {
    stickyHeadCleanup()
    stickyHeadCleanup = null
  }
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

.attendance-rate-cell {
  cursor: pointer;
  text-decoration: underline;
  text-decoration-style: dotted;
}

.attendance-rate-cell:hover {
  opacity: 0.8;
}

/* Modal 樣式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 1000px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease-out;
  overflow: hidden;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  padding: 24px 28px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
  color: #333;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.modal-close {
  background: transparent;
  border: 1px solid #e0e0e0;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: #e9ecef;
  border-color: #d0d0d0;
  color: #333;
  transform: rotate(90deg);
}

.modal-body {
  padding: 24px 28px;
  overflow-y: auto;
  flex: 1;
  background: #f9fafb;
}

.session-info-header {
  margin-bottom: 24px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.info-value.checked-count {
  color: #059669;
}

.info-value.unchecked-count {
  color: #dc2626;
}

.filter-controls {
  margin-bottom: 20px;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.toggle-switch {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
}

.toggle-switch input[type="checkbox"] {
  display: none;
}

.toggle-slider {
  position: relative;
  width: 52px;
  height: 28px;
  background: #e5e7eb;
  border-radius: 28px;
  transition: background 0.3s ease;
  flex-shrink: 0;
}

.toggle-slider::before {
  content: '';
  position: absolute;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: white;
  top: 2px;
  left: 2px;
  transition: transform 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.toggle-switch input[type="checkbox"]:checked + .toggle-slider {
  background: linear-gradient(135deg, #4b5563 0%, #6b7280 100%);
}

.toggle-switch input[type="checkbox"]:checked + .toggle-slider::before {
  transform: translateX(24px);
}

.toggle-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.session-details-table .no-results-message {
  padding: 40px 20px;
  text-align: center;
  color: #6b7280;
}

.session-details-table .no-results-message p {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.session-table-wrapper {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow-x: auto;
  overflow-y: auto;
  max-height: calc(85vh - 280px);
  position: relative;
}

/* 自定義滾動條樣式 */
.session-table-wrapper::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.session-table-wrapper::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

.session-table-wrapper::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.session-table-wrapper::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.session-details-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: auto;
}

.session-details-table thead {
  position: sticky;
  top: 0;
  z-index: 10;
}

.session-details-table thead tr {
  background: #f8f9fa;
}

.session-details-table th {
  padding: 0.75rem;
  text-align: left;
  font-weight: 600;
  font-size: 13px;
  color: #333;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
  border-right: 1px solid #e0e0e0;
  box-shadow: 0 2px 2px -1px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 10;
}

.session-details-table th:first-child {
  border-left: 1px solid #e0e0e0;
}

.session-details-table th:last-child {
  border-right: 1px solid #e0e0e0;
}

.session-details-table tbody tr {
  transition: background-color 0.2s ease;
}

.session-details-table tbody tr:hover {
  background-color: #f8f9fa;
}

.session-details-table td {
  padding: 0.75rem;
  text-align: left;
  font-size: 14px;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  border-right: 1px solid #e0e0e0;
  background: white;
}

.session-details-table td:first-child {
  border-left: 1px solid #e0e0e0;
}

.session-details-table td:last-child {
  border-right: 1px solid #e0e0e0;
}

.session-details-table tbody tr:last-child td {
  border-bottom: none;
}

.status-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.status-checked-in {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(16, 185, 129, 0.3);
}

.status-not-checked-in {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3);
}

.status-canceled {
  color: #d97706;
  margin-left: 8px;
  font-size: 12px;
  font-weight: 600;
}

.modal-loading-state,
.modal-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.modal-loading-state p,
.modal-empty-state p {
  margin-top: 16px;
  font-size: 16px;
  color: #6b7280;
  font-weight: 500;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e5e7eb;
  border-top-color: #4b5563;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-icon {
  width: 64px;
  height: 64px;
  color: #d1d5db;
  margin-bottom: 8px;
}

/* 可搜索下拉選單 */
.searchable-select {
  position: relative;
  width: 100%;
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-height: 300px;
  overflow-y: auto;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  margin-top: 4px;
}

.dropdown-item {
  padding: 10px 12px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: background-color 0.2s;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

.dropdown-item.no-results {
  color: #999;
  cursor: default;
  font-style: italic;
}

.dropdown-item.no-results:hover {
  background-color: transparent;
}
</style>

