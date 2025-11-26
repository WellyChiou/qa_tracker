<template>
  <div class="admin-page">
    <TopNavbar />
    <header class="header">
      <div class="header-top">
        <h1>📑 菜單管理</h1>
        <button class="btn btn-primary" @click="showAddModal = true">新增菜單</button>
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
      <div class="modal-content" @click.stop>
        <h2>{{ editingMenu ? '編輯菜單' : '新增菜單' }}</h2>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>菜單代碼</label>
            <input v-model="form.menuCode" required />
          </div>
          <div class="form-group">
            <label>菜單名稱</label>
            <input v-model="form.menuName" required />
          </div>
          <div class="form-group">
            <label>圖標</label>
            <input v-model="form.icon" placeholder="例如: 📊 或 icon-class" />
          </div>
          <div class="form-group">
            <label>URL</label>
            <input v-model="form.url" placeholder="/path 或 #" />
          </div>
          <div class="form-group">
            <label>父菜單</label>
            <select v-model.number="form.parentId">
              <option :value="null">無（頂層菜單）</option>
              <option v-for="m in menus" :key="m.id" :value="m.id">{{ m.menuName }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>排序</label>
            <input type="number" v-model.number="form.orderIndex" />
          </div>
          <div class="form-group">
            <label>啟用</label>
            <input type="checkbox" v-model="form.isActive" />
          </div>
          <div class="form-group">
            <label>顯示在儀表板</label>
            <input type="checkbox" v-model="form.showInDashboard" />
          </div>
          <div class="form-group">
            <label>所需權限</label>
            <input v-model="form.requiredPermission" />
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
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.5);
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
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: var(--bg-card);
  color: var(--text-primary);
  padding: var(--spacing-2xl);
  border-radius: var(--border-radius-xl);
  min-width: 400px;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: var(--shadow-xl);
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

.modal-content h2 {
  margin-top: 0;
  margin-bottom: var(--spacing-xl);
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
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

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid var(--border-color);
  border-radius: var(--border-radius);
  font-size: 15px;
  transition: var(--transition);
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-group input:disabled {
  background: var(--bg-primary);
  opacity: 0.6;
  cursor: not-allowed;
}

.form-group input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: var(--primary-color);
}

.form-actions {
  display: flex;
  gap: var(--spacing-md);
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-color);
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
