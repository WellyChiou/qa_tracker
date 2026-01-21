<template>
  <div v-if="show" class="modal-overlay" @click="closeModal">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">新增崗位</h2>
        <button class="btn-close" @click="closeModal">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="position-form">
          <div class="form-group">
            <label class="form-label">崗位代碼 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.positionCode"
              required
              class="form-input"
              placeholder="例如：computer, sound, light, live"
            />
            <small class="form-hint">用於系統識別，建議使用英文小寫</small>
          </div>

          <div class="form-group">
            <label class="form-label">崗位名稱 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.positionName"
              required
              class="form-input"
              placeholder="例如：電腦、混音、燈光、直播"
            />
          </div>

          <div class="form-group">
            <label class="form-label">崗位描述</label>
            <textarea
              v-model="form.description"
              class="form-input"
              rows="3"
              placeholder="可選，輸入崗位描述"
            ></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">排序順序</label>
            <input
              type="number"
              v-model.number="form.sortOrder"
              class="form-input"
              placeholder="數字越小越靠前"
              min="0"
            />
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
import { toast } from '@shared/composables/useToast'
import { ref, watch } from 'vue'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'created'])

const saving = ref(false)
const form = ref({
  positionCode: '',
  positionName: '',
  description: '',
  sortOrder: 0,
  isActive: true
})

const handleSubmit = async () => {
  if (!form.value.positionCode.trim()) {
    toast.info('請輸入崗位代碼')
    return
  }

  if (!form.value.positionName.trim()) {
    toast.info('請輸入崗位名稱')
    return
  }

  saving.value = true
  try {
    const response = await apiRequest('/church/positions', {
      method: 'POST',
      body: JSON.stringify(form.value)
    })

    const result = await response.json()
    
    if (response.ok && result.success !== false) {
      emit('created', result.position)
      resetForm()
      closeModal()
    } else {
      console.error('創建失敗響應：', result)
      toast.error('創建失敗：' + (result.error || '未知錯誤'))
    }
  } catch (error) {
    console.error('創建崗位失敗：', error)
    toast.error('創建失敗：' + (error.message || '網絡錯誤'))
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  form.value = {
    positionCode: '',
    positionName: '',
    description: '',
    sortOrder: 0,
    isActive: true
  }
}

const closeModal = () => {
  resetForm()
  emit('close')
}

watch(() => props.show, (newVal) => {
  if (!newVal) {
    resetForm()
  }
})
</script>

<style scoped>

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1400;
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

.position-form {
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

.form-hint {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: -0.25rem;
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

