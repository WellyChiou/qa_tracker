<template>
  <div class="home">
    <section class="hero">
      <div class="hero-background">
        <img src="/images/word-cloud.png" alt="極光教會文字雲" class="hero-bg-image" />
      </div>
      <div class="hero-content">
        <h1>Welcome to Power Light Church!</h1>
        <p class="hero-subtitle">歡迎您加入PLC極光這個溫暖大家庭，讓我們在基督的愛及聖經真理中成長茁壯，邁向蒙福豐盛光明的人生！</p>
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
            <p style="white-space: pre-line;">{{ news.content }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="section bg-light">
      <div class="container">
        <h2 class="section-title">聚會時間</h2>
        <div class="schedule-grid">
          <div class="card">
            <h3>晚崇聚會</h3>
            <p><strong>時間：</strong>每週六晚上 7:00</p>
            <p><strong>地點：</strong>教會大堂</p>
          </div>
          <div class="card">
            <h3>早崇聚會</h3>
            <p><strong>時間：</strong>每週日早上 10:00</p>
            <p><strong>地點：</strong>教會大堂</p>
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
    title: '12月份週日信息講員',
    date: '2025-12',
    content: '12/7 週日 早上10:00 - 傅金源 牧師\n12/14 週日 早上10:00 - 呂代豪 牧師\n12/21 週日 早上10:00 - 王天佑 牧師\n12/28 週日 早上10:00 - 黃仁鵬 傳道（聯合主日）'
  },
  {
    id: 2,
    title: '12月份週六信息講員',
    date: '2025-12',
    content: '12/6 週六 晚間 7:00 - 胡斯漢 老師（名人講座2）\n12/13 週六 晚間 7:00 - 王倩倩 老師、曾俊盛 總經理（名人講座3）\n12/20 週六 下午 4:00 - 聖誕慶典\n12/28 週日 上午 10:00 - 黃仁鵬 傳道（聯合主日）'
  },
  {
    id: 3,
    title: '聯合受洗主日',
    date: '2025-12-28',
    content: '12月28日（週日）早上10:00，聯合受洗主日，歡迎弟兄姊妹一同參與這個重要的時刻。'
  },
  {
    id: 4,
    title: '聖誕慶典',
    date: '2025-12-20',
    content: '12月20日（週六）下午 4:00，聖誕慶典，一起慶祝這個美好的節日。'
  }
])
</script>

<style scoped>
.hero {
  position: relative;
  color: white;
  padding: 6rem 2rem;
  text-align: center;
  overflow: hidden;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  opacity: 0.3;
}

.hero-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.hero-content {
  position: relative;
  z-index: 1;
  background: rgba(0, 0, 0, 0.3);
  padding: 2rem;
  border-radius: 10px;
  backdrop-filter: blur(5px);
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

