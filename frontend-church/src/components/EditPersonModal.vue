<template>
  <div v-if="show" class="modal-overlay" @click="closeModal">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">編輯人員</h2>
        <button class="btn-close" @click="closeModal">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="person-form">
          <div class="form-group">
            <label class="form-label">姓名 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.personName"
              required
              class="form-input"
              placeholder="請輸入姓名"
            />
          </div>

          <div class="form-group">
            <label class="form-label">顯示名稱</label>
            <input
              type="text"
              v-model="form.displayName"
              class="form-input"
              placeholder="可選，用於顯示"
            />
          </div>

          <div class="form-group">
            <label class="form-label">電話</label>
            <input
              type="text"
              v-model="form.phone"
              class="form-input"
              placeholder="請輸入電話"
            />
          </div>

          <div class="form-group">
            <label class="form-label">電子郵件</label>
            <input
              type="email"
              v-model="form.email"
              class="form-input"
              placeholder="請輸入電子郵件"
            />
          </div>

          <div class="form-group">
            <label class="form-label">備註</label>
            <textarea
              v-model="form.notes"
              class="form-input"
              rows="3"
              placeholder="可選，輸入備註"
            ></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">
              <input
                type="checkbox"
                v-model="form.isActive"
                class="form-checkbox"
              />
              <span class="checkbox-label">啟用</span>
            </label>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
            <button type="button" class="btn btn-secondary" @click="closeModal">
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  person: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'updated'])

const saving = ref(false)
const form = ref({
  personName: '',
  displayName: '',
  phone: '',
  email: '',
  notes: '',
  isActive: true
})

const loadPersonData = () => {
  if (props.person) {
    form.value = {
      personName: props.person.personName || '',
      displayName: props.person.displayName || '',
      phone: props.person.phone || '',
      email: props.person.email || '',
      notes: props.person.notes || '',
      isActive: props.person.isActive !== false
    }
  }
}

const handleSubmit = async () => {
  if (!form.value.personName.trim()) {
    alert('請輸入姓名')
    return
  }

  if (!props.person?.id) {
    alert('人員 ID 不存在')
    return
  }

  saving.value = true
  try {
    const response = await apiRequest(`/church/persons/${props.person.id}`, {
      method: 'PUT',
      body: JSON.stringify(form.value)
    })

    const result = await response.json()
    
    if (response.ok && result.success !== false) {
      emit('updated', result.person)
      closeModal()
    } else {
      console.error('更新失敗響應：', result)
      alert('更新失敗：' + (result.error || '未知錯誤'))
    }
  } catch (error) {
    console.error('更新人員失敗：', error)
    alert('更新失敗：' + (error.message || '網絡錯誤'))
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  form.value = {
    personName: '',
    displayName: '',
    phone: '',
    email: '',
    notes: '',
    isActive: true
  }
}

const closeModal = () => {
  resetForm()
  emit('close')
}

watch(() => props.show, (newVal) => {
  if (newVal) {
    loadPersonData()
  } else {
    resetForm()
  }
})

watch(() => props.person, () => {
  if (props.show && props.person) {
    loadPersonData()
  }
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1300;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-panel {
  width: 100%;
  max-width: 500px;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(to right, #f8fafc, white);
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  color: #64748b;
  transition: color 0.2s;
}

.btn-close:hover {
  color: #1e293b;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.person-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.required {
  color: #dc3545;
}

.form-input {
  padding: 0.875rem 1rem;
  border: 2px solid #e2e8f0;
  border-radius: 0.5rem;
  font-size: 1rem;
  outline: none;
  transition: all 0.2s;
  font-family: inherit;
}

.form-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input::placeholder {
  color: #9ca3af;
}

.form-checkbox {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
}

.checkbox-label {
  font-weight: normal;
  cursor: pointer;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
  transform: translateY(-2px);
}

.w-4 {
  width: 1rem;
  height: 1rem;
}
</style>

