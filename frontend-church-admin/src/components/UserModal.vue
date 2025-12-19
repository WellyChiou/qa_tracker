<template>
  <div v-if="show" class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <h3>{{ isEdit ? '編輯用戶' : '新增用戶' }}</h3>
        <button @click="close" class="close-btn">×</button>
      </div>
      
      <form @submit.prevent="handleSubmit" class="modal-body">
        <div class="form-group">
          <label>用戶名 <span class="required">*</span></label>
          <input
            v-model="formData.username"
            type="text"
            required
            :disabled="isEdit"
            placeholder="請輸入用戶名"
          />
        </div>
        
        <div class="form-group">
          <label>密碼 <span v-if="!isEdit" class="required">*</span></label>
          <input
            v-model="formData.password"
            type="password"
            :required="!isEdit"
            :placeholder="isEdit ? '留空則不修改密碼' : '請輸入密碼'"
          />
        </div>
        
        <div class="form-group">
          <label>電子郵件 <span class="required">*</span></label>
          <input
            v-model="formData.email"
            type="email"
            required
            placeholder="請輸入電子郵件"
          />
        </div>
        
        <div class="form-group">
          <label>顯示名稱</label>
          <input
            v-model="formData.displayName"
            type="text"
            placeholder="請輸入顯示名稱"
          />
        </div>
        
        <div class="form-group">
          <label>狀態</label>
          <div class="radio-group">
            <label class="radio-label">
              <input
                type="radio"
                :value="true"
                v-model="formData.isEnabled"
              />
              <span>啟用</span>
            </label>
            <label class="radio-label">
              <input
                type="radio"
                :value="false"
                v-model="formData.isEnabled"
              />
              <span>停用</span>
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
import { ref, watch, onMounted } from 'vue'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: Boolean,
  user: Object,
})

const emit = defineEmits(['close', 'saved'])

const isEdit = ref(false)
const loading = ref(false)
const error = ref('')

const formData = ref({
  username: '',
  password: '',
  email: '',
  displayName: '',  isEnabled: true
})

watch(() => props.show, (newVal) => {
  if (newVal) {
    if (props.user) {
      isEdit.value = true
      formData.value = {
        username: props.user.username || '',
        password: '',
        email: props.user.email || '',
        displayName: props.user.displayName || '',        isEnabled: props.user.isEnabled !== false
      }
    } else {
      isEdit.value = false
      formData.value = {
        username: '',
        password: '',
        email: '',
        displayName: '',        isEnabled: true
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
      ? `/church/admin/users/${props.user.uid}`
      : '/church/admin/users'
    
    const method = isEdit.value ? 'PUT' : 'POST'
    
    const payload = {
      username: formData.value.username,
      email: formData.value.email,
      displayName: formData.value.displayName,
      isEnabled: formData.value.isEnabled
    }
    
    if (!isEdit.value && formData.value.password) {
      payload.password = formData.value.password
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
.form-group input[type="email"],
.form-group input[type="password"] {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.checkbox-group,
.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.checkbox-label,
.radio-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-weight: normal;
}

.checkbox-label input,
.radio-label input {
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

