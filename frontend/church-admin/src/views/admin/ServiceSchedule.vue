<template>
  <AdminLayout>
    <div class="admin-service-schedule">
      <div class="page-header">
        <div>
          <h2>服事表管理</h2>
          <p>整理年度服事配置與區間，讓主日排班更容易追蹤與維護。</p>
        </div>
        <button @click="openAddModal" class="btn btn-primary">+ 新增服事表</button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前服事表</span>
          <strong>{{ totalRecords }}</strong>
          <p>後台目前可維護的服事表總數。</p>
        </article>
        <article class="overview-card">
          <span>當前頁面</span>
          <strong>{{ schedules.length }}</strong>
          <p>目前這一頁實際載入的年度服事表。</p>
        </article>
        <article class="overview-card">
          <span>年份篩選</span>
          <strong>{{ filters.year || '全部' }}</strong>
          <p>可直接聚焦單一年度的排班配置與日期區間。</p>
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
            <label>年份</label>
            <select v-model="filters.year" class="form-input">
              <option value="">全部</option>
              <option v-for="year in availableYears" :key="year" :value="year">{{ year }}年</option>
            </select>
          </div>
        </div>
        </div>
      </details>

      <div class="schedule-list card surface-card">
        <div v-if="schedules.length === 0" class="empty-state">
          <p>尚無服事表</p>
        </div>
        <div v-else class="schedule-table-wrapper">
          <div class="table-header">
            <h3>服事表列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table class="schedule-table">
            <thead>
              <tr>
                <th>年度</th>
                <th>日期範圍</th>
                <th>建立時間</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="schedule in schedules" :key="schedule.year">
                <td>{{ schedule.year }}年</td>
                <td>
                  <span v-if="schedule.startDate && schedule.endDate">
                {{ formatDate(schedule.startDate) }} ~ {{ formatDate(schedule.endDate) }}
                  </span>
                  <span v-else>-</span>
                </td>
                <td>
                  <span v-if="schedule.createdAt">{{ formatDateTime(schedule.createdAt) }}</span>
                  <span v-else>-</span>
                </td>
                <td><div class="table-actions"><button @click="editSchedule(schedule.year)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
              <button @click="deleteSchedule(schedule.year)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
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

      <!-- 服事表 Modal（重用現有的 ServiceScheduleModal） -->
      <ServiceScheduleModal
        v-if="showModal"
        :show="showModal"
        :mode="modalMode"
        :schedule-year="currentScheduleYear"
        :position-config="positionConfig"
        @close="closeModal"
        @saved="handleSaved"
        @updated="handleUpdated"
      />
    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@shared/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import ServiceScheduleModal from '@/components/ServiceScheduleModal.vue'
import { apiRequest } from '@/utils/api'

const schedules = ref([])
const showModal = ref(false)
const modalMode = ref('add')
const currentScheduleYear = ref(null)
const positionConfig = ref({})

// 查詢條件
const filters = ref({
  year: ''
})

// 計算可用的年份列表
const availableYears = computed(() => {
  const years = new Set()
  schedules.value.forEach(schedule => {
    if (schedule.year) {
      years.add(schedule.year)
    }
  })
  return Array.from(years).sort((a, b) => b - a) // 降序排列
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
  loadSchedules()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadSchedules()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadSchedules()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadSchedules()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadSchedules()
  } else {
    jumpPage.value = currentPage.value
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    year: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
  loadSchedules()
}

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => filters.value.year, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadSchedules()
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadSchedules()
})

const loadSchedules = async () => {
  try {
    const params = new URLSearchParams()
    if (filters.value.year) {
      params.append('year', filters.value.year.toString())
    }
    params.append('page', (currentPage.value - 1).toString())
    params.append('size', recordsPerPage.value.toString())
    
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/service-schedules?${params.toString()}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 處理 PageResponse 格式
      let pageData = data
      if (data.content !== undefined && data.totalElements !== undefined) {
        // 這是 PageResponse 格式
        schedules.value = Array.isArray(data.content) ? data.content : []
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
        schedules.value = data.content || data || []
        if (data.totalElements !== undefined) {
          totalRecords.value = data.totalElements
          totalPages.value = data.totalPages || 1
          if (currentPage.value > totalPages.value) {
            currentPage.value = totalPages.value
            jumpPage.value = totalPages.value
          }
          jumpPage.value = currentPage.value
        } else {
          totalRecords.value = schedules.value.length
          totalPages.value = 1
          currentPage.value = 1
          jumpPage.value = 1
        }
      }
      
      // 更新可用年份列表（從當前數據中提取）
      const years = new Set()
      schedules.value.forEach(schedule => {
        if (schedule.year) {
          years.add(schedule.year)
        }
      })
    }
  } catch (error) {
    console.error('載入服事表失敗:', error)
  }
}

const loadPositionConfig = async () => {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/positions/config/full', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 後端返回格式：{ "config": {...}, "message": "..." }
      const config = data.config || data || {}
      
      // 按 sortOrder 排序崗位配置
      const sortedConfig = {}
      const sortedEntries = Object.entries(config).sort((a, b) => {
        const sortOrderA = a[1].sortOrder || 0
        const sortOrderB = b[1].sortOrder || 0
        return sortOrderA - sortOrderB
      })
      
      for (const [posCode, posData] of sortedEntries) {
        sortedConfig[posCode] = posData
      }
      
      positionConfig.value = sortedConfig
    }
  } catch (error) {
    console.error('載入崗位配置失敗:', error)
  }
}

const openAddModal = () => {
  modalMode.value = 'add'
  currentScheduleYear.value = null
  showModal.value = true
}

const editSchedule = (year) => {
  modalMode.value = 'edit'
  currentScheduleYear.value = year
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  currentScheduleYear.value = null
}

const handleSaved = () => {
  loadSchedules()
  closeModal()
}

const handleUpdated = () => {
  loadSchedules()
  closeModal()
}

const deleteSchedule = async (year) => {
  if (!confirm(`確定要刪除 ${year} 年的服事表嗎？`)) {
    return
  }
  
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/service-schedules/${year}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (data !== null) {
      loadSchedules()
    } else {
      toast.error('刪除失敗')
    }
  } catch (error) {
    console.error('刪除服事表失敗:', error)
    toast.error('刪除失敗: ' + error.message)
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  loadSchedules()
  loadPositionConfig()
})
</script>

<style scoped>
.admin-service-schedule{
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
.admin-service-schedule .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-service-schedule .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-service-schedule .page-header p,
.admin-service-schedule .subtitle,
.admin-service-schedule .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-service-schedule .table-container,
.admin-service-schedule .list-container,
.admin-service-schedule .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-service-schedule .table-container{ padding:0; }

/* Inline helpers */
.admin-service-schedule .hint,
.admin-service-schedule .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-service-schedule .actions,
.admin-service-schedule .header-actions{
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
