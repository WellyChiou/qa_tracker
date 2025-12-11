<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">💱 匯率管理</h2>
        <button class="btn-close" @click="$emit('close')" title="關閉">
          <svg class="close-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <div class="exchange-rate-section">
          <h3>匯率管理 (對台幣)</h3>
          <div class="rate-date-controls">
            <div class="form-group">
              <label for="rateDate">匯率日期</label>
              <input type="date" id="rateDate" v-model="rateDate" />
            </div>
            <button class="btn btn-outline" @click="loadRatesForDate" :disabled="loading">
              📅 載入該日期匯率
            </button>
          </div>
          
          <div class="exchange-rates-cards">
            <div class="exchange-rate-card" v-for="currency in currencies" :key="currency.code">
              <div class="currency-header">
                <span class="currency-name">{{ currency.name }}</span>
                <span class="currency-code">({{ currency.code }})</span>
              </div>
              <div class="currency-rate">
                <input 
                  type="number" 
                  :id="`${currency.code}Rate`"
                  v-model.number="rates[currency.code]" 
                  step="0.0001" 
                  :placeholder="currency.placeholder"
                />
              </div>
            </div>
          </div>
          
          <div class="exchange-rate-actions">
            <button class="btn btn-primary" @click="fetchLatestRates" :disabled="loading">
              🔄 {{ loading ? '更新中...' : '自動更新匯率' }}
            </button>
            <button class="btn btn-secondary" @click="saveExchangeRates" :disabled="saving">
              💾 {{ saving ? '儲存中...' : '儲存匯率' }}
            </button>
            <button class="btn btn-info" @click="autoFillMissingRates" :disabled="loading">
              🔧 {{ loading ? '補足中...' : '自動補足最近7天匯率' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiService } from '@/composables/useApi'

const emit = defineEmits(['close'])

const loading = ref(false)
const saving = ref(false)
const rateDate = ref(new Date().toISOString().split('T')[0])

const currencies = [
  { code: 'USD', name: '美元', placeholder: '30.0000' },
  { code: 'EUR', name: '歐元', placeholder: '32.0000' },
  { code: 'JPY', name: '日圓', placeholder: '0.2000' },
  { code: 'CNY', name: '人民幣', placeholder: '4.2000' }
]

const rates = ref({
  USD: null,
  EUR: null,
  JPY: null,
  CNY: null
})

const loadRatesForDate = async () => {
  loading.value = true
  try {
    const response = await apiService.getExchangeRate(rateDate.value)
    if (response) {
      rates.value = {
        USD: response.usdRate ? parseFloat(response.usdRate) : null,
        EUR: response.eurRate ? parseFloat(response.eurRate) : null,
        JPY: response.jpyRate ? parseFloat(response.jpyRate) : null,
        CNY: response.cnyRate ? parseFloat(response.cnyRate) : null
      }
    } else {
      // 如果沒有該日期的匯率，嘗試載入最新的
      const latest = await apiService.getLatestExchangeRate(rateDate.value)
      if (latest) {
        rates.value = {
          USD: latest.usdRate ? parseFloat(latest.usdRate) : null,
          EUR: latest.eurRate ? parseFloat(latest.eurRate) : null,
          JPY: latest.jpyRate ? parseFloat(latest.jpyRate) : null,
          CNY: latest.cnyRate ? parseFloat(latest.cnyRate) : null
        }
      }
    }
  } catch (error) {
    console.error('載入匯率失敗:', error)
  } finally {
    loading.value = false
  }
}

const fetchLatestRates = async () => {
  loading.value = true
  try {
    let successCount = 0
    for (const currency of currencies) {
      if (currency.code === 'TWD') continue
      try {
        const response = await fetch(`https://api.exchangerate-api.com/v4/latest/TWD`)
        const data = await response.json()
        const rate = 1 / data.rates[currency.code]
        rates.value[currency.code] = parseFloat(rate.toFixed(4))
        successCount++
      } catch (error) {
        console.error(`取得 ${currency.code} 匯率失敗:`, error)
      }
    }
    if (successCount > 0) {
      alert(`已成功更新 ${successCount} 種匯率`)
    } else {
      alert('更新匯率失敗，請稍後再試')
    }
  } catch (error) {
    console.error('更新匯率失敗:', error)
    alert('更新匯率失敗: ' + error.message)
  } finally {
    loading.value = false
  }
}

const saveExchangeRates = async () => {
  saving.value = true
  try {
    await apiService.saveExchangeRate({
      date: rateDate.value,
      usdRate: rates.value.USD,
      eurRate: rates.value.EUR,
      jpyRate: rates.value.JPY,
      cnyRate: rates.value.CNY
    })
    emit('close')
  } catch (error) {
    console.error('儲存匯率失敗:', error)
    alert('儲存失敗: ' + error.message)
  } finally {
    saving.value = false
  }
}

const autoFillMissingRates = async () => {
  loading.value = true
  try {
    // 調用後端 API 自動補足匯率（後端會自動檢查並補足最近7天缺少的匯率）
    const response = await apiService.autoFillExchangeRates(7)
    const message = response?.message || `已補足 ${response?.filledCount || 0} 個日期的匯率`
    alert(message)
    // 重新載入當前日期的匯率
    await loadRatesForDate()
  } catch (error) {
    console.error('自動補足匯率失敗:', error)
    alert('自動補足匯率失敗: ' + (error.message || '未知錯誤'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRatesForDate()
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
  overflow-y: auto;
}

.modal-panel {
  width: 100%;
  max-width: 56rem;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  margin: 2rem 0;
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

.exchange-rate-section h3 {
  margin-bottom: 1.5rem;
  color: #1e293b;
}

.rate-date-controls {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  margin-bottom: 1.5rem;
}

.exchange-rates-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.exchange-rate-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 0.75rem;
  padding: 1rem;
}

.currency-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.currency-name {
  font-weight: 600;
  color: #1e293b;
}

.currency-code {
  font-size: 0.875rem;
  color: #64748b;
}

.currency-rate input {
  width: 100%;
  padding: 0.625rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 0.5rem;
  font-size: 1rem;
}

.exchange-rate-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
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

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-info {
  background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
  color: white;
}

.btn-outline {
  background: white;
  color: #475569;
  border: 1.5px solid #cbd5e1;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 600;
  color: #475569;
}

.form-group input {
  padding: 0.625rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 0.5rem;
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

