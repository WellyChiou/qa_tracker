<template>
  <div class="expenses-page">
    <header class="header">
      <div class="header-top">
        <h1>家庭記帳系統</h1>
        <div v-if="currentUser" class="user-info">
          <span>{{ currentUser.email || '用戶' }}</span>
          <button class="logout-btn" @click="goToDashboard">返回儀表板</button>
        </div>
      </div>
      <div class="summary">
        <div class="summary-item">
          <span class="label">本月收入</span>
          <span class="amount income">${{ formatNumber(monthlyIncome) }}</span>
        </div>
        <div class="summary-item">
          <span class="label">本月支出</span>
          <span class="amount expense">${{ formatNumber(monthlyExpense) }}</span>
        </div>
        <div class="summary-item">
          <span class="label">本月淨收入</span>
          <span class="amount net-income">${{ formatNumber(monthlyNetIncome) }}</span>
        </div>
      </div>
    </header>

    <main class="main-content">
      <section class="add-record">
        <h2>{{ editingId ? '編輯記錄' : '新增記帳記錄' }}</h2>
        <form @submit.prevent="handleSubmit" class="record-form">
          <div class="form-grid">
            <div class="form-group">
              <label for="member">家庭成員</label>
              <select id="member" v-model="form.member" required>
                <option value="">請選擇成員</option>
                <option value="爸爸">爸爸</option>
                <option value="媽媽">媽媽</option>
                <option value="孩子">孩子</option>
                <option value="其他">其他</option>
              </select>
            </div>

            <div class="form-group">
              <label for="type">類型</label>
              <select id="type" v-model="form.type" required>
                <option value="">請選擇類型</option>
                <option value="支出">支出</option>
                <option value="收入">收入</option>
              </select>
            </div>

            <div class="form-group">
              <label for="mainCategory">類別</label>
              <select id="mainCategory" v-model="form.mainCategory" required>
                <option value="">請選擇類別</option>
                <option v-for="cat in mainCategories" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>

            <div class="form-group">
              <label for="subCategory">細項</label>
              <select id="subCategory" v-model="form.subCategory" required>
                <option value="">請選擇細項</option>
                <option v-for="sub in subCategories" :key="sub" :value="sub">{{ sub }}</option>
              </select>
            </div>

            <div class="form-group">
              <label for="amount">金額</label>
              <input type="number" id="amount" v-model.number="form.amount" step="0.01" required />
            </div>

            <div class="form-group">
              <label for="currency">幣別</label>
              <select id="currency" v-model="form.currency">
                <option value="TWD">TWD</option>
                <option value="USD">USD</option>
                <option value="EUR">EUR</option>
                <option value="JPY">JPY</option>
                <option value="CNY">CNY</option>
              </select>
            </div>

            <div class="form-group">
              <label for="date">日期</label>
              <input type="date" id="date" v-model="form.date" required />
            </div>

            <div class="form-group full-width">
              <label for="description">備註</label>
              <input type="text" id="description" v-model="form.description" />
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
            <label>年份</label>
            <select v-model.number="filters.year">
              <option :value="null">全部</option>
              <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
            </select>
          </div>
          <div class="filter-group">
            <label>月份</label>
            <select v-model.number="filters.month">
              <option :value="null">全部</option>
              <option v-for="m in 12" :key="m" :value="m">{{ m }}月</option>
            </select>
          </div>
          <div class="filter-group">
            <label>成員</label>
            <select v-model="filters.member">
              <option value="">全部</option>
              <option value="爸爸">爸爸</option>
              <option value="媽媽">媽媽</option>
              <option value="孩子">孩子</option>
              <option value="其他">其他</option>
            </select>
          </div>
          <div class="filter-group">
            <label>類型</label>
            <select v-model="filters.type">
              <option value="">全部</option>
              <option value="支出">支出</option>
              <option value="收入">收入</option>
            </select>
          </div>
          <div class="filter-group">
            <label>類別</label>
            <select v-model="filters.mainCategory">
              <option value="">全部</option>
              <option v-for="cat in mainCategories" :key="cat" :value="cat">{{ cat }}</option>
            </select>
          </div>
        </div>
      </section>

      <section class="records-list">
        <h2>記帳記錄 (共 {{ filteredRecords.length }} 筆)</h2>
        <div class="pagination-info">
          顯示第 {{ (currentPage - 1) * recordsPerPage + 1 }} - 
          {{ Math.min(currentPage * recordsPerPage, filteredRecords.length) }} 筆
        </div>
        <table class="records-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>成員</th>
              <th>類型</th>
              <th>類別</th>
              <th>細項</th>
              <th>金額</th>
              <th>備註</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in paginatedRecords" :key="record.id">
              <td>{{ formatDate(record.date) }}</td>
              <td>{{ record.member }}</td>
              <td>{{ record.type }}</td>
              <td>{{ record.mainCategory }}</td>
              <td>{{ record.subCategory }}</td>
              <td :class="record.type === '收入' ? 'income' : 'expense'">
                {{ record.type === '收入' ? '+' : '-' }}${{ formatNumber(record.amount) }}
                ({{ record.currency || 'TWD' }})
              </td>
              <td>{{ record.description || '-' }}</td>
              <td class="action-buttons">
                <button class="action-btn edit-btn" @click="editRecord(record.id)" title="編輯">✏️</button>
                <button class="action-btn copy-btn" @click="copyRecord(record.id)" title="複製">📋</button>
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
const editingId = ref(null)
const currentPage = ref(1)
const recordsPerPage = 20

const form = ref({
  member: '',
  type: '',
  mainCategory: '',
  subCategory: '',
  amount: 0,
  currency: 'TWD',
  date: new Date().toISOString().split('T')[0],
  description: ''
})

const filters = ref({
  year: null,
  month: null,
  member: '',
  type: '',
  mainCategory: ''
})

const notification = ref({
  show: false,
  message: '',
  type: 'success'
})

const mainCategories = ['食', '衣', '住', '行', '育', '樂', '醫', '其他']
const subCategoryMap = {
  '食': ['早餐', '午餐', '晚餐', '點心', '飲料', '食材', '外食', '其他'],
  '衣': ['服裝', '鞋子', '配件', '保養品', '其他'],
  '住': ['房租', '水電', '瓦斯', '網路', '管理費', '其他'],
  '行': ['油錢', '停車費', '大眾運輸', '計程車', '其他'],
  '育': ['學費', '書籍', '文具', '補習費', '其他'],
  '樂': ['娛樂', '旅遊', '運動', '其他'],
  '醫': ['醫療', '藥品', '健檢', '其他'],
  '其他': ['其他']
}

const subCategories = computed(() => {
  if (!form.value.mainCategory) return []
  return subCategoryMap[form.value.mainCategory] || []
})

const years = computed(() => {
  const currentYear = new Date().getFullYear()
  return Array.from({ length: 10 }, (_, i) => currentYear - i)
})

const filteredRecords = computed(() => {
  let filtered = [...records.value]
  
  if (filters.value.year) {
    filtered = filtered.filter(r => new Date(r.date).getFullYear() === filters.value.year)
  }
  if (filters.value.month) {
    filtered = filtered.filter(r => new Date(r.date).getMonth() + 1 === filters.value.month)
  }
  if (filters.value.member) {
    filtered = filtered.filter(r => r.member === filters.value.member)
  }
  if (filters.value.type) {
    filtered = filtered.filter(r => r.type === filters.value.type)
  }
  if (filters.value.mainCategory) {
    filtered = filtered.filter(r => r.mainCategory === filters.value.mainCategory)
  }
  
  return filtered.sort((a, b) => new Date(b.date) - new Date(a.date))
})

const paginatedRecords = computed(() => {
  const start = (currentPage.value - 1) * recordsPerPage
  return filteredRecords.value.slice(start, start + recordsPerPage)
})

const totalPages = computed(() => {
  return Math.ceil(filteredRecords.value.length / recordsPerPage)
})

const monthlyIncome = computed(() => {
  const now = new Date()
  return filteredRecords.value
    .filter(r => r.type === '收入' && 
      new Date(r.date).getFullYear() === now.getFullYear() &&
      new Date(r.date).getMonth() === now.getMonth())
    .reduce((sum, r) => sum + (r.amount || 0), 0)
})

const monthlyExpense = computed(() => {
  const now = new Date()
  return filteredRecords.value
    .filter(r => r.type === '支出' && 
      new Date(r.date).getFullYear() === now.getFullYear() &&
      new Date(r.date).getMonth() === now.getMonth())
    .reduce((sum, r) => sum + (r.amount || 0), 0)
})

const monthlyNetIncome = computed(() => {
  return monthlyIncome.value - monthlyExpense.value
})

const formatNumber = (num) => {
  return Math.round(num || 0).toLocaleString('zh-TW')
}

const formatDate = (dateStr) => {
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
    const expenses = await apiService.getAllExpenses()
    records.value = expenses.map(expense => ({
      id: expense.id,
      member: expense.member,
      type: expense.type,
      mainCategory: expense.mainCategory,
      subCategory: expense.subCategory,
      amount: parseFloat(expense.amount),
      currency: expense.currency || 'TWD',
      date: expense.date,
      description: expense.description
    }))
  } catch (error) {
    console.error('載入記錄失敗:', error)
    showNotification('載入記錄失敗', 'error')
  }
}

const handleSubmit = async () => {
  try {
    const expenseData = {
      ...form.value,
      exchangeRate: 1
    }
    
    if (editingId.value) {
      await apiService.updateExpense(editingId.value, expenseData)
      showNotification('記錄已更新', 'success')
    } else {
      await apiService.createExpense(expenseData)
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
    member: record.member,
    type: record.type,
    mainCategory: record.mainCategory,
    subCategory: record.subCategory,
    amount: record.amount,
    currency: record.currency || 'TWD',
    date: record.date,
    description: record.description || ''
  }
  
  document.querySelector('.add-record')?.scrollIntoView({ behavior: 'smooth' })
}

const copyRecord = (id) => {
  const record = records.value.find(r => r.id === id || r.id === Number(id))
  if (!record) {
    showNotification('找不到要複製的記錄', 'error')
    return
  }
  
  editingId.value = null
  form.value = {
    member: record.member,
    type: record.type,
    mainCategory: record.mainCategory,
    subCategory: record.subCategory,
    amount: record.amount,
    currency: record.currency || 'TWD',
    date: new Date().toISOString().split('T')[0],
    description: record.description || ''
  }
  
  showNotification('記錄已複製到表單', 'success')
}

const deleteRecord = async (id) => {
  if (!confirm('確定要刪除這筆記錄嗎？')) {
    return
  }
  
  try {
    const recordId = typeof id === 'string' ? (isNaN(id) ? id : Number(id)) : id
    await apiService.deleteExpense(recordId)
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
    member: '',
    type: '',
    mainCategory: '',
    subCategory: '',
    amount: 0,
    currency: 'TWD',
    date: new Date().toISOString().split('T')[0],
    description: ''
  }
}

const goToDashboard = () => {
  router.push('/')
}

watch(() => filters.value, () => {
  currentPage.value = 1
}, { deep: true })

onMounted(async () => {
  await loadRecords()
})
</script>

<style scoped>
.expenses-page {
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

.summary-item .amount.income {
  color: #4ade80;
}

.summary-item .amount.expense {
  color: #f87171;
}

.summary-item .amount.net-income {
  color: #60a5fa;
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
.form-group select {
  padding: 10px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  font-size: 16px;
}

.form-group input::placeholder {
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

.filter-group select {
  padding: 8px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
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

.income {
  color: #4ade80;
}

.expense {
  color: #f87171;
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

.copy-btn {
  background: rgba(107, 114, 128, 0.2);
  color: #9ca3af;
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

