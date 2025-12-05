<template>
  <div class="activities">
    <section class="section">
      <div class="container">
        <h1 class="section-title">活動資訊</h1>
        
        <div class="activities-grid">
          <div class="card activity-card" v-for="activity in activities" :key="activity.id">
            <div class="activity-header">
              <h3>{{ activity.title }}</h3>
            </div>
            <p class="activity-time">時間：{{ activity.time }}</p>
            <p class="activity-location">地點：{{ activity.location }}</p>
            <p class="activity-description">{{ activity.description }}</p>
            <div class="activity-tags">
              <span class="tag" v-for="tag in activity.tags" :key="tag">{{ tag }}</span>
            </div>
            <span class="activity-date">{{ activity.date }}</span>
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
    title: '聯合受洗主日',
    date: '2024-12-28',
    time: '10:00am',
    location: '榮耀堂',
    description: '聯合受洗主日，歡迎弟兄姊妹一同參與這個重要的時刻。',
    tags: ['主日', '受洗']
  },
  {
    id: 2,
    title: '極光元旦登高',
    date: '2025-01-01',
    time: '9:30am',
    location: '觀音山遊客中心',
    description: '新的一年，讓我們一起登高，迎接新的開始！',
    tags: ['戶外', '運動']
  },
  {
    id: 3,
    title: '極光同工尾牙',
    date: '2025-01-11',
    time: '傍晚（時間待定）',
    location: '地點待定',
    description: '感謝同工們一年來的辛勞，讓我們一起歡聚慶祝！',
    tags: ['同工', '聚餐']
  },
  {
    id: 4,
    title: '極光門徒退修會',
    date: '2025-02-27 ~ 2025-02-28',
    time: '兩天一夜',
    location: '桃園復興鄉',
    description: '門徒退修會，讓我們在安靜中親近神，重新得力。',
    tags: ['退修會', '門徒']
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
  position: relative;
  transition: transform 0.3s, box-shadow 0.3s;
  padding-bottom: 3.5rem; /* 為日期預留空間 */
}

.activity-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.activity-header {
  margin-bottom: 1rem;
}

.activity-header h3 {
  color: #667eea;
  font-size: 1.5rem;
  margin: 0;
}

.activity-date {
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
  margin-bottom: 0.5rem; /* 與日期保持距離 */
  max-width: calc(100% - 150px); /* 為日期預留空間，避免重疊 */
}

.tag {
  background: #f0f0f0;
  color: #667eea;
  padding: 0.25rem 0.75rem;
  border-radius: 15px;
  font-size: 0.85rem;
}
</style>

