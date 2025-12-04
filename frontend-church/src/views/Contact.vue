<template>
  <div class="contact">
    <section class="section">
      <div class="container">
        <h1 class="section-title">聯絡我們</h1>
        
        <div class="contact-grid">
          <div class="card contact-info">
            <h2>聯絡資訊</h2>
            <div class="info-item">
              <strong>地址：</strong>
              <p>新北市淡水區民族路31巷1號B1</p>
            </div>
            <div class="info-item">
              <strong>電話：</strong>
              <p>(02) 2345-6789</p>
            </div>
            <div class="info-item">
              <strong>電子郵件：</strong>
              <p>contact@church.org</p>
            </div>
            <div class="info-item">
              <strong>服務時間：</strong>
              <p>週一至週五：上午 9:00 - 下午 5:00</p>
              <p>週六：上午 9:00 - 中午 12:00</p>
            </div>
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
              <button type="submit" class="btn">送出</button>
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
import { ref } from 'vue'

const form = ref({
  name: '',
  email: '',
  phone: '',
  message: ''
})

const submitStatus = ref('')
const submitMessage = ref('')

const submitForm = () => {
  // 這裡可以連接到後端 API
  console.log('表單提交：', form.value)
  
  // 模擬提交成功
  submitStatus.value = 'success'
  submitMessage.value = '感謝您的留言！我們會盡快與您聯繫。'
  
  // 清空表單
  form.value = {
    name: '',
    email: '',
    phone: '',
    message: ''
  }
  
  // 3秒後清除訊息
  setTimeout(() => {
    submitStatus.value = ''
    submitMessage.value = ''
  }, 3000)
}
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
</style>

