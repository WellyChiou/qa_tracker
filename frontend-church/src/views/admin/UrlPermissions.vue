<template>
  <AdminLayout>
    <div class="admin-url-permissions">
      <div class="page-header">
        <h2>URL 權限管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增 URL 權限</button>
      </div>

      <div class="url-permissions-list">
        <div v-if="permissions.length === 0" class="empty-state">
          <p>尚無 URL 權限資料</p>
        </div>
        <div v-else class="url-permissions-table">
          <table>
            <thead>
              <tr>
                <th>URL 模式</th>
                <th>HTTP 方法</th>
                <th>是否公開</th>
                <th>需要角色</th>
                <th>需要權限</th>
                <th>是否啟用</th>
                <th>排序</th>
                <th>描述</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="permission in permissions" :key="permission.id">
                <td>{{ permission.urlPattern }}</td>
                <td>{{ permission.httpMethod || '全部' }}</td>
                <td>{{ permission.isPublic ? '是' : '否' }}</td>
                <td>{{ permission.requiredRole || '-' }}</td>
                <td>{{ permission.requiredPermission || '-' }}</td>
                <td>{{ permission.isActive ? '是' : '否' }}</td>
                <td>{{ permission.orderIndex }}</td>
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
    
    <UrlPermissionModal
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
import UrlPermissionModal from '@/components/UrlPermissionModal.vue'
import { apiRequest } from '@/utils/api'

const permissions = ref([])
const showModal = ref(false)
const selectedPermission = ref(null)

const loadPermissions = async () => {
  try {
    const response = await apiRequest('/church/admin/url-permissions', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      permissions.value = data.permissions || data || []
    }
  } catch (error) {
    console.error('載入 URL 權限失敗:', error)
  }
}

const openAddModal = () => {
  selectedPermission.value = null
  showModal.value = true
}

const editPermission = async (id) => {
  try {
    const response = await apiRequest(`/church/admin/url-permissions/${id}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      selectedPermission.value = data.permission || data
      showModal.value = true
    } else {
      alert('載入 URL 權限資料失敗')
    }
  } catch (error) {
    console.error('載入 URL 權限資料失敗:', error)
    alert('載入 URL 權限資料失敗: ' + error.message)
  }
}

const closeModal = () => {
  showModal.value = false
  selectedPermission.value = null
}

const handleSaved = () => {
  loadPermissions()
  closeModal()
}

const deletePermission = async (id) => {
  if (!confirm('確定要刪除此 URL 權限嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/admin/url-permissions/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadPermissions()
    } else {
      alert('刪除失敗')
    }
  } catch (error) {
    console.error('刪除 URL 權限失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadPermissions()
})
</script>

<style scoped>
.admin-url-permissions {
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

.url-permissions-list {
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

.url-permissions-table {
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

tbody tr:hover {
  background: #f9f9f9;
}

.btn-edit {
  background: #667eea;
  color: white;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.btn-edit:hover {
  background: #5568d3;
}

.btn-delete {
  background: #ef4444;
  color: white;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.btn-delete:hover {
  background: #dc2626;
}
</style>

