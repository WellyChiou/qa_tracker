<template>
  <AdminLayout>
    <div class="admin-permissions">
      <div class="page-header">
        <h2>權限管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增權限</button>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
        <div class="filter-grid">
          <div class="filter-group">
            <label>權限代碼</label>
            <input
              type="text"
              v-model="filters.permissionCode"
              placeholder="輸入權限代碼"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>資源</label>
            <input
              type="text"
              v-model="filters.resource"
              placeholder="輸入資源"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>操作</label>
            <input
              type="text"
              v-model="filters.action"
              placeholder="輸入操作"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <button @click="resetFilters" class="btn btn-secondary">清除條件</button>
          </div>
        </div>
      </section>

      <div class="permissions-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ permissions.length === 0 ? '尚無權限資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="permissions-table">
          <div class="table-header">
            <h3>權限列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
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
              <tr v-for="permission in paginatedList" :key="permission.id">
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
    
    <PermissionModal
      :show="showModal"
      :permission="selectedPermission"
      @close="closeModal"
      @saved="handleSaved"
    />
  </AdminLayout>
</template>

<script setup>
import { toast } from '@/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import PermissionModal from '@/components/PermissionModal.vue'
import { apiRequest } from '@/utils/api'

const permissions = ref([])
const showModal = ref(false)
const selectedPermission = ref(null)

// 查詢條件
const filters = ref({
  permissionCode: '',
  resource: '',
  action: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...permissions.value]
  
  if (filters.value.permissionCode) {
    filtered = filtered.filter(perm => 
      perm.permissionCode?.toLowerCase().includes(filters.value.permissionCode.toLowerCase())
    )
  }
  
  if (filters.value.resource) {
    filtered = filtered.filter(perm => 
      (perm.resource || '').toLowerCase().includes(filters.value.resource.toLowerCase())
    )
  }
  
  if (filters.value.action) {
    filtered = filtered.filter(perm => 
      (perm.action || '').toLowerCase().includes(filters.value.action.toLowerCase())
    )
  }
  
  return filtered.sort((a, b) => {
    if (a.resource !== b.resource) {
      return (a.resource || '').localeCompare(b.resource || '')
    }
    return (a.action || '').localeCompare(b.action || '')
  })
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
    permissionCode: '',
    resource: '',
    action: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => [filters.value.permissionCode, filters.value.resource, filters.value.action], () => {
  currentPage.value = 1
  jumpPage.value = 1
})

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

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
      toast.error('載入權限資料失敗')
    }
  } catch (error) {
    console.error('載入權限資料失敗:', error)
    toast.error('載入權限資料失敗: ' + error.message)
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
      toast.error('刪除失敗')
    }
  } catch (error) {
    console.error('刪除權限失敗:', error)
    toast.error('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadPermissions()
})
</script>

<style scoped>
.admin-permissions{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-permissions .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-permissions .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-permissions .page-header p,
.admin-permissions .subtitle,
.admin-permissions .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-permissions .table-container,
.admin-permissions .list-container,
.admin-permissions .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-permissions .table-container{ padding:0; }

/* Inline helpers */
.admin-permissions .hint,
.admin-permissions .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-permissions .actions,
.admin-permissions .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
