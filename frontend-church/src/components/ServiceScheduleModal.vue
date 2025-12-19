<template>
  <Teleport to="body">
    <div v-if="show" class="modal-overlay" @click="closeModal">
      <div class="modal-panel service-schedule-modal" @click.stop>
        <div class="modal-header">
        <h2 class="modal-title">{{ modalTitle }}</h2>
        <button class="btn-close" @click="closeModal" aria-label="關閉">✕</button>
      </div>
      <div class="modal-body">
        <!-- 年份選擇和崗位選擇（僅在新增模式顯示） -->
        <div v-if="mode === 'add'" class="card">
          <h3>建立年度服事表</h3>
          <div class="year-selection-group">
            <div class="year-input-wrapper">
              <label>選擇年份：</label>
              <select v-model="selectedYear" class="form-input year-select">
                <option value="">請選擇年份</option>
                <option v-for="year in availableYearsForCreate" :key="year" :value="year">{{ year }}年</option>
              </select>
              <span v-if="yearWarning" class="year-warning">⚠️ 該年度已存在服事表</span>
            </div>
          </div>
          
          <!-- 崗位選擇（用於新增服事表） -->
          <div class="position-selection">
            <label class="position-selection-label">
              選擇要包含的崗位（用於新增服事表）：
            </label>
            <div class="position-checkboxes">
              <div 
                v-for="(posData, posCode) in positionConfig" 
                :key="posCode"
                class="position-checkbox-wrapper"
              >
                <label class="position-checkbox-item">
                  <input 
                    type="checkbox" 
                    v-model="localInitialSelectedPositions[posCode]"
                    :disabled="!hasPositionConfig(posCode)"
                  />
                  <span>{{ posData.positionName || posCode }}</span>
                  <span v-if="!hasPositionConfig(posCode)" class="no-config-hint">（尚未配置人員）</span>
                </label>
              </div>
            </div>
          </div>
          
          <div class="generate-button-wrapper">
            <button @click="generateSchedule" class="btn btn-primary" :disabled="!canGenerate" :title="getGenerateButtonTooltip()">
              產生年度服事表
            </button>
            <div v-if="!canGenerate" class="generate-hint">
              <small>{{ getGenerateButtonTooltip() }}</small>
            </div>
          </div>
        </div>

        <!-- 服事表內容 -->
        <div v-if="localSchedule && localSchedule.length > 0" class="card">
          <div class="schedule-header">
            <div class="schedule-title-section">
              <h3>服事表 <span v-if="isEditing" class="editing-badge">編輯模式</span></h3>
              <!-- 年度顯示 -->
              <div v-if="calculatedYear || localScheduleYear" class="schedule-year-section">
                <span class="year-badge">{{ calculatedYear || localScheduleYear }}年</span>
                <span v-if="yearWarning" class="year-warning">⚠️ 該年度已存在服事表</span>
              </div>
              <!-- 編輯模式下的崗位選擇（用於新增崗位） -->
              <div v-if="isEditing" class="edit-position-selection">
                <div class="edit-position-header">
                  <label class="edit-position-selection-label">
                    選擇要包含的崗位（可新增崗位）：
                  </label>
                  <label class="random-assignment-toggle">
                    <input 
                      type="checkbox" 
                      v-model="localUseRandomAssignment"
                      class="toggle-input"
                    />
                    <span class="toggle-slider"></span>
                    <span class="toggle-label">完全隨機分配（不考慮服務次數）</span>
                  </label>
                </div>
                <div class="position-checkboxes">
                  <div 
                    v-for="(posData, posCode) in positionConfig" 
                    :key="posCode"
                    class="position-checkbox-wrapper"
                  >
                    <label class="position-checkbox-item">
                      <input 
                        type="checkbox" 
                        v-model="localSelectedPositions[posCode]"
                        @change="handlePositionSelectionChange(posCode)"
                      />
                      <span>{{ posData.positionName || posCode }}</span>
                      <span v-if="!hasPositionConfig(posCode)" class="no-config-hint">（尚未配置人員）</span>
                    </label>
                  </div>
                </div>
              </div>
            </div>
            <div class="schedule-actions">
              <button v-if="isEditing" @click="cancelEdit" class="btn btn-cancel">取消</button>
              <!-- 編輯模式：如果有 editingScheduleYear，顯示「更新」按鈕；否則顯示「新增」按鈕 -->
              <button v-if="isEditing && editingScheduleYear" @click="updateSchedule" class="btn btn-save" :disabled="saving">更新</button>
              <button v-else-if="isEditing && !editingScheduleYear && mode === 'add'" @click="saveSchedule" class="btn btn-save" :disabled="saving">新增</button>
              <button @click="exportSchedule" class="btn btn-export">匯出服事表</button>
            </div>
          </div>
          <div v-if="mode === 'view'" class="month-cards">
            <div v-for="m in monthCards" :key="m.key" class="month-card">
              <div class="month-card__header">
                <div class="month-card__title">
                  <span class="month-title">{{ m.title }}</span>
                  <span class="month-meta">共 {{ m.weekCount }} 週</span>
                </div>
              </div>
              <div class="month-card__weeks">
                <details v-for="w in m.weeks" :key="w.id" class="week-card">
                  <summary class="week-card__summary">
                    <div class="week-card__left">
                      <div class="week-title">{{ w.title }}</div>
</div>
                    <div class="week-card__right">查看明細 ▸</div>
                  </summary>
                  <div class="week-card__detail">
                    <div v-if="w.sat" class="day-block">
                      <div class="day-block__title">{{ String(w.sat.date).slice(0,10) }}（六）</div>
                      <div class="day-grid">
                        <div v-for="posCode in selectedPositionsList" :key="'sat-' + posCode" class="kv">
                          <div class="k">{{ (props.positionConfig[posCode]?.positionName || posCode) }}</div>
                          <div class="v">{{ w.sat[posCode] || '-' }}</div>
                        </div>
                      </div>
                    </div>
                    <div v-if="w.sun" class="day-block">
                      <div class="day-block__title">{{ String(w.sun.date).slice(0,10) }}（日）</div>
                      <div class="day-grid">
                        <div v-for="posCode in selectedPositionsList" :key="'sun-' + posCode" class="kv">
                          <div class="k">{{ (props.positionConfig[posCode]?.positionName || posCode) }}</div>
                          <div class="v">{{ w.sun[posCode] || '-' }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </details>
              </div>
            </div>
          </div>

          <div v-else class="schedule-table">
            <table>
              <thead>
                <tr>
                  <th class="date-column">日期</th>
                  <th 
                        v-for="posCode in selectedPositionsList" 
                        :key="posCode"
                        class="position-header-cell"
                      >
                        <div class="position-header-content">
                          <span class="position-header-name">{{ positionConfig[posCode]?.positionName || posCode }}</span>
                          <button 
                            v-if="isEditing && hasPositionConfig(posCode)"
                            @click="autoAssignPosition(posCode)"
                            class="btn-auto-assign-header"
                            title="重新隨機分配該崗位的人員"
                          >
                            🔄
                          </button>
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in localSchedule" :key="index">
                  <td class="date-column">
                    <div class="date-cell-content">
                      <span>{{ item.formattedDate || formatDisplayDate(item.date, item.dayOfWeek) || item.date }}</span>
                      <button 
                        v-if="isEditing" 
                        @click="clearDayPersons(index)" 
                        class="btn-clear-day"
                        title="清空當天所有人員"
                      >
                        清空
                      </button>
                    </div>
                  </td>
                  <td 
                        v-for="posCode in selectedPositionsList" 
                        :key="posCode"
                        class="position-cell"
                      >
                    <div v-if="isEditing" class="person-multi-select">
                        <select 
                          v-model="item[posCode + 'Ids']" 
                          @change="handlePersonChange(item, posCode)" 
                          class="edit-select"
                          multiple
                          :size="Math.min(getAvailablePersons(item, posCode).length + 1, 5)"
                        >
                          <option v-for="person in getAvailablePersons(item, posCode)" :key="person.id" :value="person.id">
                            {{ person.name }}
                          </option>
                        </select>
                        <div v-if="item[posCode + 'Ids'] && item[posCode + 'Ids'].length > 0" class="selected-persons">
                          <span class="selected-label">已選：</span>
                          <span class="selected-names">{{ item[posCode] || '-' }}</span>
                        </div>
                      </div>
                      <span v-else>{{ item[posCode] || '-' }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        </div>
        <!-- 通知組件 -->
        <Notification ref="notificationRef" />
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { apiRequest } from '@/utils/api'
import Notification from '@/components/Notification.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  mode: {
    type: String,
    default: 'add', // 'add' | 'edit' | 'view'
    validator: (value) => ['add', 'edit', 'view'].includes(value)
  },
  scheduleYear: {
    type: Number,
    default: null
  },
  positionConfig: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['close', 'saved', 'updated'])

// 本地狀態
const localSchedule = ref([])
const localScheduleYear = ref(null)
const localDateRange = ref([])
const localSelectedPositions = ref({})
const localInitialSelectedPositions = ref({})
const localUseRandomAssignment = ref(false)
const editingScheduleYear = ref(null)
const isEditing = ref(false)
const saving = ref(false)
const originalSchedule = ref([])
const yearWarning = ref(false)
const selectedYear = ref('')
const existingYears = ref([]) // 已存在的年份列表

// 計算屬性
const modalTitle = computed(() => {
  switch (props.mode) {
    case 'add':
      return '新增服事表'
    case 'edit':
      return '修改服事表'
    case 'view':
      return '詳細資料'
    default:
      return '服事表'
  }
})

const selectedPositionsList = computed(() => {
  return Object.keys(props.positionConfig).filter(
    posCode => localSelectedPositions.value[posCode] === true
  )
})


/**
 * View 模式：月份卡片（方案 B）
 * - 不改資料流：只用 localSchedule 做前端分組
 * - 顯示：月份卡片 + 週卡（可展開）+ 缺人提示
 */
const isSat = (item) => {
  const d = String(item?.dayOfWeek ?? '')
  return d.includes('六') || d.toLowerCase().includes('sat')
}
const isSun = (item) => {
  const d = String(item?.dayOfWeek ?? '')
  return d.includes('日') || d.toLowerCase().includes('sun')
}
const parseDate = (dateStr) => {
  if (!dateStr) return null
  if (dateStr instanceof Date) {
    return isNaN(dateStr.getTime()) ? null : dateStr
  }
  // support YYYY-MM-DD
  let m = String(dateStr).match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (m) {
    const y = Number(m[1])
    const mm = Number(m[2]) - 1
    const dd = Number(m[3])
    const d = new Date(y, mm, dd)
    return isNaN(d.getTime()) ? null : d
  }
  // support YYYY/MM/DD
  m = String(dateStr).match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})/)
  if (m) {
    const y = Number(m[1])
    const mm = Number(m[2]) - 1
    const dd = Number(m[3])
    const d = new Date(y, mm, dd)
    return isNaN(d.getTime()) ? null : d
  }
  const d = new Date(String(dateStr))
  return isNaN(d.getTime()) ? null : d
}

const monthKeyFromDate = (ymd) => {
  const m = String(ymd).match(/^(\d{4})-(\d{2})-/)
  return m ? `${m[1]}-${m[2]}` : ''
}
const monthTitleOf = (key) => {
  const m = String(key).match(/^(\d{4})-(\d{2})$/)
  if (!m) return key
  return `${m[1]} 年 ${Number(m[2])} 月`
}
const startOfWeekMon = (dt) => {
  const day = dt.getDay()
  const diff = (day === 0 ? -6 : 1 - day)
  const res = new Date(dt)
  res.setDate(dt.getDate() + diff)
  res.setHours(0,0,0,0)
  return res
}
const weekKeyOfDate = (dt) => {
  const mon = startOfWeekMon(dt)
  const y = mon.getFullYear()
  const m = String(mon.getMonth() + 1).padStart(2, '0')
  const d = String(mon.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}
const safeNamesFrom = (val) => {
  if (!val) return []
  if (Array.isArray(val)) return val.filter(Boolean).map(String)
  if (typeof val === 'string') {
    return val
      .split(/[,、/\s]+/g)
      .map(s => s.trim())
      .filter(Boolean)
  }
  if (typeof val === 'object') {
    const name = val.name || val.person || val.label
    return name ? [String(name)] : []
  }
  return [String(val)]
}
const assignedCountForItem = (item) => {
  const posCodes = selectedPositionsList.value || []
  let assigned = 0
  for (const pos of posCodes) {
    const names = safeNamesFrom(item?.[pos])
    if (names.length > 0 && names[0] !== '-') assigned += 1
  }
  return assigned
}
const totalPositions = computed(() => (selectedPositionsList.value || []).length)
const missingPositionsForItem = (item) => {
  const posCodes = selectedPositionsList.value || []
  const missing = []
  for (const pos of posCodes) {
    const names = safeNamesFrom(item?.[pos])
    if (!names.length || names[0] === '-') {
      missing.push(props.positionConfig?.[pos]?.positionName || pos)
    }
  }
  return missing
}

const monthCards = computed(() => {
  const rows = Array.isArray(localSchedule.value) ? [...localSchedule.value] : []
  rows.sort((a, b) => String(a?.date || '').localeCompare(String(b?.date || '')))

  // week groups
  const weeks = new Map()
  for (const item of rows) {
    const ymd = String(item?.date || '').slice(0, 10)
    const dt = parseDate(ymd)
    if (!dt) continue
    const wk = weekKeyOfDate(dt)
    if (!weeks.has(wk)) weeks.set(wk, [])
    weeks.get(wk).push(item)
  }

  const weekList = []
  for (const [wk, items] of weeks.entries()) {
    const sorted = [...items].sort((a, b) => String(a?.date || '').localeCompare(String(b?.date || '')))
    const sat = sorted.find(isSat) || null
    const sun = sorted.find(isSun) || null
    const anchor = sat || sun || sorted[0]
    const anchorYMD = String(anchor?.date || '').slice(0,10)
    const mk = monthKeyFromDate(anchorYMD)
    weekList.push({ weekKey: wk, monthKey: mk || 'unknown', sat, sun })
  }

  const months = new Map()
  for (const w of weekList) {
    if (!months.has(w.monthKey)) months.set(w.monthKey, [])
    months.get(w.monthKey).push(w)
  }

  const keys = [...months.keys()].filter(k => k !== 'unknown').sort()
  if (months.has('unknown')) keys.push('unknown')

  return keys.map((mk) => {
    const ws = (months.get(mk) || []).sort((a,b) => a.weekKey.localeCompare(b.weekKey))
    let monthAssigned = 0
    let monthTotal = 0
    for (const w of ws) {
      for (const d of [w.sat, w.sun]) {
        if (!d) continue
        monthAssigned += assignedCountForItem(d)
        monthTotal += totalPositions.value
      }
    }
    const satAssigned = ws.reduce((acc, w) => acc + (w.sat ? assignedCountForItem(w.sat) : 0), 0)
    const sunAssigned = ws.reduce((acc, w) => acc + (w.sun ? assignedCountForItem(w.sun) : 0), 0)
    const satTotal = ws.reduce((acc, w) => acc + (w.sat ? totalPositions.value : 0), 0)
    const sunTotal = ws.reduce((acc, w) => acc + (w.sun ? totalPositions.value : 0), 0)

    return {
      key: mk,
      title: monthTitleOf(mk),
      weekCount: ws.length,
      assigned: monthAssigned,
      total: monthTotal,
      satAssigned,
      sunAssigned,
      satTotal,
      sunTotal,
      weeks: ws.map((w, idx) => {
        const missing = []
        if (w.sat) missing.push(...missingPositionsForItem(w.sat))
        if (w.sun) missing.push(...missingPositionsForItem(w.sun))
        const missingUniq = [...new Set(missing)]
        const titleParts = []
        if (w.sat) titleParts.push(`${String(w.sat.date).slice(0,10)}（六）`)
        if (w.sun) titleParts.push(`${String(w.sun.date).slice(0,10)}（日）`)
        return {
          id: `${mk}-${w.weekKey}-${idx}`,
          title: titleParts.join(' ・ '),
          sat: w.sat,
          sun: w.sun,
          missing: missingUniq.slice(0, 3),
          missingCount: missingUniq.length,
        }
      })
    }
  })
})

// 計算年度（從日期範圍的開始日期）
const calculatedYear = computed(() => {
  if (!localDateRange.value || localDateRange.value.length !== 2) {
    return null
  }
  const startDate = new Date(localDateRange.value[0])
  return startDate.getFullYear()
})

// 檢查該年度是否已有服事表
const checkYearExists = async (year) => {
  try {
    const response = await apiRequest(`/church/service-schedules/year/${year}`, {
      method: 'GET'
    })
    return response.ok
  } catch (error) {
    return false
  }
}

// 計算可用的年份列表（用於新增）- 只能選擇當年度以及下一年度
const availableYearsForCreate = computed(() => {
  const currentYear = new Date().getFullYear()
  const nextYear = currentYear + 2
  const years = []
  
  // 只包含當年度和下一年度，且未存在的年份
  if (!existingYears.value.includes(currentYear)) {
    years.push(currentYear)
  }
  if (!existingYears.value.includes(nextYear)) {
    years.push(nextYear)
  }
  
  return years.sort((a, b) => b - a) // 降序排列
})

// 載入已存在的年份列表
const loadExistingYears = async () => {
  try {
    const response = await apiRequest('/church/service-schedules', {
      method: 'GET'
    })
    if (response.ok) {
      const schedules = await response.json()
      existingYears.value = schedules.map(s => s.year).filter(y => y != null)
    }
  } catch (error) {
    console.error('載入已存在年份失敗:', error)
  }
}

// 監聽選中年份變化，檢查是否已存在
watch(selectedYear, async (newYear) => {
  if (newYear && props.mode === 'add') {
    const exists = await checkYearExists(parseInt(newYear))
    yearWarning.value = exists
    if (exists) {
      selectedYear.value = '' // 如果已存在，清空選擇
    }
  } else {
    yearWarning.value = false
  }
})

// 監聽年度變化，檢查是否已存在（用於編輯模式）
watch(calculatedYear, async (newYear) => {
  if (newYear && props.mode === 'add' && !editingScheduleYear.value) {
    const exists = await checkYearExists(newYear)
    yearWarning.value = exists
  } else {
    yearWarning.value = false
  }
})

const canGenerate = computed(() => {
  // 新增模式：檢查年份和崗位
  if (props.mode === 'add') {
    if (!selectedYear.value) {
      return false
    }
    
    const selectedPositionsList = Object.keys(localInitialSelectedPositions.value).filter(
      posCode => localInitialSelectedPositions.value[posCode] === true
    )
    
    if (selectedPositionsList.length === 0) {
      return false
    }
    
    const allSelectedHaveConfig = selectedPositionsList.every(posCode => hasPositionConfig(posCode))
    
    return allSelectedHaveConfig && !yearWarning.value
  }
  
  // 編輯模式：檢查日期範圍和崗位
  if (!localDateRange.value || localDateRange.value.length !== 2) {
    return false
  }
  
  const selectedPositionsList = Object.keys(localInitialSelectedPositions.value).filter(
    posCode => localInitialSelectedPositions.value[posCode] === true
  )
  
  if (selectedPositionsList.length === 0) {
    return false
  }
  
  const allSelectedHaveConfig = selectedPositionsList.every(posCode => hasPositionConfig(posCode))
  
  return allSelectedHaveConfig
})

// 方法
const hasPositionConfig = (posCode) => {
  const posData = props.positionConfig[posCode]
  if (!posData) return false
  const saturdayCount = (posData.saturday || []).length
  const sundayCount = (posData.sunday || []).length
  return saturdayCount > 0 || sundayCount > 0
}

const getGenerateButtonTooltip = () => {
  if (!localDateRange.value || localDateRange.value.length !== 2) {
    return '請先選擇日期範圍'
  }
  
  const selectedPositionsList = Object.keys(localInitialSelectedPositions.value).filter(
    posCode => localInitialSelectedPositions.value[posCode] === true
  )
  
  if (selectedPositionsList.length === 0) {
    return '請至少選擇一個崗位'
  }
  
  const positionsWithoutConfig = selectedPositionsList.filter(posCode => !hasPositionConfig(posCode))
  if (positionsWithoutConfig.length > 0) {
    return `以下崗位尚未配置人員：${positionsWithoutConfig.map(code => props.positionConfig[code]?.positionName || code).join('、')}`
  }
  
  return ''
}

const closeModal = () => {
  emit('close')
  resetState()
}

const resetState = () => {
  localSchedule.value = []
  localScheduleYear.value = null
  localDateRange.value = []
  localSelectedPositions.value = {}
  localInitialSelectedPositions.value = {}
  localUseRandomAssignment.value = false
  editingScheduleYear.value = null
  isEditing.value = false
  originalSchedule.value = []
  yearWarning.value = false
}

const initializePositionSelection = () => {
  for (const posCode in props.positionConfig) {
    const hasConfig = hasPositionConfig(posCode)
    localInitialSelectedPositions.value[posCode] = hasConfig
    localSelectedPositions.value[posCode] = false
  }
}

// 通知組件引用
const notificationRef = ref(null)

// 顯示通知
const showNotification = (message, type = 'info', duration = 3000) => {
  if (notificationRef.value && typeof notificationRef.value.showNotification === 'function') {
    notificationRef.value.showNotification(message, type, duration)
  } else {
    // 如果通知組件還沒掛載，使用 console 或 alert
    console.log(`[${type}] ${message}`)
  }
}


// 工具函數：格式化日期為 ISO 格式
const formatDateISO = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 工具函數：格式化日期顯示
const formatDateDisplay = (date, dayOfWeek) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}/${month}/${day}(${dayOfWeek})`
}

// 工具函數：獲取週末日期
const getWeekendDates = (start, end) => {
  const dates = []
  const startDate = new Date(start)
  const endDate = new Date(end)
  const currentDate = new Date(startDate)
  while (currentDate <= endDate) {
    const dayOfWeek = currentDate.getDay()
    if (dayOfWeek === 0 || dayOfWeek === 6) {
      dates.push({
        date: new Date(currentDate),
        dayOfWeek: dayOfWeek === 0 ? '日' : '六',
        isSaturday: dayOfWeek === 6
      })
    }
    currentDate.setDate(currentDate.getDate() + 1)
  }
  return dates
}

// 工具函數：根據 ID 獲取人員名稱
const getPersonNameById = (personId, position, item) => {
  if (!personId) return ''
  const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
  const dayKey = dayOfWeek === '六' ? 'saturday' : 'sunday'
  const posData = props.positionConfig[position]
  const persons = (posData && posData[dayKey]) ? posData[dayKey] : []
  const person = persons.find(p => {
    if (typeof p === 'object') {
      return (p.personId || p.id) === personId
    }
    return false
  })
  return person && typeof person === 'object' ? (person.displayName || person.personName || '') : ''
}

// 根據人員名稱查找 ID（用於載入時）
const getPersonIdByName = (personName, posCode, item) => {
  if (!personName) return null
  const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
  const dayKey = dayOfWeek === '六' ? 'saturday' : 'sunday'
  const posData = props.positionConfig[posCode]
  const persons = (posData && posData[dayKey]) ? posData[dayKey] : []
  const person = persons.find(p => {
    if (typeof p === 'object') {
      const displayName = p.displayName || ''
      const pName = p.personName || ''
      return displayName === personName || pName === personName
    }
    return false
  })
  return person && typeof person === 'object' ? (person.personId || person.id || null) : null
}

const loadScheduleForEdit = async (scheduleYear) => {
  try {
    const response = await apiRequest(`/church/service-schedules/${scheduleYear}`, {
      method: 'GET',
      credentials: 'include'
    }, '載入服事表中...')
    const data = await response.json()
    
    if (response.ok) {
      localScheduleYear.value = data.year
      editingScheduleYear.value = scheduleYear
      
      if (data.startDate && data.endDate) {
        localDateRange.value = [data.startDate, data.endDate]
      } else {
        localDateRange.value = []
      }
      
      if (data.scheduleData && Array.isArray(data.scheduleData) && data.scheduleData.length > 0) {
        localSchedule.value = data.scheduleData.map(item => {
          const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
          const scheduleItem = {
            date: item.date,
            formattedDate: item.formattedDate || formatDisplayDate(item.date, dayOfWeek),
            dayOfWeek: dayOfWeek
          }
          
          for (const key in item) {
            if (key !== 'date' && key !== 'formattedDate' && key !== 'dayOfWeek') {
              scheduleItem[key] = item[key]
            }
          }
          
          // 處理多人情況：優先使用後端返回的名稱（已用 "/" 串接）和 IDs 陣列
          Object.keys(props.positionConfig).forEach(posCode => {
            // 如果後端返回了 IDs 陣列，使用它
            if (scheduleItem[posCode + 'Ids'] && Array.isArray(scheduleItem[posCode + 'Ids'])) {
              // 確保名稱欄位存在（後端應該已經返回，但如果沒有則從 ID 查找）
              if (!scheduleItem[posCode] || scheduleItem[posCode].trim() === '') {
                const personNames = []
                for (const personId of scheduleItem[posCode + 'Ids']) {
                  const personName = getPersonNameById(personId, posCode, scheduleItem)
                  if (personName) {
                    personNames.push(personName)
                  }
                }
                if (personNames.length > 0) {
                  scheduleItem[posCode] = personNames.join('/')
                }
              }
            } else if (scheduleItem[posCode] && !scheduleItem[posCode + 'Ids']) {
              // 如果只有名稱（可能包含 "/"），嘗試從名稱查找 ID
              const personNameStr = scheduleItem[posCode]
              if (personNameStr.includes('/')) {
                const names = personNameStr.split('/')
                const ids = []
                for (const name of names) {
                  const personId = getPersonIdByName(name.trim(), posCode, scheduleItem)
                  if (personId) {
                    ids.push(personId)
                  }
                }
                if (ids.length > 0) {
                  scheduleItem[posCode + 'Ids'] = ids
                }
              } else {
                // 單個人員
                const personId = getPersonIdByName(personNameStr, posCode, scheduleItem)
                if (personId) {
                  scheduleItem[posCode + 'Ids'] = [personId]
                }
              }
            }
          })
          
          return scheduleItem
        })
      } else {
        localSchedule.value = []
      }
      
      const loadedPositions = new Set()
      localSchedule.value.forEach(item => {
        for (const key in item) {
          if (key.endsWith('Id') && item[key]) {
            const posCode = key.replace('Id', '')
            if (props.positionConfig[posCode]) {
              loadedPositions.add(posCode)
            }
          } else if (!key.endsWith('Id') && key !== 'date' && key !== 'formattedDate' && key !== 'dayOfWeek' && item[key]) {
            if (props.positionConfig[key]) {
              loadedPositions.add(key)
            }
          }
        }
      })
      
      const newSelectedPositions = {}
      for (const posCode in props.positionConfig) {
        newSelectedPositions[posCode] = loadedPositions.has(posCode)
      }
      localSelectedPositions.value = newSelectedPositions
      
      isEditing.value = true
      originalSchedule.value = JSON.parse(JSON.stringify(localSchedule.value || []))
      localUseRandomAssignment.value = false
      
      showNotification('服事表載入成功！', 'success', 3000)
    } else {
      showNotification('載入失敗：' + (data.error || '未知錯誤'), 'error', 3000)
    }
  } catch (error) {
    showNotification('載入失敗：' + error.message, 'error', 3000)
  }
}

const loadScheduleForView = async (scheduleYear) => {
  try {
    const response = await apiRequest(`/church/service-schedules/${scheduleYear}`, {
      method: 'GET'
    }, '載入服事表中...')
    const data = await response.json()
    
    if (response.ok) {
      localScheduleYear.value = data.year
      
      if (data.scheduleData && Array.isArray(data.scheduleData) && data.scheduleData.length > 0) {
        localSchedule.value = data.scheduleData.map(item => {
          const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
          const scheduleItem = {
            date: item.date,
            formattedDate: item.formattedDate || formatDisplayDate(item.date, dayOfWeek),
            dayOfWeek: dayOfWeek
          }
          
          for (const key in item) {
            if (key !== 'date' && key !== 'formattedDate' && key !== 'dayOfWeek') {
              scheduleItem[key] = item[key]
            }
          }
          
          // 處理多人情況：優先使用後端返回的名稱（已用 "/" 串接）和 IDs 陣列
          Object.keys(props.positionConfig).forEach(posCode => {
            // 如果後端返回了 IDs 陣列，使用它
            if (scheduleItem[posCode + 'Ids'] && Array.isArray(scheduleItem[posCode + 'Ids'])) {
              // 確保名稱欄位存在（後端應該已經返回，但如果沒有則從 ID 查找）
              if (!scheduleItem[posCode] || scheduleItem[posCode].trim() === '') {
                const personNames = []
                for (const personId of scheduleItem[posCode + 'Ids']) {
                  const personName = getPersonNameById(personId, posCode, scheduleItem)
                  if (personName) {
                    personNames.push(personName)
                  }
                }
                if (personNames.length > 0) {
                  scheduleItem[posCode] = personNames.join('/')
                }
              }
            } else if (scheduleItem[posCode] && !scheduleItem[posCode + 'Ids']) {
              // 如果只有名稱（可能包含 "/"），嘗試從名稱查找 ID
              const personNameStr = scheduleItem[posCode]
              if (personNameStr.includes('/')) {
                const names = personNameStr.split('/')
                const ids = []
                for (const name of names) {
                  const personId = getPersonIdByName(name.trim(), posCode, scheduleItem)
                  if (personId) {
                    ids.push(personId)
                  }
                }
                if (ids.length > 0) {
                  scheduleItem[posCode + 'Ids'] = ids
                }
              } else {
                // 單個人員
                const personId = getPersonIdByName(personNameStr, posCode, scheduleItem)
                if (personId) {
                  scheduleItem[posCode + 'Ids'] = [personId]
                }
              }
            }
          })
          
          return scheduleItem
        })
      } else {
        localSchedule.value = []
      }
      
      const loadedPositions = new Set()
      localSchedule.value.forEach(item => {
        for (const key in item) {
          if (key.endsWith('Id') && item[key]) {
            const posCode = key.replace('Id', '')
            if (props.positionConfig[posCode]) {
              loadedPositions.add(posCode)
            }
          } else if (!key.endsWith('Id') && key !== 'date' && key !== 'formattedDate' && key !== 'dayOfWeek' && item[key]) {
            if (props.positionConfig[key]) {
              loadedPositions.add(key)
            }
          }
        }
      })
      
      const newSelectedPositions = {}
      for (const posCode in props.positionConfig) {
        newSelectedPositions[posCode] = loadedPositions.has(posCode)
      }
      localSelectedPositions.value = newSelectedPositions
      
      isEditing.value = false
      showNotification('服事表載入成功！', 'success', 3000)
    } else {
      showNotification('載入失敗：' + (data.error || '未知錯誤'), 'error', 3000)
    }
  } catch (error) {
    showNotification('載入失敗：' + error.message, 'error', 3000)
  }
}

// 當 show、mode 或 scheduleYear 改變時，載入對應的資料
watch([() => props.show, () => props.mode, () => props.scheduleYear], async ([show, mode, scheduleYear]) => {
  if (show) {
    resetState()
    
    if (mode === 'add') {
      // 新增模式：初始化崗位選擇並載入已存在的年份
      initializePositionSelection()
      await loadExistingYears()
    } else if (mode === 'edit' && scheduleYear) {
      // 編輯模式：載入資料並進入編輯模式
      // 如果 positionConfig 為空，等待一下再載入（給父組件時間載入配置）
      if (Object.keys(props.positionConfig).length === 0) {
        // 等待 positionConfig 載入（最多等待 1 秒）
        let retries = 0
        while (Object.keys(props.positionConfig).length === 0 && retries < 10) {
          await new Promise(resolve => setTimeout(resolve, 100))
          retries++
        }
      }
      await loadScheduleForEdit(scheduleYear)
    } else if (mode === 'view' && scheduleYear) {
      // 查看模式：只載入資料，不進入編輯模式
      await loadScheduleForView(scheduleYear)
    }
  }
}, { immediate: true })

// 分配人員到服事表
const distributePersons = (dates) => {
  const schedule = []
  const serviceCount = {}
  
  // 初始化服務次數統計
  const allPersons = new Set()
  Object.keys(props.positionConfig).forEach(position => {
    const posData = props.positionConfig[position]
    if (posData && posData.saturday) {
      posData.saturday.forEach(p => {
        if (typeof p === 'string' || (typeof p === 'object' && (p.includeInAutoSchedule !== false))) {
          const personName = typeof p === 'string' ? p : (p.personName || p.displayName)
          allPersons.add(personName)
        }
      })
    }
    if (posData && posData.sunday) {
      posData.sunday.forEach(p => {
        if (typeof p === 'string' || (typeof p === 'object' && (p.includeInAutoSchedule !== false))) {
          const personName = typeof p === 'string' ? p : (p.personName || p.displayName)
          allPersons.add(personName)
        }
      })
    }
  })
  
  allPersons.forEach(person => {
    const personCount = { total: 0 }
    Object.keys(props.positionConfig).forEach(posCode => {
      personCount[posCode] = 0
    })
    serviceCount[person] = personCount
  })
  
  const selectedPositionsList = Object.keys(localInitialSelectedPositions.value).filter(
    posCode => localInitialSelectedPositions.value[posCode] === true
  )
  
  dates.forEach((dateInfo) => {
    const dayType = dateInfo.isSaturday ? 'saturday' : 'sunday'
    const assignment = {
      date: formatDateISO(dateInfo.date),
      dayOfWeek: dateInfo.dayOfWeek,
      formattedDate: formatDateDisplay(dateInfo.date, dateInfo.dayOfWeek),
    }
    
    selectedPositionsList.forEach(posCode => {
      assignment[posCode] = ''
      assignment[posCode + 'Ids'] = []
    })
    
    const positionsWithDuplicateCheck = []
    const positionsWithoutDuplicateCheck = []
    
    selectedPositionsList.forEach(posCode => {
      const posData = props.positionConfig[posCode]
      if (posData && posData.allowDuplicate) {
        positionsWithoutDuplicateCheck.push(posCode)
      } else {
        positionsWithDuplicateCheck.push(posCode)
      }
    })
    
    const usedPersons = new Set()
    
    positionsWithDuplicateCheck.forEach(posCode => {
      const posData = props.positionConfig[posCode]
      if (!posData || !posData[dayType]) return
      
      const availablePersons = posData[dayType]
        .filter(p => {
          if (typeof p === 'object' && p.includeInAutoSchedule === false) return false
          const personName = typeof p === 'string' ? p : (p.personName || p.displayName)
          return !usedPersons.has(personName)
        })
        .map(p => {
          if (typeof p === 'string') {
            return { id: null, name: p }
          } else {
            const personId = p.personId || p.id
            const personName = p.displayName || p.personName || ''
            return { id: personId, name: personName }
          }
        })
        .filter(p => p.id && p.name)
      
      if (availablePersons.length > 0) {
        availablePersons.sort((a, b) => {
          const countA = serviceCount[a.name] ? (serviceCount[a.name][posCode] || 0) : 0
          const countB = serviceCount[b.name] ? (serviceCount[b.name][posCode] || 0) : 0
          if (countA !== countB) return countA - countB
          const totalA = serviceCount[a.name] ? serviceCount[a.name].total : 0
          const totalB = serviceCount[b.name] ? serviceCount[b.name].total : 0
          return totalA - totalB
        })
        assignment[posCode] = availablePersons[0].name
        assignment[posCode + 'Ids'] = [availablePersons[0].id]
        usedPersons.add(availablePersons[0].name)
        
        if (assignment[posCode] && serviceCount[assignment[posCode]]) {
          if (!serviceCount[assignment[posCode]][posCode]) {
            serviceCount[assignment[posCode]][posCode] = 0
          }
          serviceCount[assignment[posCode]][posCode]++
          serviceCount[assignment[posCode]].total++
        }
      }
    })
    
    positionsWithoutDuplicateCheck.forEach(posCode => {
      const posData = props.positionConfig[posCode]
      if (!posData || !posData[dayType]) return
      
      const availablePersons = posData[dayType]
        .filter(p => typeof p !== 'object' || p.includeInAutoSchedule !== false)
        .map(p => {
          if (typeof p === 'string') {
            return { id: null, name: p }
          } else {
            const personId = p.personId || p.id
            const personName = p.displayName || p.personName || ''
            return { id: personId, name: personName }
          }
        })
        .filter(p => p.id && p.name)
      
      if (availablePersons.length > 0) {
        availablePersons.sort((a, b) => {
          const countA = serviceCount[a.name] ? (serviceCount[a.name][posCode] || 0) : 0
          const countB = serviceCount[b.name] ? (serviceCount[b.name][posCode] || 0) : 0
          if (countA !== countB) return countA - countB
          const totalA = serviceCount[a.name] ? serviceCount[a.name].total : 0
          const totalB = serviceCount[b.name] ? serviceCount[b.name].total : 0
          return totalA - totalB
        })
        assignment[posCode] = availablePersons[0].name
        assignment[posCode + 'Ids'] = [availablePersons[0].id]
        
        if (assignment[posCode] && serviceCount[assignment[posCode]]) {
          if (!serviceCount[assignment[posCode]][posCode]) {
            serviceCount[assignment[posCode]][posCode] = 0
          }
          serviceCount[assignment[posCode]][posCode]++
          serviceCount[assignment[posCode]].total++
        }
      }
    })
    
    schedule.push(assignment)
  })
  
  return schedule
}

// 獲取整年度的週六和週日
const getYearWeekendDates = (year) => {
  const dates = []
  const startDate = new Date(year, 0, 1) // 1月1日
  const endDate = new Date(year, 11, 31) // 12月31日
  
  const currentDate = new Date(startDate)
  while (currentDate <= endDate) {
    const dayOfWeek = currentDate.getDay()
    // 0 = 週日, 6 = 週六
    if (dayOfWeek === 0 || dayOfWeek === 6) {
      dates.push({
        date: new Date(currentDate),
        dayOfWeek: dayOfWeek === 0 ? '日' : '六',
        isSaturday: dayOfWeek === 6
      })
    }
    currentDate.setDate(currentDate.getDate() + 1)
  }
  
  return dates
}

// 產生服事表
const generateSchedule = () => {
  if (!canGenerate.value) {
    if (props.mode === 'add') {
      showNotification('請選擇年份並確保每個崗位至少配置一位人員', 'warning', 4000)
    } else {
    showNotification('請確保每個崗位至少配置一位人員，並選擇日期範圍', 'warning', 4000)
    }
    return
  }
  
  let weekendDates = []
  
  // 新增模式：根據選中的年份生成整年度的週六和週日
  if (props.mode === 'add' && selectedYear.value) {
    const year = parseInt(selectedYear.value)
    weekendDates = getYearWeekendDates(year)
    
    // 設置日期範圍（用於後續保存）
    if (weekendDates.length > 0) {
      const firstDate = weekendDates[0].date
      const lastDate = weekendDates[weekendDates.length - 1].date
      localDateRange.value = [
        `${firstDate.getFullYear()}-${String(firstDate.getMonth() + 1).padStart(2, '0')}-${String(firstDate.getDate()).padStart(2, '0')}`,
        `${lastDate.getFullYear()}-${String(lastDate.getMonth() + 1).padStart(2, '0')}-${String(lastDate.getDate()).padStart(2, '0')}`
      ]
    }
  } else {
    // 編輯模式：使用日期範圍
  const [startDate, endDate] = localDateRange.value
    weekendDates = getWeekendDates(startDate, endDate)
  }
  
  if (weekendDates.length === 0) {
    showNotification('選擇的日期範圍內沒有週六或週日', 'warning', 3000)
    return
  }
  
  localSelectedPositions.value = JSON.parse(JSON.stringify(localInitialSelectedPositions.value))
  
  localSchedule.value = distributePersons(weekendDates)
  
  localSchedule.value.forEach(item => {
    if (item.date && !item.formattedDate) {
      const dateObj = parseDate(item.date)
      if (dateObj) {
        const year = dateObj.getFullYear()
        const month = String(dateObj.getMonth() + 1).padStart(2, '0')
        const day = String(dateObj.getDate()).padStart(2, '0')
        const dayOfWeekChar = item.dayOfWeek === '六' ? '六' : '日'
        item.formattedDate = `${year}/${month}/${day}(${dayOfWeekChar})`
      } else {
        item.formattedDate = item.date
      }
    }
  })
  
  isEditing.value = true
  originalSchedule.value = JSON.parse(JSON.stringify(localSchedule.value || []))
  localUseRandomAssignment.value = false
  
  showNotification('服事表已產生，您可以直接編輯調整', 'success', 3000)
}

// 處理人員選擇變更：支援多選
const handlePersonChange = (item, posCode) => {
  // 確保 Ids 陣列存在
  if (!item[posCode + 'Ids']) {
    item[posCode + 'Ids'] = []
  }
  
  const personIds = item[posCode + 'Ids']
  
  if (!personIds || personIds.length === 0) {
    // 如果沒有選擇人員，清空該崗位的人員
    item[posCode] = ''
    item[posCode + 'Ids'] = []
  } else {
    // 如果有選擇人員，更新人員名稱（用 "/" 串接）
    const availablePersons = getAvailablePersons(item, posCode)
    const selectedNames = []
    
    for (const personId of personIds) {
      const person = availablePersons.find(p => {
        // 處理 ID 類型匹配（可能是字符串或數字）
        const pId = p.id
        return pId != null && (pId === personId || String(pId) === String(personId) || Number(pId) === Number(personId))
      })
      
      if (person) {
        selectedNames.push(person.name || '')
      }
    }
    
    // 用 "/" 串接多人名稱
    item[posCode] = selectedNames.join('/')
    
    // 為了向後兼容，也保留單個 ID（取第一個）
  }
}

// 清空當天所有人員
const clearDayPersons = (index) => {
  const item = localSchedule.value[index]
  if (!item) return
  
  selectedPositionsList.value.forEach(posCode => {
    item[posCode] = ''
    item[posCode + 'Ids'] = []
  })
  
  showNotification('已清空當天所有人員', 'success', 2000)
}

const handlePositionSelectionChange = (posCode) => {
  if (!isEditing.value) return
  
  const isSelected = localSelectedPositions.value[posCode]
  
  if (isSelected) {
    // 檢查原始資料中是否有該崗位的資料
    const hasOriginalData = originalSchedule.value.length > 0 && 
      originalSchedule.value.some(item => 
        (item[posCode] && item[posCode] !== '') || 
        (item[posCode + 'Ids'] && item[posCode + 'Ids'].length > 0)
      )
    
    if (hasOriginalData) {
      // 如果有原始資料，從 originalSchedule 中恢復
      localSchedule.value.forEach((item, index) => {
        const originalItem = originalSchedule.value[index]
        if (originalItem) {
          if (originalItem[posCode] || (originalItem[posCode + 'Ids'] && originalItem[posCode + 'Ids'].length > 0)) {
            item[posCode] = originalItem[posCode] || ''
            item[posCode + 'Ids'] = originalItem[posCode + 'Ids'] || []
          } else {
            // 如果原始資料中該日期沒有該崗位資料，則初始化為空
            item[posCode] = ''
            item[posCode + 'Ids'] = []
          }
        } else {
          // 如果沒有對應的原始資料，初始化為空
          item[posCode] = ''
          item[posCode + 'Ids'] = []
        }
      })
    } else {
      // 如果沒有原始資料，初始化為空
      localSchedule.value.forEach(item => {
        if (!item.hasOwnProperty(posCode)) {
          item[posCode] = ''
          item[posCode + 'Ids'] = []
        }
      })
      
      // 如果有崗位配置，才自動分配（僅針對沒有原始資料的情況）
      if (hasPositionConfig(posCode) && localSchedule.value.length > 0) {
        autoAssignPositionForNewSelection(posCode)
      }
    }
  } else {
    // 取消勾選時，清空該崗位的資料
    localSchedule.value.forEach(item => {
      item[posCode] = ''
      item[posCode + 'Ids'] = []
    })
  }
}

// 為新選擇的崗位自動分配人員
const autoAssignPositionForNewSelection = (posCode) => {
  const posData = props.positionConfig[posCode]
  if (!posData) return
  
  const serviceCount = {}
  localSchedule.value.forEach(item => {
    const personIds = item[posCode + 'Ids'] || []
    const personName = item[posCode]
    if (personIds.length > 0 && personName) {
      const personId = personIds[0]
      if (!serviceCount[personName]) {
        serviceCount[personName] = { count: 0, id: personId }
      }
      serviceCount[personName].count++
    }
  })
  
  const allowDuplicate = posData.allowDuplicate === true
  
  localSchedule.value.forEach(item => {
    // 優先使用 Ids 陣列檢查
    const personIds = item[posCode + 'Ids'] || []
    if (personIds.length > 0) return
    
    const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
    const dayType = dayOfWeek === '六' ? 'saturday' : 'sunday'
    const dayKey = dayType
    
    const availablePersons = (posData[dayKey] || [])
      .filter(p => typeof p !== 'object' || p.includeInAutoSchedule !== false)
      .map(p => {
        if (typeof p === 'string') {
          return { id: null, name: p }
        } else {
          const personId = p.personId || p.id
          const personName = p.displayName || p.personName || ''
          return { id: personId, name: personName }
        }
      })
      .filter(p => p.id && p.name)
    
    if (availablePersons.length === 0) return
    
    let filteredPersons = availablePersons
    if (!allowDuplicate) {
      const usedPersonIds = new Set()
      Object.keys(localSelectedPositions.value).forEach(otherPosCode => {
        if (localSelectedPositions.value[otherPosCode] && otherPosCode !== posCode) {
          // 優先使用 Ids 陣列，向後兼容 Id
          const otherPersonIds = item[otherPosCode + 'Ids'] || []
          otherPersonIds.forEach(id => usedPersonIds.add(id))
        }
      })
      filteredPersons = availablePersons.filter(p => !usedPersonIds.has(p.id))
    }
    
    if (filteredPersons.length === 0) {
      filteredPersons = availablePersons
    }
    
    let selectedPerson
    if (localUseRandomAssignment.value) {
      const randomIndex = Math.floor(Math.random() * filteredPersons.length)
      selectedPerson = filteredPersons[randomIndex]
    } else {
      filteredPersons.sort((a, b) => {
        const countA = serviceCount[a.name] ? serviceCount[a.name].count : 0
        const countB = serviceCount[b.name] ? serviceCount[b.name].count : 0
        return countA - countB
      })
      
      const minCount = filteredPersons.length > 0 
        ? (serviceCount[filteredPersons[0].name] ? serviceCount[filteredPersons[0].name].count : 0)
        : 0
      const minCountGroup = filteredPersons.filter(p => {
        const count = serviceCount[p.name] ? serviceCount[p.name].count : 0
        return count === minCount
      })
      
      const randomIndex = Math.floor(Math.random() * minCountGroup.length)
      selectedPerson = minCountGroup[randomIndex]
    }
    
    if (selectedPerson) {
      item[posCode] = selectedPerson.name
      item[posCode + 'Ids'] = [selectedPerson.id]
      
      if (!serviceCount[selectedPerson.name]) {
        serviceCount[selectedPerson.name] = { count: 0, id: selectedPerson.id }
      }
      serviceCount[selectedPerson.name].count++
    }
  })
  
  showNotification(`已為崗位「${posData.positionName || posCode}」重新隨機分配人員`, 'success', 3000)
}

// 自動分派指定崗位的人員
const autoAssignPosition = (posCode) => {
  if (!isEditing.value || !localSchedule.value || localSchedule.value.length === 0) {
    showNotification('請先進入編輯模式並載入服事表', 'warning', 3000)
    return
  }
  
  const posData = props.positionConfig[posCode]
  if (!posData) {
    showNotification('崗位配置不存在', 'error', 3000)
    return
  }
  
  if (!hasPositionConfig(posCode)) {
    showNotification(`崗位「${posData.positionName || posCode}」尚未配置人員`, 'warning', 3000)
    return
  }
  
  localSchedule.value.forEach(item => {
    item[posCode] = ''
    item[posCode + 'Ids'] = []
  })
  
  const serviceCount = {}
  localSchedule.value.forEach(item => {
    const personIds = item[posCode + 'Ids'] || []
    const personName = item[posCode]
    if (personIds.length > 0 && personName) {
      const personId = personIds[0]
      if (!serviceCount[personName]) {
        serviceCount[personName] = { count: 0, id: personId }
      }
      serviceCount[personName].count++
    }
  })
  
  const allowDuplicate = posData.allowDuplicate === true
  
  localSchedule.value.forEach(item => {
    const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
    const dayType = dayOfWeek === '六' ? 'saturday' : 'sunday'
    const dayKey = dayType
    
    const availablePersons = (posData[dayKey] || [])
      .filter(p => typeof p !== 'object' || p.includeInAutoSchedule !== false)
      .map(p => {
        if (typeof p === 'string') {
          return { id: null, name: p }
        } else {
          const personId = p.personId || p.id
          const personName = p.displayName || p.personName || ''
          return { id: personId, name: personName }
        }
      })
      .filter(p => p.id && p.name)
    
    if (availablePersons.length === 0) return
    
    let filteredPersons = availablePersons
    if (!allowDuplicate) {
      const usedPersonIds = new Set()
      Object.keys(localSelectedPositions.value).forEach(otherPosCode => {
        if (localSelectedPositions.value[otherPosCode] && otherPosCode !== posCode) {
          // 優先使用 Ids 陣列，向後兼容 Id
          const otherPersonIds = item[otherPosCode + 'Ids'] || []
          otherPersonIds.forEach(id => usedPersonIds.add(id))
        }
      })
      filteredPersons = availablePersons.filter(p => !usedPersonIds.has(p.id))
    }
    
    if (filteredPersons.length === 0) {
      filteredPersons = availablePersons
    }
    
    let selectedPerson
    if (localUseRandomAssignment.value) {
      const randomIndex = Math.floor(Math.random() * filteredPersons.length)
      selectedPerson = filteredPersons[randomIndex]
    } else {
      filteredPersons.sort((a, b) => {
        const countA = serviceCount[a.name] ? serviceCount[a.name].count : 0
        const countB = serviceCount[b.name] ? serviceCount[b.name].count : 0
        return countA - countB
      })
      
      const minCount = filteredPersons.length > 0 
        ? (serviceCount[filteredPersons[0].name] ? serviceCount[filteredPersons[0].name].count : 0)
        : 0
      const minCountGroup = filteredPersons.filter(p => {
        const count = serviceCount[p.name] ? serviceCount[p.name].count : 0
        return count === minCount
      })
      
      const randomIndex = Math.floor(Math.random() * minCountGroup.length)
      selectedPerson = minCountGroup[randomIndex]
    }
    
    if (selectedPerson) {
      item[posCode] = selectedPerson.name
      item[posCode + 'Ids'] = [selectedPerson.id]
      
      if (!serviceCount[selectedPerson.name]) {
        serviceCount[selectedPerson.name] = { count: 0, id: selectedPerson.id }
      }
      serviceCount[selectedPerson.name].count++
    }
  })
  
  showNotification(`已重新隨機分配崗位「${posData.positionName || posCode}」的人員`, 'success', 3000)
}

const cancelEdit = () => {
  if (confirm('確定要取消編輯嗎？未保存的修改將丟失。')) {
    if (originalSchedule.value.length > 0) {
      localSchedule.value = JSON.parse(JSON.stringify(originalSchedule.value))
    }
    isEditing.value = false
    // 關閉視窗
    closeModal()
  }
}

// 驗證服事表
const validateSchedule = () => {
  const errors = []
  
  localSchedule.value.forEach((item, index) => {
    const positionsWithDuplicateCheck = []
    const positionsWithoutDuplicateCheck = []
    
    Object.keys(localSelectedPositions.value).forEach(posCode => {
      // 優先使用 Ids 陣列，向後兼容 Id
      const personIds = item[posCode + 'Ids'] || []
      if (localSelectedPositions.value[posCode] && personIds.length > 0) {
        const posData = props.positionConfig[posCode]
        if (posData && !posData.allowDuplicate) {
          positionsWithDuplicateCheck.push(posCode)
        } else {
          positionsWithoutDuplicateCheck.push(posCode)
        }
      }
    })
    
    const usedPersonIds = new Set()
    positionsWithDuplicateCheck.forEach(posCode => {
      // 優先使用 Ids 陣列，向後兼容 Id
      const personIds = item[posCode + 'Ids'] || []
      personIds.forEach(personId => {
        if (personId) {
          if (usedPersonIds.has(personId)) {
            const dateStr = item.formattedDate || item.date || `第 ${index + 1} 行`
            const personName = item[posCode] || '未知人員'
            const positionName = props.positionConfig[posCode]?.positionName || posCode
            // 檢查其他崗位是否也使用了相同的人員 ID
            const otherPositions = positionsWithDuplicateCheck
              .filter(p => {
                if (p === posCode) return false
                const otherPersonIds = item[p + 'Ids'] || []
                return otherPersonIds.includes(personId)
              })
              .map(p => props.positionConfig[p]?.positionName || p)
              .join('、')
            if (otherPositions) {
              errors.push(`${dateStr}：${personName} 同時擔任多個崗位（${positionName} 與 ${otherPositions} 之間不能重複）`)
            }
          }
          usedPersonIds.add(personId)
        }
      })
    })
  })
  
  return errors
}

// 保存服事表
const saveSchedule = async () => {
  if (!localSchedule.value || localSchedule.value.length === 0 || !localDateRange.value || localDateRange.value.length !== 2) {
    showNotification('請先產生服事表', 'warning', 3000)
    return
  }

  // 檢查年度是否已存在
  const year = calculatedYear.value
  if (!year) {
    showNotification('無法計算年度，請檢查日期範圍', 'warning', 3000)
    return
  }
  
  const yearExists = await checkYearExists(year)
  if (yearExists) {
    showNotification(`該年度（${year}年）已存在服事表，每個年度只能有一個版本。請先刪除或更新現有的服事表。`, 'error', 5000)
    return
  }

  const validationErrors = validateSchedule()
  if (validationErrors.length > 0) {
    showNotification('驗證失敗：\n' + validationErrors.join('\n') + '\n\n請修正後再保存。', 'error', 5000)
    return
  }

  saving.value = true
  try {
    const scheduleDataForBackend = localSchedule.value.map(item => {
      const result = { date: item.date }
      
      Object.keys(localSelectedPositions.value).forEach(posCode => {
        if (localSelectedPositions.value[posCode]) {
          // 優先使用 IDs 陣列（多人）
          if (item[posCode + 'Ids'] && Array.isArray(item[posCode + 'Ids']) && item[posCode + 'Ids'].length > 0) {
            result[posCode] = item[posCode] || ''
            result[posCode + 'Ids'] = item[posCode + 'Ids'].map(id => Number(id))
          }
        }
      })
      
      return result
    })

    const response = await apiRequest('/church/service-schedules', {
      method: 'POST',
      body: JSON.stringify({
        scheduleData: scheduleDataForBackend,
        dateRange: localDateRange.value
      })
    }, '保存服事表中...')

    const result = await response.json()
    
    if (response.ok && result.success !== false) {
      showNotification('服事表保存成功！', 'success', 3000)
      editingScheduleYear.value = result.year
      localScheduleYear.value = result.year
      emit('saved', result.year)
      // 延遲關閉視窗，讓用戶看到成功訊息
      setTimeout(() => {
        closeModal()
      }, 1500)
    } else {
      const errorMsg = result.error || '未知錯誤'
      showNotification('保存失敗：' + errorMsg, 'error', 5000)
    }
  } catch (error) {
    const errorMsg = error.message || '未知錯誤'
    if (errorMsg.includes('年度') || errorMsg.includes('year')) {
      showNotification(errorMsg, 'error', 5000)
    } else {
      showNotification('保存失敗：' + errorMsg, 'error', 4000)
    }
  } finally {
    saving.value = false
  }
}

// 更新服事表
const updateSchedule = async () => {
  if (!editingScheduleYear.value) {
    showNotification('請先載入要編輯的服事表', 'warning', 3000)
    return
  }

  if (localSchedule.value && localSchedule.value.length > 0) {
    const validationErrors = validateSchedule()
    if (validationErrors.length > 0) {
      showNotification('驗證失敗：\n' + validationErrors.join('\n') + '\n\n請修正後再保存。', 'error', 5000)
      return
    }

    const hasEmpty = localSchedule.value.some(item => {
      return Object.keys(localSelectedPositions.value).some(posCode => {
        if (localSelectedPositions.value[posCode]) {
          // 優先使用 Ids 陣列，向後兼容 Id
          const personIds = item[posCode + 'Ids'] || []
          return personIds.length === 0
        }
        return false
      })
    })
    if (hasEmpty) {
      if (!confirm('部分日期的人員未填寫完整，確定要保存嗎？')) {
        return
      }
    }
  }

  saving.value = true
  try {
    const scheduleDataForBackend = localSchedule.value.map(item => {
      const result = { date: item.date }
      
      Object.keys(localSelectedPositions.value).forEach(posCode => {
        if (localSelectedPositions.value[posCode]) {
          // 優先使用 IDs 陣列（多人）
          if (item[posCode + 'Ids'] && Array.isArray(item[posCode + 'Ids']) && item[posCode + 'Ids'].length > 0) {
            result[posCode] = item[posCode] || ''
            result[posCode + 'Ids'] = item[posCode + 'Ids'].map(id => Number(id))
          }
        }
      })
      
      return result
    })

    const response = await apiRequest(`/church/service-schedules/${editingScheduleYear.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        scheduleData: scheduleDataForBackend,
        dateRange: localDateRange.value
      })
    }, '更新服事表中...')

    const result = await response.json()
    
    if (response.ok) {
      showNotification('服事表更新成功！', 'success', 3000)
      // 如果年度改變了，更新當前年度
      if (result.year && result.year !== editingScheduleYear.value) {
        localScheduleYear.value = result.year
        editingScheduleYear.value = result.year
      }
      isEditing.value = false
      originalSchedule.value = []
      emit('updated')
      // 延遲關閉視窗，讓用戶看到成功訊息
      setTimeout(() => {
        closeModal()
      }, 1500)
    } else {
      showNotification('更新失敗：' + (result.error || '未知錯誤'), 'error', 4000)
    }
  } catch (error) {
    showNotification('更新失敗：' + error.message, 'error', 4000)
  } finally {
    saving.value = false
  }
}

// 匯出服事表
const exportSchedule = () => {
  if (!localSchedule.value || localSchedule.value.length === 0) return
  
  const selectedPositionsList = Object.keys(localSelectedPositions.value).filter(
    posCode => localSelectedPositions.value[posCode] === true
  )
  
  const headers = ['日期', ...selectedPositionsList.map(posCode => props.positionConfig[posCode]?.positionName || posCode)]
  const rows = localSchedule.value.map(item => {
    const row = [item.formattedDate || formatDisplayDate(item.date, item.dayOfWeek) || item.date]
    selectedPositionsList.forEach(posCode => {
      row.push(item[posCode] || '-')
    })
    return row
  })
  
  const csvContent = [
    headers.join(','),
    ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
  ].join('\n')
  
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `服事表_${localScheduleYear.value || calculatedYear.value || '未知年度'}_${new Date().toISOString().split('T')[0]}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  showNotification('服事表已匯出', 'success', 3000)
}

const formatDisplayDate = (date, dayOfWeek) => {
  if (!date) return ''
  const dateObj = typeof date === 'string' ? parseDate(date) : date
  if (!dateObj || isNaN(dateObj.getTime())) return date
  
  const year = dateObj.getFullYear()
  const month = String(dateObj.getMonth() + 1).padStart(2, '0')
  const day = String(dateObj.getDate()).padStart(2, '0')
  
  if (dayOfWeek) {
    return `${year}/${month}/${day}(${dayOfWeek})`
  }
  
  const dayOfWeekNum = dateObj.getDay()
  const dayOfWeekChar = dayOfWeekNum === 6 ? '六' : dayOfWeekNum === 0 ? '日' : ''
  return `${year}/${month}/${day}${dayOfWeekChar ? `(${dayOfWeekChar})` : ''}`
}

const getAvailablePersons = (item, posCode) => {
  // 保留日期判斷邏輯
  let dayOfWeek = item.dayOfWeek
  
  if (!dayOfWeek && item.date) {
    const dateObj = parseDate(item.date)
    if (dateObj) {
      const day = dateObj.getDay()
      dayOfWeek = day === 6 ? '六' : '日'
    }
  }
  
  if (!dayOfWeek) {
    dayOfWeek = '六'
  }
  
  const dayKey = dayOfWeek === '六' ? 'saturday' : 'sunday'
  const posData = props.positionConfig[posCode]
  let availablePersons = (posData && posData[dayKey]) ? posData[dayKey] : []
  
  // 格式化人員資料
  availablePersons = availablePersons.map(p => {
    if (typeof p === 'string') {
      return { id: null, name: p }
    } else {
      const personId = p.personId || p.id
      const personName = p.displayName || p.personName || ''
      return { id: personId, name: personName }
    }
  }).filter(p => p.id && p.name)
  
  // 移除 allowDuplicate 檢查邏輯
  // 直接返回該日期對應的人員列表
  
  return availablePersons
}

// 阻止背景滾動
let scrollY = 0
const lockBodyScroll = () => {
  scrollY = window.scrollY || window.pageYOffset || document.documentElement.scrollTop
  const body = document.body
  const html = document.documentElement
  
  // 保存當前滾動位置
  body.style.position = 'fixed'
  body.style.top = `-${scrollY}px`
  body.style.width = '100%'
  body.style.overflow = 'hidden'
  html.style.overflow = 'hidden'
}

const unlockBodyScroll = () => {
  const body = document.body
  const html = document.documentElement
  
  body.style.position = ''
  body.style.top = ''
  body.style.width = ''
  body.style.overflow = ''
  html.style.overflow = ''
  
  // 恢復滾動位置
  if (scrollY !== undefined) {
    window.scrollTo(0, scrollY)
  }
}

// 監聽 show 變化，控制背景滾動
watch(() => props.show, (newVal) => {
  if (newVal) {
    // 延遲一點確保 DOM 已更新
    setTimeout(() => {
      lockBodyScroll()
    }, 0)
  } else {
    unlockBodyScroll()
  }
}, { immediate: true })

// 組件卸載時確保恢復滾動
onUnmounted(() => {
  unlockBodyScroll()
})

onMounted(() => {
  initializePositionSelection()
})
</script>

<style scoped>
/* Modal 基礎樣式 */
.modal-overlay {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  min-height: 100vh !important;
  max-height: 100vh !important;
  background: rgba(0, 0, 0, 0.5) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  z-index: 99999 !important;
  overflow-y: auto !important;
  overflow-x: hidden !important;
  -webkit-overflow-scrolling: touch;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
  /* 確保覆蓋所有元素 */
  pointer-events: auto !important;
}

.modal-panel {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  max-width: 95vw;
  max-height: 95vh;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.service-schedule-modal {
  max-width: 1400px;
  width: 95vw;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.modal-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  font-size: 20px;
  line-height: 1;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  color: #333;  background: rgba(0,0,0,0.04);
  border-radius: 8px;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

/* 從主頁面複製的樣式 */
.card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.card h3 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: #333;
}

.date-range-group {
  margin-bottom: 1rem;
}

.date-range-input-wrapper {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.position-selection {
  margin-top: 1rem;
}

.position-selection-label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.75rem;
  color: #333;
}

.position-checkboxes {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.75rem;
}

.position-checkbox-wrapper {
  display: flex;
  align-items: center;
}

.position-checkbox-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  user-select: none;
}

.no-config-hint {
  color: #999;
  font-size: 0.85rem;
}

.generate-button-wrapper {
  margin-top: 1rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.generate-hint {
  color: #666;
  font-size: 0.9rem;
}

.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.schedule-title-section {
  flex: 1;
}

.schedule-name-section {
  margin-top: 0.75rem;
}

.schedule-name-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #333;
}

.schedule-name-input {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  flex: 1;
  min-width: 200px;
}

.schedule-name-display {
  color: #333;
}

.schedule-year-section {
  margin-top: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.year-badge {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  padding: 0.375rem 0.75rem;
  border-radius: 15px;
  font-size: 0.9rem;
  font-weight: 600;
}

.year-warning {
  color: #dc3545;
  font-size: 0.85rem;
  font-weight: 500;
}

.editing-badge {
  background: #667eea;
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  margin-left: 0.5rem;
}

.edit-position-selection {
  margin-top: 1rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.edit-position-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.edit-position-selection-label {
  font-weight: 600;
  color: #333;
  margin: 0;
}

.random-assignment-toggle {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  user-select: none;
  font-size: 0.9rem;
  color: #555;
  position: relative;
}

.toggle-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  background-color: #ccc;
  border-radius: 24px;
  transition: background-color 0.3s;
  flex-shrink: 0;
}

.toggle-slider::before {
  content: "";
  position: absolute;
  width: 20px;
  height: 20px;
  left: 2px;
  top: 2px;
  background-color: white;
  border-radius: 50%;
  transition: transform 0.3s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.toggle-input:checked + .toggle-slider {
  background-color: #667eea;
}

.toggle-input:checked + .toggle-slider::before {
  transform: translateX(20px);
}

.toggle-input:focus + .toggle-slider {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}

.toggle-label {
  flex: 1;
}

.random-assignment-toggle:hover {
  color: #333;
}

.random-assignment-toggle:hover .toggle-slider {
  background-color: #bbb;
}

.random-assignment-toggle:hover .toggle-input:checked + .toggle-slider {
  background-color: #5568d3;
}

.edit-position-selection .position-checkboxes {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.75rem;
}

.schedule-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.schedule-table {
  overflow-x: auto;
  overflow-y: auto;
  max-height: 70vh;
  position: relative;
}

table {
  width: 100%;
  border-collapse: collapse;
  table-layout: auto;
}

thead {
  position: sticky;
  top: 0;
  z-index: 10;
}

th, td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
  border-right: 1px solid #e0e0e0;
}

th:last-child, td:last-child {
  border-right: none;
}

th {
  background: #f8f9fa;
  font-weight: 600;
  color: #333;
  box-shadow: 0 2px 2px -1px rgba(0, 0, 0, 0.1);
}

.date-column {
  min-width: 120px;
  font-weight: 600;
  vertical-align: top;
}

.positions-header {
  padding: 0 !important;
}

.positions-cell {
  padding: 0 !important;
  vertical-align: top;
}

.position-header-cell {
  padding: 0.75rem;
  text-align: center;
  background: #f5f5f5;
  font-weight: 600;
  min-width: 120px;
  white-space: nowrap;
}

.position-header-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.position-header-name {
  flex: 1;
}

.btn-auto-assign-header {
  background: rgba(102, 126, 234, 0.2);
  border: 1px solid rgba(102, 126, 234, 0.4);
  color: #667eea;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-auto-assign-header:hover {
  background: rgba(102, 126, 234, 0.3);
  border-color: rgba(102, 126, 234, 0.6);
  transform: scale(1.05);
}

.btn-auto-assign-header:active {
  transform: scale(0.95);
}

.position-cell {
  padding: 0.75rem;
  text-align: center;
  min-width: 120px;
  white-space: nowrap;
}

.edit-select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.person-multi-select {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.person-multi-select select[multiple] {
  min-height: 80px;
  max-height: 150px;
}

.selected-persons {
  padding: 0.5rem;
  background: #f0f7ff;
  border: 1px solid #b3d9ff;
  border-radius: 4px;
  font-size: 0.85rem;
}

.selected-label {
  font-weight: 600;
  color: #0066cc;
  margin-right: 0.5rem;
}

.selected-names {
  color: #333;
}

.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-save {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(40, 167, 69, 0.3);
}

.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-cancel {
  background: #6c757d;
  color: white;
}

.btn-cancel:hover {
  background: #5a6268;
}

.btn-export {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.btn-export:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(245, 87, 108, 0.3);
}

/* 年份選擇樣式 */
.year-selection-group {
  margin-bottom: 1.5rem;
}

.year-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.year-input-wrapper label {
  font-weight: 600;
  color: #4a5568;
  font-size: 0.9rem;
}

.year-select {
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.95rem;
  min-width: 200px;
}

.year-select:focus {
  outline: none;
  border-color: #667eea;
}

/* 日期欄位樣式 */
.date-cell-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-start;
}

.btn-clear-day {
  background: #ef4444;
  color: white;
  padding: 0.25rem 0.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.75rem;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-clear-day:hover {
  background: #dc2626;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3);
}

.btn-clear-day:active {
  transform: translateY(0);
}


/* =============================
   UI Refresh (Comfort/Polish A)
   Beautify "檢視" 詳細資料（不動資料流）
   ============================= */

.modal-panel{
  border-radius:24px;
  box-shadow:0 24px 70px rgba(15,23,42,.18);
  border:1px solid rgba(15,23,42,.08);
}

.modal-header{
  padding:20px 22px;
  background:linear-gradient(180deg, rgba(248,250,252,.96), rgba(255,255,255,.92));
  border-bottom:1px solid rgba(15,23,42,.08);
}

.modal-title{
  font-size:20px;
  letter-spacing:.2px;
  color:#0f172a;
}

.modal-body{
  padding:18px 18px 22px;
  background:
    radial-gradient(1200px 600px at 20% -10%, rgba(16,185,129,.09), rgba(255,255,255,0)),
    radial-gradient(900px 500px at 90% 0%, rgba(59,130,246,.06), rgba(255,255,255,0)),
    #fff;
}

.card{
  border-radius:18px;
  box-shadow:0 10px 30px rgba(15,23,42,.08);
  border:1px solid rgba(15,23,42,.06);
}

.schedule-header{
  align-items:flex-start;
  gap:14px;
}

.schedule-title-section h3{
  font-size:18px;
  margin:0;
  color:#0f172a;
}

.year-badge{
  display:inline-flex;
  align-items:center;
  gap:8px;
  padding:6px 10px;
  border-radius:999px;
  background:rgba(16,185,129,.10);
  color:#065f46;
  border:1px solid rgba(16,185,129,.18);
  font-weight:700;
}

.editing-badge{
  margin-left:10px;
  font-size:12px;
  padding:4px 8px;
  border-radius:999px;
  background:rgba(59,130,246,.10);
  border:1px solid rgba(59,130,246,.16);
  color:#1d4ed8;
}

.schedule-actions .btn-export{
  background:linear-gradient(180deg, #0ea5e9, #0284c7);
  border:none;
  color:#fff;
  box-shadow:0 10px 18px rgba(2,132,199,.22);
}

.schedule-actions .btn-save{
  background:linear-gradient(180deg, #16a34a, #15803d);
  border:none;
  color:#fff;
  box-shadow:0 10px 18px rgba(21,128,61,.20);
}

.schedule-actions .btn-cancel{
  background:#fff;
  border:1px solid rgba(15,23,42,.14);
  color:#0f172a;
}

.schedule-table{
  border-radius:16px;
  overflow:hidden;
  border:1px solid rgba(15,23,42,.08);
  background:#fff;
}

.schedule-table table{
  width:100%;
  border-collapse:separate;
  border-spacing:0;
  font-size:14px;
}

.schedule-table thead th{
  position:sticky;
  top:0;
  z-index:2;
  background:linear-gradient(180deg, rgba(248,250,252,.98), rgba(241,245,249,.95));
  color:#0f172a;
  font-weight:700;
  border-bottom:1px solid rgba(15,23,42,.08);
}

.schedule-table th,
.schedule-table td{
  padding:12px 12px;
  border-bottom:1px solid rgba(15,23,42,.06);
  vertical-align:top;
}

.schedule-table tbody tr:nth-child(even) td{
  background:rgba(248,250,252,.65);
}

.date-column{
  min-width:160px;
  white-space:nowrap;
}

.position-cell{
  min-width:140px;
}

.position-header-content{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:8px;
}

.position-header-name{
  line-height:1.1;
}

.btn-auto-assign-header{
  border:1px solid rgba(15,23,42,.12);
  background:#fff;
  border-radius:10px;
  padding:4px 8px;
}

.btn-auto-assign-header:hover{
  background:rgba(15,23,42,.04);
}

.btn-clear-day{
  border-radius:10px;
  border:1px solid rgba(15,23,42,.12);
  background:#fff;
}

.btn-clear-day:hover{
  background:rgba(15,23,42,.04);
}

.person-multi-select .edit-select{
  border-radius:14px;
  border:1px solid rgba(15,23,42,.14);
  padding:10px;
  background:rgba(255,255,255,.9);
}

.selected-persons{
  margin-top:8px;
  border-radius:12px;
  padding:8px 10px;
  background:rgba(16,185,129,.08);
  border:1px solid rgba(16,185,129,.12);
}

@media (max-width: 768px){
  .service-schedule-modal{ width:96vw; }
  .schedule-table{ overflow:auto; }
  .schedule-table table{ min-width: 900px; }
}



/* ===== 月份卡片檢視（View 模式） ===== */
.month-cards{
  display:flex;
  flex-direction:column;
  gap:14px;
}
.month-card{
  border:1px solid #eef0f2;
  background: linear-gradient(180deg, #ffffff 0%, #fbfbfc 100%);
  border-radius:14px;
  padding:14px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
}
.month-card__header{
  display:flex;
  align-items:flex-start;
  justify-content:space-between;
  gap:12px;
  padding-bottom:10px;
  border-bottom:1px dashed #e7e9ec;
}
.month-card__title{
  display:flex;
  flex-direction:column;
  gap:4px;
}
.month-title{
  font-size:1.05rem;
  font-weight:700;
  color:#1f2937;
}
.month-meta{
  font-size:.85rem;
  color:#6b7280;
}
.month-card__summary{
  display:flex;
  flex-wrap:wrap;
  gap:8px;
  justify-content:flex-end;
}
.pill{
  display:inline-flex;
  align-items:center;
  gap:6px;
  font-size:.85rem;
  color:#374151;
  background:#f3f4f6;
  border:1px solid #e5e7eb;
  padding:6px 10px;
  border-radius:999px;
}
.month-card__weeks{
  margin-top:12px;
  display:flex;
  flex-direction:column;
  gap:10px;
}
.week-card{
  border:1px solid #eef0f2;
  border-radius:12px;
  background:#fff;
  overflow:hidden;
}
.week-card__summary{
  list-style:none;
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:12px;
  padding:12px 12px;
  cursor:pointer;
}
.week-card__summary::-webkit-details-marker{display:none;}
.week-title{
  font-weight:650;
  color:#111827;
}
.week-missing{
  margin-top:4px;
  font-size:.85rem;
  color:#b45309;
}
.week-ok{
  margin-top:4px;
  font-size:.85rem;
  color:#047857;
}
.week-card__right{
  font-size:.9rem;
  color:#4b5563;
  white-space:nowrap;
}
.week-card__detail{
  padding:12px;
  border-top:1px solid #f1f2f4;
  background:#fcfcfd;
}
.day-block + .day-block{
  margin-top:12px;
}
.day-block__title{
  font-weight:700;
  font-size:.9rem;
  color:#1f2937;
  margin-bottom:8px;
}
.day-grid{
  display:grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap:10px;
}
.kv{
  border:1px solid #eef0f2;
  background:#fff;
  border-radius:10px;
  padding:10px;
}
.k{
  font-size:.8rem;
  color:#6b7280;
  margin-bottom:4px;
}
.v{
  font-size:.95rem;
  color:#111827;
  font-weight:600;
  word-break:break-word;
}
@media (max-width: 768px){
  .month-card__header{flex-direction:column; align-items:flex-start;}
  .month-card__summary{justify-content:flex-start;}
  .day-grid{grid-template-columns: 1fr;}
}


/* ===== UI polish: month summary lines + week counts (screen) ===== */
.month-card__summary{
  display:grid;
  gap:6px;
  justify-items:end;
  text-align:right;
}
.month-card__summary .sum-line{
  font-size:.85rem;
  color:#6b7280;
  line-height:1.2;
}

/* ===== Print summary mode (print/PDF only) ===== */

/* ===== Print summary mode (only in print/PDF) ===== */
@media print {
  .modal-overlay { background: transparent !important; padding: 0 !important; }
  .service-schedule-modal { max-height: none !important; box-shadow: none !important; border-radius: 0 !important; }
  .btn-close, .schedule-actions, .week-card__right { display: none !important; }
  .week-card__detail { display: none !important; }
  details.week-card { border: 1px solid #ddd !important; break-inside: avoid; }
  summary.week-card__summary { padding: 10px 12px !important; }
  .month-card { break-inside: avoid; page-break-inside: avoid; }
  .month-card__header { border-bottom: 1px solid #ddd !important; padding-bottom: 8px !important; }
  .month-card__summary { text-align: right !important; }
  .month-card__summary .sum-line { display: block !important; color: #000 !important; }
  .week-title { font-weight: 600 !important; }
}

</style>
