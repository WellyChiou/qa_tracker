<template>
  <AdminLayout>
    <div class="system-url-permissions-page">
      <div class="page-header">
        <div>
          <h2>URL 權限管理</h2>
          <p>維護 API 路徑與授權規則對應，對齊 church baseline 管理節奏。</p>
        </div>
        <button
          class="btn btn-primary"
          :disabled="!canManageUrlPermissions"
          @click="openCreateModal"
        >
          + 新增 URL 權限
        </button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前規則</span>
          <strong>{{ pagination.totalElements }}</strong>
          <p>invest ACL URL 權限規則總筆數。</p>
        </article>
        <article class="overview-card">
          <span>當前頁面</span>
          <strong>{{ rows.length }}</strong>
          <p>這一頁符合條件的規則筆數。</p>
        </article>
        <article class="overview-card">
          <span>查詢狀態</span>
          <strong>{{ hasFilterApplied ? '已套用' : '全部' }}</strong>
          <p>可依 URL、方法、公開性與權限快速定位。</p>
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
              <label>URL Pattern</label>
              <input v-model="filters.urlPattern" type="text" placeholder="輸入 URL pattern" />
            </div>
            <div class="filter-group">
              <label>HTTP Method</label>
              <select v-model="filters.httpMethod">
                <option value="">全部</option>
                <option v-for="option in methodOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
            <div class="filter-group">
              <label>公開性</label>
              <select v-model="filters.isPublic">
                <option value="">全部</option>
                <option :value="true">公開</option>
                <option :value="false">需認證</option>
              </select>
            </div>
            <div class="filter-group">
              <label>required permission</label>
              <input v-model="filters.requiredPermission" type="text" placeholder="輸入 permission code" />
            </div>
            <div class="filter-group">
              <label>狀態</label>
              <select v-model="filters.isActive">
                <option value="">全部</option>
                <option :value="true">啟用</option>
                <option :value="false">停用</option>
              </select>
            </div>
            <div class="filter-group actions">
              <button class="btn btn-primary" @click="applyFilters">查詢</button>
              <button class="btn btn-secondary" @click="resetFilters">清除條件</button>
            </div>
          </div>
        </div>
      </details>

      <div class="card surface-card table-wrap">
        <div v-if="loading" class="empty-state">載入 URL 權限資料中...</div>
        <div v-else-if="rows.length === 0" class="empty-state">
          {{ pagination.totalElements === 0 ? '尚無 URL 權限資料' : '沒有符合條件的資料' }}
        </div>
        <div v-else>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>URL Pattern</th>
                <th>Method</th>
                <th>公開</th>
                <th>required role</th>
                <th>required permission</th>
                <th>排序</th>
                <th>狀態</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td>{{ row.id }}</td>
                <td class="url-cell" :title="row.urlPattern">{{ row.urlPattern }}</td>
                <td><code>{{ row.httpMethod || 'ALL' }}</code></td>
                <td>{{ row.isPublic ? '是' : '否' }}</td>
                <td><code>{{ row.requiredRole || '-' }}</code></td>
                <td><code>{{ row.requiredPermission || '-' }}</code></td>
                <td>{{ row.orderIndex ?? 0 }}</td>
                <td>
                  <span :class="row.isActive ? 'status-active' : 'status-inactive'">
                    {{ row.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>
                  <div class="table-actions">
                    <button class="btn btn-secondary" :disabled="!canManageUrlPermissions" @click="openEditModal(row)">
                      編輯
                    </button>
                    <button class="btn btn-danger" :disabled="!canManageUrlPermissions" @click="removeRule(row)">
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
            page-size-id="system-url-permissions-page-size"
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
            <h3 class="modal-title">{{ editingId ? '編輯 URL 權限' : '新增 URL 權限' }}</h3>
            <button class="btn-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="filter-group filter-group--full">
                <label>URL Pattern <span class="required">*</span></label>
                <input v-model="form.urlPattern" type="text" placeholder="例如：/api/invest/system/menus/*" />
                <small class="hint">支援 `*`（單層）與 `**`（多層）路徑。</small>
              </div>

              <div class="filter-group">
                <label>HTTP Method</label>
                <select v-model="form.httpMethod">
                  <option value="">ALL</option>
                  <option v-for="option in methodOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </div>

              <div class="filter-group">
                <label>排序（order_index）</label>
                <input v-model.number="form.orderIndex" type="number" min="0" placeholder="0" />
              </div>

              <div class="filter-group">
                <label>required role</label>
                <input v-model="form.requiredRole" type="text" placeholder="例如：ROLE_INVEST_ADMIN，可留空" />
              </div>

              <div class="filter-group">
                <label>required permission</label>
                <select v-model="form.requiredPermission">
                  <option value="">無（不限制）</option>
                  <option
                    v-for="permission in permissionOptionsForForm"
                    :key="permission.permissionCode"
                    :value="permission.permissionCode"
                  >
                    {{ permission.permissionCode }} - {{ permission.permissionName }}
                  </option>
                </select>
              </div>

              <div class="filter-group">
                <label>公開性</label>
                <select v-model="form.isPublic">
                  <option :value="false">需認證</option>
                  <option :value="true">公開</option>
                </select>
              </div>

              <div class="filter-group">
                <label>啟用狀態</label>
                <select v-model="form.isActive">
                  <option :value="true">啟用</option>
                  <option :value="false">停用</option>
                </select>
              </div>

              <div class="filter-group filter-group--full">
                <label>描述</label>
                <textarea v-model="form.description" rows="3" placeholder="請輸入說明"></textarea>
              </div>
            </div>

            <div class="modal-actions">
              <button class="btn btn-primary" :disabled="saving" @click="submitForm">
                {{ saving ? '儲存中...' : '儲存' }}
              </button>
              <button class="btn btn-secondary" :disabled="saving" @click="closeModal">取消</button>
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
const editingId = ref(null)
const saving = ref(false)

const permissionOptions = ref([])
const methodOptions = ref([])

const filters = reactive({
  urlPattern: '',
  httpMethod: '',
  isPublic: '',
  requiredPermission: '',
  isActive: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const form = reactive({
  urlPattern: '',
  httpMethod: '',
  isPublic: false,
  requiredRole: '',
  requiredPermission: '',
  description: '',
  isActive: true,
  orderIndex: 0
})

const { currentUser } = useAuth()

const canManageUrlPermissions = computed(() => hasPermission(currentUser.value, 'INVEST_SYS_URL_PERMISSIONS_EDIT'))

const hasFilterApplied = computed(() => (
  !!filters.urlPattern
  || !!filters.httpMethod
  || filters.isPublic !== ''
  || !!filters.requiredPermission
  || filters.isActive !== ''
))

const permissionOptionsForForm = computed(() => {
  const options = [...(permissionOptions.value || [])]
  const value = (form.requiredPermission || '').trim()
  if (!value) {
    return options
  }

  const exists = options.some(item => item.permissionCode === value)
  if (exists) {
    return options
  }

  return [{
    id: -1,
    permissionCode: value,
    permissionName: '（已不存在）',
    resource: '',
    action: ''
  }, ...options]
})

function buildParams() {
  const params = {
    page: pagination.page - 1,
    size: pagination.size
  }
  if (filters.urlPattern) params.urlPattern = filters.urlPattern
  if (filters.httpMethod) params.httpMethod = filters.httpMethod
  if (filters.isPublic !== '') params.isPublic = filters.isPublic
  if (filters.requiredPermission) params.requiredPermission = filters.requiredPermission
  if (filters.isActive !== '') params.isActive = filters.isActive
  return params
}

async function loadRows() {
  loading.value = true
  try {
    const data = await investApiService.getSystemUrlPermissionsPaged(buildParams())
    rows.value = Array.isArray(data?.content) ? data.content : []
    pagination.totalElements = Number(data?.totalElements || 0)
    pagination.totalPages = Number(data?.totalPages || 1)
    if (pagination.page > pagination.totalPages) {
      pagination.page = pagination.totalPages || 1
    }
    jumpPage.value = pagination.page
  } catch (error) {
    toast.error(`載入 URL 權限資料失敗：${error.message || '未知錯誤'}`)
  } finally {
    loading.value = false
  }
}

async function loadPermissionOptions() {
  try {
    const data = await investApiService.getSystemUrlPermissionOptions()
    permissionOptions.value = Array.isArray(data) ? data : []
  } catch (error) {
    toast.error(`載入 permission 選項失敗：${error.message || '未知錯誤'}`)
  }
}

async function loadMethodOptions() {
  try {
    const data = await investApiService.getSystemUrlMethodOptions()
    methodOptions.value = Array.isArray(data) ? data : []
  } catch (error) {
    toast.error(`載入 method 選項失敗：${error.message || '未知錯誤'}`)
  }
}

function applyFilters() {
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
}

function resetFilters() {
  filters.urlPattern = ''
  filters.httpMethod = ''
  filters.isPublic = ''
  filters.requiredPermission = ''
  filters.isActive = ''
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
}

function goPage(targetPage) {
  const page = Math.max(1, Math.min(targetPage, pagination.totalPages || 1))
  pagination.page = page
  jumpPage.value = page
  loadRows()
}

const goFirstPage = () => goPage(1)
const goPreviousPage = () => goPage(pagination.page - 1)
const goNextPage = () => goPage(pagination.page + 1)
const goLastPage = () => goPage(pagination.totalPages)
const goJumpPage = () => goPage(Number(jumpPage.value || 1))

function resetForm() {
  form.urlPattern = ''
  form.httpMethod = ''
  form.isPublic = false
  form.requiredRole = ''
  form.requiredPermission = ''
  form.description = ''
  form.isActive = true
  form.orderIndex = 0
}

async function openCreateModal() {
  if (!canManageUrlPermissions.value) {
    toast.error('你沒有新增 URL 權限權限')
    return
  }
  editingId.value = null
  resetForm()
  await Promise.all([loadPermissionOptions(), loadMethodOptions()])
  showModal.value = true
}

async function openEditModal(row) {
  if (!canManageUrlPermissions.value) {
    toast.error('你沒有編輯 URL 權限權限')
    return
  }

  try {
    const detail = await investApiService.getSystemUrlPermission(row.id)
    editingId.value = detail.id
    form.urlPattern = detail.urlPattern || ''
    form.httpMethod = detail.httpMethod || ''
    form.isPublic = Boolean(detail.isPublic)
    form.requiredRole = detail.requiredRole || ''
    form.requiredPermission = detail.requiredPermission || ''
    form.description = detail.description || ''
    form.isActive = Boolean(detail.isActive)
    form.orderIndex = Number(detail.orderIndex ?? 0)

    await Promise.all([loadPermissionOptions(), loadMethodOptions()])
    showModal.value = true
  } catch (error) {
    toast.error(`載入 URL 權限明細失敗：${error.message || '未知錯誤'}`)
  }
}

function closeModal() {
  showModal.value = false
  editingId.value = null
  resetForm()
}

async function submitForm() {
  if (!canManageUrlPermissions.value) {
    toast.error('你沒有管理 URL 權限權限')
    return
  }

  const urlPattern = (form.urlPattern || '').trim()
  if (!urlPattern) {
    toast.error('urlPattern 為必填')
    return
  }

  const payload = {
    urlPattern,
    httpMethod: (form.httpMethod || '').trim() || null,
    isPublic: Boolean(form.isPublic),
    requiredRole: (form.requiredRole || '').trim() || null,
    requiredPermission: (form.requiredPermission || '').trim() || null,
    description: (form.description || '').trim() || null,
    isActive: Boolean(form.isActive),
    orderIndex: Number.isFinite(Number(form.orderIndex)) ? Number(form.orderIndex) : 0
  }

  saving.value = true
  try {
    if (editingId.value) {
      await investApiService.updateSystemUrlPermission(editingId.value, payload)
      toast.success('URL 權限更新成功')
    } else {
      await investApiService.createSystemUrlPermission(payload)
      toast.success('URL 權限新增成功')
    }

    closeModal()
    await loadRows()
  } catch (error) {
    toast.error(`儲存 URL 權限失敗：${error.message || '未知錯誤'}`)
  } finally {
    saving.value = false
  }
}

async function removeRule(row) {
  if (!canManageUrlPermissions.value) {
    toast.error('你沒有刪除 URL 權限權限')
    return
  }

  if (!window.confirm(`確定要刪除 URL 規則「${row.urlPattern} [${row.httpMethod || 'ALL'}]」嗎？`)) {
    return
  }

  try {
    await investApiService.deleteSystemUrlPermission(row.id)
    toast.success('URL 權限刪除成功')
    await loadRows()
  } catch (error) {
    toast.error(`刪除 URL 權限失敗：${error.message || '未知錯誤'}`)
  }
}

watch(() => pagination.size, () => {
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
})

onMounted(async () => {
  await loadMethodOptions()
  if (canManageUrlPermissions.value) {
    await loadPermissionOptions()
  }
  loadRows()
})
</script>

<style scoped>
.system-url-permissions-page {
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

.url-cell {
  max-width: 340px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.hint {
  display: block;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
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
