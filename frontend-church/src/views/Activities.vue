<template>
  <div class="activities">
    <section class="section">
      <div class="container">
        <h1 class="section-title">活動資訊</h1>
        
        <div class="activities-grid">
          <div class="card activity-card" v-for="activity in activities" :key="activity.id">
            <div class="activity-header">
              <h3>{{ activity.title }}</h3>
              <span class="activity-date">{{ activity.date }}</span>
            </div>
            <p class="activity-time">時間：{{ activity.time }}</p>
            <p class="activity-location">地點：{{ activity.location }}</p>
            <p class="activity-description">{{ activity.description }}</p>
            <div class="activity-tags">
              <span class="tag" v-for="tag in activity.tags" :key="tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const activities = ref([
  {
    id: 1,
    title: '聖誕節慶祝活動',
    date: '2024-12-25',
    time: '晚上 7:00',
    location: '教會大堂',
    description: '一起慶祝聖誕節，有詩歌、見證分享和愛宴。',
    tags: ['節慶', '聚會']
  },
  {
    id: 2,
    title: '青年團契',
    date: '每週六',
    time: '下午 2:00',
    location: '青年中心',
    description: '青年人的團契時間，有查經、遊戲和分享。',
    tags: ['青年', '團契']
  },
  {
    id: 3,
    title: '社區服務日',
    date: '2024-12-30',
    time: '上午 9:00',
    location: '社區中心',
    description: '到社區進行關懷服務，歡迎大家一起參與。',
    tags: ['服務', '社區']
  },
  {
    id: 4,
    title: '讀經分享會',
    date: '每週四',
    time: '晚上 7:30',
    location: '教會副堂',
    description: '一起讀經、分享和討論，加深對神話語的認識。',
    tags: ['查經', '分享']
  }
])
</script>

<style scoped>
.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 2rem;
}

.activity-card {
  transition: transform 0.3s, box-shadow 0.3s;
}

.activity-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  margin-bottom: 1rem;
}

.activity-header h3 {
  color: #667eea;
  font-size: 1.5rem;
  flex: 1;
}

.activity-date {
  background: #667eea;
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 5px;
  font-size: 0.9rem;
  white-space: nowrap;
}

.activity-time,
.activity-location {
  color: #666;
  margin-bottom: 0.5rem;
}

.activity-description {
  margin: 1rem 0;
  line-height: 1.6;
}

.activity-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.tag {
  background: #f0f0f0;
  color: #667eea;
  padding: 0.25rem 0.75rem;
  border-radius: 15px;
  font-size: 0.85rem;
}
</style>

