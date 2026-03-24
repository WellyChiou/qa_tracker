<template>
  <AdminLayout>
    <div class="system-menus-page">
      <div class="page-header">
        <div>
          <h2>菜單管理</h2>
          <p>維護 invest-admin 導航結構、父子層級與 required permission 綁定。</p>
        </div>
        <button
          class="btn btn-primary"
          :disabled="!canManageMenus"
          @click="openCreateModal"
        >
          + 新增菜單
        </button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前菜單</span>
          <strong>{{ flattenedRows.length }}</strong>
          <p>invest ACL 目前可管理的菜單總筆數。</p>
        </article>
        <article class="overview-card">
          <span>根節點</span>
          <strong>{{ rootCount }}</strong>
          <p>第一層主菜單數量。</p>
        </article>
        <article class="overview-card">
          <span>啟用中</span>
          <strong>{{ activeCount }}</strong>
          <p>目前可見且啟用的菜單節點。</p>
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
              <label>菜單代碼</label>
              <input v-model="filters.menuCode" type="text" placeholder="輸入菜單代碼" />
            </div>
            <div class="filter-group">
              <label>菜單名稱</label>
              <input v-model="filters.menuName" type="text" placeholder="輸入菜單名稱" />
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
        <div v-if="loading" class="empty-state">載入菜單資料中...</div>
        <div v-else-if="pagedRows.length === 0" class="empty-state">
          {{ filteredRows.length === 0 ? '尚無菜單資料' : '沒有符合條件的資料' }}
        </div>
        <div v-else>
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>菜單名稱（Tree）</th>
                <th>菜單代碼</th>
                <th>URL</th>
                <th>排序</th>
                <th>required permission</th>
                <th>狀態</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in pagedRows" :key="row.id">
                <td>{{ row.id }}</td>
                <td>
                  <div class="tree-name" :style="{ paddingLeft: `${row.level * 18}px` }">
                    <span class="tree-dot">{{ row.level === 0 ? '●' : '└' }}</span>
                    <span>{{ row.menuName }}</span>
                  </div>
                </td>
                <td><code>{{ row.menuCode }}</code></td>
                <td class="url-cell" :title="row.url || '-'">{{ row.url || '-' }}</td>
                <td>{{ row.orderIndex ?? 0 }}</td>
                <td><code>{{ row.requiredPermission || '-' }}</code></td>
                <td>
                  <span :class="row.isActive ? 'status-active' : 'status-inactive'">
                    {{ row.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>
                  <div class="table-actions">
                    <button class="btn btn-secondary" :disabled="!canManageMenus" @click="openEditModal(row)">
                      編輯
                    </button>
                    <button
                      class="btn"
                      :class="row.isActive ? 'btn-danger' : 'btn-success'"
                      :disabled="!canManageMenus"
                      @click="toggleEnabled(row)"
                    >
                      {{ row.isActive ? '停用' : '啟用' }}
                    </button>
                    <button class="btn btn-danger" :disabled="!canManageMenus" @click="removeMenu(row)">
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
            page-size-id="system-menus-page-size"
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
            <h3 class="modal-title">{{ editingMenuId ? '編輯菜單' : '新增菜單' }}</h3>
            <button class="btn-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="filter-group">
                <label>菜單代碼 <span class="required">*</span></label>
                <input v-model="form.menuCode" type="text" placeholder="例如：INVEST_SYS_MENUS" />
              </div>
              <div class="filter-group">
                <label>菜單名稱 <span class="required">*</span></label>
                <input v-model="form.menuName" type="text" placeholder="例如：菜單管理" />
              </div>
              <div class="filter-group">
                <label>icon</label>
                <input v-model="form.icon" type="text" placeholder="例如：🧭" />
              </div>
              <div class="filter-group">
                <label>URL</label>
                <input v-model="form.url" type="text" placeholder="例如：/invest-admin/system/menus" />
              </div>
              <div class="filter-group">
                <label>父菜單</label>
                <select v-model="form.parentId">
                  <option :value="null">無（根菜單）</option>
                  <option
                    v-for="option in parentOptions"
                    :key="option.id"
                    :value="option.id"
                  >
                    {{ option.menuName }}（{{ option.menuCode }}）
                  </option>
                </select>
              </div>
              <div class="filter-group">
                <label>排序</label>
                <input v-model.number="form.orderIndex" type="number" min="0" placeholder="0" />
              </div>
              <div class="filter-group">
                <label>required permission</label>
                <select v-model="form.requiredPermission">
                  <option value="">無（不限制）</option>
                  <option
                    v-for="permission in requiredPermissionOptionsForForm"
                    :key="permission.permissionCode"
                    :value="permission.permissionCode"
                  >
                    {{ permission.permissionCode }} - {{ permission.permissionName }}
                  </option>
                </select>
              </div>
              <div class="filter-group">
                <label>啟用狀態</label>
                <select v-model="form.isActive">
                  <option :value="true">啟用</option>
                  <option :value="false">停用</option>
                </select>
              </div>
              <div class="filter-group">
                <label>顯示於 Dashboard</label>
                <select v-model="form.showInDashboard" :disabled="form.parentId !== null">
                  <option :value="true">顯示</option>
                  <option :value="false">隱藏</option>
                </select>
                <small class="hint">子菜單固定不顯示於 Dashboard。</small>
              </div>
              <div class="filter-group filter-group--full">
                <label>描述</label>
                <textarea v-model="form.description" rows="3" placeholder="請輸入描述"></textarea>
              </div>
            </div>

            <div class="modal-actions">
              <button class="btn btn-primary" :disabled="savingMenu" @click="submitMenuForm">
                {{ savingMenu ? '儲存中...' : '儲存' }}
              </button>
              <button class="btn btn-secondary" :disabled="savingMenu" @click="closeModal">取消</button>
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

const treeRows = ref([])
const loading = ref(false)
const jumpPage = ref(1)

const showModal = ref(false)
const editingMenuId = ref(null)
const savingMenu = ref(false)

const parentOptions = ref([])
const requiredPermissionOptions = ref([])

const filters = reactive({
  menuCode: '',
  menuName: '',
  isActive: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const form = reactive({
  menuCode: '',
  menuName: '',
  icon: '',
  url: '',
  parentId: null,
  orderIndex: 0,
  requiredPermission: '',
  isActive: true,
  showInDashboard: true,
  description: ''
})

const { currentUser } = useAuth()

const canManageMenus = computed(() => hasPermission(currentUser.value, 'INVEST_SYS_MENUS_EDIT'))

const flattenedRows = computed(() => flattenTree(treeRows.value || []))

const filteredRows = computed(() => {
  const codeKeyword = filters.menuCode.trim().toLowerCase()
  const nameKeyword = filters.menuName.trim().toLowerCase()

  return flattenedRows.value.filter(row => {
    if (codeKeyword && !(row.menuCode || '').toLowerCase().includes(codeKeyword)) {
      return false
    }
    if (nameKeyword && !(row.menuName || '').toLowerCase().includes(nameKeyword)) {
      return false
    }
    if (filters.isActive !== '' && Boolean(row.isActive) !== Boolean(filters.isActive)) {
      return false
    }
    return true
  })
})

const pagedRows = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filteredRows.value.slice(start, start + pagination.size)
})

const activeCount = computed(() => flattenedRows.value.filter(row => row.isActive).length)
const rootCount = computed(() => flattenedRows.value.filter(row => row.level === 0).length)

const requiredPermissionOptionsForForm = computed(() => {
  const options = [...(requiredPermissionOptions.value || [])]
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

function flattenTree(nodes, level = 0) {
  const list = []
  for (const node of nodes || []) {
    list.push({ ...node, level })
    if (Array.isArray(node.children) && node.children.length > 0) {
      list.push(...flattenTree(node.children, level + 1))
    }
  }
  return list
}

function refreshPagination() {
  pagination.totalElements = filteredRows.value.length
  pagination.totalPages = Math.max(1, Math.ceil(pagination.totalElements / pagination.size))
  if (pagination.page > pagination.totalPages) {
    pagination.page = pagination.totalPages
  }
  jumpPage.value = pagination.page
}

async function loadTree() {
  loading.value = true
  try {
    const data = await investApiService.getSystemMenusTree()
    treeRows.value = Array.isArray(data) ? data : []
    refreshPagination()
  } catch (error) {
    toast.error(`載入菜單資料失敗：${error.message || '未知錯誤'}`)
  } finally {
    loading.value = false
  }
}

async function loadRequiredPermissionOptions() {
  try {
    const data = await investApiService.getSystemMenuRequiredPermissionOptions()
    requiredPermissionOptions.value = Array.isArray(data) ? data : []
  } catch (error) {
    toast.error(`載入 required permission 清單失敗：${error.message || '未知錯誤'}`)
  }
}

async function loadParentOptions(excludeId = null) {
  try {
    const data = await investApiService.getSystemMenuParentOptions(excludeId)
    parentOptions.value = Array.isArray(data) ? data : []
  } catch (error) {
    toast.error(`載入父菜單清單失敗：${error.message || '未知錯誤'}`)
  }
}

function applyFilters() {
  pagination.page = 1
  refreshPagination()
}

function resetFilters() {
  filters.menuCode = ''
  filters.menuName = ''
  filters.isActive = ''
  pagination.page = 1
  refreshPagination()
}

function goPage(target) {
  const normalized = Math.max(1, Math.min(target, pagination.totalPages || 1))
  pagination.page = normalized
  jumpPage.value = normalized
}

const goFirstPage = () => goPage(1)
const goPreviousPage = () => goPage(pagination.page - 1)
const goNextPage = () => goPage(pagination.page + 1)
const goLastPage = () => goPage(pagination.totalPages)

const goJumpPage = () => {
  const target = Number(jumpPage.value)
  if (!Number.isInteger(target) || target < 1 || target > pagination.totalPages) {
    jumpPage.value = pagination.page
    return
  }
  goPage(target)
}

function resetForm() {
  form.menuCode = ''
  form.menuName = ''
  form.icon = ''
  form.url = ''
  form.parentId = null
  form.orderIndex = 0
  form.requiredPermission = ''
  form.isActive = true
  form.showInDashboard = true
  form.description = ''
}

async function openCreateModal() {
  if (!canManageMenus.value) {
    toast.error('你沒有新增菜單權限')
    return
  }

  editingMenuId.value = null
  resetForm()
  await Promise.all([
    loadParentOptions(null),
    loadRequiredPermissionOptions()
  ])
  showModal.value = true
}

async function openEditModal(row) {
  if (!canManageMenus.value) {
    toast.error('你沒有編輯菜單權限')
    return
  }

  try {
    const detail = await investApiService.getSystemMenu(row.id)
    editingMenuId.value = detail.id
    form.menuCode = detail.menuCode || ''
    form.menuName = detail.menuName || ''
    form.icon = detail.icon || ''
    form.url = detail.url || ''
    form.parentId = Number.isFinite(Number(detail.parentId)) ? Number(detail.parentId) : null
    form.orderIndex = Number(detail.orderIndex ?? 0)
    form.requiredPermission = detail.requiredPermission || ''
    form.isActive = Boolean(detail.isActive)
    form.showInDashboard = Boolean(detail.showInDashboard)
    form.description = detail.description || ''

    await Promise.all([
      loadParentOptions(detail.id),
      loadRequiredPermissionOptions()
    ])

    showModal.value = true
  } catch (error) {
    toast.error(`載入菜單明細失敗：${error.message || '未知錯誤'}`)
  }
}

function closeModal() {
  showModal.value = false
  editingMenuId.value = null
  resetForm()
}

async function submitMenuForm() {
  if (!canManageMenus.value) {
    toast.error('你沒有管理菜單權限')
    return
  }

  const menuCode = (form.menuCode || '').trim()
  const menuName = (form.menuName || '').trim()

  if (!menuCode || !menuName) {
    toast.error('菜單代碼與菜單名稱為必填')
    return
  }

  const payload = {
    menuCode,
    menuName,
    icon: (form.icon || '').trim() || null,
    url: (form.url || '').trim() || null,
    parentId: form.parentId === null || form.parentId === '' ? null : Number(form.parentId),
    orderIndex: Number.isFinite(Number(form.orderIndex)) ? Number(form.orderIndex) : 0,
    isActive: Boolean(form.isActive),
    showInDashboard: Boolean(form.showInDashboard),
    requiredPermission: (form.requiredPermission || '').trim() || null,
    description: (form.description || '').trim() || null
  }

  savingMenu.value = true
  try {
    if (editingMenuId.value) {
      await investApiService.updateSystemMenu(editingMenuId.value, payload)
      toast.success('菜單更新成功')
    } else {
      await investApiService.createSystemMenu(payload)
      toast.success('菜單新增成功')
    }

    closeModal()
    await loadTree()
  } catch (error) {
    toast.error(`儲存菜單失敗：${error.message || '未知錯誤'}`)
  } finally {
    savingMenu.value = false
  }
}

async function toggleEnabled(row) {
  if (!canManageMenus.value) {
    toast.error('你沒有更新菜單狀態權限')
    return
  }

  const targetEnabled = !row.isActive
  const actionText = targetEnabled ? '啟用' : '停用'
  if (!window.confirm(`確定要${actionText}菜單「${row.menuName}」嗎？`)) {
    return
  }

  try {
    await investApiService.setSystemMenuEnabled(row.id, targetEnabled)
    toast.success(`菜單${actionText}成功`)
    await loadTree()
  } catch (error) {
    toast.error(`菜單${actionText}失敗：${error.message || '未知錯誤'}`)
  }
}

async function removeMenu(row) {
  if (!canManageMenus.value) {
    toast.error('你沒有刪除菜單權限')
    return
  }

  if (!window.confirm(`確定要刪除菜單「${row.menuName}」嗎？`)) {
    return
  }

  try {
    await investApiService.deleteSystemMenu(row.id)
    toast.success('菜單刪除成功')
    await loadTree()
  } catch (error) {
    toast.error(`刪除菜單失敗：${error.message || '未知錯誤'}`)
  }
}

watch(() => pagination.size, () => {
  pagination.page = 1
  refreshPagination()
})

watch(filteredRows, () => {
  refreshPagination()
})

watch(() => form.parentId, (value) => {
  if (value !== null) {
    form.showInDashboard = false
  }
})

onMounted(() => {
  loadTree()
})
</script>

<style scoped>
.system-menus-page {
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
  width: 220px;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.tree-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tree-dot {
  color: rgba(15, 23, 42, 0.48);
  font-size: 12px;
  min-width: 12px;
}

.url-cell {
  max-width: 260px;
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
