<template>
  <div class="tracker-page">
    <TopNavbar />
    <header class="header">
      <div class="header-top">
        <h1>QA 追蹤</h1>
      </div>

      <!-- 年份選擇器 -->
      <div class="year-filter">
        <label class="year-filter-label">統計年份：</label>
        <select v-model.number="selectedYear" @change="handleYearChange" class="year-filter-select">
          <option v-for="year in [new Date().getFullYear() + 1, new Date().getFullYear(), new Date().getFullYear() - 1, new Date().getFullYear() - 2, new Date().getFullYear() - 3]" :key="year" :value="year">
            {{ year }}年
          </option>
        </select>
      </div>

      <div class="summary">
        <div class="summary-item">
          <span class="label">總記錄數</span>
          <span class="amount">{{ totalRecordsByYear }}</span>
        </div>
        <div class="summary-item">
          <span class="label">執行中</span>
          <span class="amount in-progress">{{ yearlyInProgressCount }}</span>
        </div>
        <div class="summary-item">
          <span class="label">已完成</span>
          <span class="amount completed">{{ yearlyCompletedCount }}</span>
        </div>
      </div>
    </header>

    <main class="main-content">
      <!-- Modal：建立 / 編輯資料 -->
      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-panel" @click.stop>
          <div class="modal-header">
            <div>
              <h2 class="modal-title">{{ editingId ? '編輯資料' : '新增 Issue' }}</h2>
              <div class="modal-subtitle">* Issue Number 為必填，其餘欄位皆為選填</div>
            </div>
            <button class="btn-secondary" @click="closeModal">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
              關閉
            </button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="handleSubmit" class="record-form">
              <div class="form-grid">
                <!-- Row 1 -->
                <div class="form-group">
                  <label for="status">狀態</label>
                  <select id="status" v-model.number="form.status">
                    <option :value="1" selected>執行中</option>
                    <option :value="0">執行中止</option>
                    <option :value="2">完成</option>
                  </select>
                </div>
                <div class="form-group">
                  <label for="completedAt">完成日期</label>
                  <input type="date" id="completedAt" v-model="form.completedAt" />
                </div>
                <div class="form-group">
                  <label for="issueNumber">Issue Number</label>
                  <input type="number" id="issueNumber" v-model.number="form.issueNumber" @input="updateIssueLink" placeholder="例如 1234" />
                </div>
                <div class="form-group">
                  <label for="testPlan">Test Plan</label>
                  <select id="testPlan" v-model="form.testPlan">
                    <option value="0" selected>否</option>
                    <option value="1">是</option>
                  </select>
                </div>

                <!-- Row 2 -->
                <div class="form-group">
                  <label for="category">類型</label>
                  <select id="category" v-model.number="form.category">
                    <option :value="1" selected>BUG</option>
                    <option :value="2">改善</option>
                    <option :value="3">優化</option>
                    <option :value="4">模組</option>
                    <option :value="5">QA</option>
                  </select>
                </div>
                <div class="form-group" style="grid-column: span 3;">
                  <label for="feature">功能</label>
                  <input type="text" id="feature" v-model="form.feature" placeholder="文字描述" />
                </div>

                <!-- Row 3 -->
                <div class="form-group full-width">
                  <label for="memo">Memo</label>
                  <textarea id="memo" v-model="form.memo" rows="2" placeholder="備註（非必填）"></textarea>
                </div>

                <!-- Row 4 -->
                <div class="form-group" style="grid-column: span 2;">
                  <label for="testStartDate">開始測試日期</label>
                  <input type="date" id="testStartDate" v-model="form.testStartDate" />
                </div>
                <div class="form-group" style="grid-column: span 2;">
                  <label for="etaDate">預計交付日期</label>
                  <input type="date" id="etaDate" v-model="form.etaDate" />
                </div>

                <!-- Row 5 -->
                <div class="form-group">
                  <label for="verifyFailed">驗證失敗</label>
                  <select id="verifyFailed" v-model.number="form.verifyFailed">
                    <option :value="0" selected>否</option>
                    <option :value="1">是</option>
                  </select>
                </div>
                <div class="form-group">
                  <label for="testCases">測試案例</label>
                  <input type="number" id="testCases" v-model.number="form.testCases" value="0" min="0" />
                </div>
                <div class="form-group">
                  <label for="fileCount">檔案數量</label>
                  <input type="number" id="fileCount" v-model.number="form.fileCount" value="0" min="0" />
                </div>
                <div class="form-group"></div>

                <!-- Row 6 -->
                <div class="form-group">
                  <label for="bugFound">發現 BUG</label>
                  <select id="bugFound" v-model.number="form.bugFound">
                    <option :value="0" selected>否</option>
                    <option :value="1">是</option>
                  </select>
                </div>
                <div class="form-group">
                  <label for="optimizationPoints">可優化項目</label>
                  <input type="number" id="optimizationPoints" v-model.number="form.optimizationPoints" placeholder="數字；僅在 發現BUG=1 時可填" min="0" step="1" :disabled="form.bugFound !== 1" />
                </div>
                <div class="form-group" style="grid-column: span 2;"></div>
              </div>

              <div class="form-actions">
                <button type="submit" class="btn btn-primary">儲存</button>
                <button type="button" class="btn-secondary" @click="resetForm">重設</button>
                <a v-if="form.issueNumber" :href="getIssueLink(form.issueNumber)" target="_blank" class="issue-link-preview">
                  打開 Issue
                </a>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- List Card -->
      <section class="list-section">

        <!-- Title row -->
        <div class="section-title">
          <h2>資料列表</h2>
        </div>

        <!-- Filters block -->
        <div class="filters-block">
          <div class="filters-header">
            <h3>篩選條件</h3>
          </div>
          <div class="filter-grid">
            <div class="filter-group full-width">
              <label>關鍵字搜尋</label>
              <input type="text" v-model="filters.keyword" placeholder="搜尋功能、備註或 Issue 編號..." />
            </div>
            <div class="filter-group">
              <label>狀態</label>
              <select v-model.number="filters.status">
                <option :value="null">全部</option>
                <option :value="0">執行中止</option>
                <option :value="1">執行中</option>
                <option :value="2">完成</option>
              </select>
            </div>
            <div class="filter-group">
              <label>類型</label>
              <select v-model.number="filters.category">
                <option :value="null">全部</option>
                <option :value="1">BUG</option>
                <option :value="2">改善</option>
                <option :value="3">優化</option>
                <option :value="4">模組</option>
                <option :value="5">QA</option>
              </select>
            </div>
            <div class="filter-group">
              <label>Test Plan</label>
              <select v-model="filters.testPlan">
                <option value="">全部</option>
                <option value="1">是</option>
                <option value="0">否</option>
              </select>
            </div>
            <div class="filter-group">
              <label>發現BUG</label>
              <select v-model.number="filters.bugFound">
                <option :value="null">全部</option>
                <option :value="1">是</option>
                <option :value="0">否</option>
              </select>
            </div>
            <div class="filter-group full-width">
              <label>開始測試日期範圍</label>
              <DateRangePicker 
                v-model="filters.testStartDateRange" 
                placeholder="選擇開始測試日期範圍"
              />
            </div>
            <div class="filter-group full-width">
              <label>預計交付日期範圍</label>
              <DateRangePicker 
                v-model="filters.etaDateRange" 
                placeholder="選擇預計交付日期範圍"
              />
            </div>
          </div>
        </div>

        <!-- Buttons row -->
        <div class="buttons-row">
          <div class="buttons-left">
            <button class="btn btn-primary" @click="openModal">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              新增
            </button>
            <button class="btn btn-info" @click="loadRecords">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
              查詢
            </button>
            <button class="btn btn-warning" @click="filterByInProgress">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              執行中 {{ filteredInProgressCount }}
            </button>
            <button class="btn btn-secondary" @click="clearFilters">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
              清空
            </button>
          </div>
          <div class="buttons-right">
            <button class="btn btn-info" @click="copyInProgressLines">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16h8M8 12h8m-6-4h6M8 8H6a2 2 0 00-2 2v8a2 2 0 002 2h8a2 2 0 002-2v-2M10 4h8a2 2 0 012 2v8a2 2 0 01-2 2h-8a2 2 0 01-2-2V6a2 2 0 012-2z"/>
              </svg>
              複製執行中
            </button>
            <button class="btn btn-success" @click="exportExcel">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              匯出
            </button>
            <button class="btn btn-warning" @click="openGitlabModal">
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                <path d="M23.546 10.93L13.067.452c-.604-.603-1.582-.603-2.188 0L.452 10.93c-.6.605-.6 1.584 0 2.189l10.48 10.477c.604.604 1.582.604 2.186 0l10.428-10.477c.603-.603.603-1.582 0-2.189z"/>
              </svg>
              GitLab Issues
            </button>
          </div>
        </div>

        <div class="pagination-info">
          顯示第 {{ (currentPage - 1) * recordsPerPage + 1 }} - 
          {{ Math.min(currentPage * recordsPerPage, totalRecords) }} 筆，共 {{ totalRecords }} 筆
        </div>
        <div class="table-wrap">
          <table class="records-table">
            <colgroup>
              <col style="width:100px" />  <!-- 操作 -->
              <col style="width:80px" />  <!-- 序號 -->
              <col style="width:80px" />  <!-- Issue# -->
              <col style="width:80px" />  <!-- 狀態 -->
              <col style="width:80px" />  <!-- 類型 -->
              <col style="width:150px" />  <!-- 功能 -->
              <col style="width:120px" />  <!-- 開始測試日期 -->
              <col style="width:120px" />  <!-- 預計交付日期 -->
              <col style="width:120px" />  <!-- 完成日期 -->
              <col style="width:80px" />  <!-- 測試案例 -->
              <col style="width:300px" />  <!-- Memo -->
            </colgroup>
            <thead>
              <tr>
                <th>操作</th>
                <th>#</th>
                <th>Issue#</th>
                <th>狀態</th>
                <th>類型</th>
                <th>功能</th>
                <th>開始測試日期</th>
                <th>預計交付日期</th>
                <th>完成日期</th>
                <th>測試案例</th>
                <th>Memo</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(record, index) in records" :key="record.id">
                <td class="action-buttons">
                  <button class="action-btn edit-btn" @click="editRecord(record.id)" title="編輯">✏️</button>
                  <button class="action-btn delete-btn" @click="deleteRecord(record.id)" title="刪除">🗑️</button>
                </td>
                <td>{{ (currentPage - 1) * recordsPerPage + index + 1 }}</td>
                <td>
                  <a v-if="record.issueNumber" :href="getIssueLink(record.issueNumber)" target="_blank" class="issue-link">
                    {{ record.issueNumber }}
                  </a>
                  <span v-else>-</span>
                </td>
                <td>
                  <span :class="getStatusClass(record.status)">
                    {{ getStatusText(record.status) }}
                  </span>
                </td>
                <td>
                  <span :class="getCategoryClass(record.category)">
                    {{ getCategoryText(record.category) }}
                  </span>
                </td>
                <td class="feature-cell">{{ record.feature || '-' }}</td>
                <td>{{ formatDate(record.testStartDate) }}</td>
                <td>{{ formatDate(record.etaDate) }}</td>
                <td>{{ formatDate(record.completedAt) }}</td>
                <td>{{ record.testCases || 0 }}</td>
                <td class="memo-cell">{{ record.memo || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination">
          <div class="pagination-left">
            <label for="pageSize" class="pagination-label">顯示筆數：</label>
            <select id="pageSize" v-model.number="recordsPerPage" class="page-size-select">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
            </select>
            <span class="pagination-info">共 {{ totalRecords }} 筆 (第 {{ currentPage }}/{{ totalPages }} 頁)</span>
          </div>
          <div class="pagination-right">
            <button class="btn-secondary" @click="currentPage--" :disabled="currentPage === 1">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
              </svg>
              上一頁
            </button>
            <div class="page-jump">
              <span class="pagination-label">到第</span>
              <input type="number" v-model.number="jumpPage" min="1" :max="totalPages" class="page-input" @keyup.enter="jumpToPage" />
              <span class="pagination-label">頁</span>
            </div>
            <button class="btn-secondary" @click="currentPage++" :disabled="currentPage === totalPages">
              下一頁
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
            </button>
          </div>
        </div>
      </section>
    </main>

    <!-- GitLab Issues Modal -->
    <div v-if="showGitlabModal" class="modal-overlay" @click="closeGitlabModal">
      <div class="gitlab-modal-panel" @click.stop>
        <div class="gitlab-modal-header">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-orange-100 grid place-items-center">
              <svg class="w-6 h-6 text-orange-600" fill="currentColor" viewBox="0 0 24 24">
                <path d="M23.546 10.93L13.067.452c-.604-.603-1.582-.603-2.188 0L.452 10.93c-.6.605-.6 1.584 0 2.189l10.48 10.477c.604.604 1.582.604 2.186 0l10.428-10.477c.603-.603.603-1.582 0-2.189z"/>
              </svg>
            </div>
            <div>
              <h2 class="text-xl md:text-2xl font-bold text-slate-800">GitLab Issues</h2>
              <p class="text-xs text-slate-500">查詢並匯入 GitLab 專案的 Issues</p>
            </div>
          </div>
          <div class="flex flex-wrap items-center gap-2">
            <span class="text-sm font-medium text-slate-600 px-3 py-1.5 bg-slate-100 rounded-lg">
              已選 {{ selectedGitlabIssues.length }} 筆
            </span>
            <button class="btn-secondary text-sm" @click="toggleSelectAllGitlab">
              {{ allGitlabSelected ? '全不選' : '全選' }}
            </button>
            <button class="btn btn-primary text-sm" @click="importSelectedGitlabIssues" :disabled="importingGitlab">
              {{ importingGitlab ? '匯入中...' : '匯入選取' }}
            </button>
            <button class="btn-secondary text-sm" @click="closeGitlabModal">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
              關閉
            </button>
          </div>
        </div>
        <div class="gitlab-modal-search">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div>
              <label class="block text-sm font-medium text-slate-600 mb-1.5">Assignee</label>
              <input 
                v-model="gitlabFilters.assignee" 
                class="inp" 
                placeholder="例如：Welly.Chiu"
                @keyup.enter="searchGitlabIssues"
              />
            </div>
            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-slate-600 mb-1.5">Project 路徑</label>
              <input 
                v-model="gitlabFilters.project" 
                class="inp" 
                placeholder="例如：kh/imp 或 gitlab.com/group/project"
                @keyup.enter="searchGitlabIssues"
              />
            </div>
          </div>
          <div class="mt-3 flex justify-end">
            <button class="btn btn-info" @click="searchGitlabIssues" :disabled="loadingGitlabIssues">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
              {{ loadingGitlabIssues ? '查詢中...' : '查詢 Issues' }}
            </button>
          </div>
        </div>
        <div class="gitlab-modal-body">
          <div v-if="gitlabIssues.length === 0 && !loadingGitlabIssues" class="text-center py-12">
            <svg class="w-16 h-16 mx-auto text-slate-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
            <p class="text-slate-500 text-sm">請輸入查詢條件並按「查詢 Issues」</p>
          </div>
          <div v-else-if="loadingGitlabIssues" class="text-center py-12">
            <p class="text-slate-500 text-sm">讀取中...</p>
          </div>
          <ul v-else class="divide-y divide-slate-200">
            <li v-for="issue in gitlabIssues" :key="issue.iid" class="py-3">
              <div class="flex items-start gap-3">
                <input 
                  type="checkbox"
                  class="gl-select mt-1"
                  :checked="isGitlabIssueSelected(issue.iid)"
                  @change="toggleGitlabIssue(issue)"
                />
                <div class="flex-1">
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <a :href="issue.web_url" target="_blank" class="text-brand-700 font-semibold underline">
                        #{{ issue.iid }} {{ issue.title }}
                      </a>
                      <div class="text-xs text-slate-500 mt-1">
                        專案：{{ issue.references?.full || '' }}<br>
                        更新時間：{{ formatGitlabDate(issue.updated_at) }}
                      </div>
                    </div>
                    <button 
                      class="btn-secondary text-sm px-3 py-1.5"
                      @click="toggleGitlabIssueDetail(issue.iid)"
                    >
                      {{ expandedGitlabDetails.has(issue.iid) ? '隱藏' : '詳細資訊' }}
                    </button>
                  </div>
                  <div v-if="expandedGitlabDetails.has(issue.iid)" class="mt-3">
                    <div v-if="gitlabIssueNotes[issue.iid] === undefined" class="text-slate-500 text-sm">
                      讀取留言中…
                    </div>
                    <div v-else-if="gitlabIssueNotes[issue.iid].length === 0" class="text-slate-600 text-sm">
                      沒有留言。
                    </div>
                    <div v-else class="border rounded-xl p-3 bg-slate-50">
                      <div v-for="note in gitlabIssueNotes[issue.iid]" :key="note.id" class="mb-3">
                        <div class="text-sm font-medium text-slate-700">
                          {{ note.author?.name || note.author?.username || '未知' }}
                        </div>
                        <div class="text-xs text-slate-500">{{ formatGitlabDate(note.created_at) }}</div>
                        <div class="mt-1 whitespace-pre-wrap text-slate-800">{{ note.body || '' }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 通知已移至全局 ToastHost -->
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import DateRangePicker from '@/components/DateRangePicker.vue'
import TopNavbar from '@/components/TopNavbar.vue'
import { apiService } from '@/composables/useApi'
import { toast } from '@shared/composables/useToast'
import * as XLSX from 'xlsx'

const records = ref([])
const totalRecords = ref(0)
const totalRecordsByYear = ref(0) // 新增：按年份篩選的總記錄數
const selectedYear = ref(new Date().getFullYear()) // 新增：選擇的年份
const totalPages = ref(0)
const editingId = ref(null)
const currentPage = ref(1)
const recordsPerPage = ref(10)
const showModal = ref(false)
const showGitlabModal = ref(false)
const jumpPage = ref(1)

// GitLab Issues 相關狀態
const gitlabIssues = ref([])
const selectedGitlabIssues = ref([])
const allGitlabSelected = ref(false)
const loadingGitlabIssues = ref(false)
const importingGitlab = ref(false)
const expandedGitlabDetails = ref(new Set())
const gitlabIssueNotes = ref({})
const gitlabToken = ref(null)
const gitlabFilters = ref({
  assignee: 'Welly.Chiu',
  project: 'kh/imp'
})
const GITLAB_BASE = 'https://gitlab.fb-tek.com/api/v4'

const form = ref({
  issueNumber: null,
  status: 1,
  category: null,
  feature: '',
  memo: '',
  testPlan: '0',
  bugFound: 0,
  optimizationPoints: 0,
  verifyFailed: 0,
  testCases: 0,
  fileCount: 0,
  testStartDate: '',
  etaDate: '',
  completedAt: null
})

// 根據 Issue Number 自動生成 Issue Link
const getIssueLink = (issueNumber) => {
  if (!issueNumber) return ''
  return `https://gitlab.fb-tek.com/kh/imp/-/issues/${issueNumber}`
}

// 當 Issue Number 改變時，自動更新 Issue Link（用於表單顯示）
const updateIssueLink = () => {
  // Issue Link 會在提交時自動生成，這裡不需要做任何事
}

const filters = ref({
  status: null,
  category: null,
  issueNumber: null,
  keyword: '',
  testPlan: '',
  bugFound: null,
  testStartDateRange: [], // [startDate, endDate] 格式為 'YYYY-MM-DD'
  etaDateRange: [] // [startDate, endDate] 格式為 'YYYY-MM-DD'
})

// notification 已改用全局 toast 系統

const yearlyInProgressCount = ref(0) // 年度統計中的執行中數量
const yearlyCompletedCount = ref(0) // 年度統計中的已完成數量
const filteredInProgressCount = ref(0) // 目前查詢結果中的執行中數量

const getStatusText = (status) => {
  const statusMap = {
    0: '執行中止',
    1: '執行中',
    2: '完成'
  }
  return statusMap[status] || '未知'
}

const getStatusClass = (status) => {
  const classMap = {
    0: 'status-cancelled',
    1: 'status-in-progress',
    2: 'status-completed'
  }
  return classMap[status] || ''
}

const getCategoryText = (category) => {
  const categoryMap = {
    1: 'BUG',
    2: '改善',
    3: '優化',
    4: '模組',
    5: 'QA'
  }
  return categoryMap[category] || '未知'
}

const getCategoryClass = (category) => {
  const classMap = {
    1: 'chip-red',
    2: 'chip-indigo',
    3: 'chip-amber',
    4: 'chip-gray',
    5: 'chip-green'
  }
  return classMap[category] || 'chip-gray'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-TW')
}

// showNotification 已改用全局 toast 系統
const showNotification = (message, type = 'success') => {
  if (type === 'error') {
    toast.error(message)
  } else if (type === 'warning') {
    toast.warning(message)
  } else if (type === 'info') {
    toast.info(message)
  } else {
    toast.success(message)
  }
}

// 載入年度統計（總記錄數、執行中、已完成）
const loadYearlyStats = async (year = selectedYear.value, showLoading = true) => {
  try {
    const response = await apiService.getYearlyStats(year, showLoading)
    totalRecordsByYear.value = response.total || 0
    yearlyInProgressCount.value = response.inProgress || 0
    yearlyCompletedCount.value = response.completed || 0
  } catch (error) {
    console.error('載入年度統計失敗:', error)
    totalRecordsByYear.value = 0
    yearlyInProgressCount.value = 0
    yearlyCompletedCount.value = 0
  }
}

// 年份選擇器變化處理
const handleYearChange = async () => {
  await loadYearlyStats(selectedYear.value)
}

const buildRecordQueryParams = ({ forceStatus, page, size } = {}) => {
  const params = {
    page: page ?? currentPage.value - 1,
    size: size ?? recordsPerPage.value,
    status: forceStatus ?? filters.value.status,
    category: filters.value.category,
    issueNumber: filters.value.issueNumber,
    keyword: filters.value.keyword || undefined,
    testPlan: filters.value.testPlan || undefined,
    bugFound: filters.value.bugFound
  }

  if (filters.value.testStartDateRange && filters.value.testStartDateRange.length > 0) {
    const sortedDates = [...filters.value.testStartDateRange].sort()
    if (sortedDates[0]) {
      params.testStartDateFrom = sortedDates[0]
    }
    if (sortedDates.length > 1 && sortedDates[1]) {
      params.testStartDateTo = sortedDates[1]
    }
  }

  if (filters.value.etaDateRange && filters.value.etaDateRange.length > 0) {
    const sortedDates = [...filters.value.etaDateRange].sort()
    if (sortedDates[0]) {
      params.etaDateFrom = sortedDates[0]
    }
    if (sortedDates.length > 1 && sortedDates[1]) {
      params.etaDateTo = sortedDates[1]
    }
  }

  Object.keys(params).forEach(key => {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key]
    }
  })

  return params
}

const loadRecords = async () => {
  try {
    const params = buildRecordQueryParams()
    const response = await apiService.getRecords(params)
    records.value = response.content || []
    totalRecords.value = response.totalElements || 0
    totalPages.value = response.totalPages || 1
    
    // 更新統計信息（查詢結果中的狀態統計）
    if (response.stats) {
      filteredInProgressCount.value = response.stats.inProgress || 0
    } else {
      filteredInProgressCount.value = 0
    }

    // 載入年度統計（總記錄數、執行中、已完成）
    await loadYearlyStats(selectedYear.value, false)
    toast.success(`載入成功，共 ${totalRecords.value} 筆記錄`)
  } catch (error) {
    console.error('載入記錄失敗:', error)
    toast.error('載入記錄失敗')
  }
}


const handleSubmit = async () => {
  if (!form.value.issueNumber) {
    toast.error('請填寫 Issue Number')
    return
  }
  
  try {
    const recordData = { ...form.value }
    // 自動生成 Issue Link
    if (recordData.issueNumber) {
      recordData.issueLink = getIssueLink(recordData.issueNumber)
    }
    
    if (editingId.value) {
      await apiService.updateRecord(editingId.value, recordData)
      closeModal()
      toast.success('記錄已更新')
      await loadRecords()
    } else {
      await apiService.createRecord(recordData)
      closeModal()
      toast.success('記錄已新增')
      await loadRecords()
    }
  } catch (error) {
    console.error('儲存失敗:', error)
    toast.error('儲存失敗: ' + error.message)
  }
}

const openModal = () => {
  editingId.value = null
  resetForm()
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingId.value = null
  resetForm()
}

const openGitlabModal = async () => {
  showGitlabModal.value = true
  // 預設值
  if (!gitlabFilters.value.assignee) gitlabFilters.value.assignee = 'Welly.Chiu'
  if (!gitlabFilters.value.project) gitlabFilters.value.project = 'kh/imp'
  // 載入 token
  await loadGitlabToken()
  // 自動查詢
  await searchGitlabIssues()
}

const closeGitlabModal = () => {
  showGitlabModal.value = false
  gitlabIssues.value = []
  selectedGitlabIssues.value = []
  expandedGitlabDetails.value.clear()
  gitlabIssueNotes.value = {}
  allGitlabSelected.value = false
}

const filterByInProgress = () => {
  // 清除所有其他篩選條件，只保留狀態為執行中
  filters.value = {
    status: 1,
    category: null,
    issueNumber: null,
    keyword: '',
    testPlan: '',
    bugFound: null,
    testStartDateRange: [],
    etaDateRange: []
  }
  currentPage.value = 1
  loadRecords()
}

const clearFilters = () => {
  filters.value = {
    status: null,
    category: null,
    issueNumber: null,
    keyword: '',
    testPlan: '',
    bugFound: null,
    testStartDateRange: [],
    etaDateRange: []
  }
  currentPage.value = 1
  loadRecords()
}

const copyInProgressLines = async () => {
  try {
    const params = buildRecordQueryParams({
      forceStatus: 1,
      page: 0,
      size: 10000
    })

    const response = await apiService.getRecords(params)
    const rows = response.content || []

    if (rows.length === 0) {
      toast.info('目前沒有可複製的執行中資料')
      return
    }

    const lines = rows.map((record) => {
      const issueNumber = record.issueNumber ?? ''
      const memo = (record.memo ?? '').toString().replace(/\r?\n/g, ' ').trim()
      return `IMP_${issueNumber}:${memo}`
    })

    await navigator.clipboard.writeText(lines.join('\n'))
    toast.success(`已複製 ${lines.length} 筆執行中資料`)
  } catch (error) {
    console.error('複製執行中資料失敗:', error)
    toast.error('複製失敗: ' + (error.message || '未知錯誤'))
  }
}

const exportExcel = async () => {
  try {
    showNotification('正在準備匯出 Excel...', 'info')
    
    // 使用當前篩選條件獲取所有記錄
    const params = buildRecordQueryParams({ page: 0, size: 10000 })
    const response = await apiService.getRecords(params)
    const allRecords = response.content || []
    
    if (allRecords.length === 0) {
      showNotification('沒有資料可匯出', 'info')
      return
    }
    
    // 1) 產生表頭
    const headers = ['ISSUE#', '狀態', '類型', '功能', 'Test Plan', 'BUG', '驗證失敗', '開始測試日期', '預計交付日期', '完成日期', '測試案例', 'MEMO']
    
    // 2) 轉換工具
    const statusText = (v) => {
      const n = Number(v)
      if (n === 1) return '執行中'
      if (n === 0) return '執行中止'
      if (n === 2) return '完成'
      return ''
    }
    
    const categoryText = (v) => {
      const map = { 1: 'BUG', 2: '改善', 3: '優化', 4: '模組', 5: 'QA' }
      return map[Number(v)] || ''
    }
    
    const toYN = (v) => (v === 1 || v === '1' || String(v).toUpperCase() === 'Y') ? 'Y' : 'N'
    
    const fmt = (v) => {
      if (!v) return ''
      try {
        const d = new Date(v)
        if (isNaN(d.getTime())) return ''
        const yyyy = d.getFullYear()
        const mm = String(d.getMonth() + 1).padStart(2, '0')
        const dd = String(d.getDate()).padStart(2, '0')
        return `${yyyy}-${mm}-${dd}`
      } catch {
        return ''
      }
    }
    
    // 3) 取得用來判斷季度的日期
    const pickDate = (r) => {
      let d = r?.testStartDate || r?.completedAt || r?.createdAt
      if (!d) return null
      return new Date(d)
    }
    
    const getQuarter = (d) => {
      if (!(d instanceof Date) || isNaN(d.getTime())) return null
      const m = d.getMonth() + 1 // 1~12
      if (m >= 1 && m <= 3) return 'Q1'
      if (m >= 4 && m <= 6) return 'Q2'
      if (m >= 7 && m <= 9) return 'Q3'
      if (m >= 10 && m <= 12) return 'Q4'
      return null
    }
    
    // 4) 把 rows 依季度分桶
    const buckets = { Q1: [], Q2: [], Q3: [], Q4: [] }
    for (const r of allRecords) {
      const d = pickDate(r)
      const q = getQuarter(d) || 'Q1' // 沒日期就先歸 Q1，避免遺漏
      const row = [
        r.issueNumber ?? '',
        statusText(r.status),
        categoryText(r.category),
        r.feature ?? '',
        toYN(r.testPlan),
        toYN(r.bugFound),
        toYN(r.verifyFailed),
        fmt(r.testStartDate),
        fmt(r.etaDate),
        fmt(r.completedAt),
        r.testCases ?? '',
        (r.memo ?? '').toString().replaceAll('\n', ' ')
      ]
      buckets[q].push(row)
    }
    
    // 5) 產生四個季度的 sheet
    const wb = XLSX.utils.book_new()
    const wsQ1 = XLSX.utils.aoa_to_sheet([headers, ...buckets.Q1])
    const wsQ2 = XLSX.utils.aoa_to_sheet([headers, ...buckets.Q2])
    const wsQ3 = XLSX.utils.aoa_to_sheet([headers, ...buckets.Q3])
    const wsQ4 = XLSX.utils.aoa_to_sheet([headers, ...buckets.Q4])
    XLSX.utils.book_append_sheet(wb, wsQ1, 'Q1列表')
    XLSX.utils.book_append_sheet(wb, wsQ2, 'Q2列表')
    XLSX.utils.book_append_sheet(wb, wsQ3, 'Q3列表')
    XLSX.utils.book_append_sheet(wb, wsQ4, 'Q4列表')
    
    // 6) 摘要統計（依開始測試日期分月 + 季度彙總，限定狀態=完成）
    const monthly = new Map() // key: 'YYYY-MM' -> {A..G}
    for (const r of allRecords) {
      if (Number(r.status) !== 2) continue // 僅統計完成
      
      let d = r?.testStartDate || r?.completedAt || r?.createdAt
      if (!d) continue
      d = new Date(d)
      if (!(d instanceof Date) || isNaN(d.getTime())) continue
      
      const y = d.getFullYear()
      const m = d.getMonth() + 1
      const key = `${y}-${String(m).padStart(2, '0')}`
      
      if (!monthly.has(key)) monthly.set(key, { A: 0, B: 0, C: 0, D: 0, E: 0, F: 0, G: 0 })
      const acc = monthly.get(key)
      
      const bug = Number(r.bugFound ?? 0)
      const tp = (r.testPlan === 1 || r.testPlan === '1') ? 1 : 0
      const opt = Number(r.optimizationPoints ?? 0)
      const cases = Number(r.testCases ?? 0)
      const vf = Number(r.verifyFailed ?? 0)
      const files = Number(r.fileCount ?? 0)
      
      if (bug === 1) acc.A += 1 // A: 發現BUG=1 的筆數
      acc.B += Number.isFinite(opt) ? opt : 0 // B: 可優化項目數
      if (tp === 1) acc.C += 1 // C: Test Plan=1 的筆數
      if (bug === 0 && tp === 0) acc.D += 1 // D: BUG=0 且 TP=0 的筆數
      acc.E += Number.isFinite(cases) ? cases : 0 // E: 測試案例總數
      if (vf === 1) acc.F += 1 // F: 驗證失敗=1 的筆數
      acc.G += Number.isFinite(files) ? files : 0 // G: 檔案數總數
    }
    
    const lines = [['Monthly Summary']]
    const sortedMonths = Array.from(monthly.entries()).sort((a, b) => a[0].localeCompare(b[0]))
    for (const [key, v] of sortedMonths) {
      const monthNum = Number(key.slice(5))
      const text = `${monthNum}月 發現 ${v.B} 個可優化項目，開立 ${v.A} 張優化 issues｜ 產生 ${v.C} 份測試報告｜ ${v.D} 張驗證單，複驗 ${v.F} 張單｜ 建立 ${v.G} 個測試範本、${v.E} 項測試案例`
      lines.push([text])
    }
    
    function quarterOf(month) {
      return Math.floor((month - 1) / 3) + 1
    }
    const quarterly = new Map() // key: 'YYYY-Qn' -> {A..G}
    for (const [ym, v] of sortedMonths) {
      const y = ym.slice(0, 4)
      const m = Number(ym.slice(5))
      const qKey = `${y}-Q${quarterOf(m)}`
      if (!quarterly.has(qKey)) quarterly.set(qKey, { A: 0, B: 0, C: 0, D: 0, E: 0, F: 0, G: 0 })
      const acc = quarterly.get(qKey)
      acc.A += v.A
      acc.B += v.B
      acc.C += v.C
      acc.D += v.D
      acc.E += v.E
      acc.F += v.F
      acc.G += v.G
    }
    
    lines.push([''], ['Quarterly Summary'])
    const sortedQuarters = Array.from(quarterly.entries()).sort((a, b) => a[0].localeCompare(b[0]))
    for (const [qKey, v] of sortedQuarters) {
      const [year, qLabel] = qKey.split('-')
      const textQ = `${qLabel}: 發現 ${v.B} 個可優化項目，開立 ${v.A} 張優化 issues｜ 產生 ${v.C} 份測試報告｜ ${v.D} 張驗證單，複驗 ${v.F} 張單｜ 建立 ${v.G} 個測試範本、${v.E} 項測試案例`
      lines.push([`${year} ${textQ}`])
    }
    
    const wsSummary = XLSX.utils.aoa_to_sheet(lines)
    XLSX.utils.book_append_sheet(wb, wsSummary, '摘要')
    
    // 7) 輸出
    XLSX.writeFile(wb, 'records.xlsx')
    showNotification(`成功匯出 ${allRecords.length} 筆記錄`, 'success')
  } catch (error) {
    console.error('匯出失敗:', error)
    showNotification('匯出失敗: ' + error.message, 'error')
  }
}

const editRecord = async (id) => {
  const record = records.value.find(r => r.id === id)
  if (!record) {
    toast.error('找不到要編輯的記錄')
    return
  }
  
  editingId.value = id
  form.value = {
    issueNumber: record.issueNumber,
    status: record.status,
    category: record.category,
    feature: record.feature || '',
    memo: record.memo || '',
    testPlan: record.testPlan || '0',
    bugFound: record.bugFound || 0,
    optimizationPoints: record.optimizationPoints || 0,
    verifyFailed: record.verifyFailed || 0,
    testCases: record.testCases || 0,
    fileCount: record.fileCount || 0,
    testStartDate: record.testStartDate || '',
    etaDate: record.etaDate || '',
    completedAt: record.completedAt || null
  }
  
  showModal.value = true
}

const deleteRecord = async (id) => {
  if (!confirm('確定要刪除這筆記錄嗎？')) {
    return
  }
  
  try {
    await apiService.deleteRecord(id)
    toast.success('記錄已刪除')
    await loadRecords()
  } catch (error) {
    console.error('刪除失敗:', error)
    toast.error('刪除失敗')
  }
}

const resetForm = () => {
  editingId.value = null
  form.value = {
    issueNumber: null,
    status: 1,
    category: null,
    feature: '',
    memo: '',
    testPlan: '0',
    bugFound: 0,
    optimizationPoints: 0,
    verifyFailed: 0,
    testCases: 0,
    fileCount: 0,
    testStartDate: '',
    etaDate: '',
    completedAt: null
  }
}

// GitLab Issues 相關函數
const loadGitlabToken = async () => {
  if (gitlabToken.value) return gitlabToken.value
  
  try {
    const response = await apiService.request('/system-settings/gitlab_token')
    
    if (!response || !response.setting || !response.setting.settingValue || response.setting.settingValue.trim() === '') {
      throw new Error('GitLab token 配置為空，請先在系統設定中配置 GitLab API Token')
    }
    gitlabToken.value = response.setting.settingValue
    console.log('成功載入 GitLab token')
    return gitlabToken.value
  } catch (error) {
    console.error('載入 GitLab token 失敗:', error)
    showNotification('載入 GitLab token 失敗: ' + error.message, 'error')
    throw error
  }
}

const searchGitlabIssues = async () => {
  if (!gitlabFilters.value.project?.trim()) {
    showNotification('請輸入 Project 路徑', 'error')
    return
  }
  
  loadingGitlabIssues.value = true
  gitlabIssues.value = []
  
  try {
    const token = await loadGitlabToken()
    const project = encodeURIComponent(gitlabFilters.value.project.trim())
    const assignee = gitlabFilters.value.assignee?.trim()
    
    const params = new URLSearchParams({ state: 'opened', per_page: '50' })
    if (assignee) params.set('assignee_username', assignee)
    
    const url = `${GITLAB_BASE}/projects/${project}/issues?${params.toString()}`
    const res = await fetch(url, {
      headers: { 'PRIVATE-TOKEN': token, 'Accept': 'application/json' }
    })
    
    if (!res.ok) throw new Error('GitLab API 錯誤：' + res.status)
    
    const issues = await res.json()
    if (!Array.isArray(issues) || issues.length === 0) {
      gitlabIssues.value = []
      showNotification('沒有符合條件的 Issue', 'info')
      return
    }
    
    gitlabIssues.value = issues
    selectedGitlabIssues.value = []
    allGitlabSelected.value = false
    showNotification(`找到 ${issues.length} 筆 Issues`, 'success')
  } catch (error) {
    console.error('查詢 GitLab Issues 失敗:', error)
    showNotification('查詢失敗: ' + error.message, 'error')
    gitlabIssues.value = []
  } finally {
    loadingGitlabIssues.value = false
  }
}

const toggleGitlabIssue = (issue) => {
  const index = selectedGitlabIssues.value.findIndex(i => i.iid === issue.iid)
  if (index >= 0) {
    selectedGitlabIssues.value.splice(index, 1)
  } else {
    selectedGitlabIssues.value.push(issue)
  }
  updateGitlabSelectedCount()
}

const isGitlabIssueSelected = (iid) => {
  return selectedGitlabIssues.value.some(i => i.iid === iid)
}

const toggleSelectAllGitlab = () => {
  allGitlabSelected.value = !allGitlabSelected.value
  if (allGitlabSelected.value) {
    selectedGitlabIssues.value = [...gitlabIssues.value]
  } else {
    selectedGitlabIssues.value = []
  }
  updateGitlabSelectedCount()
}

const updateGitlabSelectedCount = () => {
  // 這個函數已經通過 computed 在模板中處理了
}

const toggleGitlabIssueDetail = async (iid) => {
  if (expandedGitlabDetails.value.has(iid)) {
    expandedGitlabDetails.value.delete(iid)
    return
  }
  
  expandedGitlabDetails.value.add(iid)
  
  // 如果已經載入過 notes，就不重新載入
  if (gitlabIssueNotes.value[iid] !== undefined) {
    return
  }
  
  try {
    const token = await loadGitlabToken()
    const project = encodeURIComponent(gitlabFilters.value.project.trim())
    const url = `${GITLAB_BASE}/projects/${project}/issues/${iid}/notes?per_page=100`
    const res = await fetch(url, {
      headers: { 'PRIVATE-TOKEN': token, 'Accept': 'application/json' }
    })
    
    if (!res.ok) throw new Error(`讀取留言失敗：${res.status}`)
    
    const notes = await res.json()
    if (!Array.isArray(notes)) {
      gitlabIssueNotes.value[iid] = []
      return
    }
    
    // 過濾掉系統訊息
    const humanNotes = notes.filter(n => !n.system)
    gitlabIssueNotes.value[iid] = humanNotes
  } catch (error) {
    console.error('讀取留言失敗:', error)
    gitlabIssueNotes.value[iid] = []
  }
}

const formatGitlabDate = (dateStr) => {
  if (!dateStr) return ''
  try {
    return new Date(dateStr).toLocaleString('zh-TW')
  } catch {
    return dateStr
  }
}

const thisFriday = (from = new Date()) => {
  const d = new Date(from)
  const day = d.getDay()
  const map = { 0: 7, 1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 6 }
  const dow = map[day]
  const delta = 5 - dow
  d.setDate(d.getDate() + delta)
  return d
}

const formatDateInput = (d) => {
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const importSelectedGitlabIssues = async () => {
  if (selectedGitlabIssues.value.length === 0) {
    showNotification('請先勾選要匯入的資料', 'error')
    return
  }
  
  importingGitlab.value = true
  
  try {
    const today = new Date()
    let successCount = 0
    let skippedCount = 0
    let failCount = 0
    
    for (const issue of selectedGitlabIssues.value) {
      const iid = Number(issue.iid)
      const title = issue.title || ''
      const url = issue.web_url || ''
      
      // 檢查是否已存在相同 issue_number 的記錄
      try {
        const checkParams = { issueNumber: iid, page: 0, size: 1000 } // 查詢所有相同 issueNumber 的記錄
        const checkResponse = await apiService.getRecords(checkParams)
        if (checkResponse.content && checkResponse.content.length > 0) {
          // 檢查是否有一筆記錄的 bugFound != 1
          const hasBugFoundNotOne = checkResponse.content.some(record => 
            record.bugFound !== 1 && record.bugFound !== '1'
          )
          if (hasBugFoundNotOne) {
            console.log(`Issue ${iid} 已存在且其中一筆 bugFound!=1，略過匯入`)
            skippedCount++
            continue
          }
          // 如果所有記錄的 bugFound = 1，則繼續匯入（新增一筆資料）
        }
      } catch (err) {
        console.error('檢查記錄失敗:', err)
      }
      
      // 準備匯入資料
      const payload = {
        status: 1,
        completedAt: null,
        issueNumber: iid,
        testPlan: '0',
        category: null,
        feature: '透過系統匯入請手動調整',
        memo: title,
        testStartDate: formatDateInput(today),
        etaDate: formatDateInput(thisFriday(today)),
        bugFound: 0,
        optimizationPoints: 0,
        verifyFailed: 0,
        testCases: 0,
        fileCount: 0,
        issueLink: url
      }
      
      try {
        // 新增一筆資料
        await apiService.createRecord(payload)
        successCount++
      } catch (err) {
        console.error(`匯入 Issue ${iid} 失敗：`, err)
        failCount++
      }
    }
    
    const message = `匯入完成：\n成功 ${successCount} 筆\n略過（已存在） ${skippedCount} 筆\n失敗 ${failCount} 筆`
    showNotification(message, successCount > 0 ? 'success' : 'info')
    
    closeGitlabModal()
    await loadRecords() // 重新載入表格
  } catch (error) {
    console.error('匯入發生錯誤:', error)
    showNotification('匯入發生錯誤: ' + error.message, 'error')
  } finally {
    importingGitlab.value = false
  }
}


watch(() => filters.value, (newVal, oldVal) => {
  // 排除日期範圍的變化，日期範圍只有在點擊"確定"按鈕時才應該觸發查詢
  // 但由於用戶要求點擊"搜尋"按鈕才查詢，所以這裡完全移除自動查詢
  // 日期範圍的變化不會觸發查詢，只有點擊"搜尋"按鈕才會
  if (newVal.testStartDateRange !== oldVal?.testStartDateRange || 
      newVal.etaDateRange !== oldVal?.etaDateRange) {
    // 日期範圍變化時，不自動觸發查詢
    return
  }
  currentPage.value = 1
  loadRecords()
}, { deep: true })

watch(() => currentPage.value, (newVal) => {
  jumpPage.value = newVal
  loadRecords()
})

watch(() => recordsPerPage.value, () => {
  currentPage.value = 1
  loadRecords()
})

// 監聽狀態變化，自動設置完成日期
watch(() => form.value.status, (newStatus, oldStatus) => {
  // 當狀態改為完成(2)時，自動設置完成日期為當日
  if (newStatus === 2 && (!form.value.completedAt || form.value.completedAt === '')) {
    form.value.completedAt = new Date().toISOString().split('T')[0]
  }
  // 如果狀態從完成改為其他狀態，可以選擇清空完成日期（可選）
  // else if (oldStatus === 2 && newStatus !== 2) {
  //   form.value.completedAt = null
  // }
})


const jumpToPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    currentPage.value = jumpPage.value
  } else {
    jumpPage.value = currentPage.value
  }
}

// ESC 鍵關閉 Modal
const handleEscape = (e) => {
  if (e.key === 'Escape' && showModal.value) {
    closeModal()
  }
}

onMounted(async () => {
  await loadRecords()
  document.addEventListener('keydown', handleEscape)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscape)
})
</script>

<style scoped>
.tracker-page {
  min-height: 100vh;
  background: #f5f5f5;
  color: #333;
  padding: 20px;
}

.tracker-page::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 50%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(118, 75, 162, 0.3) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 22px;
  border-radius: 14px;
  margin-bottom: 22px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.header h1 {
  font-size: clamp(2rem, 3.6vw, 2.7rem);
  margin: 0;
  text-align: center;
  flex: 1;
  letter-spacing: -0.03em;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(15px);
  padding: 10px 18px;
  border-radius: 12px;
}

.logout-btn {
  background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.logout-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 0.85rem;
  margin-bottom: 1.25rem;
}

.summary-item {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  padding: 1.1rem;
  border-radius: 14px;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: var(--shadow-lg);
  transition: var(--transition);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 104px;
}

.summary-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.5) 0%, transparent 100%);
}

.summary-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-lg);
  background: rgba(255, 255, 255, 0.2);
}

.summary-item .label {
  display: block;
  margin-bottom: 0.5rem;
  opacity: 0.95;
  font-size: 0.76rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.summary-item .amount {
  font-size: 1.7rem;
  font-weight: 800;
  line-height: 1.2;
}

.summary-item .amount.in-progress {
  color: #fbbf24;
  text-shadow: 0 2px 10px rgba(251, 191, 36, 0.3);
}

.summary-item .amount.completed {
  color: #4ade80;
  text-shadow: 0 2px 10px rgba(74, 222, 128, 0.3);
}

.year-filter {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 14px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.year-filter-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: #e2e8f0;
  margin: 0;
}

.year-filter-select {
  background: rgba(255, 255, 255, 0.95);
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 0.84rem;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 100px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.year-filter-select:focus {
  outline: none;
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(129, 140, 248, 0.2);
  background: white;
}

.year-filter-select:hover {
  border-color: #94a3b8;
  background: white;
}

.main-content {
  max-width: 1180px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* Modal 樣式 */
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
  max-width: 50rem;
  background: white;
  border-radius: 0.875rem;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.14);
  border: 1px solid #e2e8f0;
  margin: 1rem 0;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(to right, #f8fafc, white);
  border-radius: 0.875rem 0.875rem 0 0;
}

.modal-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.modal-subtitle {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: 0.25rem;
}

.modal-body {
  padding: 1.25rem;
}

.list-section {
  background: white;
  border-radius: 0.875rem;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.08);
  padding: 1.25rem;
  border: 1px solid #e2e8f0;
}

.section-title {
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #e2e8f0;
}

.section-title h2 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filters-block {
  background: #f8fafc;
  border-radius: 0.875rem;
  padding: 1rem;
  margin-bottom: 1rem;
  border: 1px solid #e2e8f0;
}

.filters-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.filters-header h3 {
  font-weight: 600;
  color: #475569;
  margin: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.875rem;
  margin-bottom: 1.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  display: block;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0.25rem;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.7rem 0.8rem;
  border-radius: 0.65rem;
  border: 1px solid #cbd5e1;
  outline: none;
  background: white;
  transition: all 0.2s ease;
  font-size: 0.875rem;
  color: #1e293b;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
  border-color: #818cf8;
}

.form-group input:hover:not(:focus),
.form-group select:hover:not(:focus),
.form-group textarea:hover:not(:focus) {
  border-color: #94a3b8;
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: #94a3b8;
}

.form-group select option {
  background: white;
  color: #1e293b;
  padding: 0.5rem;
}

.form-actions {
  display: flex;
  gap: var(--spacing-md);
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.buttons-row {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

@media (min-width: 768px) {
  .buttons-row {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
}

.buttons-left,
.buttons-right {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.btn {
  padding: 0.65rem 1rem;
  border-radius: 0.65rem;
  font-weight: 600;
  font-size: 0.84rem;
  transition: all 0.2s ease;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  white-space: nowrap;
  min-height: 2.35rem;
}

.btn:hover {
  transform: translateY(-1px);
}

.btn:active {
  transform: translateY(0);
}

.btn-primary {
  background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
}

.btn-primary:hover {
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
}

.btn-info {
  background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.3);
}

.btn-info:hover {
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.4);
}

.btn-warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
}

.btn-warning:hover {
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
}

.btn-success {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.btn-success:hover {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.btn-secondary {
  padding: 0.65rem 1rem;
  border-radius: 0.65rem;
  border: 1px solid #cbd5e1;
  background: white;
  color: #475569;
  font-weight: 600;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  cursor: pointer;
  min-height: 2.35rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn-secondary:hover {
  background: #f8fafc;
  border-color: #94a3b8;
  transform: translateY(-1px);
}

.issue-link-preview {
  color: #4f46e5;
  font-weight: 600;
  text-decoration: underline;
  margin-left: 0.5rem;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 0.75rem;
}

@media (min-width: 768px) {
  .filter-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1024px) {
  .filter-grid {
    grid-template-columns: repeat(5, 1fr);
  }
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.filter-group.full-width {
  grid-column: 1 / -1;
}

@media (min-width: 1024px) {
  .filter-group.full-width {
    grid-column: span 2;
  }
}

.filter-group label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0.25rem;
}

.filter-group input,
.filter-group select {
  width: 100%;
  padding: 0.7rem 0.8rem;
  border-radius: 0.65rem;
  border: 1px solid #cbd5e1;
  outline: none;
  background: white;
  transition: all 0.2s ease;
  font-size: 0.875rem;
  color: #1e293b;
  box-sizing: border-box;
}

.filter-group input:focus,
.filter-group select:focus {
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
  border-color: #818cf8;
}

.filter-group input::placeholder {
  color: #94a3b8;
}

.filter-group select option {
  background: white;
  color: #1e293b;
}


.table-wrap {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 0.875rem;
}

.records-table {
  width: 100%;
  min-width: 1320px;
  table-layout: fixed;
  border-collapse: collapse;
  background: white;
  font-size: 0.84rem;
}

.records-table th,
.records-table td {
  padding: 0.65rem 0.75rem;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
  white-space: normal;
  word-break: break-word;
}

.records-table th {
  position: sticky;
  top: 0;
  background: #f8fafc;
  white-space: nowrap;
  font-weight: 600;
  z-index: 10;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.72rem;
}

.records-table tbody tr {
  transition: background-color 0.15s ease;
}

.records-table tbody tr:hover {
  background: #f8fafc;
}

.records-table tbody tr:last-child td {
  border-bottom: none;
}

.memo-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.issue-link {
  color: #4f46e5;
  text-decoration: none;
  font-weight: 600;
}

.issue-link:hover {
  text-decoration: underline;
  color: #4338ca;
}

.feature-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-in-progress {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}

.status-completed {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}

.status-cancelled {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.chip-green {
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}

.chip-gray {
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #e2e8f0;
}

.chip-red {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.chip-indigo {
  background: #eef2ff;
  color: #3730a3;
  border: 1px solid #c7d2fe;
}

.chip-amber {
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}

.action-buttons {
  display: flex;
  gap: 5px;
}

.action-btn {
  padding: 0.375rem 0.75rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s ease;
  background: white;
}

.edit-btn {
  color: #007bff;
  border-color: #007bff;
}

.edit-btn:hover {
  background: #007bff;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 123, 255, 0.3);
}

.delete-btn {
  color: #dc3545;
  border-color: #dc3545;
}

.delete-btn:hover {
  background: #dc3545;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(220, 53, 69, 0.3);
}

.pagination-info {
  margin-bottom: 10px;
  opacity: 0.8;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.85rem;
  margin-top: 1.2rem;
  flex-wrap: wrap;
}

.pagination-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.pagination-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.page-size-select {
  padding: 0.625rem 0.875rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 0.75rem;
  background: white;
  color: #1e293b;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 80px;
}

.page-size-select:focus {
  outline: none;
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}

.page-size-select option {
  background: white;
  color: #1e293b;
  padding: 8px;
}

.page-size-info {
  font-size: 14px;
  opacity: 0.9;
  font-weight: 500;
}

.pagination button {
  padding: 0.55rem 0.9rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.65rem;
  background: white;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 600;
  font-size: 0.82rem;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.pagination button:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #94a3b8;
  transform: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

.page-jump {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.page-input {
  padding: 0.5rem 0.75rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 0.5rem;
  background: white;
  color: #1e293b;
  font-size: 0.875rem;
  font-weight: 600;
  width: 60px;
  text-align: center;
  transition: all 0.2s ease;
}

.page-input:focus {
  outline: none;
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}

.pagination-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #475569;
  white-space: nowrap;
}

.notification {
  position: fixed;
  bottom: var(--spacing-xl);
  left: var(--spacing-xl);
  padding: var(--spacing-lg) var(--spacing-xl);
  border-radius: var(--border-radius-lg);
  color: white;
  font-weight: 600;
  z-index: 10000;
  animation: slideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-xl);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  min-width: 300px;
}

.notification.success {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.95) 0%, rgba(5, 150, 105, 0.95) 100%);
}

.notification.error {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.95) 0%, rgba(220, 38, 38, 0.95) 100%);
}

.notification.info {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.95) 0%, rgba(37, 99, 235, 0.95) 100%);
}

@keyframes slideIn {
  from {
    transform: translateX(-120%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.w-4 {
  width: 1rem;
}

.h-4 {
  height: 1rem;
}

.w-5 {
  width: 1.25rem;
}

.h-5 {
  height: 1.25rem;
}

/* GitLab Modal 樣式 */
.gitlab-modal-panel {
  width: 100%;
  max-width: 56rem;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  margin: 2rem 0;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.gitlab-modal-header {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(to right, #fff7ed, white);
  border-radius: 1rem 1rem 0 0;
}

.gitlab-modal-search {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: white;
}

.gitlab-modal-body {
  padding: 1.25rem 1.5rem;
  max-height: 60vh;
  overflow-y: auto;
  background: #f8fafc;
}

.inp {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border-radius: 0.75rem;
  border: 1.5px solid #cbd5e1;
  outline: none;
  background: white;
  transition: all 0.2s ease;
  font-size: 0.875rem;
  color: #1e293b;
  box-sizing: border-box;
}

.inp:focus {
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
  border-color: #818cf8;
}

.inp::placeholder {
  color: #94a3b8;
}

.gl-select {
  cursor: pointer;
  width: 1.25rem;
  height: 1.25rem;
}

.text-brand-700 {
  color: #4338ca;
}

.grid {
  display: grid;
}

.grid-cols-1 {
  grid-template-columns: repeat(1, minmax(0, 1fr));
}

@media (min-width: 768px) {
  .md\:grid-cols-3 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  
  .md\:col-span-2 {
    grid-column: span 2 / span 2;
  }
  
  .md\:text-2xl {
    font-size: 1.5rem;
    line-height: 2rem;
  }
  
  .md\:px-6 {
    padding-left: 1.5rem;
    padding-right: 1.5rem;
  }
  
  .md\:flex-row {
    flex-direction: row;
  }
  
  .md\:items-center {
    align-items: center;
  }
  
  .md\:justify-between {
    justify-content: space-between;
  }
}

.flex {
  display: flex;
}

.items-center {
  align-items: center;
}

.items-start {
  align-items: flex-start;
}

.justify-between {
  justify-content: space-between;
}

.gap-2 {
  gap: 0.5rem;
}

.gap-3 {
  gap: 0.75rem;
}

.flex-wrap {
  flex-wrap: wrap;
}

.flex-1 {
  flex: 1 1 0%;
}

.divide-y > :not([hidden]) ~ :not([hidden]) {
  border-top-width: 1px;
  border-color: #e2e8f0;
}

.py-3 {
  padding-top: 0.75rem;
  padding-bottom: 0.75rem;
}

.mt-1 {
  margin-top: 0.25rem;
}

.mt-3 {
  margin-top: 0.75rem;
}

.mb-1\.5 {
  margin-bottom: 0.375rem;
}

.mb-3 {
  margin-bottom: 0.75rem;
}

.mb-4 {
  margin-bottom: 1rem;
}

.text-sm {
  font-size: 0.875rem;
  line-height: 1.25rem;
}

.text-xs {
  font-size: 0.75rem;
  line-height: 1rem;
}

.text-xl {
  font-size: 1.25rem;
  line-height: 1.75rem;
}

.font-bold {
  font-weight: 700;
}

.font-semibold {
  font-weight: 600;
}

.font-medium {
  font-weight: 500;
}

.text-slate-500 {
  color: #64748b;
}

.text-slate-600 {
  color: #475569;
}

.text-slate-700 {
  color: #334155;
}

.text-slate-800 {
  color: #1e293b;
}

.text-red-600 {
  color: #dc2626;
}

.underline {
  text-decoration-line: underline;
}

.whitespace-pre-wrap {
  white-space: pre-wrap;
}

.rounded-lg {
  border-radius: 0.5rem;
}

.rounded-xl {
  border-radius: 0.75rem;
}

.bg-slate-50 {
  background-color: #f8fafc;
}

.bg-slate-100 {
  background-color: #f1f5f9;
}

.bg-orange-100 {
  background-color: #ffedd5;
}

.text-orange-600 {
  color: #ea580c;
}

.px-3 {
  padding-left: 0.75rem;
  padding-right: 0.75rem;
}

.py-1\.5 {
  padding-top: 0.375rem;
  padding-bottom: 0.375rem;
}

.p-3 {
  padding: 0.75rem;
}

.text-center {
  text-align: center;
}

.py-12 {
  padding-top: 3rem;
  padding-bottom: 3rem;
}

.mx-auto {
  margin-left: auto;
  margin-right: auto;
}

.w-4 {
  width: 1rem;
}

.h-4 {
  height: 1rem;
}

.w-6 {
  width: 1.5rem;
}

.h-6 {
  height: 1.5rem;
}

.w-10 {
  width: 2.5rem;
}

.h-10 {
  height: 2.5rem;
}

.w-16 {
  width: 4rem;
}

.h-16 {
  height: 4rem;
}

.place-items-center {
  place-items: center;
}

.block {
  display: block;
}
</style>
