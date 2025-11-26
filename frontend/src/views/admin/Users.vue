<template>
  <div class="admin-page">
    <header class="header">
      <h1>👥 用戶管理</h1>
      <button class="btn btn-primary" @click="showAddModal = true">新增用戶</button>
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
              <button class="btn-sm btn-delete" @click="deleteUser(user.uid)">刪除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </main>

    <!-- 新增/編輯模態框 -->
    <div v-if="showAddModal || editingUser" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h2>{{ editingUser ? '編輯用戶' : '新增用戶' }}</h2>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>UID</label>
            <input v-model="form.uid" :disabled="!!editingUser" required />
          </div>
          <div class="form-group">
            <label>用戶名</label>
            <input v-model="form.username" required />
          </div>
          <div class="form-group">
            <label>Email</label>
            <input type="email" v-model="form.email" />
          </div>
          <div class="form-group">
            <label>顯示名稱</label>
            <input v-model="form.displayName" />
          </div>
          <div class="form-group">
            <label>密碼</label>
            <input type="password" v-model="form.password" :required="!editingUser" />
          </div>
          <div class="form-group">
            <label>啟用</label>
            <input type="checkbox" v-model="form.isEnabled" />
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

const users = ref([])
const showAddModal = ref(false)
const editingUser = ref(null)
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

const handleSubmit = async () => {
  try {
    const userData = { ...form.value }
    if (!userData.password) delete userData.password
    
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

onMounted(loadUsers)
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

.badge {
  display: inline-block;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  margin-right: 4px;
  font-size: 0.85rem;
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

.form-group input {
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
