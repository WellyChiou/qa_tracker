<template>
  <div v-if="show" class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <h3>{{ isEdit ? '編輯角色' : '新增角色' }}</h3>
        <button @click="close" class="close-btn">×</button>
      </div>
      
      <form @submit.prevent="handleSubmit" class="modal-body">
        <div class="form-group">
          <label>角色名稱 <span class="required">*</span></label>
          <input
            v-model="formData.roleName"
            type="text"
            required
            placeholder="例如: ROLE_CHURCH_ADMIN"
          />
        </div>
        
        <div class="form-group">
          <label>描述</label>
          <textarea
            v-model="formData.description"
            rows="3"
            placeholder="請輸入角色描述"
          ></textarea>
        </div>
        
        
        <div class="form-group">
          <label>權限</label>

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
                    <div class="transfer-sub"><code>({{ p.permissionCode }})</code></div>
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
                    <div class="transfer-sub"><code>({{ p.permissionCode }})</code></div>
                  </div>
                </div>
                <div v-if="filteredSelectedPerms.length === 0" class="transfer-empty">尚未加入任何權限</div>
              </div>
              <div class="transfer-actions">
                <button type="button" class="btn-action-full" @click="removeAllPerms" :disabled="filteredSelectedPerms.length === 0">
                  全部移除
                </button>
              </div>
            </div>
          </div>
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
import { ref, watch, computed } from 'vue'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: Boolean,
  role: Object,
  availablePermissions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'saved'])

const isEdit = ref(false)
const loading = ref(false)
const error = ref('')

const formData = ref({
  roleName: '',
  description: '',
  permissionIds: []
})

// 權限（左右分欄）
const permSearchLeft = ref('')
const permSearchRight = ref('')

const selectedPermSet = computed(() => new Set(formData.value.permissionIds || []))
const availablePerms = computed(() => {
  const perms = props.availablePermissions || []
  return Array.isArray(perms) ? perms : []
})
const unselectedPerms = computed(() => {
  if (!Array.isArray(availablePerms.value)) return []
  return availablePerms.value.filter(p => p && p.id != null && !selectedPermSet.value.has(p.id))
})
const selectedPerms = computed(() => {
  if (!Array.isArray(availablePerms.value)) return []
  return availablePerms.value.filter(p => p && p.id != null && selectedPermSet.value.has(p.id))
})

const filteredAvailablePerms = computed(() => {
  const q = permSearchLeft.value.trim().toLowerCase()
  if (!q) return unselectedPerms.value
  return unselectedPerms.value.filter(p =>
    `${p.permissionName} ${p.permissionCode}`.toLowerCase().includes(q)
  )
})

const filteredSelectedPerms = computed(() => {
  const q = permSearchRight.value.trim().toLowerCase()
  if (!q) return selectedPerms.value
  return selectedPerms.value.filter(p =>
    `${p.permissionName} ${p.permissionCode}`.toLowerCase().includes(q)
  )
})

const addPerm = (id) => {
  const ids = new Set(formData.value.permissionIds || [])
  ids.add(id)
  formData.value.permissionIds = Array.from(ids)
}

const removePerm = (id) => {
  formData.value.permissionIds = (formData.value.permissionIds || []).filter(x => x !== id)
}

const addAllPerms = () => {
  const ids = filteredAvailablePerms.value.map(p => p?.id).filter(id => id != null)
  const s = new Set(formData.value.permissionIds || [])
  ids.forEach(id => s.add(id))
  formData.value.permissionIds = Array.from(s)
}

const removeAllPerms = () => {
  formData.value.permissionIds = []
}

watch(() => props.show, (newVal) => {
  if (newVal) {
    if (props.role) {
      isEdit.value = true
      // 確保 permissions 是數組，並正確提取 ID
      let permissionIds = []
      if (props.role.permissions) {
        if (Array.isArray(props.role.permissions)) {
          permissionIds = props.role.permissions.map(p => p?.id || p).filter(id => id != null)
        } else if (typeof props.role.permissions === 'object') {
          // 如果是 Set 或其他對象，嘗試轉換為數組
          const permsArray = Array.from(props.role.permissions)
          permissionIds = permsArray.map(p => p?.id || p).filter(id => id != null)
        }
      }
      formData.value = {
        roleName: props.role.roleName || '',
        description: props.role.description || '',
        permissionIds: permissionIds
      }
    } else {
      isEdit.value = false
      formData.value = {
        roleName: '',
        description: '',
        permissionIds: []
      }
    }
    error.value = ''
  }
})

const close = () => {
  emit('close')
}

const handleSubmit = async () => {
  error.value = ''
  loading.value = true
  
  try {
    const url = isEdit.value 
      ? `/church/admin/roles/${props.role.id}`
      : '/church/admin/roles'
    
    const method = isEdit.value ? 'PUT' : 'POST'
    
    const payload = {
      roleName: formData.value.roleName,
      description: formData.value.description,
      permissions: formData.value.permissionIds.map(id => ({ id }))
    }
    
    const data = await apiRequest(url, {
      method,
      body: JSON.stringify(payload),
      credentials: 'include'
    })
    
    if (data !== null) {
      emit('saved', data)
      close()
    } else {
      error.value = '操作失敗'
    }
  } catch (err) {
    error.value = err.message || '操作失敗'
  } finally {
    loading.value = false
  }
}
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
  max-width: 600px;
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

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
}

.required {
  color: #ef4444;
}

.form-group input[type="text"],
.form-group textarea {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
  font-family: inherit;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: 300px;
  overflow-y: auto;
  padding: 0.5rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-weight: normal;
}

.checkbox-label input {
  width: auto;
  margin: 0;
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

.transfer{
  display:grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 10px;
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
  max-height: 260px;
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
.transfer-title{
  font-weight: 900;
  color: #0f172a;
  line-height: 1.2;
}
.transfer-sub{
  margin-top: 4px;
  color: #64748b;
  font-weight: 700;
  font-size: 12px;
}
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
.transfer-empty{
  padding: 14px 8px;
  text-align:center;
  color:#94a3b8;
  font-weight: 800;
}
@media (max-width: 980px){
  .transfer{ grid-template-columns: 1fr; }
  .transfer-search{ width: 100%; }
}

</style>
