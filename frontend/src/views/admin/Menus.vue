<template>
  <div class="admin-page">
    <TopNavbar />
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TopNavbar from '@/components/TopNavbar.vue'
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
  background: rgba(59, 130, 246, 0.25);
  color: #93c5fd;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.btn-edit:hover {
  background: rgba(59, 130, 246, 0.35);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-delete {
  background: rgba(239, 68, 68, 0.25);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.btn-delete:hover {
  background: rgba(239, 68, 68, 0.35);
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
  max-width: 700px;
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
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-weight: 600;
  color: #374151;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 0.5rem;
  font-size: 0.9375rem;
  transition: all 0.2s;
  background: white;
  color: #1f2937;
  font-family: inherit;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input::placeholder {
  color: #9ca3af;
}

textarea.form-input {
  resize: vertical;
  min-height: 80px;
}

.checkbox-group {
  margin-top: 0.5rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  font-weight: 500;
  color: #374151;
}

.checkbox-input {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
  accent-color: #667eea;
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

.text-danger {
  color: #ef4444;
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
