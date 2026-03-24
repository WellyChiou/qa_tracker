import { ref } from 'vue'
import { setLoadingCallbacks } from './useInvestApi'

const isLoading = ref(false)
const message = ref('載入中...')
let loadingCount = 0

export function useLoading() {
  const showLoading = (text = '載入中...') => {
    loadingCount += 1
    message.value = text
    isLoading.value = true
  }

  const hideLoading = () => {
    loadingCount -= 1
    if (loadingCount <= 0) {
      loadingCount = 0
      isLoading.value = false
    }
  }

  const setMessage = (text) => {
    message.value = text
  }

  setLoadingCallbacks(showLoading, hideLoading)

  return {
    isLoading,
    message,
    showLoading,
    hideLoading,
    setMessage
  }
}
