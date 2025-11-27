<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">📊 資產組合管理</h2>
        <button class="btn-close" @click="$emit('close')" title="關閉">
          <svg class="close-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <div class="asset-portfolio-section">
          <div class="portfolio-header">
            <h3>我的資產組合 <span class="asset-count">({{ assets.length }} 項)</span></h3>
            <div class="header-actions">
              <button class="btn btn-primary" @click="showAddAssetForm">
                ➕ 新增資產
              </button>
              <button class="btn btn-primary" @click="updateAllStockPrices" :disabled="updatingPrices">
                📈 {{ updatingPrices ? '更新中...' : '更新所有股價' }}
              </button>
            </div>
          </div>
          
          <div class="portfolio-summary">
            <div class="summary-item">
              <span class="label">總資產價值 (台幣)</span>
              <span class="amount">${{ formatNumber(portfolioTotalValue) }}</span>
            </div>
            <div class="summary-item">
              <span class="label">總成本 (台幣)</span>
              <span class="amount">${{ formatNumber(portfolioTotalCost) }}</span>
            </div>
            <div class="summary-item">
              <span class="label">總損益價值 (台幣)</span>
              <span class="amount" :class="portfolioTotalProfitLoss >= 0 ? 'profit' : 'loss'">
                {{ portfolioTotalProfitLoss >= 0 ? '+' : '' }}${{ formatNumber(Math.abs(portfolioTotalProfitLoss)) }}
              </span>
            </div>
          </div>
          
          <div class="assets-table">
            <table>
              <thead>
                <tr>
                  <th>取得最新價格</th>
                  <th>資產名稱</th>
                  <th>股票代碼</th>
                  <th>類型</th>
                  <th>數量</th>
                  <th>購買平均成本價</th>
                  <th>當前價格</th>
                  <th>成本</th>
                  <th>當前價值</th>
                  <th>損益</th>
                  <th>損益%</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="assets.length === 0">
                  <td colspan="12" style="text-align: center; padding: 20px;">尚無資產記錄</td>
                </tr>
                <tr v-for="asset in assets" :key="asset.id" :data-asset-id="asset.id">
                  <td class="operation-cell">
                    <button 
                      v-if="asset.assetType === '股票' && asset.stockCode" 
                      class="btn-update" 
                      @click="updateCurrentPrice(asset.id)"
                      :disabled="updatingPriceId === asset.id"
                    >
                      📈 {{ updatingPriceId === asset.id ? '更新中...' : '更新股價' }}
                    </button>
                    <span v-else style="color: #999;">-</span>
                  </td>
                  <td>{{ asset.name || asset.stockCode || '-' }}</td>
                  <td>{{ asset.stockCode || '-' }}</td>
                  <td>{{ asset.assetType }}</td>
                  <td>{{ formatNumber(asset.quantity) }}</td>
                  <td>{{ asset.currency }} {{ formatNumber(asset.unitPrice, 2) }}</td>
                  <td>{{ asset.currency }} {{ formatNumber(asset.currentPrice || asset.unitPrice, 2) }}</td>
                  <td>NT$ {{ formatNumber(asset.costTWD) }}</td>
                  <td>NT$ {{ formatNumber(asset.currentValueTWD) }}</td>
                  <td :class="asset.profitLossTWD >= 0 ? 'profit' : 'loss'">
                    {{ asset.profitLossTWD >= 0 ? '+' : '' }}NT$ {{ formatNumber(Math.abs(asset.profitLossTWD)) }}
                  </td>
                  <td :class="asset.profitLossPercent >= 0 ? 'profit' : 'loss'">
                    {{ asset.profitLossPercent >= 0 ? '+' : '' }}{{ asset.profitLossPercent.toFixed(2) }}%
                  </td>
                  <td class="operation-cell">
                    <div class="btn-group">
                      <button class="btn-edit" @click="editAsset(asset)" title="編輯">✏️</button>
                      <button class="btn-delete" @click="deleteAsset(asset.id)" title="刪除">🗑️</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 更新股價進度 Modal -->
    <div v-if="showUpdateProgressModal" class="modal-overlay" @click="hideUpdateProgressModal">
      <div class="modal-panel" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">
            📈 更新股價 
            <span v-if="updateProgress.total > 0" class="progress-badge">
              {{ updateProgress.current }}/{{ updateProgress.total }}
            </span>
            <span v-if="updateProgress.completed" class="result-badge">
              ✅ {{ updateProgress.success }} 成功 ❌ {{ updateProgress.fail }} 失敗
            </span>
          </h2>
          <button class="btn-secondary" @click="hideUpdateProgressModal">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
            關閉
          </button>
        </div>
        <div class="modal-body">
          <div class="loading-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: updateProgress.percentage + '%' }"></div>
            </div>
            <div class="progress-text">{{ updateProgress.text }}</div>
          </div>
          
          <div class="stocks-update-list">
            <div 
              v-for="stock in updateProgress.stocks" 
              :key="stock.id" 
              class="stock-update-item"
              :class="stock.status"
            >
              <div class="stock-info">
                <div class="stock-name">{{ stock.name }}</div>
                <div class="stock-code">{{ stock.stockCode }}</div>
              </div>
              <div class="update-status" :class="'status-' + stock.status">
                {{ stock.message }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 新增/編輯資產 Modal -->
    <div v-if="showAddAssetModal" class="modal-overlay" @click="hideAddAssetForm">
      <div class="modal-panel" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">{{ editingAsset ? '✏️ 編輯資產' : '➕ 新增資產' }}</h2>
          <button class="btn-secondary" @click="hideAddAssetForm">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
            關閉
          </button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveAsset">
            <div class="form-group">
              <label for="assetName">資產名稱</label>
              <input type="text" id="assetName" v-model="assetForm.name" required placeholder="例如：AAPL 股票" />
            </div>
            
            <div class="form-group" v-if="assetForm.assetType === '股票'">
              <label for="assetStockCode">股票代碼</label>
              <input type="text" id="assetStockCode" v-model="assetForm.stockCode" placeholder="例如：AAPL、TSLA、2330" />
              <small style="color: #666; font-size: 12px;">
                美股：AAPL、TSLA | 台股：2330、2317 | 港股：0700、9988
              </small>
            </div>
            
            <div class="form-group">
              <label for="assetType">資產類型</label>
              <select id="assetType" v-model="assetForm.assetType" required>
                <option value="">請選擇類型</option>
                <option value="股票">股票</option>
                <option value="基金">基金</option>
                <option value="債券">債券</option>
                <option value="加密貨幣">加密貨幣</option>
                <option value="不動產">不動產</option>
                <option value="存款">存款</option>
                <option value="其他">其他</option>
              </select>
            </div>
            
            <div class="form-group">
              <label for="assetQuantity">股數/數量</label>
              <input type="number" id="assetQuantity" v-model.number="assetForm.quantity" step="0.01" min="0" required placeholder="例如：1000" />
            </div>
            
            <div class="form-group">
              <label for="assetCurrency">幣別</label>
              <select id="assetCurrency" v-model="assetForm.currency" required>
                <option value="TWD">台幣 (TWD)</option>
                <option value="USD">美元 (USD)</option>
                <option value="EUR">歐元 (EUR)</option>
                <option value="JPY">日圓 (JPY)</option>
                <option value="CNY">人民幣 (CNY)</option>
              </select>
            </div>
            
            <div class="form-group">
              <label for="assetCost">總成本</label>
              <input type="number" id="assetCost" v-model.number="assetForm.cost" step="0.01" min="0" required placeholder="例如：100000" />
              <small style="color: #666; font-size: 12px;">
                實際投入的總金額（包含手續費等）
              </small>
            </div>
            
            <div class="form-group">
              <label for="assetUnitPrice">購買平均成本價（自動計算）</label>
              <input type="number" id="assetUnitPrice" v-model.number="assetForm.unitPrice" step="0.0001" readonly placeholder="自動計算" />
              <small style="color: #666; font-size: 12px;">
                購買平均成本價 = 總成本 ÷ 股數
              </small>
            </div>
            
            <div class="form-group" v-if="assetForm.assetType === '股票'">
              <label for="assetCurrentPrice">當前價格 (選填)</label>
              <div style="display: flex; gap: 0.5rem; align-items: center;">
                <input type="number" id="assetCurrentPrice" v-model.number="assetForm.currentPrice" step="0.0001" min="0" placeholder="例如：160.00" style="flex: 1;" />
                <button type="button" class="btn-fetch-price" @click="fetchCurrentStockPrice" :disabled="fetchingPrice">
                  📈 {{ fetchingPrice ? '取得中...' : '取得最新價格' }}
                </button>
              </div>
              <small style="color: #666; font-size: 12px;">
                留空將使用購買價格計算價值
              </small>
            </div>
            
            <div class="form-group">
              <label for="assetPurchaseDate">購買日期 (選填)</label>
              <input type="date" id="assetPurchaseDate" v-model="assetForm.purchaseDate" />
            </div>
            
            <div class="form-group">
              <label for="assetDescription">描述</label>
              <textarea id="assetDescription" v-model="assetForm.description" placeholder="資產相關說明..."></textarea>
            </div>
            
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="hideAddAssetForm">取消</button>
              <button type="submit" class="btn btn-primary" :disabled="saving">
                {{ saving ? '儲存中...' : (editingAsset ? '更新資產' : '新增資產') }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { apiService } from '@/composables/useApi'

const emit = defineEmits(['close'])

const assets = ref([])
const showAddAssetModal = ref(false)
const editingAsset = ref(null)
const saving = ref(false)
const updatingPrices = ref(false)
const updatingPriceId = ref(null)
const fetchingPrice = ref(false)
const showUpdateProgressModal = ref(false)
const updateProgress = ref({
  current: 0,
  total: 0,
  percentage: 0,
  text: '準備中...',
  success: 0,
  fail: 0,
  completed: false,
  stocks: []
})
const exchangeRates = ref({
  USD: 30.0,
  EUR: 32.0,
  JPY: 0.2,
  CNY: 4.2
})

const assetForm = ref({
  name: '',
  stockCode: '',
  assetType: '',
  quantity: 0,
  currency: 'TWD',
  cost: 0,
  unitPrice: 0,
  currentPrice: null,
  purchaseDate: null,
  description: ''
})

// 監聽數量和成本變化，自動計算平均成本價
watch([() => assetForm.value.quantity, () => assetForm.value.cost], ([quantity, cost]) => {
  if (quantity > 0 && cost > 0) {
    assetForm.value.unitPrice = cost / quantity
  } else {
    assetForm.value.unitPrice = 0
  }
})

// 載入匯率
const loadExchangeRates = async () => {
  try {
    const today = new Date().toISOString().split('T')[0]
    const response = await apiService.getLatestExchangeRate(today)
    if (response) {
      exchangeRates.value = {
        USD: response.usdRate ? parseFloat(response.usdRate) : 30.0,
        EUR: response.eurRate ? parseFloat(response.eurRate) : 32.0,
        JPY: response.jpyRate ? parseFloat(response.jpyRate) : 0.2,
        CNY: response.cnyRate ? parseFloat(response.cnyRate) : 4.2
      }
    }
  } catch (error) {
    console.log('使用預設匯率')
  }
}

// 轉換為台幣
const convertToTWD = async (amount, currency, date = null) => {
  if (currency === 'TWD') {
    return amount
  }
  
  // 如果沒有指定日期，使用當前匯率
  if (!date) {
    const rate = exchangeRates.value[currency] || 1
    return amount * rate
  }
  
  // 使用指定日期的匯率
  try {
    const dateStr = typeof date === 'string' ? date : date.toISOString().split('T')[0]
    const response = await apiService.getExchangeRate(dateStr)
    if (response) {
      let rate = null
      if (currency === 'USD') rate = response.usdRate ? parseFloat(response.usdRate) : null
      else if (currency === 'EUR') rate = response.eurRate ? parseFloat(response.eurRate) : null
      else if (currency === 'JPY') rate = response.jpyRate ? parseFloat(response.jpyRate) : null
      else if (currency === 'CNY') rate = response.cnyRate ? parseFloat(response.cnyRate) : null
      
      if (rate !== null) {
        return amount * rate
      }
    }
    
    // 如果沒有找到歷史匯率，使用當前匯率
    const rate = exchangeRates.value[currency] || 1
    return amount * rate
  } catch (error) {
    console.error('獲取歷史匯率失敗，使用當前匯率:', error)
    const rate = exchangeRates.value[currency] || 1
    return amount * rate
  }
}

const portfolioTotalValue = computed(() => {
  return assets.value.reduce((sum, asset) => sum + (asset.currentValueTWD || 0), 0)
})

const portfolioTotalCost = computed(() => {
  return assets.value.reduce((sum, asset) => sum + (asset.costTWD || 0), 0)
})

const portfolioTotalProfitLoss = computed(() => {
  return portfolioTotalValue.value - portfolioTotalCost.value
})

const formatNumber = (num, decimals = 0) => {
  if (num === null || num === undefined) return '0'
  return parseFloat(num).toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const loadAssets = async () => {
  try {
    const response = await apiService.getAssets()
    assets.value = await Promise.all(response.map(async (asset) => {
      const quantity = parseFloat(asset.quantity || 0)
      const cost = parseFloat(asset.cost || 0)
      const unitPrice = quantity > 0 ? cost / quantity : 0
      const currentPrice = parseFloat(asset.currentPrice || unitPrice)
      const currency = asset.currency || 'TWD'
      const purchaseDate = asset.purchaseDate || null
      
      // 計算成本（台幣，使用購買日期的匯率）
      const costTWD = await convertToTWD(cost, currency, purchaseDate)
      
      // 計算當前價值（台幣，使用當前匯率）
      const currentValue = quantity * currentPrice
      const currentValueTWD = await convertToTWD(currentValue, currency)
      
      // 計算損益
      const profitLossTWD = currentValueTWD - costTWD
      const profitLossPercent = costTWD > 0 ? (profitLossTWD / costTWD) * 100 : 0
      
      return {
        ...asset,
        quantity,
        cost,
        unitPrice,
        currentPrice,
        currency,
        purchaseDate,
        costTWD,
        currentValueTWD,
        profitLossTWD,
        profitLossPercent
      }
    }))
    
    // 更新摘要
    updatePortfolioSummary()
  } catch (error) {
    console.error('載入資產失敗:', error)
  }
}

const updatePortfolioSummary = () => {
  // 摘要已通過 computed 自動更新
}

const showAddAssetForm = () => {
  editingAsset.value = null
  assetForm.value = {
    name: '',
    stockCode: '',
    assetType: '',
    quantity: 0,
    currency: 'TWD',
    cost: 0,
    unitPrice: 0,
    currentPrice: null,
    purchaseDate: null,
    description: ''
  }
  showAddAssetModal.value = true
}

const hideAddAssetForm = () => {
  showAddAssetModal.value = false
  editingAsset.value = null
}

const editAsset = (asset) => {
  editingAsset.value = asset
  assetForm.value = {
    name: asset.name || '',
    stockCode: asset.stockCode || '',
    assetType: asset.assetType || '',
    quantity: asset.quantity || 0,
    currency: asset.currency || 'TWD',
    cost: asset.cost || 0,
    unitPrice: asset.unitPrice || 0,
    currentPrice: asset.currentPrice || null,
    purchaseDate: asset.purchaseDate || null,
    description: asset.description || ''
  }
  showAddAssetModal.value = true
}

const saveAsset = async () => {
  saving.value = true
  try {
    const data = {
      name: assetForm.value.name,
      stockCode: assetForm.value.stockCode || null,
      assetType: assetForm.value.assetType,
      quantity: assetForm.value.quantity,
      currency: assetForm.value.currency,
      cost: assetForm.value.cost,
      unitPrice: assetForm.value.unitPrice,
      currentPrice: assetForm.value.currentPrice || null,
      purchaseDate: assetForm.value.purchaseDate || null,
      description: assetForm.value.description || null
    }
    
    if (editingAsset.value) {
      await apiService.updateAsset(editingAsset.value.id, data)
    } else {
      await apiService.createAsset(data)
    }
    
    await loadAssets()
    hideAddAssetForm()
  } catch (error) {
    console.error('儲存資產失敗:', error)
    alert('儲存失敗: ' + error.message)
  } finally {
    saving.value = false
  }
}

const deleteAsset = async (id) => {
  if (!confirm('確定要刪除這個資產嗎？')) {
    return
  }
  
  try {
    await apiService.deleteAsset(id)
    await loadAssets()
  } catch (error) {
    console.error('刪除資產失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

// 根據股票代碼判斷幣別
const getCurrencyByMarket = (stockCode) => {
  if (!stockCode) return 'TWD'
  // 美股：第一碼為英文字母
  if (/^[A-Za-z]/.test(stockCode)) {
    return 'USD'
  }
  // 台股：4位數字 或 4位數字+字母（如00937B）
  else if (/^\d{4}[A-Za-z]?$/.test(stockCode)) {
    return 'TWD'
  }
  // 其他情況預設為台幣
  else {
    return 'TWD'
  }
}

// Yahoo Finance API
const tryYahooFinance = async (stockCode) => {
  console.log(`📊 Yahoo Finance: ${stockCode}`)
  
  try {
    // 根據股票代碼自動添加市場後綴
    let yahooSymbol = stockCode.toUpperCase()
    
    // 常見美國ETF列表
    const usEtfSymbols = ["SPY", "VOO", "VTI", "IVV", "QQQ", "SCHD", "JEPI", "VT", "ARKK", "ARKQ", "ARKW", "ARKG", "ARKF", "XLF", "XLK", "XLE", "XLI", "XLV", "XLY", "XLP", "XLU", "XLRE", "XLB", "XLC", "XLNX"]
    
    // 檢測股票代碼類型並添加相應的市場後綴
    if (/^\d{4}[A-Za-z]?$/.test(stockCode)) {
      // 台股：4位數字或4位數字+字母（如00692B）
      yahooSymbol = `${stockCode}.TW`
      console.log(`台股代碼: ${stockCode} -> ${yahooSymbol}`)
    } else if (usEtfSymbols.includes(stockCode.toUpperCase())) {
      // 美國ETF：直接使用代碼
      yahooSymbol = stockCode.toUpperCase()
      console.log(`美國ETF: ${stockCode} -> ${yahooSymbol}`)
    } else if (/^[A-Za-z]{1,5}$/.test(stockCode)) {
      // 美國股票：直接使用代碼
      yahooSymbol = stockCode.toUpperCase()
      console.log(`美國股票: ${stockCode} -> ${yahooSymbol}`)
    } else if (/^\d{4}$/.test(stockCode)) {
      // 4位純數字：可能是台股
      yahooSymbol = `${stockCode}.TW`
      console.log(`4位數字台股: ${stockCode} -> ${yahooSymbol}`)
    } else {
      // 其他情況：預設為美股
      yahooSymbol = stockCode.toUpperCase()
      console.log(`預設美股: ${stockCode} -> ${yahooSymbol}`)
    }
    
    const apiUrl = `https://query1.finance.yahoo.com/v8/finance/chart/${yahooSymbol}?interval=1d&range=1mo`
    console.log(`Yahoo Finance API URL: ${apiUrl}`)
    
    // 使用 allorigins.win 的 raw 端點
    const proxyUrl = `https://api.allorigins.win/raw?url=${encodeURIComponent(apiUrl)}`
    console.log(`CORS 代理 URL: ${proxyUrl}`)
    
    const response = await fetch(proxyUrl, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
      }
    })
    
    if (!response.ok) {
      throw new Error(`代理請求失敗: ${response.status} ${response.statusText}`)
    }
    
    const data = await response.json()
    console.log('Yahoo Finance 回應:', data)
    
    // 解析 Yahoo Finance 回應
    if (data.chart && data.chart.result && data.chart.result.length > 0) {
      const result = data.chart.result[0]
      const meta = result.meta
      
      if (meta && meta.regularMarketPrice) {
        const price = meta.regularMarketPrice
        const currency = meta.currency || getCurrencyByMarket(stockCode)
        const symbol = meta.symbol || stockCode
        
        console.log(`✅ Yahoo Finance 解析成功: ${price} ${currency}`)
        return {
          price: price,
          currency: currency,
          symbol: symbol
        }
      } else {
        throw new Error('無法從 Yahoo Finance 回應中提取價格數據')
      }
    } else {
      throw new Error('Yahoo Finance 回應格式異常')
    }
  } catch (error) {
    console.error(`Yahoo Finance 代理失敗:`, error)
    throw error
  }
}

// 取得真實股票價格 - 多 API 支援
const fetchRealStockPrice = async (stockCode) => {
  console.log(`🔍 開始查詢 ${stockCode} 的價格...`)
  
  // 優先使用 Yahoo Finance API
  try {
    console.log(`嘗試 Yahoo Finance API`)
    const result = await tryYahooFinance(stockCode)
    if (result) {
      console.log(`✅ Yahoo Finance 成功: ${stockCode} = ${result.price} ${result.currency}`)
      return result
    }
  } catch (error) {
    console.warn(`❌ Yahoo Finance 失敗:`, error.message)
  }
  
  throw new Error(`無法取得 ${stockCode} 的價格資料，請手動輸入當前價格`)
}

// 根據股票代碼取得價格
const getStockPrice = async (stockCode) => {
  try {
    // 嘗試取得真實股票價格
    const realPrice = await fetchRealStockPrice(stockCode)
    if (realPrice) {
      console.log(`取得 ${stockCode} 真實價格: ${realPrice.price.toFixed(2)} ${realPrice.currency}`)
      return realPrice
    }
  } catch (error) {
    console.warn(`取得 ${stockCode} 真實價格失敗:`, error.message)
  }
  
  // 如果真實 API 失敗，拋出錯誤而不是使用固定價格
  throw new Error(`無法取得 ${stockCode} 的價格資料，請手動輸入當前價格`)
}

const updateCurrentPrice = async (assetId) => {
  updatingPriceId.value = assetId
  try {
    const asset = assets.value.find(a => a.id === assetId)
    if (!asset) {
      alert('找不到資產')
      return
    }
    
    if (asset.assetType !== '股票') {
      alert('此功能僅適用於股票')
      return
    }
    
    if (!asset.stockCode) {
      alert('此股票沒有股票代碼，無法自動更新價格')
      return
    }
    
    console.log(`🔄 開始更新 ${asset.stockCode} (${asset.name}) 的價格...`)
    
    // 根據股票代碼取得最新價格
    const stockData = await getStockPrice(asset.stockCode)
    
    // 更新資產
    await apiService.updateAsset(assetId, {
      currentPrice: stockData.price,
      currency: stockData.currency // 同時更新幣別
    })
    
    console.log(`✅ 更新 ${asset.stockCode} (${asset.name}) 價格: ${stockData.price.toFixed(2)} ${stockData.currency}`)
    alert(`${asset.stockCode} (${asset.name}) 價格已更新為 ${stockData.price.toFixed(2)} ${stockData.currency}`)
    
    // 重新載入資產
    await loadAssets()
  } catch (error) {
    console.error('❌ 更新價格失敗:', error)
    alert(`更新價格失敗: ${error.message}`)
  } finally {
    updatingPriceId.value = null
  }
}

const showUpdateProgress = () => {
  showUpdateProgressModal.value = true
}

const hideUpdateProgressModal = () => {
  showUpdateProgressModal.value = false
  updateProgress.value = {
    current: 0,
    total: 0,
    percentage: 0,
    text: '準備中...',
    success: 0,
    fail: 0,
    completed: false,
    stocks: []
  }
}

const updateProgressStatus = (percentage, text) => {
  updateProgress.value.percentage = percentage
  updateProgress.value.text = text
}

const updateStockStatus = (assetId, status, message) => {
  const stock = updateProgress.value.stocks.find(s => s.id === assetId)
  if (stock) {
    stock.status = status
    stock.message = message
  }
}

const updateAllStockPrices = async () => {
  const stockAssets = assets.value.filter(asset => 
    asset.assetType === '股票' && asset.stockCode
  )
  
  if (stockAssets.length === 0) {
    alert('沒有找到可更新的股票')
    return
  }
  
  // 初始化進度 Modal
  updateProgress.value = {
    current: 0,
    total: stockAssets.length,
    percentage: 0,
    text: '準備中...',
    success: 0,
    fail: 0,
    completed: false,
    stocks: stockAssets.map(asset => ({
      id: asset.id,
      name: asset.name,
      stockCode: asset.stockCode,
      status: 'waiting',
      message: '等待中...'
    }))
  }
  
  showUpdateProgress()
  updatingPrices.value = true
  
  try {
    console.log(`🔄 開始批量更新 ${stockAssets.length} 支股票...`)
    updateProgressStatus(0, '開始更新...')
    
    for (let i = 0; i < stockAssets.length; i++) {
      const asset = stockAssets[i]
      updatingPriceId.value = asset.id
      
      // 更新進度
      const progress = ((i + 1) / stockAssets.length) * 100
      updateProgressStatus(progress, `正在更新 ${asset.name} (${asset.stockCode})...`)
      updateProgress.value.current = i + 1
      
      // 更新股票狀態為「更新中」
      updateStockStatus(asset.id, 'updating', '正在取得價格...')
      
      try {
        console.log(`[${i + 1}/${stockAssets.length}] 更新 ${asset.stockCode} (${asset.name})...`)
        
        updateStockStatus(asset.id, 'updating', '嘗試取得價格...')
        const stockData = await getStockPrice(asset.stockCode)
        
        updateStockStatus(asset.id, 'updating', '正在儲存資料...')
        await apiService.updateAsset(asset.id, {
          currentPrice: stockData.price,
          currency: stockData.currency
        })
        
        updateProgress.value.success++
        updateStockStatus(asset.id, 'success', `成功: ${stockData.price.toFixed(2)} ${stockData.currency}`)
        console.log(`✅ [${i + 1}/${stockAssets.length}] ${asset.stockCode} 更新成功: ${stockData.price.toFixed(2)} ${stockData.currency}`)
        
        // 稍微延遲，避免 API 請求過快
        if (i < stockAssets.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 1000))
        }
      } catch (error) {
        updateProgress.value.fail++
        updateStockStatus(asset.id, 'fail', `失敗: ${error.message}`)
        console.error(`❌ [${i + 1}/${stockAssets.length}] ${asset.stockCode} 更新失敗:`, error)
      }
    }
    
    // 更新完成
    updateProgressStatus(100, '更新完成！')
    updateProgress.value.completed = true
    
    // 重新載入資產
    await loadAssets()
  } catch (error) {
    console.error('批量更新過程發生錯誤:', error)
    updateProgressStatus(0, '更新失敗')
    alert('批量更新過程發生錯誤: ' + error.message)
  } finally {
    updatingPrices.value = false
    updatingPriceId.value = null
  }
}

const fetchCurrentStockPrice = async () => {
  if (assetForm.value.assetType !== '股票') {
    alert('此功能僅適用於股票')
    return
  }
  
  if (!assetForm.value.stockCode) {
    alert('請先輸入股票代碼')
    return
  }
  
  fetchingPrice.value = true
  try {
    // 根據股票代碼取得價格
    const stockData = await getStockPrice(assetForm.value.stockCode)
    assetForm.value.currentPrice = stockData.price
    
    // 同時更新幣別欄位
    assetForm.value.currency = stockData.currency
    
    console.log(`取得 ${assetForm.value.stockCode} (${assetForm.value.name}) 最新價格: ${stockData.price.toFixed(2)} ${stockData.currency}`)
    alert(`${assetForm.value.stockCode} (${assetForm.value.name}) 最新價格: ${stockData.price.toFixed(2)} ${stockData.currency}`)
  } catch (error) {
    console.error('取得股票價格失敗:', error)
    alert(`取得 ${assetForm.value.stockCode} 價格失敗，請手動輸入`)
  } finally {
    fetchingPrice.value = false
  }
}

onMounted(async () => {
  await loadExchangeRates()
  await loadAssets()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  overflow-y: auto;
}

.modal-panel {
  width: 100%;
  max-width: 1200px;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  margin: 2rem 0;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(to right, #f8fafc, white);
  border-radius: 1rem 1rem 0 0;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.modal-body {
  padding: 1.5rem;
}

.asset-portfolio-section {
  width: 100%;
}

.portfolio-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.portfolio-header h3 {
  margin: 0;
  color: #1e293b;
  font-size: 1.25rem;
}

.asset-count {
  font-size: 0.8em;
  color: #64748b;
  font-weight: normal;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

.portfolio-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f8fafc;
  border-radius: 0.75rem;
  border: 1px solid #e2e8f0;
}

.summary-item {
  text-align: center;
}

.summary-item .label {
  display: block;
  font-size: 0.875rem;
  color: #64748b;
  margin-bottom: 0.5rem;
}

.summary-item .amount {
  display: block;
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

.summary-item .amount.profit {
  color: #28a745;
}

.summary-item .amount.loss {
  color: #dc3545;
}

.assets-table {
  overflow-x: auto;
}

.assets-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.assets-table th,
.assets-table td {
  padding: 8px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.assets-table th {
  background-color: #f8f9fa;
  font-weight: bold;
}

.assets-table tr:hover {
  background-color: #f5f5f5;
}

.operation-cell {
  white-space: nowrap;
}

.profit {
  color: #28a745;
  font-weight: bold;
}

.loss {
  color: #dc3545;
  font-weight: bold;
}

.btn-update {
  background: #28a745;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s ease;
}

.btn-update:hover:not(:disabled) {
  background: #218838;
}

.btn-update:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-group {
  display: flex;
  gap: 4px;
}

.btn-edit, .btn-delete {
  background: none;
  border: none;
  padding: 4px 6px;
  margin: 1px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
}

.btn-edit {
  color: white;
  background-color: #007bff;
}

.btn-edit:hover {
  background-color: #0056b3;
  transform: scale(1.05);
}

.btn-delete {
  color: white;
  background-color: #dc3545;
}

.btn-delete:hover {
  background-color: #c82333;
  transform: scale(1.05);
}

.btn {
  padding: 0.625rem 1.25rem;
  border-radius: 0.5rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
}

.btn-close {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #64748b;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.btn-close::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  transform: translate(-50%, -50%);
  transition: width 0.3s ease, height 0.3s ease;
  z-index: 0;
}

.btn-close:hover::before {
  width: 100%;
  height: 100%;
}

.btn-close:hover {
  border-color: #ef4444;
  transform: scale(1.1) rotate(90deg);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.btn-close:hover .close-icon {
  color: white;
  transform: scale(1.1);
}

.btn-close:active {
  transform: scale(0.95) rotate(90deg);
}

.close-icon {
  width: 20px;
  height: 20px;
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
}

.btn-fetch-price {
  padding: 0.5rem 1rem;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 0.375rem;
  cursor: pointer;
  font-size: 0.875rem;
  white-space: nowrap;
}

.btn-fetch-price:hover:not(:disabled) {
  background: #218838;
}

.btn-fetch-price:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #475569;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.625rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 0.5rem;
  font-size: 1rem;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}

.form-group input[readonly] {
  background-color: #f8fafc;
  cursor: not-allowed;
}

.form-group small {
  display: block;
  margin-top: 0.25rem;
  color: #64748b;
  font-size: 0.875rem;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.5rem;
  justify-content: flex-end;
}

.w-4 {
  width: 1rem;
}

.h-4 {
  height: 1rem;
}

.progress-badge,
.result-badge {
  font-size: 0.7em;
  color: white;
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 8px;
  border-radius: 12px;
  margin-left: 10px;
}

.loading-progress {
  margin-bottom: 1.5rem;
}

.progress-bar {
  width: 100%;
  height: 24px;
  background: #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s ease;
}

.progress-text {
  text-align: center;
  color: #475569;
  font-weight: 600;
}

.stocks-update-list {
  max-height: 400px;
  overflow-y: auto;
}

.stock-update-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  margin-bottom: 0.5rem;
  border-radius: 0.5rem;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  transition: all 0.2s ease;
}

.stock-update-item.waiting {
  background: #f8fafc;
}

.stock-update-item.updating {
  background: #fef3c7;
  border-color: #f59e0b;
}

.stock-update-item.success {
  background: #d1fae5;
  border-color: #10b981;
}

.stock-update-item.fail {
  background: #fee2e2;
  border-color: #ef4444;
}

.stock-info {
  flex: 1;
}

.stock-name {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 0.25rem;
}

.stock-code {
  font-size: 0.875rem;
  color: #64748b;
}

.update-status {
  padding: 0.375rem 0.75rem;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  font-weight: 600;
  white-space: nowrap;
}

.status-waiting {
  background: #e2e8f0;
  color: #64748b;
}

.status-updating {
  background: #fef3c7;
  color: #92400e;
}

.status-success {
  background: #d1fae5;
  color: #065f46;
}

.status-fail {
  background: #fee2e2;
  color: #991b1b;
}
</style>
