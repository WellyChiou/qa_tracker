<template>
  <AdminLayout>
    <div class="admin-permissions">
      <div class="page-header">
        <div>
          <h2>權限管理</h2>
          <p>整理資源與操作權限，作為角色與使用者授權的基礎資料。</p>
        </div>
        <button
          v-if="canManageAdmin"
          @click="openAddModal"
          class="btn btn-primary"
        >
          + 新增權限
        </button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前權限</span>
          <strong>{{ totalRecords }}</strong>
          <p>後台目前可管理的權限總筆數。</p>
        </article>
        <article class="overview-card">
          <span>當前頁面</span>
          <strong>{{ permissions.length }}</strong>
          <p>目前這一頁實際載入的權限資料。</p>
        </article>
        <article class="overview-card">
          <span>查詢狀態</span>
          <strong>{{ filters.permissionCode || filters.resource || filters.action ? '已套用' : '全部' }}</strong>
          <p>可透過代碼、資源與操作快速定位權限。</p>
        </article>
      </section>

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
        </div>
      </details>

      <div class="permissions-list card surface-card">
        <div v-if="permissions.length === 0" class="empty-state">
          <p>尚無權限資料</p>
        </div>
        <div v-else class="permissions-table">
          <div class="table-header">
            <h3>權限列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>權限代碼</th>
                <th>權限名稱</th>
                <th>資源</th>
                <th>動作</th>
                <th>描述</th>
                <th v-if="canManageAdmin">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="permission in permissions" :key="permission.id">
                <td>{{ permission.permissionCode }}</td>
                <td>{{ permission.permissionName }}</td>
                <td>{{ permission.resource || '-' }}</td>
                <td>{{ permission.action || '-' }}</td>
                <td>{{ permission.description || '-' }}</td>
                <td v-if="canManageAdmin"><div class="table-actions"><button @click="editPermission(permission.id)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                  <button @click="deletePermission(permission.id)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
              </tr>
            </tbody>
          </table>
          
          <!-- 分頁 -->
          <PaginationControls
            v-model:pageSize="recordsPerPage"
            v-model:jumpPage="jumpPage"
            :total-records="totalRecords"
            :current-page="currentPage"
            :total-pages="totalPages"
            @first="firstPage"
            @previous="previousPage"
            @next="nextPage"
            @last="lastPage"
            @jump="jumpToPage"
          />
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
import { toast } from '@shared/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import AdminLayout from '@/components/AdminLayout.vue'
import PermissionModal from '@/components/PermissionModal.vue'
import { apiService } from '@/composables/useApi'
import { hasPermission } from '@shared/utils/permission'

const permissions = ref([])
const showModal = ref(false)
const selectedPermission = ref(null)
const { currentUser } = useAuth()
const canManageAdmin = computed(() => hasPermission(currentUser.value, 'ADMIN_ACCESS'))

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
    permissionCode: '',
    resource: '',
    action: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
  loadPermissions()
}

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => [filters.value.permissionCode, filters.value.resource, filters.value.action], () => {
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
    if (filters.value.permissionCode) {
      params.append('permissionCode', filters.value.permissionCode)
    }
    if (filters.value.resource) {
      params.append('resource', filters.value.resource)
    }
    if (filters.value.action) {
      params.append('action', filters.value.action)
    }
    params.append('page', (currentPage.value - 1).toString())
    params.append('size', recordsPerPage.value.toString())
    
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiService.request(`/permissions/paged?${params.toString()}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 處理 ApiResponse 格式或直接返回的資料
      let responseData = data
      if (data.success && data.data) {
        responseData = data.data
      } else if (data.data) {
        responseData = data.data
      }
      
      permissions.value = responseData.permissions || responseData.content || responseData || []
      
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
        totalRecords.value = permissions.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
      }
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
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiService.request(`/permissions/${id}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
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
  toast.success(selectedPermission.value ? '更新成功' : '新增成功')
  loadPermissions()
}

const deletePermission = async (id) => {
  if (!confirm('確定要刪除此權限嗎？')) {
    return
  }
  
  try {
    await apiService.request(`/permissions/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })

    toast.success('刪除成功')
    loadPermissions()
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

.overview-strip{
  display:grid;
  grid-template-columns:repeat(3, minmax(0, 1fr));
  gap:12px;
}

.overview-card{
  padding:16px;
  border-radius:20px;
  border:1px solid rgba(2,6,23,.08);
  background:rgba(255,255,255,.88);
  box-shadow:var(--shadow-sm);
}

.overview-card span{
  display:block;
  color:rgba(2,6,23,.56);
  font-size:12px;
  font-weight:900;
  letter-spacing:.12em;
  text-transform:uppercase;
}

.overview-card strong{
  display:block;
  margin-top:8px;
  font-size:28px;
  line-height:1;
  letter-spacing:-0.04em;
}

.overview-card p{
  margin:8px 0 0;
  color:rgba(2,6,23,.62);
  font-size:13px;
  line-height:1.6;
  font-weight:700;
}

.overview-card--accent{
  background:linear-gradient(140deg, rgba(15,23,42,.96), rgba(29,78,216,.92));
}

.overview-card--accent span,
.overview-card--accent strong,
.overview-card--accent p{
  color:white;
}

.overview-card--accent p{
  color:rgba(255,255,255,.76);
}

.surface-card{
  padding:16px;
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
  .overview-strip{
    grid-template-columns:1fr;
  }
}
</style>
