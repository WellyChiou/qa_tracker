<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">💰 交易費用配置</h2>
        <button class="btn-close" @click="$emit('close')" title="關閉">
          <svg class="close-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <form @submit.prevent="saveConfig">
          <div class="form-group">
            <label for="commissionRate">手續費率 (%)</label>
            <input 
              type="number" 
              id="commissionRate" 
              v-model.number="config.commission" 
              step="0.001" 
              min="0" 
              max="100"
              required
            />
            <small>例如：0.1425 表示 0.1425%</small>
          </div>
          
          <div class="form-group">
            <label for="taxRateTWD">台股稅率 (%)</label>
            <input 
              type="number" 
              id="taxRateTWD" 
              v-model.number="config.tax.TWD" 
              step="0.001" 
              min="0" 
              max="100"
              required
            />
          </div>
          
          <div class="form-group">
            <label for="taxRateUSD">美股稅率 (%)</label>
            <input 
              type="number" 
              id="taxRateUSD" 
              v-model.number="config.tax.USD" 
              step="0.001" 
              min="0" 
              max="100"
              required
            />
          </div>
          
          <div class="form-group">
            <label for="taxRateETF">ETF 稅率 (%)</label>
            <input 
              type="number" 
              id="taxRateETF" 
              v-model.number="config.tax.ETF" 
              step="0.001" 
              min="0" 
              max="100"
              required
            />
          </div>
          
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="saving">
              {{ saving ? '儲存中...' : '💾 儲存配置' }}
            </button>
            <button type="button" class="btn btn-secondary" @click="$emit('close')">
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiService } from '@/composables/useApi'

const emit = defineEmits(['close'])

const saving = ref(false)
const config = ref({
  commission: 0.1425,
  tax: {
    TWD: 0.3,
    USD: 0,
    ETF: 0.1
  }
})

const loadConfig = async () => {
  try {
    const response = await apiService.request('/personal/admin/system-settings/trading.fees')
    if (response && response.setting && response.setting.settingValue) {
      const parsed = typeof response.setting.settingValue === 'string' 
        ? JSON.parse(response.setting.settingValue) 
        : response.setting.settingValue
      config.value = {
        commission: parsed.commission || 0.1425,
        tax: {
          TWD: parsed.tax?.TWD || 0.3,
          USD: parsed.tax?.USD || 0,
          ETF: parsed.tax?.ETF || 0.1
        }
      }
    }
  } catch (error) {
    console.log('使用預設交易費用配置')
  }
}

const saveConfig = async () => {
  saving.value = true
  try {
    await apiService.request('/personal/admin/system-settings/trading.fees', {
      method: 'PUT',
      body: JSON.stringify({
        settingValue: JSON.stringify(config.value),
        description: '交易費用配置'
      })
    })
    emit('close')
  } catch (error) {
    console.error('儲存交易費用配置失敗:', error)
    alert('儲存失敗: ' + error.message)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-panel {
  width: 100%;
  max-width: 32rem;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(to right, #f8fafc, white);
  border-radius: 1rem 1rem 0 0;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
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
  color: #475569;
}

.form-group input {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 0.5rem;
  font-size: 1rem;
  transition: all 0.2s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}

.form-group small {
  display: block;
  margin-top: 0.25rem;
  color: #64748b;
  font-size: 0.875rem;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.btn {
  padding: 0.625rem 1.25rem;
  border-radius: 0.5rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
}

.w-4 {
  width: 1rem;
}

.h-4 {
  height: 1rem;
}

.btn-close {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #64748b;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.btn-close::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  transform: translate(-50%, -50%);
  transition: width 0.3s ease, height 0.3s ease;
  z-index: 0;
}

.btn-close:hover::before {
  width: 100%;
  height: 100%;
}

.btn-close:hover {
  border-color: #ef4444;
  transform: scale(1.1) rotate(90deg);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.btn-close:hover .close-icon {
  color: white;
  transform: scale(1.1);
}

.btn-close:active {
  transform: scale(0.95) rotate(90deg);
}

.close-icon {
  width: 20px;
  height: 20px;
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
}
</style>

