<template>
  <div class="admin-page">
    <TopNavbar />
    <header class="header">
      <div class="header-top">
        <h1>🔑 權限管理</h1>
        <button class="btn btn-primary btn-add" @click="showAddModal = true">
          <i class="fas fa-plus me-2"></i>新增權限
        </button>
      </div>
    </header>

    <main class="main-content">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>權限代碼</th>
            <th>權限名稱</th>
            <th>資源</th>
            <th>操作</th>
            <th>描述</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="permission in permissions" :key="permission.id">
            <td>{{ permission.id }}</td>
            <td>{{ permission.permissionCode }}</td>
            <td>{{ permission.permissionName }}</td>
            <td>{{ permission.resource || '-' }}</td>
            <td>{{ permission.action || '-' }}</td>
            <td>{{ permission.description || '-' }}</td>
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
          <h2 class="modal-title">{{ editingPermission ? '編輯權限' : '新增權限' }}</h2>
          <button class="btn-close" @click="closeModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit" class="form-container">
            <div class="form-group">
              <label class="form-label">權限代碼 <span class="text-danger">*</span></label>
              <input v-model="form.permissionCode" required class="form-input" placeholder="請輸入權限代碼" />
            </div>
            <div class="form-group">
              <label class="form-label">權限名稱 <span class="text-danger">*</span></label>
              <input v-model="form.permissionName" required class="form-input" placeholder="請輸入權限名稱" />
            </div>
            <div class="form-group">
              <label class="form-label">資源</label>
              <input v-model="form.resource" class="form-input" placeholder="請輸入資源名稱" />
            </div>
            <div class="form-group">
              <label class="form-label">操作</label>
              <select v-model="form.action" class="form-input">
                <option value="">請選擇</option>
                <option value="read">讀取</option>
                <option value="write">寫入</option>
                <option value="delete">刪除</option>
                <option value="all">全部</option>
              </select>
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TopNavbar from '@/components/TopNavbar.vue'
import { apiService } from '@/composables/useApi'

const permissions = ref([])
const showAddModal = ref(false)
const editingPermission = ref(null)
const notification = ref({ show: false, message: '', type: 'success' })

const form = ref({
  permissionCode: '',
  permissionName: '',
  resource: '',
  action: '',
  description: ''
})

const loadPermissions = async () => {
  try {
    permissions.value = await apiService.getPermissions()
  } catch (error) {
    showNotification('載入權限失敗', 'error')
  }
}

const handleSubmit = async () => {
  try {
    if (editingPermission.value) {
      await apiService.updatePermission(editingPermission.value.id, form.value)
      showNotification('權限已更新', 'success')
    } else {
      await apiService.createPermission(form.value)
      showNotification('權限已新增', 'success')
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
    permissionCode: permission.permissionCode,
    permissionName: permission.permissionName,
    resource: permission.resource || '',
    action: permission.action || '',
    description: permission.description || ''
  }
}

const deletePermission = async (id) => {
  if (!confirm('確定要刪除這個權限嗎？')) return
  try {
    await apiService.deletePermission(id)
    showNotification('權限已刪除', 'success')
    await loadPermissions()
  } catch (error) {
    showNotification('刪除失敗', 'error')
  }
}

const closeModal = () => {
  showAddModal.value = false
  editingPermission.value = null
  form.value = {
    permissionCode: '',
    permissionName: '',
    resource: '',
    action: '',
    description: ''
  }
}

const showNotification = (message, type = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => { notification.value.show = false }, 3000)
}

onMounted(loadPermissions)
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 2rem;
}

.header {
  background: white;
  padding: 2rem;
  margin-bottom: 2rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
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
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  border-radius: 6px;
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
  background: white;
  border-radius: 12px;
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
  border-radius: 6px;
  font-size: 0.95rem;
  transition: all 0.2s;
  background: white;
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

.text-danger { color: #ef4444; }
.me-2 { margin-right: 0.5rem; }

.notification {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  left: auto;
  padding: 1rem 1.5rem;
  border-radius: 8px;
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
