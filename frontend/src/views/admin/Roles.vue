<template>
  <div class="admin-page">
    <header class="header">
      <h1>🔐 角色管理</h1>
      <button class="btn btn-primary" @click="showAddModal = true">新增角色</button>
    </header>

    <main class="main-content">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>角色名稱</th>
            <th>描述</th>
            <th>權限數</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="role in roles" :key="role.id">
            <td>{{ role.id }}</td>
            <td>{{ role.roleName }}</td>
            <td>{{ role.description || '-' }}</td>
            <td>{{ role.permissions ? role.permissions.length : 0 }}</td>
            <td class="actions">
              <button class="btn-sm btn-edit" @click="editRole(role)">編輯</button>
              <button class="btn-sm btn-permissions" @click="editPermissions(role)">權限</button>
              <button class="btn-sm btn-delete" @click="deleteRole(role.id)">刪除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </main>

    <!-- 新增/編輯模態框 -->
    <div v-if="showAddModal || editingRole" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h2>{{ editingRole ? '編輯角色' : '新增角色' }}</h2>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>角色名稱</label>
            <input v-model="form.roleName" required />
          </div>
          <div class="form-group">
            <label>描述</label>
            <textarea v-model="form.description" rows="3"></textarea>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary">儲存</button>
            <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 權限管理模態框 -->
    <div v-if="showPermissionsModal" class="modal-overlay" @click="closePermissionsModal">
      <div class="modal-content" @click.stop style="max-width: 600px;">
        <h2>管理角色權限: {{ selectedRole?.roleName }}</h2>
        <div class="permissions-list">
          <label v-for="permission in allPermissions" :key="permission.id" class="permission-item">
            <input type="checkbox" 
              :value="permission.id" 
              v-model="selectedPermissionIds" />
            <span>{{ permission.permissionName }} ({{ permission.permissionCode }})</span>
          </label>
        </div>
        <div class="form-actions">
          <button type="button" class="btn btn-primary" @click="savePermissions">儲存權限</button>
          <button type="button" class="btn btn-secondary" @click="closePermissionsModal">取消</button>
        </div>
      </div>
    </div>

    <div v-if="notification.show" class="notification" :class="notification.type">
      {{ notification.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiService } from '@/composables/useApi'

const roles = ref([])
const allPermissions = ref([])
const showAddModal = ref(false)
const editingRole = ref(null)
const showPermissionsModal = ref(false)
const selectedRole = ref(null)
const selectedPermissionIds = ref([])
const notification = ref({ show: false, message: '', type: 'success' })

const form = ref({
  roleName: '',
  description: ''
})

const loadRoles = async () => {
  try {
    roles.value = await apiService.getRoles()
  } catch (error) {
    showNotification('載入角色失敗', 'error')
  }
}

const loadPermissions = async () => {
  try {
    allPermissions.value = await apiService.getPermissions()
  } catch (error) {
    showNotification('載入權限失敗', 'error')
  }
}

const handleSubmit = async () => {
  try {
    if (editingRole.value) {
      await apiService.updateRole(editingRole.value.id, form.value)
      showNotification('角色已更新', 'success')
    } else {
      await apiService.createRole(form.value)
      showNotification('角色已新增', 'success')
    }
    closeModal()
    await loadRoles()
  } catch (error) {
    showNotification(error.message || '操作失敗', 'error')
  }
}

const editRole = (role) => {
  editingRole.value = role
  form.value = {
    roleName: role.roleName,
    description: role.description || ''
  }
}

const editPermissions = async (role) => {
  selectedRole.value = role
  selectedPermissionIds.value = role.permissions ? role.permissions.map(p => p.id) : []
  showPermissionsModal.value = true
}

const savePermissions = async () => {
  try {
    await apiService.updateRolePermissions(selectedRole.value.id, selectedPermissionIds.value)
    showNotification('權限已更新', 'success')
    closePermissionsModal()
    await loadRoles()
  } catch (error) {
    showNotification('更新權限失敗', 'error')
  }
}

const deleteRole = async (id) => {
  if (!confirm('確定要刪除這個角色嗎？')) return
  try {
    await apiService.deleteRole(id)
    showNotification('角色已刪除', 'success')
    await loadRoles()
  } catch (error) {
    showNotification('刪除失敗', 'error')
  }
}

const closeModal = () => {
  showAddModal.value = false
  editingRole.value = null
  form.value = { roleName: '', description: '' }
}

const closePermissionsModal = () => {
  showPermissionsModal.value = false
  selectedRole.value = null
  selectedPermissionIds.value = []
}

const showNotification = (message, type = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => { notification.value.show = false }, 3000)
}

onMounted(async () => {
  await loadRoles()
  await loadPermissions()
})
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h1 {
  font-size: 2rem;
  margin: 0;
}

.main-content {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 30px;
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: rgba(255, 255, 255, 0.05);
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.data-table th {
  background: rgba(255, 255, 255, 0.1);
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 0.9rem;
}

.btn-edit {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.btn-permissions {
  background: rgba(139, 92, 246, 0.2);
  color: #a78bfa;
  border: 1px solid rgba(139, 92, 246, 0.3);
}

.btn-delete {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.3);
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

.modal-content {
  background: rgba(255, 255, 255, 0.95);
  color: #333;
  padding: 30px;
  border-radius: 20px;
  min-width: 400px;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.permissions-list {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 20px;
}

.permission-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #e0e0e0;
  cursor: pointer;
}

.permission-item input {
  margin-right: 10px;
}

.notification {
  position: fixed;
  bottom: 20px;
  left: 20px;
  padding: 15px 20px;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  z-index: 10000;
}

.notification.success {
  background: #10b981;
}

.notification.error {
  background: #ef4444;
}
</style>
