<template>
  <div class="admin-page">
    <header class="header">
      <h1>📑 菜單管理</h1>
      <button class="btn btn-primary" @click="showAddModal = true">新增菜單</button>
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

.status-active {
  color: #4ade80;
}

.status-inactive {
  color: #f87171;
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
.form-group select,
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
