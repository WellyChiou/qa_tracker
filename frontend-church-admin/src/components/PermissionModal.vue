<template>
  <div v-if="show" class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <h3>{{ isEdit ? '編輯權限' : '新增權限' }}</h3>
        <button @click="close" class="close-btn">×</button>
      </div>
      
      <form @submit.prevent="handleSubmit" class="modal-body">
        <div class="form-group">
          <label>權限代碼 <span class="required">*</span></label>
          <input
            v-model="formData.permissionCode"
            type="text"
            required
            placeholder="例如: SERVICE_SCHEDULE_READ"
          />
        </div>
        
        <div class="form-group">
          <label>權限名稱 <span class="required">*</span></label>
          <input
            v-model="formData.permissionName"
            type="text"
            required
            placeholder="例如: 服事表查看"
          />
        </div>
        
        <div class="form-row">
          <div class="form-group">
            <label>資源</label>
            <input
              v-model="formData.resource"
              type="text"
              placeholder="例如: service_schedule"
            />
          </div>
          
          <div class="form-group">
            <label>操作</label>
            <input
              v-model="formData.action"
              type="text"
              placeholder="例如: read, write"
            />
          </div>
        </div>
        
        <div class="form-group">
          <label>描述</label>
          <textarea
            v-model="formData.description"
            rows="3"
            placeholder="請輸入權限描述"
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
import { ref, watch } from 'vue'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: Boolean,
  permission: Object
})

const emit = defineEmits(['close', 'saved'])

const isEdit = ref(false)
const loading = ref(false)
const error = ref('')

const formData = ref({
  permissionCode: '',
  permissionName: '',
  resource: '',
  action: '',
  description: ''
})

watch(() => props.show, (newVal) => {
  if (newVal) {
    if (props.permission) {
      isEdit.value = true
      formData.value = {
        permissionCode: props.permission.permissionCode || '',
        permissionName: props.permission.permissionName || '',
        resource: props.permission.resource || '',
        action: props.permission.action || '',
        description: props.permission.description || ''
      }
    } else {
      isEdit.value = false
      formData.value = {
        permissionCode: '',
        permissionName: '',
        resource: '',
        action: '',
        description: ''
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
      ? `/church/admin/permissions/${props.permission.id}`
      : '/church/admin/permissions'
    
    const method = isEdit.value ? 'PUT' : 'POST'
    
    const payload = {
      permissionCode: formData.value.permissionCode,
      permissionName: formData.value.permissionName,
      resource: formData.value.resource || null,
      action: formData.value.action || null,
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
.form-group input:focus,
.form-group textarea:focus {
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

.form-hint {
  display: block;
  margin-top: 0.25rem;
  color: #666;
  font-size: 0.875rem;
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
</style>

