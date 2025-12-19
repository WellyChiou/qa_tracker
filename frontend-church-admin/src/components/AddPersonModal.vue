<template>
  <div v-if="show" class="modal-overlay" @click="closeModal">
    <div class="modal-panel" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">添加人員到{{ dayType === 'saturday' ? '週六' : '週日' }}</h2>
        <button class="btn-close" @click="closeModal">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <!-- 搜尋框 -->
        <div class="search-section">
          <input
            type="text"
            v-model="searchKeyword"
            placeholder="搜尋人員..."
            class="search-input"
            @input="searchPersons"
          />
        </div>

        <!-- 人員列表（左右分欄） -->
        <div class="transfer">
          <div class="transfer-col">
            <div class="transfer-head">
              <span>可加入</span>
            </div>
            <div class="transfer-list">
              <div
                v-for="person in filteredPersons"
                :key="person.id"
                class="transfer-item"
              >
                <div class="transfer-item-main">
                  <div class="transfer-title">
                    {{ person.personName }}
                    <span v-if="person.displayName && person.displayName !== person.personName" class="muted">
                      ({{ person.displayName }})
                    </span>
                  </div>
                </div>
                <button type="button" class="mini-btn" @click="toggleSelect(person)">
                  {{ isSelected(person.id) ? '已選' : '選擇' }}
                </button>
              </div>
              <div v-if="filteredPersons.length === 0" class="transfer-empty">沒有可加入的人員</div>
            </div>
          </div>

          <div class="transfer-col">
            <div class="transfer-head">
              <span>已選擇</span>
            </div>
            <div class="transfer-list">
              <div v-for="person in selectedPersons" :key="person.id" class="transfer-item">
                <div class="transfer-item-main">
                  <div class="transfer-title">
                    {{ person.personName }}
                    <span v-if="person.displayName && person.displayName !== person.personName" class="muted">
                      ({{ person.displayName }})
                    </span>
                  </div>
                </div>
                <button type="button" class="mini-btn mini-btn--danger" @click="toggleSelect(person)">
                  移除
                </button>
              </div>
              <div v-if="selectedPersons.length === 0" class="transfer-empty">尚未選擇任何人員</div>
            </div>
          </div>
        </div>

        <div class="form-actions" style="margin-top:12px;">
          <button type="button" class="btn btn-primary" :disabled="selectedPersons.length===0" @click="confirmAdd">
            加入所選 ({{ selectedPersons.length }})
          </button>
          <button type="button" class="btn btn-secondary" @click="openCreatePersonModal">
            + 新增人員
          </button>
          <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
        </div>
      </div>
    </div>
  </div>

  <!-- 新增人員 Modal -->
  <CreatePersonModal
    v-if="showCreatePersonModal"
    :show="showCreatePersonModal"
    @close="closeCreatePersonModal"
    @created="handlePersonCreated"
  />

</template>

<script setup>
import { toast } from '@/composables/useToast'
import { ref, computed, watch, onMounted } from 'vue'
import CreatePersonModal from './CreatePersonModal.vue'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  dayType: {
    type: String,
    required: true
  },
  positionId: {
    type: Number,
    required: true
  },
  existingPersonIds: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'added'])

const persons = ref([])
const selectedIds = ref(new Set())

const isSelected = (id) => selectedIds.value.has(id)
const toggleSelect = (person) => {
  const s = new Set(selectedIds.value)
  if (s.has(person.id)) s.delete(person.id)
  else s.add(person.id)
  selectedIds.value = s
}

const selectedPersons = computed(() => {
  const s = selectedIds.value
  return (persons.value || []).filter(p => s.has(p.id))
})

const confirmAdd = () => {
  emit('added', { dayType: props.dayType, persons: selectedPersons.value })
  closeModal()
}
const searchKeyword = ref('')
const showCreatePersonModal = ref(false)

const filteredPersons = computed(() => {
  let filtered = persons.value.filter(p => 
    !props.existingPersonIds.includes(p.id) && p.isActive !== false
  )
  
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(p => 
      p.personName.toLowerCase().includes(keyword) ||
      (p.displayName && p.displayName.toLowerCase().includes(keyword))
    )
  }
  
  return filtered
})

const loadPersons = async () => {
  try {
    const response = await apiRequest('/church/persons/active', {
      method: 'GET'
    })
    const result = await response.json()
    persons.value = result.persons || []
  } catch (error) {
    console.error('載入人員列表失敗：', error)
    toast.error('載入人員列表失敗：' + error.message)
  }
}

const searchPersons = () => {
  // 搜尋邏輯已在 computed 中處理
}


const openCreatePersonModal = () => {
  showCreatePersonModal.value = true
}

const closeCreatePersonModal = () => {
  showCreatePersonModal.value = false
  loadPersons() // 重新載入人員列表
}

const handlePersonCreated = async () => {
  await loadPersons()
  closeCreatePersonModal()
}

const closeModal = () => {
  searchKeyword.value = ''
  emit('close')
}

watch(() => props.show, (newVal) => {
  if (newVal) {
    selectedIds.value = new Set()
    loadPersons()
  }
})

onMounted(() => {
  if (props.show) {
    loadPersons()
  }
})
</script>

<style scoped>

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-panel {
  width: 100%;
  max-width: 600px;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(to right, #f8fafc, white);
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  color: #64748b;
  transition: color 0.2s;
}

.btn-close:hover {
  color: #1e293b;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.search-section {
  margin-bottom: 1.5rem;
}

.search-input {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 2px solid #e2e8f0;
  border-radius: 0.5rem;
  font-size: 1rem;
  outline: none;
  transition: all 0.2s;
}

.search-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.person-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  max-height: 400px;
  overflow-y: auto;
}

.person-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s;
}

.person-item:hover {
  background: #f1f5f9;
  border-color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.person-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
}

.person-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.person-name {
  font-size: 1rem;
  font-weight: 500;
  color: #1e293b;
}

.person-display-name {
  font-size: 0.875rem;
  color: #64748b;
}

.btn-select {
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-select:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.empty-message {
  text-align: center;
  padding: 2rem;
  color: #64748b;
  font-size: 0.9rem;
}

.add-person-section {
  padding-top: 1rem;
  border-top: 1px solid #e2e8f0;
}

.btn-add-new {
  width: 100%;
  padding: 0.875rem 1rem;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-add-new:hover {
  background: #218838;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.4);
}

.w-4 {
  width: 1rem;
  height: 1rem;
}

/* Improved scroll shadows (prevents fixed shadow overlay issue) */
.modal-body{
  overflow:auto;
  background:
    linear-gradient(#fff 30%, rgba(255,255,255,0)),
    linear-gradient(rgba(255,255,255,0), #fff 70%),
    radial-gradient(farthest-side at 50% 0, rgba(2,6,23,.16), rgba(2,6,23,0)),
    radial-gradient(farthest-side at 50% 100%, rgba(2,6,23,.16), rgba(2,6,23,0));
  background-repeat:no-repeat;
  background-size:100% 40px, 100% 40px, 100% 14px, 100% 14px;
  background-attachment:local, local, scroll, scroll;
  background-position:0 0, 0 100%, 0 0, 0 100%;
}

.transfer{
  display:grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.transfer-col{
  border:1px solid rgba(2,6,23,.08);
  background: rgba(255,255,255,.75);
  border-radius: 14px;
  overflow:hidden;
}
.transfer-head{
  padding:10px 12px;
  border-bottom:1px solid rgba(2,6,23,.06);
  background: rgba(2,6,23,.02);
  font-weight: 900;
}
.transfer-list{
  max-height: 360px;
  overflow:auto;
  padding: 10px;
}
.transfer-item{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:10px;
  padding:10px;
  border:1px solid rgba(2,6,23,.08);
  border-radius: 12px;
  background:#fff;
  margin-bottom: 10px;
}
.transfer-item-main{ min-width:0; }
.transfer-title{ font-weight: 900; color:#0f172a; line-height:1.2; }
.muted{ color:#64748b; font-weight:700; font-size:12px; margin-left:6px; }
.mini-btn{
  padding: 8px 10px;
  border-radius: 10px;
  border:1px solid rgba(2,6,23,.12);
  background: rgba(37,99,235,.10);
  color:#2563eb;
  font-weight: 900;
  cursor:pointer;
  white-space:nowrap;
}
.mini-btn--danger{ background: rgba(239,68,68,.10); color:#ef4444; }
.transfer-empty{ padding: 14px 8px; text-align:center; color:#94a3b8; font-weight: 800; }
@media (max-width: 900px){
  .transfer{ grid-template-columns: 1fr; }
}

</style>
