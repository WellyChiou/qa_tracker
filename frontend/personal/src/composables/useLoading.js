import { ref } from 'vue'
import { setLoadingCallbacks } from './useApi'

const isLoading = ref(false)
const message = ref('載入中...')
let loadingCount = 0

export function useLoading() {
  const showLoading = (text = '載入中...') => {
    loadingCount++
    message.value = text
    isLoading.value = true
  }

  const hideLoading = () => {
    loadingCount--
    if (loadingCount <= 0) {
      loadingCount = 0
      isLoading.value = false
    }
  }

  const setMessage = (text) => {
    message.value = text
  }

  // 將回調函數註冊到 API 服務
  setLoadingCallbacks(showLoading, hideLoading)

  return {
    isLoading,
    message,
    showLoading,
    hideLoading,
    setMessage
  }
}

