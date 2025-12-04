<template>
  <div class="service-schedule">
    <section class="section">
      <div class="container">
        <h1 class="section-title">服事人員安排</h1>

        <!-- 崗位人員配置 -->
        <div class="card">
          <div class="card-header">
            <h2>崗位人員配置</h2>
            <button @click="openPositionManagement" class="btn btn-manage-positions">
              管理崗位
            </button>
          </div>
          <div class="position-config-summary">
            <p>點擊「管理崗位」按鈕來配置各崗位的人員（週六/週日）</p>
            <div class="position-summary-list">
              <div v-for="(posData, posCode) in positionConfig" :key="posCode" class="position-summary-item">
                <strong>{{ posData.positionName || posCode }}：</strong>
                <span>週六 {{ (posData.saturday || []).length }} 人</span>
                <span>週日 {{ (posData.sunday || []).length }} 人</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 崗位管理 Modal -->
        <PositionManagementModal
          :show="showPositionManagement"
          @close="closePositionManagement"
        />

        <!-- 日期範圍選擇 -->
        <div class="card">
          <h2>日期範圍設定</h2>
          <div class="date-range-group">
            <div class="date-range-input-wrapper">
              <label>日期範圍：</label>
              <DateRangePicker 
                v-model="dateRange" 
                placeholder="選擇日期範圍"
              />
            </div>
            <button @click="generateSchedule" class="btn btn-primary" :disabled="!canGenerate">
              產生服事表
            </button>
          </div>
        </div>

        <!-- 歷史記錄 -->
        <div class="card">
          <div class="schedule-header">
            <h2>歷史記錄</h2>
            <button @click="loadHistory" class="btn btn-refresh">重新載入</button>
          </div>
          <div class="history-list" v-if="historyList.length > 0">
            <div class="history-item" v-for="item in historyList" :key="item.id">
              <div class="history-info">
                <span class="history-date">{{ formatDisplayDate(item.scheduleDate) }}</span>
                <span class="history-version">第 {{ item.version }} 版</span>
                <span class="history-range">{{ formatDisplayDate(item.startDate) }} ~ {{ formatDisplayDate(item.endDate) }}</span>
              </div>
              <div class="history-actions">
                <button @click="loadSchedule(item.id)" class="btn btn-load">載入</button>
                <button @click="editSchedule(item.id)" class="btn btn-edit">編輯</button>
                <button @click="deleteSchedule(item.id)" class="btn btn-delete">刪除</button>
              </div>
            </div>
          </div>
          <p v-else class="empty-message">尚無歷史記錄</p>
        </div>

        <!-- 安排結果 -->
        <div class="card" v-if="schedule.length > 0">
          <div class="schedule-header">
            <h2>服事表 <span v-if="isEditing" class="editing-badge">編輯模式</span></h2>
            <div class="schedule-actions">
              <button v-if="isEditing" @click="cancelEdit" class="btn btn-cancel">取消</button>
              <button v-if="isEditing" @click="updateSchedule" class="btn btn-save" :disabled="saving">保存修改</button>
              <button v-else-if="!isLoadedFromHistory" @click="saveSchedule" class="btn btn-save" :disabled="saving">保存服事表</button>
              <button @click="exportSchedule" class="btn btn-export">匯出服事表</button>
            </div>
          </div>
          <div class="schedule-table">
            <table>
              <thead>
                <tr>
                  <th>日期</th>
                  <th>電腦</th>
                  <th>混音</th>
                  <th>燈光</th>
                  <th>直播</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in schedule" :key="index">
                  <td>{{ item.formattedDate || formatDisplayDate(item.date) || item.date }}</td>
                  <td>
                    <select v-if="isEditing" v-model="item.computer" class="edit-select">
                      <option value="">-- 請選擇 --</option>
                      <option v-for="person in getAvailablePersons(item, 'computer')" :key="person" :value="person">
                        {{ person }}
                      </option>
                    </select>
                    <span v-else>{{ item.computer }}</span>
                  </td>
                  <td>
                    <select v-if="isEditing" v-model="item.sound" class="edit-select">
                      <option value="">-- 請選擇 --</option>
                      <option v-for="person in getAvailablePersons(item, 'sound')" :key="person" :value="person">
                        {{ person }}
                      </option>
                    </select>
                    <span v-else>{{ item.sound }}</span>
                  </td>
                  <td>
                    <select v-if="isEditing" v-model="item.light" class="edit-select">
                      <option value="">-- 請選擇 --</option>
                      <option v-for="person in getAvailablePersons(item, 'light')" :key="person" :value="person">
                        {{ person }}
                      </option>
                    </select>
                    <span v-else>{{ item.light }}</span>
                  </td>
                  <td>
                    <select v-if="isEditing" v-model="item.live" class="edit-select">
                      <option value="">-- 請選擇 --</option>
                      <option v-for="person in getAvailablePersons(item, 'live')" :key="person" :value="person">
                        {{ person }}
                      </option>
                    </select>
                    <span v-else>{{ item.live }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import DateRangePicker from '@/components/DateRangePicker.vue'
import PositionManagementModal from '@/components/PositionManagementModal.vue'
import { apiRequest } from '@/utils/api'

// 崗位人員配置（從新的 API 載入）
const positionConfig = ref({})

// 崗位管理 Modal 顯示狀態
const showPositionManagement = ref(false)

// 日期範圍（格式：[startDate, endDate]，日期格式為 'YYYY-MM-DD'）
const dateRange = ref([])

// 安排結果
const schedule = ref([])

// 歷史記錄列表
const historyList = ref([])

// 保存狀態
const saving = ref(false)

// 編輯狀態
const isEditing = ref(false)
const editingScheduleId = ref(null)
const originalSchedule = ref([]) // 保存原始數據，用於取消編輯
const isLoadedFromHistory = ref(false) // 標記是否從歷史記錄載入（載入的不應該顯示「保存服事表」按鈕）


// 檢查是否可以產生服事表
const canGenerate = computed(() => {
  const checkPosition = (posCode) => {
    const posData = positionConfig.value[posCode]
    if (!posData) return false
    const satCount = (posData.saturday || []).length
    const sunCount = (posData.sunday || []).length
    return satCount > 0 || sunCount > 0
  }
  
  const hasComputer = checkPosition('computer')
  const hasSound = checkPosition('sound')
  const hasLight = checkPosition('light')
  const hasLive = checkPosition('live')
  
  return hasComputer && hasSound && hasLight && hasLive && dateRange.value && dateRange.value.length === 2
})

// 打開崗位管理 Modal
const openPositionManagement = () => {
  showPositionManagement.value = true
}

// 關閉崗位管理 Modal
const closePositionManagement = async () => {
  showPositionManagement.value = false
  await loadPositionConfig() // 重新載入配置
  
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
  
  allPersons.forEach(person => {
    serviceCount[person] = {
      computer: 0,
      sound: 0,
      light: 0,
      live: 0,
      total: 0
    }
  })
  
  // 為每個日期分配人員
  dates.forEach((dateInfo) => {
    const dayType = dateInfo.isSaturday ? 'saturday' : 'sunday'
    const assignment = {
      date: formatDateISO(dateInfo.date), // 使用 ISO 格式存儲日期
      dayOfWeek: dateInfo.dayOfWeek, // 保存星期信息
      formattedDate: formatDateDisplay(dateInfo.date, dateInfo.dayOfWeek), // 格式化顯示日期
      computer: '',
      sound: '',
      light: '',
      live: ''
    }
    
    // 為每個崗位分配人員
    // 主要崗位（電腦、混音、燈光）之間不能重複，但直播不受限制
    const mainPositions = ['computer', 'sound', 'light'] // 主要崗位
    const livePosition = 'live' // 直播崗位（不受重複限制）
    const usedMainPersons = new Set() // 記錄今天主要崗位已使用的人員
    
    // 先分配主要崗位（電腦、混音、燈光）
    mainPositions.forEach(position => {
      const posData = positionConfig.value[position]
      if (!posData || !posData[dayType]) {
        return
      }
      // 過濾：1. 只使用參與自動分配的人員 2. 過濾掉今天已在其他主要崗位的人員
      const availablePersons = posData[dayType]
        .filter(p => {
          // 只使用參與自動分配的人員
          if (typeof p === 'object' && p.includeInAutoSchedule === false) {
            return false
          }
          // 如果是字符串，默認參與自動分配
          const personName = typeof p === 'string' ? p : (p.personName || p.displayName)
          return !usedMainPersons.has(personName)
        })
        .map(p => typeof p === 'string' ? p : (p.personName || p.displayName))
      
      if (availablePersons.length === 0) {
        // 如果沒有可用人員，從全部名單中選擇（但不在今天其他主要崗位）
        // 只使用參與自動分配的人員
        const allAvailable = posData[dayType]
          .filter(p => {
            // 只使用參與自動分配的人員
            if (typeof p === 'object' && p.includeInAutoSchedule === false) {
              return false
            }
            const personName = typeof p === 'string' ? p : (p.personName || p.displayName)
            return personName !== assignment.computer && 
                   personName !== assignment.sound && 
                   personName !== assignment.light
          })
          .map(p => typeof p === 'string' ? p : (p.personName || p.displayName))
        if (allAvailable.length > 0) {
          // 選擇該崗位服務次數最少的
          allAvailable.sort((a, b) => {
            const countA = serviceCount[a] ? serviceCount[a][position] : 0
            const countB = serviceCount[b] ? serviceCount[b][position] : 0
            if (countA !== countB) return countA - countB
            // 如果次數相同，選擇總服務次數最少的
            const totalA = serviceCount[a] ? serviceCount[a].total : 0
            const totalB = serviceCount[b] ? serviceCount[b].total : 0
            return totalA - totalB
          })
          assignment[position] = allAvailable[0]
          usedMainPersons.add(allAvailable[0])
        }
      } else {
        // 從可用人員中選擇該崗位服務次數最少的
        availablePersons.sort((a, b) => {
          const countA = serviceCount[a] ? serviceCount[a][position] : 0
          const countB = serviceCount[b] ? serviceCount[b][position] : 0
          if (countA !== countB) return countA - countB
          // 如果次數相同，選擇總服務次數最少的
          const totalA = serviceCount[a] ? serviceCount[a].total : 0
          const totalB = serviceCount[b] ? serviceCount[b].total : 0
          return totalA - totalB
        })
        assignment[position] = availablePersons[0]
        usedMainPersons.add(availablePersons[0])
      }
      
      // 更新服務次數
      if (assignment[position] && serviceCount[assignment[position]]) {
        serviceCount[assignment[position]][position]++
        serviceCount[assignment[position]].total++
      }
    })
    
    // 分配直播崗位（不受主要崗位重複限制，可以與主要崗位重複）
    // 但只使用參與自動分配的人員
    const livePosData = positionConfig.value[livePosition]
    const liveAvailablePersons = livePosData && livePosData[dayType] 
      ? livePosData[dayType]
          .filter(p => {
            // 只使用參與自動分配的人員
            if (typeof p === 'object' && p.includeInAutoSchedule === false) {
              return false
            }
            return true
          })
          .map(p => typeof p === 'string' ? p : (p.personName || p.displayName))
      : []
    if (liveAvailablePersons.length > 0) {
      // 選擇該崗位服務次數最少的（不需要檢查主要崗位的人員）
      liveAvailablePersons.sort((a, b) => {
        const countA = serviceCount[a] ? serviceCount[a][livePosition] : 0
        const countB = serviceCount[b] ? serviceCount[b][livePosition] : 0
        if (countA !== countB) return countA - countB
        // 如果次數相同，選擇總服務次數最少的
        const totalA = serviceCount[a] ? serviceCount[a].total : 0
        const totalB = serviceCount[b] ? serviceCount[b].total : 0
        return totalA - totalB
      })
      assignment[livePosition] = liveAvailablePersons[0]
      
      // 更新服務次數
      if (assignment[livePosition] && serviceCount[assignment[livePosition]]) {
        serviceCount[assignment[livePosition]][livePosition]++
        serviceCount[assignment[livePosition]].total++
      }
    }
    
    schedule.push(assignment)
  })
  
  return schedule
}

// 產生服事表
const generateSchedule = () => {
  if (!canGenerate.value) {
    alert('請確保每個崗位（電腦、混音、燈光、直播）至少配置一位人員，並選擇日期範圍')
    return
  }
  
  const [startDate, endDate] = dateRange.value
  const weekendDates = getWeekendDates(startDate, endDate)
  
  if (weekendDates.length === 0) {
    alert('選擇的日期範圍內沒有週六或週日')
    return
  }
  
  // 重置狀態
  isEditing.value = false
  editingScheduleId.value = null
  isLoadedFromHistory.value = false
  
  schedule.value = distributePersons(weekendDates)
  
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
  
  // 退出編輯模式（如果正在編輯）
  if (isEditing.value) {
    isEditing.value = false
    editingScheduleId.value = null
    originalSchedule.value = []
  }
}

// 驗證服事表（檢查主要崗位之間是否有重複）
const validateSchedule = () => {
  const mainPositions = ['computer', 'sound', 'light']
  const errors = []
  
  schedule.value.forEach((item, index) => {
    const usedMainPersons = new Set()
    const mainPositionNames = { computer: '電腦', sound: '混音', light: '燈光' }
    
    // 檢查主要崗位之間是否有重複
    mainPositions.forEach(position => {
      const person = item[position]
      if (person) {
        if (usedMainPersons.has(person)) {
          const dateStr = item.formattedDate || item.date || `第 ${index + 1} 行`
          errors.push(`${dateStr}：${person} 同時擔任多個主要崗位（電腦、混音、燈光之間不能重複）`)
        }
        usedMainPersons.add(person)
      }
    })
  })
  
  return errors
}

// 保存服事表
const saveSchedule = async () => {
  if (schedule.value.length === 0 || dateRange.value.length !== 2) {
    alert('請先產生服事表')
    return
  }

  // 驗證主要崗位之間是否有重複
  const validationErrors = validateSchedule()
  if (validationErrors.length > 0) {
    alert('驗證失敗：\n' + validationErrors.join('\n') + '\n\n請修正後再保存。')
    return
  }

  saving.value = true
  try {
    const [startDate, endDate] = dateRange.value
    const scheduleDate = startDate // 使用開始日期作為安排日期

    const response = await apiRequest('/church/service-schedules', {
      method: 'POST',
      body: JSON.stringify({
        scheduleDate,
        startDate,
        endDate,
        scheduleData: schedule.value,
        positionConfig: positionConfig.value
      })
    }, '保存服事表中...')

    const result = await response.json()
    
    if (response.ok && result.success !== false) {
      alert(`服事表保存成功！版本：第 ${result.version} 版`)
      // 標記為已保存，移除"保存服事表"按鈕
      isLoadedFromHistory.value = true
      await loadHistory() // 重新載入歷史記錄
    } else {
      alert('保存失敗：' + (result.error || '未知錯誤'))
    }
  } catch (error) {
    alert('保存失敗：' + error.message)
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
    alert('載入歷史記錄失敗：' + error.message)
  }
}

// 載入指定的服事表
const loadSchedule = async (id) => {
  try {
    const response = await apiRequest(`/church/service-schedules/${id}`, {
      method: 'GET'
    }, '載入服事表中...')
    const data = await response.json()
    
    if (response.ok) {
      // 載入日期範圍
      dateRange.value = [data.startDate, data.endDate]
      
      // 載入服事表數據
      schedule.value = data.scheduleData
      
      // 確保每個項目都有 formattedDate
      schedule.value.forEach(item => {
        if (!item.formattedDate && item.date) {
          const dateObj = parseDate(item.date)
          if (dateObj) {
            const year = dateObj.getFullYear()
            const month = String(dateObj.getMonth() + 1).padStart(2, '0')
            const day = String(dateObj.getDate()).padStart(2, '0')
            const dayOfWeekChar = item.dayOfWeek || (dateObj.getDay() === 6 ? '六' : '日')
            item.formattedDate = `${year}/${month}/${day}(${dayOfWeekChar})`
          } else {
            // 如果解析失敗，嘗試從原始字符串提取
            item.formattedDate = item.date
          }
        }
      })
      
      // 載入崗位配置（如果有）
      if (data.positionConfig) {
        positionConfig.value = data.positionConfig
      }
      
      // 退出編輯模式
      isEditing.value = false
      editingScheduleId.value = null
      isLoadedFromHistory.value = true // 標記為從歷史記錄載入
      
      alert('服事表載入成功！')
    } else {
      alert('載入失敗：' + (data.error || '未知錯誤'))
    }
  } catch (error) {
    alert('載入失敗：' + error.message)
  }
}

// 編輯指定的服事表
const editSchedule = async (id) => {
  try {
    const response = await apiRequest(`/church/service-schedules/${id}`, {
      method: 'GET'
    }, '載入服事表中...')
    const data = await response.json()
    
    if (response.ok) {
      // 載入日期範圍
      dateRange.value = [data.startDate, data.endDate]
      
      // 載入服事表數據
      schedule.value = data.scheduleData
      
      // 確保每個項目都有 formattedDate
      schedule.value.forEach(item => {
        if (!item.formattedDate && item.date) {
          const dateObj = parseDate(item.date)
          if (dateObj) {
            const year = dateObj.getFullYear()
            const month = String(dateObj.getMonth() + 1).padStart(2, '0')
            const day = String(dateObj.getDate()).padStart(2, '0')
            const dayOfWeekChar = item.dayOfWeek || (dateObj.getDay() === 6 ? '六' : '日')
            item.formattedDate = `${year}/${month}/${day}(${dayOfWeekChar})`
          } else {
            // 如果解析失敗，嘗試從原始字符串提取
            item.formattedDate = item.date
          }
        }
      })
      
      // 載入崗位配置（如果有）
      if (data.positionConfig) {
        positionConfig.value = data.positionConfig
      }
      
      // 重新載入最新的崗位配置，確保下拉選單顯示最新的人員列表
      await loadPositionConfig()
      
      // 進入編輯模式
      isEditing.value = true
      editingScheduleId.value = id
      
      // 深拷貝原始數據，用於取消編輯
      originalSchedule.value = JSON.parse(JSON.stringify(schedule.value))
      
      alert('已進入編輯模式，您可以修改人員安排')
    } else {
      alert('載入失敗')
    }
  } catch (error) {
    alert('載入失敗：' + error.message)
  }
}

// 取消編輯
const cancelEdit = () => {
  if (confirm('確定要取消編輯嗎？未保存的修改將丟失。')) {
    // 清空所有資料
    schedule.value = []
    dateRange.value = []
    isEditing.value = false
    editingScheduleId.value = null
    originalSchedule.value = []
    isLoadedFromHistory.value = false
    alert('已取消編輯，資料已清空。')
  }
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
  
  // 將對象轉換為字符串（用於下拉選單顯示）
  availablePersons = availablePersons.map(p => {
    if (typeof p === 'string') {
      return p
    } else {
      return p.personName || p.displayName || ''
    }
  }).filter(p => p) // 過濾空值
  
  // 主要崗位（電腦、混音、燈光）之間不能重複，但直播不受限制
  const mainPositions = ['computer', 'sound', 'light']
  const isMainPosition = mainPositions.includes(position)
  const isLivePosition = position === 'live'
  
  if (isMainPosition) {
    // 對於主要崗位，過濾掉今天其他主要崗位已分配的人員
    const usedMainPersons = new Set()
    mainPositions.forEach(pos => {
      if (pos !== position && item[pos]) {
        usedMainPersons.add(item[pos])
      }
    })
    availablePersons = availablePersons.filter(p => !usedMainPersons.has(p))
  }
  // 對於直播崗位，不需要過濾，可以選擇所有人員（包括主要崗位的人員）
  
  return availablePersons
}

// 更新服事表
const updateSchedule = async () => {
  if (schedule.value.length === 0 || !editingScheduleId.value) {
    alert('請先載入要編輯的服事表')
    return
  }

  // 驗證主要崗位之間是否有重複
  const validationErrors = validateSchedule()
  if (validationErrors.length > 0) {
    alert('驗證失敗：\n' + validationErrors.join('\n') + '\n\n請修正後再保存。')
    return
  }

  // 驗證是否有空值
  const hasEmpty = schedule.value.some(item => !item.computer || !item.sound || !item.light || !item.live)
  if (hasEmpty) {
    if (!confirm('部分日期的人員未填寫完整，確定要保存嗎？')) {
      return
    }
  }

  saving.value = true
  try {
    const [startDate, endDate] = dateRange.value

    const response = await fetch(`${API_BASE_URL}/church/service-schedules/${editingScheduleId.value}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        startDate,
        endDate,
        scheduleData: schedule.value,
        positionConfig: positionConfig.value
      })
    })

    const result = await response.json()
    
    if (response.ok) {
      alert('服事表更新成功！')
      isEditing.value = false
      editingScheduleId.value = null
      originalSchedule.value = []
      isLoadedFromHistory.value = true // 更新後仍然是從歷史記錄載入的狀態
      await loadHistory() // 重新載入歷史記錄
    } else {
      alert('更新失敗：' + (result.error || '未知錯誤'))
    }
  } catch (error) {
    alert('更新失敗：' + error.message)
  } finally {
    saving.value = false
  }
}

// 刪除服事表
const deleteSchedule = async (id) => {
  if (!confirm('確定要刪除此服事表嗎？')) {
    return
  }

  try {
    const response = await apiRequest(`/church/service-schedules/${id}`, {
      method: 'DELETE'
    }, '刪除服事表中...')
    
    const result = await response.json()
    
    if (response.ok && result.success !== false) {
      alert('刪除成功')
      await loadHistory() // 重新載入歷史記錄
    } else {
      alert('刪除失敗：' + (result.error || '未知錯誤'))
    }
  } catch (error) {
    alert('刪除失敗：' + error.message)
  }
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
  if (schedule.value.length === 0) return
  
  // 格式：日期,電腦/混音/燈光,直播
  let csv = '日期,電腦/混音/燈光,直播\n'
  schedule.value.forEach(item => {
    const mainPositions = `${item.computer || ''}/${item.sound || ''}/${item.light || ''}`
    csv += `${item.date},${mainPositions},${item.live || ''}\n`
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
        
        for (const [posCode, posData] of Object.entries(config)) {
          // 保留完整對象信息（包括 includeInAutoSchedule），用於產生服事表時過濾
          convertedConfig[posCode] = {
            positionName: posData.positionName || posCode,
            saturday: posData.saturday || [],
            sunday: posData.sunday || []
          }
        }
        
        positionConfig.value = convertedConfig
        console.log('崗位配置載入成功：', positionConfig.value)
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

// 組件掛載時載入配置和歷史記錄
onMounted(() => {
  loadPositionConfig()
  loadHistory()
})
</script>

<style scoped>
.service-schedule {
  padding: 2rem 0;
}

.card {
  margin-bottom: 2rem;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.card-header h2 {
  margin: 0;
}

.btn-manage-positions {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-manage-positions:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.position-config-summary {
  padding: 1.5rem;
  background: #f8fafc;
  border-radius: 0.5rem;
  border: 1px solid #e2e8f0;
}

.position-config-summary p {
  margin: 0 0 1rem 0;
  color: #64748b;
  font-size: 0.9rem;
}

.position-summary-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.position-summary-item {
  padding: 0.75rem 1rem;
  background: white;
  border-radius: 0.5rem;
  border: 1px solid #e2e8f0;
  font-size: 0.9rem;
  color: #475569;
}

.position-summary-item strong {
  color: #1e293b;
  margin-right: 0.5rem;
}

.position-summary-item span {
  margin-right: 1rem;
  color: #667eea;
  font-weight: 500;
}

.card h2 {
  color: #667eea;
  margin-bottom: 1.5rem;
  font-size: 1.5rem;
}

.position-config {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.position-group {
  padding: 1.5rem;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.position-group h3 {
  color: #667eea;
  margin-bottom: 1rem;
  font-size: 1.2rem;
}

.day-group {
  margin-bottom: 1rem;
}

.day-group:last-child {
  margin-bottom: 0;
}

.day-group label {
  display: inline-block;
  width: 60px;
  font-weight: 500;
  color: #333;
}

.person-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
  margin-top: 0.5rem;
}

.person-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.8rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 0.9rem;
}

.tag-edit {
  background: none;
  border: none;
  color: #007bff;
  cursor: pointer;
  font-size: 0.9rem;
  line-height: 1;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
  margin-right: 2px;
}

.tag-edit:hover {
  background: #e7f3ff;
}

.tag-remove {
  background: none;
  border: none;
  color: #dc3545;
  cursor: pointer;
  font-size: 1.2rem;
  line-height: 1;
  padding: 0;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.tag-remove:hover {
  background: #f8d7da;
}

.tag-edit-input {
  padding: 0.4rem 0.8rem;
  border: 2px solid #007bff;
  border-radius: 20px;
  font-size: 0.9rem;
  width: 120px;
  outline: none;
  background: white;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.1);
}

.tag-input {
  padding: 0.4rem 0.8rem;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 0.9rem;
  width: 120px;
}

.tag-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.date-range-group {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  flex-wrap: wrap;
}

.date-range-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
  min-width: 250px;
}

.date-range-input-wrapper label {
  font-weight: 500;
  color: #333;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.schedule-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-save {
  background: #007bff;
  color: white;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-save:hover:not(:disabled) {
  background: #0056b3;
}

.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-refresh {
  background: #17a2b8;
  color: white;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-refresh:hover {
  background: #138496;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 5px;
  border-left: 4px solid #667eea;
}

.history-info {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex: 1;
}

.history-date {
  font-weight: 600;
  color: #667eea;
}

.history-version {
  background: #667eea;
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 15px;
  font-size: 0.85rem;
}

.history-range {
  color: #666;
  font-size: 0.9rem;
}

.history-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-load {
  background: #28a745;
  color: white;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.85rem;
}

.btn-edit {
  background: #ffc107;
  color: #212529;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 500;
}

.btn-edit:hover {
  background: #e0a800;
}

.btn-cancel {
  background: #6c757d;
  color: white;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-cancel:hover {
  background: #5a6268;
}

.btn-load:hover {
  background: #218838;
}

.btn-delete {
  background: #dc3545;
  color: white;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.85rem;
}

.btn-delete:hover {
  background: #c82333;
}

.btn-export {
  background: #28a745;
  color: white;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-export:hover {
  background: #218838;
}

.schedule-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: white;
}

.edit-select {
  width: 100%;
  padding: 0.5rem;
  border: 2px solid #667eea;
  border-radius: 5px;
  font-size: 0.9rem;
  background: white;
  cursor: pointer;
}

.edit-select:focus {
  outline: none;
  border-color: #764ba2;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.editing-badge {
  display: inline-block;
  background: #ffc107;
  color: #212529;
  padding: 0.25rem 0.75rem;
  border-radius: 15px;
  font-size: 0.85rem;
  font-weight: 600;
  margin-left: 0.5rem;
}

thead {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

th, td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

th {
  font-weight: 600;
}

tbody tr:hover {
  background: #f8f9fa;
}

tbody tr:last-child td {
  border-bottom: none;
}

@media (max-width: 768px) {
  .date-range-group {
    flex-direction: column;
  }
  
  .date-input {
    width: 100%;
  }
  
  .schedule-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }
  
  table {
    font-size: 0.9rem;
  }
  
  th, td {
    padding: 0.5rem;
  }
  
  .position-group {
    padding: 1rem;
  }
  
  .person-tags {
    gap: 0.3rem;
  }
}
</style>
