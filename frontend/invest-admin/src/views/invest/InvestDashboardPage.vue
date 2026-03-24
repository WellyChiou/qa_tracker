<template>
  <AdminLayout>
    <div class="invest-dashboard-page">
      <div class="page-header">
        <div>
          <h2>決策 Dashboard</h2>
          <p>每天先看資料是否可靠，再判讀持股強弱與機會訊號。</p>
        </div>
        <button class="btn btn-secondary" :disabled="loadingPage" @click="reloadAll">
          {{ loadingPage ? '載入中...' : '重新整理' }}
        </button>
      </div>

      <section class="card surface-card decision-focus-card">
        <div class="section-header">
          <h3>今日決策重點（Top 1）</h3>
        </div>
        <p class="focus-text">{{ decisionFocus }}</p>
        <div class="risk-level-row">
          <strong>風險等級：{{ riskLevel.label }}</strong>
          <ul class="risk-reason-list">
            <li v-for="(reason, idx) in riskLevel.reasons" :key="`risk-reason-${idx}`">{{ reason }}</li>
          </ul>
        </div>
      </section>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>總資產概況</span>
          <strong>{{ formatMoney(overview.totalMarketValue) }}</strong>
          <p>成本 {{ formatMoney(overview.totalCost) }}，損益 {{ formatSignedMoney(overview.totalPnL) }}（{{ formatPercent(overview.totalPnLPercent) }}）</p>
          <p v-if="overview.baseCurrency === 'TWD'" class="confidence-note">
            總額以 TWD 顯示，USD 依設定匯率 {{ Number(overview.usdToTwdRate || 0).toFixed(4) }} 換算。
          </p>
        </article>
        <article class="overview-card">
          <span>持股檔數</span>
          <strong>{{ overview.holdingCount || 0 }}</strong>
          <p>決策基礎以「我的持股」為主。</p>
        </article>
        <article class="overview-card">
          <span>今日 ACTIVE 機會訊號</span>
          <strong>{{ activeSignals.length }}</strong>
          <p>僅供觀察，不構成買進指令。</p>
        </article>
        <article class="overview-card">
          <span>決策可信度</span>
          <strong>{{ decisionCredibilityLabel }}</strong>
          <p class="confidence-metrics">
            GOOD {{ qualitySummary.good }} / PARTIAL {{ qualitySummary.partial }} /
            STALE {{ qualitySummary.stale }} / INSUFFICIENT {{ qualitySummary.insufficient }}
          </p>
          <p class="confidence-note">資料品質將直接影響決策可信度。</p>
        </article>
      </section>

      <section class="card surface-card">
        <div class="section-header">
          <h3>Data As Of</h3>
        </div>
        <div class="data-asof-grid">
          <article class="asof-item">
            <span>最新交易日</span>
            <strong>{{ latestTradeDate || '-' }}</strong>
            <p>來源：持股強弱快照 / 機會訊號 / 日報基準日。</p>
          </article>
          <article class="asof-item">
            <span>最新 Price Update</span>
            <strong :class="statusClass(priceUpdateJob?.lastRunStatus)">{{ formatStatus(priceUpdateJob?.lastRunStatus) }}</strong>
            <p>{{ formatDateTime(priceUpdateJob?.lastRunAt) }}</p>
          </article>
          <article class="asof-item">
            <span>最新 Market Analysis</span>
            <strong :class="statusClass(marketAnalysisJob?.lastRunStatus)">{{ formatStatus(marketAnalysisJob?.lastRunStatus) }}</strong>
            <p>{{ formatDateTime(marketAnalysisJob?.lastRunAt) }}</p>
          </article>
          <article class="asof-item">
            <span>最新每日報告</span>
            <strong>{{ overview.latestDailyReportSummary?.reportDate || '-' }}</strong>
            <p>data as of：{{ overview.latestDailyReportSummary?.dataAsOfTradeDate || '-' }}</p>
          </article>
        </div>
      </section>

      <section class="card surface-card">
        <div class="section-header strategy-impact-header">
          <h3>策略影響（Phase 3）</h3>
          <span class="strategy-version-chip">策略版本 v{{ strategyImpact.currentVersion || '-' }}</span>
        </div>
        <div class="strategy-impact-grid">
          <article class="strategy-impact-item">
            <span>分析版本一致性</span>
            <strong :class="strategyImpact.isAligned ? 'text-up' : 'text-caution'">
              {{ strategyImpact.isAligned ? '一致' : '待同步' }}
            </strong>
            <p>{{ strategyImpact.alignmentSummary }}</p>
          </article>
          <article class="strategy-impact-item">
            <span>門檻摘要</span>
            <ul class="strategy-threshold-list">
              <li v-for="(item, idx) in strategyImpact.thresholdSummary" :key="`threshold-${idx}`">{{ item }}</li>
            </ul>
          </article>
          <article class="strategy-impact-item">
            <span>分布影響摘要</span>
            <ul class="strategy-threshold-list">
              <li v-for="(item, idx) in strategyImpact.impactSummary" :key="`impact-${idx}`">{{ item }}</li>
            </ul>
          </article>
        </div>
      </section>

      <section class="card surface-card">
        <div class="section-header">
          <h3>我的持股強弱摘要</h3>
        </div>
        <p v-if="strengthConclusion" class="section-conclusion">{{ strengthConclusion }}</p>
        <div v-if="holdingSnapshots.length === 0" class="empty-state">
          尚未有持股強弱分析資料，請先執行「更新我的持股行情」與「執行市場分析」。
        </div>
        <div v-else class="strength-grid">
          <article class="strength-panel">
            <header>
              <h4>偏強持股</h4>
              <small>STRONG / GOOD</small>
            </header>
            <div v-if="strongHoldings.length === 0" class="empty-state compact">目前沒有偏強持股。</div>
            <ul v-else class="signal-list">
              <li v-for="row in strongHoldings" :key="row.id">
                <div class="row-main">
                  <strong>{{ row.ticker }} {{ row.stockName }}</strong>
                  <span class="chip chip-strong">{{ row.strengthScore ?? '-' }} 分</span>
                </div>
                <p>{{ row.summary || '偏強但仍需觀察風險。' }}</p>
                <div class="row-meta">
                  <span>{{ formatStrengthLevel(row.strengthLevel) }}</span>
                  <span :class="qualityClass(row.dataQuality)">{{ formatDataQuality(row.dataQuality) }}</span>
                  <span>{{ formatDecisionTag(row.recommendation) }}</span>
                </div>
              </li>
            </ul>
          </article>

          <article class="strength-panel">
            <header>
              <h4>偏弱持股</h4>
              <small>NEUTRAL / WEAK</small>
            </header>
            <div v-if="weakHoldings.length === 0" class="empty-state compact">目前沒有偏弱持股。</div>
            <ul v-else class="signal-list">
              <li v-for="row in weakHoldings" :key="row.id">
                <div class="row-main">
                  <strong>{{ row.ticker }} {{ row.stockName }}</strong>
                  <span class="chip chip-weak">{{ row.strengthScore ?? '-' }} 分</span>
                </div>
                <p>{{ row.summary || '偏弱，暫不判斷方向。' }}</p>
                <div class="row-meta">
                  <span>{{ formatStrengthLevel(row.strengthLevel) }}</span>
                  <span :class="qualityClass(row.dataQuality)">{{ formatDataQuality(row.dataQuality) }}</span>
                  <span>{{ formatDecisionTag(row.recommendation) }}</span>
                </div>
              </li>
            </ul>
          </article>
        </div>
      </section>

      <section class="card surface-card">
        <div class="section-header">
          <h3>今日最值得關注</h3>
        </div>
        <div v-if="topOpportunitySignals.length === 0" class="empty-state">
          目前沒有 ACTIVE 機會訊號，可先執行市場分析或觀察後續條件變化。
        </div>
        <ul v-else class="signal-list">
          <li v-for="row in topOpportunitySignals" :key="row.id">
            <div class="row-main">
              <strong>{{ row.ticker }} {{ row.stockName }}</strong>
              <span class="chip">{{ row.signalScore ?? '-' }} 分</span>
            </div>
            <p>{{ row.summary || '-' }}</p>
            <div class="row-meta">
              <span>{{ formatDecisionTag(row.recommendation) }}</span>
              <span>{{ formatSignalType(row.signalType) }}</span>
              <span>{{ row.tradeDate || '-' }}</span>
            </div>
            <ul v-if="Array.isArray(row.topReasons) && row.topReasons.length > 0" class="reason-list">
              <li v-for="(reason, idx) in row.topReasons.slice(0, 2)" :key="`${row.id}-${idx}`">
                {{ reason.reasonTitle }}：{{ reason.reasonDetail }}
              </li>
            </ul>
          </li>
        </ul>
      </section>

      <section class="card surface-card">
        <div class="section-header">
          <h3>風險與資料品質提醒</h3>
        </div>
        <div class="quality-grid">
          <article class="quality-item">
            <span>GOOD</span>
            <strong>{{ qualitySummary.good }}</strong>
          </article>
          <article class="quality-item">
            <span>PARTIAL</span>
            <strong>{{ qualitySummary.partial }}</strong>
          </article>
          <article class="quality-item">
            <span>STALE</span>
            <strong>{{ qualitySummary.stale }}</strong>
          </article>
          <article class="quality-item">
            <span>INSUFFICIENT</span>
            <strong>{{ qualitySummary.insufficient }}</strong>
          </article>
          <article class="quality-item">
            <span>高風險 / 極高風險</span>
            <strong>{{ overview.latestDailyReportSummary?.highRiskCount || 0 }} / {{ overview.latestDailyReportSummary?.criticalRiskCount || 0 }}</strong>
          </article>
          <article class="quality-item">
            <span>警示總數 / 高嚴重</span>
            <strong>{{ Number(overview.alertSummary?.totalActiveAlerts || 0) }} / {{ Number(overview.alertSummary?.highSeverityCount || 0) }}</strong>
          </article>
        </div>
      </section>

      <section class="card surface-card">
        <div class="section-header">
          <h3>快速操作</h3>
        </div>
        <div class="action-grid">
          <button class="btn btn-primary" :disabled="!canRunPriceUpdate || actionLoading.priceUpdate" @click="runPriceUpdate">
            {{ actionLoading.priceUpdate ? '更新中...' : '更新我的持股行情' }}
          </button>
          <div class="backfill-action">
            <select v-model.number="backfillDays" class="select-control" :disabled="!canRunPriceBackfill || actionLoading.backfill">
              <option :value="30">回補 30 日</option>
              <option :value="60">回補 60 日</option>
            </select>
            <button class="btn btn-primary" :disabled="!canRunPriceBackfill || actionLoading.backfill" @click="runPriceBackfill">
              {{ actionLoading.backfill ? '回補中...' : '回補歷史行情' }}
            </button>
          </div>
          <button class="btn btn-primary" :disabled="!canRunMarketAnalysis || actionLoading.marketAnalysis" @click="runMarketAnalysis">
            {{ actionLoading.marketAnalysis ? '分析中...' : '執行市場分析' }}
          </button>
          <button class="btn btn-secondary" @click="goTo('/strong-stocks')">進入強勢股頁</button>
          <button class="btn btn-secondary" @click="goTo('/opportunities')">進入機會訊號頁</button>
          <button class="btn btn-secondary" :disabled="!canViewScheduler" @click="goTo('/system/scheduler')">進入排程管理頁</button>
        </div>
      </section>

      <section class="card surface-card">
        <div class="section-header">
          <h3>決策洞察（Decision Insight）</h3>
        </div>
        <ul class="hint-list">
          <li v-for="(insight, idx) in decisionInsights" :key="idx" class="hint-neutral">
            <p>{{ insight }}</p>
          </li>
        </ul>
        <p class="disclaimer">
          本系統為輔助工具，不保證獲利；所有訊號僅供觀察，請勿直接視為買進指令。
        </p>
      </section>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AdminLayout from '@/components/AdminLayout.vue'
import { investApiService } from '@/composables/useInvestApi'
import { useAuth } from '@/composables/useAuth'
import { toast } from '@shared/composables/useToast'

const router = useRouter()
const { currentUser } = useAuth()

const loadingPage = ref(false)
const actionLoading = reactive({
  priceUpdate: false,
  marketAnalysis: false,
  backfill: false
})
const backfillDays = ref(30)

const overview = reactive({
  totalCost: 0,
  totalMarketValue: 0,
  totalPnL: 0,
  totalPnLPercent: 0,
  baseCurrency: 'TWD',
  usdToTwdRate: 0,
  holdingCount: 0,
  alertSummary: {
    totalActiveAlerts: 0,
    highSeverityCount: 0,
    latestTriggeredAt: null
  },
  latestDailyReportSummary: null
})

const schedulerJobs = ref([])
const holdingSnapshots = ref([])
const activeSignals = ref([])
const strategySettings = ref(null)

const permissionSet = computed(() => {
  const permissions = currentUser.value?.permissions
  return new Set(Array.isArray(permissions) ? permissions : [])
})

const canRunPriceUpdate = computed(() =>
  permissionSet.value.has('INVEST_JOB_RUN_PRICE_UPDATE')
  || permissionSet.value.has('INVEST_SYS_SCHEDULER_RUN')
)

const canRunPriceBackfill = computed(() =>
  permissionSet.value.has('INVEST_JOB_RUN_PRICE_BACKFILL')
)

const canRunMarketAnalysis = computed(() =>
  permissionSet.value.has('INVEST_JOB_RUN_MARKET_ANALYSIS')
  || permissionSet.value.has('INVEST_SYS_SCHEDULER_RUN')
)

const canViewScheduler = computed(() => permissionSet.value.has('INVEST_SYS_SCHEDULER_VIEW'))

const schedulerMap = computed(() => {
  const map = {}
  for (const row of schedulerJobs.value) {
    if (row?.jobCode) {
      map[row.jobCode] = row
    }
  }
  return map
})

const priceUpdateJob = computed(() => schedulerMap.value.PRICE_UPDATE_HOLDINGS || null)
const marketAnalysisJob = computed(() => schedulerMap.value.MARKET_ANALYSIS || null)

const latestTradeDate = computed(() => {
  const dates = []
  const addDate = (value) => {
    if (typeof value === 'string' && value.trim()) {
      dates.push(value.trim())
    }
  }

  addDate(overview.latestDailyReportSummary?.dataAsOfTradeDate)
  for (const row of holdingSnapshots.value) addDate(row?.tradeDate)
  for (const row of activeSignals.value) addDate(row?.tradeDate)

  if (dates.length === 0) return null
  return dates.sort().reverse()[0]
})

const strongHoldings = computed(() => {
  const rows = holdingSnapshots.value.filter((row) =>
    ['STRONG', 'GOOD'].includes(row?.strengthLevel)
    && row?.dataQuality !== 'INSUFFICIENT'
  )

  return rows
    .sort((a, b) => (Number(b?.strengthScore || 0) - Number(a?.strengthScore || 0)))
    .slice(0, 5)
})

const weakHoldings = computed(() => {
  const rows = holdingSnapshots.value.filter((row) =>
    ['NEUTRAL', 'WEAK'].includes(row?.strengthLevel)
  )

  return rows
    .sort((a, b) => (Number(a?.strengthScore || 0) - Number(b?.strengthScore || 0)))
    .slice(0, 5)
})

const qualitySummary = computed(() => {
  const summary = { good: 0, partial: 0, stale: 0, insufficient: 0 }
  for (const row of holdingSnapshots.value) {
    const quality = String(row?.dataQuality || '').toUpperCase()
    if (quality === 'GOOD') summary.good += 1
    else if (quality === 'PARTIAL') summary.partial += 1
    else if (quality === 'STALE') summary.stale += 1
    else if (quality === 'INSUFFICIENT') summary.insufficient += 1
  }
  return summary
})

const totalHoldingSnapshotCount = computed(() => holdingSnapshots.value.length)
const strongCount = computed(() => holdingSnapshots.value.filter((row) => row?.strengthLevel === 'STRONG').length)
const weakCount = computed(() => holdingSnapshots.value.filter((row) => row?.strengthLevel === 'WEAK').length)
const observeSignalCount = computed(() => activeSignals.value.filter((row) => row?.recommendation === 'OBSERVE').length)
const waitPullbackSignalCount = computed(() => activeSignals.value.filter((row) => row?.recommendation === 'WAIT_PULLBACK').length)
const staleOrInsufficientCount = computed(() => qualitySummary.value.stale + qualitySummary.value.insufficient)

const decisionFocus = computed(() => {
  if (staleOrInsufficientCount.value > 0) {
    return `目前資料品質不足（${staleOrInsufficientCount.value} 檔），建議先更新行情再判斷`
  }

  if (strongCount.value > 0) {
    return `有 ${strongCount.value} 檔強勢股，維持偏強觀察`
  }

  if (weakCount.value > 0) {
    return `有 ${weakCount.value} 檔偏弱，需留意風險`
  }

  return '目前市場偏中性，暫無明確方向'
})

const decisionCredibilityLabel = computed(() => {
  const total = totalHoldingSnapshotCount.value
  if (total === 0) return '尚未分析'

  if (staleOrInsufficientCount.value > 0) return '偏低'
  if (qualitySummary.value.partial > 0) return '中等'
  return '較高'
})

const riskLevel = computed(() => {
  const total = totalHoldingSnapshotCount.value
  const alerts = Number(overview.alertSummary?.totalActiveAlerts || 0)
  const lowQualityCount = staleOrInsufficientCount.value
  const lowQualityRatio = total > 0 ? lowQualityCount / total : 0
  const partialRatio = total > 0 ? qualitySummary.value.partial / total : 0
  const goodRatio = total > 0 ? qualitySummary.value.good / total : 0

  if (alerts > 0 || (total > 0 && lowQualityRatio > 0.5)) {
    const reasons = []
    if (alerts > 0) reasons.push(`目前有 ${alerts} 筆警示事件。`)
    if (total > 0 && lowQualityRatio > 0.5) reasons.push(`資料品質不足占比偏高（${lowQualityCount}/${total}）。`)
    return { code: 'HIGH', label: '高', reasons }
  }

  if (total === 0) {
    return { code: 'MEDIUM', label: '中', reasons: ['尚無持股強弱資料，先更新再判讀。'] }
  }

  const partialMany = partialRatio >= 0.4
  const noStrong = strongCount.value === 0
  if (partialMany || noStrong) {
    const reasons = []
    if (partialMany) reasons.push(`PARTIAL 比例偏高（${qualitySummary.value.partial}/${total}）。`)
    if (noStrong) reasons.push('目前沒有 STRONG 持股。')
    return { code: 'MEDIUM', label: '中', reasons }
  }

  if (strongCount.value > 0 && goodRatio >= 0.6) {
    return { code: 'LOW', label: '低', reasons: ['有 STRONG 持股，且資料品質以 GOOD 為主。'] }
  }

  return { code: 'MEDIUM', label: '中', reasons: ['目前訊號偏中性，維持觀察。'] }
})

const strengthConclusion = computed(() => {
  if (strongCount.value === 0 && weakCount.value === 0) {
    return '目前無明顯強弱分布，市場偏中性'
  }
  return ''
})

const topOpportunitySignals = computed(() => {
  const recommendationPriority = {
    OBSERVE: 2,
    WAIT_PULLBACK: 1,
    REEVALUATE_WHEN_CONDITION_MET: 0,
    NOT_SUITABLE_CHASE: 0
  }

  return [...activeSignals.value]
    .sort((a, b) => {
      const scoreDiff = Number(b?.signalScore || 0) - Number(a?.signalScore || 0)
      if (scoreDiff !== 0) return scoreDiff
      const priorityDiff = Number(recommendationPriority[b?.recommendation] || 0) - Number(recommendationPriority[a?.recommendation] || 0)
      if (priorityDiff !== 0) return priorityDiff
      return String(b?.tradeDate || '').localeCompare(String(a?.tradeDate || ''))
    })
    .slice(0, 3)
})

const generateDecisionInsights = () => {
  const insights = []

  const alertCount = Number(overview.alertSummary?.totalActiveAlerts || 0)

  if (strongCount.value > 0) {
    insights.push(`有 ${strongCount.value} 檔持股呈現強勢，維持偏強觀察。`)
  }

  if (weakCount.value > 0) {
    insights.push(`有 ${weakCount.value} 檔持股偏弱，先留意風險變化。`)
  }

  if (observeSignalCount.value > 0) {
    insights.push(`目前有 ${observeSignalCount.value} 筆 OBSERVE 訊號，可持續觀察條件延續。`)
  }

  if (waitPullbackSignalCount.value > 0) {
    insights.push(`目前有 ${waitPullbackSignalCount.value} 筆 WAIT_PULLBACK 訊號，先等待回檔。`)
  }

  if (qualitySummary.value.stale > 0 || qualitySummary.value.insufficient > 0) {
    insights.push(`資料品質提醒：STALE ${qualitySummary.value.stale} 檔、INSUFFICIENT ${qualitySummary.value.insufficient} 檔，暫不判斷。`)
  }

  if (alertCount > 0) {
    insights.push(`目前有 ${alertCount} 筆警示事件，請先確認風險狀態。`)
  }

  if (insights.length === 0) {
    return [
      '目前無明顯異常。',
      `目前追蹤持股共 ${Number(overview.holdingCount || 0)} 檔，維持日常觀察。`,
      '資料品質目前可用，持續依流程更新與判讀。'
    ]
  }

  if (insights.length < 3) {
    insights.push(`目前追蹤持股共 ${Number(overview.holdingCount || 0)} 檔，維持紀律觀察。`)
  }

  if (insights.length < 3) {
    insights.push('若資料品質下降，請先更新行情再做判讀。')
  }

  return insights.slice(0, 5)
}

const decisionInsights = computed(() => generateDecisionInsights())

const strategyImpact = computed(() => {
  const settings = strategySettings.value
  const currentVersion = Number(settings?.strategyVersion || 0)

  const totalSnapshots = holdingSnapshots.value.length
  const alignedSnapshots = holdingSnapshots.value.filter((row) =>
    Number(row?.strategyVersion || 0) === currentVersion
  ).length

  const totalSignals = activeSignals.value.length
  const alignedSignals = activeSignals.value.filter((row) =>
    Number(row?.strategyVersion || 0) === currentVersion
  ).length

  const allSnapshotsAligned = totalSnapshots === 0 || alignedSnapshots === totalSnapshots
  const allSignalsAligned = totalSignals === 0 || alignedSignals === totalSignals
  const isAligned = allSnapshotsAligned && allSignalsAligned

  const alignmentSummary = isAligned
    ? `強弱快照 ${alignedSnapshots}/${totalSnapshots}、機會訊號 ${alignedSignals}/${totalSignals} 都已套用目前策略。`
    : `強弱快照 ${alignedSnapshots}/${totalSnapshots}、機會訊號 ${alignedSignals}/${totalSignals} 套用目前策略；建議重跑市場分析。`

  const strength = settings?.strength || {}
  const dataQuality = settings?.dataQuality || {}
  const opportunity = settings?.opportunity || {}
  const thresholdSummary = [
    `強弱門檻：STRONG ≥ ${strength.strongMin ?? '-'}，GOOD ≥ ${strength.goodMin ?? '-'}，WEAK ≤ ${strength.weakMax ?? '-'}`,
    `資料品質：最少 ${dataQuality.minHistoryDays ?? '-'} 日，過舊門檻 ${dataQuality.staleDays ?? '-'} 日`,
    `機會門檻：OBSERVE ≥ ${opportunity.observeMinScore ?? '-'}，WAIT_PULLBACK ≥ ${opportunity.waitPullbackMinScore ?? '-'}，REEVALUATE ≥ ${opportunity.reevaluateMinScore ?? '-'}`
  ]

  const levelCount = holdingSnapshots.value.reduce((acc, row) => {
    const key = row?.strengthLevel || 'UNKNOWN'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})

  const qualityCount = holdingSnapshots.value.reduce((acc, row) => {
    const key = row?.dataQuality || 'UNKNOWN'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})

  const recommendationCount = activeSignals.value.reduce((acc, row) => {
    const key = row?.recommendation || 'UNKNOWN'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})

  const impactSummary = [
    `持股強弱：STRONG ${levelCount.STRONG || 0}、GOOD ${levelCount.GOOD || 0}、NEUTRAL ${levelCount.NEUTRAL || 0}、WEAK ${levelCount.WEAK || 0}`,
    `資料品質：GOOD ${qualityCount.GOOD || 0}、PARTIAL ${qualityCount.PARTIAL || 0}、STALE ${qualityCount.STALE || 0}、INSUFFICIENT ${qualityCount.INSUFFICIENT || 0}`,
    `機會分布：OBSERVE ${recommendationCount.OBSERVE || 0}、WAIT_PULLBACK ${recommendationCount.WAIT_PULLBACK || 0}、REEVALUATE ${recommendationCount.REEVALUATE_WHEN_CONDITION_MET || 0}`
  ]

  return {
    currentVersion,
    isAligned,
    alignmentSummary,
    thresholdSummary,
    impactSummary
  }
})

const reloadAll = async () => {
  loadingPage.value = true
  try {
    const [overviewResult, schedulerResult, strengthResult, opportunityResult, strategySettingsResult] = await Promise.allSettled([
      investApiService.getDashboardOverview(),
      investApiService.getSystemSchedulerJobs(),
      investApiService.getStrengthSnapshotsPaged({
        universeType: 'HOLDING',
        page: 0,
        size: 100
      }),
      investApiService.getOpportunitySignalsPaged({
        status: 'ACTIVE',
        page: 0,
        size: 10
      }),
      investApiService.getSystemStrategySettings()
    ])

    if (overviewResult.status === 'fulfilled' && overviewResult.value) {
      const data = overviewResult.value
      Object.assign(overview, {
        totalCost: Number(data?.totalCost || 0),
        totalMarketValue: Number(data?.totalMarketValue || 0),
        totalPnL: Number(data?.totalPnL || 0),
        totalPnLPercent: Number(data?.totalPnLPercent || 0),
        baseCurrency: data?.baseCurrency || 'TWD',
        usdToTwdRate: Number(data?.usdToTwdRate || 0),
        holdingCount: Number(data?.holdingCount || 0),
        alertSummary: data?.alertSummary || overview.alertSummary,
        latestDailyReportSummary: data?.latestDailyReportSummary || null
      })
    } else if (overviewResult.status === 'rejected') {
      toast.error(`載入 Dashboard 概況失敗：${overviewResult.reason?.message || '未知錯誤'}`)
    }

    schedulerJobs.value = schedulerResult.status === 'fulfilled' && Array.isArray(schedulerResult.value)
      ? schedulerResult.value
      : []

    holdingSnapshots.value = strengthResult.status === 'fulfilled' && Array.isArray(strengthResult.value?.content)
      ? strengthResult.value.content
      : []

    activeSignals.value = opportunityResult.status === 'fulfilled' && Array.isArray(opportunityResult.value?.content)
      ? opportunityResult.value.content
      : []

    strategySettings.value = strategySettingsResult.status === 'fulfilled' && strategySettingsResult.value
      ? strategySettingsResult.value
      : null
  } finally {
    loadingPage.value = false
  }
}

const runPriceUpdate = async () => {
  if (!canRunPriceUpdate.value) {
    toast.error('你沒有執行價格更新的權限')
    return
  }

  actionLoading.priceUpdate = true
  try {
    const result = await investApiService.runPriceUpdate()
    const message = result?.message || (result?.status ? `價格更新結果：${result.status}` : '價格更新完成')
    toast.success(message)
    await reloadAll()
  } catch (error) {
    toast.error(`執行價格更新失敗：${error.message || '未知錯誤'}`)
  } finally {
    actionLoading.priceUpdate = false
  }
}

const runPriceBackfill = async () => {
  if (!canRunPriceBackfill.value) {
    toast.error('你沒有執行歷史行情回補的權限')
    return
  }

  actionLoading.backfill = true
  try {
    const result = await investApiService.runPriceBackfill(backfillDays.value, 'HOLDINGS_AND_WATCHLIST')
    const message = result?.message || (result?.status ? `歷史回補結果：${result.status}` : '歷史行情回補完成')
    toast.success(message)
    await reloadAll()
  } catch (error) {
    toast.error(`執行歷史行情回補失敗：${error.message || '未知錯誤'}`)
  } finally {
    actionLoading.backfill = false
  }
}

const runMarketAnalysis = async () => {
  if (!canRunMarketAnalysis.value) {
    toast.error('你沒有執行市場分析的權限')
    return
  }

  actionLoading.marketAnalysis = true
  try {
    const result = await investApiService.runMarketAnalysis()
    const message = result?.message || (result?.status ? `市場分析結果：${result.status}` : '市場分析完成')
    toast.success(message)
    await reloadAll()
  } catch (error) {
    toast.error(`執行市場分析失敗：${error.message || '未知錯誤'}`)
  } finally {
    actionLoading.marketAnalysis = false
  }
}

const goTo = (path) => {
  router.push(path)
}

const formatMoney = (value) => `$${Number(value || 0).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const formatSignedMoney = (value) => {
  const number = Number(value || 0)
  const sign = number >= 0 ? '+' : '-'
  return `${sign}${formatMoney(Math.abs(number))}`
}
const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`
const formatDateTime = (value) => value ? String(value).replace('T', ' ') : '-'

const formatStatus = (value) => {
  const map = {
    SUCCESS: '成功',
    PARTIAL_FAILED: '部分失敗',
    FAILED: '失敗',
    RUNNING: '執行中'
  }
  return map[value] || '-'
}

const statusClass = (value) => {
  if (value === 'SUCCESS') return 'text-up'
  if (value === 'PARTIAL_FAILED') return 'text-caution'
  if (value === 'FAILED') return 'text-down'
  return ''
}

const formatStrengthLevel = (value) => {
  const map = {
    STRONG: '強勢',
    GOOD: '偏強',
    NEUTRAL: '中性',
    WEAK: '偏弱'
  }
  return map[value] || value || '-'
}

const formatSignalType = (value) => {
  const map = {
    PULLBACK_WATCH: '等待回檔',
    BREAKOUT_WATCH: '突破觀察'
  }
  return map[value] || value || '-'
}

const formatDataQuality = (value) => {
  const map = {
    GOOD: '資料完整',
    PARTIAL: '資料部分不足',
    STALE: '資料過舊',
    INSUFFICIENT: '資料不足'
  }
  return map[value] || value || '-'
}

const qualityClass = (value) => {
  if (value === 'GOOD') return 'quality-good'
  if (value === 'PARTIAL') return 'quality-partial'
  if (value === 'STALE') return 'quality-stale'
  if (value === 'INSUFFICIENT') return 'quality-insufficient'
  return ''
}

const formatDecisionTag = (recommendation) => {
  const map = {
    OBSERVE: '偏強觀察',
    WAIT_PULLBACK: '等待回檔',
    REEVALUATE_WHEN_CONDITION_MET: '暫不判斷',
    NOT_SUITABLE_CHASE: '暫不判斷'
  }
  return map[recommendation] || '暫不判斷'
}

onMounted(reloadAll)
</script>

<style scoped>
.invest-dashboard-page { display: flex; flex-direction: column; gap: 14px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.page-header h2 { margin: 0; font-size: 1.8rem; letter-spacing: 0.5px; }
.page-header p { margin: 6px 0 0; color: var(--text-secondary); }

.overview-strip { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.overview-card { border: 1px solid var(--border-color); border-radius: 14px; padding: 14px; background: #fff; }
.overview-card span { color: var(--text-secondary); font-size: 0.85rem; }
.overview-card strong { display: block; margin: 6px 0; font-size: 1.35rem; }
.overview-card p { margin: 0; color: var(--text-secondary); font-size: 0.86rem; line-height: 1.4; }
.overview-card--accent { background: linear-gradient(135deg, rgba(15, 118, 110, 0.12), rgba(16, 185, 129, 0.09)); }
.confidence-metrics { margin-top: 4px !important; font-size: 0.8rem !important; line-height: 1.35 !important; }
.confidence-note { margin-top: 6px !important; font-size: 0.8rem !important; color: #6b7280 !important; }

.decision-focus-card { background: linear-gradient(135deg, rgba(30, 64, 175, 0.08), rgba(2, 132, 199, 0.08)); }
.focus-text { margin: 0; font-size: 1.05rem; font-weight: 700; color: #0f172a; }
.risk-level-row { margin-top: 8px; display: flex; flex-direction: column; gap: 6px; }
.risk-level-row strong { color: #1f2937; }
.risk-reason-list { margin: 0; padding-left: 18px; color: var(--text-secondary); font-size: 0.84rem; }
.risk-reason-list li { margin: 2px 0; }

.section-header h3 { margin: 0 0 10px; }
.data-asof-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 10px; }
.asof-item { border: 1px solid var(--border-color); border-radius: 12px; background: #fff; padding: 10px 12px; }
.asof-item span { color: var(--text-secondary); font-size: 0.82rem; }
.asof-item strong { display: block; margin: 4px 0; }
.asof-item p { margin: 0; color: var(--text-secondary); font-size: 0.82rem; }

.strategy-impact-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.strategy-version-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  color: #155e75;
  font-size: 0.78rem;
  font-weight: 600;
}

.strategy-impact-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 10px;
}

.strategy-impact-item {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: #fff;
  padding: 10px 12px;
}

.strategy-impact-item span {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.strategy-impact-item strong {
  display: block;
  margin: 4px 0 6px;
}

.strategy-impact-item p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.4;
}

.strategy-threshold-list {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
  font-size: 0.84rem;
}

.strategy-threshold-list li {
  margin: 4px 0;
}

.section-conclusion {
  margin: 0 0 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #334155;
  font-size: 0.9rem;
}

.strength-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 12px; }
.strength-panel { border: 1px solid var(--border-color); border-radius: 12px; padding: 10px; background: #fff; }
.strength-panel header { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 8px; }
.strength-panel h4 { margin: 0; font-size: 1rem; }
.strength-panel small { color: var(--text-secondary); }

.signal-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.signal-list > li { border: 1px solid #e5e7eb; border-radius: 10px; padding: 10px; background: #fff; }
.row-main { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
.row-main strong { font-size: 0.95rem; }
.row-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px; color: var(--text-secondary); font-size: 0.8rem; }
.signal-list p { margin: 6px 0 0; color: var(--text-secondary); font-size: 0.86rem; }

.chip { border-radius: 999px; padding: 2px 8px; font-size: 0.75rem; background: #e5e7eb; color: #111827; }
.chip-strong { background: #dcfce7; color: #166534; }
.chip-weak { background: #fee2e2; color: #991b1b; }

.reason-list { margin: 8px 0 0; padding-left: 18px; color: var(--text-secondary); font-size: 0.82rem; }

.quality-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; }
.quality-item { border: 1px solid var(--border-color); border-radius: 12px; background: #fff; padding: 10px 12px; }
.quality-item span { color: var(--text-secondary); font-size: 0.8rem; }
.quality-item strong { display: block; margin-top: 4px; }

.action-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 10px; }
.backfill-action { display: flex; gap: 8px; align-items: center; }
.select-control {
  min-width: 118px;
  height: 38px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: #fff;
  padding: 0 10px;
}

.hint-list { list-style: none; padding: 0; margin: 0; display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 10px; }
.hint-list li { border-radius: 12px; padding: 10px 12px; border: 1px solid var(--border-color); background: #fff; }
.hint-list strong { display: block; margin-bottom: 4px; }
.hint-list p { margin: 0; color: var(--text-secondary); font-size: 0.86rem; line-height: 1.4; }
.hint-positive { background: #ecfdf5; border-color: #bbf7d0 !important; }
.hint-watch { background: #eff6ff; border-color: #bfdbfe !important; }
.hint-caution { background: #fff7ed; border-color: #fed7aa !important; }
.hint-neutral { background: #f9fafb; border-color: #e5e7eb !important; }

.text-up { color: #0f766e; }
.text-down { color: #b91c1c; }
.text-caution { color: #b45309; }
.quality-good { color: #166534; }
.quality-partial { color: #1d4ed8; }
.quality-stale { color: #b45309; }
.quality-insufficient { color: #b91c1c; }

.empty-state { padding: 14px; text-align: center; color: var(--text-secondary); }
.empty-state.compact { padding: 10px; border: 1px dashed var(--border-color); border-radius: 10px; }
.disclaimer { margin: 10px 0 0; color: var(--text-secondary); font-size: 0.85rem; }
</style>
