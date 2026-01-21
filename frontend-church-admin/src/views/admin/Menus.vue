<template>
  <AdminLayout>
    <div class="admin-menus">
      <div class="page-header">
        <h2>菜單管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增菜單</button>
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
        </div>
      </details>

      <div class="menus-list">
        <div v-if="menus.length === 0" class="empty-state">
          <p>尚無菜單資料</p>
        </div>
        <div v-else class="menus-table">
          <div class="table-header">
            <h3>菜單列表 (共 {{ totalRecords }} 筆)</h3>
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
              <tr v-for="menu in menus" :key="menu.id">
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
                <td><div class="table-actions"><button @click="editMenu(menu.id)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                  <button @click="deleteMenu(menu.id)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
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
import { toast } from '@shared/composables/useToast'
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
const totalRecords = ref(0)
const totalPages = ref(1)

// 第一頁
const firstPage = () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadMenus()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadMenus()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadMenus()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadMenus()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadMenus()
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
  loadMenus()
}

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => [filters.value.menuCode, filters.value.menuName, filters.value.menuType, filters.value.isActive], () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadMenus()
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadMenus()
})

const loadMenus = async () => {
  try {
    const params = new URLSearchParams()
    if (filters.value.menuCode) {
      params.append('menuCode', filters.value.menuCode)
    }
    if (filters.value.menuName) {
      params.append('menuName', filters.value.menuName)
    }
    if (filters.value.menuType) {
      params.append('menuType', filters.value.menuType)
    }
    if (filters.value.isActive !== '') {
      params.append('isActive', filters.value.isActive === true || filters.value.isActive === 'true')
    }
    params.append('page', (currentPage.value - 1).toString())
    params.append('size', recordsPerPage.value.toString())
    
    const response = await apiRequest(`/church/menus?${params.toString()}`, {
      method: 'GET'
    }, '載入菜單中...', true)
    
    if (response.ok) {
      const data = await response.json()
      menus.value = data.content || data || []
      
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
        totalRecords.value = menus.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
      }
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
