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
          <div class="checkbox-group">
            <label v-for="permission in availablePermissions" :key="permission.id" class="checkbox-label">
              <input
                type="checkbox"
                :value="permission.id"
                v-model="formData.permissionIds"
              />
              <span>{{ permission.permissionName }} ({{ permission.permissionCode }})</span>
            </label>
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
import { ref, watch } from 'vue'
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

watch(() => props.show, (newVal) => {
  if (newVal) {
    if (props.role) {
      isEdit.value = true
      formData.value = {
        roleName: props.role.roleName || '',
        description: props.role.description || '',
        permissionIds: props.role.permissions ? props.role.permissions.map(p => p.id) : []
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
</style>

