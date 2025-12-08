<template>
  <AdminLayout>
    <div class="admin-service-schedule">
      <div class="page-header">
        <h2>服事表管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增服事表</button>
      </div>

      <div class="schedule-list">
        <div v-if="schedules.length === 0" class="empty-state">
          <p>尚無服事表</p>
        </div>
        <div v-else class="schedule-grid">
          <div v-for="schedule in schedules" :key="schedule.id" class="schedule-card">
            <div class="schedule-info">
              <h3>{{ schedule.name || '未命名' }}</h3>
              <p class="schedule-date" v-if="schedule.startDate && schedule.endDate">
                {{ formatDate(schedule.startDate) }} ~ {{ formatDate(schedule.endDate) }}
              </p>
              <p class="schedule-time" v-if="schedule.createdAt">
                建立時間：{{ formatDateTime(schedule.createdAt) }}
              </p>
            </div>
            <div class="schedule-actions">
              <button @click="editSchedule(schedule.id)" class="btn btn-edit">編輯</button>
              <button @click="deleteSchedule(schedule.id)" class="btn btn-delete">刪除</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 服事表 Modal（重用現有的 ServiceScheduleModal） -->
      <ServiceScheduleModal
        v-if="showModal"
        :show="showModal"
        :mode="modalMode"
        :schedule-id="currentScheduleId"
        :position-config="positionConfig"
        @close="closeModal"
        @saved="handleSaved"
        @updated="handleUpdated"
      />
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import ServiceScheduleModal from '@/components/ServiceScheduleModal.vue'
import { apiRequest } from '@/utils/api'

const schedules = ref([])
const showModal = ref(false)
const modalMode = ref('add')
const currentScheduleId = ref(null)
const positionConfig = ref({})

const loadSchedules = async () => {
  try {
    const response = await apiRequest('/church/service-schedules', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      schedules.value = await response.json()
    }
  } catch (error) {
    console.error('載入服事表失敗:', error)
  }
}

const loadPositionConfig = async () => {
  try {
    const response = await apiRequest('/church/positions/config/full', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      positionConfig.value = data || {}
    }
  } catch (error) {
    console.error('載入崗位配置失敗:', error)
  }
}

const openAddModal = () => {
  modalMode.value = 'add'
  currentScheduleId.value = null
  showModal.value = true
}

const editSchedule = (id) => {
  modalMode.value = 'edit'
  currentScheduleId.value = id
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  currentScheduleId.value = null
}

const handleSaved = () => {
  loadSchedules()
  closeModal()
}

const handleUpdated = () => {
  loadSchedules()
  closeModal()
}

const deleteSchedule = async (id) => {
  if (!confirm('確定要刪除此服事表嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/service-schedules/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadSchedules()
    } else {
      alert('刪除失敗')
    }
  } catch (error) {
    console.error('刪除服事表失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  loadSchedules()
  loadPositionConfig()
})
</script>

<style scoped>
.admin-service-schedule {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h2 {
  margin: 0;
  font-size: 1.8rem;
  color: #333;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.schedule-list {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.schedule-card {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.schedule-info h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.2rem;
  color: #333;
}

.schedule-date {
  color: #666;
  font-size: 0.9rem;
  margin: 0.25rem 0;
}

.schedule-time {
  color: #999;
  font-size: 0.85rem;
  margin: 0.25rem 0;
}

.schedule-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: auto;
}

.btn-edit {
  flex: 1;
  background: #667eea;
  color: white;
}

.btn-edit:hover {
  background: #5568d3;
}

.btn-delete {
  flex: 1;
  background: #ef4444;
  color: white;
}

.btn-delete:hover {
  background: #dc2626;
}
</style>

