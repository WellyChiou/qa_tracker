<template>
  <AdminLayout>
    <div class="admin-page">
    <header class="header">
      <div class="header-top">
        <h1>🔗 URL 權限管理</h1>
        <button class="btn btn-primary btn-add" @click="showAddModal = true">
          <i class="fas fa-plus me-2"></i>新增 URL 權限
        </button>
      </div>
    </header>

    <main class="main-content">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>URL 模式</th>
            <th>HTTP 方法</th>
            <th>所需角色</th>
            <th>所需權限</th>
            <th>公開</th>
            <th>啟用</th>
            <th>排序</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="permission in permissions" :key="permission.id">
            <td>{{ permission.id }}</td>
            <td>{{ permission.urlPattern }}</td>
            <td>{{ permission.httpMethod || 'ALL' }}</td>
            <td>{{ permission.requiredRole || '-' }}</td>
            <td>{{ permission.requiredPermission || '-' }}</td>
            <td>
              <span :class="permission.isPublic ? 'status-active' : 'status-inactive'">
                {{ permission.isPublic ? '是' : '否' }}
              </span>
            </td>
            <td>
              <span :class="permission.isActive ? 'status-active' : 'status-inactive'">
                {{ permission.isActive ? '是' : '否' }}
              </span>
            </td>
            <td>{{ permission.orderIndex }}</td>
            <td class="actions">
              <button class="btn-sm btn-edit" @click="editPermission(permission)">編輯</button>
              <button class="btn-sm btn-delete" @click="deletePermission(permission.id)">刪除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </main>

    <!-- 新增/編輯模態框 -->
    <div v-if="showAddModal || editingPermission" class="modal-overlay" @click="closeModal">
      <div class="modal-panel" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">{{ editingPermission ? '編輯 URL 權限' : '新增 URL 權限' }}</h2>
          <button class="btn-close" @click="closeModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit" class="form-container">
            <div class="form-group">
              <label class="form-label">URL 模式 <span class="text-danger">*</span></label>
              <input v-model="form.urlPattern" required class="form-input" placeholder="/api/**" />
            </div>
            <div class="form-group">
              <label class="form-label">HTTP 方法</label>
              <select v-model="form.httpMethod" class="form-input">
                <option value="">全部</option>
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">所需角色</label>
              <select v-model="form.requiredRole" class="form-input">
                <option value="">無需特定角色</option>
                <option 
                  v-for="role in availableRoles" 
                  :key="role.id" 
                  :value="role.roleName"
                >
                  {{ role.roleName }} - {{ role.description || '' }}
                </option>
              </select>
              <small class="form-hint">選擇對應的角色</small>
            </div>
            <div class="form-group">
              <label class="form-label">所需權限</label>
              <select v-model="form.requiredPermission" class="form-input">
                <option value="">無需特定權限</option>
                <option 
                  v-for="perm in availablePermissions" 
                  :key="perm.id" 
                  :value="perm.permissionCode"
                >
                  {{ perm.permissionCode }} - {{ perm.permissionName }}
                </option>
              </select>
              <small class="form-hint">選擇對應的權限代碼</small>
            </div>
            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="form.isPublic" class="checkbox-input" />
                <span>公開（無需認證）</span>
              </label>
            </div>
            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="form.isActive" class="checkbox-input" />
                <span>啟用</span>
              </label>
            </div>
            <div class="form-group">
              <label class="form-label">排序</label>
              <input type="number" v-model.number="form.orderIndex" class="form-input" placeholder="0" />
            </div>
            <div class="form-group">
              <label class="form-label">描述</label>
              <textarea v-model="form.description" rows="3" class="form-input" placeholder="請輸入描述"></textarea>
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

    <div v-if="notification.show" class="notification" :class="notification.type">
      {{ notification.message }}
    </div>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue'
import { ref, onMounted } from 'vue'
import { apiService } from '@/composables/useApi'

const permissions = ref([])
const showAddModal = ref(false)
const editingPermission = ref(null)
const notification = ref({ show: false, message: '', type: 'success' })
const availableRoles = ref([])
const availablePermissions = ref([])

const form = ref({
  urlPattern: '',
  httpMethod: '',
  requiredRole: '',
  requiredPermission: '',
  isPublic: false,
  isActive: true,
  orderIndex: 0,
  description: ''
})

const loadPermissions = async () => {
  try {
    permissions.value = await apiService.getUrlPermissions()
  } catch (error) {
    showNotification('載入 URL 權限失敗', 'error')
  }
}

const loadRoles = async () => {
  try {
    availableRoles.value = await apiService.getRoles()
  } catch (error) {
    console.error('載入角色列表失敗:', error)
  }
}

const loadAvailablePermissions = async () => {
  try {
    availablePermissions.value = await apiService.getPermissions()
  } catch (error) {
    console.error('載入權限列表失敗:', error)
  }
}

const handleSubmit = async () => {
  try {
    const permissionData = { ...form.value }
    if (!permissionData.httpMethod) permissionData.httpMethod = null
    
    if (editingPermission.value) {
      await apiService.updateUrlPermission(editingPermission.value.id, permissionData)
      showNotification('URL 權限已更新', 'success')
    } else {
      await apiService.createUrlPermission(permissionData)
      showNotification('URL 權限已新增', 'success')
    }
    closeModal()
    await loadPermissions()
  } catch (error) {
    showNotification(error.message || '操作失敗', 'error')
  }
}

const editPermission = (permission) => {
  editingPermission.value = permission
  form.value = {
    urlPattern: permission.urlPattern,
    httpMethod: permission.httpMethod || '',
    requiredRole: permission.requiredRole || '',
    requiredPermission: permission.requiredPermission || '',
    isPublic: permission.isPublic || false,
    isActive: permission.isActive !== false,
    orderIndex: permission.orderIndex || 0,
    description: permission.description || ''
  }
}

const deletePermission = async (id) => {
  if (!confirm('確定要刪除這個 URL 權限嗎？')) return
  try {
    await apiService.deleteUrlPermission(id)
    showNotification('URL 權限已刪除', 'success')
    await loadPermissions()
  } catch (error) {
    showNotification('刪除失敗', 'error')
  }
}

const closeModal = () => {
  showAddModal.value = false
  editingPermission.value = null
  form.value = {
    urlPattern: '',
    httpMethod: '',
    requiredRole: '',
    requiredPermission: '',
    isPublic: false,
    isActive: true,
    orderIndex: 0,
    description: ''
  }
}

const showNotification = (message, type = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => { notification.value.show = false }, 3000)
}

onMounted(() => {
  loadPermissions()
  loadRoles()
  loadAvailablePermissions()
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

.form-hint {
  display: block;
  margin-top: 0.25rem;
  color: #6b7280;
  font-size: 0.85rem;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid #e5e7eb;
  justify-content: flex-end;
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
