import { reactive } from 'vue'

const state = reactive({
  toasts: []
})

let idSeq = 1

function pushToast({ type = 'info', title = '', message = '', duration = 2400 } = {}) {
  const id = idSeq++
  const toast = { id, type, title, message }
  state.toasts.push(toast)

  if (duration && duration > 0) {
    setTimeout(() => {
      removeToast(id)
    }, duration)
  }
  return id
}

function removeToast(id) {
  const idx = state.toasts.findIndex(t => t.id === id)
  if (idx >= 0) state.toasts.splice(idx, 1)
}

export function useToastState() {
  return state
}

export const toast = {
  info(message, title = '提示', opts = {}) { return pushToast({ type:'info', title, message, ...opts }) },
  success(message, title = '成功', opts = {}) { return pushToast({ type:'success', title, message, ...opts }) },
  warning(message, title = '提醒', opts = {}) { return pushToast({ type:'warning', title, message, ...opts }) },
  error(message, title = '錯誤', opts = {}) { return pushToast({ type:'error', title, message, ...opts }) },
  push: pushToast,
  remove: removeToast
}
