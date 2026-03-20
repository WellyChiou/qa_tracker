<template>
  <AdminLayout>
    <div class="system-page">
      <div class="page-header">
        <div>
          <h2>{{ title }}</h2>
          <p>{{ subtitle }}</p>
        </div>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>目前狀態</span>
          <strong>骨架完成</strong>
          <p>菜單、路由與權限已接軌 invest ACL。</p>
        </article>
        <article class="overview-card">
          <span>頁面代碼</span>
          <strong>{{ pageCode }}</strong>
          <p>本輪先提供可驗證入口，不提前擴張後端 API。</p>
        </article>
      </section>

      <section class="card surface-card placeholder-card">
        <h3>功能建置中</h3>
        <p>
          此頁目前為 admin baseline 骨架，後續會依優先順序補齊查詢、列表、分頁與編輯功能。
        </p>
      </section>

      <section class="card surface-card reminder-card">
        <h3>說明</h3>
        <ul>
          <li>本輪僅完成前端骨架頁、route 與 ACL 菜單對齊。</li>
          <li>尚未新增任何系統設定後端 API。</li>
          <li>後續深做時會沿用 paged / options / all 的一致設計。</li>
        </ul>
      </section>
    </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue'

defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    required: true
  },
  pageCode: {
    type: String,
    required: true
  }
})
</script>

<style scoped>
.system-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-header h2 {
  margin: 0;
  font-size: 1.75rem;
}

.page-header p {
  margin: 6px 0 0;
  color: var(--text-secondary);
}

.overview-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.overview-card {
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 14px;
  background: #fff;
}

.overview-card span {
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.overview-card strong {
  display: block;
  margin: 6px 0;
  font-size: 1.2rem;
}

.overview-card p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.overview-card--accent {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(6, 182, 212, 0.1));
}

.placeholder-card,
.reminder-card {
  padding: 14px;
}

.placeholder-card h3,
.reminder-card h3 {
  margin: 0 0 8px;
}

.placeholder-card p {
  margin: 0;
  color: var(--text-secondary);
}

.reminder-card ul {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
}
</style>
