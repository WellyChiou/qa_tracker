<template>
  <AdminLayout>
    <div class="admin-roles">
      <div class="page-header">
        <h2>角色管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增角色</button>
      </div>

      <div class="roles-list">
        <div v-if="roles.length === 0" class="empty-state">
          <p>尚無角色資料</p>
        </div>
        <div v-else class="roles-table">
          <table>
            <thead>
              <tr>
                <th>角色名稱</th>
                <th>描述</th>
                <th>權限數量</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="role in roles" :key="role.id">
                <td>{{ role.roleName }}</td>
                <td>{{ role.description || '-' }}</td>
                <td>{{ role.permissions ? role.permissions.length : 0 }}</td>
                <td>
                  <button @click="editRole(role.id)" class="btn btn-edit">編輯</button>
                  <button @click="deleteRole(role.id)" class="btn btn-delete">刪除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    
    <RoleModal
      :show="showModal"
      :role="selectedRole"
      :available-permissions="availablePermissions"
      @close="closeModal"
      @saved="handleSaved"
    />
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import RoleModal from '@/components/RoleModal.vue'
import { apiRequest } from '@/utils/api'

const roles = ref([])
const availablePermissions = ref([])
const showModal = ref(false)
const selectedRole = ref(null)

const loadRoles = async () => {
  try {
    const response = await apiRequest('/church/admin/roles', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      roles.value = data.roles || data || []
    }
  } catch (error) {
    console.error('載入角色失敗:', error)
  }
}

const loadPermissions = async () => {
  try {
    const response = await apiRequest('/church/admin/permissions', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      availablePermissions.value = data.permissions || data || []
    }
  } catch (error) {
    console.error('載入權限失敗:', error)
  }
}

const openAddModal = () => {
  selectedRole.value = null
  showModal.value = true
}

const editRole = async (id) => {
  try {
    const response = await apiRequest(`/church/admin/roles/${id}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      selectedRole.value = data.role || data
      showModal.value = true
    } else {
      alert('載入角色資料失敗')
    }
  } catch (error) {
    console.error('載入角色資料失敗:', error)
    alert('載入角色資料失敗: ' + error.message)
  }
}

const closeModal = () => {
  showModal.value = false
  selectedRole.value = null
}

const handleSaved = () => {
  loadRoles()
}

const deleteRole = async (id) => {
  if (!confirm('確定要刪除此角色嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/admin/roles/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadRoles()
    } else {
      alert('刪除失敗')
    }
  } catch (error) {
    console.error('刪除角色失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.admin-roles {
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

.roles-list {
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

.roles-table {
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

