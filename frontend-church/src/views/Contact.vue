<template>
  <div class="contact">
    <section class="section">
      <div class="container">
        <h1 class="section-title">聯絡我們</h1>
        
        <div class="contact-grid">
          <div class="card contact-info" v-if="churchInfo">
            <h2>聯絡資訊</h2>
            <div class="info-item" v-if="churchInfo.address">
              <strong>地址：</strong>
              <p>{{ churchInfo.address }}</p>
            </div>
            <div class="info-item" v-if="churchInfo.phone">
              <strong>電話：</strong>
              <p>{{ churchInfo.phone }}</p>
            </div>
            <div class="info-item" v-if="churchInfo.email">
              <strong>電子郵件：</strong>
              <p>{{ churchInfo.email }}</p>
            </div>
            <div class="info-item" v-if="churchInfo.service_hours_weekday || churchInfo.service_hours_weekend">
              <strong>服務時間：</strong>
              <p v-if="churchInfo.service_hours_weekday">{{ churchInfo.service_hours_weekday }}</p>
              <p v-if="churchInfo.service_hours_weekend">{{ churchInfo.service_hours_weekend }}</p>
            </div>
          </div>
          <div v-else class="loading">
            <p>載入中...</p>
          </div>

          <div class="card contact-form">
            <h2>留言給我們</h2>
            <form @submit.prevent="submitForm">
              <div class="form-group">
                <label for="name">姓名 *</label>
                <input type="text" id="name" v-model="form.name" required>
              </div>
              <div class="form-group">
                <label for="email">電子郵件 *</label>
                <input type="email" id="email" v-model="form.email" required>
              </div>
              <div class="form-group">
                <label for="phone">電話</label>
                <input type="tel" id="phone" v-model="form.phone">
              </div>
              <div class="form-group">
                <label for="message">訊息 *</label>
                <textarea id="message" v-model="form.message" rows="5" required></textarea>
              </div>
              <button type="submit" class="btn" :disabled="isLoading">
                {{ isLoading ? '提交中...' : '送出' }}
              </button>
            </form>
            <div v-if="submitStatus" class="submit-message" :class="submitStatus">
              {{ submitMessage }}
            </div>
          </div>
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
    const response = await apiRequest('/church/public/church-info', {
      method: 'GET'
    }, '載入聯絡資訊', false)
    
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
    const response = await apiRequest('/church/public/contact-submissions', {
      method: 'POST',
      body: JSON.stringify(form.value)
    }, '提交中...', false)
    
    const data = await response.json()
    
    if (response.ok && data.success) {
      submitStatus.value = 'success'
      submitMessage.value = data.message || '感謝您的留言！我們會盡快與您聯繫。'
      
      // 清空表單
      form.value = {
        name: '',
        email: '',
        phone: '',
        message: ''
      }
    } else {
      submitStatus.value = 'error'
      submitMessage.value = data.message || '提交失敗，請稍後再試。'
    }
  } catch (error) {
    submitStatus.value = 'error'
    submitMessage.value = '提交失敗：' + error.message
  } finally {
    isLoading.value = false
    
    // 3秒後清除訊息
    setTimeout(() => {
      submitStatus.value = ''
      submitMessage.value = ''
    }, 3000)
  }
}

onMounted(() => {
  loadChurchInfo()
})
</script>

<style scoped>
.contact-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 2rem;
}

.contact-info h2,
.contact-form h2 {
  color: #667eea;
  margin-bottom: 1.5rem;
  font-size: 1.8rem;
}

.info-item {
  margin-bottom: 1.5rem;
}

.info-item strong {
  display: block;
  color: #667eea;
  margin-bottom: 0.5rem;
}

.info-item p {
  margin: 0.25rem 0;
  color: #666;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #333;
  font-weight: 500;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 1rem;
  font-family: inherit;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.submit-message {
  margin-top: 1rem;
  padding: 1rem;
  border-radius: 5px;
  text-align: center;
}

.submit-message.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.submit-message.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.loading {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

