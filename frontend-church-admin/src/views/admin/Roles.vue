<template>
  <AdminLayout>
    <div class="admin-roles">
      <div class="page-header">
        <h2>角色管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增角色</button>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
        <div class="filter-grid">
          <div class="filter-group">
            <label>角色名稱</label>
            <input
              type="text"
              v-model="filters.roleName"
              placeholder="輸入角色名稱"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <button @click="resetFilters" class="btn btn-secondary">清除條件</button>
          </div>
        </div>
      </section>

      <div class="roles-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ roles.length === 0 ? '尚無角色資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="roles-table">
          <div class="table-header">
            <h3>角色列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>角色名稱</th>
                <th>描述</th>
                <th>權限數量</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="role in paginatedList" :key="role.id">
                <td>{{ role.roleName }}</td>
                <td>{{ role.description || '-' }}</td>
                <td>{{ role.permissions ? role.permissions.length : 0 }}</td>
                <td>
                  <button @click="editRole(role.id)" class="btn btn-edit">編輯</button>
                  <button @click="editPermissions(role)" class="btn btn-permissions">權限</button>
                  <button @click="deleteRole(role.id)" class="btn btn-delete">刪除</button>
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
    
    <RoleModal
      :show="showModal"
      :role="selectedRole"
      :available-permissions="availablePermissions"
      @close="closeModal"
      @saved="handleSaved"
    />
    
    <!-- 權限管理模態框 -->
    <div v-if="showPermissionsModal" class="modal-overlay" @click="closePermissionsModal">
      <div class="modal-panel" @click.stop style="max-width: 700px;">
        <div class="modal-header">
          <h2 class="modal-title">管理角色權限: {{ selectedRole?.roleName }}</h2>
          <button class="btn-close" @click="closePermissionsModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="permissions-list">
            <label v-for="permission in availablePermissions" :key="permission.id" class="permission-item">
              <input type="checkbox" 
                :value="permission.id" 
                v-model="selectedPermissionIds"
                class="checkbox-input" />
              <span>{{ permission.permissionName }} <code class="permission-code">({{ permission.permissionCode }})</code></span>
            </label>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-primary" @click="savePermissions">
              <i class="fas fa-save me-2"></i>儲存權限
            </button>
            <button type="button" class="btn btn-secondary" @click="closePermissionsModal">
              <i class="fas fa-times me-2"></i>取消
            </button>
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
import RoleModal from '@/components/RoleModal.vue'
import { apiRequest } from '@/utils/api'

const roles = ref([])
const availablePermissions = ref([])
const showModal = ref(false)
const selectedRole = ref(null)
const showPermissionsModal = ref(false)
const selectedPermissionIds = ref([])

// 查詢條件
const filters = ref({
  roleName: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...roles.value]
  
  if (filters.value.roleName) {
    filtered = filtered.filter(role => 
      role.roleName?.toLowerCase().includes(filters.value.roleName.toLowerCase())
    )
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
    roleName: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => filters.value.roleName, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const loadRoles = async () => {
  try {
    const response = await apiRequest('/church/admin/roles', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      roles.value = data.roles || data || []
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
  selectedRole.value = null
  showModal.value = true
}

const editRole = async (id) => {
  try {
    const response = await apiRequest(`/church/admin/roles/${id}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      selectedRole.value = data.role || data
      showModal.value = true
    } else {
      toast.error('載入角色資料失敗')
    }
  } catch (error) {
    console.error('載入角色資料失敗:', error)
    toast.error('載入角色資料失敗: ' + error.message)
  }
}

const closeModal = () => {
  showModal.value = false
  selectedRole.value = null
}

const handleSaved = () => {
  loadRoles()
}

const editPermissions = async (role) => {
  selectedRole.value = role
  selectedPermissionIds.value = role.permissions ? role.permissions.map(p => p.id) : []
  showPermissionsModal.value = true
}

const savePermissions = async () => {
  try {
    const response = await apiRequest(`/church/admin/roles/${selectedRole.value.id}/permissions`, {
      method: 'POST',
      body: JSON.stringify({ permissionIds: selectedPermissionIds.value }),
      credentials: 'include'
    })
    
    if (response.ok) {
      closePermissionsModal()
      loadRoles()
    } else {
      const data = await response.json()
      toast.error(data.message || data.error || '更新權限失敗')
    }
  } catch (error) {
    console.error('更新權限失敗:', error)
    toast.error('更新權限失敗: ' + error.message)
  }
}

const closePermissionsModal = () => {
  showPermissionsModal.value = false
  selectedRole.value = null
  selectedPermissionIds.value = []
}

const deleteRole = async (id) => {
  if (!confirm('確定要刪除此角色嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/admin/roles/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadRoles()
    } else {
      toast.error('刪除失敗')
    }
  } catch (error) {
    console.error('刪除角色失敗:', error)
    toast.error('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.admin-roles{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-roles .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-roles .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-roles .page-header p,
.admin-roles .subtitle,
.admin-roles .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-roles .table-container,
.admin-roles .list-container,
.admin-roles .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-roles .table-container{ padding:0; }

/* Inline helpers */
.admin-roles .hint,
.admin-roles .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-roles .actions,
.admin-roles .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
