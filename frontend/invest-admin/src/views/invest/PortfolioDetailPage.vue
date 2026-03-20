<template>
  <AdminLayout>
    <div class="portfolio-detail-page">
      <div class="page-header">
        <div>
          <h2>持股明細</h2>
          <p>此頁為 invest 持股唯一正式明細頁，提供風險分析與白話提醒。</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" @click="goBack">返回列表</button>
          <button class="btn btn-primary" @click="recalculateRisk" :disabled="recalculating">
            {{ recalculating ? '重算中...' : '手動重算風險' }}
          </button>
        </div>
      </div>

      <section v-if="portfolio" class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>股票</span>
          <strong>{{ portfolio.ticker }} - {{ portfolio.stockName }}</strong>
          <p>{{ portfolio.market }} / 持股編號 {{ portfolio.id }}</p>
        </article>
        <article class="overview-card">
          <span>總成本</span>
          <strong>{{ formatMoney(portfolio.totalCost) }}</strong>
          <p>均價 {{ formatMoney(portfolio.avgCost) }}，股數 {{ formatQty(portfolio.quantity) }}</p>
        </article>
        <article class="overview-card">
          <span>市值 / 損益</span>
          <strong>{{ formatMoney(portfolio.marketValue) }}</strong>
          <p :class="Number(portfolio.unrealizedProfitLoss) >= 0 ? 'text-up' : 'text-down'">
            {{ formatSignedMoney(portfolio.unrealizedProfitLoss) }} ({{ formatPercent(portfolio.unrealizedProfitLossPercent) }})
          </p>
        </article>
      </section>

      <section class="card surface-card risk-summary">
        <h3>最新風險分析</h3>
        <div v-if="riskLoading" class="empty-state">載入風險分析中...</div>
        <div v-else-if="!riskResult" class="empty-state">
          尚未分析，可按「手動重算風險」產生結果。
        </div>
        <div v-else>
          <div class="risk-metrics">
            <div class="metric">
              <span>Risk Score</span>
              <strong>{{ riskResult.riskScore }}</strong>
            </div>
            <div class="metric">
              <span>Risk Level</span>
              <strong>{{ formatRiskLevel(riskResult.riskLevel) }}</strong>
            </div>
            <div class="metric">
              <span>Recommendation</span>
              <strong>{{ formatRecommendation(riskResult.recommendation) }}</strong>
            </div>
          </div>

          <div class="recommendation-card">
            <h4>建議摘要</h4>
            <p>{{ riskResult.summary }}</p>
            <ul>
              <li v-for="reason in riskResult.reasons || []" :key="`${reason.ruleCode}-${reason.reasonTitle}`">
                {{ reason.reasonTitle }}（影響分數 {{ reason.scoreImpact }}）
              </li>
            </ul>
            <p class="disclaimer">{{ riskResult.disclaimer }}</p>
          </div>

          <div class="reasons-list">
            <h4>風險原因明細</h4>
            <div v-if="(riskResult.riskReasons || []).length === 0" class="empty-state">
              目前沒有觸發風險規則。
            </div>
            <ul v-else>
              <li v-for="reason in riskResult.riskReasons" :key="`${reason.ruleCode}-${reason.reasonTitle}`">
                <div class="reason-head">
                  <strong>{{ reason.reasonTitle }}</strong>
                  <span>+{{ reason.scoreImpact }}</span>
                </div>
                <p>{{ reason.reasonDetail }}</p>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <section class="card surface-card alert-setting-card">
        <h3>警示設定</h3>
        <div v-if="alertSettingLoading" class="empty-state">載入警示設定中...</div>
        <div v-else class="alert-setting-form">
          <div class="form-grid">
            <div class="form-group">
              <label>停損價（可留空）</label>
              <input
                v-model="alertForm.stopLossPrice"
                type="number"
                step="0.0001"
                min="0"
                placeholder="例如 85.5000"
              />
              <small>留空代表不檢查「跌破停損價」。</small>
            </div>
            <div class="form-group">
              <label>跌幅門檻 %（可留空）</label>
              <input
                v-model="alertForm.alertDropPercent"
                type="number"
                step="0.01"
                min="0"
                placeholder="例如 4.50"
              />
              <small>留空代表不檢查「當日跌幅超過門檻」。</small>
            </div>
            <div class="form-group form-group--toggle">
              <label>啟用警示</label>
              <label class="toggle-inline">
                <input v-model="alertForm.enabled" type="checkbox" />
                <span>{{ alertForm.enabled ? '啟用中' : '停用中' }}</span>
              </label>
              <small>停用時，三種警示都不檢查。</small>
            </div>
          </div>
          <div class="alert-setting-actions">
            <button class="btn btn-primary" :disabled="savingAlertSetting" @click="saveAlertSetting">
              {{ savingAlertSetting ? '儲存中...' : '儲存警示設定' }}
            </button>
          </div>
          <div class="alert-setting-note">
            <p>ABNORMAL_DROP（短時異常下跌）在啟用警示時會獨立生效，不依賴停損價與跌幅門檻。</p>
          </div>
        </div>
      </section>

      <section class="card surface-card">
        <h3>歷史行情（最近 30 筆）</h3>
        <div v-if="!portfolio || (portfolio.priceHistory || []).length === 0" class="empty-state">
          尚無歷史行情資料
        </div>
        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>日期</th>
                <th>開盤</th>
                <th>最高</th>
                <th>最低</th>
                <th>收盤</th>
                <th>漲跌%</th>
                <th>成交量</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in portfolio.priceHistory" :key="item.id">
                <td>{{ item.tradeDate }}</td>
                <td>{{ formatMoney(item.openPrice) }}</td>
                <td>{{ formatMoney(item.highPrice) }}</td>
                <td>{{ formatMoney(item.lowPrice) }}</td>
                <td>{{ formatMoney(item.closePrice) }}</td>
                <td :class="Number(item.changePercent) >= 0 ? 'text-up' : 'text-down'">{{ formatPercent(item.changePercent) }}</td>
                <td>{{ formatVolume(item.volume) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="card surface-card reminders">
        <h3>新手提醒</h3>
        <ul>
          <li>本系統為輔助工具，不保證獲利。</li>
          <li>系統結果不可取代自主判斷，單一訊號不代表一定上漲或下跌。</li>
          <li>不要因短線漲跌而情緒化操作，也不要盲目攤平或追高。</li>
        </ul>
      </section>
    </div>
  </AdminLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from '@shared/composables/useToast'
import AdminLayout from '@/components/AdminLayout.vue'
import { investApiService } from '@/composables/useInvestApi'

const route = useRoute()
const router = useRouter()
const portfolio = ref(null)
const riskResult = ref(null)
const riskLoading = ref(false)
const recalculating = ref(false)
const alertSettingLoading = ref(false)
const savingAlertSetting = ref(false)
const alertForm = ref({
  stopLossPrice: '',
  alertDropPercent: '',
  enabled: false
})

const portfolioId = Number(route.params.id)

const loadPortfolio = async () => {
  portfolio.value = await investApiService.getPortfolioById(portfolioId)
}

const loadLatestRisk = async () => {
  riskLoading.value = true
  try {
    riskResult.value = await investApiService.getLatestPortfolioRiskResult(portfolioId)
  } catch (error) {
    riskResult.value = null
  } finally {
    riskLoading.value = false
  }
}

const loadAlertSetting = async () => {
  alertSettingLoading.value = true
  try {
    const setting = await investApiService.getPortfolioAlertSetting(portfolioId)
    alertForm.value = {
      stopLossPrice: setting?.stopLossPrice ?? '',
      alertDropPercent: setting?.alertDropPercent ?? '',
      enabled: Boolean(setting?.enabled)
    }
  } finally {
    alertSettingLoading.value = false
  }
}

const saveAlertSetting = async () => {
  savingAlertSetting.value = true
  try {
    const payload = {
      stopLossPrice: alertForm.value.stopLossPrice === '' ? null : Number(alertForm.value.stopLossPrice),
      alertDropPercent: alertForm.value.alertDropPercent === '' ? null : Number(alertForm.value.alertDropPercent),
      enabled: Boolean(alertForm.value.enabled)
    }
    const saved = await investApiService.updatePortfolioAlertSetting(portfolioId, payload)
    alertForm.value = {
      stopLossPrice: saved?.stopLossPrice ?? '',
      alertDropPercent: saved?.alertDropPercent ?? '',
      enabled: Boolean(saved?.enabled)
    }
    toast.success('警示設定已更新')
  } catch (error) {
    toast.error(`警示設定儲存失敗: ${error.message || '未知錯誤'}`)
  } finally {
    savingAlertSetting.value = false
  }
}

const recalculateRisk = async () => {
  recalculating.value = true
  try {
    riskResult.value = await investApiService.recalculatePortfolioRisk(portfolioId)
    toast.success('風險分析重算完成')
    await loadPortfolio()
  } catch (error) {
    toast.error(`風險分析重算失敗: ${error.message || '未知錯誤'}`)
  } finally {
    recalculating.value = false
  }
}

const goBack = () => {
  router.push('/portfolios')
}

const formatMoney = (value) => `$${Number(value || 0).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 4 })}`
const formatSignedMoney = (value) => {
  const number = Number(value || 0)
  const sign = number >= 0 ? '+' : '-'
  return `${sign}$${Math.abs(number).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`
const formatQty = (value) => Number(value || 0).toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 4 })
const formatVolume = (value) => Number(value || 0).toLocaleString('zh-TW')

const formatRiskLevel = (code) => {
  const map = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    CRITICAL: '極高'
  }
  return map[code] || code || '尚未分析'
}

const formatRecommendation = (code) => {
  const map = {
    HOLD: '續抱',
    WATCH: '觀察',
    REDUCE: '減碼',
    STOP_LOSS_CHECK: '停損檢查'
  }
  return map[code] || code || '尚未分析'
}

onMounted(async () => {
  try {
    await loadPortfolio()
    await loadLatestRisk()
    await loadAlertSetting()
  } catch (error) {
    toast.error(`載入持股明細失敗: ${error.message || '未知錯誤'}`)
    router.push('/portfolios')
  }
})
</script>

<style scoped>
.portfolio-detail-page { display: flex; flex-direction: column; gap: 14px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.page-header h2 { margin: 0; font-size: 1.7rem; }
.page-header p { margin: 6px 0 0; color: var(--text-secondary); }
.header-actions { display: flex; gap: 8px; }
.overview-strip { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.overview-card { border: 1px solid var(--border-color); border-radius: 14px; padding: 14px; background: #fff; }
.overview-card--accent { background: linear-gradient(135deg, rgba(15, 118, 110, 0.14), rgba(59, 130, 246, 0.12)); }
.overview-card span { color: var(--text-secondary); font-size: 0.85rem; }
.overview-card strong { display: block; margin: 6px 0; font-size: 1.1rem; }
.overview-card p { margin: 0; font-size: 0.88rem; color: var(--text-secondary); }
.risk-summary { padding: 14px; }
.risk-summary h3 { margin: 0 0 10px; }
.risk-metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 10px; margin-bottom: 12px; }
.metric { border: 1px solid var(--border-color); border-radius: 10px; padding: 10px; background: #f8fafc; }
.metric span { color: var(--text-secondary); font-size: 0.82rem; display: block; }
.metric strong { margin-top: 6px; display: block; font-size: 1.06rem; }
.recommendation-card { border: 1px solid #bfdbfe; background: #eff6ff; border-radius: 12px; padding: 12px; margin-bottom: 12px; }
.recommendation-card h4 { margin: 0 0 8px; }
.recommendation-card p { margin: 0 0 8px; }
.recommendation-card ul { margin: 0; padding-left: 18px; }
.disclaimer { color: #475569; font-size: 0.86rem; }
.reasons-list h4 { margin: 0 0 8px; }
.reasons-list ul { margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 10px; }
.reasons-list li { border: 1px solid var(--border-color); border-radius: 10px; padding: 10px; background: #fff; }
.reason-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.reason-head span { color: #b91c1c; font-weight: 600; }
.reason-head + p { margin: 8px 0 0; color: #334155; }
.alert-setting-card { padding: 14px; }
.alert-setting-form { display: flex; flex-direction: column; gap: 12px; }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: 600; }
.form-group small { display: block; margin-top: 6px; color: var(--text-secondary); font-size: 0.82rem; }
.form-group--toggle { display: flex; flex-direction: column; justify-content: center; }
.toggle-inline { display: inline-flex; gap: 8px; align-items: center; font-weight: 600; }
.alert-setting-actions { display: flex; justify-content: flex-end; }
.alert-setting-note { border: 1px solid #dbeafe; background: #eff6ff; border-radius: 10px; padding: 10px; color: #1e3a8a; font-size: 0.9rem; }
.alert-setting-note p { margin: 0; }
.table-wrap { overflow-x: auto; }
.reminders ul { margin: 0; padding-left: 18px; color: #334155; }
.empty-state { text-align: center; color: var(--text-secondary); padding: 12px; }
.text-up { color: #0f766e; }
.text-down { color: #b91c1c; }
</style>
