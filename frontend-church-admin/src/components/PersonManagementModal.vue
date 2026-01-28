<template>
  <div v-if="show" class="modal-overlay" @click="closeModal">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">人員管理</h2>
        <button class="btn-close" @click="closeModal">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <div class="person-list-header">
          <button @click="openCreateModal" class="btn btn-primary">
            + 新增人員
          </button>
          <button @click="loadPersons" class="btn btn-refresh" :disabled="loading">
            {{ loading ? '載入中...' : '重新載入' }}
          </button>
        </div>

        <div v-if="loading" class="loading-state">
          <p>載入中...</p>
        </div>
        <div v-else-if="persons.length === 0" class="empty-state">
          <p>尚無人員資料</p>
        </div>
        <div v-else class="person-list">
          <table class="person-table">
            <thead>
              <tr>
                <th>姓名</th>
                <th>顯示名稱</th>
                <th>電話</th>
                <th>電子郵件</th>
                <th>狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="person in persons" :key="person.id" :class="{ 'inactive': !person.isActive }">
                <td>{{ person.personName }}</td>
                <td>{{ person.displayName || '-' }}</td>
                <td>{{ person.phone || '-' }}</td>
                <td>{{ person.email || '-' }}</td>
                <td>
                  <span :class="['status-badge', person.isActive ? 'active' : 'inactive']">
                    {{ person.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>
                  <div class="action-buttons">
                    <button @click="openEditModal(person)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                    <button @click="deletePerson(person)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 新增人員 Modal -->
    <CreatePersonModal
      :show="showCreateModal"
      @close="closeCreateModal"
      @created="handlePersonCreated"
    />

    <!-- 編輯人員 Modal -->
    <EditPersonModal
      :show="showEditModal"
      :person="selectedPerson"
      @close="closeEditModal"
      @updated="handlePersonUpdated"
    />
  </div>
</template>

<script setup>
import { toast } from '@shared/composables/useToast'
import { ref, onMounted, watch } from 'vue'
import { apiRequest } from '@/utils/api'
import CreatePersonModal from './CreatePersonModal.vue'
import EditPersonModal from './EditPersonModal.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const loading = ref(false)
const persons = ref([])
const showCreateModal = ref(false)
const showEditModal = ref(false)
const selectedPerson = ref(null)

// 載入人員清單
const loadPersons = async () => {
  loading.value = true
  try {
    const data = await apiRequest('/church/persons', {
      method: 'GET'
    })
    
    if (data) {
      // 處理 PageResponse 格式或直接列表
      const personsData = data.content || data.persons || data || []
      persons.value = Array.isArray(personsData) ? personsData : []
    }
  } catch (error) {
    console.error('載入人員清單失敗：', error)
  } finally {
    loading.value = false
  }
}

// 打開新增 Modal
const openCreateModal = () => {
  showCreateModal.value = true
}

// 關閉新增 Modal
const closeCreateModal = () => {
  showCreateModal.value = false
}

// 處理人員新增
const handlePersonCreated = () => {
  loadPersons()
}

// 打開編輯 Modal
const openEditModal = (person) => {
  // 深拷貝 person 對象，確保資料正確傳遞
  selectedPerson.value = JSON.parse(JSON.stringify(person))
  showEditModal.value = true
}

// 關閉編輯 Modal
const closeEditModal = () => {
  selectedPerson.value = null
  showEditModal.value = false
}

// 處理人員更新
const handlePersonUpdated = () => {
  loadPersons()
}

// 刪除人員
const deletePerson = async (person) => {
  if (!confirm(`確定要刪除人員「${person.personName}」嗎？`)) {
    return
  }

  try {
    const result = await apiRequest(`/church/persons/${person.id}`, {
      method: 'DELETE'
    })
    
    if (result !== null) {
      await loadPersons()
      toast.success('刪除成功')
    } else {
      toast.error('刪除失敗：' + (result.error || '未知錯誤'))
    }
  } catch (error) {
    toast.error('刪除失敗：' + error.message)
  }
}

// 關閉 Modal
const closeModal = () => {
  emit('close')
}

// 當 Modal 顯示時載入人員清單
watch(() => props.show, (newVal) => {
  if (newVal) {
    loadPersons()
  }
})

onMounted(() => {
  if (props.show) {
    loadPersons()
  }
})
</script>

<style scoped>

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1300;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-panel {
  width: 100%;
  max-width: 900px;
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

.person-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  gap: 1rem;
}

.person-list {
  overflow-x: auto;
}

.person-table {
  width: 100%;
  border-collapse: collapse;
}

.person-table thead {
  background: #f8f9fa;
}

.person-table th {
  padding: 0.75rem;
  text-align: left;
  font-weight: 600;
  color: #374151;
  border-bottom: 2px solid #e2e8f0;
  white-space: nowrap;
}

.person-table td {
  padding: 0.75rem;
  border-bottom: 1px solid #e2e8f0;
}

.person-table tbody tr:hover {
  background: #f8f9fa;
}

.person-table tbody tr.inactive {
  opacity: 0.6;
}

.status-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 500;
}

.status-badge.active {
  background: #d4edda;
  color: #155724;
}

.status-badge.inactive {
  background: #f8d7da;
  color: #721c24;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-refresh {
  background: #17a2b8;
  color: white;
}

.btn-refresh:hover:not(:disabled) {
  background: #138496;
}

.btn-edit {
  background: #ffc107;
  color: #000;
}

.btn-edit:hover {
  background: #e0a800;
}

.btn-delete {
  background: #dc3545;
  color: white;
}

.btn-delete:hover {
  background: #c82333;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 3rem;
  color: #6c757d;
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

