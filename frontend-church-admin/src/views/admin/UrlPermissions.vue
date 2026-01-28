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
        <div v-if="permissions.length === 0" class="empty-state">
          <p>尚無 URL 權限資料</p>
        </div>
        <div v-else class="url-permissions-table">
          <div class="table-header">
            <h3>URL 權限列表 (共 {{ totalRecords }} 筆)</h3>
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
              <tr v-for="permission in permissions" :key="permission.id">
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
    
    <UrlPermissionModal
      :show="showModal"
      :permission="selectedPermission"
      @close="closeModal"
      @saved="handleSaved"
    />
  </AdminLayout>
</template>

<script setup>
import { toast } from '@shared/composables/useToast'
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
const totalRecords = ref(0)
const totalPages = ref(1)

// 第一頁
const firstPage = () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadPermissions()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadPermissions()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadPermissions()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadPermissions()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadPermissions()
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
  loadPermissions()
}

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => [
  filters.value.urlPattern, 
  filters.value.httpMethod, 
  filters.value.isPublic, 
  filters.value.requiredPermission, 
  filters.value.isActive
], () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadPermissions()
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadPermissions()
})

const loadPermissions = async () => {
  try {
    const params = new URLSearchParams()
    if (filters.value.urlPattern) {
      params.append('urlPattern', filters.value.urlPattern)
    }
    if (filters.value.httpMethod) {
      params.append('httpMethod', filters.value.httpMethod)
    }
    if (filters.value.isPublic !== '') {
      params.append('isPublic', filters.value.isPublic === true || filters.value.isPublic === 'true')
    }
    if (filters.value.requiredPermission) {
      params.append('requiredPermission', filters.value.requiredPermission)
    }
    if (filters.value.isActive !== '') {
      params.append('isActive', filters.value.isActive === true || filters.value.isActive === 'true')
    }
    params.append('page', (currentPage.value - 1).toString())
    params.append('size', recordsPerPage.value.toString())
    
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/url-permissions?${params.toString()}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 處理 PageResponse 格式
      if (data.content !== undefined && data.totalElements !== undefined) {
        // 這是 PageResponse 格式
        permissions.value = Array.isArray(data.content) ? data.content : []
        totalRecords.value = data.totalElements
        totalPages.value = data.totalPages || 1
        // 確保 currentPage 不超過 totalPages
        if (currentPage.value > totalPages.value) {
          currentPage.value = totalPages.value
          jumpPage.value = totalPages.value
        }
        jumpPage.value = currentPage.value
      } else {
        // 向後兼容：處理舊格式
        permissions.value = data.permissions || data.content || data || []
        if (data.totalElements !== undefined) {
          totalRecords.value = data.totalElements
          totalPages.value = data.totalPages || 1
          if (currentPage.value > totalPages.value) {
            currentPage.value = totalPages.value
            jumpPage.value = totalPages.value
          }
          jumpPage.value = currentPage.value
        } else {
          totalRecords.value = permissions.value.length
          totalPages.value = 1
          currentPage.value = 1
          jumpPage.value = 1
        }
      }
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
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/url-permissions/${id}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
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
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/url-permissions/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (data !== null) {
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
