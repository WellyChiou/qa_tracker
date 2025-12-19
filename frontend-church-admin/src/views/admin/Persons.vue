<template>
  <AdminLayout>
    <div class="admin-persons">
      <div class="page-header">
        <h2>人員管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增人員</button>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
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
      </section>

      <div class="persons-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ persons.length === 0 ? '尚無人員資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="persons-table">
          <div class="table-header">
            <h3>人員列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>姓名</th>
                <th>顯示名稱</th>
                <th>電話</th>
                <th>電子郵件</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="person in paginatedList" :key="person.id">
                <td>{{ person.personName }}</td>
                <td>{{ person.displayName || '-' }}</td>
                <td>{{ person.phone || '-' }}</td>
                <td>{{ person.email || '-' }}</td>
                <td>
                  <button @click="editPerson(person.id)" class="btn btn-edit">編輯</button>
                  <button @click="deletePerson(person.id)" class="btn btn-delete">刪除</button>
                </td>
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
              <span class="pagination-info">共 {{ filteredList.length }} 筆 (第 {{ currentPage }}/{{ totalPages }} 頁)</span>
            </div>
            <div class="pagination-right">
              <button class="btn-secondary" @click="currentPage--" :disabled="currentPage === 1">
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
              <button class="btn-secondary" @click="currentPage++" :disabled="currentPage === totalPages">
                下一頁
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
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
import { toast } from '@/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import PersonManagementModal from '@/components/PersonManagementModal.vue'
import EditPersonModal from '@/components/EditPersonModal.vue'
import { apiRequest } from '@/utils/api'

const persons = ref([])
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

// 分頁後的列表
const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * recordsPerPage.value
  return filteredList.value.slice(start, start + recordsPerPage.value)
})

// 總頁數
const totalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredList.value.length / recordsPerPage.value))
})

// 跳轉到指定頁
const jumpToPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    currentPage.value = jumpPage.value
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

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const loadPersons = async () => {
  try {
    const response = await apiRequest('/church/persons', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      // 後端返回格式：{ "persons": [...], "message": "..." }
      persons.value = data.persons || data || []
    }
  } catch (error) {
    console.error('載入人員失敗:', error)
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
}

const closeModal = () => {
  showModal.value = false
}

const handleSaved = () => {
  loadPersons()
  closeModal()
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
