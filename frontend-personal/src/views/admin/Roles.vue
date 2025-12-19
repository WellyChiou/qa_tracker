<template>
  <AdminLayout>
    <div class="admin-page">
    <header class="header">
      <div class="header-top">
        <h1>🔐 角色管理</h1>
        <button class="btn btn-primary btn-add" @click="showAddModal = true">
          <i class="fas fa-plus me-2"></i>新增角色
        </button>
      </div>
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
      <div class="modal-panel" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">{{ editingRole ? '編輯角色' : '新增角色' }}</h2>
          <button class="btn-close" @click="closeModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit" class="form-container">
            <div class="form-group">
              <label class="form-label">角色名稱 <span class="text-danger">*</span></label>
              <input v-model="form.roleName" required class="form-input" placeholder="請輸入角色名稱" />
            </div>
            <div class="form-group">
              <label class="form-label">描述</label>
              <textarea v-model="form.description" rows="3" class="form-input" placeholder="請輸入角色描述"></textarea>
            </div>
            <div class="form-actions">
              <button type="submit" class="btn btn-primary">
                <i class="fas fa-save me-2"></i>儲存
              </button>
              <button type="button" class="btn btn-secondary" @click="closeModal">
                <i class="fas fa-times me-2"></i>取消
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

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
          <div class="dual-pick">
            <div class="dual-col">
              <div class="dual-head">未加入權限</div>
              <div class="dual-list">
                <label v-for="permission in availablePermissions" :key="permission.id" class="dual-item">
                  <input
                    type="checkbox"
                    :value="permission.id"
                    v-model="tmpAddPermissionIds"
                    class="checkbox-input"
                  />
                  <div class="dual-item-main">
                    <div class="dual-item-title">{{ permission.permissionName }}</div>
                    <div class="dual-item-sub">
                      <code class="permission-code">{{ permission.permissionCode }}</code>
                    </div>
                  </div>
                </label>
                <div v-if="availablePermissions.length === 0" class="dual-empty">已全部加入</div>
              </div>
            </div>

            <div class="dual-actions">
              <button type="button" class="btn btn-outline-primary btn-sm" @click="addSelectedPermissions" :disabled="tmpAddPermissionIds.length === 0">
                加入 →
              </button>
              <button type="button" class="btn btn-outline-danger btn-sm" @click="removeSelectedPermissions" :disabled="tmpRemovePermissionIds.length === 0">
                ← 移除
              </button>
            </div>

            <div class="dual-col">
              <div class="dual-head">已加入權限</div>
              <div class="dual-list">
                <label v-for="permission in assignedPermissions" :key="permission.id" class="dual-item">
                  <input
                    type="checkbox"
                    :value="permission.id"
                    v-model="tmpRemovePermissionIds"
                    class="checkbox-input"
                  />
                  <div class="dual-item-main">
                    <div class="dual-item-title">{{ permission.permissionName }}</div>
                    <div class="dual-item-sub">
                      <code class="permission-code">{{ permission.permissionCode }}</code>
                    </div>
                  </div>
                </label>
                <div v-if="assignedPermissions.length === 0" class="dual-empty">尚未加入任何權限</div>
              </div>
            </div>
          </div>
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

    <div v-if="notification.show" class="notification" :class="notification.type">
      {{ notification.message }}
    </div>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue'
import { ref, onMounted, computed } from 'vue'
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
  tmpAddPermissionIds.value = []
  tmpRemovePermissionIds.value = []
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
  tmpAddPermissionIds.value = []
  tmpRemovePermissionIds.value = []
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
  background: var(--bg-primary);
  padding-bottom: 2rem;
}

.header {
  background: var(--bg-card);
  padding: 2rem;
  margin-bottom: 2rem;
  box-shadow: var(--shadow);
}

.header-top {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h1 {
  font-size: 1.8rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 2rem;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: var(--bg-card);
  border-radius: var(--border-radius);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.data-table th,
.data-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.data-table th {
  background: #f9fafb;
  font-weight: 600;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #374151;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.btn {
  padding: 0.6rem 1.2rem;
  border-radius: var(--border-radius);
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.95rem;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5a67d8;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(102, 126, 234, 0.25);
}

.btn-secondary {
  background: #fff;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background: #f9fafb;
  color: #111827;
}

.btn-sm {
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  color: white;
  transition: all 0.2s;
}

.btn-edit {
  background: #3b82f6;
}

.btn-edit:hover {
  background: #2563eb;
}

.btn-permissions {
  background: #8b5cf6;
  color: white;
}

.btn-permissions:hover {
  background: #7c3aed;
}

.btn-delete {
  background: #ef4444;
}

.btn-delete:hover {
  background: #dc2626;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-panel {
  width: 100%;
  max-width: 600px;
  background: var(--bg-card);
  border-radius: var(--border-radius);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  margin: 2rem;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.btn-close {
  background: transparent;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  background: #f3f4f6;
  color: #111827;
}

.modal-body {
  padding: 1.5rem;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-group {
  margin-bottom: 0;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
  font-size: 0.9rem;
}

.form-input {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid #d1d5db;
  border-radius: var(--border-radius);
  font-size: 0.95rem;
  transition: all 0.2s;
  background: var(--bg-card);
  color: #1f2937;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid #e5e7eb;
  justify-content: flex-end;
}

.permissions-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  max-height: 400px;
  overflow-y: auto;
  padding: 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: var(--border-radius);
  background: #f9fafb;
}

.permission-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: 4px;
  background: var(--bg-card);
  border: 1px solid #e5e7eb;
  cursor: pointer;
}

.permission-item:hover {
  border-color: #667eea;
}

.permission-code {
  background: #f3f4f6;
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
  font-size: 0.75rem;
  color: #4b5563;
  font-family: monospace;
}

.checkbox-input {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
  accent-color: #667eea;
}

.text-danger { color: #ef4444; }
.me-2 { margin-right: 0.5rem; }

.notification {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  left: auto;
  padding: 1rem 1.5rem;
  border-radius: var(--border-radius);
  color: white;
  font-weight: 600;
  z-index: 10000;
  animation: slideIn 0.3s;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  min-width: 300px;
}

.notification.success { background: #10b981; }
.notification.error { background: #ef4444; }

@keyframes slideIn {
  from { transform: translateY(100%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
</style>
