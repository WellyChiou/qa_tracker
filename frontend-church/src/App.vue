<template>
  <div id="app">
    <nav class="navbar">
      <div class="nav-container">
        <div class="nav-logo">
          <router-link to="/" class="logo-link">
            <img src="/images/logo.png" alt="極光教會 Logo" class="logo-image" />
            <span class="logo-text">極光教會-PLC</span>
          </router-link>
        </div>
        <ul class="nav-menu">
          <li><router-link to="/">首頁</router-link></li>
          <li><router-link to="/about">關於我們</router-link></li>
          <li><router-link to="/activities">活動</router-link></li>
          <li><router-link to="/service-schedule">服事安排</router-link></li>
          <li><router-link to="/contact">聯絡我們</router-link></li>
        </ul>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
    <footer class="footer">
      <div class="footer-container">
        <p>&copy; 2026 極光教會網站. 版權所有.</p>
      </div>
    </footer>
    <LoadingSpinner />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useLoading } from '@/composables/useLoading'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

// 初始化 loading 系統（註冊回調到 API 服務）
useLoading()
</script>

<style scoped>
.navbar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 1rem 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-logo a {
  font-size: 1.5rem;
  font-weight: bold;
  color: white;
  text-decoration: none;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-image {
  width: 50px;
  height: 50px;
  object-fit: contain;
  border-radius: 50%;
}

.logo-text {
  font-size: 1.5rem;
  font-weight: bold;
  color: white;
}

.nav-menu {
  display: flex;
  list-style: none;
  gap: 2rem;
  margin: 0;
  padding: 0;
}

.nav-menu a {
  color: white;
  text-decoration: none;
  font-weight: 500;
  transition: opacity 0.3s;
}

.nav-menu a:hover,
.nav-menu a.router-link-active {
  opacity: 0.8;
  text-decoration: underline;
}

.main-content {
  min-height: calc(100vh - 200px);
}

.footer {
  background: #2c3e50;
  color: white;
  padding: 2rem 0;
  margin-top: 4rem;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  text-align: center;
}
</style>

