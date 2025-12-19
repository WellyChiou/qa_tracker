<template>
  <div>
    <section class="page-hero" data-hero="true">
      <div class="container hero-surface">
        <div class="hero-inner">
          <div class="badge">Contact</div>
          <h1 class="h1" style="margin-top:12px">聯絡我們</h1>
          <p class="lead" style="margin-top:10px">如果你第一次來訪、想認識我們，或需要代禱，歡迎留言。</p>
        </div>
      </div>
    </section>

    <section class="section section--tight">
      <div class="container">
        <div class="grid grid-2">
          <article class="card" v-if="churchInfo">
            <h2 class="h2" v-reveal>
              聯絡資訊</h2>

            <div class="info" v-if="churchInfo.address">
              <div class="info__label">地址</div>
              <div class="info__value">{{ churchInfo.address }}</div>
            </div>
            <div class="info" v-if="churchInfo.phone">
              <div class="info__label">電話</div>
              <div class="info__value">{{ churchInfo.phone }}</div>
            </div>
            <div class="info" v-if="churchInfo.email">
              <div class="info__label">電子郵件</div>
              <div class="info__value">{{ churchInfo.email }}</div>
            </div>
            <div class="info" v-if="churchInfo.service_hours_weekday || churchInfo.service_hours_weekend">
              <div class="info__label">服務時間</div>
              <div class="info__value">
                <div v-if="churchInfo.service_hours_weekday">{{ churchInfo.service_hours_weekday }}</div>
                <div v-if="churchInfo.service_hours_weekend">{{ churchInfo.service_hours_weekend }}</div>
              </div>
            </div>

            <div style="margin-top:14px" class="muted text-sm">
              也歡迎在聚會後當面與我們聊聊，我們很樂意認識你。
            </div>
          </article>

          <div v-else class="loading"><p>載入中...</p></div>

          <article class="card">
            <h2 class="h2" style="margin:0 0 10px">留言給我們</h2>

            <form class="form" data-reveal="off" @submit.prevent="submitForm">
              <div class="field">
                <label for="name">姓名 <span class="req">*</span></label>
                <input type="text" id="name" v-model="form.name" required />
              </div>

              <div class="field">
                <label for="email">電子郵件 <span class="req">*</span></label>
                <input type="email" id="email" v-model="form.email" required />
              </div>

              <div class="field">
                <label for="phone">電話</label>
                <input type="tel" id="phone" v-model="form.phone" />
              </div>

              <div class="field">
                <label for="message">訊息 <span class="req">*</span></label>
                <textarea id="message" v-model="form.message" rows="5" required />
              </div>

              <button type="submit" class="btn btn-primary" :disabled="isLoading">
                {{ isLoading ? '提交中...' : '送出' }}
              </button>

              <div v-if="submitStatus" class="notice" :class="submitStatus">
                {{ submitMessage }}
              </div>
            </form>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiRequest } from '@/utils/api'

const churchInfo = ref(null)
const form = ref({
  name: '',
  email: '',
  phone: '',
  message: ''
})

const submitStatus = ref('')
const submitMessage = ref('')
const isLoading = ref(false)

const loadChurchInfo = async () => {
  try {
    const response = await apiRequest('/church/public/church-info', { method: 'GET' }, '載入聯絡資訊', false)

    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        churchInfo.value = data.data
      }
    }
  } catch (error) {
    console.error('載入教會資訊失敗:', error)
  }
}

const submitForm = async () => {
  if (isLoading.value) return

  isLoading.value = true
  submitStatus.value = ''
  submitMessage.value = ''

  try {
    const response = await apiRequest(
      '/church/public/contact-submissions',
      {
        method: 'POST',
        body: JSON.stringify(form.value)
      },
      '提交中...',
      false
    )

    const data = await response.json()

    if (response.ok && data.success) {
      submitStatus.value = 'success'
      submitMessage.value = data.message || '感謝您的留言！我們會盡快與您聯繫。'

      form.value = { name: '', email: '', phone: '', message: '' }
    } else {
      submitStatus.value = 'error'
      submitMessage.value = data.message || '提交失敗，請稍後再試。'
    }
  } catch (error) {
    submitStatus.value = 'error'
    submitMessage.value = '提交失敗：' + error.message
  } finally {
    isLoading.value = false
    setTimeout(() => {
      submitStatus.value = ''
      submitMessage.value = ''
    }, 3000)
  }
}

onMounted(loadChurchInfo)
</script>

<style scoped>
.info {
  padding: 12px;
  border: 1px solid var(--border);
  background: var(--surface-2);
  border-radius: 14px;
  margin-top: 10px;
}

.info__label {
  font-size: 12px;
  color: var(--muted-2);
  font-weight: 700;
  margin-bottom: 6px;
}

.info__value {
  color: var(--text);
}

.form {
  display: grid;
  gap: 12px;
}

.field label {
  display: block;
  font-weight: 700;
  font-size: 13px;
  color: rgba(16, 24, 40, 0.78);
  margin-bottom: 6px;
}

.req { color: #d92d20; }

.field input,
.field textarea {
  width: 100%;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.85);
  padding: 12px 12px;
  font-size: 14px;
  font-family: inherit;
  outline: none;
}

.field textarea { resize: vertical; }

.field input:focus,
.field textarea:focus {
  border-color: rgba(31, 157, 106, 0.45);
  box-shadow: 0 0 0 4px rgba(31, 157, 106, 0.12);
}

.notice {
  margin-top: 10px;
  padding: 12px;
  border-radius: 14px;
  font-weight: 600;
  border: 1px solid var(--border);
}

.notice.success {
  background: rgba(31, 157, 106, 0.10);
  color: var(--primary-2);
  border-color: rgba(31, 157, 106, 0.20);
}

.notice.error {
  background: rgba(217, 45, 32, 0.08);
  color: #b42318;
  border-color: rgba(217, 45, 32, 0.18);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
