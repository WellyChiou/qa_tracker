<template>
  <div v-if="show" class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <h3>{{ isEdit ? '編輯 URL 權限' : '新增 URL 權限' }}</h3>
        <button @click="close" class="close-btn">×</button>
      </div>
      
      <form @submit.prevent="handleSubmit" class="modal-body">
        <div class="form-group">
          <label>URL 模式 <span class="required">*</span></label>
          <input
            v-model="formData.urlPattern"
            type="text"
            required
            placeholder="例如: /api/church/menus/frontend 或 /api/church/positions/*/persons"
          />
          <small class="form-hint">支持通配符：* 表示單層路徑（如 /api/church/positions/*/persons），** 表示多層路徑（如 /api/church/positions/**）</small>
        </div>
        
        <div class="form-group">
          <label>HTTP 方法</label>
          <select v-model="formData.httpMethod" class="form-select">
            <option value="">全部方法</option>
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PUT">PUT</option>
            <option value="DELETE">DELETE</option>
            <option value="PATCH">PATCH</option>
          </select>
          <small class="form-hint">留空表示所有 HTTP 方法</small>
        </div>
        
        <div class="form-group">
          <label class="checkbox-label">
            <input
              type="checkbox"
              v-model="formData.isPublic"
            />
            <span>是否公開（無需登入即可訪問）</span>
          </label>
          <small class="form-hint">勾選後，此 URL 將無需登入即可訪問</small>
        </div>
        
        <div class="form-row">
          <div class="form-group">
            <label>需要角色</label>
            <select v-model="formData.requiredRole" class="form-select">
              <option value="">無需特定角色</option>
              <option 
                v-for="role in availableRoles" 
                :key="role.id" 
                :value="role.roleName"
              >
                {{ role.roleName }} - {{ role.description || '' }}
              </option>
            </select>
            <small class="form-hint">選擇對應的角色</small>
          </div>
          
          <div class="form-group">
            <label>需要權限</label>
            <select v-model="formData.requiredPermission" class="form-select">
              <option value="">無需特定權限</option>
              <option 
                v-for="perm in availablePermissions" 
                :key="perm.id" 
                :value="perm.permissionCode"
              >
                {{ perm.permissionCode }} - {{ perm.permissionName }}
              </option>
            </select>
            <small class="form-hint">選擇對應的權限代碼</small>
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-group">
            <label>排序順序</label>
            <input
              v-model.number="formData.orderIndex"
              type="number"
              min="0"
              placeholder="數字越小優先級越高"
            />
          </div>
          
          <div class="form-group">
            <label class="checkbox-label">
              <input
                type="checkbox"
                v-model="formData.isActive"
              />
              <span>是否啟用</span>
            </label>
          </div>
        </div>
        
        <div class="form-group">
          <label>描述</label>
          <textarea
            v-model="formData.description"
            rows="3"
            placeholder="請輸入 URL 權限描述"
          ></textarea>
        </div>
        
        <div v-if="error" class="error-message">{{ error }}</div>
        
        <div class="modal-footer">
          <button type="button" @click="close" class="btn btn-cancel">取消</button>
          <button type="submit" :disabled="loading" class="btn btn-submit">
            {{ loading ? '處理中...' : (isEdit ? '更新' : '新增') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: Boolean,
  permission: Object
})

const emit = defineEmits(['close', 'saved'])

const isEdit = ref(false)
const loading = ref(false)
const error = ref('')
const availablePermissions = ref([])
const availableRoles = ref([])

const formData = ref({
  urlPattern: '',
  httpMethod: '',
  isPublic: false,
  requiredRole: '',
  requiredPermission: '',
  orderIndex: 0,
  isActive: true,
  description: ''
})

watch(() => props.show, (newVal) => {
  if (newVal) {
    if (props.permission) {
      isEdit.value = true
      formData.value = {
        urlPattern: props.permission.urlPattern || '',
        httpMethod: props.permission.httpMethod || '',
        isPublic: props.permission.isPublic === true,
        requiredRole: props.permission.requiredRole || '',
        requiredPermission: props.permission.requiredPermission || '',
        orderIndex: props.permission.orderIndex || 0,
        isActive: props.permission.isActive !== false,
        description: props.permission.description || ''
      }
    } else {
      isEdit.value = false
      formData.value = {
        urlPattern: '',
        httpMethod: '',
        isPublic: false,
        requiredRole: '',
        requiredPermission: '',
        orderIndex: 0,
        isActive: true,
        description: ''
      }
    }
    error.value = ''
  }
})

const loadPermissions = async () => {
  try {
    const response = await apiRequest('/church/admin/permissions', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      availablePermissions.value = data.permissions || data || []
    }
  } catch (error) {
    console.error('載入權限列表失敗:', error)
  }
}

const loadRoles = async () => {
  try {
    const response = await apiRequest('/church/admin/roles', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      availableRoles.value = data.roles || data || []
    }
  } catch (error) {
    console.error('載入角色列表失敗:', error)
  }
}

const close = () => {
  emit('close')
}

const handleSubmit = async () => {
  error.value = ''
  loading.value = true
  
  try {
    const url = isEdit.value 
      ? `/church/admin/url-permissions/${props.permission.id}`
      : '/church/admin/url-permissions'
    
    const method = isEdit.value ? 'PUT' : 'POST'
    
    const payload = {
      urlPattern: formData.value.urlPattern,
      httpMethod: formData.value.httpMethod || null,
      isPublic: formData.value.isPublic || false,
      requiredRole: formData.value.requiredRole || null,
      requiredPermission: formData.value.requiredPermission || null,
      orderIndex: formData.value.orderIndex || 0,
      isActive: formData.value.isActive !== false,
      description: formData.value.description || null
    }
    
    const response = await apiRequest(url, {
      method,
      body: JSON.stringify(payload),
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      emit('saved', data)
      close()
    } else {
      const data = await response.json()
      error.value = data.message || data.error || '操作失敗'
    }
  } catch (err) {
    error.value = err.message || '操作失敗'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPermissions()
  loadRoles()
})
</script>

<style scoped>

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 2rem;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s;
}

.close-btn:hover {
  background: #f0f0f0;
}

.modal-body {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
}

.required {
  color: #ef4444;
}

.form-hint {
  display: block;
  margin-top: 0.25rem;
  color: #666;
  font-size: 0.875rem;
}

.form-group input[type="text"],
.form-group input[type="number"],
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
  font-family: inherit;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #667eea;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  width: auto;
  cursor: pointer;
}

.error-message {
  background: #fee2e2;
  color: #ef4444;
  padding: 0.75rem;
  border-radius: 8px;
  margin-bottom: 1rem;
  border: 1px solid #ef4444;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e0e0e0;
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

.btn-cancel {
  background: #f0f0f0;
  color: #333;
}

.btn-cancel:hover {
  background: #e0e0e0;
}

.btn-submit {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Improved scroll shadows (prevents fixed shadow overlay issue) */
.modal-body{
  overflow:auto;
  background:
    linear-gradient(#fff 30%, rgba(255,255,255,0)),
    linear-gradient(rgba(255,255,255,0), #fff 70%),
    radial-gradient(farthest-side at 50% 0, rgba(2,6,23,.16), rgba(2,6,23,0)),
    radial-gradient(farthest-side at 50% 100%, rgba(2,6,23,.16), rgba(2,6,23,0));
  background-repeat:no-repeat;
  background-size:100% 40px, 100% 40px, 100% 14px, 100% 14px;
  background-attachment:local, local, scroll, scroll;
  background-position:0 0, 0 100%, 0 0, 0 100%;
}
</style>

