<template>
  <AdminLayout>
    <div class="admin-users">
      <div class="page-header">
        <h2>用戶管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增用戶</button>
      </div>

      <div class="users-list">
        <div v-if="users.length === 0" class="empty-state">
          <p>尚無用戶資料</p>
        </div>
        <div v-else class="users-table">
          <table>
            <thead>
              <tr>
                <th>用戶名</th>
                <th>顯示名稱</th>
                <th>電子郵件</th>
                <th>角色</th>
                <th>狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.uid">
                <td>{{ user.username }}</td>
                <td>{{ user.displayName || '-' }}</td>
                <td>{{ user.email || '-' }}</td>
                <td>
                  <span v-for="role in user.roles" :key="role.id" class="role-badge">
                    {{ role.roleName }}
                  </span>
                  <span v-if="!user.roles || user.roles.length === 0">-</span>
                </td>
                <td>
                  <span :class="user.isEnabled ? 'status-active' : 'status-inactive'">
                    {{ user.isEnabled ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>
                  <button @click="editUser(user.uid)" class="btn btn-edit">編輯</button>
                  <button @click="deleteUser(user.uid)" class="btn btn-delete">刪除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    
    <UserModal
      :show="showModal"
      :user="selectedUser"
      :available-roles="availableRoles"
      @close="closeModal"
      @saved="handleSaved"
    />
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import UserModal from '@/components/UserModal.vue'
import { apiRequest } from '@/utils/api'

const users = ref([])
const availableRoles = ref([])
const showModal = ref(false)
const selectedUser = ref(null)

const loadUsers = async () => {
  try {
    const response = await apiRequest('/church/admin/users', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      users.value = data.users || data || []
    }
  } catch (error) {
    console.error('載入用戶失敗:', error)
  }
}

const loadRoles = async () => {
  try {
    const response = await apiRequest('/church/admin/roles', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      availableRoles.value = data.roles || data || []
    }
  } catch (error) {
    console.error('載入角色失敗:', error)
  }
}

const openAddModal = () => {
  selectedUser.value = null
  showModal.value = true
}

const editUser = async (uid) => {
  try {
    const response = await apiRequest(`/church/admin/users/${uid}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      selectedUser.value = data.user || data
      showModal.value = true
    } else {
      alert('載入用戶資料失敗')
    }
  } catch (error) {
    console.error('載入用戶資料失敗:', error)
    alert('載入用戶資料失敗: ' + error.message)
  }
}

const closeModal = () => {
  showModal.value = false
  selectedUser.value = null
}

const handleSaved = () => {
  loadUsers()
}

const deleteUser = async (uid) => {
  if (!confirm('確定要刪除此用戶嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/admin/users/${uid}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadUsers()
    } else {
      alert('刪除失敗')
    }
  } catch (error) {
    console.error('刪除用戶失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadUsers()
  loadRoles()
})
</script>

<style scoped>
.admin-users {
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

.users-list {
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

.users-table {
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

.role-badge {
  display: inline-block;
  background: #667eea;
  color: white;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.85rem;
  margin-right: 0.25rem;
}

.status-active {
  color: #10b981;
  font-weight: 600;
}

.status-inactive {
  color: #ef4444;
  font-weight: 600;
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

