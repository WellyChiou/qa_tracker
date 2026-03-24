<template>
  <AdminLayout>
    <div class="system-permissions-page">
      <div class="page-header">
        <div>
          <h2>權限管理</h2>
          <p>維護 invest ACL 權限代碼與資源動作，作為角色授權基礎資料。</p>
        </div>
        <button
          class="btn btn-primary"
          :disabled="!canManagePermissions"
          @click="openCreateModal"
        >
          + 新增權限
        </button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前權限</span>
          <strong>{{ pagination.totalElements }}</strong>
          <p>invest ACL 權限總筆數。</p>
        </article>
        <article class="overview-card">
          <span>當前頁面</span>
          <strong>{{ rows.length }}</strong>
          <p>這一頁符合條件的權限筆數。</p>
        </article>
        <article class="overview-card">
          <span>篩選狀態</span>
          <strong>{{ hasFilterApplied ? '已套用' : '全部' }}</strong>
          <p>可透過代碼、資源與動作快速定位。</p>
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
              <label>權限代碼</label>
              <input v-model="filters.permissionCode" type="text" placeholder="輸入權限代碼" />
            </div>
            <div class="filter-group">
              <label>資源</label>
              <input v-model="filters.resource" type="text" placeholder="輸入資源" />
            </div>
            <div class="filter-group">
              <label>動作</label>
              <input v-model="filters.action" type="text" placeholder="輸入動作" />
            </div>
            <div class="filter-group actions">
              <button class="btn btn-primary" @click="applyFilters">查詢</button>
              <button class="btn btn-secondary" @click="resetFilters">清除條件</button>
            </div>
          </div>
        </div>
      </details>

      <div class="card surface-card table-wrap">
        <div v-if="loading" class="empty-state">載入權限資料中...</div>
        <div v-else-if="rows.length === 0" class="empty-state">
          {{ pagination.totalElements === 0 ? '尚無權限資料' : '沒有符合條件的資料' }}
        </div>
        <div v-else>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>權限代碼</th>
                <th>權限名稱</th>
                <th>資源</th>
                <th>動作</th>
                <th>角色綁定數</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td>{{ row.id }}</td>
                <td><code>{{ row.permissionCode }}</code></td>
                <td>{{ row.permissionName }}</td>
                <td>{{ row.resource || '-' }}</td>
                <td>{{ row.action || '-' }}</td>
                <td>{{ row.roleCount ?? 0 }}</td>
                <td>
                  <div class="table-actions">
                    <button class="btn btn-secondary" :disabled="!canManagePermissions" @click="openEditModal(row)">
                      編輯
                    </button>
                    <button class="btn btn-danger" :disabled="!canManagePermissions" @click="removePermission(row)">
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
            page-size-id="system-permissions-page-size"
            @first="goFirstPage"
            @previous="goPreviousPage"
            @next="goNextPage"
            @last="goLastPage"
            @jump="goJumpPage"
          />
        </div>
      </div>

      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-panel" @click.stop>
          <div class="modal-header">
            <h3 class="modal-title">{{ editingPermissionId ? '編輯權限' : '新增權限' }}</h3>
            <button class="btn-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="filter-group">
                <label>權限代碼 <span class="required">*</span></label>
                <input v-model="form.permissionCode" type="text" placeholder="例如：INVEST_PORTFOLIO_VIEW" />
              </div>
              <div class="filter-group">
                <label>權限名稱 <span class="required">*</span></label>
                <input v-model="form.permissionName" type="text" placeholder="例如：查看持股中心" />
              </div>
              <div class="filter-group">
                <label>資源</label>
                <input v-model="form.resource" type="text" placeholder="例如：portfolio" />
              </div>
              <div class="filter-group">
                <label>動作</label>
                <input v-model="form.action" type="text" placeholder="例如：view / edit" />
              </div>
              <div class="filter-group filter-group--full">
                <label>描述</label>
                <textarea v-model="form.description" rows="3" placeholder="請輸入權限描述"></textarea>
              </div>

              <div v-if="editingPermissionId" class="filter-group filter-group--full">
                <label>角色綁定資訊（唯讀）</label>
                <div class="binding-list">
                  <span v-if="formRoleBindings.length === 0" class="binding-empty">目前無角色綁定</span>
                  <span v-for="binding in formRoleBindings" :key="binding.roleId" class="binding-chip">
                    {{ binding.roleName }}
                  </span>
                </div>
              </div>
            </div>

            <div class="modal-actions">
              <button class="btn btn-primary" :disabled="savingPermission" @click="submitPermissionForm">
                {{ savingPermission ? '儲存中...' : '儲存' }}
              </button>
              <button class="btn btn-secondary" :disabled="savingPermission" @click="closeModal">取消</button>
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
const editingPermissionId = ref(null)
const savingPermission = ref(false)
const formRoleBindings = ref([])

const filters = reactive({
  permissionCode: '',
  resource: '',
  action: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const form = reactive({
  permissionCode: '',
  permissionName: '',
  resource: '',
  action: '',
  description: ''
})

const { currentUser } = useAuth()

const canManagePermissions = computed(() => hasPermission(currentUser.value, 'INVEST_SYS_PERMISSIONS_EDIT'))

const hasFilterApplied = computed(() => (
  !!filters.permissionCode || !!filters.resource || !!filters.action
))

const buildParams = () => {
  const params = {
    page: pagination.page - 1,
    size: pagination.size
  }
  if (filters.permissionCode) params.permissionCode = filters.permissionCode
  if (filters.resource) params.resource = filters.resource
  if (filters.action) params.action = filters.action
  return params
}

const loadRows = async () => {
  loading.value = true
  try {
    const data = await investApiService.getSystemPermissionsPaged(buildParams())
    rows.value = Array.isArray(data?.content) ? data.content : []
    pagination.totalElements = Number(data?.totalElements || 0)
    pagination.totalPages = Number(data?.totalPages || 1)
    if (pagination.page > pagination.totalPages) {
      pagination.page = pagination.totalPages || 1
    }
    jumpPage.value = pagination.page
  } catch (error) {
    toast.error(`載入權限資料失敗：${error.message || '未知錯誤'}`)
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
}

const resetFilters = () => {
  filters.permissionCode = ''
  filters.resource = ''
  filters.action = ''
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
  form.permissionCode = ''
  form.permissionName = ''
  form.resource = ''
  form.action = ''
  form.description = ''
  formRoleBindings.value = []
}

const openCreateModal = () => {
  if (!canManagePermissions.value) {
    toast.error('你沒有新增權限權限')
    return
  }
  editingPermissionId.value = null
  resetForm()
  showModal.value = true
}

const openEditModal = async (row) => {
  if (!canManagePermissions.value) {
    toast.error('你沒有編輯權限權限')
    return
  }

  try {
    const detail = await investApiService.getSystemPermission(row.id)
    editingPermissionId.value = detail.id
    form.permissionCode = detail.permissionCode || ''
    form.permissionName = detail.permissionName || ''
    form.resource = detail.resource || ''
    form.action = detail.action || ''
    form.description = detail.description || ''
    formRoleBindings.value = Array.isArray(detail.roleBindings) ? detail.roleBindings : []
    showModal.value = true
  } catch (error) {
    toast.error(`載入權限明細失敗：${error.message || '未知錯誤'}`)
  }
}

const closeModal = () => {
  showModal.value = false
  editingPermissionId.value = null
  resetForm()
}

const submitPermissionForm = async () => {
  if (!canManagePermissions.value) {
    toast.error('你沒有管理權限權限')
    return
  }

  const permissionCode = (form.permissionCode || '').trim()
  const permissionName = (form.permissionName || '').trim()

  if (!permissionCode || !permissionName) {
    toast.error('權限代碼與權限名稱為必填')
    return
  }

  const payload = {
    permissionCode,
    permissionName,
    resource: (form.resource || '').trim() || null,
    action: (form.action || '').trim() || null,
    description: (form.description || '').trim() || null
  }

  savingPermission.value = true
  try {
    if (editingPermissionId.value) {
      await investApiService.updateSystemPermission(editingPermissionId.value, payload)
      toast.success('權限更新成功')
    } else {
      await investApiService.createSystemPermission(payload)
      toast.success('權限新增成功')
    }
    closeModal()
    await loadRows()
  } catch (error) {
    toast.error(`儲存權限失敗：${error.message || '未知錯誤'}`)
  } finally {
    savingPermission.value = false
  }
}

const removePermission = async (row) => {
  if (!canManagePermissions.value) {
    toast.error('你沒有刪除權限權限')
    return
  }

  if (!window.confirm(`確定要刪除權限「${row.permissionCode}」嗎？`)) {
    return
  }

  try {
    await investApiService.deleteSystemPermission(row.id)
    toast.success('權限刪除成功')
    await loadRows()
  } catch (error) {
    toast.error(`刪除權限失敗：${error.message || '未知錯誤'}`)
  }
}

watch(() => pagination.size, () => {
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
})

onMounted(() => {
  loadRows()
})
</script>

<style scoped>
.system-permissions-page {
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
  width: 180px;
}

.table-actions {
  display: flex;
  gap: 8px;
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

.binding-list {
  min-height: 44px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 12px;
  padding: 10px;
  background: #fff;
}

.binding-chip {
  background: rgba(37, 99, 235, 0.1);
  border: 1px solid rgba(37, 99, 235, 0.24);
  color: #1d4ed8;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
}

.binding-empty {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 960px) {
  .overview-strip {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
