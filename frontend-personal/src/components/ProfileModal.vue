<template>
  <div v-if="show" class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <div>
          <h2 class="modal-title">
            <i class="fas fa-user-circle me-2"></i>
            個人資料設定
          </h2>
        </div>
        <button class="btn-secondary" @click="close">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
          關閉
        </button>
      </div>
      <div class="modal-body">
        <!-- 載入遮罩 -->
        <div v-if="loading" class="loading-overlay">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">載入中...</span>
          </div>
        </div>

        <!-- 用戶基本資訊 -->
        <div class="user-info-section mb-4">
          <h5 class="section-title">
            <i class="fas fa-user me-2"></i>
            基本資訊
          </h5>

          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">顯示名稱</label>
              <p class="form-control-plaintext">{{ user?.displayName || '未設定' }}</p>
            </div>

            <div class="form-group">
              <label class="form-label">電子郵件</label>
              <p class="form-control-plaintext">{{ user?.email || '未設定' }}</p>
            </div>

            <div class="form-group">
              <label class="form-label">用戶名稱</label>
              <p class="form-control-plaintext">{{ user?.username || '未設定' }}</p>
            </div>

            <div class="form-group">
              <label class="form-label">最後登入</label>
              <p class="form-control-plaintext">
                {{ user?.lastLoginAt ? formatDateTime(user.lastLoginAt) : '無記錄' }}
              </p>
            </div>
          </div>
        </div>

        <!-- LINE Bot 綁定區域 -->
        <div class="line-bot-section">
          <h5 class="section-title">
            <i class="fab fa-line me-2"></i>
            LINE Bot 設定
          </h5>

          <div class="line-bot-card">
            <div class="line-bot-status mb-3">
              <div class="d-flex align-items-center">
                <span class="status-icon me-2">
                  <i :class="lineBindingStatus.isBound ? 'fas fa-check-circle text-success' : 'fas fa-times-circle text-secondary'"></i>
                </span>
                <span class="status-text fw-bold">
                  {{ lineBindingStatus.isBound ? '已綁定 LINE 帳號' : '未綁定 LINE 帳號' }}
                </span>
              </div>

              <p class="text-muted small mt-1 mb-0">
                {{ lineBindingStatus.isBound
                  ? '您可以通過 LINE 直接記錄費用並接收提醒'
                  : '綁定 LINE 帳號後，您可以通過 LINE 記錄費用並接收每日提醒'
                }}
              </p>
            </div>

            <!-- 綁定 LINE 帳號 -->
            <div v-if="!lineBindingStatus.isBound" class="line-bind-form">
              <div class="alert alert-info mb-3">
                <i class="fas fa-info-circle me-2"></i>
                <strong>第一步：加入 LINE Bot</strong>
                <p class="mb-2">如果您還沒有加入 Bot，請使用以下方式加入：</p>
                
                <!-- QR Code 顯示區域 -->
                <div v-if="lineBotQrCodeUrl" class="text-center mb-3">
                  <p class="mb-2"><strong>掃描 QR Code 加入 Bot：</strong></p>
                  <img 
                    :src="lineBotQrCodeUrl" 
                    alt="LINE Bot QR Code" 
                    class="line-bot-qrcode"
                    style="max-width: 200px; border: 2px solid #ddd; border-radius: 8px; padding: 10px; background: white;"
                  />
                  <p class="small text-muted mt-2">使用 LINE 掃描上方 QR Code 即可加入 Bot</p>
                </div>
                
                <!-- Bot 加入連結顯示區域 -->
                <div v-if="lineBotJoinUrl && !lineBotQrCodeUrl" class="mb-3">
                  <p class="mb-2"><strong>點擊連結加入 Bot：</strong></p>
                  <div class="input-group">
                    <input 
                      type="text" 
                      :value="lineBotJoinUrl" 
                      readonly 
                      class="form-control text-center font-monospace"
                      style="font-size: 0.85rem;"
                    />
                    <button 
                      class="btn btn-outline-secondary" 
                      type="button"
                      @click="copyBotJoinUrl"
                      title="複製加入連結"
                    >
                      <i class="fas fa-copy"></i> 複製
                    </button>
                  </div>
                  <div class="mt-2">
                    <a 
                      :href="lineBotJoinUrl" 
                      target="_blank" 
                      class="btn btn-primary btn-sm"
                    >
                      <i class="fab fa-line me-1"></i>
                      在 LINE 中開啟並加入
                    </a>
                  </div>
                  <p class="small text-muted mt-2">點擊上方按鈕或複製連結到 LINE 中開啟</p>
                </div>
                
                <!-- Bot ID 顯示區域（如果只有 ID，沒有連結） -->
                <div v-if="lineBotId && !lineBotQrCodeUrl && !lineBotJoinUrl" class="mb-3">
                  <p class="mb-2"><strong>Bot ID：</strong></p>
                  <div class="input-group">
                    <input 
                      type="text" 
                      :value="lineBotId" 
                      readonly 
                      class="form-control text-center font-monospace"
                      style="font-size: 0.9rem;"
                    />
                    <button 
                      class="btn btn-outline-secondary" 
                      type="button"
                      @click="copyBotId"
                      title="複製 Bot ID"
                    >
                      <i class="fas fa-copy"></i> 複製
                    </button>
                  </div>
                  <p class="small text-muted mt-2">請向管理員詢問如何加入此 Bot</p>
                </div>
                
                <!-- 如果沒有配置，顯示提示 -->
                <div v-if="!lineBotQrCodeUrl && !lineBotJoinUrl && !lineBotId" class="mb-2">
                  <p class="mb-0">請向管理員索取 Bot 的 QR Code、加入連結或 Bot ID</p>
                </div>
              </div>
              
              <div class="alert alert-success">
                <i class="fas fa-magic me-2"></i>
                <strong>第二步：獲取 LINE User ID（推薦方法）</strong>
                <ol>
                  <li>在 LINE 中發送任意訊息給 Bot（如：「你好」或「test」）</li>
                  <li>Bot 會自動回復並顯示您的 LINE User ID（以 U 開頭的長字符串）</li>
                  <li>複製這個 ID</li>
                </ol>
                <p class="alert-footer mb-0">
                  🎯 <strong>超簡單！</strong>讓 LINE Bot 告訴您您的 ID
                </p>
              </div>
              
              <div class="alert alert-secondary">
                <i class="fas fa-keyboard me-2"></i>
                <strong>第三步：綁定帳號</strong>
                <p class="mb-0">在下方輸入框貼上剛才複製的 LINE User ID，然後點擊「綁定 LINE 帳號」</p>
              </div>

              <div class="alert alert-info">
                <i class="fas fa-info-circle me-2"></i>
                <strong>手動獲取方法：</strong>
                <ol>
                  <li>電腦瀏覽器開啟 https://line.me</li>
                  <li>登入您的 LINE 帳號</li>
                  <li>按 F12 開啟開發者工具</li>
                  <li>在 Console 輸入：<code>JSON.parse(localStorage.getItem('loginInfo')).userId</code></li>
                  <li>顯示的長字符串即為您的 LINE User ID</li>
                </ol>
              </div>

              <form @submit.prevent="bindLineAccount" class="form-grid">
                <div class="form-group full-width">
                  <label for="lineUserId" class="form-label">
                    LINE 用戶 ID <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="lineUserId"
                    v-model="lineUserIdInput"
                    class="form-control"
                    placeholder="例如: U1234567890abcdef1234567890abcdef"
                    required
                    pattern="^U[a-fA-F0-9]{32}$"
                    title="LINE 用戶 ID 必須以 U 開頭，後面跟 32 位十六進制字符"
                  >
                  <div class="form-text">
                    <small class="text-info">
                      <i class="fas fa-info-circle me-1"></i>
                      這是 LINE 的內部 ID，不是您的自定義 LINE ID（如 jia-wei-chiou）
                    </small>
                  </div>
                  <div class="form-text">
                    <small class="text-muted">
                      LINE 用戶 ID 通常以 U 開頭，跟隨 32 位十六進制字符
                    </small>
                  </div>
                </div>

                <div class="form-group full-width">
                  <button
                    type="submit"
                    class="btn btn-primary"
                    :disabled="!lineUserIdInput || isBinding"
                  >
                    <i class="fab fa-line me-2" v-if="!isBinding"></i>
                    <i class="fas fa-spinner fa-spin me-2" v-else></i>
                    {{ isBinding ? '綁定中...' : '綁定 LINE 帳號' }}
                  </button>
                </div>
              </form>
            </div>

            <!-- 已綁定狀態 -->
            <div v-else class="line-bound-info">
              <div class="alert alert-success">
                <i class="fab fa-line me-2"></i>
                <strong>LINE 帳號已綁定！</strong>
                <p>現在您可以通過 LINE 使用以下功能：</p>
                <ul>
                  <li>發送「支出 食 外食 150 午餐」記錄費用</li>
                  <li>發送「收入 薪資 本薪 50000」記錄收入</li>
                  <li>發送「狀態」查看今日統計</li>
                  <li>發送「今天」查看今日記錄</li>
                  <li>發送「幫助」查看更多指令</li>
                </ul>
              </div>

              <div class="button-group">
                <button
                  @click="testLineMessage"
                  class="btn btn-outline-primary btn-sm"
                  :disabled="isTesting"
                >
                  <i class="fab fa-line me-2" v-if="!isTesting"></i>
                  <i class="fas fa-spinner fa-spin me-2" v-else></i>
                  發送測試訊息
                </button>

                <button
                  @click="unbindLineAccount"
                  class="btn btn-outline-danger btn-sm"
                  :disabled="isUnbinding"
                >
                  <i class="fas fa-unlink me-2" v-if="!isUnbinding"></i>
                  <i class="fas fa-spinner fa-spin me-2" v-else></i>
                  {{ isUnbinding ? '解除中...' : '解除綁定' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 功能說明 -->
        <div class="usage-guide mt-4">
          <h5 class="section-title">
            <i class="fas fa-question-circle me-2"></i>
            使用說明
          </h5>

          <div class="accordion">
            <div class="accordion-item">
              <h2 class="accordion-header">
                <button class="accordion-button" :class="{ collapsed: !accordionOpen.expenseRecording }" type="button" @click="toggleAccordion('expenseRecording')">
                  費用記錄
                  <span class="accordion-arrow">{{ accordionOpen.expenseRecording ? '▼' : '▶' }}</span>
                </button>
              </h2>
              <div v-show="accordionOpen.expenseRecording" class="accordion-body">
                <p>在 LINE 中發送以下格式的訊息來記錄費用：</p>
                <div class="code-block">
                  <code>類型 主類別 細項 金額 描述</code>
                </div>
                <p class="example">例如：<br>
                  <code>支出 食 外食 150 午餐</code><br>
                  <code>收入 薪資 本薪 50000</code>
                </p>
                <p class="mt-2"><strong>格式說明：</strong></p>
                <ul>
                  <li><strong>類型</strong>：支出 或 收入</li>
                  <li><strong>主類別</strong>：
                    <ul>
                      <li>支出：食、衣、住、行、育、樂、醫療、其他支出</li>
                      <li>收入：薪資、投資</li>
                    </ul>
                  </li>
                  <li><strong>細項</strong>：根據主類別選擇對應的細項，例如「食」的細項有：外食、食材、飲料、零食、其他</li>
                  <li><strong>金額</strong>：數字，可包含小數點</li>
                  <li><strong>描述</strong>：可選，用於補充說明</li>
                </ul>
                <p class="mt-2 text-muted small">
                  <i class="fas fa-info-circle me-1"></i>
                  提示：發送「幫助」指令可查看完整的類別和細項列表
                </p>
              </div>
            </div>

            <div class="accordion-item">
              <h2 class="accordion-header">
                <button class="accordion-button" :class="{ collapsed: !accordionOpen.queryCommands }" type="button" @click="toggleAccordion('queryCommands')">
                  查詢指令
                  <span class="accordion-arrow">{{ accordionOpen.queryCommands ? '▼' : '▶' }}</span>
                </button>
              </h2>
              <div v-show="accordionOpen.queryCommands" class="accordion-body">
                <ul class="command-list">
                  <li><code>狀態</code> - 查看今日收支統計</li>
                  <li><code>今天</code> - 查看今日所有記錄</li>
                  <li><code>幫助</code> - 顯示使用說明</li>
                </ul>
              </div>
            </div>

            <div class="accordion-item">
              <h2 class="accordion-header">
                <button class="accordion-button" :class="{ collapsed: !accordionOpen.automaticReminders }" type="button" @click="toggleAccordion('automaticReminders')">
                  自動提醒
                  <span class="accordion-arrow">{{ accordionOpen.automaticReminders ? '▼' : '▶' }}</span>
                </button>
              </h2>
              <div v-show="accordionOpen.automaticReminders" class="accordion-body">
                <p>系統會自動發送提醒：</p>
                <ul>
                  <li><strong>每日提醒</strong>：晚上 8:00 檢查是否記錄了今日費用</li>
                  <li><strong>統計報告</strong>：晚上 9:00 發送今日費用統計</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { apiService } from '@/composables/useApi'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

// 響應式數據
const user = ref(null)
const lineBindingStatus = ref({
  isBound: false,
  lineUserId: null
})
const lineUserIdInput = ref('')
const loading = ref(false)
const isBinding = ref(false)
const isUnbinding = ref(false)
const isTesting = ref(false)
const lineBotQrCodeUrl = ref(null)
const lineBotId = ref(null)
const lineBotJoinUrl = ref(null)
const accordionOpen = ref({
  expenseRecording: false,
  queryCommands: false,
  automaticReminders: false
})

// 組合式函數
const { currentUser } = useAuth()
const api = apiService

// 計算屬性
const userId = computed(() => user.value?.uid)

// 方法
const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return ''
  const date = new Date(dateTimeString)
  return date.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const loadUserProfile = async () => {
  try {
    loading.value = true
    user.value = currentUser.value

    if (user.value?.uid) {
      await loadLineBindingStatus()
      await loadLineBotInfo()
    }
  } catch (error) {
    console.error('載入用戶資料失敗:', error)
    alert('載入用戶資料失敗，請稍後再試')
  } finally {
    loading.value = false
  }
}

const loadLineBotInfo = async () => {
  try {
    // 嘗試獲取 QR Code URL
    try {
      const qrCodeResponse = await api.request('/personal/admin/system-settings/line.bot.qr-code-url')
      if (qrCodeResponse?.setting?.settingValue) {
        lineBotQrCodeUrl.value = qrCodeResponse.setting.settingValue
      }
    } catch (e) {
      // QR Code URL 不存在，忽略
    }

    // 嘗試獲取加入連結（優先）
    try {
      const joinUrlResponse = await api.request('/personal/admin/system-settings/line.bot.join-url')
      if (joinUrlResponse?.setting?.settingValue) {
        lineBotJoinUrl.value = joinUrlResponse.setting.settingValue
      }
    } catch (e) {
      // 加入連結不存在，忽略
    }

    // 嘗試獲取 Bot ID（如果沒有加入連結）
    if (!lineBotJoinUrl.value) {
      try {
        const botIdResponse = await api.request('/personal/admin/system-settings/line.bot.id')
        if (botIdResponse?.setting?.settingValue) {
          lineBotId.value = botIdResponse.setting.settingValue
        }
      } catch (e) {
        // Bot ID 不存在，忽略
      }
    }
  } catch (error) {
    console.error('載入 LINE Bot 資訊失敗:', error)
    // 靜默處理錯誤
  }
}

const copyBotId = async () => {
  if (!lineBotId.value) return
  
  try {
    await navigator.clipboard.writeText(lineBotId.value)
    alert('Bot ID 已複製到剪貼簿！')
  } catch (error) {
    // 如果 clipboard API 不可用，使用傳統方法
    const textArea = document.createElement('textarea')
    textArea.value = lineBotId.value
    textArea.style.position = 'fixed'
    textArea.style.opacity = '0'
    document.body.appendChild(textArea)
    textArea.select()
    try {
      document.execCommand('copy')
      alert('Bot ID 已複製到剪貼簿！')
    } catch (e) {
      alert('複製失敗，請手動複製：' + lineBotId.value)
    }
    document.body.removeChild(textArea)
  }
}

const copyBotJoinUrl = async () => {
  if (!lineBotJoinUrl.value) return
  
  try {
    await navigator.clipboard.writeText(lineBotJoinUrl.value)
    alert('加入連結已複製到剪貼簿！')
  } catch (error) {
    // 如果 clipboard API 不可用，使用傳統方法
    const textArea = document.createElement('textarea')
    textArea.value = lineBotJoinUrl.value
    textArea.style.position = 'fixed'
    textArea.style.opacity = '0'
    document.body.appendChild(textArea)
    textArea.select()
    try {
      document.execCommand('copy')
      alert('加入連結已複製到剪貼簿！')
    } catch (e) {
      alert('複製失敗，請手動複製：' + lineBotJoinUrl.value)
    }
    document.body.removeChild(textArea)
  }
}

const loadLineBindingStatus = async () => {
  try {
    const response = await api.getUserLineStatus(user.value.uid)
    lineBindingStatus.value = response
  } catch (error) {
    console.error('載入 LINE 綁定狀態失敗:', error)
    // 靜默處理錯誤
  }
}

const bindLineAccount = async () => {
  if (!lineUserIdInput.value.trim()) {
    alert('請輸入 LINE 用戶 ID')
    return
  }

  try {
    isBinding.value = true

    const response = await api.bindUserLineAccount(user.value.uid, lineUserIdInput.value.trim())

    if (response && response.success) {
      alert('LINE 帳號綁定成功！')
      lineUserIdInput.value = ''
      await loadLineBindingStatus()
    } else {
      alert('綁定失敗：' + (response?.message || '未知錯誤'))
    }
  } catch (error) {
    console.error('綁定 LINE 帳號失敗:', error)
    const errorMessage = error.message || '綁定失敗，請稍後再試'
    alert(errorMessage)
  } finally {
    isBinding.value = false
  }
}

const unbindLineAccount = async () => {
  if (!confirm('確定要解除 LINE 帳號綁定嗎？解除後將無法通過 LINE 接收提醒。')) {
    return
  }

  try {
    isUnbinding.value = true

    const response = await api.unbindUserLineAccount(user.value.uid)

    if (response && response.success) {
      alert('LINE 帳號解除綁定成功！')
      await loadLineBindingStatus()
    } else {
      alert('解除綁定失敗：' + (response?.message || '未知錯誤'))
    }
  } catch (error) {
    console.error('解除 LINE 帳號綁定失敗:', error)
    const errorMessage = error.message || '解除綁定失敗，請稍後再試'
    alert(errorMessage)
  } finally {
    isUnbinding.value = false
  }
}

const testLineMessage = async () => {
  try {
    isTesting.value = true

    const response = await api.sendLineTestMessage(
      lineBindingStatus.value.lineUserId,
      `🧪 測試訊息\n來自 ${user.value.displayName || user.value.username} 的測試訊息\n時間：${new Date().toLocaleString('zh-TW')}`
    )

    if (response && response.success) {
      alert('✅ 測試訊息已發送！請檢查您的 LINE。')
    } else {
      alert('⚠️ ' + (response?.message || '發送測試訊息失敗，請稍後再試'))
    }
  } catch (error) {
    console.error('發送測試訊息失敗:', error)
    alert('❌ 發送測試訊息失敗: ' + (error.message || '請稍後再試'))
  } finally {
    isTesting.value = false
  }
}

const close = () => {
  emit('close')
}

const handleOverlayClick = (event) => {
  if (event.target === event.currentTarget) {
    close()
  }
}

const toggleAccordion = (key) => {
  accordionOpen.value[key] = !accordionOpen.value[key]
}

// 監聽 show 變化，當 Modal 打開時載入資料
watch(() => props.show, (newValue) => {
  if (newValue) {
    loadUserProfile()
  }
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
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-panel {
  width: 100%;
  max-width: 56rem;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  margin: 2rem 0;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
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
  position: relative;
}

.section-title {
  color: #495057;
  border-bottom: 2px solid #e9ecef;
  padding-bottom: 0.5rem;
  margin-bottom: 1.5rem;
  font-size: 1.1rem;
  font-weight: 600;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: var(--text-primary, #333);
  font-size: 0.95rem;
}

.form-control-plaintext {
  padding: 0.5rem 0;
  color: var(--text-secondary, #666);
}

.line-bot-card {
  background: linear-gradient(135deg, #00c300 0%, #00a000 100%);
  color: white;
  padding: 1.5rem;
  border-radius: 10px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.line-bot-card .alert {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
}

.line-bot-card .alert-info {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

.line-bot-card .alert-success {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

.line-bot-card .btn-outline-primary {
  color: white;
  border-color: rgba(255, 255, 255, 0.5);
}

.line-bot-card .btn-outline-primary:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: white;
}

.line-bot-card .btn-outline-danger {
  color: white;
  border-color: rgba(255, 255, 255, 0.5);
}

.line-bot-card .btn-outline-danger:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: white;
}

.status-icon {
  font-size: 1.2rem;
}

.usage-guide {
  border-top: 1px solid #e9ecef;
  padding-top: 2rem;
}

.alert {
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.alert-success {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #065f46;
}

.alert-info {
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  color: #1e40af;
}

.alert ol,
.alert ul {
  margin: 0.5rem 0;
  padding-left: 1.5rem;
}

.alert ol li,
.alert ul li {
  margin: 0.25rem 0;
}

.alert-footer {
  margin-top: 0.5rem;
  margin-bottom: 0;
  font-size: 0.875rem;
}

.alert code {
  background: rgba(0, 0, 0, 0.1);
  padding: 0.125rem 0.25rem;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
}

.accordion {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.accordion-item {
  border-bottom: 1px solid #e2e8f0;
}

.accordion-item:last-child {
  border-bottom: none;
}

.accordion-header {
  margin: 0;
}

.accordion-button {
  width: 100%;
  padding: 1rem 1.5rem;
  background: #f8fafc;
  border: none;
  text-align: left;
  cursor: pointer;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s;
}

.accordion-button:hover {
  background: #f1f5f9;
}

.accordion-button.collapsed {
  background: #f8fafc;
}

.accordion-arrow {
  font-size: 0.75rem;
  color: #64748b;
}

.accordion-body {
  padding: 1rem 1.5rem;
  background: white;
  border-top: 1px solid #e2e8f0;
}

.code-block {
  background: #f8fafc;
  padding: 1rem;
  border-radius: 6px;
  margin: 0.5rem 0;
}

.code-block code {
  display: block;
  color: #1e293b;
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
  line-height: 1.6;
}

.example {
  margin-top: 0.5rem;
  margin-bottom: 0;
}

.command-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.command-list li {
  padding: 0.5rem 0;
  border-bottom: 1px solid #e2e8f0;
}

.command-list li:last-child {
  border-bottom: none;
}

.command-list code {
  background: #f1f5f9;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
  color: #667eea;
  font-weight: 600;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.btn-secondary {
  background: transparent;
  border: 1px solid #e2e8f0;
  color: #64748b;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s;
  font-weight: 500;
}

.btn-secondary:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #475569;
}

.btn-primary {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-control {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 6px;
  font-size: 1rem;
}

.form-text {
  margin-top: 0.5rem;
  font-size: 0.875rem;
}

.button-group {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.btn-sm {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
}

.btn-outline-primary {
  background: transparent;
  border: 1px solid #3b82f6;
  color: #3b82f6;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-outline-primary:hover {
  background: #3b82f6;
  color: white;
}

.btn-outline-danger {
  background: transparent;
  border: 1px solid #ef4444;
  color: #ef4444;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-outline-danger:hover {
  background: #ef4444;
  color: white;
}

@media (max-width: 768px) {
  .modal-panel {
    width: 95%;
    max-height: 95vh;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .line-bot-card {
    padding: 1rem;
  }
}
</style>

