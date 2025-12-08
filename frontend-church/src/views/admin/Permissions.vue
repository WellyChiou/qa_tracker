<template>
  <AdminLayout>
    <div class="admin-permissions">
      <div class="page-header">
        <h2>權限管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增權限</button>
      </div>

      <div class="permissions-list">
        <div v-if="permissions.length === 0" class="empty-state">
          <p>尚無權限資料</p>
        </div>
        <div v-else class="permissions-table">
          <table>
            <thead>
              <tr>
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
                <td>{{ permission.permissionCode }}</td>
                <td>{{ permission.permissionName }}</td>
                <td>{{ permission.resource || '-' }}</td>
                <td>{{ permission.action || '-' }}</td>
                <td>{{ permission.description || '-' }}</td>
                <td>
                  <button @click="editPermission(permission.id)" class="btn btn-edit">編輯</button>
                  <button @click="deletePermission(permission.id)" class="btn btn-delete">刪除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    
    <PermissionModal
      :show="showModal"
      :permission="selectedPermission"
      @close="closeModal"
      @saved="handleSaved"
    />
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import PermissionModal from '@/components/PermissionModal.vue'
import { apiRequest } from '@/utils/api'

const permissions = ref([])
const showModal = ref(false)
const selectedPermission = ref(null)

const loadPermissions = async () => {
  try {
    const response = await apiRequest('/church/admin/permissions', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      permissions.value = data.permissions || data || []
    }
  } catch (error) {
    console.error('載入權限失敗:', error)
  }
}

const openAddModal = () => {
  selectedPermission.value = null
  showModal.value = true
}

const editPermission = async (id) => {
  try {
    const response = await apiRequest(`/church/admin/permissions/${id}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      selectedPermission.value = data.permission || data
      showModal.value = true
    } else {
      alert('載入權限資料失敗')
    }
  } catch (error) {
    console.error('載入權限資料失敗:', error)
    alert('載入權限資料失敗: ' + error.message)
  }
}

const closeModal = () => {
  showModal.value = false
  selectedPermission.value = null
}

const handleSaved = () => {
  loadPermissions()
}

const deletePermission = async (id) => {
  if (!confirm('確定要刪除此權限嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/admin/permissions/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadPermissions()
    } else {
      alert('刪除失敗')
    }
  } catch (error) {
    console.error('刪除權限失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadPermissions()
})
</script>

<style scoped>
.admin-permissions {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h2 {
  margin: 0;
  font-size: 1.8rem;
  color: #333;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.permissions-list {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.permissions-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f5f5f5;
}

th, td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  font-weight: 600;
  color: #333;
}

.btn-edit {
  background: #667eea;
  color: white;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
}

.btn-edit:hover {
  background: #5568d3;
}

.btn-delete {
  background: #ef4444;
  color: white;
  padding: 0.5rem 1rem;
}

.btn-delete:hover {
  background: #dc2626;
}
</style>

