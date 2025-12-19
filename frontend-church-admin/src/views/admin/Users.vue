<template>
  <AdminLayout>
    <div class="admin-users">
      <div class="page-header">
        <h2>用戶管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增用戶</button>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
        <div class="filter-grid">
          <div class="filter-group">
            <label>用戶名</label>
            <input
              type="text"
              v-model="filters.username"
              placeholder="輸入用戶名"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>電子郵件</label>
            <input
              type="text"
              v-model="filters.email"
              placeholder="輸入電子郵件"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>角色</label>
            <select v-model="filters.roleId">
              <option value="">全部</option>
              <option v-for="role in availableRoles" :key="role.id" :value="role.id">
                {{ role.roleName }}
              </option>
            </select>
          </div>
          <div class="filter-group">
            <label>狀態</label>
            <select v-model="filters.isEnabled">
              <option value="">全部</option>
              <option :value="true">啟用</option>
              <option :value="false">停用</option>
            </select>
          </div>
          <div class="filter-group">
            <button @click="resetFilters" class="btn btn-secondary">清除條件</button>
          </div>
        </div>
      </section>

      <div class="users-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ users.length === 0 ? '尚無用戶資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="users-table">
          <div class="table-header">
            <h3>用戶列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>用戶名</th>
                <th>顯示名稱</th>
                <th>電子郵件</th>
                <th>角色</th>
                <th>狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in paginatedList" :key="user.uid">
                <td>{{ user.username }}</td>
                <td>{{ user.displayName || '-' }}</td>
                <td>{{ user.email || '-' }}</td>
                <td>
                  <span v-for="role in user.roles" :key="role.id" class="role-badge">
                    {{ role.roleName }}
                  </span>
                  <span v-if="!user.roles || user.roles.length === 0">-</span>
                </td>
                <td>
                  <span :class="user.isEnabled ? 'status-active' : 'status-inactive'">
                    {{ user.isEnabled ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>
                  <button @click="editUser(user.uid)" class="btn btn-edit">編輯</button>
                  <button @click="editRoles(user)" class="btn btn-roles">角色</button>
                  <button @click="editPermissions(user)" class="btn btn-permissions">權限</button>
                  <button @click="deleteUser(user.uid)" class="btn btn-delete">刪除</button>
                </td>
              </tr>
            </tbody>
          </table>
          
          <!-- 分頁 -->
          <div class="pagination">
            <div class="pagination-left">
              <label for="pageSize" class="pagination-label">顯示筆數：</label>
              <select id="pageSize" v-model.number="recordsPerPage" class="page-size-select">
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
                <option :value="100">100</option>
              </select>
              <span class="pagination-info">共 {{ filteredList.length }} 筆 (第 {{ currentPage }}/{{ totalPages }} 頁)</span>
            </div>
            <div class="pagination-right">
              <button class="btn-secondary" @click="currentPage--" :disabled="currentPage === 1">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
                </svg>
                上一頁
              </button>
              <div class="page-jump">
                <span class="pagination-label">到第</span>
                <input type="number" v-model.number="jumpPage" min="1" :max="totalPages" class="page-input" @keyup.enter="jumpToPage" />
                <span class="pagination-label">頁</span>
              </div>
              <button class="btn-secondary" @click="currentPage++" :disabled="currentPage === totalPages">
                下一頁
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <UserModal
      :show="showModal"
      :user="selectedUser"
      :available-roles="availableRoles"
      @close="closeModal"
      @saved="handleSaved"
    />
    
    <!-- 角色分配模態框 -->
    <div v-if="showRolesModal" class="modal-overlay" @click="closeRolesModal">
      <div class="modal-panel" @click.stop style="max-width: 600px;">
        <div class="modal-header">
          <h2 class="modal-title">分配角色: {{ selectedUser?.displayName || selectedUser?.username }}</h2>
          <button class="btn-close" @click="closeRolesModal">×</button>
        </div>
        <div class="modal-body">
          <div class="roles-list">
            <label v-for="role in availableRoles" :key="role.id" class="role-item">
              <input 
                type="checkbox" 
                :value="role.id" 
                v-model="selectedRoleIds"
                class="checkbox-input" />
              <span>{{ role.roleName }}</span>
              <small v-if="role.description" class="role-description">{{ role.description }}</small>
            </label>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-primary" @click="saveRoles">儲存角色</button>
            <button type="button" class="btn btn-secondary" @click="closeRolesModal">取消</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 權限分配模態框 -->
    <div v-if="showPermissionsModal" class="modal-overlay" @click="closePermissionsModal">
      <div class="modal-panel" @click.stop style="max-width: 700px;">
        <div class="modal-header">
          <h2 class="modal-title">分配權限: {{ selectedUser?.displayName || selectedUser?.username }}</h2>
          <button class="btn-close" @click="closePermissionsModal">×</button>
        </div>
        <div class="modal-body">
          <div class="permissions-list">
            <label v-for="permission in availablePermissions" :key="permission.id" class="permission-item">
              <input 
                type="checkbox" 
                :value="permission.id" 
                v-model="selectedPermissionIds"
                class="checkbox-input" />
              <span>{{ permission.permissionName }} <code class="permission-code">({{ permission.permissionCode }})</code></span>
            </label>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-primary" @click="savePermissions">儲存權限</button>
            <button type="button" class="btn btn-secondary" @click="closePermissionsModal">取消</button>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import UserModal from '@/components/UserModal.vue'
import { apiRequest } from '@/utils/api'

const users = ref([])
const availableRoles = ref([])
const availablePermissions = ref([])
const showModal = ref(false)
const selectedUser = ref(null)
const showRolesModal = ref(false)
const showPermissionsModal = ref(false)
const selectedRoleIds = ref([])
const selectedPermissionIds = ref([])

// 查詢條件
const filters = ref({
  username: '',
  email: '',
  roleId: '',
  isEnabled: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...users.value]
  
  if (filters.value.username) {
    filtered = filtered.filter(user => 
      user.username?.toLowerCase().includes(filters.value.username.toLowerCase())
    )
  }
  
  if (filters.value.email) {
    filtered = filtered.filter(user => 
      (user.email || '').toLowerCase().includes(filters.value.email.toLowerCase())
    )
  }
  
  if (filters.value.roleId) {
    filtered = filtered.filter(user => 
      user.roles && user.roles.some(role => role.id === filters.value.roleId)
    )
  }
  
  if (filters.value.isEnabled !== '') {
    filtered = filtered.filter(user => user.isEnabled === filters.value.isEnabled)
  }
  
  return filtered
})

// 分頁後的列表
const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * recordsPerPage.value
  return filteredList.value.slice(start, start + recordsPerPage.value)
})

// 總頁數
const totalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredList.value.length / recordsPerPage.value))
})

// 跳轉到指定頁
const jumpToPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    currentPage.value = jumpPage.value
  } else {
    jumpPage.value = currentPage.value
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    username: '',
    email: '',
    roleId: '',
    isEnabled: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => [filters.value.username, filters.value.email, filters.value.roleId, filters.value.isEnabled], () => {
  currentPage.value = 1
  jumpPage.value = 1
})

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const loadUsers = async () => {
  try {
    const response = await apiRequest('/church/admin/users', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      users.value = data.users || data || []
    }
  } catch (error) {
    console.error('載入用戶失敗:', error)
  }
}

const loadRoles = async () => {
  try {
    const response = await apiRequest('/church/admin/roles', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      availableRoles.value = data.roles || data || []
    }
  } catch (error) {
    console.error('載入角色失敗:', error)
  }
}

const loadPermissions = async () => {
  try {
    const response = await apiRequest('/church/admin/permissions', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      availablePermissions.value = data.permissions || data || []
    }
  } catch (error) {
    console.error('載入權限失敗:', error)
  }
}

const openAddModal = () => {
  selectedUser.value = null
  showModal.value = true
}

const editUser = async (uid) => {
  try {
    const response = await apiRequest(`/church/admin/users/${uid}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      selectedUser.value = data.user || data
      showModal.value = true
    } else {
      toast.error('載入用戶資料失敗')
    }
  } catch (error) {
    console.error('載入用戶資料失敗:', error)
    toast.error('載入用戶資料失敗: ' + error.message)
  }
}

const closeModal = () => {
  showModal.value = false
  selectedUser.value = null
}

const handleSaved = () => {
  loadUsers()
}

const editRoles = async (user) => {
  selectedUser.value = user
  selectedRoleIds.value = user.roles ? user.roles.map(r => r.id) : []
  showRolesModal.value = true
}

const closeRolesModal = () => {
  showRolesModal.value = false
  selectedUser.value = null
  selectedRoleIds.value = []
}

const saveRoles = async () => {
  try {
    const response = await apiRequest(`/church/admin/users/${selectedUser.value.uid}/roles`, {
      method: 'POST',
      body: JSON.stringify({ roleIds: selectedRoleIds.value }),
      credentials: 'include'
    })
    
    if (response.ok) {
      closeRolesModal()
      loadUsers()
    } else {
      const data = await response.json()
      toast.error(data.message || data.error || '更新角色失敗')
    }
  } catch (error) {
    console.error('更新角色失敗:', error)
    toast.error('更新角色失敗: ' + error.message)
  }
}

const editPermissions = async (user) => {
  selectedUser.value = user
  selectedPermissionIds.value = user.permissions ? user.permissions.map(p => p.id) : []
  showPermissionsModal.value = true
}

const closePermissionsModal = () => {
  showPermissionsModal.value = false
  selectedUser.value = null
  selectedPermissionIds.value = []
}

const savePermissions = async () => {
  try {
    const response = await apiRequest(`/church/admin/users/${selectedUser.value.uid}/permissions`, {
      method: 'POST',
      body: JSON.stringify({ permissionIds: selectedPermissionIds.value }),
      credentials: 'include'
    })
    
    if (response.ok) {
      closePermissionsModal()
      loadUsers()
    } else {
      const data = await response.json()
      toast.error(data.message || data.error || '更新權限失敗')
    }
  } catch (error) {
    console.error('更新權限失敗:', error)
    toast.error('更新權限失敗: ' + error.message)
  }
}

const deleteUser = async (uid) => {
  if (!confirm('確定要刪除此用戶嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/admin/users/${uid}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadUsers()
    } else {
      toast.error('刪除失敗')
    }
  } catch (error) {
    console.error('刪除用戶失敗:', error)
    toast.error('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadUsers()
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.admin-users{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-users .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-users .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-users .page-header p,
.admin-users .subtitle,
.admin-users .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-users .table-container,
.admin-users .list-container,
.admin-users .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-users .table-container{ padding:0; }

/* Inline helpers */
.admin-users .hint,
.admin-users .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-users .actions,
.admin-users .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
