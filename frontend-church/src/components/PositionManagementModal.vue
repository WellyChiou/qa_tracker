<template>
  <div v-if="show" class="modal-overlay" @click="closeModal">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">崗位管理</h2>
        <button class="btn-close" @click="closeModal">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <!-- 新增崗位按鈕 -->
        <div class="action-bar">
          <button @click="openCreatePositionModal" class="btn-add-position">
            + 新增崗位
          </button>
        </div>

        <!-- 崗位列表 -->
        <div class="position-list">
          <div 
            v-for="position in positions" 
            :key="position.id"
            class="position-item"
            @click="openPositionEdit(position)"
          >
            <div class="position-info">
              <h3>{{ position.positionName }}</h3>
              <p v-if="position.description" class="position-desc">{{ position.description }}</p>
              <div class="position-stats">
                <span>週六：{{ getPositionPersonCount(position.id, 'saturday') }} 人</span>
                <span>週日：{{ getPositionPersonCount(position.id, 'sunday') }} 人</span>
              </div>
            </div>
            <div class="position-actions">
              <button class="btn-edit-info" @click.stop="openEditPositionModal(position)" title="編輯崗位信息">
                ✏️
              </button>
              <button class="btn-edit-position" @click.stop="openPositionEdit(position)" title="編輯人員">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </button>
              <button class="btn-delete-position" @click.stop="deletePosition(position)" title="刪除崗位">
                🗑️
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 崗位編輯 Modal（編輯人員） -->
  <PositionEditModal 
    v-if="editingPosition"
    :position="editingPosition"
    :show="editingPosition !== null"
    @close="closePositionEdit"
    @updated="handlePositionUpdated"
  />

  <!-- 新增崗位 Modal -->
  <CreatePositionModal
    v-if="showCreatePositionModal"
    :show="showCreatePositionModal"
    @close="closeCreatePositionModal"
    @created="handlePositionCreated"
  />

  <!-- 編輯崗位信息 Modal -->
  <EditPositionModal
    v-if="showEditPositionModal && editingPositionInfo"
    :show="showEditPositionModal"
    :position="editingPositionInfo"
    @close="closeEditPositionModal"
    @updated="handlePositionInfoUpdated"
  />
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import PositionEditModal from './PositionEditModal.vue'
import CreatePositionModal from './CreatePositionModal.vue'
import EditPositionModal from './EditPositionModal.vue'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const positions = ref([])
const editingPosition = ref(null) // 用於編輯人員
const editingPositionInfo = ref(null) // 用於編輯崗位信息
const showCreatePositionModal = ref(false)
const showEditPositionModal = ref(false)
const positionPersonCounts = ref({}) // { positionId: { saturday: count, sunday: count } }

const loadPositions = async () => {
  try {
    console.log('請求崗位列表 URL: /church/positions/active')
    
    const response = await apiRequest('/church/positions/active', {
      method: 'GET'
    })
    
    const result = await response.json()
    positions.value = result.positions || []
    await loadPositionPersonCounts()
  } catch (error) {
    console.error('載入崗位列表失敗：', error)
    console.error('錯誤詳情:', error.message, error.stack)
  }
}

const loadPositionPersonCounts = async (positionId = null) => {
  // 如果指定了 positionId，只更新該崗位的人數
  const positionsToLoad = positionId 
    ? positions.value.filter(p => p.id === positionId)
    : positions.value
  
  for (const position of positionsToLoad) {
    try {
      const response = await apiRequest(`/church/positions/${position.id}/persons`, {
        method: 'GET'
      })
      
      if (response.ok) {
        const result = await response.json()
        positionPersonCounts.value[position.id] = {
          saturday: result.persons?.saturday?.length || 0,
          sunday: result.persons?.sunday?.length || 0
        }
      } else {
        console.error(`載入崗位 ${position.id} 人員數量失敗，HTTP 狀態:`, response.status)
      }
    } catch (error) {
      console.error(`載入崗位 ${position.id} 人員數量失敗：`, error)
    }
  }
}

const getPositionPersonCount = (positionId, dayType) => {
  return positionPersonCounts.value[positionId]?.[dayType] || 0
}

const openPositionEdit = (position) => {
  editingPosition.value = position
}

const closePositionEdit = () => {
  const positionId = editingPosition.value?.id
  editingPosition.value = null
  // 只更新被編輯的崗位的人數，而不是所有崗位
  if (positionId) {
    loadPositionPersonCounts(positionId)
  }
}

const handlePositionUpdated = (positionId = null) => {
  // 如果傳入了 positionId，只更新該崗位的人數
  // 否則更新所有崗位（用於批量更新場景）
  loadPositionPersonCounts(positionId)
}

const openCreatePositionModal = () => {
  showCreatePositionModal.value = true
}

const closeCreatePositionModal = () => {
  showCreatePositionModal.value = false
}

const handlePositionCreated = async () => {
  await loadPositions()
  closeCreatePositionModal()
}

const openEditPositionModal = (position) => {
  console.log('打開編輯崗位 Modal，position:', position)
  // 深拷貝 position 對象，確保資料正確傳遞
  editingPositionInfo.value = { ...position }
  showEditPositionModal.value = true
  console.log('editingPositionInfo.value:', editingPositionInfo.value)
}

const closeEditPositionModal = () => {
  showEditPositionModal.value = false
  editingPositionInfo.value = null
}

const handlePositionInfoUpdated = async () => {
  await loadPositions()
  closeEditPositionModal()
}

const deletePosition = async (position) => {
  if (!confirm(`確定要刪除崗位「${position.positionName}」嗎？\n\n注意：刪除崗位會同時刪除該崗位的所有人員關聯。`)) {
    return
  }

  try {
    const response = await apiRequest(`/church/positions/${position.id}`, {
      method: 'DELETE'
    })

    const result = await response.json()
    
    if (response.ok && result.success !== false) {
      await loadPositions()
    } else {
      console.error('刪除失敗響應：', result)
      alert('刪除失敗：' + (result.error || '未知錯誤'))
    }
  } catch (error) {
    console.error('刪除崗位失敗：', error)
    alert('刪除失敗：' + (error.message || '網絡錯誤'))
  }
}

const closeModal = () => {
  emit('close')
}

watch(() => props.show, (newVal) => {
  if (newVal) {
    loadPositions()
  }
})

onMounted(() => {
  if (props.show) {
    loadPositions()
  }
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-panel {
  width: 100%;
  max-width: 800px;
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

.action-bar {
  margin-bottom: 1.5rem;
  display: flex;
  justify-content: flex-end;
}

.btn-add-position {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-add-position:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.4);
}

.position-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.position-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem;
  background: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.position-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.position-item:hover {
  background: #f1f5f9;
  border-color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.position-info {
  flex: 1;
}

.position-info h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 0.5rem 0;
}

.position-desc {
  color: #64748b;
  font-size: 0.9rem;
  margin: 0 0 0.75rem 0;
}

.position-stats {
  display: flex;
  gap: 1rem;
  font-size: 0.875rem;
  color: #475569;
}

.btn-edit-info, .btn-edit-position, .btn-delete-position {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.375rem;
  transition: all 0.2s;
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-edit-info {
  color: #007bff;
}

.btn-edit-info:hover {
  background: #e7f3ff;
}

.btn-edit-position {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-edit-position:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-delete-position {
  color: #dc3545;
}

.btn-delete-position:hover {
  background: #f8d7da;
}

.w-4 {
  width: 1rem;
  height: 1rem;
}

.w-5 {
  width: 1.25rem;
  height: 1.25rem;
}
</style>

