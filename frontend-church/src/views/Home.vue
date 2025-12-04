<template>
  <div class="home">
    <section class="hero">
      <div class="hero-content">
        <h1>歡迎來到我們的教會</h1>
        <p class="hero-subtitle">我們是一個充滿愛與關懷的信仰社群</p>
        <router-link to="/about" class="btn">了解更多</router-link>
      </div>
    </section>

    <section class="section">
      <div class="container">
        <h2 class="section-title">最新消息</h2>
        <div class="news-grid">
          <div class="card" v-for="news in newsList" :key="news.id">
            <h3>{{ news.title }}</h3>
            <p class="news-date">{{ news.date }}</p>
            <p>{{ news.content }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="section bg-light">
      <div class="container">
        <h2 class="section-title">聚會時間</h2>
        <div class="schedule-grid">
          <div class="card">
            <h3>主日崇拜</h3>
            <p><strong>時間：</strong>每週日上午 10:00</p>
            <p><strong>地點：</strong>教會大堂</p>
          </div>
          <div class="card">
            <h3>查經班</h3>
            <p><strong>時間：</strong>每週三晚上 7:30</p>
            <p><strong>地點：</strong>教會副堂</p>
          </div>
          <div class="card">
            <h3>禱告會</h3>
            <p><strong>時間：</strong>每週五晚上 7:00</p>
            <p><strong>地點：</strong>教會禱告室</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const newsList = ref([
  {
    id: 1,
    title: '聖誕節特別聚會',
    date: '2024-12-20',
    content: '歡迎大家參加我們的聖誕節特別聚會，一起慶祝這個美好的節日。'
  },
  {
    id: 2,
    title: '新年度讀經計劃',
    date: '2024-12-15',
    content: '新的一年即將開始，我們邀請大家一起參與年度讀經計劃。'
  },
  {
    id: 3,
    title: '社區服務活動',
    date: '2024-12-10',
    content: '我們將舉辦社區服務活動，歡迎弟兄姊妹一起參與。'
  }
])
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 6rem 2rem;
  text-align: center;
}

.hero-content h1 {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.hero-subtitle {
  font-size: 1.5rem;
  margin-bottom: 2rem;
  opacity: 0.9;
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.news-date {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
}

.bg-light {
  background: #f8f9fa;
  padding: 4rem 0;
}

.card h3 {
  color: #667eea;
  margin-bottom: 1rem;
}

.card p {
  margin-bottom: 0.5rem;
}
</style>

