<template>
  <AdminLayout>
    <div class="admin-session-list">
      <div class="page-header">
        <div>
          <h2>場次管理</h2>
          <p>維護聚會場次、開放時間與狀態。</p>
        </div>
        <button @click="openAddModal" class="btn btn-primary">新增場次</button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>場次總數</span>
          <strong>{{ totalRecords }}</strong>
          <p>目前條件下可管理的場次總筆數。</p>
        </article>
        <article class="overview-card">
          <span>當前頁面</span>
          <strong>{{ filteredList.length }}</strong>
          <p>此頁顯示的場次筆數，依後端分頁結果同步。</p>
        </article>
        <article class="overview-card">
          <span>篩選狀態</span>
          <strong>{{ filters.sessionCode || filters.title || filters.status || filters.sessionType || filters.groupId || filters.startDate || filters.endDate ? '已套用' : '全部' }}</strong>
          <p>可依場次代碼、名稱、型態、日期與狀態快速縮小範圍。</p>
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
              <label>場次代碼</label>
              <input
                type="text"
                v-model="filters.sessionCode"
                placeholder="輸入場次代碼"
                class="form-input"
                @keyup.enter="load"
              />
            </div>
            <div class="filter-group">
              <label>標題</label>
              <input
                type="text"
                v-model="filters.title"
                placeholder="輸入標題"
                class="form-input"
                @keyup.enter="load"
              />
            </div>
            <div class="filter-group">
              <label>狀態</label>
              <select v-model="filters.status" class="form-input">
                <option value="">全部</option>
                <option value="ACTIVE">啟用</option>
                <option value="INACTIVE">停用</option>
                <option value="DRAFT">草稿</option>
              </select>
            </div>
            <div class="filter-group">
              <label>類型</label>
              <select v-model="filters.sessionType" class="form-input" @change="onSessionTypeChange">
                <option value="">全部</option>
                <option value="SATURDAY">週六晚崇</option>
                <option value="SUNDAY">週日早崇</option>
                <option value="WEEKDAY">小組</option>
                <option value="SPECIAL">活動</option>
              </select>
            </div>
            <div class="filter-group" v-if="filters.sessionType === 'WEEKDAY'">
              <label>小組</label>
              <select v-model="filters.groupId" class="form-input">
                <option value="">全部小組</option>
                <option v-for="group in activeGroups" :key="group.id" :value="group.id">
                  {{ group.groupName }}
                </option>
              </select>
            </div>
            <div class="filter-group">
              <label>開始日期</label>
              <input
                type="date"
                v-model="filters.startDate"
                class="form-input"
              />
            </div>
            <div class="filter-group">
              <label>結束日期</label>
              <input
                type="date"
                v-model="filters.endDate"
                class="form-input"
              />
            </div>
            <div class="filter-group">
              <button @click="load" class="btn btn-primary">查詢</button>
              <button @click="resetFilters" class="btn btn-secondary">清除</button>
            </div>
          </div>
        </div>
      </details>

      <div class="sessions-list card surface-card">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ sessions.length === 0 ? '尚無場次資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="sessions-table">
          <div class="table-header">
            <h3>場次列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>場次代碼</th>
                <th>標題</th>
                <th>類型</th>
                <th>日期</th>
                <th>開始時間</th>
                <th>結束時間</th>
                <th>狀態</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="session in paginatedList" :key="session.id">
                <td>{{ session.sessionCode || '-' }}</td>
                <td>{{ session.title || '-' }}</td>
                <td>{{ getSessionTypeText(session.sessionType) }}</td>
                <td>{{ formatDate(session.sessionDate) }}</td>
                <td>{{ formatDateTime(session.openAt) }}</td>
                <td>{{ formatDateTime(session.closeAt) }}</td>
                <td>
                  <span :class="['status-badge', getStatusClass(session.status)]">
                    {{ getStatusText(session.status) }}
                  </span>
                </td>
                <td>
                  <div class="table-actions">
                    <button @click="editSession(session.id)" class="btn btn-edit">
                      <span class="btn__icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M12 20h9"/>
                          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/>
                        </svg>
                      </span>
                      <span>編輯</span>
                    </button>
                    <button @click="deleteSession(session.id)" class="btn btn-delete">
                      <span class="btn__icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <polyline points="3 6 5 6 21 6"/>
                          <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                          <path d="M10 11v6"/>
                          <path d="M14 11v6"/>
                          <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                        </svg>
                      </span>
                      <span>刪除</span>
                    </button>
                  </div>
                </td>
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

      <!-- 新增/編輯場次 Modal -->
      <SessionModal
        v-if="showModal"
        :show="showModal"
        :session="editingSession"
        @close="closeModal"
        @saved="handleSaved"
      />
    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@shared/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import AdminLayout from '@/components/AdminLayout.vue'
import SessionModal from '@/components/SessionModal.vue'
import { apiRequest } from '@/utils/api'

const router = useRouter()
const sessions = ref([])
const showModal = ref(false)
const editingSession = ref(null)

const filters = ref({
  sessionCode: '',
  title: '',
  status: '',
  sessionType: '',
  groupId: '',
  startDate: '',
  endDate: ''
})

const activeGroups = ref([])

const recordsPerPage = ref(20)
const currentPage = ref(1)
const totalRecords = ref(0)
const totalPages = ref(1)
const jumpPage = ref(1)

// 前端不再需要篩選，因為後端已經處理了
const filteredList = computed(() => {
  return sessions.value.sort((a, b) => {
    if (a.sessionDate && b.sessionDate) {
      return new Date(b.sessionDate) - new Date(a.sessionDate)
    }
    return (b.id || 0) - (a.id || 0)
  })
})

// 注意：分頁現在由後端處理，但前端過濾仍然保留（過濾當前頁數據）
const paginatedList = computed(() => {
  return filteredList.value
})

watch([() => filters.value.sessionCode, () => filters.value.title, () => filters.value.status, () => filters.value.sessionType, () => filters.value.groupId], () => {
  currentPage.value = 1
  jumpPage.value = 1
})

watch(currentPage, (newVal) => {
  jumpPage.value = newVal
})

watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  load()
})

async function load() {
  try {
    // 構建查詢參數
    const params = new URLSearchParams()
    if (filters.value.sessionCode) params.append('sessionCode', filters.value.sessionCode)
    if (filters.value.title) params.append('title', filters.value.title)
    if (filters.value.status) params.append('status', filters.value.status)
    if (filters.value.sessionType) params.append('sessionType', filters.value.sessionType)
    if (filters.value.groupId) params.append('groupId', filters.value.groupId)
    if (filters.value.startDate) params.append('startDate', filters.value.startDate)
    if (filters.value.endDate) params.append('endDate', filters.value.endDate)
    
    const queryString = params.toString()
    // 添加分頁參數
    const paginationParams = `page=${currentPage.value - 1}&size=${recordsPerPage.value}`
    const url = queryString 
      ? `/church/checkin/admin/sessions?${queryString}&${paginationParams}` 
      : `/church/checkin/admin/sessions?${paginationParams}`
    
    // apiRequest 現在會自動返回解析後的資料
    const responseData = await apiRequest(url, {
      method: 'GET'
    })
    
    if (responseData) {
      // 處理 ApiResponse 格式或直接返回的資料
      let data = responseData
      if (responseData.success && responseData.data) {
        data = responseData.data
      } else if (responseData.data) {
        data = responseData.data
      }
      
      // 處理分頁響應
      if (data.content) {
        sessions.value = data.content
        totalRecords.value = data.totalElements || 0
        totalPages.value = data.totalPages || 1
        // 確保 currentPage 不超過 totalPages
        if (currentPage.value > totalPages.value) {
          currentPage.value = totalPages.value
          jumpPage.value = totalPages.value
        }
        // 同步 jumpPage 與 currentPage
        jumpPage.value = currentPage.value
        toast.success(`查詢成功，共 ${totalRecords.value} 筆場次`, '場次管理')
      } else {
        // 兼容舊格式（無分頁）
        sessions.value = Array.isArray(data) ? data : []
        totalRecords.value = sessions.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
        toast.success(`查詢成功，共 ${sessions.value.length} 筆場次`, '場次管理')
      }
    }
  } catch (error) {
    console.error('載入場次列表失敗:', error)
    toast.error('查詢場次列表失敗', '場次管理')
  }
}

function resetFilters() {
  filters.value = {
    sessionCode: '',
    title: '',
    status: '',
    sessionType: '',
    groupId: '',
    startDate: '',
    endDate: ''
  }
  load()
}

function onSessionTypeChange() {
  // 當類型不是「小組」時，清除小組篩選
  if (filters.value.sessionType !== 'WEEKDAY') {
    filters.value.groupId = ''
  }
  // 不自動載入，讓用戶點擊查詢按鈕
}

async function loadActiveGroups() {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/groups/active', {
      method: 'GET'
    })
    if (data) {
      activeGroups.value = data.groups || data || []
    }
  } catch (error) {
    console.error('載入小組列表失敗:', error)
  }
}

function openAddModal() {
  editingSession.value = null
  showModal.value = true
}

function editSession(id) {
  router.push(`/checkin/admin/sessions/${id}`)
}

async function deleteSession(id) {
  const session = sessions.value.find(s => s.id === id)
  const confirmMsg = `確定要刪除場次「${session?.title || session?.sessionCode || id}」嗎？\n此操作無法復原。`
  
  if (!window.confirm(confirmMsg)) {
    return
  }

  try {
    await apiRequest(`/church/checkin/admin/sessions/${id}`, {
      method: 'DELETE'
    }, '刪除中...', true)
    toast.success('場次已刪除')
    await load()
  } catch (error) {
    console.error('刪除場次失敗:', error)
    toast.error('刪除場次失敗')
  }
}

function closeModal() {
  showModal.value = false
  editingSession.value = null
}

function handleSaved() {
  closeModal()
  load()
}

function firstPage() {
  currentPage.value = 1
  jumpPage.value = 1
  load()
}

function previousPage() {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    load()
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    load()
  }
}

function lastPage() {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  load()
}

function jumpToPage() {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    load()
  } else {
    jumpPage.value = currentPage.value
  }
}

function formatDate(date) {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return date
  return d.toLocaleDateString('zh-TW')
}

function formatDateTime(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

function getSessionTypeText(sessionType) {
  const typeMap = {
    'SATURDAY': '週六晚崇',
    'SUNDAY': '週日早崇',
    'WEEKDAY': '小組',
    'SPECIAL': '活動'
  }
  return typeMap[sessionType] || sessionType || '-'
}

function getStatusText(status) {
  const statusMap = {
    'ACTIVE': '啟用',
    'INACTIVE': '停用',
    'DRAFT': '草稿'
  }
  return statusMap[status] || status || '-'
}

function getStatusClass(status) {
  const classMap = {
    'ACTIVE': 'status-active',
    'INACTIVE': 'status-inactive',
    'DRAFT': 'status-draft'
  }
  return classMap[status] || ''
}

onMounted(() => {
  loadActiveGroups()
  load()
})
</script>

<style scoped>
.admin-session-list {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: var(--text);
}

.page-header p {
  margin: 6px 0 0;
  max-width: 700px;
  color: var(--text-muted);
  line-height: 1.55;
  font-size: 13px;
}

.overview-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.overview-card {
  padding: 13px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.92));
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

.overview-card span {
  display: block;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: var(--text-muted);
}

.overview-card strong {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  line-height: 1;
  color: var(--text);
}

.overview-card p {
  margin: 6px 0 0;
  color: var(--text-muted);
  line-height: 1.5;
  font-size: 12px;
}

.overview-card--accent {
  background: linear-gradient(135deg, rgba(180, 83, 9, 0.92), rgba(194, 65, 12, 0.92));
  border-color: transparent;
}

.overview-card--accent span,
.overview-card--accent strong,
.overview-card--accent p {
  color: #fff7ed;
}

.surface-card {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.08);
}

.empty-state {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.sessions-table {
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.table-header {
  padding: 11px 14px;
  border-bottom: 1px solid var(--border);
  background: var(--bg);
}

.table-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
}

table {
  width: 100%;
  border-collapse: collapse;
}

table th,
table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}

table th {
  font-weight: 600;
  color: var(--text);
  background: var(--bg);
}

table td {
  color: var(--text);
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 700;
}

.status-active {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.status-inactive {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.status-draft {
  background: rgba(251, 191, 36, 0.1);
  color: #fbbf24;
}

.table-actions {
  display: flex;
  gap: 6px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--btn-bg);
  color: var(--text);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.btn:hover {
  background: var(--btn-hover-bg);
  transform: none;
}

.btn-primary {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.btn-primary:hover {
  background: var(--primary-hover);
}

.btn-secondary {
  background: var(--bg);
  color: var(--text);
}

.btn-edit {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border-color: rgba(59, 130, 246, 0.2);
}

.btn-edit:hover {
  background: rgba(59, 130, 246, 0.15);
}

.btn-delete {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.2);
}

.btn-delete:hover {
  background: rgba(239, 68, 68, 0.15);
}

.btn__icon {
  display: inline-flex;
  width: 16px;
  height: 16px;
}

.btn__icon svg {
  width: 100%;
  height: 100%;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 11px 14px;
  border-top: 1px solid var(--border);
  background: var(--bg);
}

.pagination-left,
.pagination-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-label {
  font-size: 12px;
  color: var(--text-muted);
}

.page-size-select,
.page-input {
  padding: 6px 9px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--input-bg);
  color: var(--text);
  font-size: 12px;
}

.page-input {
  width: 60px;
  text-align: center;
}

/* Filters 樣式（參考 Persons.vue） */
.filters {
  margin-bottom: 14px;
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.filters summary {
  list-style: none;
  cursor: pointer;
  padding: 12px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filters summary::-webkit-details-marker {
  display: none;
}

.filters__title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filters__title h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
}

.filters__badge {
  font-size: 11px;
  color: var(--text-muted);
  padding: 2px 7px;
  background: var(--bg);
  border-radius: 4px;
}

.filters__chev {
  transition: transform 0.2s ease;
}

.filters[open] .filters__chev {
  transform: rotate(180deg);
}

.filters__content {
  padding: 14px;
  border-top: 1px solid var(--border);
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-group label {
  font-size: 12px;
  font-weight: 700;
  color: var(--text);
}

.form-input {
  padding: 9px 11px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--input-bg);
  color: var(--text);
  font-size: 13px;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .overview-strip {
    grid-template-columns: 1fr;
  }
}
</style>
