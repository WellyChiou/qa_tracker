<template>
  <div class="admin-page">
    <TopNavbar />
    <header class="header">
      <div class="header-top">
        <h1>👥 用戶管理</h1>
        <button class="btn btn-primary btn-add" @click="showAddModal = true">
          <i class="fas fa-plus me-2"></i>新增用戶
        </button>
      </div>
    </header>

    <main class="main-content">
      <table class="data-table">
        <thead>
          <tr>
            <th>UID</th>
            <th>用戶名</th>
            <th>Email</th>
            <th>顯示名稱</th>
            <th>角色</th>
            <th>啟用狀態</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.uid">
            <td>{{ user.uid }}</td>
            <td>{{ user.username || '-' }}</td>
            <td>{{ user.email || '-' }}</td>
            <td>{{ user.displayName || '-' }}</td>
            <td>
              <span v-for="role in user.roles" :key="role.id" class="badge">
                {{ role.roleName }}
              </span>
            </td>
            <td>
              <span :class="user.isEnabled ? 'status-active' : 'status-inactive'">
                {{ user.isEnabled ? '啟用' : '停用' }}
              </span>
            </td>
            <td class="actions">
              <button class="btn-sm btn-edit" @click="editUser(user)">編輯</button>
              <button class="btn-sm btn-roles" @click="editRoles(user)">角色</button>
              <button class="btn-sm btn-permissions" @click="editPermissions(user)">權限</button>
              <button class="btn-sm btn-delete" @click="deleteUser(user.uid)">刪除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </main>

    <!-- 新增/編輯模態框 -->
    <div v-if="showAddModal || editingUser" class="modal-overlay" @click="closeModal">
      <div class="modal-panel" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">{{ editingUser ? '編輯用戶' : '新增用戶' }}</h2>
          <button class="btn-close" @click="closeModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit" class="form-container">
            <div v-if="editingUser" class="form-group">
              <label class="form-label">UID</label>
              <input v-model="form.uid" disabled class="form-input" />
              <small class="form-text text-muted">UID 無法修改</small>
            </div>
            <div class="form-group">
              <label class="form-label">用戶名 <span class="text-danger">*</span></label>
              <input v-model="form.username" required class="form-input" placeholder="請輸入用戶名" />
            </div>
            <div class="form-group">
              <label class="form-label">Email</label>
              <input type="email" v-model="form.email" class="form-input" placeholder="請輸入電子郵件" />
            </div>
            <div class="form-group">
              <label class="form-label">顯示名稱</label>
              <input v-model="form.displayName" class="form-input" placeholder="請輸入顯示名稱" />
            </div>
            <div class="form-group">
              <label class="form-label">密碼 <span v-if="!editingUser" class="text-danger">*</span></label>
              <input type="password" v-model="form.password" :required="!editingUser" class="form-input" placeholder="請輸入密碼" />
              <small v-if="editingUser" class="form-text text-muted">留空則不修改密碼</small>
            </div>
            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="form.isEnabled" class="checkbox-input" />
                <span>啟用帳號</span>
              </label>
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

    <!-- 角色分配模態框 -->
    <div v-if="showRolesModal" class="modal-overlay" @click="closeRolesModal">
      <div class="modal-panel" @click.stop style="max-width: 600px;">
        <div class="modal-header">
          <h2 class="modal-title">分配角色: {{ selectedUser?.displayName || selectedUser?.username }}</h2>
          <button class="btn-close" @click="closeRolesModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="roles-list">
            <label v-for="role in allRoles" :key="role.id" class="role-item">
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
            <button type="button" class="btn btn-primary" @click="saveRoles">
              <i class="fas fa-save me-2"></i>儲存角色
            </button>
            <button type="button" class="btn btn-secondary" @click="closeRolesModal">
              <i class="fas fa-times me-2"></i>取消
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 權限分配模態框 -->
    <div v-if="showPermissionsModal" class="modal-overlay" @click="closePermissionsModal">
      <div class="modal-panel" @click.stop style="max-width: 700px;">
        <div class="modal-header">
          <h2 class="modal-title">分配權限: {{ selectedUser?.displayName || selectedUser?.username }}</h2>
          <button class="btn-close" @click="closePermissionsModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="permissions-list">
            <label v-for="permission in allPermissions" :key="permission.id" class="permission-item">
              <input 
                type="checkbox" 
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

    <div v-if="notification.show" class="notification" :class="notification.type">
      {{ notification.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TopNavbar from '@/components/TopNavbar.vue'
import { apiService } from '@/composables/useApi'

const users = ref([])
const showAddModal = ref(false)
const editingUser = ref(null)
const showRolesModal = ref(false)
const showPermissionsModal = ref(false)
const selectedUser = ref(null)
const allRoles = ref([])
const allPermissions = ref([])
const selectedRoleIds = ref([])
const selectedPermissionIds = ref([])
const notification = ref({ show: false, message: '', type: 'success' })

const form = ref({
  uid: '',
  username: '',
  email: '',
  displayName: '',
  password: '',
  isEnabled: true
})

const loadUsers = async () => {
  try {
    users.value = await apiService.getUsers()
  } catch (error) {
    showNotification('載入用戶失敗', 'error')
  }
}

const loadRoles = async () => {
  try {
    allRoles.value = await apiService.getRoles()
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
    const userData = { ...form.value }
    
    // 新增用戶時，不發送 UID（讓後端自動生成）
    if (!editingUser.value) {
      delete userData.uid
    }
    
    // 編輯用戶時，如果密碼為空則不發送
    if (editingUser.value && !userData.password) {
      delete userData.password
    } else if (!editingUser.value && !userData.password) {
      // 新增用戶時，密碼為必填
      showNotification('請輸入密碼', 'error')
      return
    }
    
    // 編輯用戶時，不發送 roles 字段（角色應該通過專門的角色管理功能來更新）
    // 避免意外清空用戶的角色
    if (editingUser.value) {
      delete userData.roles
    }
    
    if (editingUser.value) {
      await apiService.updateUser(editingUser.value.uid, userData)
      showNotification('用戶已更新', 'success')
    } else {
      await apiService.createUser(userData)
      showNotification('用戶已新增', 'success')
    }
    closeModal()
    await loadUsers()
  } catch (error) {
    showNotification(error.message || '操作失敗', 'error')
  }
}

const editUser = (user) => {
  editingUser.value = user
  form.value = {
    uid: user.uid,
    username: user.username || '',
    email: user.email || '',
    displayName: user.displayName || '',
    password: '',
    isEnabled: user.isEnabled !== false
  }
}

const deleteUser = async (uid) => {
  if (!confirm('確定要刪除這個用戶嗎？')) return
  try {
    await apiService.deleteUser(uid)
    showNotification('用戶已刪除', 'success')
    await loadUsers()
  } catch (error) {
    showNotification('刪除失敗', 'error')
  }
}

const editRoles = async (user) => {
  selectedUser.value = user
  // 載入用戶當前的角色 ID
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
    await apiService.updateUserRoles(selectedUser.value.uid, selectedRoleIds.value)
    showNotification('角色已更新', 'success')
    closeRolesModal()
    await loadUsers()
  } catch (error) {
    showNotification(error.message || '更新失敗', 'error')
  }
}

const editPermissions = async (user) => {
  selectedUser.value = user
  // 載入用戶當前的權限 ID
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
    await apiService.updateUserPermissions(selectedUser.value.uid, selectedPermissionIds.value)
    showNotification('權限已更新', 'success')
    closePermissionsModal()
    await loadUsers()
  } catch (error) {
    showNotification(error.message || '更新失敗', 'error')
  }
}

const closeModal = () => {
  showAddModal.value = false
  editingUser.value = null
  form.value = {
    uid: '',
    username: '',
    email: '',
    displayName: '',
    password: '',
    isEnabled: true
  }
}

const showNotification = (message, type = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => { notification.value.show = false }, 3000)
}

onMounted(() => {
  loadUsers()
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-attachment: fixed;
  color: white;
  padding: var(--spacing-xl);
  position: relative;
}

.admin-page::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 50%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(118, 75, 162, 0.3) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.header {
  margin-bottom: var(--spacing-2xl);
  position: relative;
  z-index: 1;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-lg);
}

.header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.main-content {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  border-radius: var(--border-radius-xl);
  padding: var(--spacing-2xl);
  overflow-x: auto;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: var(--shadow-xl);
  position: relative;
  z-index: 1;
}

.data-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: rgba(255, 255, 255, 0.08);
  border-radius: var(--border-radius-lg);
  overflow: hidden;
}

.data-table th,
.data-table td {
  padding: 16px;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.data-table th {
  background: rgba(255, 255, 255, 0.15);
  font-weight: 700;
  font-size: 0.9rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: rgba(255, 255, 255, 0.95);
  position: sticky;
  top: 0;
  z-index: 10;
}

.data-table tbody tr {
  transition: var(--transition);
}

.data-table tbody tr:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: scale(1.01);
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}

.badge {
  display: inline-block;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  border-radius: var(--border-radius);
  margin-right: var(--spacing-xs);
  margin-bottom: var(--spacing-xs);
  font-size: 0.85rem;
  font-weight: 600;
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: var(--transition);
}

.badge:hover {
  background: rgba(255, 255, 255, 0.35);
  transform: translateY(-2px);
}

.status-active {
  color: #4ade80;
  font-weight: 700;
  text-shadow: 0 2px 10px rgba(74, 222, 128, 0.3);
}

.status-inactive {
  color: #f87171;
  font-weight: 700;
  text-shadow: 0 2px 10px rgba(248, 113, 113, 0.3);
}

.actions {
  display: flex;
  gap: var(--spacing-sm);
}

.btn {
  padding: 14px 28px;
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-weight: 600;
  font-size: 15px;
  transition: var(--transition);
  position: relative;
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.btn:hover::before {
  width: 300px;
  height: 300px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.5);
}

.btn-add {
  font-size: 1rem;
  padding: 0.875rem 1.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.me-2 {
  margin-right: 0.5rem;
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-3px);
  box-shadow: 0 10px 25px rgba(255, 255, 255, 0.2);
}

.btn-sm {
  padding: 8px 16px;
  font-size: 14px;
}

.btn-edit {
  background: #3b82f6;
  color: white;
  border: 1px solid #2563eb;
}

.btn-edit:hover {
  background: #2563eb;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-delete {
  background: #ef4444;
  color: white;
  border: 1px solid #dc2626;
}

.btn-delete:hover {
  background: #dc2626;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-roles {
  background: rgba(139, 92, 246, 0.25);
  color: #c4b5fd;
  border: 1px solid rgba(139, 92, 246, 0.3);
}

.btn-roles:hover {
  background: rgba(139, 92, 246, 0.35);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-permissions {
  background: #8b5cf6;
  color: white;
  border: 1px solid #7c3aed;
}

.btn-permissions:hover {
  background: #7c3aed;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-panel {
  width: 100%;
  max-width: 600px;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  border: 1px solid #e2e8f0;
  margin: 2rem;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(to right, #f8fafc, white);
  border-radius: 1rem 1rem 0 0;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.btn-close {
  background: transparent;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #f1f5f9;
  color: #1e293b;
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
  margin-bottom: var(--spacing-lg);
}

.form-group label {
  display: block;
  margin-bottom: var(--spacing-sm);
  font-weight: 600;
  color: var(--text-primary);
  font-size: 0.95rem;
}

.form-input,
.form-group input[type="text"],
.form-group input[type="email"],
.form-group input[type="password"] {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.2s ease;
  background: #ffffff;
  color: #1f2937;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.form-input:focus,
.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: #ffffff;
}

.form-input:disabled,
.form-group input:disabled {
  background: #f9fafb;
  opacity: 0.6;
  cursor: not-allowed;
  border-color: #e5e7eb;
}

.form-text {
  display: block;
  margin-top: 0.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.text-danger {
  color: #ef4444;
}

.text-muted {
  color: #6b7280;
}

.checkbox-group {
  display: flex;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-weight: 600;
  color: #1f2937;
  font-size: 0.95rem;
  gap: 0.75rem;
}

.checkbox-input {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #667eea;
  border-radius: 4px;
  border: 2px solid #d1d5db;
  transition: all 0.2s ease;
}

.checkbox-input:checked {
  background-color: #667eea;
  border-color: #667eea;
}

.checkbox-input:hover {
  border-color: #667eea;
}

.roles-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  max-height: 400px;
  overflow-y: auto;
  padding: 0.5rem;
}

.role-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.role-item:hover {
  border-color: #667eea;
  background: #f8fafc;
}

.role-item input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #667eea;
}

.role-item span {
  font-weight: 600;
  color: #1f2937;
  flex: 1;
}

.role-description {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: normal;
  margin-left: 0.5rem;
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
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
  justify-content: flex-end;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  font-weight: 600;
  font-size: 0.9375rem;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 6px -1px rgba(102, 126, 234, 0.3);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px -2px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #e5e7eb;
}

.btn-secondary:hover {
  background: #e5e7eb;
  color: #1f2937;
}

.me-2 {
  margin-right: 0.5rem;
}

.notification {
  position: fixed;
  bottom: var(--spacing-xl);
  left: var(--spacing-xl);
  padding: var(--spacing-lg) var(--spacing-xl);
  border-radius: var(--border-radius-lg);
  color: white;
  font-weight: 600;
  z-index: 10000;
  animation: slideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-xl);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  min-width: 300px;
}

.notification.success {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.95) 0%, rgba(5, 150, 105, 0.95) 100%);
}

.notification.error {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.95) 0%, rgba(220, 38, 38, 0.95) 100%);
}

@keyframes slideIn {
  from {
    transform: translateX(-120%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>
