<template>
  <AdminLayout>
    <div class="admin-url-permissions">
      <div class="page-header">
        <h2>URL 權限管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增 URL 權限</button>
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
            <label>URL 模式</label>
            <input
              type="text"
              v-model="filters.urlPattern"
              placeholder="輸入 URL 模式"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>HTTP 方法</label>
            <select v-model="filters.httpMethod">
              <option value="">全部</option>
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
            </select>
          </div>
          <div class="filter-group">
            <label>是否公開</label>
            <select v-model="filters.isPublic">
              <option value="">全部</option>
              <option :value="true">公開</option>
              <option :value="false">需認證</option>
            </select>
          </div>
          <div class="filter-group">
            <label>需要權限</label>
            <input
              type="text"
              v-model="filters.requiredPermission"
              placeholder="輸入權限代碼"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>是否啟用</label>
            <select v-model="filters.isActive">
              <option value="">全部</option>
              <option :value="true">啟用</option>
              <option :value="false">停用</option>
            </select>
          </div>
          <div class="filter-group">
            <button @click="resetFilters" class="btn btn-secondary">清除條件</button>
          </div>
        </div>
        </div>
      </details>

      <div class="url-permissions-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ permissions.length === 0 ? '尚無 URL 權限資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="url-permissions-table">
          <div class="table-header">
            <h3>URL 權限列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
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
              <tr v-for="permission in paginatedList" :key="permission.id">
                <td>{{ permission.urlPattern }}</td>
                <td>{{ permission.httpMethod || '全部' }}</td>
                <td>{{ permission.isPublic ? '是' : '否' }}</td>
                <td>{{ permission.requiredRole || '-' }}</td>
                <td>{{ permission.requiredPermission || '-' }}</td>
                <td>{{ permission.isActive ? '是' : '否' }}</td>
                <td>{{ permission.orderIndex }}</td>
                <td>{{ permission.description || '-' }}</td>
                <td><div class="table-actions"><button @click="editPermission(permission.id)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                  <button @click="deletePermission(permission.id)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
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
    
    <UrlPermissionModal
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
import UrlPermissionModal from '@/components/UrlPermissionModal.vue'
import { apiRequest } from '@/utils/api'

const permissions = ref([])
const showModal = ref(false)
const selectedPermission = ref(null)

// 查詢條件
const filters = ref({
  urlPattern: '',
  httpMethod: '',
  isPublic: '',
  requiredPermission: '',
  isActive: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...permissions.value]
  
  if (filters.value.urlPattern) {
    filtered = filtered.filter(perm => 
      perm.urlPattern?.toLowerCase().includes(filters.value.urlPattern.toLowerCase())
    )
  }
  
  if (filters.value.httpMethod) {
    filtered = filtered.filter(perm => 
      (perm.httpMethod || '').toLowerCase() === filters.value.httpMethod.toLowerCase()
    )
  }
  
  if (filters.value.isPublic !== '') {
    filtered = filtered.filter(perm => perm.isPublic === filters.value.isPublic)
  }
  
  if (filters.value.requiredPermission) {
    filtered = filtered.filter(perm => 
      (perm.requiredPermission || '').toLowerCase().includes(filters.value.requiredPermission.toLowerCase())
    )
  }
  
  if (filters.value.isActive !== '') {
    filtered = filtered.filter(perm => perm.isActive === filters.value.isActive)
  }
  
  return filtered.sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0))
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
    urlPattern: '',
    httpMethod: '',
    isPublic: '',
    requiredPermission: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => [
  filters.value.urlPattern, 
  filters.value.httpMethod, 
  filters.value.isPublic, 
  filters.value.requiredPermission, 
  filters.value.isActive
], () => {
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
      toast.error('載入 URL 權限資料失敗')
    }
  } catch (error) {
    console.error('載入 URL 權限資料失敗:', error)
    toast.error('載入 URL 權限資料失敗: ' + error.message)
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
      toast.error('刪除失敗')
    }
  } catch (error) {
    console.error('刪除 URL 權限失敗:', error)
    toast.error('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadPermissions()
})
</script>

<style scoped>
.admin-url-permissions{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-url-permissions .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-url-permissions .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-url-permissions .page-header p,
.admin-url-permissions .subtitle,
.admin-url-permissions .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-url-permissions .table-container,
.admin-url-permissions .list-container,
.admin-url-permissions .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-url-permissions .table-container{ padding:0; }

/* Inline helpers */
.admin-url-permissions .hint,
.admin-url-permissions .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-url-permissions .actions,
.admin-url-permissions .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
