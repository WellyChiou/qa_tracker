<template>
  <AdminLayout>
    <div class="admin-persons">
      <div class="page-header">
        <h2>人員管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增人員</button>
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
            <label>姓名</label>
            <input
              type="text"
              v-model="filters.personName"
              placeholder="輸入姓名"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>電子郵件</label>
            <input
              type="text"
              v-model="filters.email"
              placeholder="輸入電子郵件"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>電話</label>
            <input
              type="text"
              v-model="filters.phone"
              placeholder="輸入電話"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <button @click="resetFilters" class="btn btn-secondary">清除條件</button>
          </div>
        </div>
        </div>
      </details>

      <div class="persons-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ persons.length === 0 ? '尚無人員資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="persons-table">
          <div class="table-header">
            <h3>人員列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>姓名</th>
                <th>顯示名稱</th>
                <th>會員編號</th>
                <th>所屬小組</th>
                <th>電話</th>
                <th>電子郵件</th>
                <th>生日</th>
                <th>建立時間</th>
                <th>更新時間</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="person in persons" :key="person.id">
                <td>{{ person.personName }}</td>
                <td>{{ person.displayName || '-' }}</td>
                <td>{{ person.memberNo || '-' }}</td>
                <td>{{ getGroupNames(person.id) || '-' }}</td>
                <td>{{ person.phone || '-' }}</td>
                <td>{{ person.email || '-' }}</td>
                <td>{{ formatDate(person.birthday) }}</td>
                <td>{{ formatDateTime(person.createdAt) }}</td>
                <td>{{ formatDateTime(person.updatedAt) }}</td>
                <td><div class="table-actions"><button @click="editPerson(person.id)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                  <button @click="deletePerson(person.id)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
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

      <!-- 新增人員 Modal -->
      <PersonManagementModal
        v-if="showModal && !editingPerson"
        :show="showModal"
        @close="closeModal"
        @saved="handleSaved"
      />

      <!-- 編輯人員 Modal -->
      <EditPersonModal
        v-if="editingPerson"
        :show="!!editingPerson"
        :person="editingPerson"
        @close="closeEditModal"
        @updated="handleUpdated"
      />
    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@shared/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import PersonManagementModal from '@/components/PersonManagementModal.vue'
import EditPersonModal from '@/components/EditPersonModal.vue'
import { apiRequest } from '@/utils/api'

const persons = ref([])
const groups = ref([])
const personGroupsMap = ref({}) // 存儲每個人員的小組列表
const showModal = ref(false)
const editingPerson = ref(null)

// 查詢條件
const filters = ref({
  personName: '',
  email: '',
  phone: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)
const totalRecords = ref(0)
const totalPages = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...persons.value]
  
  if (filters.value.personName) {
    filtered = filtered.filter(person => 
      person.personName?.toLowerCase().includes(filters.value.personName.toLowerCase())
    )
  }
  
  if (filters.value.email) {
    filtered = filtered.filter(person => 
      (person.email || '').toLowerCase().includes(filters.value.email.toLowerCase())
    )
  }
  
  if (filters.value.phone) {
    filtered = filtered.filter(person => 
      (person.phone || '').includes(filters.value.phone)
    )
  }
  
  return filtered
})

// 注意：分頁現在由後端處理，但前端過濾仍然保留（過濾當前頁數據）

// 第一頁
const firstPage = () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadPersons()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadPersons()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadPersons()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadPersons()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadPersons()
  } else {
    jumpPage.value = currentPage.value
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    personName: '',
    email: '',
    phone: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => [filters.value.personName, filters.value.email, filters.value.phone], () => {
  currentPage.value = 1
  jumpPage.value = 1
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadPersons()
})

const loadPersons = async () => {
  try {
    const url = `/church/persons?page=${currentPage.value - 1}&size=${recordsPerPage.value}`
    const response = await apiRequest(url, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      // 後端返回格式：{ "persons": [...], "message": "..." }
      persons.value = data.persons || data.content || data || []
      
      // 更新分頁信息
      if (data.totalElements !== undefined) {
        totalRecords.value = data.totalElements
        totalPages.value = data.totalPages || 1
        // 確保 currentPage 不超過 totalPages
        if (currentPage.value > totalPages.value) {
          currentPage.value = totalPages.value
          jumpPage.value = totalPages.value
        }
        // 同步 jumpPage 與 currentPage
        jumpPage.value = currentPage.value
      } else {
        totalRecords.value = persons.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
      }
      
      // 載入所有人員的小組列表
      await loadAllPersonGroups()
    }
  } catch (error) {
    console.error('載入人員失敗:', error)
  }
}

const loadGroups = async () => {
  try {
    const response = await apiRequest('/church/groups', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      groups.value = data.groups || []
    }
  } catch (error) {
    console.error('載入小組失敗:', error)
  }
}

const getGroupNames = (personId) => {
  if (!personId) return null
  const personGroups = personGroupsMap.value[personId] || []
  if (personGroups.length === 0) return null
  // 只顯示活躍的小組
  const activeGroups = personGroups.filter(g => g.isActive)
  if (activeGroups.length === 0) return null
  return activeGroups.map(g => {
    const group = groups.value.find(gr => gr.id === g.groupId)
    return group ? group.groupName : null
  }).filter(Boolean).join(', ')
}

const loadPersonGroups = async (personId) => {
  try {
    const response = await apiRequest(`/church/persons/${personId}/groups`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      personGroupsMap.value[personId] = data.groups || []
    }
  } catch (error) {
    console.error(`載入人員 ${personId} 的小組列表失敗:`, error)
  }
}

const loadAllPersonGroups = async () => {
  // 批量載入所有人員的小組列表
  if (persons.value.length === 0) {
    return
  }
  
  try {
    const personIds = persons.value.map(person => person.id)
    const response = await apiRequest('/church/persons/groups/batch', {
      method: 'POST',
      body: JSON.stringify({ personIds }),
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      const personGroups = data.personGroups || {}
      
      // 將批量查詢結果存入 personGroupsMap
      // 注意：JSON 中的數字 key 會被轉換為字符串，需要轉換回數字
      for (const [personIdStr, groups] of Object.entries(personGroups)) {
        const personId = typeof personIdStr === 'string' ? parseInt(personIdStr, 10) : personIdStr
        personGroupsMap.value[personId] = Array.isArray(groups) ? groups : []
      }
    }
  } catch (error) {
    console.error('批量載入人員小組列表失敗:', error)
    // 如果批量查詢失敗，回退到逐一查詢
    const promises = persons.value.map(person => loadPersonGroups(person.id))
    await Promise.all(promises)
  }
}

const openAddModal = () => {
  showModal.value = true
}

const editPerson = (id) => {
  // 找到要編輯的人員
  const person = persons.value.find(p => p.id === id)
  if (person) {
    editingPerson.value = person
  }
}

const closeEditModal = () => {
  editingPerson.value = null
}

const handleUpdated = () => {
  loadPersons()
  closeEditModal()
  // 清除該人員的小組緩存，下次載入時會重新獲取
  if (editingPerson.value?.id) {
    delete personGroupsMap.value[editingPerson.value.id]
  }
}

const closeModal = () => {
  showModal.value = false
}

const handleSaved = () => {
  loadPersons()
  closeModal()
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '-'
  const date = new Date(dateTimeStr)
  if (isNaN(date.getTime())) return dateTimeStr
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const deletePerson = async (id) => {
  if (!confirm('確定要刪除此人員嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/persons/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadPersons()
    } else {
      toast.error('刪除失敗')
    }
  } catch (error) {
    console.error('刪除人員失敗:', error)
    toast.error('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadPersons()
  loadGroups()
})
</script>

<style scoped>
.admin-persons{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-persons .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-persons .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-persons .page-header p,
.admin-persons .subtitle,
.admin-persons .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-persons .table-container,
.admin-persons .list-container,
.admin-persons .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-persons .table-container{ padding:0; }

/* Inline helpers */
.admin-persons .hint,
.admin-persons .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-persons .actions,
.admin-persons .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
