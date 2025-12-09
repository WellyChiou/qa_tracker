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
                  <button @click="editPermissions(role)" class="btn btn-permissions">權限</button>
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
    
    <!-- 權限管理模態框 -->
    <div v-if="showPermissionsModal" class="modal-overlay" @click="closePermissionsModal">
      <div class="modal-panel" @click.stop style="max-width: 700px;">
        <div class="modal-header">
          <h2 class="modal-title">管理角色權限: {{ selectedRole?.roleName }}</h2>
          <button class="btn-close" @click="closePermissionsModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="permissions-list">
            <label v-for="permission in availablePermissions" :key="permission.id" class="permission-item">
              <input type="checkbox" 
                :value="permission.id" 
                v-model="selectedPermissionIds"
                class="checkbox-input" />
              <span>{{ permission.permissionName }} <code class="permission-code">({{ permission.permissionCode }})</code></span>
            </label>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-primary" @click="savePermissions">
              <i class="fas fa-save me-2"></i>儲存權限
            </button>
            <button type="button" class="btn btn-secondary" @click="closePermissionsModal">
              <i class="fas fa-times me-2"></i>取消
            </button>
          </div>
        </div>
      </div>
    </div>
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
const showPermissionsModal = ref(false)
const selectedPermissionIds = ref([])

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

const editPermissions = async (role) => {
  selectedRole.value = role
  selectedPermissionIds.value = role.permissions ? role.permissions.map(p => p.id) : []
  showPermissionsModal.value = true
}

const savePermissions = async () => {
  try {
    const response = await apiRequest(`/church/admin/roles/${selectedRole.value.id}/permissions`, {
      method: 'POST',
      body: JSON.stringify({ permissionIds: selectedPermissionIds.value }),
      credentials: 'include'
    })
    
    if (response.ok) {
      closePermissionsModal()
      loadRoles()
    } else {
      const data = await response.json()
      alert(data.message || data.error || '更新權限失敗')
    }
  } catch (error) {
    console.error('更新權限失敗:', error)
    alert('更新權限失敗: ' + error.message)
  }
}

const closePermissionsModal = () => {
  showPermissionsModal.value = false
  selectedRole.value = null
  selectedPermissionIds.value = []
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
  font-size: 1.5rem;
  color: #666;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 4px;
  transition: background 0.2s;
}

.btn-close:hover {
  background: #f0f0f0;
}

.modal-body {
  padding: 1.5rem;
}

.permissions-list {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 1.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  background: #f9fafb;
}

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

.permission-item:hover {
  background: #f3f4f6;
  border-color: #e5e7eb;
}

.permission-item:last-child {
  margin-bottom: 0;
}

.checkbox-input {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
  accent-color: #667eea;
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

.me-2 {
  margin-right: 0.5rem;
}
</style>

