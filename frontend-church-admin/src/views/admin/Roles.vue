<template>
  <AdminLayout>
    <div class="admin-roles">
      <div class="page-header">
        <h2>角色管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增角色</button>
      </div>

      <!-- 查詢條件 -->
      <details class="filters filters--collapsible" open>
        <summary>
          <div class="filters__title">
            <h3>查詢條件</h3>
            <span class="filters__badge">點擊可收合</span>
          </div>
          <div class="filters__chev" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 9l6 6 6-6"/>
            </svg>
          </div>
        </summary>
        <div class="filters__content">
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
        </div>
      </details>

      <div class="roles-list">
        <div v-if="roles.length === 0" class="empty-state">
          <p>尚無角色資料</p>
        </div>
        <div v-else class="roles-table">
          <div class="table-header">
            <h3>角色列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>角色名稱</th>
                <th>描述</th>
                <th>權限數量</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="role in roles" :key="role.id">
                <td>{{ role.roleName }}</td>
                <td>{{ role.description || '-' }}</td>
                <td>{{ role.permissions ? role.permissions.length : 0 }}</td>
                <td><div class="table-actions"><button @click="editRole(role.id)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
<button @click="deleteRole(role.id)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
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
              <span class="pagination-info">共 {{ totalRecords }} 筆 (第 {{ currentPage }}/{{ totalPages }} 頁)</span>
            </div>
            <div class="pagination-right">
              <button class="btn-secondary" @click="firstPage" :disabled="currentPage === 1" title="第一頁">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7"/>
                </svg>
              </button>
              <button class="btn-secondary" @click="previousPage" :disabled="currentPage === 1">
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
              <button class="btn-secondary" @click="nextPage" :disabled="currentPage === totalPages">
                下一頁
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </button>
              <button class="btn-secondary" @click="lastPage" :disabled="currentPage === totalPages" title="最後一頁">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7"/>
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
  </AdminLayout>
</template>

<script setup>
import { toast } from '@shared/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import RoleModal from '@/components/RoleModal.vue'
import { apiRequest } from '@/utils/api'

const roles = ref([])
const availablePermissions = ref([])
const showModal = ref(false)
const selectedRole = ref(null)

// 查詢條件
const filters = ref({
  roleName: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)
const totalRecords = ref(0)
const totalPages = ref(1)

// 第一頁
const firstPage = () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadRoles()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadRoles()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadRoles()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadRoles()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadRoles()
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
  loadRoles()
}

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => filters.value.roleName, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadRoles()
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadRoles()
})

const loadRoles = async () => {
  try {
    const params = new URLSearchParams()
    if (filters.value.roleName) {
      params.append('roleName', filters.value.roleName)
    }
    params.append('page', (currentPage.value - 1).toString())
    params.append('size', recordsPerPage.value.toString())
    
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/roles?${params.toString()}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // apiRequest 已經提取了 ApiResponse.data，所以 data 可能是：
      // 1. PageResponse 對象（有 content 字段）
      // 2. 直接數組
      // 3. 其他格式
      const rolesData = data.content || (Array.isArray(data) ? data : [])
      roles.value = Array.isArray(rolesData) ? rolesData : []
      
      // 更新分頁信息
      if (responseData.totalElements !== undefined) {
        totalRecords.value = responseData.totalElements
        totalPages.value = responseData.totalPages || 1
        // 確保 currentPage 不超過 totalPages
        if (currentPage.value > totalPages.value) {
          currentPage.value = totalPages.value
          jumpPage.value = totalPages.value
        }
        // 同步 jumpPage 與 currentPage
        jumpPage.value = currentPage.value
      } else if (data.totalElements !== undefined) {
        totalRecords.value = data.totalElements
        totalPages.value = data.totalPages || 1
        if (currentPage.value > totalPages.value) {
          currentPage.value = totalPages.value
          jumpPage.value = totalPages.value
        }
        jumpPage.value = currentPage.value
      } else {
        totalRecords.value = roles.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
      }
    }
  } catch (error) {
    console.error('載入角色失敗:', error)
  }
}

const loadPermissions = async () => {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/admin/permissions', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 處理 PageResponse 格式或直接列表
      const permissionsData = data.content || data.permissions || data || []
      availablePermissions.value = Array.isArray(permissionsData) ? permissionsData : []
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
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/roles/${id}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // apiRequest 已經提取了 ApiResponse.data，所以 data 直接是角色對象
      selectedRole.value = data
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




const deleteRole = async (id) => {
  if (!confirm('確定要刪除此角色嗎？')) {
    return
  }
  
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/roles/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (data !== null) {
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

/* Table column widths */
:deep(.table){ table-layout: fixed; width: 100%; }
:deep(.table th.col-actions), :deep(.table td.col-actions){ width: 240px; min-width: 240px; }

</style>
