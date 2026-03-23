import { createApiClient } from '@shared/utils/apiClient'

const investApiClient = createApiClient({
  accessTokenKey: 'invest_access_token',
  refreshTokenKey: 'invest_refresh_token',
  authExpiredMessage: 'Invest 認證已過期，請重新登入',
  credentials: 'omit',
  getApiBaseUrl() {
    return import.meta.env.VITE_API_BASE_URL || (
      import.meta.env.DEV
        ? `${window.location.protocol}//${window.location.hostname}:8080/api/invest`
        : `${window.location.protocol}//${window.location.hostname}/api/invest`
    )
  },
  isAuthRelatedRequest(url) {
    return url.includes('/auth/')
  },
  refreshEndpoint: '/auth/refresh',
  rejectRedirected: true,
  shouldAttachToken(url) {
    const isAuthEndpoint = url.includes('/auth/login')
      || url.includes('/auth/register')
      || url.includes('/auth/refresh')

    return !isAuthEndpoint
  }
})

export const setLoadingCallbacks = investApiClient.setLoadingCallbacks
export const setTokens = investApiClient.setTokens
export const clearTokens = investApiClient.clearTokens

class InvestApiService {
  async request(url, options = {}) {
    return investApiClient.request(url, options, {
      loadingMessage: options.loadingMessage || '載入中...',
      showLoading: options.showLoading !== false
    })
  }

  async login(username, password) {
    const result = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
      loadingMessage: '登入中...'
    })

    if (result && result.accessToken) {
      setTokens(result.accessToken, result.refreshToken || null)
    }

    return result
  }

  async logout() {
    try {
      await this.request('/auth/logout', {
        method: 'POST',
        loadingMessage: '登出中...'
      })
    } finally {
      clearTokens()
    }
  }

  async getCurrentUser() {
    return this.request('/auth/current-user', { showLoading: false })
  }

  getStocksPaged(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/stocks/paged?${query}` : '/stocks/paged', { method: 'GET' })
  }

  getStockOptions() {
    return this.request('/stocks/options', { method: 'GET' })
  }

  getStocksAll(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/stocks/all?${query}` : '/stocks/all', { method: 'GET' })
  }

  createStock(payload) {
    return this.request('/stocks', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  }

  updateStock(id, payload) {
    return this.request(`/stocks/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  }

  deleteStock(id) {
    return this.request(`/stocks/${id}`, { method: 'DELETE' })
  }

  getStockPriceDailiesPaged(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/stock-price-dailies/paged?${query}` : '/stock-price-dailies/paged', { method: 'GET' })
  }

  getStockPriceDailiesAll(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/stock-price-dailies/all?${query}` : '/stock-price-dailies/all', { method: 'GET' })
  }

  createStockPriceDaily(payload) {
    return this.request('/stock-price-dailies', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  }

  updateStockPriceDaily(id, payload) {
    return this.request(`/stock-price-dailies/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  }

  deleteStockPriceDaily(id) {
    return this.request(`/stock-price-dailies/${id}`, { method: 'DELETE' })
  }

  getPortfoliosPaged(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/portfolios/paged?${query}` : '/portfolios/paged', { method: 'GET' })
  }

  getPortfoliosAll(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/portfolios/all?${query}` : '/portfolios/all', { method: 'GET' })
  }

  getPortfolioById(id) {
    return this.request(`/portfolios/${id}`, { method: 'GET' })
  }

  createPortfolio(payload) {
    return this.request('/portfolios', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  }

  updatePortfolio(id, payload) {
    return this.request(`/portfolios/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  }

  deletePortfolio(id) {
    return this.request(`/portfolios/${id}`, { method: 'DELETE' })
  }

  getDashboardOverview() {
    return this.request('/portfolio-dashboard/overview', { method: 'GET' })
  }

  getPortfolioRiskResultsPaged(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/portfolio-risk-results/paged?${query}` : '/portfolio-risk-results/paged', { method: 'GET' })
  }

  getPortfolioRiskResultById(id) {
    return this.request(`/portfolio-risk-results/${id}`, { method: 'GET' })
  }

  getLatestPortfolioRiskResult(portfolioId) {
    return this.request(`/portfolio-risk-results/latest?portfolioId=${portfolioId}`, { method: 'GET' })
  }

  recalculatePortfolioRisk(portfolioId) {
    return this.request(`/portfolio-risk-results/recalculate/${portfolioId}`, {
      method: 'POST'
    })
  }

  getDailyReportsPaged(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/daily-reports/paged?${query}` : '/daily-reports/paged', { method: 'GET' })
  }

  getDailyReportById(id) {
    return this.request(`/daily-reports/${id}`, { method: 'GET' })
  }

  getLatestDailyReport(reportType = 'PORTFOLIO_RISK_DAILY') {
    return this.request(`/daily-reports/latest?reportType=${reportType}`, { method: 'GET' })
  }

  getSchedulerJobLogsPaged(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/scheduler-job-logs/paged?${query}` : '/scheduler-job-logs/paged', { method: 'GET' })
  }

  runDailyPortfolioRiskReport(payload = {}) {
    const query = new URLSearchParams(payload).toString()
    return this.request(query ? `/jobs/run-daily-portfolio-risk-report?${query}` : '/jobs/run-daily-portfolio-risk-report', {
      method: 'POST'
    })
  }

  runAlertPolling() {
    return this.request('/jobs/run-alert-polling', {
      method: 'POST'
    })
  }

  runPriceUpdate() {
    return this.request('/jobs/run-price-update', {
      method: 'POST'
    })
  }

  runMarketAnalysis(scope = 'HOLDINGS_AND_WATCHLIST') {
    const query = new URLSearchParams({ scope }).toString()
    return this.request(`/jobs/run-market-analysis?${query}`, {
      method: 'POST'
    })
  }

  getPortfolioAlertSetting(portfolioId) {
    return this.request(`/portfolio-alert-settings/${portfolioId}`, { method: 'GET' })
  }

  updatePortfolioAlertSetting(portfolioId, payload) {
    return this.request(`/portfolio-alert-settings/${portfolioId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  }

  getPortfolioAlertEventsPaged(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/portfolio-alert-events/paged?${query}` : '/portfolio-alert-events/paged', { method: 'GET' })
  }

  getLatestPortfolioAlertEvents(limit = 10) {
    return this.request(`/portfolio-alert-events/latest?limit=${limit}`, { method: 'GET' })
  }

  getDefaultWatchlist() {
    return this.request('/watchlists/default', { method: 'GET' })
  }

  getDefaultWatchlistItemsPaged(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/watchlists/default/items/paged?${query}` : '/watchlists/default/items/paged', { method: 'GET' })
  }

  addDefaultWatchlistItem(payload) {
    return this.request('/watchlists/default/items', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  }

  removeDefaultWatchlistItem(itemId) {
    return this.request(`/watchlists/default/items/${itemId}`, {
      method: 'DELETE'
    })
  }

  getStrengthSnapshotsPaged(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/strength-snapshots/paged?${query}` : '/strength-snapshots/paged', { method: 'GET' })
  }

  getStrengthSnapshotById(id) {
    return this.request(`/strength-snapshots/${id}`, { method: 'GET' })
  }

  getLatestStrengthSnapshot(stockId, universeType = '') {
    const query = new URLSearchParams({ stockId, universeType }).toString()
    return this.request(`/strength-snapshots/latest?${query}`, { method: 'GET' })
  }

  getOpportunitySignalsPaged(params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(query ? `/opportunity-signals/paged?${query}` : '/opportunity-signals/paged', { method: 'GET' })
  }

  getOpportunitySignalById(id) {
    return this.request(`/opportunity-signals/${id}`, { method: 'GET' })
  }

  getLatestOpportunitySignal(stockId) {
    return this.request(`/opportunity-signals/latest?stockId=${stockId}`, { method: 'GET' })
  }

  getSystemSchedulerJobs() {
    return this.request('/system/scheduler/jobs', { method: 'GET' })
  }

  runSystemSchedulerJobNow(jobCode) {
    return this.request(`/system/scheduler/jobs/${jobCode}/run-now`, {
      method: 'POST'
    })
  }

  getSystemSchedulerJobLogsPaged(jobCode, params = {}) {
    const query = new URLSearchParams(params).toString()
    return this.request(
      query
        ? `/system/scheduler/jobs/${jobCode}/logs/paged?${query}`
        : `/system/scheduler/jobs/${jobCode}/logs/paged`,
      { method: 'GET' }
    )
  }
}

export const investApiService = new InvestApiService()
