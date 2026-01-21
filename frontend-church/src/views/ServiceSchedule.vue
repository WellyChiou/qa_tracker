<template>
  <div class="service-schedule">
    <section class="page-hero" data-hero="true">
      <div class="container hero-surface">
        <div class="hero-inner">
          <div class="badge">Schedule</div>
          <h1 class="h1" style="margin-top:12px">服事人員安排</h1>
          <p class="lead" style="margin-top:10px">本週服事與崗位人員配置一覽。</p>
        </div>
      </div>
    </section>

    <section class="section section--tight" v-reveal="{ mode: 'minimal' }">
      <div class="container">
        <!-- Modals / Notification (keep logic untouched) -->
        <PositionManagementModal :show="showPositionManagement" @close="closePositionManagement" />

        <div class="two-col">
          <!-- 本週服事（重點呈現，閱讀優先） -->
          <div class="card card--stack">
            <div class="card-head">
              <div>
                <h2 class="h2" style="margin:0">本週服事</h2>
                <p class="muted" style="margin:6px 0 0">快速查看週六 / 週日各崗位人員。</p>
              </div>
              <div class="pill" v-if="loadingCurrentWeek">載入中…</div>
            </div>

            <div v-if="currentWeekSchedule && (currentWeekSchedule.saturday || currentWeekSchedule.sunday)" class="week-grid">
              <!-- 週六 -->
              <div v-if="currentWeekSchedule.saturday" class="day-card">
                <div class="day-head">
                  <div class="day-left">
                    <div class="day-title">週六</div>
                    <div class="day-date">{{ formatWeekDate(currentWeekSchedule.saturday.date) }}</div>
                  </div>
                  <div class="day-right">
                    <div class="day-kpi muted">已安排 {{ assignedCount('sat') }} / {{ totalPositionsCount }} 崗位</div>
                    <button type="button" class="acc-toggle btn btn-ghost" @click="toggleDay('sat')">
                      {{ dayOpen.sat ? '收合' : '展開' }}
                    </button>
                  </div>
                </div>

                <transition name="acc" v-if="!isMobile">
                  <div v-show="dayOpen.sat" class="acc-body">
                    <div class="pos-grid">
                      <div v-for="(posData, posCode) in positionConfig" :key="`sat-${posCode}`" class="pos-row">
                        <div class="pos-name">{{ posData.positionName || posCode }}</div>
                        <div class="pos-person">{{ getCurrentWeekPerson(currentWeekSchedule.saturday, posCode) || '-' }}</div>
                      </div>
                    </div>
                  </div>
                </transition>
                <div v-else v-show="dayOpen.sat" class="acc-body">
                  <div class="pos-grid">
                    <div v-for="(posData, posCode) in positionConfig" :key="`sat-m-${posCode}`" class="pos-row">
                      <div class="pos-name">{{ posData.positionName || posCode }}</div>
                      <div class="pos-person">{{ getCurrentWeekPerson(currentWeekSchedule.saturday, posCode) || '-' }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 週日 -->
              <div v-if="currentWeekSchedule.sunday" class="day-card">
                <div class="day-head">
                  <div class="day-left">
                    <div class="day-title">週日</div>
                    <div class="day-date">{{ formatWeekDate(currentWeekSchedule.sunday.date) }}</div>
                  </div>
                  <div class="day-right">
                    <div class="day-kpi muted">已安排 {{ assignedCount('sun') }} / {{ totalPositionsCount }} 崗位</div>
                    <button type="button" class="acc-toggle btn btn-ghost" @click="toggleDay('sun')">
                      {{ dayOpen.sun ? '收合' : '展開' }}
                    </button>
                  </div>
                </div>

                <transition name="acc" v-if="!isMobile">
                  <div v-show="dayOpen.sun" class="acc-body">
                    <div class="pos-grid">
                      <div v-for="(posData, posCode) in positionConfig" :key="`sun-${posCode}`" class="pos-row">
                        <div class="pos-name">{{ posData.positionName || posCode }}</div>
                        <div class="pos-person">{{ getCurrentWeekPerson(currentWeekSchedule.sunday, posCode) || '-' }}</div>
                      </div>
                    </div>
                  </div>
                </transition>
                <div v-else v-show="dayOpen.sun" class="acc-body">
                  <div class="pos-grid">
                    <div v-for="(posData, posCode) in positionConfig" :key="`sun-m-${posCode}`" class="pos-row">
                      <div class="pos-name">{{ posData.positionName || posCode }}</div>
                      <div class="pos-person">{{ getCurrentWeekPerson(currentWeekSchedule.sunday, posCode) || '-' }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="empty">
              <p class="muted" style="margin:0">本週尚無服事安排</p>
            </div>
          </div>

          <!-- 崗位人員配置（摘要 + 入口） -->
          <div class="card card--stack">
            <div class="card-head">
              <div>
                <h2 class="h2" style="margin:0">崗位人員配置</h2>
                <p class="muted" style="margin:6px 0 0">查看每個崗位在週六 / 週日的人員設定。</p>
              </div>
              <button @click="openPositionManagement" class="btn btn-ghost">檢視崗位</button>
            </div>

            <div class="kv">
              <div class="kv-item">
                <div class="kv-label">崗位數</div>
                <div class="kv-value">{{ Object.keys(positionConfig || {}).length }}</div>
              </div>
              <div class="kv-item">
                <div class="kv-label">週六人員</div>
                <div class="kv-value">{{ satPersonCount }}</div>
              </div>
              <div class="kv-item">
                <div class="kv-label">週日人員</div>
                <div class="kv-value">{{ sunPersonCount }}</div>
              </div>
            </div>

            <div class="pos-summary">
              <div v-for="(posData, posCode) in positionConfig" :key="posCode" class="pos-chip">
                <span class="pos-chip__name">{{ posData.positionName || posCode }}</span>
                <span class="pos-chip__meta">
                  週六 {{ normalizePeople(posData.saturday).length }} / 週日 {{ normalizePeople(posData.sunday).length }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 歷史服事表（列表：可讀性優先，卡片化；操作固定位置） -->
        <div class="card card--stack" style="margin-top:18px" v-reveal="{ mode: 'minimal' }">
          <div class="card-head">
            <div>
              <h2 class="h2" style="margin:0">服事表</h2>
              <p class="muted" style="margin:6px 0 0">按年度檢視已保存的服事表。</p>
            </div>
            <div class="filters-inline">
              <label class="filters-label">年份</label>
              <select v-model="filterYear" class="form-input filters-select">
                <option value="">全部</option>
                <option v-for="year in availableYears" :key="year" :value="year">{{ year }}年</option>
              </select>
              <button class="btn btn-ghost" @click="resetFilters" :disabled="!filterYear">清除</button>
            </div>
          </div>

          <div v-if="filteredHistoryList.length > 0" class="history">
            <div class="history-meta">
              <div class="muted">共 {{ displayedHistoryList.length }} 筆</div>
            </div>

            <div class="history-cards">
              <div class="history-card" v-for="item in displayedHistoryList" :key="item.year">
                <div class="history-card__top">
                  <div>
                    <div class="history-year">{{ item.year }}年</div>
                    <div class="muted history-created">建立時間：{{ formatDateTime(item.createdAt) }}</div>
                  </div>
                  <span class="badge">年度</span>
                </div>

                <div class="history-card__actions">
                  <button @click="openViewModal(item.year)" class="btn btn-primary">檢視</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty">
            <p class="muted" style="margin:0">
              {{ historyList.length === 0 ? '尚無服事表資料' : '沒有符合條件的資料' }}
            </p>
          </div>
        </div>

        <!-- 服事表 Modal -->
        <ServiceScheduleModal
          :show="showScheduleModal"
          :mode="scheduleModalMode"
          :schedule-year="currentScheduleYear"
          :position-config="positionConfig"
          @close="closeScheduleModal"
          @saved="handleScheduleSaved"
          @updated="handleScheduleUpdated"
        />
      </div>
    </section>
  </div>
</template>


<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import DateRangePicker from '@/components/DateRangePicker.vue'
import PositionManagementModal from '@/components/PositionManagementModal.vue'
import ServiceScheduleModal from '@/components/ServiceScheduleModal.vue'
import { apiRequest } from '@/utils/api'
import { toast } from '@shared/composables/useToast'

// 崗位人員配置（從新的 API 載入）
const positionConfig = ref({})

// UI only: responsive flags (do NOT touch data flow)
const isMobile = ref(false)
let _mq
const _updateMobile = () => {
  if (typeof window === 'undefined') return
  if (!_mq) _mq = window.matchMedia('(max-width: 768px)')
  isMobile.value = _mq.matches
}

const dayOpen = ref({ sat: true, sun: true })
const toggleDay = (key) => {
  dayOpen.value = { ...dayOpen.value, [key]: !dayOpen.value[key] }
}



// 週六/週日人員總數（僅用於摘要顯示，不影響資料流）
const normalizePeople = (arr) => {
  const list = Array.isArray(arr) ? arr : []
  return list
    .map((x) => {
      if (x == null) return ''
      if (typeof x === 'string') return x
      if (typeof x === 'object') return x.personName || x.name || x.person || x.label || ''
      return ''
    })
    .map((s) => String(s).trim())
    .filter(Boolean)
}

const satPersonCount = computed(() => {
  const cfg = positionConfig.value || {}
  const s = new Set()
  Object.values(cfg).forEach((p) => {
    normalizePeople(p?.saturday).forEach((name) => s.add(name))
  })
  return s.size
})

const sunPersonCount = computed(() => {
  const cfg = positionConfig.value || {}
  const s = new Set()
  Object.values(cfg).forEach((p) => {
    normalizePeople(p?.sunday).forEach((name) => s.add(name))
  })
  return s.size
})


// 當週已指派崗位數（僅 UI 摘要用）
const totalPositionsCount = computed(() => Object.keys(positionConfig.value || {}).length)
const assignedCount = (dayKey) => {
  const day = dayKey === 'sat' ? currentWeekSchedule.value?.saturday : currentWeekSchedule.value?.sunday
  if (!day) return 0
  const cfg = positionConfig.value || {}
  let n = 0
  Object.keys(cfg).forEach((posCode) => {
    const p = getCurrentWeekPerson(day, posCode)
    if (p && p !== '-') n += 1
  })
  return n
}
// 崗位管理 Modal 顯示狀態
const showPositionManagement = ref(false)

// 服事表 Modal 顯示狀態
const showScheduleModal = ref(false)
const scheduleModalMode = ref('add') // 'add' | 'edit' | 'view'

// 日期範圍（格式：[startDate, endDate]，日期格式為 'YYYY-MM-DD'）
const dateRange = ref([])

// 選擇的崗位（用於產生服事表時）- 動態初始化
const selectedPositions = ref({}) // 編輯模式下的崗位選擇
const initialSelectedPositions = ref({}) // 新增服事表時的崗位選擇

// 安排結果
const schedule = ref([])

// 歷史記錄列表
const historyList = ref([])
const sortedHistoryList = computed(() => {
  const list = Array.isArray(historyList.value) ? [...historyList.value] : []
  // 依日期/年份排序：新的在前
  return list.sort((a, b) => {
    const ad = a?.dateRange?.[0] || a?.startDate || a?.createdAt || ''
    const bd = b?.dateRange?.[0] || b?.startDate || b?.createdAt || ''
    return String(bd).localeCompare(String(ad))
  })
})


// 本週服事人員
const currentWeekSchedule = ref({ saturday: null, sunday: null })
const loadingCurrentWeek = ref(false)

// 篩選條件
const filterYear = ref('')

// 計算可用的年份列表
const availableYears = computed(() => {
  const years = new Set()
  historyList.value.forEach(item => {
    if (item.year) {
      years.add(item.year)
    }
  })
  return Array.from(years).sort((a, b) => b - a) // 降序排列
})

// 篩選後的歷史記錄列表
const filteredHistoryList = computed(() => {
  if (!filterYear.value) {
    return historyList.value
  }
  return historyList.value.filter(item => item.year === parseInt(filterYear.value))
})

// 顯示用歷史清單（已移除分頁，閱讀優先）
const displayedHistoryList = computed(() => {
  const list = Array.isArray(filteredHistoryList.value) ? [...filteredHistoryList.value] : []
  // 新的在前：優先 createdAt，其次 year
  return list.sort((a, b) => {
    const ac = a?.createdAt || ''
    const bc = b?.createdAt || ''
    const by = b?.year ?? 0
    const ay = a?.year ?? 0
    if (bc && ac && String(bc) !== String(ac)) return String(bc).localeCompare(String(ac))
    if (by != ay) return Number(by) - Number(ay)
    return 0
  })
})

// 保存狀態
const saving = ref(false)

// 編輯狀態
const isEditing = ref(false)
const editingScheduleYear = ref(null)
const originalSchedule = ref([]) // 保存原始數據，用於取消編輯
const isLoadedFromHistory = ref(false) // 標記是否從歷史記錄載入（載入的不應該顯示「保存服事表」按鈕）
const useRandomAssignment = ref(false) // 是否使用完全隨機分配（不考慮服務次數）

// 當前載入的服事表年度
const currentScheduleYear = ref(null)

// 顯示通知（使用共用 Toast 系統）
const showNotification = (message, type = 'info', duration = 3000) => {
  const opts = duration > 0 ? { duration } : {}
  if (type === 'success') {
    toast.success(message, '成功', opts)
  } else if (type === 'error') {
    toast.error(message, '錯誤', opts)
  } else if (type === 'warning') {
    toast.warning(message, '提醒', opts)
  } else {
    toast.info(message, '提示', opts)
  }
}


// 檢查崗位是否有配置人員
const hasPositionConfig = (posCode) => {
    const posData = positionConfig.value[posCode]
    if (!posData) return false
    const satCount = (posData.saturday || []).length
    const sunCount = (posData.sunday || []).length
    return satCount > 0 || sunCount > 0
  }
  
// 獲取選中的崗位列表（用於表格渲染）
const selectedPositionsList = computed(() => {
  return Object.keys(positionConfig.value).filter(
    posCode => selectedPositions.value[posCode] === true
  )
})

// 檢查是否可以產生服事表
const canGenerate = computed(() => {
  // 檢查日期範圍
  if (!dateRange.value || dateRange.value.length !== 2) {
    return false
  }
  
  // 至少選擇一個崗位（使用 initialSelectedPositions，因為這是新增服事表時的選擇）
  const selectedPositionsList = Object.keys(initialSelectedPositions.value).filter(
    posCode => initialSelectedPositions.value[posCode] === true
  )
  
  if (selectedPositionsList.length === 0) {
    return false
  }
  
  // 檢查選中的崗位是否都有配置人員
  const allSelectedHaveConfig = selectedPositionsList.every(posCode => hasPositionConfig(posCode))
  
  return allSelectedHaveConfig
})

// 獲取產生服事表按鈕的提示信息
const getGenerateButtonTooltip = () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    return '請先選擇日期範圍'
  }
  
  const selectedPositionsList = Object.keys(initialSelectedPositions.value).filter(
    posCode => initialSelectedPositions.value[posCode] === true
  )
  
  if (selectedPositionsList.length === 0) {
    return '請至少選擇一個崗位'
  }
  
  const positionsWithoutConfig = selectedPositionsList.filter(posCode => !hasPositionConfig(posCode))
  if (positionsWithoutConfig.length > 0) {
    const positionNames = positionsWithoutConfig.map(posCode => {
      const posData = positionConfig.value[posCode]
      return posData ? posData.positionName || posCode : posCode
    }).join('、')
    return `崗位「${positionNames}」尚未配置人員`
  }
  
  return '可以產生服事表'
}

const openViewModal = (year) => {
  scheduleModalMode.value = 'view'
  currentScheduleYear.value = year
  showScheduleModal.value = true
}

const closeScheduleModal = () => {
  showScheduleModal.value = false
  scheduleModalMode.value = 'add'
  currentScheduleYear.value = null
}

const handleScheduleSaved = () => {
  closeScheduleModal()
  loadHistory()
  showNotification('服事表保存成功！', 'success', 3000)
}

const handleScheduleUpdated = () => {
  closeScheduleModal()
  loadHistory()
  showNotification('服事表更新成功！', 'success', 3000)
}

// 打開崗位管理 Modal
const openPositionManagement = () => {
  showPositionManagement.value = true
}

// 關閉崗位管理 Modal
const closePositionManagement = async () => {
  showPositionManagement.value = false
  await loadPositionConfig() // 重新載入配置
  
  // 更新 initialSelectedPositions：如果崗位沒有配置人員，取消勾選
  const newInitialSelectedPositions = { ...initialSelectedPositions.value }
  for (const posCode in positionConfig.value) {
    if (!hasPositionConfig(posCode)) {
      newInitialSelectedPositions[posCode] = false
    }
  }
  initialSelectedPositions.value = newInitialSelectedPositions
  
  // 更新 selectedPositions：如果崗位沒有配置人員，取消勾選（編輯模式下）
  const newSelectedPositions = { ...selectedPositions.value }
  for (const posCode in positionConfig.value) {
    if (!hasPositionConfig(posCode)) {
      newSelectedPositions[posCode] = false
    }
  }
  selectedPositions.value = newSelectedPositions
  
  // 如果正在編輯模式，確保下拉選單顯示最新的人員列表
  // 由於 positionConfig 是響應式的，下拉選單會自動更新
}

// 獲取日期範圍內的所有週六和週日
const getWeekendDates = (start, end) => {
  const dates = []
  const startDate = new Date(start)
  const endDate = new Date(end)
  
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

// 格式化日期為 ISO 格式（YYYY-MM-DD），用於存儲
const formatDateISO = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 格式化日期（包含星期），用於顯示
const formatDateDisplay = (date, dayOfWeek) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}/${month}/${day}(${dayOfWeek})`
}

// 從固定名單中分配人員
const distributePersons = (dates) => {
  const schedule = []
  
  // 統計每個人在每個崗位的服務次數
  const serviceCount = {}
  
  // 初始化服務次數統計
  // 只統計參與自動分配的人員
  const allPersons = new Set()
  Object.keys(positionConfig.value).forEach(position => {
    const posData = positionConfig.value[position]
    if (posData && posData.saturday) {
      posData.saturday.forEach(p => {
        // 如果是對象且有 includeInAutoSchedule 字段，檢查是否為 true
        // 如果是字符串，默認參與自動分配
        if (typeof p === 'string' || (typeof p === 'object' && (p.includeInAutoSchedule !== false))) {
          const personName = typeof p === 'string' ? p : (p.personName || p.displayName)
          allPersons.add(personName)
        }
      })
    }
    if (posData && posData.sunday) {
      posData.sunday.forEach(p => {
        // 如果是對象且有 includeInAutoSchedule 字段，檢查是否為 true
        // 如果是字符串，默認參與自動分配
        if (typeof p === 'string' || (typeof p === 'object' && (p.includeInAutoSchedule !== false))) {
          const personName = typeof p === 'string' ? p : (p.personName || p.displayName)
          allPersons.add(personName)
        }
      })
    }
  })
  
  // 動態初始化服務次數統計（根據所有崗位）
  allPersons.forEach(person => {
    const personCount = { total: 0 }
    // 為每個崗位初始化計數
    Object.keys(positionConfig.value).forEach(posCode => {
      personCount[posCode] = 0
    })
    serviceCount[person] = personCount
  })
  
  // 獲取所有選中的崗位
  const selectedPositionsList = Object.keys(selectedPositions.value).filter(
    posCode => selectedPositions.value[posCode] === true
  )
  
  // 為每個日期分配人員
  dates.forEach((dateInfo) => {
    const dayType = dateInfo.isSaturday ? 'saturday' : 'sunday'
    
    // 動態初始化 assignment 對象（只包含選中的崗位）
    const assignment = {
      date: formatDateISO(dateInfo.date), // 使用 ISO 格式存儲日期
      dayOfWeek: dateInfo.dayOfWeek, // 保存星期信息
      formattedDate: formatDateDisplay(dateInfo.date, dateInfo.dayOfWeek), // 格式化顯示日期
    }
    
    // 為每個選中的崗位初始化欄位
    selectedPositionsList.forEach(posCode => {
      assignment[posCode] = ''
      assignment[posCode + 'Ids'] = []
    })
    
    // 根據 allowDuplicate 分類崗位
    const positionsWithDuplicateCheck = [] // 需要檢查重複的崗位
    const positionsWithoutDuplicateCheck = [] // 不需要檢查重複的崗位（allowDuplicate = true）
    
    selectedPositionsList.forEach(posCode => {
      const posData = positionConfig.value[posCode]
      if (posData && posData.allowDuplicate) {
        positionsWithoutDuplicateCheck.push(posCode)
      } else {
        positionsWithDuplicateCheck.push(posCode)
      }
    })
    
    const usedPersons = new Set() // 記錄今天已使用的人員（用於檢查重複）
    
    // 先分配需要檢查重複的崗位
    positionsWithDuplicateCheck.forEach(posCode => {
      const posData = positionConfig.value[posCode]
      if (!posData || !posData[dayType]) {
        return
      }
      
      // 過濾：1. 只使用參與自動分配的人員 2. 過濾掉今天已在其他崗位的人員（如果該崗位不允許重複）
      const availablePersons = posData[dayType]
        .filter(p => {
          // 只使用參與自動分配的人員
          if (typeof p === 'object' && p.includeInAutoSchedule === false) {
            return false
          }
          // 如果是字符串，默認參與自動分配
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
        // 從可用人員中選擇該崗位服務次數最少的
        availablePersons.sort((a, b) => {
          const countA = serviceCount[a.name] ? (serviceCount[a.name][posCode] || 0) : 0
          const countB = serviceCount[b.name] ? (serviceCount[b.name][posCode] || 0) : 0
          if (countA !== countB) return countA - countB
          // 如果次數相同，選擇總服務次數最少的
          const totalA = serviceCount[a.name] ? serviceCount[a.name].total : 0
          const totalB = serviceCount[b.name] ? serviceCount[b.name].total : 0
          return totalA - totalB
        })
        assignment[posCode] = availablePersons[0].name
        assignment[posCode + 'Ids'] = [availablePersons[0].id]
        usedPersons.add(availablePersons[0].name)
        
        // 更新服務次數
        if (assignment[posCode] && serviceCount[assignment[posCode]]) {
          if (!serviceCount[assignment[posCode]][posCode]) {
            serviceCount[assignment[posCode]][posCode] = 0
          }
          serviceCount[assignment[posCode]][posCode]++
          serviceCount[assignment[posCode]].total++
        }
      }
    })
    
    // 再分配允許重複的崗位（不需要檢查重複）
    positionsWithoutDuplicateCheck.forEach(posCode => {
      const posData = positionConfig.value[posCode]
      if (!posData || !posData[dayType]) {
        return
      }
      
            // 只使用參與自動分配的人員
      const availablePersons = posData[dayType]
        .filter(p => {
            if (typeof p === 'object' && p.includeInAutoSchedule === false) {
              return false
            }
            return true
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
        // 選擇該崗位服務次數最少的（不需要檢查重複）
        availablePersons.sort((a, b) => {
          const countA = serviceCount[a.name] ? (serviceCount[a.name][posCode] || 0) : 0
          const countB = serviceCount[b.name] ? (serviceCount[b.name][posCode] || 0) : 0
          if (countA !== countB) return countA - countB
          // 如果次數相同，選擇總服務次數最少的
          const totalA = serviceCount[a.name] ? serviceCount[a.name].total : 0
          const totalB = serviceCount[b.name] ? serviceCount[b.name].total : 0
          return totalA - totalB
        })
        assignment[posCode] = availablePersons[0].name
        assignment[posCode + 'Ids'] = [availablePersons[0].id]
        
        // 更新服務次數
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

// 產生服事表
const generateSchedule = () => {
  if (!canGenerate.value) {
    showNotification('請確保每個崗位（電腦、混音、燈光、直播）至少配置一位人員，並選擇日期範圍', 'warning', 4000)
    return
  }
  
  const [startDate, endDate] = dateRange.value
  const weekendDates = getWeekendDates(startDate, endDate)
  
  if (weekendDates.length === 0) {
    showNotification('選擇的日期範圍內沒有週六或週日', 'warning', 3000)
    return
  }
  
  // 重置狀態
  editingScheduleYear.value = null
  isLoadedFromHistory.value = false
  currentScheduleYear.value = null
  
  // 將 initialSelectedPositions 複製到 selectedPositions（用於生成服事表）
  selectedPositions.value = JSON.parse(JSON.stringify(initialSelectedPositions.value))
  
  // 確保 selectedPositions 有正確的值（從用戶選擇的崗位）
  console.log('產生服事表前的 selectedPositions:', selectedPositions.value)
  console.log('產生服事表前的 positionConfig:', Object.keys(positionConfig.value))
  
  schedule.value = distributePersons(weekendDates)
  
  console.log('產生服事表後的 selectedPositions:', selectedPositions.value)
  console.log('產生服事表後的 positionConfig:', Object.keys(positionConfig.value))
  console.log('產生服事表後的 schedule 第一筆資料:', schedule.value[0])
  console.log('選中的崗位列表:', Object.keys(selectedPositions.value).filter(posCode => selectedPositions.value[posCode]))
  
  // 設置格式化日期
  schedule.value.forEach(item => {
    if (item.date && !item.formattedDate) {
      const dateObj = parseDate(item.date)
      if (dateObj) {
        const year = dateObj.getFullYear()
        const month = String(dateObj.getMonth() + 1).padStart(2, '0')
        const day = String(dateObj.getDate()).padStart(2, '0')
        const dayOfWeekChar = item.dayOfWeek === '六' ? '六' : '日'
        item.formattedDate = `${year}/${month}/${day}(${dayOfWeekChar})`
      } else {
        // 如果解析失敗，使用原始日期字符串
        item.formattedDate = item.date
      }
    }
  })
  
  // 產生服事表後自動進入編輯模式，方便用戶直接調整
  isEditing.value = true
  originalSchedule.value = JSON.parse(JSON.stringify(schedule.value || []))
  useRandomAssignment.value = false // 重置隨機分配選項
  
  showNotification('服事表已產生，您可以直接編輯調整', 'success', 3000)
}

// 驗證服事表（檢查崗位之間是否有重複，根據 allowDuplicate 欄位判斷）
const validateSchedule = () => {
  const errors = []
  
  schedule.value.forEach((item, index) => {
    // 根據 allowDuplicate 分類崗位
    const positionsWithDuplicateCheck = [] // 需要檢查重複的崗位
    const positionsWithoutDuplicateCheck = [] // 不需要檢查重複的崗位
    
    // 遍歷所有選中的崗位
    Object.keys(selectedPositions.value).forEach(posCode => {
      if (selectedPositions.value[posCode] && item[posCode + 'Ids'] && item[posCode + 'Ids'].length > 0) {
        const posData = positionConfig.value[posCode]
        if (posData && !posData.allowDuplicate) {
          positionsWithDuplicateCheck.push(posCode)
        } else {
          positionsWithoutDuplicateCheck.push(posCode)
        }
      }
    })
    
    // 檢查需要檢查重複的崗位之間是否有重複（使用 ID 比較）
    const usedPersonIds = new Set()
    positionsWithDuplicateCheck.forEach(posCode => {
      const personIds = item[posCode + 'Ids'] || []
      personIds.forEach(personId => {
        if (personId) {
          if (usedPersonIds.has(personId)) {
            const dateStr = item.formattedDate || item.date || `第 ${index + 1} 行`
            const personName = item[posCode] || '未知人員'
            const positionName = positionConfig.value[posCode]?.positionName || posCode
            const otherPositions = positionsWithDuplicateCheck
              .filter(p => {
                if (p === posCode) return false
                const otherPersonIds = item[p + 'Ids'] || []
                return otherPersonIds.includes(personId)
              })
              .map(p => positionConfig.value[p]?.positionName || p)
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

// 計算年度（從日期範圍的開始日期）
const calculateYear = () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    return null
  }
  const startDate = new Date(dateRange.value[0])
  return startDate.getFullYear()
}

// 檢查該年度是否已有服事表
const checkYearExists = async (year) => {
  try {
    const response = await apiRequest(`/church/service-schedules/year/${year}`, {
      method: 'GET'
    })
    return response.ok
  } catch (error) {
    // 如果查詢失敗（例如 404），表示該年度沒有服事表
    return false
  }
}

// 保存服事表
const saveSchedule = async () => {
  if (!schedule.value || schedule.value.length === 0 || !dateRange.value || dateRange.value.length !== 2) {
    showNotification('請先產生服事表', 'warning', 3000)
    return
  }

  // 計算年度並檢查是否已存在
  const year = calculateYear()
  if (!year) {
    showNotification('無法計算年度，請檢查日期範圍', 'warning', 3000)
    return
  }
  
  const yearExists = await checkYearExists(year)
  if (yearExists) {
    showNotification(`該年度（${year}年）已存在服事表，每個年度只能有一個版本。請先刪除或更新現有的服事表。`, 'error', 5000)
    return
  }

  // 驗證主要崗位之間是否有重複
  const validationErrors = validateSchedule()
  if (validationErrors.length > 0) {
    showNotification('驗證失敗：\n' + validationErrors.join('\n') + '\n\n請修正後再保存。', 'error', 5000)
    return
  }

  saving.value = true
  try {
    // 轉換為後端需要的格式：將 ID 轉換為人員名稱（用於向後兼容，但後端會改用 ID）
    // 只保存選中的崗位（新產生的服事表）
    const scheduleDataForBackend = schedule.value.map(item => {
      const result = {
        date: item.date
      }
      
      // 動態保存所有選中的崗位
      Object.keys(selectedPositions.value).forEach(posCode => {
        if (selectedPositions.value[posCode]) {
          result[posCode] = item[posCode] || ''
          result[posCode + 'Ids'] = item[posCode + 'Ids'] || []
        }
      })
      
      return result
    })

    const response = await apiRequest('/church/service-schedules', {
      method: 'POST',
      body: JSON.stringify({
        scheduleData: scheduleDataForBackend,
        dateRange: dateRange.value
      })
    }, '保存服事表中...')

    const result = await response.json()
    
    if (response.ok && result.success !== false) {
      showNotification('服事表保存成功！', 'success', 3000)
      currentScheduleYear.value = result.year
      editingScheduleYear.value = result.year // 設置編輯年度，這樣下次就可以使用「更新」按鈕
      // 標記為已保存
      isLoadedFromHistory.value = true
      // 保持編輯模式，讓用戶可以繼續調整
      // isEditing.value = false // 不退出編輯模式，讓用戶可以繼續調整
      await loadHistory() // 重新載入歷史記錄
    } else {
      const errorMsg = result.error || '未知錯誤'
      showNotification('保存失敗：' + errorMsg, 'error', 5000)
    }
  } catch (error) {
    const errorMsg = error.message || '未知錯誤'
    // 檢查是否為年度衝突錯誤
    if (errorMsg.includes('年度') || errorMsg.includes('year')) {
      showNotification(errorMsg, 'error', 5000)
    } else {
      showNotification('保存失敗：' + errorMsg, 'error', 4000)
    }
  } finally {
    saving.value = false
  }
}

// 載入歷史記錄
const loadHistory = async () => {
  try {
    const response = await apiRequest('/church/service-schedules', {
      method: 'GET'
    }, '載入歷史記錄中...')
    const result = await response.json()
    historyList.value = result || []
  } catch (error) {
    console.error('載入歷史記錄失敗：', error)
    showNotification('載入歷史記錄失敗：' + error.message, 'error', 3000)
  }
}

// 載入指定的服事表
const loadSchedule = async (year) => {
  try {
    const response = await apiRequest(`/church/service-schedules/${year}`, {
      method: 'GET'
    }, '載入服事表中...')
    const data = await response.json()
    
    if (response.ok) {
      // 載入年度
      currentScheduleYear.value = data.year
      
      // 載入日期範圍（如果有）
      if (data.startDate && data.endDate) {
      dateRange.value = [data.startDate, data.endDate]
          } else {
        dateRange.value = []
      }
      
      // 先載入崗位配置，以便從 ID 查找名稱（如果後端沒有返回名稱）
      await loadPositionConfig()
      
      // 載入服事表數據（從新的 dates 關聯表結構）
      if (data.scheduleData && Array.isArray(data.scheduleData) && data.scheduleData.length > 0) {
        schedule.value = data.scheduleData.map(item => {
          // 從人員 ID 查找名稱（用於顯示，如果後端沒有返回名稱）
          const getPersonNameByIdLocal = (personId, position, dayOfWeek) => {
            if (!personId) return ''
            const dayKey = dayOfWeek === '六' ? 'saturday' : 'sunday'
            const posData = positionConfig.value[position]
            const persons = (posData && posData[dayKey]) ? posData[dayKey] : []
            const person = persons.find(p => {
              if (typeof p === 'object') {
                return (p.personId || p.id) === personId
              }
              return false
            })
            return person && typeof person === 'object' ? (person.displayName || person.personName || '') : ''
          }
          
          const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
          
          // 動態處理所有崗位（從 positionConfig 讀取）
          const scheduleItem = {
            date: item.date,
            formattedDate: item.formattedDate || formatDisplayDate(item.date),
            dayOfWeek: dayOfWeek
          }
          
          // 處理後端返回的崗位資料
          // 後端返回的資料中，崗位欄位是動態的，直接使用
          for (const key in item) {
            if (key !== 'date' && key !== 'formattedDate' && key !== 'dayOfWeek') {
              scheduleItem[key] = item[key]
            }
          }
          
          // 確保崗位名稱欄位存在（如果只有 Ids 沒有名稱，從 positionConfig 查找）
          Object.keys(positionConfig.value).forEach(posCode => {
            const personIds = scheduleItem[posCode + 'Ids'] || []
            if (personIds.length > 0 && !scheduleItem[posCode]) {
              // 如果有 Ids 但沒有名稱，嘗試從 positionConfig 查找
              const personNames = []
              for (const personId of personIds) {
                const personName = getPersonNameByIdLocal(personId, posCode, dayOfWeek)
                if (personName) {
                  personNames.push(personName)
                }
              }
              if (personNames.length > 0) {
                scheduleItem[posCode] = personNames.join('/')
              }
            }
          })
          
          return scheduleItem
        })
          } else {
        // 如果沒有服事表數據，設置為空數組
        schedule.value = []
      }
      
      // 根據載入的資料推斷哪些崗位被選中（檢查 schedule 中哪些崗位有資料）
      const loadedPositions = new Set()
      schedule.value.forEach(item => {
        // 遍歷 item 的所有屬性，找出崗位相關的欄位
        for (const key in item) {
          // 如果 key 以 'Id' 結尾，且不是 'date'、'formattedDate'、'dayOfWeek' 等系統欄位
          if (key.endsWith('Id') && item[key]) {
            const posCode = key.replace('Id', '')
            // 檢查這個 posCode 是否在 positionConfig 中
            if (positionConfig.value[posCode]) {
              loadedPositions.add(posCode)
            }
          } else if (!key.endsWith('Id') && key !== 'date' && key !== 'formattedDate' && key !== 'dayOfWeek' && item[key]) {
            // 如果 key 不是系統欄位，且值不為空，可能是崗位名稱
            if (positionConfig.value[key]) {
              loadedPositions.add(key)
            }
          }
        }
      })
      
      // 只選中載入的崗位，其他崗位設為 false
      // 確保所有崗位都在 selectedPositions 中有對應的鍵
      // 使用新的對象來觸發 Vue 的響應式更新
      const newSelectedPositions = {}
      for (const posCode in positionConfig.value) {
        newSelectedPositions[posCode] = loadedPositions.has(posCode)
      }
      selectedPositions.value = newSelectedPositions
      
      console.log('載入後的 selectedPositions:', selectedPositions.value)
      console.log('載入後的 loadedPositions:', Array.from(loadedPositions))
      console.log('載入後的 positionConfig:', Object.keys(positionConfig.value))
      console.log('載入後的 schedule 第一筆資料:', schedule.value[0])
      console.log('選中的崗位列表:', Object.keys(selectedPositions.value).filter(posCode => selectedPositions.value[posCode] === true))
      
      // 退出編輯模式
      isEditing.value = false
      editingScheduleYear.value = null
      isLoadedFromHistory.value = true // 標記為從歷史記錄載入
      
      showNotification('服事表載入成功！', 'success', 3000)
    } else {
      showNotification('載入失敗：' + (data.error || '未知錯誤'), 'error', 3000)
    }
  } catch (error) {
    showNotification('載入失敗：' + error.message, 'error', 3000)
  }
}

// 編輯指定的服事表
const editSchedule = async (year) => {
  try {
    const response = await apiRequest(`/church/service-schedules/${year}`, {
      method: 'GET'
    }, '載入服事表中...')
    const data = await response.json()
    
    if (response.ok) {
      // 載入年度
      currentScheduleYear.value = data.year
      
      // 載入日期範圍（如果有）
      if (data.startDate && data.endDate) {
      dateRange.value = [data.startDate, data.endDate]
          } else {
        dateRange.value = []
          }
      
      // 先載入崗位配置，以便從名稱查找 ID
      await loadPositionConfig()
      
      // 載入服事表數據（從新的 dates 關聯表結構）
      if (data.scheduleData && Array.isArray(data.scheduleData) && data.scheduleData.length > 0) {
        schedule.value = data.scheduleData.map(item => {
          // 從人員 ID 查找名稱（用於顯示，如果後端沒有返回名稱）
          const getPersonNameById = (personId, position, dayOfWeek) => {
            if (!personId) return ''
            const dayKey = dayOfWeek === '六' ? 'saturday' : 'sunday'
            const posData = positionConfig.value[position]
            const persons = (posData && posData[dayKey]) ? posData[dayKey] : []
            const person = persons.find(p => {
              if (typeof p === 'object') {
                return (p.personId || p.id) === personId
              }
              return false
            })
            return person && typeof person === 'object' ? (person.displayName || person.personName || '') : ''
          }
          
          const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
          
          // 動態處理所有崗位（從 positionConfig 讀取）
          const scheduleItem = {
            date: item.date,
            formattedDate: item.formattedDate || formatDisplayDate(item.date),
            dayOfWeek: dayOfWeek
          }
          
          // 只處理後端返回的崗位資料（不遍歷所有崗位配置，避免添加空欄位）
          // 後端返回的資料中，崗位欄位是動態的，直接使用
          for (const key in item) {
            if (key !== 'date' && key !== 'formattedDate' && key !== 'dayOfWeek') {
              scheduleItem[key] = item[key]
            }
          }
          
          return scheduleItem
        })
          } else {
        // 如果沒有服事表數據，設置為空數組
        schedule.value = []
      }
      
      // 根據載入的資料推斷哪些崗位被選中（檢查 schedule 中哪些崗位有資料）
      const loadedPositions = new Set()
      schedule.value.forEach(item => {
        // 遍歷 item 的所有屬性，找出崗位相關的欄位
        for (const key in item) {
          // 如果 key 以 'Id' 結尾，且不是 'date'、'formattedDate'、'dayOfWeek' 等系統欄位
          if (key.endsWith('Id') && item[key]) {
            const posCode = key.replace('Id', '')
            // 檢查這個 posCode 是否在 positionConfig 中
            if (positionConfig.value[posCode]) {
              loadedPositions.add(posCode)
            }
          } else if (!key.endsWith('Id') && key !== 'date' && key !== 'formattedDate' && key !== 'dayOfWeek' && item[key]) {
            // 如果 key 不是系統欄位，且值不為空，可能是崗位名稱
            if (positionConfig.value[key]) {
              loadedPositions.add(key)
            }
          }
        }
      })
      
      // 只選中載入的崗位，其他崗位設為 false
      // 確保所有崗位都在 selectedPositions 中有對應的鍵
      // 使用新的對象來觸發 Vue 的響應式更新
      const newSelectedPositions = {}
      for (const posCode in positionConfig.value) {
        newSelectedPositions[posCode] = loadedPositions.has(posCode)
      }
      selectedPositions.value = newSelectedPositions
      
      console.log('編輯模式載入後的 selectedPositions:', selectedPositions.value)
      console.log('編輯模式載入後的 loadedPositions:', Array.from(loadedPositions))
      console.log('編輯模式載入後的 positionConfig:', Object.keys(positionConfig.value))
      console.log('編輯模式載入後的 schedule 第一筆資料:', schedule.value[0])
      console.log('選中的崗位列表:', Object.keys(selectedPositions.value).filter(posCode => selectedPositions.value[posCode] === true))
      
      // 進入編輯模式
      isEditing.value = true
      editingScheduleYear.value = year
      currentScheduleYear.value = year
      isLoadedFromHistory.value = true // 標記為從歷史記錄載入（編輯模式）
      useRandomAssignment.value = false // 重置隨機分配選項
      
      // 深拷貝原始數據，用於取消編輯
      originalSchedule.value = JSON.parse(JSON.stringify(schedule.value || []))
      
      showNotification('已進入編輯模式，您可以修改人員安排', 'info', 3000)
    } else {
      showNotification('載入失敗', 'error', 3000)
    }
  } catch (error) {
    showNotification('載入失敗：' + error.message, 'error', 3000)
  }
}

// 取消編輯
const cancelEdit = () => {
  if (confirm('確定要取消編輯嗎？未保存的修改將丟失。')) {
    // 如果有原始數據，恢復原始數據
    if (originalSchedule.value.length > 0) {
      schedule.value = JSON.parse(JSON.stringify(originalSchedule.value))
    }
    
    // 如果從歷史記錄載入，重新載入
    if (currentScheduleYear.value) {
      // 重新載入以獲取原始資料
      loadSchedule(currentScheduleYear.value).then(() => {
        isEditing.value = false
        editingScheduleYear.value = null
        originalSchedule.value = []
      })
    } else {
    // 清空所有資料
    schedule.value = []
    dateRange.value = []
      currentScheduleYear.value = null
    isEditing.value = false
    editingScheduleYear.value = null
    originalSchedule.value = []
    isLoadedFromHistory.value = false
    useRandomAssignment.value = false
    selectedPositions.value = {}
    initialSelectedPositions.value = {}
      showNotification('已取消編輯，資料已清空。', 'info', 3000)
  }
  }
}

// 根據 ID 獲取人員名稱（用於保存時轉換）
const getPersonNameById = (personId, position, item) => {
  if (!personId) return ''
  
  // 判斷是週六還是週日
  let dayOfWeek = item.dayOfWeek
  if (!dayOfWeek && item.date) {
    const dateObj = parseDate(item.date)
    if (dateObj) {
      const day = dateObj.getDay()
      dayOfWeek = day === 6 ? '六' : '日'
    }
  }
  if (!dayOfWeek) dayOfWeek = '六'
  
  const dayKey = dayOfWeek === '六' ? 'saturday' : 'sunday'
  const posData = positionConfig.value[position]
  const persons = (posData && posData[dayKey]) ? posData[dayKey] : []
  
  const person = persons.find(p => {
    if (typeof p === 'object') {
      return (p.personId || p.id) === personId
    }
    return false
  })
  
  if (person) {
    return person.displayName || person.personName || ''
  }
  return ''
}

// 處理崗位選擇變化（編輯模式下新增崗位時使用）
const handlePositionSelectionChange = (posCode) => {
  // 只在編輯模式下處理
  if (!isEditing.value) return
  
  const isSelected = selectedPositions.value[posCode]
  
  if (isSelected) {
    // 如果勾選了新崗位，為所有日期添加該崗位的欄位
    schedule.value.forEach(item => {
      if (!item.hasOwnProperty(posCode)) {
        item[posCode] = ''
        item[posCode + 'Ids'] = []
      }
    })
    
    // 如果該崗位有配置人員，自動為每一日分配人員
    if (hasPositionConfig(posCode) && schedule.value.length > 0) {
      // 使用自動分派邏輯為該崗位分配人員
      autoAssignPositionForNewSelection(posCode)
    }
  } else {
    // 如果取消勾選崗位，移除該崗位的資料（但保留欄位，只是清空）
    schedule.value.forEach(item => {
      item[posCode] = ''
      item[posCode + 'Ids'] = []
    })
  }
}

// 為新選擇的崗位自動分配人員（編輯模式下使用，類似 autoAssignPosition 但只處理空欄位）
const autoAssignPositionForNewSelection = (posCode) => {
  const posData = positionConfig.value[posCode]
  if (!posData) {
    return
  }
  
  // 統計該崗位在現有服事表中的服務次數（只統計已有分配的部分）
  const serviceCount = {}
  schedule.value.forEach(item => {
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
  
  // 獲取該崗位是否允許重複
  const allowDuplicate = posData.allowDuplicate === true
  
  // 為每個日期分配人員（只處理空欄位）
  schedule.value.forEach(item => {
    // 如果該日期已經有人員，跳過
    if (item[posCode + 'Ids'] && item[posCode + 'Ids'].length > 0) {
      return
    }
    
    const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
    const dayType = dayOfWeek === '六' ? 'saturday' : 'sunday'
    const dayKey = dayType
    
    // 獲取該崗位該日期類型的人員列表
    const availablePersons = (posData[dayKey] || [])
      .filter(p => {
        // 只使用參與自動分配的人員
        if (typeof p === 'object' && p.includeInAutoSchedule === false) {
          return false
        }
        return true
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
    
    if (availablePersons.length === 0) {
      return // 沒有可用人員
    }
    
    // 根據 allowDuplicate 判斷是否需要過濾已使用的人員
    let filteredPersons = availablePersons
    if (!allowDuplicate) {
      // 如果不允許重複，過濾掉今天所有其他崗位已使用的人員
      const usedPersonIds = new Set()
      Object.keys(selectedPositions.value).forEach(otherPosCode => {
        if (selectedPositions.value[otherPosCode] && otherPosCode !== posCode) {
          const otherPersonIds = item[otherPosCode + 'Ids'] || []
          otherPersonIds.forEach(id => {
            if (id) {
              usedPersonIds.add(id)
            }
          })
        }
      })
      filteredPersons = availablePersons.filter(p => !usedPersonIds.has(p.id))
    }
    // 如果 allowDuplicate === true，則不過濾，可以使用所有人員（包括已經在其他崗位的人員）
    
    // 如果過濾後沒有可用人員，使用全部人員
    if (filteredPersons.length === 0) {
      filteredPersons = availablePersons
    }
    
    let selectedPerson
    if (useRandomAssignment.value) {
      // 完全隨機分配：不考慮服務次數
      const randomIndex = Math.floor(Math.random() * filteredPersons.length)
      selectedPerson = filteredPersons[randomIndex]
    } else {
      // 根據服務次數排序，找出服務次數最少的組
      filteredPersons.sort((a, b) => {
        const countA = serviceCount[a.name] ? serviceCount[a.name].count : 0
        const countB = serviceCount[b.name] ? serviceCount[b.name].count : 0
        return countA - countB
      })
      
      // 找出服務次數最少的組（可能有多個人服務次數相同）
      const minCount = filteredPersons.length > 0 
        ? (serviceCount[filteredPersons[0].name] ? serviceCount[filteredPersons[0].name].count : 0)
        : 0
      const minCountGroup = filteredPersons.filter(p => {
        const count = serviceCount[p.name] ? serviceCount[p.name].count : 0
        return count === minCount
      })
      
      // 在服務次數最少的組中隨機選擇
      const randomIndex = Math.floor(Math.random() * minCountGroup.length)
      selectedPerson = minCountGroup[randomIndex]
    }
    
    if (selectedPerson) {
      item[posCode] = selectedPerson.name
      item[posCode + 'Ids'] = [selectedPerson.id]
      
      // 更新服務次數統計（即使使用完全隨機，也更新統計以便後續使用）
      if (!serviceCount[selectedPerson.name]) {
        serviceCount[selectedPerson.name] = { count: 0, id: selectedPerson.id }
      }
      serviceCount[selectedPerson.name].count++
    }
  })
  
  showNotification(`已為崗位「${posData.positionName || posCode}」重新隨機分配人員`, 'success', 3000)
}

// 自動分派指定崗位的人員（編輯模式下使用）
const autoAssignPosition = (posCode) => {
  if (!isEditing.value || !schedule.value || schedule.value.length === 0) {
    showNotification('請先進入編輯模式並載入服事表', 'warning', 3000)
    return
  }
  
  const posData = positionConfig.value[posCode]
  if (!posData) {
    showNotification('崗位配置不存在', 'error', 3000)
    return
  }
  
  // 檢查該崗位是否有配置人員
  if (!hasPositionConfig(posCode)) {
    showNotification(`崗位「${posData.positionName || posCode}」尚未配置人員`, 'warning', 3000)
    return
  }
  
  // 先清除該崗位在所有日期的現有人員（以便重新分配）
  schedule.value.forEach(item => {
    item[posCode] = ''
    item[posCode + 'Ids'] = []
  })
  
  // 統計該崗位在現有服事表中的服務次數（重新分配後，該崗位的服務次數應該為 0）
  // 但我們需要統計其他崗位的服務次數，以便在重新分配時避免重複（如果不允許重複）
  const serviceCount = {}
  schedule.value.forEach(item => {
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
  
  // 獲取該崗位是否允許重複
  const allowDuplicate = posData.allowDuplicate === true
  
  // 為每個日期重新分配人員（「重新隨機分配」會重新分配所有日期）
  schedule.value.forEach(item => {
    
    const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
    const dayType = dayOfWeek === '六' ? 'saturday' : 'sunday'
    const dayKey = dayType
    
    // 獲取該崗位該日期類型的人員列表
    const availablePersons = (posData[dayKey] || [])
      .filter(p => {
        // 只使用參與自動分配的人員
        if (typeof p === 'object' && p.includeInAutoSchedule === false) {
          return false
        }
        return true
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
    
    if (availablePersons.length === 0) {
      return // 沒有可用人員
    }
    
    // 根據 allowDuplicate 判斷是否需要過濾已使用的人員
    let filteredPersons = availablePersons
    if (!allowDuplicate) {
      // 如果不允許重複，過濾掉今天所有其他崗位已使用的人員
      const usedPersonIds = new Set()
      Object.keys(selectedPositions.value).forEach(otherPosCode => {
        if (selectedPositions.value[otherPosCode] && otherPosCode !== posCode) {
          const otherPersonIds = item[otherPosCode + 'Ids'] || []
          otherPersonIds.forEach(id => {
            if (id) {
              usedPersonIds.add(id)
            }
          })
        }
      })
      filteredPersons = availablePersons.filter(p => !usedPersonIds.has(p.id))
    }
    // 如果 allowDuplicate === true，則不過濾，可以使用所有人員（包括已經在其他崗位的人員）
    
    // 如果過濾後沒有可用人員，使用全部人員
    if (filteredPersons.length === 0) {
      filteredPersons = availablePersons
    }
    
    let selectedPerson
    if (useRandomAssignment.value) {
      // 完全隨機分配：不考慮服務次數
      const randomIndex = Math.floor(Math.random() * filteredPersons.length)
      selectedPerson = filteredPersons[randomIndex]
    } else {
      // 根據服務次數排序，找出服務次數最少的組
      filteredPersons.sort((a, b) => {
        const countA = serviceCount[a.name] ? serviceCount[a.name].count : 0
        const countB = serviceCount[b.name] ? serviceCount[b.name].count : 0
        return countA - countB
      })
      
      // 找出服務次數最少的組（可能有多個人服務次數相同）
      const minCount = filteredPersons.length > 0 
        ? (serviceCount[filteredPersons[0].name] ? serviceCount[filteredPersons[0].name].count : 0)
        : 0
      const minCountGroup = filteredPersons.filter(p => {
        const count = serviceCount[p.name] ? serviceCount[p.name].count : 0
        return count === minCount
      })
      
      // 在服務次數最少的組中隨機選擇
      const randomIndex = Math.floor(Math.random() * minCountGroup.length)
      selectedPerson = minCountGroup[randomIndex]
    }
    
    if (selectedPerson) {
      item[posCode] = selectedPerson.name
      item[posCode + 'Ids'] = [selectedPerson.id]
      
      // 更新服務次數統計（即使使用完全隨機，也更新統計以便後續使用）
      if (!serviceCount[selectedPerson.name]) {
        serviceCount[selectedPerson.name] = { count: 0, id: selectedPerson.id }
      }
      serviceCount[selectedPerson.name].count++
    }
  })
  
  showNotification(`已重新隨機分配崗位「${posData.positionName || posCode}」的人員`, 'success', 3000)
}

// 獲取可選人員列表
// 注意：此函數從 positionConfig 中讀取人員列表
// positionConfig 會在以下情況自動更新：
// 1. 頁面載入時（onMounted）
// 2. 進入編輯模式時（editSchedule）
// 3. 關閉崗位管理視窗時（closePositionManagement）
// 由於 Vue 的響應式系統，下拉選單會自動顯示最新的人員列表
const getAvailablePersons = (item, position) => {
  // 判斷是週六還是週日
  let dayOfWeek = item.dayOfWeek
  
  // 如果沒有 dayOfWeek，從日期判斷
  if (!dayOfWeek && item.date) {
    const dateObj = parseDate(item.date)
    if (dateObj) {
      const day = dateObj.getDay() // 0 = 週日, 6 = 週六
      dayOfWeek = day === 6 ? '六' : '日'
    }
  }
  
  // 默認為週六
  if (!dayOfWeek) {
    dayOfWeek = '六'
  }
  
  const dayKey = dayOfWeek === '六' ? 'saturday' : 'sunday'
  
  // 獲取該崗位該日期的人員列表
  // 編輯模式下顯示所有人員（包括不參與自動分配的）
  const posData = positionConfig.value[position]
  let availablePersons = (posData && posData[dayKey]) ? posData[dayKey] : []
  
  // 將對象轉換為包含 id 和 name 的對象（用於下拉選單顯示）
  availablePersons = availablePersons.map(p => {
    if (typeof p === 'string') {
      // 如果是字符串（舊格式），嘗試從所有人員中查找 ID
      // 這種情況應該很少見，但為了兼容性保留
      return { id: null, name: p }
    } else {
      // 新格式：包含 personId 的對象
      const personId = p.personId || p.id
      const personName = p.displayName || p.personName || ''
      return { id: personId, name: personName }
    }
  }).filter(p => p.id && p.name) // 過濾空值，確保有 ID 和名稱
  
  // 根據 allowDuplicate 欄位判斷是否需要檢查重複
  const allowDuplicate = posData && posData.allowDuplicate
  
  if (!allowDuplicate) {
    // 對於不允許重複的崗位，過濾掉今天其他不允許重複的崗位已分配的人員（使用 ID 比較）
    const usedPersonIds = new Set()
    // 遍歷所有選中的崗位，找出其他不允許重複的崗位
    Object.keys(selectedPositions.value).forEach(posCode => {
      if (selectedPositions.value[posCode] && posCode !== position) {
        const otherPosData = positionConfig.value[posCode]
        if (otherPosData && !otherPosData.allowDuplicate) {
          const idsKey = posCode + 'Ids'
          const personIds = item[idsKey] || []
          personIds.forEach(id => {
            if (id) {
              usedPersonIds.add(id)
            }
          })
        }
      }
    })
    availablePersons = availablePersons.filter(p => !usedPersonIds.has(p.id))
  }
  // 對於允許重複的崗位，不需要過濾，可以選擇所有人員
  
  return availablePersons
}

// 更新服事表
const updateSchedule = async () => {
  if (!editingScheduleYear.value) {
    showNotification('請先載入要編輯的服事表', 'warning', 3000)
    return
  }

  // 計算新的年度並檢查（如果年度改變了）
  const newYear = calculateYear()
  if (!newYear) {
    showNotification('無法計算年度，請檢查日期範圍', 'warning', 3000)
    return
  }
  
  if (newYear !== editingScheduleYear.value) {
    // 如果年度改變了，檢查新年度是否已有服事表
    const yearExists = await checkYearExists(newYear)
    if (yearExists) {
      showNotification(`該年度（${newYear}年）已存在服事表，每個年度只能有一個版本。請先刪除或更新現有的服事表。`, 'error', 5000)
      return
    }
  }

  // 如果有服事表數據，驗證主要崗位之間是否有重複
  if (schedule.value && schedule.value.length > 0) {
  const validationErrors = validateSchedule()
  if (validationErrors.length > 0) {
      showNotification('驗證失敗：\n' + validationErrors.join('\n') + '\n\n請修正後再保存。', 'error', 5000)
    return
  }

  // 驗證是否有空值（使用 ID 欄位，只檢查選中的崗位）
  const hasEmpty = schedule.value.some(item => {
    // 檢查所有選中的崗位是否有空值
    return Object.keys(selectedPositions.value).some(posCode => {
      if (selectedPositions.value[posCode]) {
        return !(item[posCode + 'Ids'] && item[posCode + 'Ids'].length > 0)
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
    // 轉換為後端需要的格式：將 ID 轉換為人員名稱（用於向後兼容，但後端會改用 ID）
    // 更新時保存所有選中的崗位（動態處理）
    const scheduleDataForBackend = schedule.value.map(item => {
      const result = {
        date: item.date
      }
      
      // 動態保存所有選中的崗位
      Object.keys(selectedPositions.value).forEach(posCode => {
        if (selectedPositions.value[posCode]) {
          result[posCode] = item[posCode] || ''
          result[posCode + 'Ids'] = item[posCode + 'Ids'] || []
        }
      })
      
      return result
    })

    const response = await apiRequest(`/church/service-schedules/${editingScheduleYear.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        scheduleData: scheduleDataForBackend,
        dateRange: dateRange.value
      })
    }, '更新服事表中...')

    const result = await response.json()
    
    if (response.ok) {
      showNotification('服事表更新成功！', 'success', 3000)
      // 如果年度改變了，更新當前年度
      if (result.year && result.year !== editingScheduleYear.value) {
        currentScheduleYear.value = result.year
        editingScheduleYear.value = result.year
      }
      isEditing.value = false
      editingScheduleYear.value = null
      originalSchedule.value = []
      isLoadedFromHistory.value = true // 更新後仍然是從歷史記錄載入的狀態
      await loadHistory() // 重新載入歷史記錄
    } else {
      const errorMsg = result.error || '未知錯誤'
      showNotification('更新失敗：' + errorMsg, 'error', 5000)
    }
  } catch (error) {
    const errorMsg = error.message || '未知錯誤'
    // 檢查是否為年度衝突錯誤
    if (errorMsg.includes('年度') || errorMsg.includes('year')) {
      showNotification(errorMsg, 'error', 5000)
    } else {
      showNotification('更新失敗：' + errorMsg, 'error', 4000)
    }
  } finally {
    saving.value = false
  }
}

// 啟用名稱編輯
const enableNameEdit = () => {
  isEditing.value = true
  editingScheduleId.value = currentScheduleId.value
}

// 啟用編輯模式（從載入的服事表）
const enableEdit = () => {
  if (currentScheduleYear.value) {
    editingScheduleYear.value = currentScheduleYear.value
    isEditing.value = true
    useRandomAssignment.value = false // 重置隨機分配選項
  }
}

// 格式化日期時間
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}/${month}/${day} ${hours}:${minutes}`
}


// 安全地解析日期字符串
const parseDate = (dateStr) => {
  if (!dateStr) return null
  
  // 如果已經是 Date 對象，直接返回
  if (dateStr instanceof Date) {
    return isNaN(dateStr.getTime()) ? null : dateStr
  }
  
  // 嘗試解析日期字符串
  let date = new Date(dateStr)
  
  // 如果解析失敗，嘗試手動解析常見格式
  if (isNaN(date.getTime())) {
    // 嘗試解析 YYYY-MM-DD 格式
    const match = String(dateStr).match(/^(\d{4})-(\d{2})-(\d{2})/)
    if (match) {
      date = new Date(parseInt(match[1]), parseInt(match[2]) - 1, parseInt(match[3]))
    } else {
      // 嘗試解析 YYYY/MM/DD 格式
      const match2 = String(dateStr).match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})/)
      if (match2) {
        date = new Date(parseInt(match2[1]), parseInt(match2[2]) - 1, parseInt(match2[3]))
      }
    }
  }
  
  return isNaN(date.getTime()) ? null : date
}

// 格式化顯示日期
const formatDisplayDate = (dateStr) => {
  if (!dateStr) return ''
  const date = parseDate(dateStr)
  if (!date) return dateStr // 如果解析失敗，返回原始字符串
  
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${year}/${month}/${day}`
}

// 匯出服事表
const exportSchedule = () => {
  if (!schedule.value || schedule.value.length === 0) return
  
  // 動態生成 CSV 標題（根據選中的崗位）
  const selectedPositionsList = Object.keys(selectedPositions.value).filter(
    posCode => selectedPositions.value[posCode] === true
  )
  
  // 生成標題行
  const headers = ['日期', ...selectedPositionsList.map(posCode => {
    const posData = positionConfig.value[posCode]
    return posData?.positionName || posCode
  })]
  let csv = headers.join(',') + '\n'
  
  // 生成資料行
  schedule.value.forEach(item => {
    // 使用格式化日期（包含星期），如果沒有則從 date 生成
    let dateStr = item.formattedDate
    if (!dateStr && item.date) {
      dateStr = formatDisplayDate(item.date)
      // 如果 formatDisplayDate 沒有包含星期，手動添加
      if (dateStr && !dateStr.includes('(')) {
        const dayOfWeek = item.dayOfWeek || (item.date ? (parseDate(item.date)?.getDay() === 6 ? '六' : '日') : '六')
        dateStr = `${dateStr}(${dayOfWeek})`
      }
    }
    const row = [dateStr || item.date || '']
    selectedPositionsList.forEach(posCode => {
      row.push(item[posCode] || '')
    })
    csv += row.join(',') + '\n'
  })
  
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  const [startDate, endDate] = dateRange.value
  link.setAttribute('download', `服事表_${startDate}_${endDate}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 載入崗位配置
// 載入崗位配置（從新的 API）
const loadPositionConfig = async () => {
  try {
    const response = await apiRequest('/church/positions/config/full', {
      method: 'GET'
    }, '載入崗位配置中...')
    const result = await response.json()
    console.log('載入崗位配置響應：', result)
      
    if (response.ok && result.config) {
      if (Object.keys(result.config).length > 0) {
        // 轉換為舊格式以保持兼容性
        const config = result.config
        const convertedConfig = {}
        
        // 先將崗位按 sortOrder 排序
        const sortedEntries = Object.entries(config).sort((a, b) => {
          const sortOrderA = a[1].sortOrder || 0
          const sortOrderB = b[1].sortOrder || 0
          return sortOrderA - sortOrderB
        })
        
        for (const [posCode, posData] of sortedEntries) {
          // 保留完整對象信息（包括 includeInAutoSchedule, allowDuplicate），用於產生服事表時過濾
          convertedConfig[posCode] = {
            positionName: posData.positionName || posCode,
            allowDuplicate: posData.allowDuplicate || false,
            saturday: posData.saturday || [],
            sunday: posData.sunday || []
          }
        }
        
        // 先設置 positionConfig，這樣 hasPositionConfig 才能正確工作
        positionConfig.value = convertedConfig
        
        // 初始化 initialSelectedPositions（用於新增服事表時的崗位選擇）
        // 注意：如果 initialSelectedPositions 已經有值（例如用戶已經選擇了崗位），則保留現有值
        const newInitialSelectedPositions = { ...initialSelectedPositions.value }
        for (const posCode in convertedConfig) {
          if (!newInitialSelectedPositions.hasOwnProperty(posCode)) {
            // 檢查該崗位是否有配置人員
            const hasConfig = hasPositionConfig(posCode)
            newInitialSelectedPositions[posCode] = hasConfig
          }
        }
        
        // 確保所有崗位都在 initialSelectedPositions 中有對應的鍵（即使是 false）
        for (const posCode in convertedConfig) {
          if (!newInitialSelectedPositions.hasOwnProperty(posCode)) {
            newInitialSelectedPositions[posCode] = false
          }
        }
        initialSelectedPositions.value = newInitialSelectedPositions
        
        // 初始化 selectedPositions（用於編輯模式下的崗位選擇）
        // 注意：如果 selectedPositions 已經有值（例如載入服事表時已設置），則保留現有值
        const newSelectedPositions = { ...selectedPositions.value }
        for (const posCode in convertedConfig) {
          if (!newSelectedPositions.hasOwnProperty(posCode)) {
            newSelectedPositions[posCode] = false
          }
        }
        selectedPositions.value = newSelectedPositions
        
        console.log('崗位配置載入成功：', positionConfig.value)
        console.log('初始化後的 initialSelectedPositions:', initialSelectedPositions.value)
        console.log('初始化後的 selectedPositions:', selectedPositions.value)
      } else {
        console.warn('崗位配置為空')
        positionConfig.value = {}
      }
    } else {
      console.error('載入崗位配置失敗，HTTP 狀態：', response.status, result)
      positionConfig.value = {}
    }
  } catch (error) {
    console.error('載入崗位配置失敗：', error)
    positionConfig.value = {}
  }
}

// 計算本週六、週日的日期
const getCurrentWeekDates = () => {
  const today = new Date()
  const currentDay = today.getDay() // 0 = 週日, 6 = 週六
  
  // 計算本週六的日期
  // 如果今天是週日，本週六是昨天（上週六）
  // 如果今天是週六，本週六是今天
  // 如果今天是週一到週五，本週六是本週的週六
  let daysUntilSaturday
  if (currentDay === 0) {
    // 今天是週日，本週六是昨天
    daysUntilSaturday = -1
  } else {
    // 今天是週一到週六，計算到本週六的天數
    daysUntilSaturday = 6 - currentDay
  }
  const saturdayDate = new Date(today)
  saturdayDate.setDate(today.getDate() + daysUntilSaturday)
  
  // 計算本週日的日期
  // 如果今天是週日，本週日是今天
  // 如果今天是週六，本週日是明天
  // 如果今天是週一到週五，本週日是本週的週日
  let daysUntilSunday
  if (currentDay === 0) {
    // 今天是週日，本週日是今天
    daysUntilSunday = 0
  } else {
    // 今天是週一到週六，計算到本週日的天數
    daysUntilSunday = 7 - currentDay
  }
  const sundayDate = new Date(today)
  sundayDate.setDate(today.getDate() + daysUntilSunday)
  
  return {
    saturday: formatDateISO(saturdayDate),
    sunday: formatDateISO(sundayDate)
  }
}

// 格式化週日期顯示
const formatWeekDate = (dateStr) => {
  if (!dateStr) return ''
  const date = parseDate(dateStr)
  if (!date) return dateStr
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}/${day}`
}

// 獲取本週某日期的某崗位人員
const getCurrentWeekPerson = (scheduleItem, posCode) => {
  if (!scheduleItem) return null
  // 直接從 scheduleItem 中讀取崗位人員
  // 後端返回的資料結構中，崗位代碼就是 key
  // 檢查所有可能的 key 格式（包括大小寫變化）
  const personName = scheduleItem[posCode] || 
                     scheduleItem[posCode.toLowerCase()] || 
                     scheduleItem[posCode.toUpperCase()]
  
  // 如果沒有找到名稱，嘗試檢查是否有 Ids 陣列（表示有分配但名稱可能為空）
  const personIds = scheduleItem[posCode + 'Ids'] || 
                    scheduleItem[posCode.toLowerCase() + 'Ids'] || 
                    scheduleItem[posCode.toUpperCase() + 'Ids']
  
  // 檢查後端返回的名稱是否完整（通過比較名稱數量與 ID 數量）
  const nameCount = personName && typeof personName === 'string' && personName.trim() !== '' 
    ? personName.split('/').filter(n => n.trim() !== '').length 
    : 0
  const idCount = personIds && Array.isArray(personIds) ? personIds.length : 0
  
  // 如果有名稱：
  // - 有 ID 時：名稱數量需與 ID 數量一致（確保不缺漏）
  // - 沒有 ID 時：直接採用名稱（許多後端只回傳名稱字串）
  if (personName && typeof personName === 'string' && personName.trim() !== '') {
    if (idCount === 0 || nameCount === idCount) {
      return personName
    }
  }
  
  // 如果有 Ids 陣列但名稱不完整或不存在，嘗試從 positionConfig 中查找
  if (personIds && Array.isArray(personIds) && personIds.length > 0) {
    const dayOfWeek = scheduleItem.dayOfWeek || (scheduleItem.date ? (parseDate(scheduleItem.date)?.getDay() === 6 ? '六' : '日') : '六')
    const dayKey = dayOfWeek === '六' ? 'saturday' : 'sunday'
    const posData = positionConfig.value[posCode]
    if (posData && posData[dayKey]) {
      const personNames = []
      for (const personId of personIds) {
        const person = posData[dayKey].find(p => {
          if (typeof p === 'object') {
            const pId = p.personId || p.id
            // 使用寬鬆比較，處理數字和字符串類型的 ID
            return pId != null && (
              pId === personId || 
              String(pId) === String(personId) || 
              Number(pId) === Number(personId)
            )
          }
          return false
        })
        if (person && typeof person === 'object') {
          const name = person.displayName || person.personName || ''
          if (name) {
            personNames.push(name)
          }
        }
      }
      if (personNames.length > 0) {
        return personNames.join('/')
      }
    }
  }
  
  return null
}

// 載入本週服事人員資料
const loadCurrentWeekSchedule = async () => {
  loadingCurrentWeek.value = true
  try {
    // 重置
    currentWeekSchedule.value = { saturday: null, sunday: null }
    
    // 獲取本週六、週日的日期
    const weekDates = getCurrentWeekDates()
    
    // 獲取當前年份
    const currentYear = new Date().getFullYear()
    
    // 載入當年的服事表
    try {
      const response = await apiRequest(`/church/service-schedules/${currentYear}`, {
        method: 'GET'
      }, '載入本週服事人員中...')
      
      if (response.ok) {
        const data = await response.json()
        
        if (data.scheduleData && Array.isArray(data.scheduleData)) {
          // 找出本週六、週日的資料
          const saturdayData = data.scheduleData.find(item => item.date === weekDates.saturday)
          const sundayData = data.scheduleData.find(item => item.date === weekDates.sunday)
          
          // 調試信息：檢查找到的資料
          if (saturdayData) {
            console.log('找到週六資料：', saturdayData)
            console.log('週六資料的 keys：', Object.keys(saturdayData))
          }
          if (sundayData) {
            console.log('找到週日資料：', sundayData)
            console.log('週日資料的 keys：', Object.keys(sundayData))
          }
          
          currentWeekSchedule.value = {
            saturday: saturdayData || null,
            sunday: sundayData || null
          }
        }
      }
    } catch (error) {
      // 如果當年沒有服事表，currentWeekSchedule 保持為空
      console.log('本週無服事表資料或載入失敗：', error.message)
    }
  } catch (error) {
    console.error('載入本週服事人員失敗：', error)
  } finally {
    loadingCurrentWeek.value = false
  }
}

// 組件掛載時載入配置和歷史記錄
onMounted(async () => {
  _updateMobile()
  try {
    if (typeof window !== 'undefined') {
      _mq = window.matchMedia('(max-width: 768px)')
      _mq.addEventListener?.('change', _updateMobile)
      // Safari < 14
      _mq.addListener?.(_updateMobile)
    }
  } catch {}

  // Default accordion state: open on desktop, calm on mobile
  dayOpen.value = { sat: !isMobile.value, sun: !isMobile.value }

  await loadPositionConfig()
  loadHistory()
  // 確保崗位配置載入完成後再載入本週服事人員
  loadCurrentWeekSchedule()
})

onBeforeUnmount(() => {
  try {
    _mq?.removeEventListener?.('change', _updateMobile)
    _mq?.removeListener?.(_updateMobile)
  } catch {}
})
</script>

<style scoped>
.service-schedule {}

/* Layout */
.two-col{
  display:grid;
  gap: 18px;
}
@media (min-width: 980px){
  .two-col{ grid-template-columns: 1.25fr 1fr; align-items: start; }
}

.card--stack{
  display:flex;
  flex-direction:column;
  gap: 14px;
}

/* Header row */
.card-head{
  display:flex;
  align-items:flex-start;
  justify-content:space-between;
  gap: 12px;
}
.pill{
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--surface-2);
  color: var(--muted);
  white-space: nowrap;
}

/* Week schedule */
.week-grid{
  display:grid;
  gap: 12px;
}
@media (min-width: 980px){
  .week-grid{ grid-template-columns: 1fr 1fr; }
}

.day-card{
  border: 1px solid var(--border);
  background: var(--surface-2);
  border-radius: calc(var(--radius) - 6px);
  padding: 14px;
}
.day-head{
  display:flex;
  align-items:baseline;
  justify-content:space-between;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}
.day-title{ font-weight: 700; }
.day-date{ color: var(--muted); font-size: 13px; }

.day-left{
  display:flex;
  flex-direction:column;
  gap: 2px;
}
.day-right{
  display:flex;
  align-items:center;
  gap: 10px;
}
.day-kpi{
  font-size: 12px;
  white-space: nowrap;
}
.acc-toggle.btn{
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
}
.acc-body{ padding-top: 8px; }

.acc-enter-active, .acc-leave-active{
  transition: opacity .22s ease, transform .22s ease;
}
.acc-enter-from, .acc-leave-to{
  opacity: 0;
  transform: translateY(-4px);
}

.pos-grid{
  display:flex;
  flex-direction:column;
  gap: 10px;
  padding-top: 12px;
}
.pos-row{
  display:flex;
  justify-content:space-between;
  gap: 12px;
}
.pos-name{ color: var(--muted); font-size: 13px; }
.pos-person{ font-weight: 600; }

/* KV summary */
.kv{
  display:grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.kv-item{
  border: 1px solid var(--border);
  background: var(--surface-2);
  border-radius: calc(var(--radius) - 6px);
  padding: 12px;
}
.kv-label{ color: var(--muted); font-size: 12px; }
.kv-value{ font-weight: 800; font-size: 18px; margin-top: 6px; }

/* Position chips */
.pos-summary{
  display:flex;
  flex-wrap:wrap;
  gap: 8px;
}
.pos-chip{
  border: 1px solid var(--border);
  background: var(--surface-2);
  border-radius: 999px;
  padding: 8px 10px;
  display:flex;
  gap: 8px;
  align-items:center;
}
.pos-chip__name{ font-weight: 700; font-size: 13px; }
.pos-chip__meta{ color: var(--muted); font-size: 12px; }

/* History */
.filters-inline{
  display:flex;
  align-items:center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content:flex-end;
}
.filters-label{ color: var(--muted); font-size: 13px; }
.filters-select{ min-width: 150px; }

.history{ display:flex; flex-direction:column; gap: 12px; }
.history-meta{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap: 10px;
}
.history-meta__right{ display:flex; align-items:center; gap: 8px; }

.history-cards{
  display:grid;
  gap: 12px;
}
@media (min-width: 860px){
  .history-cards{ grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (min-width: 1160px){
  .history-cards{ grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

.history-card{
  border: 1px solid var(--border);
  background: var(--surface-2);
  border-radius: calc(var(--radius) - 6px);
  padding: 14px;
  display:flex;
  flex-direction:column;
  min-height: 120px;
}
.history-card__top{
  display:flex;
  justify-content:space-between;
  gap: 10px;
}
.history-year{ font-weight: 800; font-size: 18px; }
.history-created{ margin-top: 4px; font-size: 12px; }

.history-card__actions{
  margin-top: auto;
  display:flex;
  justify-content:flex-end;
  padding-top: 10px;
}

/* Pagination */
.polish-pagination{
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.jump{
  display:flex;
  align-items:center;
  gap: 8px;
}
.jump-input{
  width: 80px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface);
  color: var(--text);
}

/* Empty */
.empty{
  padding: 14px;
  border: 1px dashed var(--border);
  border-radius: calc(var(--radius) - 6px);
  background: var(--surface-2);
}

/* Mobile: keep it calm */
@media (max-width: 768px){
  .filters-inline{ justify-content:flex-start; }
  .kv{ grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
