<template>
  <AdminLayout>
    <div class="invest-dashboard-page">
      <div class="page-header">
        <div>
          <h2>股票持股總覽</h2>
          <p>給新手的持股整理與基礎風險追蹤入口（Step 1 基礎版）。</p>
        </div>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>總成本</span>
          <strong>{{ formatMoney(overview.totalCost) }}</strong>
          <p>目前持股投入成本總和。</p>
        </article>
        <article class="overview-card">
          <span>總市值</span>
          <strong>{{ formatMoney(overview.totalMarketValue) }}</strong>
          <p>以最新日行情估算的持股市值。</p>
        </article>
        <article class="overview-card">
          <span>未實現損益</span>
          <strong :class="overview.totalPnL >= 0 ? 'text-up' : 'text-down'">
            {{ formatSignedMoney(overview.totalPnL) }}
          </strong>
          <p>{{ formatPercent(overview.totalPnLPercent) }}</p>
        </article>
        <article class="overview-card">
          <span>持股檔數</span>
          <strong>{{ overview.holdingCount || 0 }}</strong>
          <p>目前啟用中的持股筆數。</p>
        </article>
      </section>

      <section class="card surface-card quick-links">
        <div class="section-header">
          <h3>快速功能</h3>
        </div>
        <div class="link-grid">
          <router-link to="/portfolios" class="quick-link">持股中心</router-link>
          <router-link to="/stocks" class="quick-link">股票主資料</router-link>
          <router-link to="/stock-price-dailies" class="quick-link">每日行情資料</router-link>
          <router-link to="/daily-reports" class="quick-link">每日報告</router-link>
          <router-link to="/alerts" class="quick-link">警示事件</router-link>
        </div>
      </section>

      <section class="card surface-card latest-alert-summary">
        <div class="section-header">
          <h3>警示摘要</h3>
        </div>
        <div class="latest-alert-grid">
          <article class="report-item">
            <span>今日警示總數</span>
            <strong>{{ Number(overview.alertSummary?.totalActiveAlerts || 0) }}</strong>
          </article>
          <article class="report-item">
            <span>高嚴重警示</span>
            <strong>{{ Number(overview.alertSummary?.highSeverityCount || 0) }}</strong>
            <p>高嚴重定義：停損 / 短時異常下跌</p>
          </article>
          <article class="report-item">
            <span>最新觸發時間</span>
            <strong>{{ formatDateTime(overview.alertSummary?.latestTriggeredAt) }}</strong>
          </article>
        </div>
      </section>

      <section class="card surface-card latest-report">
        <div class="section-header">
          <h3>最新每日報告摘要</h3>
        </div>
        <div v-if="!overview.latestDailyReportSummary" class="empty-state">
          尚未產生每日報告，可先到「每日報告」頁手動執行批次。
        </div>
        <div v-else class="latest-report-grid">
          <article class="report-item">
            <span>報告日期</span>
            <strong>{{ overview.latestDailyReportSummary.reportDate || '-' }}</strong>
          </article>
          <article class="report-item">
            <span>資料基準交易日</span>
            <strong>{{ overview.latestDailyReportSummary.dataAsOfTradeDate || '-' }}</strong>
          </article>
          <article class="report-item">
            <span>高風險 / 極高風險</span>
            <strong>{{ overview.latestDailyReportSummary.highRiskCount || 0 }} / {{ overview.latestDailyReportSummary.criticalRiskCount || 0 }}</strong>
          </article>
        </div>
      </section>

      <section class="card surface-card novice-notice">
        <div class="section-header">
          <h3>新手提醒</h3>
        </div>
        <ul>
          <li>本系統為輔助工具，不保證獲利。</li>
          <li>系統結果不可取代自主判斷。</li>
          <li>單一訊號不代表一定上漲或下跌。</li>
          <li>不要因短線漲跌而情緒化操作。</li>
          <li>不要盲目攤平、不要追高。</li>
        </ul>
      </section>

      <div v-if="(overview.holdingCount || 0) === 0" class="empty-state card surface-card">
        目前沒有持股資料，請先到「持股中心」新增持股。
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { investApiService } from '@/composables/useInvestApi'
import { toast } from '@shared/composables/useToast'

const overview = reactive({
  totalCost: 0,
  totalMarketValue: 0,
  totalPnL: 0,
  totalPnLPercent: 0,
  holdingCount: 0,
  riskDistribution: [],
  recommendationDistribution: [],
  topRiskHoldings: [],
  alertSummary: {},
  latestDailyReportSummary: null
})

const loadOverview = async () => {
  try {
    const data = await investApiService.getDashboardOverview()
    Object.assign(overview, {
      totalCost: Number(data?.totalCost || 0),
      totalMarketValue: Number(data?.totalMarketValue || 0),
      totalPnL: Number(data?.totalPnL || 0),
      totalPnLPercent: Number(data?.totalPnLPercent || 0),
      holdingCount: Number(data?.holdingCount || 0),
      riskDistribution: data?.riskDistribution || [],
      recommendationDistribution: data?.recommendationDistribution || [],
      topRiskHoldings: data?.topRiskHoldings || [],
      alertSummary: data?.alertSummary || {},
      latestDailyReportSummary: data?.latestDailyReportSummary || null
    })
  } catch (error) {
    toast.error(`載入持股總覽失敗: ${error.message || '未知錯誤'}`)
  }
}

const formatMoney = (value) => `$${Number(value || 0).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const formatSignedMoney = (value) => {
  const number = Number(value || 0)
  const sign = number >= 0 ? '+' : '-'
  return `${sign}${formatMoney(Math.abs(number))}`
}
const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`
const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

onMounted(loadOverview)
</script>

<style scoped>
.invest-dashboard-page { display: flex; flex-direction: column; gap: 14px; }
.page-header h2 { margin: 0; font-size: 1.75rem; }
.page-header p { margin: 6px 0 0; color: var(--text-secondary); }
.overview-strip { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 12px; }
.overview-card { border: 1px solid var(--border-color); border-radius: 14px; padding: 14px; background: #fff; }
.overview-card span { color: var(--text-secondary); font-size: 0.85rem; }
.overview-card strong { display: block; margin: 6px 0; font-size: 1.3rem; }
.overview-card p { margin: 0; color: var(--text-secondary); font-size: 0.86rem; }
.overview-card--accent { background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(6, 182, 212, 0.1)); }
.quick-links, .novice-notice { padding: 14px; }
.section-header h3 { margin: 0 0 10px; }
.link-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; }
.quick-link { text-decoration: none; color: var(--text-primary); border: 1px solid var(--border-color); border-radius: 12px; padding: 10px 12px; background: #fff; font-weight: 700; }
.quick-link:hover { background: var(--bg-primary); }
.latest-report { padding: 14px; }
.latest-report-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; }
.latest-alert-summary { padding: 14px; }
.latest-alert-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; }
.report-item { border: 1px solid var(--border-color); border-radius: 12px; background: #fff; padding: 10px 12px; }
.report-item span { color: var(--text-secondary); font-size: 0.82rem; }
.report-item strong { display: block; margin-top: 4px; }
.report-item p { margin: 6px 0 0; color: var(--text-secondary); font-size: 0.82rem; }
.novice-notice ul { margin: 0; padding-left: 18px; color: var(--text-secondary); }
.empty-state { padding: 16px; text-align: center; color: var(--text-secondary); }
.text-up { color: #0f766e; }
.text-down { color: #b91c1c; }
</style>
