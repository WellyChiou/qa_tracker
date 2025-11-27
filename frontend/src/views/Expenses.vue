<template>
  <div class="expenses-page">
    <TopNavbar />
    <header class="header">
      <div class="header-top">
        <h1>家庭記帳系統</h1>
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
    
    <!-- 功能按鈕區域 -->
    <div class="function-buttons-section">
      <button class="btn btn-chart" @click="showChartsModal">
        📊 圖表分析
      </button>
      <button class="btn btn-primary" @click="showAssetPortfolio">
        📊 資產組合管理
      </button>
      <button class="btn btn-secondary" @click="showExchangeRates">
        💱 匯率管理
      </button>
      <button class="btn btn-info" @click="showTradingFeesConfig">
        💰 交易費用配置
      </button>
    </div>
  </header>

    <main class="main-content">
      <!-- Modal：新增 / 編輯記錄 -->
      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-panel" @click.stop>
          <div class="modal-header">
            <div>
              <h2 class="modal-title">{{ editingId ? '編輯記錄' : '新增記帳記錄' }}</h2>
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
                <button type="button" class="btn btn-reset" @click="resetForm">
                  清空表單
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- 新增記錄按鈕 -->
      <div class="add-record-button">
        <button class="btn btn-primary" @click="openModal">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          新增記帳記錄
        </button>
      </div>

      <section class="filters">
        <h2>篩選條件</h2>
        <div class="filter-grid">
          <div class="filter-group">
            <label>年份</label>
            <select v-model.number="filters.year">
              <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
            </select>
          </div>
          <div class="filter-group">
            <label>月份</label>
            <select v-model.number="filters.month">
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
              <option v-for="cat in filterMainCategories" :key="cat" :value="cat">{{ cat }}</option>
            </select>
          </div>
        </div>
      </section>

      <section class="records-list">
        <h2>記帳記錄 (共 {{ filteredRecords.length }} 筆)</h2>
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
          <div class="pagination-left">
            <label for="pageSize" class="pagination-label">顯示筆數：</label>
            <select id="pageSize" v-model.number="recordsPerPage" class="page-size-select">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
            </select>
            <span class="pagination-info">共 {{ filteredRecords.length }} 筆 (第 {{ currentPage }}/{{ totalPages }} 頁)</span>
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

    <!-- 圖表分析 Modal -->
    <ChartsModal v-if="showChartsModalFlag" @close="hideChartsModal" :records="records" />
    
    <!-- 匯率管理 Modal -->
    <ExchangeRateModal v-if="showExchangeRateModal" @close="hideExchangeRates" />
    
    <!-- 資產組合管理 Modal -->
    <AssetPortfolioModal v-if="showAssetPortfolioModal" @close="hideAssetPortfolio" />
    
    <!-- 交易費用配置 Modal -->
    <TradingFeesConfigModal v-if="showTradingFeesModal" @close="hideTradingFeesConfig" />

    <div v-if="notification.show" class="notification" :class="notification.type">
      {{ notification.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted, nextTick } from 'vue'
import TopNavbar from '@/components/TopNavbar.vue'
import { apiService } from '@/composables/useApi'
import ChartsModal from './expenses/ChartsModal.vue'
import ExchangeRateModal from './expenses/ExchangeRateModal.vue'
import AssetPortfolioModal from './expenses/AssetPortfolioModal.vue'
import TradingFeesConfigModal from './expenses/TradingFeesConfigModal.vue'

const records = ref([])
const editingId = ref(null)
const currentPage = ref(1)
const recordsPerPage = ref(10)
const jumpPage = ref(1)
const showModal = ref(false)
const showChartsModalFlag = ref(false)
const showExchangeRateModal = ref(false)
const showAssetPortfolioModal = ref(false)
const showTradingFeesModal = ref(false)

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

// 初始化為當前年份和月份
const currentDate = new Date()
const filters = ref({
  year: currentDate.getFullYear(),
  month: currentDate.getMonth() + 1,
  member: '',
  type: '',
  mainCategory: ''
})

const notification = ref({
  show: false,
  message: '',
  type: 'success'
})

// 根據類型過濾類別
const mainCategories = computed(() => {
  if (!form.value.type) return []
  
  if (form.value.type === '支出') {
    return ['食', '衣', '住', '行', '育', '樂', '醫療', '其他支出']
  } else if (form.value.type === '收入') {
    return ['薪資', '投資']
  }
  return []
})

// 細項映射（根據類別）
const subCategoryMap = {
  '食': ['外食', '食材', '飲料', '零食', '其他'],
  '衣': ['服飾', '鞋子', '配件', '美容', '其他'],
  '住': ['房貸', '租金', '水電瓦斯', '居家用品', '家具家電', '裝潢修繕', '網路費', '通訊', '其他'],
  '行': ['交通費', '油費', '停車費', '大眾運輸', '交通工具保養', '其他'],
  '育': ['學費', '書籍', '進修', '文具', '其他'],
  '樂': ['娛樂', '旅遊', '運動', '社交', '其他'],
  '醫療': ['診療', '藥品', '健檢', '醫療用品', '其他'],
  '其他支出': ['投資', '教會奉獻', '保險', '稅務', '其他'],
  '薪資': ['本薪', '獎金', '兼職', '其他'],
  '投資': ['股票', '基金', '債券', '加密貨幣', '其他']
}

// 根據類別過濾細項
const subCategories = computed(() => {
  if (!form.value.mainCategory) return []
  return subCategoryMap[form.value.mainCategory] || []
})

// 篩選條件中的類別（根據篩選的類型）
const filterMainCategories = computed(() => {
  if (!filters.value.type) {
    // 如果沒有選擇類型，返回所有類別
    return ['食', '衣', '住', '行', '育', '樂', '醫療', '其他支出', '薪資', '投資']
  }
  
  if (filters.value.type === '支出') {
    return ['食', '衣', '住', '行', '育', '樂', '醫療', '其他支出']
  } else if (filters.value.type === '收入') {
    return ['薪資', '投資']
  }
  return []
})

// 監聽類型變化，清除類別和細項
watch(() => form.value.type, (newType, oldType) => {
  if (newType !== oldType) {
    form.value.mainCategory = ''
    form.value.subCategory = ''
  }
})

// 監聽類別變化，清除細項
watch(() => form.value.mainCategory, (newCategory, oldCategory) => {
  if (newCategory !== oldCategory) {
    form.value.subCategory = ''
  }
})

const years = computed(() => {
  const currentYear = new Date().getFullYear()
  return Array.from({ length: 10 }, (_, i) => currentYear - i)
})

const filteredRecords = computed(() => {
  let filtered = [...records.value]
  
  // 年份和月份現在總是有值，所以直接篩選
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
  
  // 排序：先按日期降序，再按建立時間降序（如果日期相同）
  return filtered.sort((a, b) => {
    const dateDiff = new Date(b.date) - new Date(a.date)
    if (dateDiff !== 0) return dateDiff
    // 如果日期相同，按建立時間降序
    const createdAtA = a.createdAt ? new Date(a.createdAt) : new Date(0)
    const createdAtB = b.createdAt ? new Date(b.createdAt) : new Date(0)
    return createdAtB - createdAtA
  })
})

const paginatedRecords = computed(() => {
  const start = (currentPage.value - 1) * recordsPerPage.value
  return filteredRecords.value.slice(start, start + recordsPerPage.value)
})

const totalPages = computed(() => {
  return Math.ceil(filteredRecords.value.length / recordsPerPage.value)
})

const jumpToPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    currentPage.value = jumpPage.value
  } else {
    jumpPage.value = currentPage.value
  }
}

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
      description: expense.description,
      createdAt: expense.createdAt || expense.created_at || null
    }))
  } catch (error) {
    console.error('載入記錄失敗:', error)
    showNotification('載入記錄失敗', 'error')
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

// ESC 鍵關閉 Modal
const handleEscape = (e) => {
  if (e.key === 'Escape' && showModal.value) {
    closeModal()
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
    
    closeModal()
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
  
  showModal.value = true
}

const copyRecord = (id) => {
  const record = records.value.find(r => r.id === id || r.id === Number(id))
  if (!record) {
    showNotification('找不到要複製的記錄', 'error')
    return
  }
  
  editingId.value = null
  
  // 先設置類型，確保類別選項列表正確
  form.value.type = record.type
  
  // 使用 nextTick 確保類別選項列表已更新後再設置類別
  nextTick(() => {
    form.value.member = record.member
    form.value.mainCategory = record.mainCategory || ''
    // 再次使用 nextTick 確保細項選項列表已更新
    nextTick(() => {
      form.value.subCategory = record.subCategory || ''
      form.value.amount = record.amount
      form.value.currency = record.currency || 'TWD'
      form.value.date = new Date().toISOString().split('T')[0]
      form.value.description = record.description || ''
      
      showModal.value = true
      showNotification('記錄已複製到表單', 'success')
    })
  })
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

watch(() => filters.value, () => {
  currentPage.value = 1
  jumpPage.value = 1
}, { deep: true })

watch(() => currentPage.value, (newVal) => {
  jumpPage.value = newVal
})

watch(() => recordsPerPage.value, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

onMounted(async () => {
  await loadRecords()
  document.addEventListener('keydown', handleEscape)
  // 注意：自動補足匯率功能已移至後端，每天早上 7:00 自動執行
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscape)
})

// 功能按鈕處理函數
const showChartsModal = () => {
  showChartsModalFlag.value = true
}

const hideChartsModal = () => {
  showChartsModalFlag.value = false
}

const showExchangeRates = () => {
  showExchangeRateModal.value = true
}

const hideExchangeRates = () => {
  showExchangeRateModal.value = false
}

const showAssetPortfolio = () => {
  showAssetPortfolioModal.value = true
}

const hideAssetPortfolio = () => {
  showAssetPortfolioModal.value = false
}

const showTradingFeesConfig = () => {
  showTradingFeesModal.value = true
}

const hideTradingFeesConfig = () => {
  showTradingFeesModal.value = false
}
</script>

<style scoped>
.expenses-page {
  min-height: 100vh;
  background: #f5f5f5;
  color: #333;
  padding: 20px;
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

.summary {
  display: flex;
  justify-content: center;
  gap: 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  padding: 25px 20px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  margin-bottom: 25px;
}

.summary-item {
  text-align: center;
  flex: 1;
  min-width: 0;
  display: block;
  background: rgba(255, 255, 255, 0.15);
  padding: 20px 15px;
  border-radius: 15px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  position: relative;
  margin: 0 10px;
}

.summary-item:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.summary-item .label {
  display: block;
  font-size: 1.1em;
  margin-bottom: 8px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  text-shadow: none;
  letter-spacing: 0.5px;
}

.summary-item .amount {
  display: block;
  font-size: 2.2em;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 1px;
}

.summary-item .amount.income {
  color: #2ecc71;
  background: linear-gradient(135deg, #2ecc71, #27ae60);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.summary-item .amount.expense {
  color: #e74c3c;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.summary-item .amount.net-income {
  color: #3498db;
  background: linear-gradient(135deg, #3498db, #2980b9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr;
  gap: 30px;
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

.modal-body {
  padding: 1.5rem;
}

.add-record-button {
  margin-bottom: 1rem;
  display: flex;
  justify-content: flex-end;
}

.add-record-button .btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.filters,
.records-list {
  background: white;
  padding: 30px;
  border-radius: 15px;
  box-shadow: 0 5px 15px rgba(0,0,0,0.08);
  width: 100%;
  max-width: 100%;
}

.filters h2,
.records-list h2 {
  color: #667eea;
  margin-bottom: 20px;
  font-size: 1.5em;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 10px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #555;
  font-size: 1em;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1em;
  transition: all 0.3s;
  box-sizing: border-box;
  background: #fafafa;
  color: #333;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  transform: translateY(-1px);
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: #999;
}

.form-group select option {
  background: white;
  color: #333;
}

.form-actions {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 14px 28px;
  border: none;
  border-radius: var(--border-radius);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.btn:hover::before {
  width: 300px;
  height: 300px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #6c757d;
  color: white;
  border: none;
}

.btn-secondary:hover {
  background: #5a6268;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(108, 117, 125, 0.4);
}

.btn-reset {
  background: #f8f9fa;
  color: #495057;
  border: 1px solid #dee2e6;
}

.btn-reset:hover {
  background: #e9ecef;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
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
  font-weight: bold;
  color: #555;
  font-size: 0.95em;
}

.filter-group select {
  padding: 10px 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background: #fafafa;
  color: #333;
  font-size: 0.95em;
  transition: all 0.3s;
}

.filter-group select:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.filter-group select option {
  background: white;
  color: #333;
}

.records-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.records-table th,
.records-table td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.records-table th {
  background: #f8f9fa;
  font-weight: 600;
  font-size: 0.9rem;
  color: #495057;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  position: sticky;
  top: 0;
  z-index: 10;
}

.records-table tbody tr {
  transition: background-color 0.2s;
}

.records-table tbody tr:hover {
  background: #f8f9fa;
}

.records-table tbody tr:last-child td {
  border-bottom: none;
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
  padding: 6px 12px;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
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

.copy-btn {
  color: #6c757d;
  border-color: #6c757d;
}

.copy-btn:hover {
  background: #6c757d;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(108, 117, 125, 0.3);
}

.pagination {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid #e2e8f0;
}

@media (min-width: 768px) {
  .pagination {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

.pagination-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.pagination-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.pagination-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #475569;
}

.pagination-info {
  font-size: 0.875rem;
  color: #475569;
  margin-left: 0.5rem;
}

.page-size-select {
  width: 6rem;
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

.page-size-select:focus {
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
  border-color: #818cf8;
}

.page-jump {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background: #f8fafc;
  border-radius: 0.5rem;
  border: 1px solid #e2e8f0;
}

.page-input {
  width: 5rem;
  padding: 0.5rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 0.5rem;
  text-align: center;
  font-weight: 600;
  font-size: 0.875rem;
  outline: none;
  transition: all 0.2s ease;
}

.page-input:focus {
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
  border-color: #818cf8;
}

.btn-secondary {
  padding: 0.625rem 1.25rem;
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

.btn-secondary:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #94a3b8;
  transform: translateY(-1px);
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.w-5 {
  width: 1.25rem;
}

.h-5 {
  height: 1.25rem;
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

.function-buttons-section {
  display: flex;
  gap: 1.25rem;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 1.5rem;
  padding: 1.75rem;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0.05) 100%);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.function-buttons-section .btn {
  min-width: 180px;
  padding: 1rem 1.5rem;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.function-buttons-section .btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s;
}

.function-buttons-section .btn:hover::before {
  left: 100%;
}

.function-buttons-section .btn:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
}

.function-buttons-section .btn:active {
  transform: translateY(-2px) scale(0.98);
}

.function-buttons-section .btn-chart {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  border-color: rgba(255, 255, 255, 0.2);
}

.function-buttons-section .btn-chart:hover {
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.5);
  border-color: rgba(255, 255, 255, 0.4);
}

.function-buttons-section .btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: rgba(255, 255, 255, 0.2);
}

.function-buttons-section .btn-primary:hover {
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.5);
  border-color: rgba(255, 255, 255, 0.4);
}

.function-buttons-section .btn-secondary {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border-color: rgba(255, 255, 255, 0.2);
}

.function-buttons-section .btn-secondary:hover {
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.5);
  border-color: rgba(255, 255, 255, 0.4);
}

.function-buttons-section .btn-info {
  background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
  color: white;
  border-color: rgba(255, 255, 255, 0.2);
}

.function-buttons-section .btn-info:hover {
  box-shadow: 0 8px 24px rgba(6, 182, 212, 0.5);
  border-color: rgba(255, 255, 255, 0.4);
}
</style>

