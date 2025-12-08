<template>
  <AdminLayout>
    <div class="admin-positions">
      <div class="page-header">
        <h2>崗位管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增崗位</button>
      </div>

      <div class="positions-list">
        <div v-if="positions.length === 0" class="empty-state">
          <p>尚無崗位資料</p>
        </div>
        <div v-else class="positions-table">
          <table>
            <thead>
              <tr>
                <th>崗位代碼</th>
                <th>崗位名稱</th>
                <th>是否啟用</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="position in positions" :key="position.id">
                <td>{{ position.positionCode }}</td>
                <td>{{ position.positionName }}</td>
                <td>{{ position.isActive ? '是' : '否' }}</td>
                <td>
                  <button @click="editPosition(position.id)" class="btn btn-edit">編輯</button>
                  <button @click="deletePosition(position.id)" class="btn btn-delete">刪除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 新增崗位 Modal -->
      <PositionManagementModal
        v-if="showModal && !editingPosition"
        :show="showModal"
        @close="closeModal"
        @saved="handleSaved"
      />

      <!-- 編輯崗位 Modal -->
      <EditPositionModal
        v-if="editingPosition"
        :show="!!editingPosition"
        :position="editingPosition"
        @close="closeEditModal"
        @updated="handleUpdated"
      />
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import PositionManagementModal from '@/components/PositionManagementModal.vue'
import EditPositionModal from '@/components/EditPositionModal.vue'
import { apiRequest } from '@/utils/api'

const positions = ref([])
const showModal = ref(false)
const editingPosition = ref(null)

const loadPositions = async () => {
  try {
    const response = await apiRequest('/church/positions', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      // 後端返回格式：{ "positions": [...], "message": "..." }
      positions.value = data.positions || data || []
    }
  } catch (error) {
    console.error('載入崗位失敗:', error)
  }
}

const openAddModal = () => {
  showModal.value = true
}

const editPosition = (id) => {
  // 找到要編輯的崗位
  const position = positions.value.find(p => p.id === id)
  if (position) {
    editingPosition.value = position
  }
}

const closeEditModal = () => {
  editingPosition.value = null
}

const handleUpdated = () => {
  loadPositions()
  closeEditModal()
}

const closeModal = () => {
  showModal.value = false
}

const handleSaved = () => {
  loadPositions()
  closeModal()
}

const deletePosition = async (id) => {
  if (!confirm('確定要刪除此崗位嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/positions/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadPositions()
    } else {
      alert('刪除失敗')
    }
  } catch (error) {
    console.error('刪除崗位失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadPositions()
})
</script>

<style scoped>
.admin-positions {
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

.positions-list {
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

.positions-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f5f5f5;
}

th, td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  font-weight: 600;
  color: #333;
}

.btn-edit {
  background: #667eea;
  color: white;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
}

.btn-edit:hover {
  background: #5568d3;
}

.btn-delete {
  background: #ef4444;
  color: white;
  padding: 0.5rem 1rem;
}

.btn-delete:hover {
  background: #dc2626;
}
</style>

