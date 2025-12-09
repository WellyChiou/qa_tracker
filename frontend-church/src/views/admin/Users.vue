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
                  <button @click="editRoles(user)" class="btn btn-roles">角色</button>
                  <button @click="editPermissions(user)" class="btn btn-permissions">權限</button>
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
    
    <!-- 角色分配模態框 -->
    <div v-if="showRolesModal" class="modal-overlay" @click="closeRolesModal">
      <div class="modal-panel" @click.stop style="max-width: 600px;">
        <div class="modal-header">
          <h2 class="modal-title">分配角色: {{ selectedUser?.displayName || selectedUser?.username }}</h2>
          <button class="btn-close" @click="closeRolesModal">×</button>
        </div>
        <div class="modal-body">
          <div class="roles-list">
            <label v-for="role in availableRoles" :key="role.id" class="role-item">
              <input 
                type="checkbox" 
                :value="role.id" 
                v-model="selectedRoleIds"
                class="checkbox-input" />
              <span>{{ role.roleName }}</span>
              <small v-if="role.description" class="role-description">{{ role.description }}</small>
            </label>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-primary" @click="saveRoles">儲存角色</button>
            <button type="button" class="btn btn-secondary" @click="closeRolesModal">取消</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 權限分配模態框 -->
    <div v-if="showPermissionsModal" class="modal-overlay" @click="closePermissionsModal">
      <div class="modal-panel" @click.stop style="max-width: 700px;">
        <div class="modal-header">
          <h2 class="modal-title">分配權限: {{ selectedUser?.displayName || selectedUser?.username }}</h2>
          <button class="btn-close" @click="closePermissionsModal">×</button>
        </div>
        <div class="modal-body">
          <div class="permissions-list">
            <label v-for="permission in availablePermissions" :key="permission.id" class="permission-item">
              <input 
                type="checkbox" 
                :value="permission.id" 
                v-model="selectedPermissionIds"
                class="checkbox-input" />
              <span>{{ permission.permissionName }} <code class="permission-code">({{ permission.permissionCode }})</code></span>
            </label>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-primary" @click="savePermissions">儲存權限</button>
            <button type="button" class="btn btn-secondary" @click="closePermissionsModal">取消</button>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import UserModal from '@/components/UserModal.vue'
import { apiRequest } from '@/utils/api'

const users = ref([])
const availableRoles = ref([])
const availablePermissions = ref([])
const showModal = ref(false)
const selectedUser = ref(null)
const showRolesModal = ref(false)
const showPermissionsModal = ref(false)
const selectedRoleIds = ref([])
const selectedPermissionIds = ref([])

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

const editRoles = async (user) => {
  selectedUser.value = user
  selectedRoleIds.value = user.roles ? user.roles.map(r => r.id) : []
  showRolesModal.value = true
}

const closeRolesModal = () => {
  showRolesModal.value = false
  selectedUser.value = null
  selectedRoleIds.value = []
}

const saveRoles = async () => {
  try {
    const response = await apiRequest(`/church/admin/users/${selectedUser.value.uid}/roles`, {
      method: 'POST',
      body: JSON.stringify({ roleIds: selectedRoleIds.value }),
      credentials: 'include'
    })
    
    if (response.ok) {
      closeRolesModal()
      loadUsers()
    } else {
      const data = await response.json()
      alert(data.message || data.error || '更新角色失敗')
    }
  } catch (error) {
    console.error('更新角色失敗:', error)
    alert('更新角色失敗: ' + error.message)
  }
}

const editPermissions = async (user) => {
  selectedUser.value = user
  selectedPermissionIds.value = user.permissions ? user.permissions.map(p => p.id) : []
  showPermissionsModal.value = true
}

const closePermissionsModal = () => {
  showPermissionsModal.value = false
  selectedUser.value = null
  selectedPermissionIds.value = []
}

const savePermissions = async () => {
  try {
    const response = await apiRequest(`/church/admin/users/${selectedUser.value.uid}/permissions`, {
      method: 'POST',
      body: JSON.stringify({ permissionIds: selectedPermissionIds.value }),
      credentials: 'include'
    })
    
    if (response.ok) {
      closePermissionsModal()
      loadUsers()
    } else {
      const data = await response.json()
      alert(data.message || data.error || '更新權限失敗')
    }
  } catch (error) {
    console.error('更新權限失敗:', error)
    alert('更新權限失敗: ' + error.message)
  }
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
  loadPermissions()
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

.btn-roles {
  background: #8b5cf6;
  color: white;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
}

.btn-roles:hover {
  background: #7c3aed;
}

.btn-permissions {
  background: #8b5cf6;
  color: white;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
}

.btn-permissions:hover {
  background: #7c3aed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-panel {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.modal-title {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  font-size: 2rem;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s;
}

.btn-close:hover {
  background: #f0f0f0;
}

.modal-body {
  padding: 1.5rem;
}

.roles-list,
.permissions-list {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 1.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  background: #f9fafb;
}

.role-item,
.permission-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  margin-bottom: 0.5rem;
  border-radius: 0.375rem;
  transition: all 0.2s;
  cursor: pointer;
  border: 1px solid transparent;
}

.role-item:hover,
.permission-item:hover {
  background: #f3f4f6;
  border-color: #e5e7eb;
}

.role-item:last-child,
.permission-item:last-child {
  margin-bottom: 0;
}

.checkbox-input {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
  accent-color: #667eea;
}

.role-description {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: normal;
  margin-left: 0.5rem;
}

.permission-code {
  font-size: 0.75rem;
  color: #6b7280;
  background: #e5e7eb;
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-family: 'Courier New', monospace;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e0e0e0;
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.btn-secondary:hover {
  background: #e0e0e0;
}
</style>

