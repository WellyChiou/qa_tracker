<template>
  <AdminLayout>
    <div class="admin-page">
    <header class="header">
      <div class="header-top">
        <h1>📑 菜單管理</h1>
        <button class="btn btn-primary btn-add" @click="showAddModal = true">
          <i class="fas fa-plus me-2"></i>新增菜單
        </button>
      </div>
    </header>

    <main class="main-content">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>菜單代碼</th>
            <th>菜單名稱</th>
            <th>圖標</th>
            <th>URL</th>
            <th>父菜單</th>
            <th>排序</th>
            <th>啟用</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="menu in menus" :key="menu.id">
            <td>{{ menu.id }}</td>
            <td>{{ menu.menuCode }}</td>
            <td>{{ menu.menuName }}</td>
            <td>{{ menu.icon || '-' }}</td>
            <td>{{ menu.url || '-' }}</td>
            <td>{{ getParentName(menu.parentId) }}</td>
            <td>{{ menu.orderIndex }}</td>
            <td>
              <span :class="menu.isActive ? 'status-active' : 'status-inactive'">
                {{ menu.isActive ? '是' : '否' }}
              </span>
            </td>
            <td class="actions">
              <button class="btn-sm btn-edit" @click="editMenu(menu)">編輯</button>
              <button class="btn-sm btn-delete" @click="deleteMenu(menu.id)">刪除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </main>

    <!-- 新增/編輯模態框 -->
    <div v-if="showAddModal || editingMenu" class="modal-overlay" @click="closeModal">
      <div class="modal-panel" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">{{ editingMenu ? '編輯菜單' : '新增菜單' }}</h2>
          <button class="btn-close" @click="closeModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSubmit" class="form-container">
            <div class="form-group">
              <label class="form-label">菜單代碼 <span class="text-danger">*</span></label>
              <input v-model="form.menuCode" required class="form-input" placeholder="請輸入菜單代碼" />
            </div>
            <div class="form-group">
              <label class="form-label">菜單名稱 <span class="text-danger">*</span></label>
              <input v-model="form.menuName" required class="form-input" placeholder="請輸入菜單名稱" />
            </div>
            <div class="form-group">
              <label class="form-label">圖標</label>
              <input v-model="form.icon" class="form-input" placeholder="例如: 📊 或 icon-class" />
            </div>
            <div class="form-group">
              <label class="form-label">URL</label>
              <input v-model="form.url" class="form-input" placeholder="/path 或 #" />
            </div>
            <div class="form-group">
              <label class="form-label">父菜單</label>
              <select v-model.number="form.parentId" class="form-input">
                <option :value="null">無（頂層菜單）</option>
                <option v-for="m in menus" :key="m.id" :value="m.id">{{ m.menuName }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">排序</label>
              <input type="number" v-model.number="form.orderIndex" class="form-input" placeholder="0" />
            </div>
            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="form.isActive" class="checkbox-input" />
                <span>啟用</span>
              </label>
            </div>
            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="form.showInDashboard" class="checkbox-input" />
                <span>顯示在儀表板</span>
              </label>
            </div>
            <div class="form-group">
              <label class="form-label">所需權限</label>
              <input v-model="form.requiredPermission" class="form-input" placeholder="請輸入權限代碼" />
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

const menus = ref([])
const showAddModal = ref(false)
const editingMenu = ref(null)
const notification = ref({ show: false, message: '', type: 'success' })

const form = ref({
  menuCode: '',
  menuName: '',
  icon: '',
  url: '',
  parentId: null,
  orderIndex: 0,
  isActive: true,
  showInDashboard: true,
  requiredPermission: '',
  description: ''
})

const loadMenus = async () => {
  try {
    menus.value = await apiService.getAllMenuItems()
  } catch (error) {
    showNotification('載入菜單失敗', 'error')
  }
}

const getParentName = (parentId) => {
  if (!parentId) return '-'
  const parent = menus.value.find(m => m.id === parentId)
  return parent ? parent.menuName : '-'
}

const handleSubmit = async () => {
  try {
    const menuData = { ...form.value }
    if (!menuData.parentId) menuData.parentId = null
    
    if (editingMenu.value) {
      await apiService.updateMenuItem(editingMenu.value.id, menuData)
      showNotification('菜單已更新', 'success')
    } else {
      await apiService.createMenuItem(menuData)
      showNotification('菜單已新增', 'success')
    }
    closeModal()
    await loadMenus()
  } catch (error) {
    showNotification(error.message || '操作失敗', 'error')
  }
}

const editMenu = (menu) => {
  editingMenu.value = menu
  form.value = {
    menuCode: menu.menuCode,
    menuName: menu.menuName,
    icon: menu.icon || '',
    url: menu.url || '',
    parentId: menu.parentId || null,
    orderIndex: menu.orderIndex || 0,
    isActive: menu.isActive !== false,
    showInDashboard: menu.showInDashboard !== false,
    requiredPermission: menu.requiredPermission || '',
    description: menu.description || ''
  }
}

const deleteMenu = async (id) => {
  if (!confirm('確定要刪除這個菜單嗎？')) return
  try {
    await apiService.deleteMenuItem(id)
    showNotification('菜單已刪除', 'success')
    await loadMenus()
  } catch (error) {
    showNotification('刪除失敗', 'error')
  }
}

const closeModal = () => {
  showAddModal.value = false
  editingMenu.value = null
  form.value = {
    menuCode: '',
    menuName: '',
    icon: '',
    url: '',
    parentId: null,
    orderIndex: 0,
    isActive: true,
    showInDashboard: true,
    requiredPermission: '',
    description: ''
  }
}

const showNotification = (message, type = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => { notification.value.show = false }, 3000)
}

onMounted(loadMenus)
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
</style>
