<template>
  <div v-if="show" class="modal-overlay" @click="closeModal">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">{{ readonly ? '查看崗位：' : '編輯崗位：' }}{{ position?.positionName }}</h2>
        <button class="btn-close" @click="closeModal">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <!-- 週六人員 -->
        <div class="day-section">
          <h3 class="day-title">週六</h3>
          <div class="person-list">
            <div 
              v-for="(person, index) in saturdayPersons" 
              :key="person.id || index"
              class="person-item"
            >
              <span class="person-name">{{ person.personName || person.displayName }}</span>
              <div v-if="!readonly" class="person-controls">
                <label class="auto-schedule-toggle">
                  <input 
                    type="checkbox" 
                    :checked="person.includeInAutoSchedule !== false"
                    @change="updateIncludeInAutoSchedule('saturday', person, $event.target.checked)"
                    class="toggle-checkbox"
                  />
                  <span class="toggle-label">參與自動分配</span>
                </label>
              </div>
              <div v-if="!readonly" class="person-actions">
                <button @click="removePerson('saturday', person)" class="btn-remove" title="移除">×</button>
              </div>
            </div>
            <button v-if="!readonly" @click="openAddPersonModal('saturday')" class="btn-add-person">
              + 添加人員
            </button>
          </div>
        </div>

        <!-- 週日人員 -->
        <div class="day-section">
          <h3 class="day-title">週日</h3>
          <div class="person-list">
            <div 
              v-for="(person, index) in sundayPersons" 
              :key="person.id || index"
              class="person-item"
            >
              <span class="person-name">{{ person.personName || person.displayName }}</span>
              <div v-if="!readonly" class="person-controls">
                <label class="auto-schedule-toggle">
                  <input 
                    type="checkbox" 
                    :checked="person.includeInAutoSchedule !== false"
                    @change="updateIncludeInAutoSchedule('sunday', person, $event.target.checked)"
                    class="toggle-checkbox"
                  />
                  <span class="toggle-label">參與自動分配</span>
                </label>
              </div>
              <div v-if="!readonly" class="person-actions">
                <button @click="removePerson('sunday', person)" class="btn-remove" title="移除">×</button>
              </div>
            </div>
            <button v-if="!readonly" @click="openAddPersonModal('sunday')" class="btn-add-person">
              + 添加人員
            </button>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button @click="closeModal" class="btn btn-secondary">關閉</button>
      </div>
    </div>
  </div>

  <!-- 添加人員 Modal -->
  <AddPersonModal
    v-if="showAddPersonModal"
    :show="showAddPersonModal"
    :dayType="addingDayType"
    :positionId="position?.id"
    :existingPersonIds="getExistingPersonIds(addingDayType)"
    @close="closeAddPersonModal"
    @added="handlePersonAdded"
  />
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import AddPersonModal from './AddPersonModal.vue'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  position: {
    type: Object,
    required: true
  },
  show: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'updated'])

const saturdayPersons = ref([])
const sundayPersons = ref([])
const showAddPersonModal = ref(false)
const addingDayType = ref(null)

// 根據 dayType 計算已存在的人員 ID（只包含當前要添加的 dayType）
const getExistingPersonIds = (dayType) => {
  const persons = dayType === 'saturday' ? saturdayPersons.value : sundayPersons.value
  return persons.map(p => p.personId).filter(id => id != null)
}

const loadPositionPersons = async () => {
  if (!props.position?.id) return
  
  try {
    const response = await apiRequest(`/church/positions/${props.position.id}/persons`, {
      method: 'GET'
    })
    const result = await response.json()
    saturdayPersons.value = result.persons?.saturday || []
    sundayPersons.value = result.persons?.sunday || []
  } catch (error) {
    console.error('載入崗位人員失敗：', error)
    alert('載入崗位人員失敗：' + error.message)
  }
}

const updateIncludeInAutoSchedule = async (dayType, person, includeInAutoSchedule) => {
  if (!person.id) {
    console.error('人員 ID 不存在')
    return
  }

  try {
    const response = await apiRequest(
      `/church/positions/position-persons/${person.id}/include-in-auto-schedule`,
      {
        method: 'PUT',
        body: JSON.stringify({ includeInAutoSchedule })
      },
      '更新設定中...'
    )
    
    const result = await response.json()
    
    if (response.ok && result.success !== false) {
      // 更新本地狀態
      if (dayType === 'saturday') {
        const index = saturdayPersons.value.findIndex(p => p.id === person.id)
        if (index !== -1) {
          saturdayPersons.value[index].includeInAutoSchedule = includeInAutoSchedule
        }
      } else {
        const index = sundayPersons.value.findIndex(p => p.id === person.id)
        if (index !== -1) {
          sundayPersons.value[index].includeInAutoSchedule = includeInAutoSchedule
        }
      }
      // 傳遞 position.id 給父組件，讓它只更新該崗位的人數
      emit('updated', props.position?.id)
    } else {
      console.error('更新失敗響應：', result)
      alert('更新失敗：' + (result.error || '未知錯誤'))
      // 恢復 checkbox 狀態
      await loadPositionPersons()
    }
  } catch (error) {
    console.error('更新設定失敗：', error)
    alert('更新失敗：' + (error.message || '網絡錯誤'))
    // 恢復 checkbox 狀態
    await loadPositionPersons()
  }
}

const removePerson = async (dayType, person) => {
  if (!confirm(`確定要從${dayType === 'saturday' ? '週六' : '週日'}移除 ${person.personName || person.displayName} 嗎？`)) {
    return
  }

  try {
    const response = await apiRequest(
      `/church/positions/${props.position.id}/persons/${person.personId}?dayType=${dayType}`,
      { method: 'DELETE' },
      '移除人員中...'
    )
    
    const result = await response.json()
    
    if (response.ok && result.success !== false) {
      await loadPositionPersons()
      // 傳遞 position.id 給父組件，讓它只更新該崗位的人數
      emit('updated', props.position?.id)
    } else {
      console.error('移除失敗響應：', result)
      alert('移除失敗：' + (result.error || '未知錯誤'))
    }
  } catch (error) {
    console.error('移除人員失敗：', error)
    alert('移除失敗：' + (error.message || '網絡錯誤'))
  }
}

const openAddPersonModal = (dayType) => {
  addingDayType.value = dayType
  showAddPersonModal.value = true
}

const closeAddPersonModal = () => {
  showAddPersonModal.value = false
  addingDayType.value = null
}

const handlePersonAdded = async () => {
  await loadPositionPersons()
  // 傳遞 position.id 給父組件，讓它只更新該崗位的人數
  emit('updated', props.position?.id)
}

const closeModal = () => {
  emit('close')
}

// 使用 watch 來監聽 show 和 position 的變化，避免 onMounted 和 watch 重複調用
watch([() => props.show, () => props.position?.id], ([newShow, newPositionId]) => {
  if (newShow && newPositionId) {
    loadPositionPersons()
  }
}, { immediate: true })
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-panel {
  width: 100%;
  max-width: 700px;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(to right, #f8fafc, white);
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  color: #64748b;
  transition: color 0.2s;
}

.btn-close:hover {
  color: #1e293b;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.day-section {
  margin-bottom: 2rem;
}

.day-section:last-child {
  margin-bottom: 0;
}

.day-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #667eea;
  margin: 0 0 1rem 0;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #e2e8f0;
}

.person-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.person-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 0.5rem;
  transition: all 0.2s;
}

.person-item:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.person-name {
  font-size: 1rem;
  color: #1e293b;
  flex: 1;
}

.person-controls {
  display: flex;
  align-items: center;
  margin-right: 1rem;
}

.auto-schedule-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  user-select: none;
}

.toggle-checkbox {
  width: 1.125rem;
  height: 1.125rem;
  accent-color: #667eea;
  cursor: pointer;
}

.toggle-label {
  font-size: 0.875rem;
  color: #64748b;
}

.person-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 0.5rem;
  transition: all 0.2s;
  font-weight: 500;
}

.person-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-remove {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.375rem;
  border-radius: 0.375rem;
  transition: all 0.2s;
  font-size: 1rem;
  color: #dc3545;
}

.btn-remove:hover {
  background: #f8d7da;
}

.btn-add-person {
  padding: 0.875rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.2s;
  width: 100%;
}

.btn-add-person:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.modal-footer {
  padding: 1.5rem;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
  transform: translateY(-2px);
}

.w-4 {
  width: 1rem;
  height: 1rem;
}
</style>

