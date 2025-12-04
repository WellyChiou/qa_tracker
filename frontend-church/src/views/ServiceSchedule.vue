<template>
  <div class="service-schedule">
    <section class="section">
      <div class="container">
        <h1 class="section-title">服事人員安排</h1>

        <!-- 崗位人員配置 -->
        <div class="card">
          <div class="card-header">
            <h2>崗位人員配置</h2>
            <button @click="savePositionConfig" class="btn btn-save-config" :disabled="savingConfig">
              {{ savingConfig ? '保存中...' : '保存配置' }}
            </button>
          </div>
          <div class="position-config">
            <!-- 電腦崗位 -->
            <div class="position-group">
              <h3>電腦</h3>
              <div class="day-group">
                <label>週六：</label>
                <div class="person-tags">
                  <span 
                    v-for="(person, index) in positionConfig.computer.saturday" 
                    :key="index"
                    class="person-tag"
                  >
                    {{ person }}
                    <button @click="removePersonFromPosition('computer', 'saturday', index)" class="tag-remove">×</button>
                  </span>
                  <input
                    type="text"
                    v-model="newPersonInput.computer.saturday"
                    placeholder="添加人員"
                    class="tag-input"
                    @keyup.enter="addPersonToPosition('computer', 'saturday')"
                    @blur="addPersonToPosition('computer', 'saturday')"
                  />
                </div>
              </div>
              <div class="day-group">
                <label>週日：</label>
                <div class="person-tags">
                  <span 
                    v-for="(person, index) in positionConfig.computer.sunday" 
                    :key="index"
                    class="person-tag"
                  >
                    {{ person }}
                    <button @click="removePersonFromPosition('computer', 'sunday', index)" class="tag-remove">×</button>
                  </span>
                  <input
                    type="text"
                    v-model="newPersonInput.computer.sunday"
                    placeholder="添加人員"
                    class="tag-input"
                    @keyup.enter="addPersonToPosition('computer', 'sunday')"
                    @blur="addPersonToPosition('computer', 'sunday')"
                  />
                </div>
              </div>
            </div>

            <!-- 音控崗位 -->
            <div class="position-group">
              <h3>音控</h3>
              <div class="day-group">
                <label>週六：</label>
                <div class="person-tags">
                  <span 
                    v-for="(person, index) in positionConfig.sound.saturday" 
                    :key="index"
                    class="person-tag"
                  >
                    {{ person }}
                    <button @click="removePersonFromPosition('sound', 'saturday', index)" class="tag-remove">×</button>
                  </span>
                  <input
                    type="text"
                    v-model="newPersonInput.sound.saturday"
                    placeholder="添加人員"
                    class="tag-input"
                    @keyup.enter="addPersonToPosition('sound', 'saturday')"
                    @blur="addPersonToPosition('sound', 'saturday')"
                  />
                </div>
              </div>
              <div class="day-group">
                <label>週日：</label>
                <div class="person-tags">
                  <span 
                    v-for="(person, index) in positionConfig.sound.sunday" 
                    :key="index"
                    class="person-tag"
                  >
                    {{ person }}
                    <button @click="removePersonFromPosition('sound', 'sunday', index)" class="tag-remove">×</button>
                  </span>
                  <input
                    type="text"
                    v-model="newPersonInput.sound.sunday"
                    placeholder="添加人員"
                    class="tag-input"
                    @keyup.enter="addPersonToPosition('sound', 'sunday')"
                    @blur="addPersonToPosition('sound', 'sunday')"
                  />
                </div>
              </div>
            </div>

            <!-- 燈光崗位 -->
            <div class="position-group">
              <h3>燈光</h3>
              <div class="day-group">
                <label>週六：</label>
                <div class="person-tags">
                  <span 
                    v-for="(person, index) in positionConfig.light.saturday" 
                    :key="index"
                    class="person-tag"
                  >
                    {{ person }}
                    <button @click="removePersonFromPosition('light', 'saturday', index)" class="tag-remove">×</button>
                  </span>
                  <input
                    type="text"
                    v-model="newPersonInput.light.saturday"
                    placeholder="添加人員"
                    class="tag-input"
                    @keyup.enter="addPersonToPosition('light', 'saturday')"
                    @blur="addPersonToPosition('light', 'saturday')"
                  />
                </div>
              </div>
              <div class="day-group">
                <label>週日：</label>
                <div class="person-tags">
                  <span 
                    v-for="(person, index) in positionConfig.light.sunday" 
                    :key="index"
                    class="person-tag"
                  >
                    {{ person }}
                    <button @click="removePersonFromPosition('light', 'sunday', index)" class="tag-remove">×</button>
                  </span>
                  <input
                    type="text"
                    v-model="newPersonInput.light.sunday"
                    placeholder="添加人員"
                    class="tag-input"
                    @keyup.enter="addPersonToPosition('light', 'sunday')"
                    @blur="addPersonToPosition('light', 'sunday')"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

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
              產生安排表
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
            <h2>服事安排表 <span v-if="isEditing" class="editing-badge">編輯模式</span></h2>
            <div class="schedule-actions">
              <button v-if="isEditing" @click="cancelEdit" class="btn btn-cancel">取消</button>
              <button v-if="isEditing" @click="updateSchedule" class="btn btn-save" :disabled="saving">保存修改</button>
              <button v-else-if="!isLoadedFromHistory" @click="saveSchedule" class="btn btn-save" :disabled="saving">保存服事表</button>
              <button @click="exportSchedule" class="btn btn-export">匯出安排表</button>
            </div>
          </div>
          <div class="schedule-table">
            <table>
              <thead>
                <tr>
                  <th>日期</th>
                  <th>電腦</th>
                  <th>音控</th>
                  <th>燈光</th>
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
import { ref, computed, onMounted } from 'vue'
import DateRangePicker from '@/components/DateRangePicker.vue'

// 崗位人員配置（從資料庫載入）
const positionConfig = ref({
  computer: {
    saturday: [],
    sunday: []
  },
  sound: {
    saturday: [],
    sunday: []
  },
  light: {
    saturday: [],
    sunday: []
  }
})

// 新增人員輸入框
const newPersonInput = ref({
  computer: { saturday: '', sunday: '' },
  sound: { saturday: '', sunday: '' },
  light: { saturday: '', sunday: '' }
})

// 日期範圍（格式：[startDate, endDate]，日期格式為 'YYYY-MM-DD'）
const dateRange = ref([])

// 安排結果
const schedule = ref([])

// 歷史記錄列表
const historyList = ref([])

// 保存狀態
const saving = ref(false)
const savingConfig = ref(false)

// 編輯狀態
const isEditing = ref(false)
const editingScheduleId = ref(null)
const originalSchedule = ref([]) // 保存原始數據，用於取消編輯
const isLoadedFromHistory = ref(false) // 標記是否從歷史記錄載入（載入的不應該顯示「保存服事表」按鈕）

// API 基礎 URL
const API_BASE_URL = import.meta.env.DEV 
  ? `${window.location.protocol}//${window.location.hostname}:8080/api`
  : `${window.location.protocol}//${window.location.hostname}/api`

// 檢查是否可以產生安排表
const canGenerate = computed(() => {
  const hasComputer = positionConfig.value.computer.saturday.length > 0 || positionConfig.value.computer.sunday.length > 0
  const hasSound = positionConfig.value.sound.saturday.length > 0 || positionConfig.value.sound.sunday.length > 0
  const hasLight = positionConfig.value.light.saturday.length > 0 || positionConfig.value.light.sunday.length > 0
  return hasComputer && hasSound && hasLight && dateRange.value && dateRange.value.length === 2
})

// 添加人員到崗位
const addPersonToPosition = (position, day) => {
  const name = newPersonInput.value[position][day].trim()
  if (name && !positionConfig.value[position][day].includes(name)) {
    positionConfig.value[position][day].push(name)
    newPersonInput.value[position][day] = ''
  }
}

// 從崗位移除人員
const removePersonFromPosition = (position, day, index) => {
  positionConfig.value[position][day].splice(index, 1)
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
  const allPersons = new Set()
  Object.keys(positionConfig.value).forEach(position => {
    positionConfig.value[position].saturday.forEach(p => allPersons.add(p))
    positionConfig.value[position].sunday.forEach(p => allPersons.add(p))
  })
  
  allPersons.forEach(person => {
    serviceCount[person] = {
      computer: 0,
      sound: 0,
      light: 0,
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
      light: ''
    }
    
    // 為每個崗位分配人員
    const positions = ['computer', 'sound', 'light']
    const usedPersons = new Set() // 記錄今天已使用的人員
    
    positions.forEach(position => {
      const availablePersons = positionConfig.value[position][dayType].filter(p => !usedPersons.has(p))
      
      if (availablePersons.length === 0) {
        // 如果沒有可用人員，從全部名單中選擇（但不在今天其他崗位）
        const allAvailable = positionConfig.value[position][dayType].filter(p => 
          assignment.computer !== p && 
          assignment.sound !== p && 
          assignment.light !== p
        )
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
          usedPersons.add(allAvailable[0])
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
        usedPersons.add(availablePersons[0])
      }
      
      // 更新服務次數
      if (assignment[position] && serviceCount[assignment[position]]) {
        serviceCount[assignment[position]][position]++
        serviceCount[assignment[position]].total++
      }
    })
    
    schedule.push(assignment)
  })
  
  return schedule
}

// 產生安排表
const generateSchedule = () => {
  if (!canGenerate.value) {
    alert('請確保每個崗位至少配置一位人員，並選擇日期範圍')
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

// 保存安排表
const saveSchedule = async () => {
  if (schedule.value.length === 0 || dateRange.value.length !== 2) {
    alert('請先產生安排表')
    return
  }

  saving.value = true
  try {
    const [startDate, endDate] = dateRange.value
    const scheduleDate = startDate // 使用開始日期作為安排日期

    const response = await fetch(`${API_BASE_URL}/church/service-schedules`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        scheduleDate,
        startDate,
        endDate,
        scheduleData: schedule.value,
        positionConfig: positionConfig.value
      })
    })

    const result = await response.json()
    
    if (response.ok) {
      alert(`安排表保存成功！版本：第 ${result.version} 版`)
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
    const response = await fetch(`${API_BASE_URL}/church/service-schedules`)
    if (response.ok) {
      historyList.value = await response.json()
    }
  } catch (error) {
    console.error('載入歷史記錄失敗：', error)
  }
}

// 載入指定的安排表
const loadSchedule = async (id) => {
  try {
    const response = await fetch(`${API_BASE_URL}/church/service-schedules/${id}`)
    if (response.ok) {
      const data = await response.json()
      
      // 載入日期範圍
      dateRange.value = [data.startDate, data.endDate]
      
      // 載入安排表數據
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
      
      alert('安排表載入成功！')
    } else {
      alert('載入失敗')
    }
  } catch (error) {
    alert('載入失敗：' + error.message)
  }
}

// 編輯指定的安排表
const editSchedule = async (id) => {
  try {
    const response = await fetch(`${API_BASE_URL}/church/service-schedules/${id}`)
    if (response.ok) {
      const data = await response.json()
      
      // 載入日期範圍
      dateRange.value = [data.startDate, data.endDate]
      
      // 載入安排表數據
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
  const availablePersons = positionConfig.value[position]?.[dayKey] || []
  
  return availablePersons
}

// 更新安排表
const updateSchedule = async () => {
  if (schedule.value.length === 0 || !editingScheduleId.value) {
    alert('請先載入要編輯的安排表')
    return
  }

  // 驗證是否有空值
  const hasEmpty = schedule.value.some(item => !item.computer || !item.sound || !item.light)
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
      alert('安排表更新成功！')
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

// 刪除安排表
const deleteSchedule = async (id) => {
  if (!confirm('確定要刪除此安排表嗎？')) {
    return
  }

  try {
    const response = await fetch(`${API_BASE_URL}/church/service-schedules/${id}`, {
      method: 'DELETE'
    })
    
    if (response.ok) {
      alert('刪除成功')
      await loadHistory() // 重新載入歷史記錄
    } else {
      alert('刪除失敗')
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

// 匯出安排表
const exportSchedule = () => {
  if (schedule.value.length === 0) return
  
  let csv = '日期,電腦/音控/燈光\n'
  schedule.value.forEach(item => {
    csv += `${item.date},${item.computer}/${item.sound}/${item.light}\n`
  })
  
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  const [startDate, endDate] = dateRange.value
  link.setAttribute('download', `服事安排表_${startDate}_${endDate}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 載入崗位配置
const loadPositionConfig = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/church/position-config/default`)
    if (response.ok) {
      const result = await response.json()
      console.log('載入崗位配置響應：', result)
      
      if (result.config && Object.keys(result.config).length > 0) {
        // 確保配置結構正確
        const config = result.config
        positionConfig.value = {
          computer: config.computer || { saturday: [], sunday: [] },
          sound: config.sound || { saturday: [], sunday: [] },
          light: config.light || { saturday: [], sunday: [] }
        }
        console.log('崗位配置載入成功：', positionConfig.value)
      } else {
        console.warn('崗位配置為空，使用默認空配置')
        // 保持空配置，用戶可以手動添加
      }
    } else {
      const errorText = await response.text()
      console.error('載入崗位配置失敗，HTTP 狀態：', response.status, errorText)
      // 如果載入失敗，使用空配置（用戶可以手動添加）
    }
  } catch (error) {
    console.error('載入崗位配置失敗：', error)
    // 如果載入失敗，使用空配置（用戶可以手動添加）
  }
}

// 保存崗位配置
const savePositionConfig = async () => {
  savingConfig.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/church/position-config/default`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        config: positionConfig.value
      })
    })

    const result = await response.json()
    
    if (response.ok) {
      alert('崗位配置保存成功！')
    } else {
      alert('保存失敗：' + (result.error || '未知錯誤'))
    }
  } catch (error) {
    alert('保存失敗：' + error.message)
  } finally {
    savingConfig.value = false
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

.btn-save-config {
  background: #28a745;
  color: white;
  padding: 0.5rem 1.5rem;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
}

.btn-save-config:hover:not(:disabled) {
  background: #218838;
}

.btn-save-config:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
