<template>
  <div class="profile-container">
    <div class="row justify-content-center">
      <div class="col-md-8 col-lg-6">
        <div class="card shadow">
          <div class="card-header bg-primary text-white">
            <h4 class="mb-0">
              <i class="fas fa-user-circle me-2"></i>
              個人資料設定
            </h4>
          </div>

          <div class="card-body">
            <!-- 用戶基本資訊 -->
            <div class="user-info-section mb-4">
              <h5 class="section-title">
                <i class="fas fa-user me-2"></i>
                基本資訊
              </h5>

              <div class="row g-3">
                <div class="col-sm-6">
                  <label class="form-label fw-bold">顯示名稱</label>
                  <p class="form-control-plaintext">{{ user?.displayName || '未設定' }}</p>
                </div>

                <div class="col-sm-6">
                  <label class="form-label fw-bold">電子郵件</label>
                  <p class="form-control-plaintext">{{ user?.email || '未設定' }}</p>
                </div>

                <div class="col-sm-6">
                  <label class="form-label fw-bold">用戶名稱</label>
                  <p class="form-control-plaintext">{{ user?.username || '未設定' }}</p>
                </div>

                <div class="col-sm-6">
                  <label class="form-label fw-bold">最後登入</label>
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
                  <div class="alert alert-success">
                    <i class="fas fa-magic me-2"></i>
                    <strong>簡單綁定方法（推薦）：</strong>
                    <ol class="mb-2 mt-2">
                      <li>在下方輸入框輸入任意臨時 ID（如：test123 或 abc123）</li>
                      <li>點擊「綁定 LINE 帳號」按鈕</li>
                      <li>在 LINE 中發送任意訊息給 Bot（如：hello 或 test）</li>
                      <li>Bot 會回復您的真實 LINE User ID（以 U 開頭的長字符串）</li>
                      <li>複製回復的真實 ID，回到此頁面重新綁定</li>
                    </ol>
                    <p class="mb-0 small text-success">
                      🎯 <strong>超簡單！</strong>讓 LINE Bot 告訴您您的 ID
                    </p>
                  </div>

                  <div class="alert alert-info">
                    <i class="fas fa-info-circle me-2"></i>
                    <strong>手動獲取方法：</strong>
                    <ol class="mb-2 mt-2">
                      <li>電腦瀏覽器開啟 https://line.me</li>
                      <li>登入您的 LINE 帳號</li>
                      <li>按 F12 開啟開發者工具</li>
                      <li>在 Console 輸入：<code>JSON.parse(localStorage.getItem('loginInfo')).userId</code></li>
                      <li>顯示的長字符串即為您的 LINE User ID</li>
                    </ol>
                  </div>

                  <form @submit.prevent="bindLineAccount" class="row g-3">
                    <div class="col-12">
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

                    <div class="col-12">
                      <button
                        type="submit"
                        class="btn btn-success"
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
                    <p class="mb-2">現在您可以通過 LINE 使用以下功能：</p>
                    <ul class="mb-0">
                      <li>發送「支出 餐費 150 午餐」記錄費用</li>
                      <li>發送「收入 薪水 50000」記錄收入</li>
                      <li>發送「狀態」查看今日統計</li>
                      <li>發送「今天」查看今日記錄</li>
                      <li>發送「幫助」查看更多指令</li>
                    </ul>
                  </div>

                  <div class="d-flex gap-2">
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

              <div class="accordion" id="usageAccordion">
                <div class="accordion-item">
                  <h2 class="accordion-header">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#expenseRecording">
                      費用記錄
                    </button>
                  </h2>
                  <div id="expenseRecording" class="accordion-collapse collapse" data-bs-parent="#usageAccordion">
                    <div class="accordion-body">
                      <p>在 LINE 中發送以下格式的訊息來記錄費用：</p>
                      <div class="bg-light p-3 rounded">
                        <code>支出 [類別] [金額] [描述]</code><br>
                        <code>收入 [類別] [金額] [描述]</code>
                      </div>
                      <p class="mt-2 mb-0">例如：<br>
                        <code>支出 餐費 150 午餐</code><br>
                        <code>收入 薪水 50000 月薪</code>
                      </p>
                    </div>
                  </div>
                </div>

                <div class="accordion-item">
                  <h2 class="accordion-header">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#queryCommands">
                      查詢指令
                    </button>
                  </h2>
                  <div id="queryCommands" class="accordion-collapse collapse" data-bs-parent="#usageAccordion">
                    <div class="accordion-body">
                      <ul class="list-unstyled">
                        <li><code>狀態</code> - 查看今日收支統計</li>
                        <li><code>今天</code> - 查看今日所有記錄</li>
                        <li><code>幫助</code> - 顯示使用說明</li>
                      </ul>
                    </div>
                  </div>
                </div>

                <div class="accordion-item">
                  <h2 class="accordion-header">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#automaticReminders">
                      自動提醒
                    </button>
                  </h2>
                  <div id="automaticReminders" class="accordion-collapse collapse" data-bs-parent="#usageAccordion">
                    <div class="accordion-body">
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
      </div>
    </div>

    <!-- 載入遮罩 -->
    <div v-if="loading" class="loading-overlay">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">載入中...</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { apiService } from '@/composables/useApi'
import { toast } from '@shared/composables/useToast'

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
    }
  } catch (error) {
    console.error('載入用戶資料失敗:', error)
    toast.error('載入用戶資料失敗，請稍後再試')
  } finally {
    loading.value = false
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

    if (response.data.success) {
      alert('LINE 帳號綁定成功！')
      lineUserIdInput.value = ''
      await loadLineBindingStatus()
    } else {
      alert('綁定失敗：' + (response.data.message || '未知錯誤'))
    }
  } catch (error) {
    console.error('綁定 LINE 帳號失敗:', error)
    const errorMessage = error.response?.data?.message || '綁定失敗，請稍後再試'
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

    if (response.data.success) {
      alert('LINE 帳號解除綁定成功！')
      await loadLineBindingStatus()
    } else {
      alert('解除綁定失敗：' + (response.data.message || '未知錯誤'))
    }
  } catch (error) {
    console.error('解除 LINE 帳號綁定失敗:', error)
    const errorMessage = error.response?.data?.message || '解除綁定失敗，請稍後再試'
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

    alert('測試訊息已發送！請檢查您的 LINE。')
  } catch (error) {
    console.error('發送測試訊息失敗:', error)
    alert('發送測試訊息失敗，請稍後再試')
  } finally {
    isTesting.value = false
  }
}

// 生命週期
onMounted(() => {
  loadUserProfile()
})
</script>

<style scoped>
.profile-container {
  padding: 2rem 0;
  min-height: 80vh;
}

.section-title {
  color: #495057;
  border-bottom: 2px solid #e9ecef;
  padding-bottom: 0.5rem;
  margin-bottom: 1.5rem;
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

.accordion-button:not(.collapsed) {
  background-color: #f8f9fa;
  color: #495057;
}

.accordion-button:focus {
  box-shadow: none;
}

.loading-overlay {
  position: fixed;
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

@media (max-width: 768px) {
  .profile-container {
    padding: 1rem 0;
  }

  .line-bot-card {
    padding: 1rem;
  }
}
</style>
