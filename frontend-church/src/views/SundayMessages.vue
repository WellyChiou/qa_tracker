<template>
  <div class="sunday-messages">
    <section class="section">
      <div class="container">
        <h1 class="section-title">主日信息</h1>
        
        <div v-if="isLoading" class="loading">
          <p>載入中...</p>
        </div>
        <div v-else-if="messagesWithFormattedData.length > 0" class="messages-grid">
          <div class="card message-card" v-for="message in messagesWithFormattedData" :key="message.id">
            <div v-if="message.imageUrl" class="message-image">
              <img :src="message.imageUrl" :alt="message.title || '主日信息'" />
            </div>
            <div class="message-header">
              <div class="message-type-badge" :class="message.typeClass">
                {{ message.typeText }}
              </div>
              <h3>{{ message.title || '主日信息' }}</h3>
            </div>
            <div v-if="message.scripture" class="message-scripture">
              <strong>經文：</strong>{{ message.scripture }}
            </div>
            <div v-if="message.speaker" class="message-speaker">
              <strong>講員：</strong>{{ message.speaker }}
            </div>
            <div v-if="message.content" class="message-content">
              {{ message.content }}
            </div>
            <span class="message-date">{{ message.date }}</span>
          </div>
        </div>
        <div v-else class="no-messages">
          <p>目前沒有主日信息</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { apiRequest } from '@/utils/api'

const messages = ref([])
const isLoading = ref(false)

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dayOfWeek = date.getDay()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${year}/${month}/${day}(${weekdays[dayOfWeek]})`
}

const messagesWithFormattedData = computed(() => {
  // 先排序：最新到最舊（按 serviceDate 降序）
  const sortedMessages = [...messages.value].sort((a, b) => {
    if (!a.serviceDate && !b.serviceDate) return 0
    if (!a.serviceDate) return 1  // 沒有日期的排在後面
    if (!b.serviceDate) return -1 // 沒有日期的排在後面
    return new Date(b.serviceDate) - new Date(a.serviceDate) // 降序：最新在前
  })
  
  return sortedMessages.map(message => ({
    ...message,
    date: formatDate(message.serviceDate),
    typeText: message.serviceType === 'SATURDAY' ? '週六晚崇' : '週日早崇',
    typeClass: message.serviceType === 'SATURDAY' ? 'type-saturday' : 'type-sunday'
  }))
})

const loadMessages = async () => {
  isLoading.value = true
  try {
    const response = await apiRequest('/church/public/sunday-messages', {
      method: 'GET'
    }, '載入主日信息', false)
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        messages.value = data.data
      }
    }
  } catch (error) {
    console.error('載入主日信息失敗:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.messages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 2rem;
}

.message-card {
  position: relative;
  transition: transform 0.3s, box-shadow 0.3s;
  padding-bottom: 3.5rem;
  overflow: hidden;
}

.message-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.message-image {
  width: calc(100% + 4rem);
  height: 300px;
  overflow: hidden;
  margin: -2rem -2rem 1rem -2rem;
  border-radius: 10px 10px 0 0;
}

.message-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
  display: block;
}

.message-card:hover .message-image img {
  transform: scale(1.05);
}

.message-header {
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.message-type-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 15px;
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.type-saturday {
  background: #f0f0f0;
  color: #667eea;
}

.type-sunday {
  background: #667eea;
  color: white;
}

.message-header h3 {
  color: #667eea;
  font-size: 1.5rem;
  margin: 0;
  flex: 1;
}

.message-scripture,
.message-speaker {
  color: #666;
  margin-bottom: 0.75rem;
  line-height: 1.6;
}

.message-scripture strong,
.message-speaker strong {
  color: #333;
}

.message-content {
  margin: 1rem 0;
  line-height: 1.8;
  color: #444;
  white-space: pre-wrap;
}

.message-date {
  position: absolute;
  bottom: 1rem;
  right: 1rem;
  background: #667eea;
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 5px;
  font-size: 0.9rem;
  white-space: nowrap;
  z-index: 1;
}

.loading,
.no-messages {
  text-align: center;
  padding: 2rem;
  color: #666;
}
</style>

