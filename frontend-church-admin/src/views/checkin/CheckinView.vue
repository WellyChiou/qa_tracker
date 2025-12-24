<template>
  <div class="wrap">
    <h2>教會簽到</h2>
    
    <div v-if="sessionInfo.title || sessionInfo.openAt || sessionInfo.closeAt" class="session-info">
      <div v-if="sessionInfo.title" class="session-title">{{ sessionInfo.title }}</div>
      <div v-if="sessionInfo.openAt || sessionInfo.closeAt" class="session-time">
        <span v-if="sessionInfo.openAt">開始時間：{{ formatDateTime(sessionInfo.openAt) }}</span>
        <span v-if="sessionInfo.closeAt">結束時間：{{ formatDateTime(sessionInfo.closeAt) }}</span>
      </div>
    </div>
    
    <div class="hint">掃描共用 QR 進入此頁。驗證碼每 60 秒自動更新。</div>

    <label class="label">會員編號</label>
    <input class="input" v-model="memberNo" placeholder="例如 A123BC" @keyup.enter="submit" />

    <button class="btn" :disabled="loading" @click="submit">簽到</button>

    <div v-if="message" class="msg" :class="status">{{ message }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiRequest } from '@/utils/api'

const memberNo = ref('')
const message = ref('')
const status = ref('')
const loading = ref(false)
const code = location.pathname.split('/').pop()
const sessionInfo = ref({
  title: '',
  openAt: null,
  closeAt: null
})

function maskName(name){
  if(!name) return ''
  if(name.length <= 1) return name
  if(name.length === 2) return name[0] + '○'
  return name[0] + '○' + name.slice(-1)
}

function formatDateTime(dt){
  if(!dt) return '-'
  const d = new Date(dt)
  if(isNaN(d.getTime())) return dt
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

async function loadSessionInfo(){
  try {
    const res = await apiRequest(`/church/checkin/public/sessions/${code}`, {
      method: 'GET'
    }, '', false)
    if (res.ok) {
      const data = await res.json()
      sessionInfo.value = {
        title: data.title || '',
        openAt: data.openAt || null,
        closeAt: data.closeAt || null
      }
    }
  } catch (e) {
    console.error('載入場次資訊失敗:', e)
    // 不顯示錯誤，讓頁面可以繼續使用
  }
}

onMounted(() => {
  loadSessionInfo()
})

async function submit(){
  loading.value = true
  message.value = ''
  status.value = ''
  try{
    const m = memberNo.value.trim().toUpperCase()
    if(!m){ 
      message.value = '請輸入會員編號 ❌'
      status.value = 'err'
      loading.value = false
      return
    }

    const tokenRes = await apiRequest(`/church/checkin/public/sessions/${code}/token`, {
      method: 'GET'
    }, '', false)
    if (!tokenRes.ok) {
      const errorData = await tokenRes.json().catch(() => ({}))
      throw new Error(errorData.code || 'TOKEN_ERROR')
    }
    const tokenData = await tokenRes.json()

    // 直接使用 fetch 而不是 apiRequest，以便更好地處理錯誤響應
    const checkinUrl = `/api/church/checkin/public/sessions/${code}/checkin`
    const checkinRes = await fetch(checkinUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ memberNo: m, token: tokenData.token })
    })
    
    if (!checkinRes.ok) {
      // 嘗試解析錯誤響應
      let errorCode = 'CHECKIN_ERROR'
      try {
        const errorData = await checkinRes.json()
        errorCode = errorData.code || errorCode
      } catch (parseError) {
        // 如果無法解析 JSON，使用狀態碼判斷
        if (checkinRes.status === 400) {
          errorCode = 'ALREADY_CHECKED_IN' // 400 通常是重複簽到
        }
      }
      throw new Error(errorCode)
    }
    
    const checkinData = await checkinRes.json()
    message.value = `${maskName(checkinData.name)}，簽到完成 ✅`
    status.value = 'ok'
  }catch(e){
    let code = e.message || 'FAILED'
    
    if(code === 'ALREADY_CHECKED_IN'){
      message.value = '已簽到完成，請勿重複簽到 ✅'
      status.value = 'ok'
    }else if(code === 'ALREADY_CHECKED_IN_MANUAL'){
      message.value = '已簽到(補登) ✅'
      status.value = 'ok'
    }else if(code === 'TIME_WINDOW_CLOSED'){
      message.value = '目前不在簽到時間內 ⛔'
      status.value = 'err'
    }else if(code === 'TOKEN_INVALID' || code === 'TOKEN_ERROR'){
      message.value = '驗證碼已過期，請重試 🔄'
      status.value = 'warn'
    }else if(code === 'MEMBER_NOT_FOUND'){
      message.value = '查無此會員編號 ❌'
      status.value = 'err'
    }else if(code === 'SESSION_NOT_FOUND'){
      message.value = '查無此場次 ❌'
      status.value = 'err'
    }else{
      message.value = '簽到失敗，請再試一次 ❌'
      status.value = 'err'
    }
  }finally{
    loading.value = false
  }
}
</script>

<style>
.wrap{padding:24px;max-width:520px;margin:0 auto}
.session-info{
  margin-bottom:20px;
  padding:16px;
  background:rgba(255,255,255,0.05);
  border-radius:8px;
  border:1px solid rgba(255,255,255,0.1);
}
.session-title{
  font-size:18px;
  font-weight:600;
  margin-bottom:8px;
  color:inherit;
}
.session-time{
  font-size:14px;
  opacity:0.8;
  display:flex;
  flex-direction:column;
  gap:4px;
}
.session-time span{
  display:block;
}
.hint{opacity:.8;margin:8px 0 18px 0;font-size:14px}
.label{display:block;margin:10px 0 6px 0;font-size:14px}
.input{width:100%;padding:10px 12px;border:1px solid #444;border-radius:8px;background:transparent;color:inherit}
.btn{margin-top:12px;padding:10px 12px;border-radius:10px;border:1px solid #666;cursor:pointer}
.btn[disabled]{opacity:.6;cursor:not-allowed}
.msg{margin-top:14px;padding:10px 12px;border-radius:10px;border:1px solid #444}
.ok{border-color:#2f9e44}
.warn{border-color:#f59f00}
.err{border-color:#e03131}
</style>

