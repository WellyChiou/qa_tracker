<template>
  <AdminLayout>
    <div class="admin-menus">
      <div class="page-header">
        <h2>菜單管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增菜單</button>
      </div>

      <div class="menus-list">
        <div v-if="menus.length === 0" class="empty-state">
          <p>尚無菜單資料</p>
        </div>
        <div v-else class="menus-table">
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
                <td>
                  <button @click="editMenu(menu.id)" class="btn btn-edit">編輯</button>
                  <button @click="deleteMenu(menu.id)" class="btn btn-delete">刪除</button>
                </td>
              </tr>
            </tbody>
          </table>
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
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import MenuModal from '@/components/MenuModal.vue'
import { apiRequest } from '@/utils/api'

const menus = ref([])
const showModal = ref(false)
const selectedMenu = ref(null)

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
      alert('載入菜單資料失敗')
    }
  } catch (error) {
    console.error('載入菜單資料失敗:', error)
    alert('載入菜單資料失敗: ' + error.message)
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
      alert('刪除失敗')
    }
  } catch (error) {
    console.error('刪除菜單失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadMenus()
})
</script>

<style scoped>
.admin-menus {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h2 {
  margin: 0;
  font-size: 1.8rem;
  color: #333;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.menus-list {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.menus-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f5f5f5;
}

th, td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  font-weight: 600;
  color: #333;
}

.status-active {
  color: #10b981;
  font-weight: 600;
}

.status-inactive {
  color: #ef4444;
  font-weight: 600;
}

.btn-edit {
  background: #667eea;
  color: white;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
}

.btn-edit:hover {
  background: #5568d3;
}

.btn-delete {
  background: #ef4444;
  color: white;
  padding: 0.5rem 1rem;
}

.btn-delete:hover {
  background: #dc2626;
}
</style>

