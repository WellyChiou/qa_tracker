<template>
  <AdminLayout>
    <div class="admin-positions">
      <div class="page-header">
        <h2>崗位管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增崗位</button>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
        <div class="filter-grid">
          <div class="filter-group">
            <label>崗位代碼</label>
            <input
              type="text"
              v-model="filters.positionCode"
              placeholder="輸入崗位代碼"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>崗位名稱</label>
            <input
              type="text"
              v-model="filters.positionName"
              placeholder="輸入崗位名稱"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>是否啟用</label>
            <select v-model="filters.isActive">
              <option value="">全部</option>
              <option :value="true">啟用</option>
              <option :value="false">停用</option>
            </select>
          </div>
          <div class="filter-group">
            <button @click="resetFilters" class="btn btn-secondary">清除條件</button>
          </div>
        </div>
      </section>

      <div class="positions-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ positions.length === 0 ? '尚無崗位資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="positions-table">
          <div class="table-header">
            <h3>崗位列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>崗位代碼</th>
                <th>崗位名稱</th>
                <th>是否啟用</th>
                <th>允許重複</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="position in paginatedList" :key="position.id">
                <td>{{ position.positionCode }}</td>
                <td>{{ position.positionName }}</td>
                <td>{{ position.isActive ? '是' : '否' }}</td>
                <td>{{ position.allowDuplicate ? '是' : '否' }}</td>
                <td>
                  <button @click="editPosition(position.id)" class="btn btn-edit">編輯</button>
                  <button @click="managePositionPersons(position)" class="btn btn-manage">設定人員</button>
                  <button @click="deletePosition(position.id)" class="btn btn-delete">刪除</button>
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

      <!-- 新增崗位 Modal -->
      <PositionManagementModal
        v-if="showModal && !editingPosition"
        :show="showModal"
        @close="closeModal"
        @saved="handleSaved"
      />

      <!-- 編輯崗位 Modal -->
      <EditPositionModal
        v-if="editingPosition"
        :show="!!editingPosition"
        :position="editingPosition"
        @close="closeEditModal"
        @updated="handleUpdated"
      />

      <!-- 設定人員 Modal -->
      <PositionEditModal
        v-if="managingPosition"
        :show="!!managingPosition"
        :position="managingPosition"
        :readonly="false"
        @close="closeManageModal"
        @updated="handlePersonUpdated"
      />
    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import PositionManagementModal from '@/components/PositionManagementModal.vue'
import EditPositionModal from '@/components/EditPositionModal.vue'
import PositionEditModal from '@/components/PositionEditModal.vue'
import { apiRequest } from '@/utils/api'

const positions = ref([])
const showModal = ref(false)
const editingPosition = ref(null)
const managingPosition = ref(null)

// 查詢條件
const filters = ref({
  positionCode: '',
  positionName: '',
  isActive: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...positions.value]
  
  if (filters.value.positionCode) {
    filtered = filtered.filter(position => 
      position.positionCode?.toLowerCase().includes(filters.value.positionCode.toLowerCase())
    )
  }
  
  if (filters.value.positionName) {
    filtered = filtered.filter(position => 
      position.positionName?.toLowerCase().includes(filters.value.positionName.toLowerCase())
    )
  }
  
  if (filters.value.isActive !== '') {
    filtered = filtered.filter(position => position.isActive === filters.value.isActive)
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
    positionCode: '',
    positionName: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => [filters.value.positionCode, filters.value.positionName, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
})

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const loadPositions = async () => {
  try {
    const response = await apiRequest('/church/positions', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      // 後端返回格式：{ "positions": [...], "message": "..." }
      positions.value = data.positions || data || []
    }
  } catch (error) {
    console.error('載入崗位失敗:', error)
  }
}

const openAddModal = () => {
  showModal.value = true
}

const editPosition = (id) => {
  // 找到要編輯的崗位
  const position = positions.value.find(p => p.id === id)
  if (position) {
    editingPosition.value = position
  }
}

const closeEditModal = () => {
  editingPosition.value = null
}

const handleUpdated = () => {
  loadPositions()
  closeEditModal()
}

const managePositionPersons = (position) => {
  managingPosition.value = position
}

const closeManageModal = () => {
  managingPosition.value = null
}

const handlePersonUpdated = () => {
  // 人員更新後不需要重新載入崗位列表，因為只是更新人員關係
  closeManageModal()
}

const closeModal = () => {
  showModal.value = false
}

const handleSaved = () => {
  loadPositions()
  closeModal()
}

const deletePosition = async (id) => {
  if (!confirm('確定要刪除此崗位嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/positions/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadPositions()
    } else {
      toast.error('刪除失敗')
    }
  } catch (error) {
    console.error('刪除崗位失敗:', error)
    toast.error('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadPositions()
})
</script>

<style scoped>
.admin-positions{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-positions .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-positions .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-positions .page-header p,
.admin-positions .subtitle,
.admin-positions .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-positions .table-container,
.admin-positions .list-container,
.admin-positions .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-positions .table-container{ padding:0; }

/* Inline helpers */
.admin-positions .hint,
.admin-positions .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-positions .actions,
.admin-positions .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
