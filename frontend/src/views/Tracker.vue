<template>
  <div class="tracker-page">
    <header class="header">
      <div class="header-top">
        <h1>QA Tracker</h1>
        <div v-if="currentUser" class="user-info">
          <span>{{ currentUser.email || '用戶' }}</span>
          <button class="logout-btn" @click="goToDashboard">返回儀表板</button>
        </div>
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
      <section class="add-record">
        <h2>{{ editingId ? '編輯記錄' : '新增記錄' }}</h2>
        <form @submit.prevent="handleSubmit" class="record-form">
          <div class="form-grid">
            <div class="form-group">
              <label for="issueNumber">Issue 編號</label>
              <input type="number" id="issueNumber" v-model.number="form.issueNumber" />
            </div>

            <div class="form-group">
              <label for="issueLink">Issue 連結</label>
              <input type="url" id="issueLink" v-model="form.issueLink" placeholder="https://..." />
            </div>

            <div class="form-group">
              <label for="status">狀態</label>
              <select id="status" v-model.number="form.status">
                <option :value="0">執行中止</option>
                <option :value="1">執行中</option>
                <option :value="2">完成</option>
              </select>
            </div>

            <div class="form-group">
              <label for="category">類型</label>
              <select id="category" v-model.number="form.category">
                <option :value="1">BUG</option>
                <option :value="2">改善</option>
                <option :value="3">優化</option>
                <option :value="4">模組</option>
                <option :value="5">QA</option>
              </select>
            </div>

            <div class="form-group">
              <label for="testPlan">Test Plan</label>
              <select id="testPlan" v-model="form.testPlan">
                <option value="0">否</option>
                <option value="1">是</option>
              </select>
            </div>

            <div class="form-group">
              <label for="bugFound">發現 BUG</label>
              <select id="bugFound" v-model.number="form.bugFound">
                <option :value="0">否</option>
                <option :value="1">是</option>
              </select>
            </div>

            <div class="form-group">
              <label for="testStartDate">開始測試日期</label>
              <input type="date" id="testStartDate" v-model="form.testStartDate" />
            </div>

            <div class="form-group">
              <label for="etaDate">預計交付日期</label>
              <input type="date" id="etaDate" v-model="form.etaDate" />
            </div>

            <div class="form-group">
              <label for="testCases">測試案例數</label>
              <input type="number" id="testCases" v-model.number="form.testCases" min="0" />
            </div>

            <div class="form-group">
              <label for="fileCount">檔案數量</label>
              <input type="number" id="fileCount" v-model.number="form.fileCount" min="0" />
            </div>

            <div class="form-group">
              <label for="optimizationPoints">可優化項目數</label>
              <input type="number" id="optimizationPoints" v-model.number="form.optimizationPoints" min="0" />
            </div>

            <div class="form-group">
              <label for="verifyFailed">驗證失敗</label>
              <select id="verifyFailed" v-model.number="form.verifyFailed">
                <option :value="0">否</option>
                <option :value="1">是</option>
              </select>
            </div>

            <div class="form-group full-width">
              <label for="feature">功能描述</label>
              <textarea id="feature" v-model="form.feature" rows="3"></textarea>
            </div>

            <div class="form-group full-width">
              <label for="memo">備註</label>
              <textarea id="memo" v-model="form.memo" rows="3"></textarea>
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary">
              {{ editingId ? '更新記錄' : '新增記錄' }}
            </button>
            <button type="button" class="btn btn-secondary" @click="resetForm" v-if="editingId">
              取消編輯
            </button>
          </div>
        </form>
      </section>

      <section class="filters">
        <h2>篩選條件</h2>
        <div class="filter-grid">
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
            <label>Issue 編號</label>
            <input type="number" v-model.number="filters.issueNumber" placeholder="搜尋 Issue 編號" />
          </div>
          <div class="filter-group">
            <label>關鍵字</label>
            <input type="text" v-model="filters.keyword" placeholder="搜尋功能描述或備註" />
          </div>
        </div>
      </section>

      <section class="records-list">
        <h2>記錄列表 (共 {{ totalRecords }} 筆)</h2>
        <div class="pagination-info">
          顯示第 {{ (currentPage - 1) * recordsPerPage + 1 }} - 
          {{ Math.min(currentPage * recordsPerPage, totalRecords) }} 筆
        </div>
        <table class="records-table">
          <thead>
            <tr>
              <th>Issue #</th>
              <th>狀態</th>
              <th>類型</th>
              <th>功能描述</th>
              <th>開始日期</th>
              <th>預計交付</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in records" :key="record.id">
              <td>
                <a v-if="record.issueLink" :href="record.issueLink" target="_blank" class="issue-link">
                  #{{ record.issueNumber }}
                </a>
                <span v-else>#{{ record.issueNumber }}</span>
              </td>
              <td>
                <span :class="getStatusClass(record.status)">
                  {{ getStatusText(record.status) }}
                </span>
              </td>
              <td>{{ getCategoryText(record.category) }}</td>
              <td class="feature-cell">{{ record.feature || '-' }}</td>
              <td>{{ formatDate(record.testStartDate) }}</td>
              <td>{{ formatDate(record.etaDate) }}</td>
              <td class="action-buttons">
                <button class="action-btn edit-btn" @click="editRecord(record.id)" title="編輯">✏️</button>
                <button class="action-btn delete-btn" @click="deleteRecord(record.id)" title="刪除">🗑️</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="pagination">
          <button @click="currentPage--" :disabled="currentPage === 1">上一頁</button>
          <span>第 {{ currentPage }} / {{ totalPages }} 頁</span>
          <button @click="currentPage++" :disabled="currentPage === totalPages">下一頁</button>
        </div>
      </section>
    </main>

    <div v-if="notification.show" class="notification" :class="notification.type">
      {{ notification.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiService } from '@/composables/useApi'

const router = useRouter()
const { currentUser } = useAuth()

const records = ref([])
const totalRecords = ref(0)
const totalPages = ref(0)
const editingId = ref(null)
const currentPage = ref(1)
const recordsPerPage = 20

const form = ref({
  issueNumber: null,
  issueLink: '',
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

const filters = ref({
  status: null,
  category: null,
  issueNumber: null,
  keyword: ''
})

const notification = ref({
  show: false,
  message: '',
  type: 'success'
})

const inProgressCount = computed(() => {
  return records.value.filter(r => r.status === 1).length
})

const completedCount = computed(() => {
  return records.value.filter(r => r.status === 2).length
})

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
      size: recordsPerPage,
      ...filters.value
    }
    
    // 移除 null 值
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === '') {
        delete params[key]
      }
    })
    
    const response = await apiService.getRecords(params)
    records.value = response.content || []
    totalRecords.value = response.totalElements || 0
    totalPages.value = response.totalPages || 1
  } catch (error) {
    console.error('載入記錄失敗:', error)
    showNotification('載入記錄失敗', 'error')
  }
}

const loadInProgressCount = async () => {
  try {
    const response = await apiService.getInProgressCount()
    // 這個 API 返回的是 count，但我們從 records 計算
  } catch (error) {
    console.error('載入統計失敗:', error)
  }
}

const handleSubmit = async () => {
  try {
    const recordData = { ...form.value }
    
    if (editingId.value) {
      await apiService.updateRecord(editingId.value, recordData)
      showNotification('記錄已更新', 'success')
    } else {
      await apiService.createRecord(recordData)
      showNotification('記錄已新增', 'success')
    }
    
    resetForm()
    await loadRecords()
  } catch (error) {
    console.error('儲存失敗:', error)
    showNotification('儲存失敗: ' + error.message, 'error')
  }
}

const editRecord = (id) => {
  const record = records.value.find(r => r.id === id)
  if (!record) {
    showNotification('找不到要編輯的記錄', 'error')
    return
  }
  
  editingId.value = id
  form.value = {
    issueNumber: record.issueNumber,
    issueLink: record.issueLink || '',
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
  
  document.querySelector('.add-record')?.scrollIntoView({ behavior: 'smooth' })
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
    issueLink: '',
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

const goToDashboard = () => {
  router.push('/')
}

watch(() => filters.value, () => {
  currentPage.value = 1
  loadRecords()
}, { deep: true })

watch(() => currentPage.value, () => {
  loadRecords()
})

onMounted(async () => {
  await loadRecords()
  await loadInProgressCount()
})
</script>

<style scoped>
.tracker-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
}

.header {
  margin-bottom: 30px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  font-size: 2rem;
  margin: 0;
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
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.summary-item {
  flex: 1;
  min-width: 200px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  padding: 20px;
  border-radius: 12px;
  text-align: center;
}

.summary-item .label {
  display: block;
  margin-bottom: 10px;
  opacity: 0.9;
}

.summary-item .amount {
  font-size: 1.5rem;
  font-weight: bold;
}

.summary-item .amount.in-progress {
  color: #fbbf24;
}

.summary-item .amount.completed {
  color: #4ade80;
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
}

.add-record,
.filters,
.records-list {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  padding: 30px;
  border-radius: 20px;
  margin-bottom: 30px;
}

.add-record h2,
.filters h2,
.records-list h2 {
  margin-bottom: 20px;
  font-size: 1.5rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  font-weight: 500;
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 10px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  font-size: 16px;
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.form-group select option {
  background: #667eea;
  color: white;
}

.form-actions {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 15px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-group label {
  font-weight: 500;
}

.filter-group input,
.filter-group select {
  padding: 8px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.filter-group input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.filter-group select option {
  background: #667eea;
  color: white;
}

.records-table {
  width: 100%;
  border-collapse: collapse;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  overflow: hidden;
}

.records-table th,
.records-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.records-table th {
  background: rgba(255, 255, 255, 0.1);
  font-weight: 600;
}

.records-table tr:hover {
  background: rgba(255, 255, 255, 0.05);
}

.issue-link {
  color: #60a5fa;
  text-decoration: none;
}

.issue-link:hover {
  text-decoration: underline;
}

.feature-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-in-progress {
  color: #fbbf24;
  font-weight: 600;
}

.status-completed {
  color: #4ade80;
  font-weight: 600;
}

.status-cancelled {
  color: #f87171;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 5px;
}

.action-btn {
  padding: 5px 10px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.edit-btn {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
}

.delete-btn {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}

.action-btn:hover {
  transform: scale(1.1);
}

.pagination-info {
  margin-bottom: 10px;
  opacity: 0.8;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-top: 20px;
}

.pagination button {
  padding: 8px 16px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  cursor: pointer;
  transition: all 0.3s;
}

.pagination button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.notification {
  position: fixed;
  bottom: 20px;
  left: 20px;
  padding: 15px 20px;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  z-index: 10000;
  animation: slideIn 0.3s;
}

.notification.success {
  background: #10b981;
}

.notification.error {
  background: #ef4444;
}

@keyframes slideIn {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(0);
  }
}
</style>
