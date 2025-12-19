<template>
  <AdminLayout>
    <div class="admin-menus">
      <div class="page-header">
        <h2>菜單管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增菜單</button>
      </div>

      <!-- 查詢條件 -->
      <section class="filters">
        <h3>查詢條件</h3>
        <div class="filter-grid">
          <div class="filter-group">
            <label>菜單代碼</label>
            <input
              type="text"
              v-model="filters.menuCode"
              placeholder="輸入菜單代碼"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>菜單名稱</label>
            <input
              type="text"
              v-model="filters.menuName"
              placeholder="輸入菜單名稱"
              class="form-input"
            />
          </div>
          <div class="filter-group">
            <label>類型</label>
            <select v-model="filters.menuType">
              <option value="">全部</option>
              <option value="frontend">前台</option>
              <option value="admin">後台</option>
            </select>
          </div>
          <div class="filter-group">
            <label>狀態</label>
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

      <div class="menus-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ menus.length === 0 ? '尚無菜單資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="menus-table">
          <div class="table-header">
            <h3>菜單列表 (共 {{ filteredList.length }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>菜單代碼</th>
                <th>菜單名稱</th>
                <th>類型</th>
                <th>URL</th>
                <th>排序</th>
                <th>狀態</th>
                <th>儀表板</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="menu in paginatedList" :key="menu.id">
                <td>{{ menu.menuCode }}</td>
                <td>{{ menu.menuName }}</td>
                <td>{{ menu.menuType === 'frontend' ? '前台' : '後台' }}</td>
                <td>{{ menu.url || '-' }}</td>
                <td>{{ menu.orderIndex }}</td>
                <td>
                  <span :class="menu.isActive ? 'status-active' : 'status-inactive'">
                    {{ menu.isActive ? '啟用' : '停用' }}
                  </span>
                </td>
                <td>
                  <span v-if="menu.menuType === 'admin' && !menu.parentId" 
                        :class="menu.showInDashboard ? 'status-active' : 'status-inactive'">
                    {{ menu.showInDashboard ? '顯示' : '隱藏' }}
                  </span>
                  <span v-else class="status-inactive">-</span>
                </td>
                <td>
                  <button @click="editMenu(menu.id)" class="btn btn-edit">編輯</button>
                  <button @click="deleteMenu(menu.id)" class="btn btn-delete">刪除</button>
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
    </div>
    
    <MenuModal
      :show="showModal"
      :menu="selectedMenu"
      :available-menus="menus"
      @close="closeModal"
      @saved="handleSaved"
    />
  </AdminLayout>
</template>

<script setup>
import { toast } from '@/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import MenuModal from '@/components/MenuModal.vue'
import { apiRequest } from '@/utils/api'

const menus = ref([])
const showModal = ref(false)
const selectedMenu = ref(null)

// 查詢條件
const filters = ref({
  menuCode: '',
  menuName: '',
  menuType: '',
  isActive: ''
})

// 分頁
const currentPage = ref(1)
const recordsPerPage = ref(20)
const jumpPage = ref(1)

// 過濾後的列表
const filteredList = computed(() => {
  let filtered = [...menus.value]
  
  if (filters.value.menuCode) {
    filtered = filtered.filter(menu => 
      menu.menuCode?.toLowerCase().includes(filters.value.menuCode.toLowerCase())
    )
  }
  
  if (filters.value.menuName) {
    filtered = filtered.filter(menu => 
      menu.menuName?.toLowerCase().includes(filters.value.menuName.toLowerCase())
    )
  }
  
  if (filters.value.menuType) {
    filtered = filtered.filter(menu => menu.menuType === filters.value.menuType)
  }
  
  if (filters.value.isActive !== '') {
    filtered = filtered.filter(menu => menu.isActive === filters.value.isActive)
  }
  
  return filtered.sort((a, b) => (a.orderIndex || 0) - (b.orderIndex || 0))
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
    menuCode: '',
    menuName: '',
    menuType: '',
    isActive: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 監聽查詢條件變化，重置到第一頁
watch(() => [filters.value.menuCode, filters.value.menuName, filters.value.menuType, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
})

// 監聽每頁筆數變化，重置到第一頁
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
})

const loadMenus = async () => {
  try {
    const response = await apiRequest('/church/menus', {
      method: 'GET'
    }, '載入菜單中...', true)
    
    if (response.ok) {
      const data = await response.json()
      menus.value = data || []
    }
  } catch (error) {
    console.error('載入菜單失敗:', error)
  }
}

const openAddModal = () => {
  selectedMenu.value = null
  showModal.value = true
}

const editMenu = async (id) => {
  try {
    const response = await apiRequest(`/church/menus/${id}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      selectedMenu.value = data.data || data.menu || data
      showModal.value = true
    } else {
      toast.error('載入菜單資料失敗')
    }
  } catch (error) {
    console.error('載入菜單資料失敗:', error)
    toast.error('載入菜單資料失敗: ' + error.message)
  }
}

const closeModal = () => {
  showModal.value = false
  selectedMenu.value = null
}

const handleSaved = () => {
  loadMenus()
}

const deleteMenu = async (id) => {
  if (!confirm('確定要刪除此菜單嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/menus/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadMenus()
    } else {
      toast.error('刪除失敗')
    }
  } catch (error) {
    console.error('刪除菜單失敗:', error)
    toast.error('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadMenus()
})
</script>

<style scoped>
.admin-menus{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-menus .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-menus .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-menus .page-header p,
.admin-menus .subtitle,
.admin-menus .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-menus .table-container,
.admin-menus .list-container,
.admin-menus .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-menus .table-container{ padding:0; }

/* Inline helpers */
.admin-menus .hint,
.admin-menus .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-menus .actions,
.admin-menus .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}
</style>
