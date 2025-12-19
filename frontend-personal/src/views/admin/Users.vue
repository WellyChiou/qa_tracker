<template>
  <AdminLayout>
    <div class="admin-page">
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
          <div class="pickboard">
            <div class="pick-col">
              <div class="pick-head">
                <div class="pick-title">未加入</div>
                <input v-model="searchAvailableRoles" class="pick-search" placeholder="搜尋角色..." />
              </div>
              <div class="pick-list">
                <div v-for="role in availableRolesFiltered" :key="role.id" class="pick-item">
                  <div class="pick-main">
                    <div class="pick-name">{{ role.roleName }}</div>
                    <div class="pick-sub" v-if="role.description">{{ role.description }}</div>
                  </div>
                  <button type="button" class="btn-pill btn-pill-primary" @click="addRole(role.id)">加入</button>
                </div>
                <div v-if="availableRolesFiltered.length === 0" class="pick-empty">已全部加入</div>
              </div>
            </div>

            <div class="pick-col">
              <div class="pick-head">
                <div class="pick-title">已加入</div>
                <input v-model="searchAssignedRoles" class="pick-search" placeholder="搜尋已加入..." />
              </div>
              <div class="pick-list">
                <div v-for="role in assignedRolesFiltered" :key="role.id" class="pick-item">
                  <div class="pick-main">
                    <div class="pick-name">{{ role.roleName }}</div>
                    <div class="pick-sub" v-if="role.description">{{ role.description }}</div>
                  </div>
                  <button type="button" class="btn-pill btn-pill-danger" @click="removeRole(role.id)">移除</button>
                </div>
                <div v-if="assignedRolesFiltered.length === 0" class="pick-empty">尚未加入任何角色</div>
              </div>
            </div>
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
          <div class="pickboard">
            <div class="pick-col">
              <div class="pick-head">
                <div class="pick-title">未加入</div>
                <input v-model="searchAvailablePermissions" class="pick-search" placeholder="搜尋權限..." />
              </div>
              <div class="pick-list">
                <div v-for="permission in availablePermissionsFiltered" :key="permission.id" class="pick-item">
                  <div class="pick-main">
                    <div class="pick-name">{{ permission.permissionName }}</div>
                    <div class="pick-code">({{ permission.permissionCode }})</div>
                  </div>
                  <button type="button" class="btn-pill btn-pill-primary" @click="addPermission(permission.id)">加入</button>
                </div>
                <div v-if="availablePermissionsFiltered.length === 0" class="pick-empty">已全部加入</div>
              </div>
            </div>

            <div class="pick-col">
              <div class="pick-head">
                <div class="pick-title">已加入</div>
                <input v-model="searchAssignedPermissions" class="pick-search" placeholder="搜尋已加入..." />
              </div>
              <div class="pick-list">
                <div v-for="permission in assignedPermissionsFiltered" :key="permission.id" class="pick-item">
                  <div class="pick-main">
                    <div class="pick-name">{{ permission.permissionName }}</div>
                    <div class="pick-code">({{ permission.permissionCode }})</div>
                  </div>
                  <button type="button" class="btn-pill btn-pill-danger" @click="removePermission(permission.id)">移除</button>
                </div>
                <div v-if="assignedPermissionsFiltered.length === 0" class="pick-empty">尚未加入任何權限</div>
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

// ====== dual-list selection (UI only) ======
const tmpAddRoleIds = ref([])
const tmpRemoveRoleIds = ref([])
const tmpAddPermissionIds = ref([])
const tmpRemovePermissionIds = ref([])

const availableRoles = computed(() =>
  allRoles.value.filter(r => !selectedRoleIds.value.includes(r.id))
)
const assignedRoles = computed(() =>
  allRoles.value.filter(r => selectedRoleIds.value.includes(r.id))
)

const availablePermissions = computed(() =>
  allPermissions.value.filter(p => !selectedPermissionIds.value.includes(p.id))
)
const assignedPermissions = computed(() =>
  allPermissions.value.filter(p => selectedPermissionIds.value.includes(p.id))
)

// ====== styled picker (UI only) ======
const searchAvailableRoles = ref('')
const searchAssignedRoles = ref('')
const searchAvailablePermissions = ref('')
const searchAssignedPermissions = ref('')

const normalize = (s) => String(s || "").toLowerCase()

const availableRolesFiltered = computed(() => {
  const q = normalize(searchAvailableRoles.value).trim()
  if (!q) return availableRoles.value
  return availableRoles.value.filter(r =>
    normalize(r.roleName).includes(q) || normalize(r.description).includes(q)
  )
})

const assignedRolesFiltered = computed(() => {
  const q = normalize(searchAssignedRoles.value).trim()
  if (!q) return assignedRoles.value
  return assignedRoles.value.filter(r =>
    normalize(r.roleName).includes(q) || normalize(r.description).includes(q)
  )
})

const availablePermissionsFiltered = computed(() => {
  const q = normalize(searchAvailablePermissions.value).trim()
  if (!q) return availablePermissions.value
  return availablePermissions.value.filter(p =>
    normalize(p.permissionName).includes(q) || normalize(p.permissionCode).includes(q)
  )
})

const assignedPermissionsFiltered = computed(() => {
  const q = normalize(searchAssignedPermissions.value).trim()
  if (!q) return assignedPermissions.value
  return assignedPermissions.value.filter(p =>
    normalize(p.permissionName).includes(q) || normalize(p.permissionCode).includes(q)
  )
})

const addRole = (id) => {
  const next = new Set(selectedRoleIds.value)
  next.add(id)
  selectedRoleIds.value = Array.from(next)
}

const removeRole = (id) => {
  selectedRoleIds.value = selectedRoleIds.value.filter(x => x !== id)
}

const addPermission = (id) => {
  const next = new Set(selectedPermissionIds.value)
  next.add(id)
  selectedPermissionIds.value = Array.from(next)
}

const removePermission = (id) => {
  selectedPermissionIds.value = selectedPermissionIds.value.filter(x => x !== id)
}


const addSelectedRoles = () => {
  const next = new Set(selectedRoleIds.value)
  tmpAddRoleIds.value.forEach(id => next.add(id))
  selectedRoleIds.value = Array.from(next)
  tmpAddRoleIds.value = []
}

const removeSelectedRoles = () => {
  const removeSet = new Set(tmpRemoveRoleIds.value)
  selectedRoleIds.value = selectedRoleIds.value.filter(id => !removeSet.has(id))
  tmpRemoveRoleIds.value = []
}

const addSelectedPermissions = () => {
  const next = new Set(selectedPermissionIds.value)
  tmpAddPermissionIds.value.forEach(id => next.add(id))
  selectedPermissionIds.value = Array.from(next)
  tmpAddPermissionIds.value = []
}

const removeSelectedPermissions = () => {
  const removeSet = new Set(tmpRemovePermissionIds.value)
  selectedPermissionIds.value = selectedPermissionIds.value.filter(id => !removeSet.has(id))
  tmpRemovePermissionIds.value = []
}
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
  tmpAddRoleIds.value = []
  tmpRemoveRoleIds.value = []
  searchAvailableRoles.value = ''
  searchAssignedRoles.value = ''
  showRolesModal.value = true
}

const closeRolesModal = () => {
  showRolesModal.value = false
  selectedUser.value = null
  selectedRoleIds.value = []
  tmpAddRoleIds.value = []
  tmpRemoveRoleIds.value = []
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
  tmpAddPermissionIds.value = []
  tmpRemovePermissionIds.value = []
  searchAvailablePermissions.value = ''
  searchAssignedPermissions.value = ''
  showPermissionsModal.value = true
}

const closePermissionsModal = () => {
  showPermissionsModal.value = false
  selectedUser.value = null
  selectedPermissionIds.value = []
  tmpAddPermissionIds.value = []
  tmpRemovePermissionIds.value = []
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

.badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 4px;
  margin-right: 0.5rem;
  margin-bottom: 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  border: none;
}

.status-active {
  color: #10b981;
  font-weight: 600;
}

.status-inactive {
  color: #ef4444;
  font-weight: 600;
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

.btn-add {
  font-size: 0.95rem;
}

.btn-sm {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0.55rem 1.05rem;
  font-size: 0.95rem;
  border-radius: 999px;
  border: 2px solid #e5e7eb;
  cursor: pointer;
  font-weight: 800;
  letter-spacing: 0.02em;
  background: #fff;
  color: #0f172a;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
  transition: all 0.15s ease;
}

.btn-sm:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(0,0,0,0.08);
}

/* 角色 / 權限：依你給的 pill 樣式 */
.btn-roles {
  background: #ffffff;
  border-color: #e5e7eb;
  color: #0f172a;
}

.btn-roles:hover {
  background: #f8fafc;
  border-color: #d1d5db;
}

.btn-permissions {
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.35);
  color: #2f6d4a;
}

.btn-permissions:hover {
  background: rgba(34, 197, 94, 0.16);
  border-color: rgba(34, 197, 94, 0.45);
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

.form-group label {
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

.form-input:disabled {
  background: #f9fafb;
  color: #9ca3af;
}

.form-text {
  margin-top: 0.25rem;
  font-size: 0.8rem;
  color: #6b7280;
}

.text-danger { color: #ef4444; }
.text-muted { color: #6b7280; }

.checkbox-group {
  display: flex;
  align-items: center;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-weight: 500;
  color: #374151;
  gap: 0.5rem;
}

.checkbox-input {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
  accent-color: #667eea;
}

.roles-list, .permissions-list {
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

.role-item, .permission-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: 4px;
  background: var(--bg-card);
  border: 1px solid #e5e7eb;
  cursor: pointer;
}

.role-item:hover, .permission-item:hover {
  border-color: #667eea;
}

.role-description {
  color: #6b7280;
  font-size: 0.8rem;
}

.permission-code {
  background: #f3f4f6;
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
  font-size: 0.75rem;
  color: #4b5563;
  font-family: monospace;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid #e5e7eb;
  justify-content: flex-end;
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

.me-2 { margin-right: 0.5rem; }
/* ============================================
   Icon buttons: 編輯 / 刪除（參考教會後台風格）
   - 不影響 API / 邏輯，只統一視覺
   ============================================ */
.btn-edit, .btn-delete {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
  padding: 0.75rem 1.5rem;
  border-radius: 999px;
  border: 2px solid transparent;
  font-weight: 800;
  letter-spacing: 0.02em;
  cursor: pointer;
  user-select: none;
  transition: all 0.18s ease;
  background: transparent;
}

.btn-sm.btn-edit, .btn-sm.btn-delete {
  padding: 0.55rem 1.15rem;
  font-size: 0.9rem;
}

.btn-edit {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.btn-edit:hover {
  background: #dbeafe;
  border-color: #93c5fd;
  transform: translateY(-1px);
}

.btn-delete {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.btn-delete:hover {
  background: #fee2e2;
  border-color: #fca5a5;
  transform: translateY(-1px);
}

.btn-edit::before,
.btn-delete::before {
  content: "";
  width: 20px;
  height: 20px;
  display: inline-block;
  background-repeat: no-repeat;
  background-position: center;
  background-size: 20px 20px;
  flex: 0 0 20px;
}

.btn-edit::before {
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http://www.w3.org/2000/svg%22%20viewBox%3D%220%200%2024%2024%22%20fill%3D%22none%22%20stroke%3D%22%231d4ed8%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cpath%20d%3D%22M12%2020h9%22/%3E%3Cpath%20d%3D%22M16.5%203.5a2.121%202.121%200%200%201%203%203L7%2019l-4%201%201-4%2012.5-12.5z%22/%3E%3C/svg%3E");
}

.btn-delete::before {
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http://www.w3.org/2000/svg%22%20viewBox%3D%220%200%2024%2024%22%20fill%3D%22none%22%20stroke%3D%22%23b91c1c%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cpolyline%20points%3D%223%206%205%206%2021%206%22/%3E%3Cpath%20d%3D%22M19%206l-1%2014a2%202%200%200%201-2%202H8a2%202%200%200%201-2-2L5%206%22/%3E%3Cpath%20d%3D%22M10%2011v6%22/%3E%3Cpath%20d%3D%22M14%2011v6%22/%3E%3Cpath%20d%3D%22M9%206V4a2%202%200%200%201%202-2h2a2%202%200%200%201%202%202v2%22/%3E%3C/svg%3E");
}

</style>
