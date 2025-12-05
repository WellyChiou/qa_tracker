<template>
  <div v-if="show" class="modal-overlay" @click="closeModal">
    <div class="modal-panel service-schedule-modal" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">{{ modalTitle }}</h2>
        <button class="btn-close" @click="closeModal">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <!-- 日期範圍選擇（僅在新增模式顯示） -->
        <div v-if="mode === 'add'" class="card">
          <h3>日期範圍設定</h3>
          <div class="date-range-group">
            <div class="date-range-input-wrapper">
              <label>日期範圍：</label>
              <DateRangePicker 
                v-model="localDateRange" 
                placeholder="選擇日期範圍"
              />
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
              產生服事表
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
              <!-- 名稱輸入/顯示 -->
              <div class="schedule-name-section">
                <label v-if="mode !== 'view'" class="schedule-name-label">
                  名稱：
                  <input 
                    v-model="localScheduleName" 
                    type="text" 
                    class="schedule-name-input" 
                    placeholder="請輸入服事表名稱"
                    :disabled="saving"
                  />
                </label>
                <div v-else class="schedule-name-display">
                  <strong>名稱：</strong>{{ localScheduleName || '未命名' }}
                </div>
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
              <!-- 編輯模式：如果有 editingScheduleId，顯示「更新」按鈕；否則顯示「新增」按鈕 -->
              <button v-if="isEditing && editingScheduleId" @click="updateSchedule" class="btn btn-save" :disabled="saving">更新</button>
              <button v-else-if="isEditing && !editingScheduleId && mode === 'add'" @click="saveSchedule" class="btn btn-save" :disabled="saving || !localScheduleName.trim()">新增</button>
              <button @click="exportSchedule" class="btn btn-export">匯出服事表</button>
            </div>
          </div>
          <div class="schedule-table">
            <table>
              <thead>
                <tr>
                  <th class="date-column">日期</th>
                  <th class="positions-header" colspan="100%">
                    <div class="positions-grid">
                      <div 
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
                      </div>
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in localSchedule" :key="index">
                  <td class="date-column">{{ item.formattedDate || formatDisplayDate(item.date, item.dayOfWeek) || item.date }}</td>
                  <td class="positions-cell">
                    <div class="positions-grid">
                      <div 
                        v-for="posCode in selectedPositionsList" 
                        :key="posCode"
                        class="position-cell"
                      >
                        <select v-if="isEditing" v-model="item[posCode + 'Id']" class="edit-select">
                          <option value="">-- 請選擇 --</option>
                          <option v-for="person in getAvailablePersons(item, posCode)" :key="person.id" :value="person.id">
                            {{ person.name }}
                          </option>
                        </select>
                        <span v-else>{{ item[posCode] || '-' }}</span>
                      </div>
                    </div>
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
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import DateRangePicker from '@/components/DateRangePicker.vue'
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
  scheduleId: {
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
const localScheduleName = ref('')
const localDateRange = ref([])
const localSelectedPositions = ref({})
const localInitialSelectedPositions = ref({})
const localUseRandomAssignment = ref(false)
const editingScheduleId = ref(null)
const isEditing = ref(false)
const saving = ref(false)
const originalSchedule = ref([])

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

const canGenerate = computed(() => {
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
  localScheduleName.value = ''
  localDateRange.value = []
  localSelectedPositions.value = {}
  localInitialSelectedPositions.value = {}
  localUseRandomAssignment.value = false
  editingScheduleId.value = null
  isEditing.value = false
  originalSchedule.value = []
}

// 當 mode 或 scheduleId 改變時，載入對應的資料
watch([() => props.show, () => props.mode, () => props.scheduleId], async ([show, mode, scheduleId]) => {
  if (show) {
    resetState()
    
    if (mode === 'add') {
      // 新增模式：初始化崗位選擇
      initializePositionSelection()
    } else if (mode === 'edit' && scheduleId) {
      // 編輯模式：載入資料並進入編輯模式
      await loadScheduleForEdit(scheduleId)
    } else if (mode === 'view' && scheduleId) {
      // 查看模式：只載入資料，不進入編輯模式
      await loadScheduleForView(scheduleId)
    }
  }
})

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

// 工具函數：解析日期
const parseDate = (dateStr) => {
  if (!dateStr) return null
  if (dateStr instanceof Date) {
    return isNaN(dateStr.getTime()) ? null : dateStr
  }
  let date = new Date(dateStr)
  if (isNaN(date.getTime())) {
    const match = String(dateStr).match(/^(\d{4})-(\d{2})-(\d{2})/)
    if (match) {
      date = new Date(parseInt(match[1]), parseInt(match[2]) - 1, parseInt(match[3]))
    } else {
      const match2 = String(dateStr).match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})/)
      if (match2) {
        date = new Date(parseInt(match2[1]), parseInt(match2[2]) - 1, parseInt(match2[3]))
      }
    }
  }
  return isNaN(date.getTime()) ? null : date
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

const loadScheduleForEdit = async (scheduleId) => {
  try {
    const response = await apiRequest(`/church/service-schedules/${scheduleId}`, {
      method: 'GET'
    }, '載入服事表中...')
    const data = await response.json()
    
    if (response.ok) {
      localScheduleName.value = data.name || ''
      editingScheduleId.value = scheduleId
      
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
          
          Object.keys(props.positionConfig).forEach(posCode => {
            if (scheduleItem[posCode + 'Id'] && !scheduleItem[posCode]) {
              const personId = scheduleItem[posCode + 'Id']
              const personName = getPersonNameById(personId, posCode, scheduleItem)
              if (personName) {
                scheduleItem[posCode] = personName
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

const loadScheduleForView = async (scheduleId) => {
  try {
    const response = await apiRequest(`/church/service-schedules/${scheduleId}`, {
      method: 'GET'
    }, '載入服事表中...')
    const data = await response.json()
    
    if (response.ok) {
      localScheduleName.value = data.name || ''
      
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
          
          Object.keys(props.positionConfig).forEach(posCode => {
            if (scheduleItem[posCode + 'Id'] && !scheduleItem[posCode]) {
              const personId = scheduleItem[posCode + 'Id']
              const personName = getPersonNameById(personId, posCode, scheduleItem)
              if (personName) {
                scheduleItem[posCode] = personName
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
      assignment[posCode + 'Id'] = null
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
        assignment[posCode + 'Id'] = availablePersons[0].id
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
        assignment[posCode + 'Id'] = availablePersons[0].id
        
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
    showNotification('請確保每個崗位至少配置一位人員，並選擇日期範圍', 'warning', 4000)
    return
  }
  
  const [startDate, endDate] = localDateRange.value
  const weekendDates = getWeekendDates(startDate, endDate)
  
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

const handlePositionSelectionChange = (posCode) => {
  if (!isEditing.value) return
  
  const isSelected = localSelectedPositions.value[posCode]
  
  if (isSelected) {
    localSchedule.value.forEach(item => {
      if (!item.hasOwnProperty(posCode)) {
        item[posCode] = ''
        item[posCode + 'Id'] = null
      }
    })
    
    if (hasPositionConfig(posCode) && localSchedule.value.length > 0) {
      autoAssignPositionForNewSelection(posCode)
    }
  } else {
    localSchedule.value.forEach(item => {
      item[posCode] = ''
      item[posCode + 'Id'] = null
    })
  }
}

// 為新選擇的崗位自動分配人員
const autoAssignPositionForNewSelection = (posCode) => {
  const posData = props.positionConfig[posCode]
  if (!posData) return
  
  const serviceCount = {}
  localSchedule.value.forEach(item => {
    const personId = item[posCode + 'Id']
    const personName = item[posCode]
    if (personId && personName) {
      if (!serviceCount[personName]) {
        serviceCount[personName] = { count: 0, id: personId }
      }
      serviceCount[personName].count++
    }
  })
  
  const allowDuplicate = posData.allowDuplicate === true
  
  localSchedule.value.forEach(item => {
    if (item[posCode + 'Id']) return
    
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
          const otherPersonId = item[otherPosCode + 'Id']
          if (otherPersonId) {
            usedPersonIds.add(otherPersonId)
          }
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
      item[posCode + 'Id'] = selectedPerson.id
      
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
    item[posCode + 'Id'] = null
  })
  
  const serviceCount = {}
  localSchedule.value.forEach(item => {
    const personId = item[posCode + 'Id']
    const personName = item[posCode]
    if (personId && personName) {
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
          const otherPersonId = item[otherPosCode + 'Id']
          if (otherPersonId) {
            usedPersonIds.add(otherPersonId)
          }
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
      item[posCode + 'Id'] = selectedPerson.id
      
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
      if (localSelectedPositions.value[posCode] && item[posCode + 'Id']) {
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
      const personId = item[posCode + 'Id']
      if (personId) {
        if (usedPersonIds.has(personId)) {
          const dateStr = item.formattedDate || item.date || `第 ${index + 1} 行`
          const personName = item[posCode] || '未知人員'
          const positionName = props.positionConfig[posCode]?.positionName || posCode
          const otherPositions = positionsWithDuplicateCheck
            .filter(p => p !== posCode && item[p + 'Id'] === personId)
            .map(p => props.positionConfig[p]?.positionName || p)
            .join('、')
          errors.push(`${dateStr}：${personName} 同時擔任多個崗位（${positionName} 與 ${otherPositions} 之間不能重複）`)
        }
        usedPersonIds.add(personId)
      }
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

  if (!localScheduleName.value || !localScheduleName.value.trim()) {
    showNotification('請輸入服事表名稱', 'warning', 3000)
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
          result[posCode] = item[posCode] || (item[posCode + 'Id'] ? getPersonNameById(item[posCode + 'Id'], posCode, item) : '')
          result[posCode + 'Id'] = item[posCode + 'Id'] ? Number(item[posCode + 'Id']) : null
        }
      })
      
      return result
    })

    const response = await apiRequest('/church/service-schedules', {
      method: 'POST',
      body: JSON.stringify({
        name: localScheduleName.value.trim(),
        scheduleData: scheduleDataForBackend,
        dateRange: localDateRange.value
      })
    }, '保存服事表中...')

    const result = await response.json()
    
    if (response.ok && result.success !== false) {
      showNotification('服事表保存成功！', 'success', 3000)
      editingScheduleId.value = result.id
      emit('saved', result.id)
      // 延遲關閉視窗，讓用戶看到成功訊息
      setTimeout(() => {
        closeModal()
      }, 1500)
    } else {
      showNotification('保存失敗：' + (result.error || '未知錯誤'), 'error', 4000)
    }
  } catch (error) {
    showNotification('保存失敗：' + error.message, 'error', 4000)
  } finally {
    saving.value = false
  }
}

// 更新服事表
const updateSchedule = async () => {
  if (!editingScheduleId.value) {
    showNotification('請先載入要編輯的服事表', 'warning', 3000)
    return
  }

  if (!localScheduleName.value || !localScheduleName.value.trim()) {
    showNotification('請輸入服事表名稱', 'warning', 3000)
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
          return !item[posCode + 'Id']
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
          result[posCode] = item[posCode] || (item[posCode + 'Id'] ? getPersonNameById(item[posCode + 'Id'], posCode, item) : '')
          result[posCode + 'Id'] = item[posCode + 'Id'] ? Number(item[posCode + 'Id']) : null
        }
      })
      
      return result
    })

    const response = await apiRequest(`/church/service-schedules/${editingScheduleId.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: localScheduleName.value.trim(),
        scheduleData: scheduleDataForBackend,
        dateRange: localDateRange.value
      })
    }, '更新服事表中...')

    const result = await response.json()
    
    if (response.ok) {
      showNotification('服事表更新成功！', 'success', 3000)
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
  link.setAttribute('download', `${localScheduleName.value || '服事表'}_${new Date().toISOString().split('T')[0]}.csv`)
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
  
  availablePersons = availablePersons.map(p => {
    if (typeof p === 'string') {
      return { id: null, name: p }
    } else {
      const personId = p.personId || p.id
      const personName = p.displayName || p.personName || ''
      return { id: personId, name: personName }
    }
  }).filter(p => p.id && p.name)
  
  const allowDuplicate = posData && posData.allowDuplicate
  
  if (!allowDuplicate) {
    const usedPersonIds = new Set()
    Object.keys(localSelectedPositions.value).forEach(otherPosCode => {
      if (localSelectedPositions.value[otherPosCode] && otherPosCode !== posCode) {
        const otherPosData = props.positionConfig[otherPosCode]
        if (otherPosData && !otherPosData.allowDuplicate) {
          const otherPersonId = item[otherPosCode + 'Id']
          if (otherPersonId) {
            usedPersonIds.add(otherPersonId)
          }
        }
      }
    })
    availablePersons = availablePersons.filter(p => !usedPersonIds.has(p.id))
  }
  
  return availablePersons
}

onMounted(() => {
  initializePositionSelection()
})
</script>

<style scoped>
/* Modal 基礎樣式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
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
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  color: #333;
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
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  background: #f8f9fa;
  font-weight: 600;
  color: #333;
}

.date-column {
  min-width: 120px;
  font-weight: 600;
}

.positions-header {
  padding: 0 !important;
}

.positions-cell {
  padding: 0 !important;
  vertical-align: top;
}

.positions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.5rem;
  padding: 0.75rem;
}

.position-header-cell {
  padding: 0.5rem;
  text-align: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  font-weight: 600;
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
  padding: 0.5rem;
  text-align: center;
  border-right: 1px solid #e0e0e0;
}

.position-cell:last-child {
  border-right: none;
}

.edit-select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
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
</style>

