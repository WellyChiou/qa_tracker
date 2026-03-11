export function createChurchScheduledJobsApi(apiRequest) {
  return {
    async createChurchScheduledJob(job) {
      const data = await apiRequest('/church/scheduled-jobs', {
        method: 'POST',
        body: JSON.stringify(job)
      }, '建立中...', true)
      if (data == null) throw new Error('建立任務失敗')
      return data
    },

    async deleteChurchScheduledJob(id) {
      await apiRequest(`/church/scheduled-jobs/${id}`, { method: 'DELETE' }, '刪除中...', true)
    },

    async executeChurchScheduledJob(id) {
      const data = await apiRequest(`/church/scheduled-jobs/${id}/execute`, { method: 'POST' }, '執行中...', true)
      if (data == null) throw new Error('執行任務失敗')
      return data
    },

    async getChurchJobExecutionById(executionId) {
      const data = await apiRequest(`/church/scheduled-jobs/executions/${executionId}`, { method: 'GET' }, '', true)
      if (data == null) throw new Error('載入執行記錄失敗')
      return data
    },

    async getChurchJobExecutions(jobId) {
      const data = await apiRequest(`/church/scheduled-jobs/${jobId}/executions`, { method: 'GET' }, '載入記錄中...', true)
      if (data == null) throw new Error('載入執行記錄失敗')
      return data
    },

    async getChurchLatestJobExecution(jobId) {
      try {
        const data = await apiRequest(`/church/scheduled-jobs/${jobId}/executions/latest`, { method: 'GET' }, '', true)
        return data == null ? null : data
      } catch {
        return null
      }
    },

    async getChurchScheduledJobById(id) {
      const data = await apiRequest(`/church/scheduled-jobs/${id}`, { method: 'GET' }, '', true)
      if (data == null) throw new Error('載入任務失敗')
      return data
    },

    async getChurchScheduledJobs() {
      const data = await apiRequest('/church/scheduled-jobs', { method: 'GET' }, '載入任務中...', true)
      if (data == null) throw new Error('載入任務失敗')
      return data
    },

    async toggleChurchScheduledJob(id, enabled) {
      const data = await apiRequest(`/church/scheduled-jobs/${id}/toggle?enabled=${enabled}`, {
        method: 'PUT'
      }, '更新中...', true)
      if (data == null) throw new Error('切換狀態失敗')
      return data
    },

    async updateChurchScheduledJob(id, job) {
      const data = await apiRequest(`/church/scheduled-jobs/${id}`, {
        method: 'PUT',
        body: JSON.stringify(job)
      }, '更新中...', true)
      if (data == null) throw new Error('更新任務失敗')
      return data
    }
  }
}
