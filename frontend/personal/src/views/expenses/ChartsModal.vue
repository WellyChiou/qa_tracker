<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">📊 圖表分析</h2>
        <button class="btn-close" @click="$emit('close')" title="關閉">
          <svg class="close-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <!-- 圖表篩選器 -->
        <div class="chart-filters">
          <h3>圖表篩選</h3>
          <div class="filter-group">
            <label for="chartFilterType">類型：</label>
            <select id="chartFilterType" v-model="filters.type">
              <option value="">所有類型</option>
              <option value="收入">收入</option>
              <option value="支出">支出</option>
            </select>
            
            <label for="chartFilterYear">年份：</label>
            <select id="chartFilterYear" v-model.number="filters.year" required>
              <option v-for="year in availableYears" :key="year" :value="year">{{ year }}</option>
            </select>
            
            <label for="chartFilterMonth">月份：</label>
            <select id="chartFilterMonth" v-model.number="filters.month">
              <option :value="0">整年</option>
              <option v-for="m in 12" :key="m" :value="m">{{ m }}月</option>
            </select>

            <label for="chartFilterMember">家庭成員：</label>
            <select id="chartFilterMember" v-model="filters.member">
              <option value="">全部成員</option>
              <option value="爸爸">爸爸</option>
              <option value="媽媽">媽媽</option>
              <option value="孩子">孩子</option>
              <option value="其他">其他</option>
            </select>
          </div>
        </div>
        
        <h2 class="expense-section-title">支出統計</h2>
        <div class="chart-row expense-section">
          <div class="chart-container">
            <h3>支出類別分布</h3>
            <canvas ref="expenseCategoryChart"></canvas>
          </div>
          <div class="chart-container">
            <h3>家庭成員支出分布</h3>
            <canvas ref="expenseMemberChart"></canvas>
          </div>
        </div>
        
        <h2 class="income-section-title">收入統計</h2>
        <div class="chart-row income-section">
          <div class="chart-container">
            <h3>收入類別分布</h3>
            <canvas ref="incomeCategoryChart"></canvas>
          </div>
          <div class="chart-container">
            <h3>家庭成員收入分布</h3>
            <canvas ref="incomeMemberChart"></canvas>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

const props = defineProps({
  records: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['close'])

const filters = ref({
  type: '',
  year: new Date().getFullYear(),
  month: 0,
  member: ''
})

const expenseCategoryChart = ref(null)
const expenseMemberChart = ref(null)
const incomeCategoryChart = ref(null)
const incomeMemberChart = ref(null)

let chartInstances = {
  expenseCategory: null,
  expenseMember: null,
  incomeCategory: null,
  incomeMember: null
}

const availableYears = computed(() => {
  const years = [...new Set(props.records.map(r => new Date(r.date).getFullYear()))]
  return years.sort((a, b) => b - a)
})

const filteredRecords = computed(() => {
  let filtered = [...props.records]
  
  if (filters.value.year) {
    filtered = filtered.filter(r => new Date(r.date).getFullYear() === filters.value.year)
  }
  
  if (filters.value.month > 0) {
    filtered = filtered.filter(r => new Date(r.date).getMonth() + 1 === filters.value.month)
  }
  
  if (filters.value.member) {
    filtered = filtered.filter(r => r.member === filters.value.member)
  }
  
  return filtered
})

const expenseRecords = computed(() => {
  return filteredRecords.value.filter(r => r.type === '支出')
})

const incomeRecords = computed(() => {
  return filteredRecords.value.filter(r => r.type === '收入')
})

const updateCharts = async () => {
  await nextTick()
  
  // 更新支出類別圖表
  updateExpenseCategoryChart()
  
  // 更新支出成員圖表
  updateExpenseMemberChart()
  
  // 更新收入類別圖表
  updateIncomeCategoryChart()
  
  // 更新收入成員圖表
  updateIncomeMemberChart()
}

const updateExpenseCategoryChart = () => {
  const data = expenseRecords.value.reduce((acc, record) => {
    const category = record.mainCategory || '其他'
    acc[category] = (acc[category] || 0) + parseFloat(record.amount || 0)
    return acc
  }, {})
  
  const labels = Object.keys(data)
  const values = Object.values(data)
  
  if (chartInstances.expenseCategory) {
    chartInstances.expenseCategory.destroy()
  }
  
  if (expenseCategoryChart.value) {
    chartInstances.expenseCategory = new Chart(expenseCategoryChart.value, {
      type: 'pie',
      data: {
        labels,
        datasets: [{
          data: values,
          backgroundColor: [
            '#ef4444', '#f59e0b', '#10b981', '#3b82f6',
            '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'
          ]
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    })
  }
}

const updateExpenseMemberChart = () => {
  const data = expenseRecords.value.reduce((acc, record) => {
    const member = record.member || '其他'
    acc[member] = (acc[member] || 0) + parseFloat(record.amount || 0)
    return acc
  }, {})
  
  const labels = Object.keys(data)
  const values = Object.values(data)
  
  if (chartInstances.expenseMember) {
    chartInstances.expenseMember.destroy()
  }
  
  if (expenseMemberChart.value) {
    chartInstances.expenseMember = new Chart(expenseMemberChart.value, {
      type: 'pie',
      data: {
        labels,
        datasets: [{
          data: values,
          backgroundColor: [
            '#3b82f6', '#10b981', '#f59e0b', '#ef4444'
          ]
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    })
  }
}

const updateIncomeCategoryChart = () => {
  const data = incomeRecords.value.reduce((acc, record) => {
    const category = record.mainCategory || '其他'
    acc[category] = (acc[category] || 0) + parseFloat(record.amount || 0)
    return acc
  }, {})
  
  const labels = Object.keys(data)
  const values = Object.values(data)
  
  if (chartInstances.incomeCategory) {
    chartInstances.incomeCategory.destroy()
  }
  
  if (incomeCategoryChart.value) {
    chartInstances.incomeCategory = new Chart(incomeCategoryChart.value, {
      type: 'pie',
      data: {
        labels,
        datasets: [{
          data: values,
          backgroundColor: [
            '#10b981', '#3b82f6', '#8b5cf6', '#f59e0b'
          ]
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    })
  }
}

const updateIncomeMemberChart = () => {
  const data = incomeRecords.value.reduce((acc, record) => {
    const member = record.member || '其他'
    acc[member] = (acc[member] || 0) + parseFloat(record.amount || 0)
    return acc
  }, {})
  
  const labels = Object.keys(data)
  const values = Object.values(data)
  
  if (chartInstances.incomeMember) {
    chartInstances.incomeMember.destroy()
  }
  
  if (incomeMemberChart.value) {
    chartInstances.incomeMember = new Chart(incomeMemberChart.value, {
      type: 'pie',
      data: {
        labels,
        datasets: [{
          data: values,
          backgroundColor: [
            '#3b82f6', '#10b981', '#f59e0b', '#ef4444'
          ]
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    })
  }
}

watch(() => filters.value, () => {
  updateCharts()
}, { deep: true })

watch(() => props.records, () => {
  updateCharts()
}, { deep: true })

onMounted(() => {
  if (availableYears.value.length > 0 && !filters.value.year) {
    filters.value.year = availableYears.value[0]
  }
  updateCharts()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 1rem;
  overflow-y: auto;
}

.modal-panel {
  width: 100%;
  max-width: 95vw;
  max-height: 95vh;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  margin: 1rem 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(to right, #f8fafc, white);
  border-radius: 1rem 1rem 0 0;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.chart-filters {
  margin-bottom: 2rem;
  padding: 1rem;
  background: #f8fafc;
  border-radius: 0.75rem;
}

.chart-filters h3 {
  margin-bottom: 1rem;
  color: #1e293b;
}

.filter-group {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: center;
}

.filter-group label {
  font-weight: 600;
  color: #475569;
}

.filter-group select {
  padding: 0.5rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 0.5rem;
}

.expense-section-title,
.income-section-title {
  margin-top: 2rem;
  margin-bottom: 1rem;
  color: #1e293b;
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.chart-container {
  background: #f8fafc;
  padding: 1rem;
  border-radius: 0.75rem;
  border: 1px solid #e2e8f0;
}

.chart-container h3 {
  margin-bottom: 1rem;
  color: #1e293b;
  font-size: 1.125rem;
}

.chart-container canvas {
  max-height: 300px;
}

.btn-close {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #64748b;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.btn-close::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  transform: translate(-50%, -50%);
  transition: width 0.3s ease, height 0.3s ease;
  z-index: 0;
}

.btn-close:hover::before {
  width: 100%;
  height: 100%;
}

.btn-close:hover {
  border-color: #ef4444;
  transform: scale(1.1) rotate(90deg);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.btn-close:hover .close-icon {
  color: white;
  transform: scale(1.1);
}

.btn-close:active {
  transform: scale(0.95) rotate(90deg);
}

.close-icon {
  width: 20px;
  height: 20px;
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
}

.w-4 {
  width: 1rem;
}

.h-4 {
  height: 1rem;
}
</style>

