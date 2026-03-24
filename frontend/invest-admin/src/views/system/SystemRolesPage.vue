<template>
  <AdminLayout>
    <div class="system-roles-page">
      <div class="page-header">
        <div>
          <h2>角色管理</h2>
          <p>維護 invest 後台角色與權限組合，對齊既有 admin baseline 管理節奏。</p>
        </div>
        <button
          class="btn btn-primary"
          :disabled="!canManageRoles"
          @click="openCreateModal"
        >
          + 新增角色
        </button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前角色</span>
          <strong>{{ pagination.totalElements }}</strong>
          <p>invest ACL 角色總筆數。</p>
        </article>
        <article class="overview-card">
          <span>當前頁面</span>
          <strong>{{ rows.length }}</strong>
          <p>這一頁符合查詢條件的角色筆數。</p>
        </article>
        <article class="overview-card">
          <span>篩選狀態</span>
          <strong>{{ filters.roleName ? '已套用' : '全部' }}</strong>
          <p>可透過角色名稱快速收斂清單。</p>
        </article>
      </section>

      <details class="filters filters--collapsible" open>
        <summary>
          <div class="filters__title">
            <h3>查詢條件</h3>
          </div>
        </summary>
        <div class="filters__content">
          <div class="filter-grid">
            <div class="filter-group">
              <label>角色名稱</label>
              <input v-model="filters.roleName" type="text" placeholder="輸入角色名稱" />
            </div>
            <div class="filter-group actions">
              <button class="btn btn-primary" @click="applyFilters">查詢</button>
              <button class="btn btn-secondary" @click="resetFilters">清除條件</button>
            </div>
          </div>
        </div>
      </details>

      <div class="card surface-card table-wrap">
        <div v-if="loading" class="empty-state">載入角色資料中...</div>
        <div v-else-if="rows.length === 0" class="empty-state">
          {{ pagination.totalElements === 0 ? '尚無角色資料' : '沒有符合條件的資料' }}
        </div>
        <div v-else>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>角色名稱</th>
                <th>描述</th>
                <th>權限數量</th>
                <th>更新時間</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td>{{ row.id }}</td>
                <td>{{ row.roleName }}</td>
                <td>{{ row.description || '-' }}</td>
                <td>{{ row.permissionCount ?? 0 }}</td>
                <td>{{ formatDateTime(row.updatedAt) }}</td>
                <td>
                  <div class="table-actions">
                    <button class="btn btn-secondary" :disabled="!canManageRoles" @click="openEditModal(row)">
                      編輯
                    </button>
                    <button class="btn btn-danger" :disabled="!canManageRoles" @click="removeRole(row)">
                      刪除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <PaginationControls
            v-model:pageSize="pagination.size"
            v-model:jumpPage="jumpPage"
            :total-records="pagination.totalElements"
            :current-page="pagination.page"
            :total-pages="pagination.totalPages"
            :page-size-options="[10, 20, 50]"
            page-size-id="system-roles-page-size"
            @first="goFirstPage"
            @previous="goPreviousPage"
            @next="goNextPage"
            @last="goLastPage"
            @jump="goJumpPage"
          />
        </div>
      </div>

      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-panel modal-panel--large" @click.stop>
          <div class="modal-header">
            <h3 class="modal-title">{{ editingRoleId ? '編輯角色' : '新增角色' }}</h3>
            <button class="btn-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="filter-group">
                <label>角色名稱 <span class="required">*</span></label>
                <input v-model="form.roleName" type="text" placeholder="例如：ROLE_INVEST_MANAGER" />
              </div>
              <div class="filter-group filter-group--full">
                <label>描述</label>
                <textarea v-model="form.description" rows="3" placeholder="請輸入角色描述"></textarea>
              </div>
              <div class="filter-group filter-group--full">
                <label>權限綁定</label>
                <div class="transfer">
                  <div class="transfer-col">
                    <div class="transfer-head">
                      <span>未加入</span>
                      <input v-model="permissionSearchLeft" class="transfer-search" placeholder="搜尋權限..." />
                    </div>
                    <div class="transfer-list">
                      <button
                        v-for="permission in filteredAvailablePermissions"
                        :key="`left-${permission.id}`"
                        type="button"
                        class="transfer-item"
                        @click="addPermission(permission.id)"
                      >
                        <div class="transfer-title">{{ permission.permissionName }}</div>
                        <div class="transfer-sub"><code>{{ permission.permissionCode }}</code></div>
                      </button>
                      <div v-if="filteredAvailablePermissions.length === 0" class="transfer-empty">沒有可加入的權限</div>
                    </div>
                    <div class="transfer-actions">
                      <button
                        type="button"
                        class="btn btn-secondary btn-sm"
                        :disabled="filteredAvailablePermissions.length === 0"
                        @click="addAllPermissions"
                      >
                        全部加入
                      </button>
                    </div>
                  </div>

                  <div class="transfer-col">
                    <div class="transfer-head">
                      <span>已加入</span>
                      <input v-model="permissionSearchRight" class="transfer-search" placeholder="搜尋已加入..." />
                    </div>
                    <div class="transfer-list">
                      <button
                        v-for="permission in filteredSelectedPermissions"
                        :key="`right-${permission.id}`"
                        type="button"
                        class="transfer-item transfer-item--selected"
                        @click="removePermission(permission.id)"
                      >
                        <div class="transfer-title">{{ permission.permissionName }}</div>
                        <div class="transfer-sub"><code>{{ permission.permissionCode }}</code></div>
                      </button>
                      <div v-if="filteredSelectedPermissions.length === 0" class="transfer-empty">尚未加入任何權限</div>
                    </div>
                    <div class="transfer-actions">
                      <button
                        type="button"
                        class="btn btn-secondary btn-sm"
                        :disabled="filteredSelectedPermissions.length === 0"
                        @click="removeAllPermissions"
                      >
                        全部移除
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="modal-actions">
              <button class="btn btn-primary" :disabled="savingRole" @click="submitRoleForm">
                {{ savingRole ? '儲存中...' : '儲存' }}
              </button>
              <button class="btn btn-secondary" :disabled="savingRole" @click="closeModal">取消</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { toast } from '@shared/composables/useToast'
import { hasPermission } from '@shared/utils/permission'
import PaginationControls from '@shared/components/PaginationControls.vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { useAuth } from '@/composables/useAuth'
import { investApiService } from '@/composables/useInvestApi'

const rows = ref([])
const loading = ref(false)
const jumpPage = ref(1)

const showModal = ref(false)
const editingRoleId = ref(null)
const savingRole = ref(false)

const permissionOptions = ref([])
const permissionSearchLeft = ref('')
const permissionSearchRight = ref('')

const filters = reactive({
  roleName: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const form = reactive({
  roleName: '',
  description: '',
  permissionIds: []
})

const { currentUser } = useAuth()

const canManageRoles = computed(() => hasPermission(currentUser.value, 'INVEST_SYS_ROLES_EDIT'))

const selectedPermissionIdSet = computed(() => new Set(form.permissionIds || []))

const availablePermissions = computed(() => {
  return (permissionOptions.value || []).filter(item => !selectedPermissionIdSet.value.has(item.id))
})

const selectedPermissions = computed(() => {
  return (permissionOptions.value || []).filter(item => selectedPermissionIdSet.value.has(item.id))
})

const filteredAvailablePermissions = computed(() => {
  const keyword = permissionSearchLeft.value.trim().toLowerCase()
  if (!keyword) {
    return availablePermissions.value
  }
  return availablePermissions.value.filter(item => {
    const source = `${item.permissionName || ''} ${item.permissionCode || ''}`.toLowerCase()
    return source.includes(keyword)
  })
})

const filteredSelectedPermissions = computed(() => {
  const keyword = permissionSearchRight.value.trim().toLowerCase()
  if (!keyword) {
    return selectedPermissions.value
  }
  return selectedPermissions.value.filter(item => {
    const source = `${item.permissionName || ''} ${item.permissionCode || ''}`.toLowerCase()
    return source.includes(keyword)
  })
})

const buildParams = () => {
  const params = {
    page: pagination.page - 1,
    size: pagination.size
  }
  if (filters.roleName) {
    params.roleName = filters.roleName
  }
  return params
}

const loadRows = async () => {
  loading.value = true
  try {
    const data = await investApiService.getSystemRolesPaged(buildParams())
    rows.value = Array.isArray(data?.content) ? data.content : []
    pagination.totalElements = Number(data?.totalElements || 0)
    pagination.totalPages = Number(data?.totalPages || 1)
    if (pagination.page > pagination.totalPages) {
      pagination.page = pagination.totalPages || 1
    }
    jumpPage.value = pagination.page
  } catch (error) {
    toast.error(`載入角色資料失敗：${error.message || '未知錯誤'}`)
  } finally {
    loading.value = false
  }
}

const loadPermissionOptions = async () => {
  try {
    const data = await investApiService.getSystemRolePermissionOptions()
    permissionOptions.value = Array.isArray(data) ? data : []
  } catch (error) {
    toast.error(`載入權限清單失敗：${error.message || '未知錯誤'}`)
  }
}

const applyFilters = () => {
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
}

const resetFilters = () => {
  filters.roleName = ''
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
}

const goFirstPage = () => {
  if (pagination.page === 1) return
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
}

const goPreviousPage = () => {
  if (pagination.page <= 1) return
  pagination.page -= 1
  jumpPage.value = pagination.page
  loadRows()
}

const goNextPage = () => {
  if (pagination.page >= pagination.totalPages) return
  pagination.page += 1
  jumpPage.value = pagination.page
  loadRows()
}

const goLastPage = () => {
  if (pagination.page === pagination.totalPages) return
  pagination.page = pagination.totalPages
  jumpPage.value = pagination.page
  loadRows()
}

const goJumpPage = () => {
  const target = Number(jumpPage.value)
  if (!Number.isInteger(target) || target < 1 || target > pagination.totalPages) {
    jumpPage.value = pagination.page
    return
  }
  pagination.page = target
  loadRows()
}

const resetForm = () => {
  form.roleName = ''
  form.description = ''
  form.permissionIds = []
  permissionSearchLeft.value = ''
  permissionSearchRight.value = ''
}

const openCreateModal = () => {
  if (!canManageRoles.value) {
    toast.error('你沒有新增角色權限')
    return
  }
  editingRoleId.value = null
  resetForm()
  showModal.value = true
}

const openEditModal = async (row) => {
  if (!canManageRoles.value) {
    toast.error('你沒有編輯角色權限')
    return
  }

  try {
    const detail = await investApiService.getSystemRole(row.id)
    editingRoleId.value = detail.id
    form.roleName = detail.roleName || ''
    form.description = detail.description || ''
    form.permissionIds = Array.isArray(detail.permissions)
      ? detail.permissions.map(item => item.id).filter(id => Number.isFinite(Number(id)))
      : []
    permissionSearchLeft.value = ''
    permissionSearchRight.value = ''
    showModal.value = true
  } catch (error) {
    toast.error(`載入角色明細失敗：${error.message || '未知錯誤'}`)
  }
}

const closeModal = () => {
  showModal.value = false
  editingRoleId.value = null
  resetForm()
}

const addPermission = (permissionId) => {
  const id = Number(permissionId)
  if (!Number.isFinite(id)) return
  if (!selectedPermissionIdSet.value.has(id)) {
    form.permissionIds = [...form.permissionIds, id]
  }
}

const removePermission = (permissionId) => {
  const id = Number(permissionId)
  if (!Number.isFinite(id)) return
  form.permissionIds = form.permissionIds.filter(item => Number(item) !== id)
}

const addAllPermissions = () => {
  const ids = filteredAvailablePermissions.value.map(item => Number(item.id)).filter(Number.isFinite)
  form.permissionIds = Array.from(new Set([...form.permissionIds, ...ids]))
}

const removeAllPermissions = () => {
  const removeIds = new Set(
    filteredSelectedPermissions.value.map(item => Number(item.id)).filter(Number.isFinite)
  )
  form.permissionIds = form.permissionIds.filter(item => !removeIds.has(Number(item)))
}

const submitRoleForm = async () => {
  if (!canManageRoles.value) {
    toast.error('你沒有管理角色權限')
    return
  }

  const roleName = (form.roleName || '').trim()
  if (!roleName) {
    toast.error('角色名稱為必填')
    return
  }

  const payload = {
    roleName,
    description: (form.description || '').trim() || null,
    permissionIds: Array.from(new Set(form.permissionIds.map(item => Number(item)).filter(Number.isFinite)))
  }

  savingRole.value = true
  try {
    if (editingRoleId.value) {
      await investApiService.updateSystemRole(editingRoleId.value, payload)
      toast.success('角色更新成功')
    } else {
      await investApiService.createSystemRole(payload)
      toast.success('角色新增成功')
    }
    closeModal()
    await loadRows()
  } catch (error) {
    toast.error(`儲存角色失敗：${error.message || '未知錯誤'}`)
  } finally {
    savingRole.value = false
  }
}

const removeRole = async (row) => {
  if (!canManageRoles.value) {
    toast.error('你沒有刪除角色權限')
    return
  }

  if (!window.confirm(`確定要刪除角色「${row.roleName}」嗎？`)) {
    return
  }

  try {
    await investApiService.deleteSystemRole(row.id)
    toast.success('角色刪除成功')
    await loadRows()
  } catch (error) {
    toast.error(`刪除角色失敗：${error.message || '未知錯誤'}`)
  }
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return value
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

watch(() => pagination.size, () => {
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
})

onMounted(async () => {
  await Promise.all([loadRows(), loadPermissionOptions()])
})
</script>

<style scoped>
.system-roles-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.overview-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.overview-card {
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(2, 6, 23, 0.08);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-sm);
}

.overview-card span {
  display: block;
  color: rgba(2, 6, 23, 0.56);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.overview-card strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
  letter-spacing: -0.04em;
}

.overview-card p {
  margin: 8px 0 0;
  color: rgba(2, 6, 23, 0.62);
  font-size: 13px;
  line-height: 1.6;
  font-weight: 700;
}

.overview-card--accent {
  background: linear-gradient(140deg, rgba(15, 23, 42, 0.96), rgba(29, 78, 216, 0.92));
}

.overview-card--accent span,
.overview-card--accent strong,
.overview-card--accent p {
  color: white;
}

.overview-card--accent p {
  color: rgba(255, 255, 255, 0.76);
}

.table-wrap {
  padding: 16px;
}

.col-actions {
  width: 190px;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.modal-panel--large {
  width: min(960px, 92vw);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.filter-group--full {
  grid-column: 1 / -1;
}

textarea {
  width: 100%;
  min-height: 88px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: #fff;
  resize: vertical;
}

.required {
  color: #dc2626;
}

.transfer {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.transfer-col {
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 14px;
  background: #fff;
  display: flex;
  flex-direction: column;
  min-height: 260px;
}

.transfer-head {
  padding: 10px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.transfer-head span {
  font-size: 13px;
  font-weight: 800;
}

.transfer-search {
  width: 100%;
  padding: 7px 10px;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.15);
}

.transfer-list {
  flex: 1;
  overflow: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.transfer-item {
  text-align: left;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 10px;
  background: #fff;
  padding: 8px 10px;
  cursor: pointer;
}

.transfer-item:hover {
  border-color: rgba(37, 99, 235, 0.5);
  background: rgba(37, 99, 235, 0.04);
}

.transfer-item--selected {
  border-color: rgba(29, 78, 216, 0.4);
  background: rgba(29, 78, 216, 0.08);
}

.transfer-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.transfer-sub {
  margin-top: 4px;
  color: #475569;
  font-size: 12px;
}

.transfer-empty {
  text-align: center;
  color: #64748b;
  padding: 20px 8px;
  font-size: 13px;
}

.transfer-actions {
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  padding: 8px 10px;
  display: flex;
  justify-content: flex-end;
}

.btn-sm {
  padding: 6px 10px;
  font-size: 12px;
}

@media (max-width: 960px) {
  .overview-strip {
    grid-template-columns: 1fr;
  }

  .transfer {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
