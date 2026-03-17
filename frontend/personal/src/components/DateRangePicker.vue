<template>
  <div class="date-range-picker">
    <div class="date-range-input" @click="togglePicker">
      <span v-if="displayText" class="date-text">{{ displayText }}</span>
      <span v-else class="placeholder">{{ placeholder }}</span>
      <span class="calendar-icon">📅</span>
    </div>
    <div v-if="showPicker" class="picker-overlay" @click="closePicker">
      <div class="picker-container" @click.stop>
        <div class="picker-header">
          <button v-if="viewMode === 'date'" @click="prevMonth" class="nav-btn">‹</button>
          <button v-else @click="backToPreviousView" class="nav-btn">‹</button>
          <span class="month-year" @click="handleMonthYearClick">{{ headerText }}</span>
          <button v-if="viewMode === 'date'" @click="nextMonth" class="nav-btn">›</button>
          <span v-else class="nav-btn-placeholder"></span>
        </div>
        
        <!-- 年份選擇器 -->
        <div v-if="viewMode === 'year'" class="year-picker">
          <div class="year-grid">
            <button
              v-for="year in years"
              :key="year"
              @click="selectYear(year)"
              :class="['year-btn', { 'active': year === currentYear }]"
            >
              {{ year }}
            </button>
          </div>
        </div>
        
        <!-- 月份選擇器 -->
        <div v-if="viewMode === 'month'" class="month-picker">
          <div class="month-grid">
            <button
              v-for="(month, index) in months"
              :key="index"
              @click="selectMonth(index)"
              :class="['month-btn', { 'active': index === currentMonth && currentYear === tempYear }]"
            >
              {{ month }}
            </button>
          </div>
        </div>
        
        <!-- 日期選擇器 -->
        <div v-if="viewMode === 'date'" class="picker-body">
          <div class="calendar-wrapper">
            <div class="calendar-grid">
              <div class="day-names">
                <span v-for="dayName in dayNames" :key="dayName" class="day-name">{{ dayName }}</span>
              </div>
              <div class="calendar-days">
                <span
                  v-for="day in daysInMonth"
                  :key="day.date"
                  :class="[
                    'calendar-day',
                    { 'is-empty': !day.date },
                    { 'is-today': day.isToday },
                    { 'is-selected': isSelected(day.date) },
                    { 'is-start': isStartDate(day.date) },
                    { 'is-end': isEndDate(day.date) },
                    { 'is-in-range': isInRange(day.date) }
                  ]"
                  @click="selectDate(day.date)"
                >
                  {{ day.day }}
                </span>
              </div>
            </div>
          </div>
          <div class="picker-actions">
            <button @click="clearRange" class="btn-clear">清除</button>
            <button @click="confirmSelection" class="btn-close">確定</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array, // [startDate, endDate] 格式為 'YYYY-MM-DD'
    default: () => []
  },
  placeholder: {
    type: String,
    default: '選擇日期範圍'
  }
})

const emit = defineEmits(['update:modelValue'])

const showPicker = ref(false)
const viewMode = ref('date') // 'year', 'month', 'date'
const currentMonth = ref(new Date().getMonth())
const currentYear = ref(new Date().getFullYear())
const tempYear = ref(new Date().getFullYear()) // 用於月份選擇時保存年份
const selectedDates = ref([...props.modelValue]) // [start, end]

const dayNames = ['日', '一', '二', '三', '四', '五', '六']
const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

const years = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let i = currentYear - 10; i <= currentYear + 10; i++) {
    years.push(i)
  }
  return years
})

const daysInMonth = computed(() => {
  const date = new Date(currentYear.value, currentMonth.value, 1)
  const firstDayIndex = date.getDay()
  const days = []
  const daysCount = new Date(currentYear.value, currentMonth.value + 1, 0).getDate()

  // 添加前一個月的末尾日期（用於填充）
  const prevMonthDays = new Date(currentYear.value, currentMonth.value, 0).getDate()
  for (let i = firstDayIndex - 1; i >= 0; i--) {
    days.push({ day: '', date: null, isToday: false })
  }

  // 添加當月的日期
  const today = new Date()
  for (let day = 1; day <= daysCount; day++) {
    const dayDate = new Date(currentYear.value, currentMonth.value, day)
    // 使用本地時間格式化日期，避免時區問題
    const year = dayDate.getFullYear()
    const month = String(dayDate.getMonth() + 1).padStart(2, '0')
    const date = String(dayDate.getDate()).padStart(2, '0')
    const dateStr = `${year}-${month}-${date}`
    days.push({
      day,
      date: dateStr,
      isToday: dayDate.toDateString() === today.toDateString()
    })
  }

  return days
})

const currentMonthYear = computed(() => {
  return `${currentYear.value}年${currentMonth.value + 1}月`
})

const headerText = computed(() => {
  if (viewMode.value === 'year') {
    return '選擇年份'
  } else if (viewMode.value === 'month') {
    return `${tempYear.value}年`
  } else {
    return currentMonthYear.value
  }
})

const displayText = computed(() => {
  // 顯示已確認的值（props.modelValue），而不是臨時選擇的值（selectedDates.value）
  if (props.modelValue && props.modelValue.length === 2 && props.modelValue[0] && props.modelValue[1]) {
    const start = formatDate(props.modelValue[0])
    const end = formatDate(props.modelValue[1])
    return `${start} ~ ${end}`
  } else if (props.modelValue && props.modelValue.length === 1 && props.modelValue[0]) {
    return `${formatDate(props.modelValue[0])} ~ `
  }
  return ''
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  // 直接解析 YYYY-MM-DD 格式，避免時區問題
  const [year, month, day] = dateStr.split('-')
  return `${year}/${month}/${day}`
}

const isSelected = (date) => {
  if (!date) return false
  return selectedDates.value.includes(date)
}

const isStartDate = (date) => {
  if (!date || selectedDates.value.length === 0) return false
  return selectedDates.value[0] === date
}

const isEndDate = (date) => {
  if (!date || selectedDates.value.length < 2) return false
  return selectedDates.value[1] === date
}

const isInRange = (date) => {
  if (!date || selectedDates.value.length !== 2) return false
  const [start, end] = selectedDates.value.sort()
  return date > start && date < end
}

const selectDate = (date) => {
  if (!date) return

  if (selectedDates.value.length === 0 || selectedDates.value.length === 2) {
    // 如果沒有選擇或已選擇兩個日期，重新開始選擇
    selectedDates.value = [date]
  } else if (selectedDates.value.length === 1) {
    // 如果已選擇一個日期，選擇第二個日期
    const existingDate = selectedDates.value[0]
    if (date === existingDate) {
      // 如果點擊同一個日期，清除選擇
      selectedDates.value = []
    } else {
      // 排序日期，確保 start < end
      selectedDates.value = [existingDate, date].sort()
      // 選擇完成後不自動關閉，讓用戶可以調整
    }
  }
  
  // 不立即 emit，等用戶點擊"確定"按鈕
}

const clearRange = () => {
  selectedDates.value = []
  emit('update:modelValue', [])
  closePicker()
}

const confirmSelection = () => {
  // 點擊"確定"按鈕時才 emit 更新
  emit('update:modelValue', [...selectedDates.value])
  closePicker()
}

const togglePicker = () => {
  showPicker.value = !showPicker.value
  if (showPicker.value) {
    // 打開時，重置為當前的 modelValue（恢復原值）
    selectedDates.value = [...props.modelValue]
    viewMode.value = 'date' // 重置為日期視圖
    // 如果有已選擇的日期，顯示對應的月份
    if (props.modelValue.length > 0 && props.modelValue[0]) {
      const date = new Date(props.modelValue[0])
      currentMonth.value = date.getMonth()
      currentYear.value = date.getFullYear()
      tempYear.value = date.getFullYear()
    }
  } else {
    // 關閉時，如果沒有點擊"確定"，恢復原值
    selectedDates.value = [...props.modelValue]
    viewMode.value = 'date' // 重置為日期視圖
  }
}

const closePicker = () => {
  showPicker.value = false
}

const yearChanged = () => {
  // 年份改變時保持當前月份
}

const monthChanged = () => {
  // 月份改變時保持當前年份
}

const prevMonth = () => {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

const nextMonth = () => {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

// 處理點擊月份/年份標題
const handleMonthYearClick = () => {
  if (viewMode.value === 'date') {
    // 從日期視圖切換到年份視圖
    tempYear.value = currentYear.value
    viewMode.value = 'year'
  }
}

// 返回上一層視圖
const backToPreviousView = () => {
  if (viewMode.value === 'month') {
    viewMode.value = 'year'
  } else if (viewMode.value === 'year') {
    viewMode.value = 'date'
  }
}

// 選擇年份
const selectYear = (year) => {
  tempYear.value = year
  currentYear.value = year
  viewMode.value = 'month' // 切換到月份選擇
}

// 選擇月份
const selectMonth = (month) => {
  currentMonth.value = month
  currentYear.value = tempYear.value
  viewMode.value = 'date' // 切換到日期選擇
}

// 監聽外部值變化
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    selectedDates.value = [...newVal]
  } else {
    selectedDates.value = []
  }
}, { deep: true })
</script>

<style scoped>
.date-range-picker {
  position: relative;
  width: 100%;
}

.date-range-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 0.75rem;
  background: white;
  color: #1e293b;
  cursor: pointer;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.date-range-input:hover {
  border-color: #94a3b8;
}

.date-range-input:focus-within {
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}

.date-text {
  flex: 1;
  font-size: 0.875rem;
  font-weight: 500;
  color: #1e293b;
}

.placeholder {
  flex: 1;
  color: #94a3b8;
  font-size: 0.875rem;
}

.calendar-icon {
  font-size: 14px;
  margin-left: 8px;
  color: #64748b;
  line-height: 1;
}

.date-range-hint {
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 500;
}

.picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.picker-container {
  background: var(--bg-card);
  border-radius: var(--border-radius-xl);
  box-shadow: var(--shadow-xl);
  padding: var(--spacing-xl);
  min-width: 350px;
  max-width: 400px;
  animation: slideUp 0.3s;
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

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-color);
}

.month-year {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.month-year:hover {
  background-color: rgba(102, 126, 234, 0.1);
}

.year-picker,
.month-picker {
  margin-top: 8px;
  padding: 12px;
  background: var(--bg-card);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
}

.year-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}

.year-btn {
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-primary);
  border-radius: var(--border-radius-sm);
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s;
}

.year-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: var(--primary-color);
}

.year-btn.active {
  background: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.month-btn {
  padding: 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-primary);
  border-radius: var(--border-radius-sm);
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 500;
  transition: all 0.2s;
}

.month-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: var(--primary-color);
}

.month-btn.active {
  background: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.nav-btn {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  padding: 6px 12px;
  cursor: pointer;
  font-size: 1.2rem;
  color: var(--text-primary);
  transition: var(--transition);
  min-width: 36px;
}

.nav-btn-placeholder {
  min-width: 36px;
}

.nav-btn:hover {
  background: var(--bg-primary);
  border-color: var(--primary-color);
}

.picker-body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.calendar-wrapper {
  width: 100%;
}

.calendar-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.day-names {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 8px;
}

.day-name {
  text-align: center;
  font-weight: 700;
  color: var(--primary-color);
  font-size: 0.9rem;
  padding: 8px 0;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: var(--border-radius-sm);
  transition: var(--transition);
  font-size: 0.95rem;
  font-weight: 500;
  color: var(--text-primary);
  position: relative;
}

.calendar-day.is-empty {
  visibility: hidden;
  cursor: default;
}

.calendar-day:not(.is-empty):hover {
  background: rgba(102, 126, 234, 0.2);
  transform: scale(1.1);
}

.calendar-day.is-today {
  border: 2px solid var(--primary-color);
  font-weight: 700;
}

.calendar-day.is-selected {
  background: rgba(102, 126, 234, 0.3);
  color: white;
  font-weight: 700;
}

.calendar-day.is-start {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 700;
  border-radius: var(--border-radius-sm) 0 0 var(--border-radius-sm);
}

.calendar-day.is-end {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 700;
  border-radius: 0 var(--border-radius-sm) var(--border-radius-sm) 0;
}

.calendar-day.is-in-range {
  background: rgba(102, 126, 234, 0.15);
  color: var(--text-primary);
  border-radius: 0;
}

.calendar-day.is-start.is-end {
  border-radius: var(--border-radius-sm);
}

.picker-actions {
  display: flex;
  gap: var(--spacing-md);
  justify-content: flex-end;
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-color);
}

.btn-clear,
.btn-close {
  padding: 10px 20px;
  border: none;
  border-radius: var(--border-radius);
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  font-size: 14px;
}

.btn-clear {
  background: var(--bg-primary);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.btn-clear:hover {
  background: var(--bg-secondary);
}

.btn-close {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-close:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
</style>
