import { createChurchApiModule } from '@shared/utils/churchApiModule'

export const {
  apiRequest,
  clearTokens,
  createChurchScheduledJob,
  deleteChurchScheduledJob,
  executeChurchScheduledJob,
  getAccessToken,
  getApiBaseUrl,
  getChurchJobExecutionById,
  getChurchJobExecutions,
  getChurchLatestJobExecution,
  getChurchScheduledJobById,
  getChurchScheduledJobs,
  getRefreshToken,
  refreshAccessToken,
  setLoadingCallbacks,
  setTokens,
  toggleChurchScheduledJob,
  updateChurchScheduledJob
} = createChurchApiModule()
