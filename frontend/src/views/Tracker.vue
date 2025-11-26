<template>
  <div class="tracker-page">
    <TopNavbar />
    <header class="header">
      <div class="header-top">
        <h1>QA Tracker</h1>
      </div>
      <div class="summary">
        <div class="summary-item">
          <span class="label">總記錄數</span>
          <span class="amount">{{ totalRecords }}</span>
        </div>
        <div class="summary-item">
          <span class="label">執行中</span>
          <span class="amount in-progress">{{ inProgressCount }}</span>
        </div>
        <div class="summary-item">
          <span class="label">已完成</span>
          <span class="amount completed">{{ completedCount }}</span>
        </div>
      </div>
    </header>

    <main class="main-content">
      <!-- Modal：建立 / 編輯資料 -->
      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-panel" @click.stop>
          <div class="modal-header">
            <div>
              <h2 class="modal-title">{{ editingId ? '編輯資料' : '新增 Issue' }}</h2>
              <div class="modal-subtitle">* Issue Number 為必填，其餘欄位皆為選填</div>
            </div>
            <button class="btn-secondary" @click="closeModal">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
              關閉
            </button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="handleSubmit" class="record-form">
              <div class="form-grid">
                <!-- Row 1 -->
                <div class="form-group">
                  <label for="status">狀態</label>
                  <select id="status" v-model.number="form.status">
                    <option :value="1" selected>執行中</option>
                    <option :value="0">執行中止</option>
                    <option :value="2">完成</option>
                  </select>
                </div>
                <div class="form-group">
                  <label for="completedAt">完成日期</label>
                  <input type="date" id="completedAt" v-model="form.completedAt" />
                </div>
                <div class="form-group">
                  <label for="issueNumber">Issue Number</label>
                  <input type="number" id="issueNumber" v-model.number="form.issueNumber" @input="updateIssueLink" placeholder="例如 1234" />
                </div>
                <div class="form-group">
                  <label for="testPlan">Test Plan</label>
                  <select id="testPlan" v-model="form.testPlan">
                    <option value="0" selected>否</option>
                    <option value="1">是</option>
                  </select>
                </div>

                <!-- Row 2 -->
                <div class="form-group">
                  <label for="category">類型</label>
                  <select id="category" v-model.number="form.category">
                    <option :value="1" selected>BUG</option>
                    <option :value="2">改善</option>
                    <option :value="3">優化</option>
                    <option :value="4">模組</option>
                    <option :value="5">QA</option>
                  </select>
                </div>
                <div class="form-group" style="grid-column: span 3;">
                  <label for="feature">功能</label>
                  <input type="text" id="feature" v-model="form.feature" placeholder="文字描述" />
                </div>

                <!-- Row 3 -->
                <div class="form-group full-width">
                  <label for="memo">Memo</label>
                  <textarea id="memo" v-model="form.memo" rows="2" placeholder="備註（非必填）"></textarea>
                </div>

                <!-- Row 4 -->
                <div class="form-group" style="grid-column: span 2;">
                  <label for="testStartDate">開始測試日期</label>
                  <input type="date" id="testStartDate" v-model="form.testStartDate" />
                </div>
                <div class="form-group" style="grid-column: span 2;">
                  <label for="etaDate">預計交付日期</label>
                  <input type="date" id="etaDate" v-model="form.etaDate" />
                </div>

                <!-- Row 5 -->
                <div class="form-group">
                  <label for="verifyFailed">驗證失敗</label>
                  <select id="verifyFailed" v-model.number="form.verifyFailed">
                    <option :value="0" selected>否</option>
                    <option :value="1">是</option>
                  </select>
                </div>
                <div class="form-group">
                  <label for="testCases">測試案例</label>
                  <input type="number" id="testCases" v-model.number="form.testCases" value="0" min="0" />
                </div>
                <div class="form-group">
                  <label for="fileCount">檔案數量</label>
                  <input type="number" id="fileCount" v-model.number="form.fileCount" value="0" min="0" />
                </div>
                <div class="form-group"></div>

                <!-- Row 6 -->
                <div class="form-group">
                  <label for="bugFound">發現 BUG</label>
                  <select id="bugFound" v-model.number="form.bugFound">
                    <option :value="0" selected>否</option>
                    <option :value="1">是</option>
                  </select>
                </div>
                <div class="form-group">
                  <label for="optimizationPoints">可優化項目</label>
                  <input type="number" id="optimizationPoints" v-model.number="form.optimizationPoints" placeholder="數字；僅在 發現BUG=1 時可填" min="0" step="1" :disabled="form.bugFound !== 1" />
                </div>
                <div class="form-group" style="grid-column: span 2;"></div>
              </div>

              <div class="form-actions">
                <button type="submit" class="btn btn-primary">儲存</button>
                <button type="button" class="btn-secondary" @click="resetForm">重設</button>
                <a v-if="form.issueNumber" :href="getIssueLink(form.issueNumber)" target="_blank" class="issue-link-preview">
                  打開 Issue
                </a>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- List Card -->
      <section class="list-section">

        <!-- Title row -->
        <div class="section-title">
          <h2>資料列表</h2>
        </div>

        <!-- Filters block -->
        <div class="filters-block">
          <div class="filters-header">
            <h3>篩選條件</h3>
          </div>
          <div class="filter-grid">
            <div class="filter-group full-width">
              <label>關鍵字搜尋</label>
              <input type="text" v-model="filters.keyword" placeholder="搜尋功能、備註或 Issue 編號..." />
            </div>
            <div class="filter-group">
              <label>狀態</label>
              <select v-model.number="filters.status">
                <option :value="null">全部</option>
                <option :value="0">執行中止</option>
                <option :value="1">執行中</option>
                <option :value="2">完成</option>
              </select>
            </div>
            <div class="filter-group">
              <label>類型</label>
              <select v-model.number="filters.category">
                <option :value="null">全部</option>
                <option :value="1">BUG</option>
                <option :value="2">改善</option>
                <option :value="3">優化</option>
                <option :value="4">模組</option>
                <option :value="5">QA</option>
              </select>
            </div>
            <div class="filter-group">
              <label>Test Plan</label>
              <select v-model="filters.testPlan">
                <option value="">全部</option>
                <option value="1">是</option>
                <option value="0">否</option>
              </select>
            </div>
            <div class="filter-group">
              <label>發現BUG</label>
              <select v-model.number="filters.bugFound">
                <option :value="null">全部</option>
                <option :value="1">是</option>
                <option :value="0">否</option>
              </select>
            </div>
            <div class="filter-group full-width">
              <label>開始日期範圍</label>
              <DateRangePicker 
                v-model="filters.testStartDateRange" 
                placeholder="選擇開始日期範圍"
              />
            </div>
            <div class="filter-group full-width">
              <label>預計交付日期範圍</label>
              <DateRangePicker 
                v-model="filters.etaDateRange" 
                placeholder="選擇預計交付日期範圍"
              />
            </div>
          </div>
        </div>

        <!-- Buttons row -->
        <div class="buttons-row">
          <div class="buttons-left">
            <button class="btn btn-primary" @click="openModal">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              新增 Issue
            </button>
            <button class="btn btn-info" @click="loadRecords">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
              搜尋
            </button>
            <button class="btn btn-warning" @click="filterByInProgress">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              ① 執行中({{ inProgressCount }})
            </button>
            <button class="btn btn-secondary" @click="clearFilters">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
              清空
            </button>
          </div>
          <div class="buttons-right">
            <button class="btn btn-success" @click="exportExcel">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              匯出 Excel
            </button>
            <button class="btn btn-warning" @click="openGitlabModal">
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                <path d="M23.546 10.93L13.067.452c-.604-.603-1.582-.603-2.188 0L.452 10.93c-.6.605-.6 1.584 0 2.189l10.48 10.477c.604.604 1.582.604 2.186 0l10.428-10.477c.603-.603.603-1.582 0-2.189z"/>
              </svg>
              GitLab Issues
            </button>
          </div>
        </div>

        <div class="pagination-info">
          顯示第 {{ (currentPage - 1) * recordsPerPage + 1 }} - 
          {{ Math.min(currentPage * recordsPerPage, totalRecords) }} 筆，共 {{ totalRecords }} 筆
        </div>
        <div class="table-wrap">
          <table class="records-table">
            <colgroup>
              <col style="width:100px" />  <!-- 操作 -->
              <col style="width:80px" />  <!-- 序號 -->
              <col style="width:80px" />  <!-- Issue# -->
              <col style="width:80px" />  <!-- 狀態 -->
              <col style="width:80px" />  <!-- 類型 -->
              <col style="width:150px" />  <!-- 功能 -->
              <col style="width:120px" />  <!-- 開始測試日期 -->
              <col style="width:120px" />  <!-- 預計交付日期 -->
              <col style="width:120px" />  <!-- 完成日期 -->
              <col style="width:80px" />  <!-- 測試案例 -->
              <col style="width:300px" />  <!-- Memo -->
            </colgroup>
            <thead>
              <tr>
                <th>操作</th>
                <th>#</th>
                <th>Issue#</th>
                <th>狀態</th>
                <th>類型</th>
                <th>功能</th>
                <th>開始測試日期</th>
                <th>預計交付日期</th>
                <th>完成日期</th>
                <th>測試案例</th>
                <th>Memo</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(record, index) in records" :key="record.id">
                <td class="action-buttons">
                  <button class="action-btn edit-btn" @click="editRecord(record.id)" title="編輯">✏️</button>
                  <button class="action-btn delete-btn" @click="deleteRecord(record.id)" title="刪除">🗑️</button>
                </td>
                <td>{{ (currentPage - 1) * recordsPerPage + index + 1 }}</td>
                <td>
                  <a v-if="record.issueNumber" :href="getIssueLink(record.issueNumber)" target="_blank" class="issue-link">
                    {{ record.issueNumber }}
                  </a>
                  <span v-else>-</span>
                </td>
                <td>
                  <span :class="getStatusClass(record.status)">
                    {{ getStatusText(record.status) }}
                  </span>
                </td>
                <td>
                  <span :class="getCategoryClass(record.category)">
                    {{ getCategoryText(record.category) }}
                  </span>
                </td>
                <td class="feature-cell">{{ record.feature || '-' }}</td>
                <td>{{ formatDate(record.testStartDate) }}</td>
                <td>{{ formatDate(record.etaDate) }}</td>
                <td>{{ formatDate(record.completedAt) }}</td>
                <td>{{ record.testCases || 0 }}</td>
                <td class="memo-cell">{{ record.memo || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
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
      </section>
    </main>

    <div v-if="notification.show" class="notification" :class="notification.type">
      {{ notification.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import TopNavbar from '@/components/TopNavbar.vue'
import DateRangePicker from '@/components/DateRangePicker.vue'
import { apiService } from '@/composables/useApi'

const records = ref([])
const totalRecords = ref(0)
const totalPages = ref(0)
const editingId = ref(null)
const currentPage = ref(1)
const recordsPerPage = ref(10)
const showModal = ref(false)
const showGitlabModal = ref(false)
const jumpPage = ref(1)

const form = ref({
  issueNumber: null,
  status: 1,
  category: null,
  feature: '',
  memo: '',
  testPlan: '0',
  bugFound: 0,
  optimizationPoints: 0,
  verifyFailed: 0,
  testCases: 0,
  fileCount: 0,
  testStartDate: '',
  etaDate: '',
  completedAt: null
})

// 根據 Issue Number 自動生成 Issue Link
const getIssueLink = (issueNumber) => {
  if (!issueNumber) return ''
  return `https://gitlab.fb-tek.com/kh/imp/-/issues/${issueNumber}`
}

// 當 Issue Number 改變時，自動更新 Issue Link（用於表單顯示）
const updateIssueLink = () => {
  // Issue Link 會在提交時自動生成，這裡不需要做任何事
}

const filters = ref({
  status: null,
  category: null,
  issueNumber: null,
  keyword: '',
  testPlan: '',
  bugFound: null,
  testStartDateRange: [], // [startDate, endDate] 格式為 'YYYY-MM-DD'
  etaDateRange: [] // [startDate, endDate] 格式為 'YYYY-MM-DD'
})

const notification = ref({
  show: false,
  message: '',
  type: 'success'
})

const inProgressCount = ref(0) // 查詢結果中執行中的數量
const completedCount = ref(0) // 查詢結果中已完成的數量

const getStatusText = (status) => {
  const statusMap = {
    0: '執行中止',
    1: '執行中',
    2: '完成'
  }
  return statusMap[status] || '未知'
}

const getStatusClass = (status) => {
  const classMap = {
    0: 'status-cancelled',
    1: 'status-in-progress',
    2: 'status-completed'
  }
  return classMap[status] || ''
}

const getCategoryText = (category) => {
  const categoryMap = {
    1: 'BUG',
    2: '改善',
    3: '優化',
    4: '模組',
    5: 'QA'
  }
  return categoryMap[category] || '未知'
}

const getCategoryClass = (category) => {
  const classMap = {
    1: 'chip-red',
    2: 'chip-indigo',
    3: 'chip-amber',
    4: 'chip-gray',
    5: 'chip-green'
  }
  return classMap[category] || 'chip-gray'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-TW')
}

const showNotification = (message, type = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => {
    notification.value.show = false
  }, 3000)
}

const loadRecords = async () => {
  try {
    const params = {
      page: currentPage.value - 1,
      size: recordsPerPage.value,
      status: filters.value.status,
      category: filters.value.category,
      issueNumber: filters.value.issueNumber,
      keyword: filters.value.keyword || undefined
    }
    
    // 處理開始日期範圍
    if (filters.value.testStartDateRange && filters.value.testStartDateRange.length > 0) {
      const sortedDates = [...filters.value.testStartDateRange].sort()
      if (sortedDates[0]) {
        params.testStartDateFrom = sortedDates[0]
      }
      if (sortedDates.length > 1 && sortedDates[1]) {
        params.testStartDateTo = sortedDates[1]
      }
    }
    
    // 處理預計交付日期範圍
    if (filters.value.etaDateRange && filters.value.etaDateRange.length > 0) {
      const sortedDates = [...filters.value.etaDateRange].sort()
      if (sortedDates[0]) {
        params.etaDateFrom = sortedDates[0]
      }
      if (sortedDates.length > 1 && sortedDates[1]) {
        params.etaDateTo = sortedDates[1]
      }
    }
    
    // 移除 null 和 undefined 值
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === undefined || params[key] === '') {
        delete params[key]
      }
    })
    
    const response = await apiService.getRecords(params)
    records.value = response.content || []
    totalRecords.value = response.totalElements || 0
    totalPages.value = response.totalPages || 1
    
    // 更新統計信息（查詢結果中的狀態統計）
    if (response.stats) {
      inProgressCount.value = response.stats.inProgress || 0
      completedCount.value = response.stats.completed || 0
    } else {
      // 降級方案：從當前記錄計算
      inProgressCount.value = records.value.filter(r => r.status === 1).length
      completedCount.value = records.value.filter(r => r.status === 2).length
    }
  } catch (error) {
    console.error('載入記錄失敗:', error)
    showNotification('載入記錄失敗', 'error')
  }
}


const handleSubmit = async () => {
  if (!form.value.issueNumber) {
    showNotification('請填寫 Issue Number', 'error')
    return
  }
  
  try {
    const recordData = { ...form.value }
    // 自動生成 Issue Link
    if (recordData.issueNumber) {
      recordData.issueLink = getIssueLink(recordData.issueNumber)
    }
    
    if (editingId.value) {
      await apiService.updateRecord(editingId.value, recordData)
      showNotification('記錄已更新', 'success')
    } else {
      await apiService.createRecord(recordData)
      showNotification('記錄已新增', 'success')
    }
    
    closeModal()
    await loadRecords()
  } catch (error) {
    console.error('儲存失敗:', error)
    showNotification('儲存失敗: ' + error.message, 'error')
  }
}

const openModal = () => {
  editingId.value = null
  resetForm()
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingId.value = null
  resetForm()
}

const openGitlabModal = () => {
  showGitlabModal.value = true
  showNotification('GitLab Issues 功能開發中', 'info')
}

const filterByInProgress = () => {
  filters.value.status = 1
  currentPage.value = 1
  loadRecords()
}

const clearFilters = () => {
  filters.value = {
    status: null,
    category: null,
    issueNumber: null,
    keyword: '',
    testPlan: '',
    bugFound: null,
    testStartDateRange: [],
    etaDateRange: []
  }
  currentPage.value = 1
  loadRecords()
}

const exportExcel = async () => {
  try {
    // 使用當前篩選條件獲取所有記錄
    const params = {
      status: filters.value.status,
      category: filters.value.category,
      issueNumber: filters.value.issueNumber,
      keyword: filters.value.keyword || undefined,
      testPlan: filters.value.testPlan || undefined,
      bugFound: filters.value.bugFound,
      page: 0,
      size: 10000 // 獲取大量記錄用於匯出
    }
    
    if (filters.value.testStartDateRange && filters.value.testStartDateRange.length > 0) {
      const sortedDates = [...filters.value.testStartDateRange].sort()
      if (sortedDates[0]) params.testStartDateFrom = sortedDates[0]
      if (sortedDates.length > 1 && sortedDates[1]) params.testStartDateTo = sortedDates[1]
    }
    
    if (filters.value.etaDateRange && filters.value.etaDateRange.length > 0) {
      const sortedDates = [...filters.value.etaDateRange].sort()
      if (sortedDates[0]) params.etaDateFrom = sortedDates[0]
      if (sortedDates.length > 1 && sortedDates[1]) params.etaDateTo = sortedDates[1]
    }
    
    // 移除 null 和 undefined 值
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === undefined || params[key] === '') {
        delete params[key]
      }
    })
    
    const response = await apiService.getRecords(params)
    const allRecords = response.content || []
    
    // 這裡可以實現 Excel 匯出邏輯
    // 目前先顯示通知
    showNotification(`準備匯出 ${allRecords.length} 筆記錄（Excel 匯出功能開發中）`, 'info')
  } catch (error) {
    console.error('匯出失敗:', error)
    showNotification('匯出失敗', 'error')
  }
}

const editRecord = async (id) => {
  const record = records.value.find(r => r.id === id)
  if (!record) {
    showNotification('找不到要編輯的記錄', 'error')
    return
  }
  
  editingId.value = id
  form.value = {
    issueNumber: record.issueNumber,
    status: record.status,
    category: record.category,
    feature: record.feature || '',
    memo: record.memo || '',
    testPlan: record.testPlan || '0',
    bugFound: record.bugFound || 0,
    optimizationPoints: record.optimizationPoints || 0,
    verifyFailed: record.verifyFailed || 0,
    testCases: record.testCases || 0,
    fileCount: record.fileCount || 0,
    testStartDate: record.testStartDate || '',
    etaDate: record.etaDate || '',
    completedAt: record.completedAt || null
  }
  
  showModal.value = true
}

const deleteRecord = async (id) => {
  if (!confirm('確定要刪除這筆記錄嗎？')) {
    return
  }
  
  try {
    await apiService.deleteRecord(id)
    showNotification('記錄已刪除', 'success')
    await loadRecords()
  } catch (error) {
    console.error('刪除失敗:', error)
    showNotification('刪除失敗', 'error')
  }
}

const resetForm = () => {
  editingId.value = null
  form.value = {
    issueNumber: null,
    status: 1,
    category: null,
    feature: '',
    memo: '',
    testPlan: '0',
    bugFound: 0,
    optimizationPoints: 0,
    verifyFailed: 0,
    testCases: 0,
    fileCount: 0,
    testStartDate: '',
    etaDate: '',
    completedAt: null
  }
}


watch(() => filters.value, (newVal, oldVal) => {
  // 排除日期範圍的變化，日期範圍只有在點擊"確定"按鈕時才應該觸發查詢
  // 但由於用戶要求點擊"搜尋"按鈕才查詢，所以這裡完全移除自動查詢
  // 日期範圍的變化不會觸發查詢，只有點擊"搜尋"按鈕才會
  if (newVal.testStartDateRange !== oldVal?.testStartDateRange || 
      newVal.etaDateRange !== oldVal?.etaDateRange) {
    // 日期範圍變化時，不自動觸發查詢
    return
  }
  currentPage.value = 1
  loadRecords()
}, { deep: true })

watch(() => currentPage.value, (newVal) => {
  jumpPage.value = newVal
  loadRecords()
})

watch(() => recordsPerPage.value, () => {
  currentPage.value = 1
  loadRecords()
})


const jumpToPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    currentPage.value = jumpPage.value
  } else {
    jumpPage.value = currentPage.value
  }
}

// ESC 鍵關閉 Modal
const handleEscape = (e) => {
  if (e.key === 'Escape' && showModal.value) {
    closeModal()
  }
}

onMounted(async () => {
  await loadRecords()
  document.addEventListener('keydown', handleEscape)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscape)
})
</script>

<style scoped>
.tracker-page {
  min-height: 100vh;
  background: #f5f5f5;
  color: #333;
  padding: 20px;
}

.tracker-page::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 50%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(118, 75, 162, 0.3) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 30px;
  border-radius: 15px;
  margin-bottom: 30px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  font-size: 2.5em;
  margin: 0;
  text-align: center;
  flex: 1;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(15px);
  padding: 10px 18px;
  border-radius: 12px;
}

.logout-btn {
  background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.logout-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.summary-item {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  padding: var(--spacing-xl);
  border-radius: var(--border-radius-lg);
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: var(--shadow-lg);
  transition: var(--transition);
  position: relative;
  overflow: hidden;
}

.summary-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.5) 0%, transparent 100%);
}

.summary-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl);
  background: rgba(255, 255, 255, 0.2);
}

.summary-item .label {
  display: block;
  margin-bottom: var(--spacing-md);
  opacity: 0.95;
  font-size: 0.95rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.summary-item .amount {
  font-size: 2rem;
  font-weight: 700;
  line-height: 1.2;
}

.summary-item .amount.in-progress {
  color: #fbbf24;
  text-shadow: 0 2px 10px rgba(251, 191, 36, 0.3);
}

.summary-item .amount.completed {
  color: #4ade80;
  text-shadow: 0 2px 10px rgba(74, 222, 128, 0.3);
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* Modal 樣式 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  overflow-y: auto;
}

.modal-panel {
  width: 100%;
  max-width: 56rem;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  margin: 2rem 0;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(to right, #f8fafc, white);
  border-radius: 1rem 1rem 0 0;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.modal-subtitle {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: 0.25rem;
}

.modal-body {
  padding: 1.5rem;
}

.list-section {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.08);
  padding: 1.5rem;
  border: 1px solid #e2e8f0;
}

.section-title {
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #e2e8f0;
}

.section-title h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filters-block {
  background: #f8fafc;
  border-radius: 0.75rem;
  padding: 1.25rem;
  margin-bottom: 1rem;
  border: 1px solid #e2e8f0;
}

.filters-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.filters-header h3 {
  font-weight: 600;
  color: #475569;
  margin: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0.375rem;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border-radius: 0.75rem;
  border: 1.5px solid #cbd5e1;
  outline: none;
  background: white;
  transition: all 0.2s ease;
  font-size: 0.875rem;
  color: #1e293b;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
  border-color: #818cf8;
}

.form-group input:hover:not(:focus),
.form-group select:hover:not(:focus),
.form-group textarea:hover:not(:focus) {
  border-color: #94a3b8;
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: #94a3b8;
}

.form-group select option {
  background: white;
  color: #1e293b;
  padding: 0.5rem;
}

.form-actions {
  display: flex;
  gap: var(--spacing-md);
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-lg);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.buttons-row {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

@media (min-width: 768px) {
  .buttons-row {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
}

.buttons-left,
.buttons-right {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.btn {
  padding: 0.75rem 1.25rem;
  border-radius: 0.75rem;
  font-weight: 600;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  white-space: nowrap;
  min-height: 2.5rem;
}

.btn:hover {
  transform: translateY(-1px);
}

.btn:active {
  transform: translateY(0);
}

.btn-primary {
  background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
}

.btn-primary:hover {
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
}

.btn-info {
  background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.3);
}

.btn-info:hover {
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.4);
}

.btn-warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
}

.btn-warning:hover {
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
}

.btn-success {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.btn-success:hover {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.btn-secondary {
  padding: 0.75rem 1.25rem;
  border-radius: 0.75rem;
  border: 1.5px solid #cbd5e1;
  background: white;
  color: #475569;
  font-weight: 600;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  cursor: pointer;
  min-height: 2.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn-secondary:hover {
  background: #f8fafc;
  border-color: #94a3b8;
  transform: translateY(-1px);
}

.issue-link-preview {
  color: #4f46e5;
  font-weight: 600;
  text-decoration: underline;
  margin-left: 0.5rem;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 0.75rem;
}

@media (min-width: 768px) {
  .filter-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1024px) {
  .filter-grid {
    grid-template-columns: repeat(5, 1fr);
  }
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.filter-group.full-width {
  grid-column: 1 / -1;
}

@media (min-width: 1024px) {
  .filter-group.full-width {
    grid-column: span 2;
  }
}

.filter-group label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0.375rem;
}

.filter-group input,
.filter-group select {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border-radius: 0.75rem;
  border: 1.5px solid #cbd5e1;
  outline: none;
  background: white;
  transition: all 0.2s ease;
  font-size: 0.875rem;
  color: #1e293b;
  box-sizing: border-box;
}

.filter-group input:focus,
.filter-group select:focus {
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
  border-color: #818cf8;
}

.filter-group input::placeholder {
  color: #94a3b8;
}

.filter-group select option {
  background: white;
  color: #1e293b;
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 1rem;
}

.records-table {
  width: 100%;
  min-width: 1600px;
  table-layout: fixed;
  border-collapse: collapse;
  background: white;
  font-size: 0.875rem;
}

.records-table th,
.records-table td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
  white-space: normal;
  word-break: break-word;
}

.records-table th {
  position: sticky;
  top: 0;
  background: #f8fafc;
  white-space: nowrap;
  font-weight: 600;
  z-index: 10;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.75rem;
}

.records-table tbody tr {
  transition: background-color 0.15s ease;
}

.records-table tbody tr:hover {
  background: #f8fafc;
}

.records-table tbody tr:last-child td {
  border-bottom: none;
}

.memo-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.issue-link {
  color: #4f46e5;
  text-decoration: none;
  font-weight: 600;
}

.issue-link:hover {
  text-decoration: underline;
  color: #4338ca;
}

.feature-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-in-progress {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}

.status-completed {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}

.status-cancelled {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.chip-green {
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}

.chip-gray {
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #e2e8f0;
}

.chip-red {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.chip-indigo {
  background: #eef2ff;
  color: #3730a3;
  border: 1px solid #c7d2fe;
}

.chip-amber {
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}

.action-buttons {
  display: flex;
  gap: 5px;
}

.action-btn {
  padding: 0.375rem 0.75rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s ease;
  background: white;
}

.edit-btn {
  color: #007bff;
  border-color: #007bff;
}

.edit-btn:hover {
  background: #007bff;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 123, 255, 0.3);
}

.delete-btn {
  color: #dc3545;
  border-color: #dc3545;
}

.delete-btn:hover {
  background: #dc3545;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(220, 53, 69, 0.3);
}

.pagination-info {
  margin-bottom: 10px;
  opacity: 0.8;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-lg);
  margin-top: var(--spacing-xl);
  flex-wrap: wrap;
}

.pagination-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.pagination-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.page-size-select {
  padding: 10px 16px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-radius: var(--border-radius);
  background: rgba(255, 255, 255, 0.15);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: var(--transition);
  min-width: 80px;
}

.page-size-select:focus {
  outline: none;
  border-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.25);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.1);
}

.page-size-select option {
  background: #667eea;
  color: white;
  padding: 8px;
}

.page-size-info {
  font-size: 14px;
  opacity: 0.9;
  font-weight: 500;
}

.pagination button {
  padding: 10px 20px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-radius: var(--border-radius);
  background: rgba(255, 255, 255, 0.15);
  color: white;
  cursor: pointer;
  transition: var(--transition);
  font-weight: 600;
  backdrop-filter: blur(10px);
}

.pagination button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

.page-select {
  padding: 10px 16px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-radius: var(--border-radius);
  background: rgba(255, 255, 255, 0.15);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin: 0 var(--spacing-sm);
  min-width: 70px;
  backdrop-filter: blur(10px);
  transition: var(--transition);
}

.page-select option {
  background: #667eea;
  color: white;
  padding: 8px;
}

.page-select:focus {
  outline: none;
  border-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.25);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.1);
}

.notification {
  position: fixed;
  bottom: var(--spacing-xl);
  left: var(--spacing-xl);
  padding: var(--spacing-lg) var(--spacing-xl);
  border-radius: var(--border-radius-lg);
  color: white;
  font-weight: 600;
  z-index: 10000;
  animation: slideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-xl);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  min-width: 300px;
}

.notification.success {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.95) 0%, rgba(5, 150, 105, 0.95) 100%);
}

.notification.error {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.95) 0%, rgba(220, 38, 38, 0.95) 100%);
}

.notification.info {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.95) 0%, rgba(37, 99, 235, 0.95) 100%);
}

@keyframes slideIn {
  from {
    transform: translateX(-120%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.w-4 {
  width: 1rem;
}

.h-4 {
  height: 1rem;
}

.w-5 {
  width: 1.25rem;
}

.h-5 {
  height: 1.25rem;
}
</style>
