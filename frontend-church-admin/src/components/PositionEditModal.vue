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
        <button v-if="isDirty" @click="saveChanges" class="btn btn-primary" :disabled="saving">
          <span v-if="saving" class="spinner" aria-hidden="true"></span>
          {{ saving ? "儲存中..." : "儲存變更" }}
        </button>
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
import { toast } from '@/composables/useToast'
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
const originalSaturdayPersons = ref([])
const originalSundayPersons = ref([])
const saving = ref(false)

const clone = (v) => JSON.parse(JSON.stringify(v || []))
const isDirty = computed(() => {
  return JSON.stringify(saturdayPersons.value || []) !== JSON.stringify(originalSaturdayPersons.value || []) ||
         JSON.stringify(sundayPersons.value || []) !== JSON.stringify(originalSundayPersons.value || [])
})
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
    originalSaturdayPersons.value = clone(saturdayPersons.value)
    originalSundayPersons.value = clone(sundayPersons.value)
  } catch (error) {
    console.error('載入崗位人員失敗：', error)
    toast.error('載入崗位人員失敗：' + error.message)
  }
}

const updateIncludeInAutoSchedule = (dayType, person, includeInAutoSchedule) => {
  const list = dayType === 'saturday' ? saturdayPersons.value : sundayPersons.value
  const idx = list.findIndex(p => p.id === person.id)
  if (idx >= 0) {
    list[idx].includeInAutoSchedule = includeInAutoSchedule
  }
}

const removePerson = (dayType, person) => {
  const list = dayType === 'saturday' ? saturdayPersons.value : sundayPersons.value
  const idx = list.findIndex(p => p.personId === person.personId)
  if (idx >= 0) list.splice(idx, 1)
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


const saveChanges = async () => {
  if (!isDirty.value || !props.position?.id) return
  saving.value = true
  try {
    const positionId = props.position.id

    const syncDay = async (dayType, draftList, origList) => {
      const draftIds = new Set((draftList || []).map(p => p.personId))
      const origIds = new Set((origList || []).map(p => p.personId))

      // 新增
      for (const p of (draftList || [])) {
        if (!origIds.has(p.personId)) {
          await apiRequest(`/church/positions/${positionId}/persons/${p.personId}?dayType=${dayType}`, { method: 'POST' })
        }
      }

      // 移除
      for (const p of (origList || [])) {
        if (!draftIds.has(p.personId)) {
          await apiRequest(`/church/positions/${positionId}/persons/${p.personId}?dayType=${dayType}`, { method: 'DELETE' })
        }
      }
    }

    await syncDay('saturday', saturdayPersons.value, originalSaturdayPersons.value)
    await syncDay('sunday', sundayPersons.value, originalSundayPersons.value)

    // includeInAutoSchedule（只對已存在的關係 id 生效）
    const updateInclude = async (draftList, origList) => {
      const origById = new Map((origList || []).filter(p => p.id).map(p => [p.id, p]))
      for (const p of (draftList || [])) {
        if (!p.id) continue
        const o = origById.get(p.id)
        if (o && o.includeInAutoSchedule !== p.includeInAutoSchedule) {
          await apiRequest(`/church/positions/position-persons/${p.id}/include-in-auto-schedule`, {
            method: 'PATCH',
            body: JSON.stringify({ includeInAutoSchedule: p.includeInAutoSchedule }),
            credentials: 'include'
          })
        }
      }
    }
    await updateInclude(saturdayPersons.value, originalSaturdayPersons.value)
    await updateInclude(sundayPersons.value, originalSundayPersons.value)

    toast.success('人員設定已儲存')
    await loadPositionPersons()
    closeModal()
  } catch (e) {
    console.error(e)
    toast.error('儲存失敗：' + (e?.message || '未知錯誤'))
  } finally {
    saving.value = false
  }
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

/* Improved scroll shadows (prevents fixed shadow overlay issue) */
.modal-body{
  overflow:auto;
  background:
    linear-gradient(#fff 30%, rgba(255,255,255,0)),
    linear-gradient(rgba(255,255,255,0), #fff 70%),
    radial-gradient(farthest-side at 50% 0, rgba(2,6,23,.16), rgba(2,6,23,0)),
    radial-gradient(farthest-side at 50% 100%, rgba(2,6,23,.16), rgba(2,6,23,0));
  background-repeat:no-repeat;
  background-size:100% 40px, 100% 40px, 100% 14px, 100% 14px;
  background-attachment:local, local, scroll, scroll;
  background-position:0 0, 0 100%, 0 0, 0 100%;
}
</style>
