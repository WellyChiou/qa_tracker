<template>
  <AdminLayout>
    <div class="system-users-page">
      <div class="page-header">
        <div>
          <h2>用戶管理</h2>
          <p>集中管理 invest 後台登入帳號、啟停狀態與密碼重設。</p>
        </div>
        <button
          class="btn btn-primary"
          :disabled="!canManageUsers"
          @click="openCreateModal"
        >
          + 新增用戶
        </button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前用戶</span>
          <strong>{{ pagination.totalElements }}</strong>
          <p>invest ACL 內可管理的帳號總數。</p>
        </article>
        <article class="overview-card">
          <span>當前頁面</span>
          <strong>{{ rows.length }}</strong>
          <p>這一頁符合條件的用戶筆數。</p>
        </article>
        <article class="overview-card">
          <span>啟用中</span>
          <strong>{{ activeCount }}</strong>
          <p>目前頁面中可登入帳號數量。</p>
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
              <label>用戶名</label>
              <input v-model="filters.username" type="text" placeholder="輸入用戶名" />
            </div>
            <div class="filter-group">
              <label>電子郵件</label>
              <input v-model="filters.email" type="text" placeholder="輸入電子郵件" />
            </div>
            <div class="filter-group">
              <label>狀態</label>
              <select v-model="filters.isEnabled">
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
        <div v-if="loading" class="empty-state">載入用戶資料中...</div>
        <div v-else-if="rows.length === 0" class="empty-state">
          {{ pagination.totalElements === 0 ? '尚無用戶資料' : '沒有符合條件的資料' }}
        </div>
        <div v-else>
          <table class="table">
            <thead>
              <tr>
                <th>UID</th>
                <th>用戶名</th>
                <th>顯示名稱</th>
                <th>電子郵件</th>
                <th>角色</th>
                <th>狀態</th>
                <th>最後登入</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.uid">
                <td><code class="uid">{{ row.uid }}</code></td>
                <td>{{ row.username }}</td>
                <td>{{ row.displayName || '-' }}</td>
                <td>{{ row.email || '-' }}</td>
                <td>{{ formatRoles(row) }}</td>
                <td>
                  <span :class="row.isEnabled ? 'status-active' : 'status-inactive'">
                    {{ row.isEnabled ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>{{ formatDateTime(row.lastLoginAt) }}</td>
                <td>
                  <div class="table-actions">
                    <button class="btn btn-secondary" :disabled="!canManageUsers" @click="openEditModal(row)">
                      編輯
                    </button>
                    <button
                      class="btn"
                      :class="row.isEnabled ? 'btn-danger' : 'btn-success'"
                      :disabled="!canManageUsers"
                      @click="toggleEnabled(row)"
                    >
                      {{ row.isEnabled ? '停用' : '啟用' }}
                    </button>
                    <button class="btn btn-secondary" :disabled="!canResetPassword" @click="openResetModal(row)">
                      重設密碼
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
            page-size-id="system-users-page-size"
            @first="goFirstPage"
            @previous="goPreviousPage"
            @next="goNextPage"
            @last="goLastPage"
            @jump="goJumpPage"
          />
        </div>
      </div>

      <div v-if="showUserModal" class="modal-overlay" @click="closeUserModal">
        <div class="modal-panel" @click.stop>
          <div class="modal-header">
            <h3 class="modal-title">{{ editingUid ? '編輯用戶' : '新增用戶' }}</h3>
            <button class="btn-close" @click="closeUserModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="filter-group">
                <label>用戶名 <span class="required">*</span></label>
                <input
                  v-model="form.username"
                  type="text"
                  placeholder="請輸入用戶名"
                  :disabled="!!editingUid"
                />
              </div>
              <div class="filter-group">
                <label>電子郵件 <span class="required">*</span></label>
                <input v-model="form.email" type="email" placeholder="請輸入電子郵件" />
              </div>
              <div class="filter-group">
                <label>顯示名稱</label>
                <input v-model="form.displayName" type="text" placeholder="可留空" />
              </div>
              <div class="filter-group">
                <label>頭像網址</label>
                <input v-model="form.photoUrl" type="text" placeholder="可留空" />
              </div>
              <div v-if="!editingUid" class="filter-group">
                <label>初始密碼 <span class="required">*</span></label>
                <input v-model="form.password" type="password" placeholder="至少 6 碼" />
              </div>
              <div class="filter-group">
                <label>啟用狀態</label>
                <select v-model="form.isEnabled">
                  <option :value="true">啟用</option>
                  <option :value="false">停用</option>
                </select>
              </div>
              <div class="filter-group">
                <label>帳號鎖定狀態</label>
                <select v-model="form.isAccountNonLocked">
                  <option :value="true">未鎖定</option>
                  <option :value="false">已鎖定</option>
                </select>
              </div>
            </div>
            <div class="modal-actions">
              <button class="btn btn-primary" :disabled="savingUser" @click="submitUserForm">
                {{ savingUser ? '儲存中...' : '儲存' }}
              </button>
              <button class="btn btn-secondary" :disabled="savingUser" @click="closeUserModal">取消</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showResetModal" class="modal-overlay" @click="closeResetModal">
        <div class="modal-panel" @click.stop>
          <div class="modal-header">
            <h3 class="modal-title">重設密碼</h3>
            <button class="btn-close" @click="closeResetModal">×</button>
          </div>
          <div class="modal-body">
            <p class="reset-target">
              目標用戶：<strong>{{ resetTarget?.displayName || resetTarget?.username || '-' }}</strong>
            </p>
            <div class="form-grid">
              <div class="filter-group">
                <label>新密碼 <span class="required">*</span></label>
                <input v-model="resetForm.newPassword" type="password" placeholder="至少 6 碼" />
              </div>
              <div class="filter-group">
                <label>確認新密碼 <span class="required">*</span></label>
                <input v-model="resetForm.confirmPassword" type="password" placeholder="再次輸入新密碼" />
              </div>
            </div>
            <div class="modal-actions">
              <button class="btn btn-primary" :disabled="resettingPassword" @click="submitResetPassword">
                {{ resettingPassword ? '儲存中...' : '確認重設' }}
              </button>
              <button class="btn btn-secondary" :disabled="resettingPassword" @click="closeResetModal">取消</button>
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

const showUserModal = ref(false)
const editingUid = ref('')
const savingUser = ref(false)

const showResetModal = ref(false)
const resetTarget = ref(null)
const resettingPassword = ref(false)

const filters = reactive({
  username: '',
  email: '',
  isEnabled: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  totalPages: 1,
  totalElements: 0
})

const form = reactive({
  username: '',
  email: '',
  displayName: '',
  photoUrl: '',
  password: '',
  isEnabled: true,
  isAccountNonLocked: true
})

const resetForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const { currentUser } = useAuth()

const canManageUsers = computed(() => hasPermission(currentUser.value, 'INVEST_SYS_USERS_EDIT'))
const canResetPassword = computed(() => (
  hasPermission(currentUser.value, 'INVEST_SYS_USERS_RESET_PASSWORD') || canManageUsers.value
))
const activeCount = computed(() => rows.value.filter(row => row.isEnabled).length)

const buildParams = () => {
  const params = {
    page: pagination.page - 1,
    size: pagination.size
  }
  if (filters.username) params.username = filters.username
  if (filters.email) params.email = filters.email
  if (filters.isEnabled !== '') params.isEnabled = filters.isEnabled
  return params
}

const loadRows = async () => {
  loading.value = true
  try {
    const data = await investApiService.getSystemUsersPaged(buildParams())
    rows.value = Array.isArray(data?.content) ? data.content : []
    pagination.totalElements = Number(data?.totalElements || 0)
    pagination.totalPages = Number(data?.totalPages || 1)
    if (pagination.page > pagination.totalPages) {
      pagination.page = pagination.totalPages || 1
    }
    jumpPage.value = pagination.page
  } catch (error) {
    toast.error(`載入用戶資料失敗：${error.message || '未知錯誤'}`)
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
  filters.username = ''
  filters.email = ''
  filters.isEnabled = ''
  pagination.page = 1
  jumpPage.value = 1
  loadRows()
}

const goPage = (targetPage) => {
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

const resetUserForm = () => {
  form.username = ''
  form.email = ''
  form.displayName = ''
  form.photoUrl = ''
  form.password = ''
  form.isEnabled = true
  form.isAccountNonLocked = true
}

const openCreateModal = () => {
  if (!canManageUsers.value) {
    return
  }
  editingUid.value = ''
  resetUserForm()
  showUserModal.value = true
}

const openEditModal = async (row) => {
  if (!canManageUsers.value) {
    return
  }
  try {
    const detail = await investApiService.getSystemUser(row.uid)
    editingUid.value = row.uid
    form.username = detail?.username || ''
    form.email = detail?.email || ''
    form.displayName = detail?.displayName || ''
    form.photoUrl = detail?.photoUrl || ''
    form.password = ''
    form.isEnabled = detail?.isEnabled !== false
    form.isAccountNonLocked = detail?.isAccountNonLocked !== false
    showUserModal.value = true
  } catch (error) {
    toast.error(`載入用戶明細失敗：${error.message || '未知錯誤'}`)
  }
}

const closeUserModal = () => {
  showUserModal.value = false
  editingUid.value = ''
  resetUserForm()
}

const submitUserForm = async () => {
  if (!form.username || !form.email) {
    toast.error('用戶名與電子郵件為必填')
    return
  }
  if (!editingUid.value && !form.password) {
    toast.error('新增用戶時初始密碼為必填')
    return
  }
  if (!editingUid.value && form.password.length < 6) {
    toast.error('初始密碼至少需 6 碼')
    return
  }

  savingUser.value = true
  try {
    const payload = {
      username: form.username,
      email: form.email,
      displayName: form.displayName || null,
      photoUrl: form.photoUrl || null,
      isEnabled: form.isEnabled,
      isAccountNonLocked: form.isAccountNonLocked
    }
    if (!editingUid.value) {
      payload.password = form.password
      await investApiService.createSystemUser(payload)
      toast.success('用戶新增成功')
    } else {
      await investApiService.updateSystemUser(editingUid.value, payload)
      toast.success('用戶更新成功')
    }
    closeUserModal()
    loadRows()
  } catch (error) {
    toast.error(`儲存用戶失敗：${error.message || '未知錯誤'}`)
  } finally {
    savingUser.value = false
  }
}

const toggleEnabled = async (row) => {
  if (!canManageUsers.value) {
    return
  }
  const targetEnabled = !row.isEnabled
  const action = targetEnabled ? '啟用' : '停用'
  if (!confirm(`確定要${action}用戶 ${row.username} 嗎？`)) {
    return
  }
  try {
    await investApiService.setSystemUserEnabled(row.uid, targetEnabled)
    toast.success(`已${action} ${row.username}`)
    loadRows()
  } catch (error) {
    toast.error(`更新狀態失敗：${error.message || '未知錯誤'}`)
  }
}

const openResetModal = (row) => {
  if (!canResetPassword.value) {
    return
  }
  resetTarget.value = row
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  showResetModal.value = true
}

const closeResetModal = () => {
  showResetModal.value = false
  resetTarget.value = null
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
}

const submitResetPassword = async () => {
  if (!resetTarget.value) {
    return
  }
  if (!resetForm.newPassword) {
    toast.error('請輸入新密碼')
    return
  }
  if (resetForm.newPassword.length < 6) {
    toast.error('新密碼至少需 6 碼')
    return
  }
  if (resetForm.newPassword !== resetForm.confirmPassword) {
    toast.error('兩次輸入的新密碼不一致')
    return
  }

  resettingPassword.value = true
  try {
    await investApiService.resetSystemUserPassword(resetTarget.value.uid, resetForm.newPassword)
    toast.success(`已重設 ${resetTarget.value.username} 密碼`)
    closeResetModal()
  } catch (error) {
    toast.error(`重設密碼失敗：${error.message || '未知錯誤'}`)
  } finally {
    resettingPassword.value = false
  }
}

const formatRoles = (row) => {
  if (!Array.isArray(row?.roleNames) || row.roleNames.length === 0) {
    return '-'
  }
  return row.roleNames.join(', ')
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-TW', { hour12: false })
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
.system-users-page {
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
  font-weight: 700;
  margin-bottom: 4px;
}

.overview-card strong {
  display: block;
  font-size: 24px;
  line-height: 1.1;
  margin-bottom: 6px;
}

.overview-card p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.overview-card--accent {
  background: linear-gradient(135deg, #1f3b8f, #3457c5);
  color: #fff;
}

.overview-card--accent span,
.overview-card--accent p {
  color: rgba(255, 255, 255, 0.85);
}

.overview-card--accent strong {
  color: #fff;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.filter-group.actions {
  display: flex;
  flex-direction: row;
  align-items: flex-end;
  gap: 8px;
}

.table-wrap {
  padding: 14px;
}

.uid {
  font-size: 12px;
}

.col-actions {
  width: 320px;
}

.table-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.required {
  color: #dc2626;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.reset-target {
  margin-top: 0;
  margin-bottom: 12px;
  color: var(--text-secondary);
}

@media (max-width: 1024px) {
  .overview-strip {
    grid-template-columns: 1fr;
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .col-actions {
    width: 260px;
  }
}

@media (max-width: 768px) {
  .filter-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .filter-group.actions {
    flex-direction: column;
  }

  .table-actions {
    flex-direction: column;
  }
}
</style>
