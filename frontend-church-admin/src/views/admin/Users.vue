<template>
  <AdminLayout>
    <div class="admin-users">
      <div class="page-header">
        <h2>用戶管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增用戶</button>
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
            <label>用戶名</label>
            <input
              type="text"
              v-model="filters.username"
              placeholder="輸入用戶名"
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
            <label>角色</label>
            <select v-model="filters.roleId">
              <option value="">全部</option>
              <option v-for="role in availableRoles" :key="role.id" :value="role.id">
                {{ role.roleName }}
              </option>
            </select>
          </div>
          <div class="filter-group">
            <label>狀態</label>
            <select v-model="filters.isEnabled">
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

      <div class="users-list">
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ users.length === 0 ? '尚無用戶資料' : '沒有符合條件的資料' }}</p>
        </div>
        <div v-else class="users-table">
          <div class="table-header">
            <h3>用戶列表 (共 {{ totalRecords }} 筆)</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>用戶名</th>
                <th>顯示名稱</th>
                <th>電子郵件</th>
                <th>角色</th>
                <th>狀態</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.uid">
                <td>{{ user.username }}</td>
                <td>{{ user.displayName || '-' }}</td>
                <td>{{ user.email || '-' }}</td>
                <td>
                  <span v-for="role in user.roles" :key="role.id" class="role-badge">
                    {{ role.roleName }}
                  </span>
                  <span v-if="!user.roles || user.roles.length === 0">-</span>
                </td>
                <td>
                  <span :class="user.isEnabled ? 'status-active' : 'status-inactive'">
                    {{ user.isEnabled ? '啟用' : '停用' }}
                  </span>
                </td>
                <td><div class="table-actions"><button @click="editUser(user.uid)" class="btn btn-edit"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z"/></svg></span><span>編輯</span></button>
                  <button @click="editRoles(user)" class="btn btn-roles">角色</button>
                  <button @click="editPermissions(user)" class="btn btn-permissions">權限</button>
                  <button @click="deleteUser(user.uid)" class="btn btn-delete"><span class="btn__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg></span><span>刪除</span></button></div></td>
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
    
    <UserModal
      :show="showModal"
      :user="selectedUser"
      @close="closeModal"
      @saved="handleSaved"
    />
    
    <!-- 角色分配模態框 -->
    <div v-if="showRolesModal" class="modal-overlay" @click="closeRolesModal">
      <div class="modal-panel" @click.stop style="max-width: 600px;">
        <div class="modal-header">
          <h2 class="modal-title">分配角色: {{ selectedUser?.displayName || selectedUser?.username }}</h2>
          <button class="btn-close" @click="closeRolesModal">×</button>
        </div>
        <div class="modal-body">
          <div class="transfer">
            <div class="transfer-col">
              <div class="transfer-head">
                <span>未加入</span>
                <input v-model="roleSearchLeft" class="transfer-search" placeholder="搜尋角色…" />
              </div>
              <div class="transfer-list">
                <div v-for="r in filteredAvailableRoles" :key="r.id" class="transfer-item" @click="addRole(r.id)">
                  <div class="transfer-item-main">
                    <div class="transfer-title">{{ r.roleName }}</div>
                    <div class="transfer-sub" v-if="r.description">{{ r.description }}</div>
                  </div>
                </div>
                <div v-if="filteredAvailableRoles.length === 0" class="transfer-empty">沒有可加入的角色</div>
              </div>
              <div class="transfer-actions">
                <button type="button" class="btn-action-full" @click="addAllRoles" :disabled="filteredAvailableRoles.length === 0">
                  全部加入
                </button>
              </div>
            </div>

            <div class="transfer-col">
              <div class="transfer-head">
                <span>已加入</span>
                <input v-model="roleSearchRight" class="transfer-search" placeholder="搜尋已加入…" />
              </div>
              <div class="transfer-list">
                <div v-for="r in filteredSelectedRoles" :key="r.id" class="transfer-item" @click="removeRole(r.id)">
                  <div class="transfer-item-main">
                    <div class="transfer-title">{{ r.roleName }}</div>
                    <div class="transfer-sub" v-if="r.description">{{ r.description }}</div>
                  </div>
                </div>
                <div v-if="filteredSelectedRoles.length === 0" class="transfer-empty">尚未加入任何角色</div>
              </div>
              <div class="transfer-actions">
                <button type="button" class="btn-action-full" @click="removeAllRoles" :disabled="selectedRoles.length === 0">
                  全部移除
                </button>
              </div>
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-primary" @click="saveRoles">儲存角色</button>
            <button type="button" class="btn btn-secondary" @click="closeRolesModal">取消</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 權限分配模態框 -->
    <div v-if="showPermissionsModal" class="modal-overlay" @click="closePermissionsModal">
      <div class="modal-panel" @click.stop style="max-width: 700px;">
        <div class="modal-header">
          <h2 class="modal-title">分配權限: {{ selectedUser?.displayName || selectedUser?.username }}</h2>
          <button class="btn-close" @click="closePermissionsModal">×</button>
        </div>
        <div class="modal-body">
          <div class="transfer">
            <div class="transfer-col">
              <div class="transfer-head">
                <span>未加入</span>
                <input v-model="permSearchLeft" class="transfer-search" placeholder="搜尋權限…" />
              </div>
              <div class="transfer-list">
                <div v-for="p in filteredAvailablePerms" :key="p.id" class="transfer-item" @click="addPerm(p.id)">
                  <div class="transfer-item-main">
                    <div class="transfer-title">{{ p.permissionName }}</div>
                    <div class="transfer-sub"><code class="permission-code">{{ p.permissionCode }}</code></div>
                  </div>
                </div>
                <div v-if="filteredAvailablePerms.length === 0" class="transfer-empty">沒有可加入的權限</div>
              </div>
              <div class="transfer-actions">
                <button type="button" class="btn-action-full" @click="addAllPerms" :disabled="filteredAvailablePerms.length === 0">
                  全部加入
                </button>
              </div>
            </div>

            <div class="transfer-col">
              <div class="transfer-head">
                <span>已加入</span>
                <input v-model="permSearchRight" class="transfer-search" placeholder="搜尋已加入…" />
              </div>
              <div class="transfer-list">
                <div v-for="p in filteredSelectedPerms" :key="p.id" class="transfer-item" @click="removePerm(p.id)">
                  <div class="transfer-item-main">
                    <div class="transfer-title">{{ p.permissionName }}</div>
                    <div class="transfer-sub"><code class="permission-code">{{ p.permissionCode }}</code></div>
                  </div>
                </div>
                <div v-if="filteredSelectedPerms.length === 0" class="transfer-empty">尚未加入任何權限</div>
              </div>
              <div class="transfer-actions">
                <button type="button" class="btn-action-full" @click="removeAllPerms" :disabled="selectedPerms.length === 0">
                  全部移除
                </button>
              </div>
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-primary" @click="savePermissions">儲存權限</button>
            <button type="button" class="btn btn-secondary" @click="closePermissionsModal">取消</button>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@shared/composables/useToast'
import { ref, computed, onMounted, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import UserModal from '@/components/UserModal.vue'
import { apiRequest } from '@/utils/api'

const users = ref([])
const availableRoles = ref([])
const availablePermissions = ref([])
const showModal = ref(false)
const selectedUser = ref(null)
const showRolesModal = ref(false)
const showPermissionsModal = ref(false)
const selectedRoleIds = ref([])
const roleSearchLeft = ref('')
const roleSearchRight = ref('')

const selectedRoleSet = computed(() => new Set(selectedRoleIds.value || []))
const unselectedRoles = computed(() => {
  if (!Array.isArray(availableRoles.value)) return []
  return availableRoles.value.filter(r => r && r.id != null && !selectedRoleSet.value.has(r.id))
})
const selectedRoles = computed(() => {
  if (!Array.isArray(availableRoles.value)) return []
  return availableRoles.value.filter(r => r && r.id != null && selectedRoleSet.value.has(r.id))
})

const filteredAvailableRoles = computed(() => {
  const q = roleSearchLeft.value.trim().toLowerCase()
  if (!q) return unselectedRoles.value
  return unselectedRoles.value.filter(r => `${r.roleName} ${r.description || ''}`.toLowerCase().includes(q))
})

const filteredSelectedRoles = computed(() => {
  const q = roleSearchRight.value.trim().toLowerCase()
  if (!q) return selectedRoles.value
  return selectedRoles.value.filter(r => `${r.roleName} ${r.description || ''}`.toLowerCase().includes(q))
})

const addRole = (id) => {
  const s = new Set(selectedRoleIds.value || [])
  s.add(id)
  selectedRoleIds.value = Array.from(s)
}

const removeRole = (id) => {
  selectedRoleIds.value = (selectedRoleIds.value || []).filter(x => x !== id)
}
const selectedPermissionIds = ref([])

// permissions transfer
const permSearchLeft = ref('')
const permSearchRight = ref('')

const selectedPermSet = computed(() => new Set(selectedPermissionIds.value || []))
const unselectedPerms = computed(() => {
  if (!Array.isArray(availablePermissions.value)) return []
  return availablePermissions.value.filter(p => p && p.id != null && !selectedPermSet.value.has(p.id))
})
const selectedPerms = computed(() => {
  if (!Array.isArray(availablePermissions.value)) return []
  return availablePermissions.value.filter(p => p && p.id != null && selectedPermSet.value.has(p.id))
})

const filteredAvailablePerms = computed(() => {
  const q = permSearchLeft.value.trim().toLowerCase()
  if (!q) return unselectedPerms.value
  return unselectedPerms.value.filter(p => `${p.permissionName} ${p.permissionCode}`.toLowerCase().includes(q))
})

const filteredSelectedPerms = computed(() => {
  const q = permSearchRight.value.trim().toLowerCase()
  if (!q) return selectedPerms.value
  return selectedPerms.value.filter(p => `${p.permissionName} ${p.permissionCode}`.toLowerCase().includes(q))
})

const addPerm = (id) => {
  const s = new Set(selectedPermissionIds.value || [])
  s.add(id)
  selectedPermissionIds.value = Array.from(s)
}

const removePerm = (id) => {
  selectedPermissionIds.value = (selectedPermissionIds.value || []).filter(x => x !== id)
}

const addAllRoles = () => {
  const ids = filteredAvailableRoles.value.map(r => r?.id).filter(id => id != null)
  const s = new Set(selectedRoleIds.value || [])
  ids.forEach(id => s.add(id))
  selectedRoleIds.value = Array.from(s)
}

const removeAllRoles = () => {
  selectedRoleIds.value = []
}

const addAllPerms = () => {
  const ids = filteredAvailablePerms.value.map(p => p?.id).filter(id => id != null)
  const s = new Set(selectedPermissionIds.value || [])
  ids.forEach(id => s.add(id))
  selectedPermissionIds.value = Array.from(s)
}

const removeAllPerms = () => {
  selectedPermissionIds.value = []
}


// 查詢條件
const filters = ref({
  username: '',
  email: '',
  roleId: '',
  isEnabled: ''
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
  loadUsers()
}

// 上一頁
const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    jumpPage.value = currentPage.value
    loadUsers()
  }
}

// 下一頁
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    jumpPage.value = currentPage.value
    loadUsers()
  }
}

// 最後一頁
const lastPage = () => {
  currentPage.value = totalPages.value
  jumpPage.value = totalPages.value
  loadUsers()
}

// 跳轉到指定頁
const jumpToPage = () => {
  const targetPage = Number(jumpPage.value)
  if (targetPage >= 1 && targetPage <= totalPages.value && !isNaN(targetPage)) {
    currentPage.value = targetPage
    jumpPage.value = targetPage
    loadUsers()
  } else {
    jumpPage.value = currentPage.value
  }
}

// 重置查詢條件
const resetFilters = () => {
  filters.value = {
    username: '',
    email: '',
    roleId: '',
    isEnabled: ''
  }
  currentPage.value = 1
  jumpPage.value = 1
}

// 過濾後的用戶列表（用於顯示）
const filteredList = computed(() => {
  if (!users.value || !Array.isArray(users.value)) {
    return []
  }
  return users.value
})

// 監聽查詢條件變化，重置到第一頁並重新載入
watch(() => [filters.value.username, filters.value.email, filters.value.roleId, filters.value.isEnabled], () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadUsers()
})

// 監聽每頁筆數變化，重置到第一頁並重新載入
watch(recordsPerPage, () => {
  currentPage.value = 1
  jumpPage.value = 1
  loadUsers()
})

const loadUsers = async () => {
  try {
    // 構建查詢參數
    const params = new URLSearchParams({
      page: (currentPage.value - 1).toString(),
      size: recordsPerPage.value.toString()
    })
    
    if (filters.value.username) {
      params.append('username', filters.value.username)
    }
    if (filters.value.email) {
      params.append('email', filters.value.email)
    }
    if (filters.value.roleId) {
      params.append('roleId', filters.value.roleId)
    }
    if (filters.value.isEnabled !== '') {
      params.append('isEnabled', filters.value.isEnabled.toString())
    }
    
    const url = `/church/admin/users?${params.toString()}`
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(url, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // apiRequest 已經提取了 ApiResponse.data，所以 data 可能是：
      // 1. PageResponse 對象（有 content 字段）
      // 2. 直接數組
      // 3. 其他格式
      const usersData = data.content || (Array.isArray(data) ? data : [])
      users.value = Array.isArray(usersData) ? usersData : []
      // 更新分頁信息（PageResponse 格式）
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
        totalRecords.value = users.value.length
        totalPages.value = 1
        currentPage.value = 1
        jumpPage.value = 1
      }
      toast.success(`載入成功，共 ${totalRecords.value} 位用戶`)
    } else {
      toast.error('載入用戶失敗')
    }
  } catch (error) {
    console.error('載入用戶失敗:', error)
    toast.error('載入用戶失敗: ' + (error.message || '未知錯誤'))
  }
}

const loadRoles = async () => {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/admin/roles', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 處理 PageResponse 格式或直接列表
      const rolesData = data.content || data.roles || data || []
      availableRoles.value = Array.isArray(rolesData) ? rolesData : []
    } else {
      toast.error('載入角色失敗')
    }
  } catch (error) {
    console.error('載入角色失敗:', error)
    toast.error('載入角色失敗: ' + (error.message || '未知錯誤'))
  }
}

const loadPermissions = async () => {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest('/church/admin/permissions', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      // 處理 PageResponse 格式或直接列表
      const permissionsData = data.content || data.permissions || data || []
      availablePermissions.value = Array.isArray(permissionsData) ? permissionsData : []
    } else {
      toast.error('載入權限失敗')
    }
  } catch (error) {
    console.error('載入權限失敗:', error)
    toast.error('載入權限失敗: ' + (error.message || '未知錯誤'))
  }
}

const openAddModal = () => {
  selectedUser.value = null
  showModal.value = true
}

const editUser = async (uid) => {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/users/${uid}`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (data) {
      selectedUser.value = data.user || data
      showModal.value = true
    } else {
      toast.error('載入用戶資料失敗')
    }
  } catch (error) {
    console.error('載入用戶資料失敗:', error)
    toast.error('載入用戶資料失敗: ' + error.message)
  }
}

const closeModal = () => {
  showModal.value = false
  selectedUser.value = null
}

const handleSaved = () => {
  loadUsers()
}

const editRoles = async (user) => {
  try {
    selectedUser.value = user
    // 確保 roles 是數組，並正確提取 ID
    if (user.roles && Array.isArray(user.roles)) {
      selectedRoleIds.value = user.roles.map(r => r.id || r).filter(id => id != null)
    } else {
      selectedRoleIds.value = []
    }
    // 重新載入角色列表以確保數據是最新的
    await loadRoles()
    showRolesModal.value = true
  } catch (error) {
    console.error('打開角色編輯失敗:', error)
    toast.error('打開角色編輯失敗: ' + error.message)
  }
}

const closeRolesModal = () => {
  showRolesModal.value = false
  selectedUser.value = null
  selectedRoleIds.value = []
}

const saveRoles = async () => {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/users/${selectedUser.value.uid}/roles`, {
      method: 'POST',
      body: JSON.stringify({ roleIds: selectedRoleIds.value }),
      credentials: 'include'
    })
    
    if (data !== null) {
      // apiRequest 成功返回數據，表示更新成功
      closeRolesModal()
      loadUsers()
      toast.success('角色更新成功')
    } else {
      toast.error('更新角色失敗')
    }
  } catch (error) {
    console.error('更新角色失敗:', error)
    toast.error('更新角色失敗: ' + error.message)
  }
}

const editPermissions = async (user) => {
  try {
    selectedUser.value = user
    // 確保 permissions 是數組，並正確提取 ID
    if (user.permissions && Array.isArray(user.permissions)) {
      selectedPermissionIds.value = user.permissions.map(p => p.id || p).filter(id => id != null)
    } else {
      selectedPermissionIds.value = []
    }
    permSearchLeft.value = ''
    permSearchRight.value = ''
    // 重新載入權限列表以確保數據是最新的
    await loadPermissions()
    showPermissionsModal.value = true
  } catch (error) {
    console.error('打開權限編輯失敗:', error)
    toast.error('打開權限編輯失敗: ' + error.message)
  }
}

const closePermissionsModal = () => {
  showPermissionsModal.value = false
  selectedUser.value = null
  selectedPermissionIds.value = []
  permSearchLeft.value = ''
  permSearchRight.value = ''
}

const savePermissions = async () => {
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/users/${selectedUser.value.uid}/permissions`, {
      method: 'POST',
      body: JSON.stringify({ permissionIds: selectedPermissionIds.value }),
      credentials: 'include'
    })
    
    if (data !== null) {
      // apiRequest 成功返回數據，表示更新成功
      closePermissionsModal()
      loadUsers()
      toast.success('權限更新成功')
    } else {
      toast.error('更新權限失敗')
    }
  } catch (error) {
    console.error('更新權限失敗:', error)
    toast.error('更新權限失敗: ' + error.message)
  }
}

const deleteUser = async (uid) => {
  if (!confirm('確定要刪除此用戶嗎？')) {
    return
  }
  
  try {
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/admin/users/${uid}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (data !== null) {
      loadUsers()
    } else {
      toast.error('刪除失敗')
    }
  } catch (error) {
    console.error('刪除用戶失敗:', error)
    toast.error('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadUsers()
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.admin-users{
  display:flex;
  flex-direction:column;
  gap:14px;
}

/* Header */
.admin-users .page-header{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  flex-wrap:wrap;
  margin-bottom:2px;
}
.admin-users .page-header h2{
  font-size:22px;
  font-weight:900;
  letter-spacing:-0.02em;
}
.admin-users .page-header p,
.admin-users .subtitle,
.admin-users .description{
  color:var(--muted);
  font-weight:700;
  font-size:14px;
  margin-top:6px;
}
/* Lists / table wrap */
.admin-users .table-container,
.admin-users .list-container,
.admin-users .data-container{
  border:1px solid var(--border);
  border-radius:var(--radius);
  overflow:auto;
  background:var(--surface);
  box-shadow:var(--shadow-sm);
}
.admin-users .table-container{ padding:0; }

/* Inline helpers */
.admin-users .hint,
.admin-users .muted{
  color:var(--muted);
  font-size:13px;
  font-weight:700;
}

.admin-users .actions,
.admin-users .header-actions{
  display:flex;
  gap:10px;
  flex-wrap:wrap;
}

/* Mobile tweaks */
@media (max-width: 640px){
}

.transfer{
  display:grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.transfer-col{
  border:1px solid rgba(2,6,23,.08);
  background: rgba(255,255,255,.7);
  border-radius: 14px;
  overflow:hidden;
}
.transfer-head{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:10px;
  padding:10px 12px;
  border-bottom:1px solid rgba(2,6,23,.06);
  background: rgba(2,6,23,.02);
  font-weight: 900;
}
.transfer-search{
  width: 52%;
  min-width: 160px;
  padding: 8px 10px;
  border-radius: 10px;
  border:1px solid rgba(2,6,23,.10);
  background: #fff;
  font-weight: 700;
  outline: none;
}
.transfer-list{
  max-height: 320px;
  overflow:auto;
  padding: 10px;
}
.transfer-item{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:10px;
  padding:10px;
  border:1px solid rgba(2,6,23,.08);
  border-radius: 12px;
  background:#fff;
  margin-bottom: 10px;
  cursor: pointer;
  transition: background 0.2s;
}
.transfer-item:hover{
  background:#f0f0f0;
}
.transfer-item-main{ min-width:0; flex: 1; }
.transfer-title{ font-weight: 900; color:#0f172a; line-height:1.2; }
.transfer-sub{ margin-top:4px; color:#64748b; font-weight:700; font-size:12px; }
.transfer-actions{
  padding: 8px;
  border-top: 1px solid rgba(2,6,23,.08);
}
.btn-action-full{
  width: 100%;
  padding: 8px 16px;
  border: 1px solid rgba(2,6,23,.12);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  font-weight: 600;
}
.btn-action-full:hover:not(:disabled){
  background: #f0f0f0;
  border-color: rgba(2,6,23,.2);
}
.btn-action-full:disabled{
  opacity: 0.5;
  cursor: not-allowed;
}
.transfer-empty{ padding: 14px 8px; text-align:center; color:#94a3b8; font-weight: 800; }
@media (max-width: 980px){
  .transfer{ grid-template-columns: 1fr; }
  .transfer-search{ width: 100%; }
}


/* Table column widths */
:deep(.table){ table-layout: fixed; width: 100%; }
:deep(.table th.col-actions), :deep(.table td.col-actions){ width: 280px; min-width: 280px; }

</style>