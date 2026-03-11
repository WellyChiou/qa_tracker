package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.JobExecution;
import com.example.helloworld.entity.church.ScheduledJob;
import com.example.helloworld.repository.church.JobExecutionRepository;
import com.example.helloworld.repository.church.ScheduledJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Service
public class ChurchScheduledJobService {
    private static final Logger log = LoggerFactory.getLogger(ChurchScheduledJobService.class);

    @Autowired
    @Qualifier("churchScheduledJobRepository")
    private ScheduledJobRepository scheduledJobRepository;

    @Autowired
    @Qualifier("churchJobExecutionRepository")
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private TaskScheduler taskScheduler;

    // 儲存正在執行的任務
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    // 儲存 Job 執行器映射
    private final Map<String, Runnable> jobExecutors = new HashMap<>();

    // 儲存正在執行的 JobExecution ID
    private final Map<Long, Long> runningExecutions = new HashMap<>();

    /**
     * 註冊 Job 執行器
     */
    public void registerJobExecutor(String jobClass, Runnable executor) {
        jobExecutors.put(jobClass, executor);
    }

    /**
     * 獲取所有 Job
     */
    public List<ScheduledJob> getAllJobs() {
        return scheduledJobRepository.findAll();
    }

    /**
     * 根據 ID 獲取 Job
     */
    public Optional<ScheduledJob> getJobById(Long id) {
        return scheduledJobRepository.findById(id);
    }

    /**
     * 創建 Job
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ScheduledJob createJob(ScheduledJob job) {
        ScheduledJob saved = scheduledJobRepository.save(job);
        if (saved.getEnabled()) {
            scheduleJob(saved);
        }
        return saved;
    }

    /**
     * 更新 Job
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ScheduledJob updateJob(Long id, ScheduledJob job) {
        Optional<ScheduledJob> existingOpt = scheduledJobRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("Job not found with id: " + id);
        }

        ScheduledJob existing = existingOpt.get();
        existing.setJobName(job.getJobName());
        existing.setJobClass(job.getJobClass());
        existing.setCronExpression(job.getCronExpression());
        existing.setDescription(job.getDescription());
        existing.setEnabled(job.getEnabled());

        ScheduledJob saved = scheduledJobRepository.save(existing);

        // 取消舊任務
        cancelJob(id);

        // 如果啟用，重新調度
        if (saved.getEnabled()) {
            scheduleJob(saved);
        }

        return saved;
    }

    /**
     * 刪除 Job
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteJob(Long id) {
        cancelJob(id);
        scheduledJobRepository.deleteById(id);
    }

    /**
     * 立即執行 Job
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public Long executeJob(Long id) {
        Optional<ScheduledJob> jobOpt = scheduledJobRepository.findById(id);
        if (jobOpt.isEmpty()) {
            throw new RuntimeException("Job not found with id: " + id);
        }

        ScheduledJob job = jobOpt.get();
        Runnable executor = jobExecutors.get(job.getJobClass());
        if (executor == null) {
            throw new RuntimeException("Job executor not found for class: " + job.getJobClass());
        }

        // 創建執行記錄
        JobExecution execution = new JobExecution();
        execution.setJobId(id);
        execution.setStatus("PENDING");
        execution.setStartedAt(LocalDateTime.now());
        execution = jobExecutionRepository.save(execution);
        final Long executionId = execution.getId();
        final String jobName = job.getJobName();

        // 在新線程中執行
        new Thread(() -> {
            try {
                // 重新載入執行記錄以確保是最新的
                // 添加重試邏輯，因為新線程可能需要等待事務提交
                JobExecution currentExecution = loadExecutionWithRetry(executionId, 5);
                
                if (currentExecution == null) {
                    // 如果仍然找不到，創建一個新的執行記錄作為 fallback
                    log.warn("⚠️ [Church] 無法載入執行記錄 ID {}，創建新的執行記錄", executionId);
                    currentExecution = new JobExecution();
                    currentExecution.setId(executionId);
                    currentExecution.setJobId(id);
                    currentExecution.setStatus("RUNNING");
                    currentExecution.setStartedAt(LocalDateTime.now());
                    currentExecution = jobExecutionRepository.save(currentExecution);
                } else {
                    // 更新狀態為執行中
                    currentExecution.setStatus("RUNNING");
                    currentExecution.setStartedAt(LocalDateTime.now());
                    currentExecution = jobExecutionRepository.save(currentExecution);
                }
                
                runningExecutions.put(id, executionId);

                log.info("🚀 [Church] 立即執行 Job: {} (Execution ID: {})", jobName, executionId);
                
                // 清除之前的結果
                com.example.helloworld.scheduler.church.JobResultHolder.clear();
                
                executor.run();

                // 重新載入執行記錄（添加重試邏輯）
                currentExecution = loadExecutionWithRetry(executionId, 5);
                
                if (currentExecution != null) {
                    // 更新狀態為成功
                    currentExecution.setStatus("SUCCESS");
                    currentExecution.setCompletedAt(LocalDateTime.now());
                    
                    // 從 ThreadLocal 獲取詳細結果
                    String detailedResult = com.example.helloworld.scheduler.church.JobResultHolder.getResult();
                    if (detailedResult != null && !detailedResult.isEmpty()) {
                        currentExecution.setResultMessage("[手動執行] " + detailedResult);
                    } else {
                        currentExecution.setResultMessage("[手動執行] Job 執行成功");
                    }
                    
                    // 清除 ThreadLocal
                    com.example.helloworld.scheduler.church.JobResultHolder.clear();
                    
                    jobExecutionRepository.save(currentExecution);
                    log.info("✅ [Church] Job 執行完成: {}", jobName);
                } else {
                    log.warn("⚠️ [Church] 無法更新執行記錄 ID {} 的狀態為成功", executionId);
                }
            } catch (Exception e) {
                // 重新載入執行記錄（添加重試邏輯）
                JobExecution currentExecution = loadExecutionWithRetry(executionId, 5);
                
                if (currentExecution != null) {
                    // 更新狀態為失敗
                    currentExecution.setStatus("FAILED");
                    currentExecution.setCompletedAt(LocalDateTime.now());
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                    currentExecution.setErrorMessage("[手動執行] " + errorMsg);
                    jobExecutionRepository.save(currentExecution);
                } else {
                    log.warn("⚠️ [Church] 無法更新執行記錄 ID {} 的狀態為失敗", executionId);
                }
                log.error("❌ [Church] Job 執行失敗: {} - {}", jobName, e.getMessage(), e);
            } finally {
                runningExecutions.remove(id);
            }
        }).start();

        return executionId;
    }

    /**
     * 獲取 Job 的執行記錄
     */
    public List<JobExecution> getJobExecutions(Long jobId) {
        return jobExecutionRepository.findByJobIdOrderByCreatedAtDesc(jobId);
    }

    /**
     * 獲取 Job 的最新執行狀態
     */
    public Optional<JobExecution> getLatestExecution(Long jobId) {
        return jobExecutionRepository.findFirstByJobIdOrderByCreatedAtDesc(jobId);
    }

    /**
     * 獲取執行記錄詳情
     */
    public Optional<JobExecution> getExecutionById(Long executionId) {
        return jobExecutionRepository.findById(executionId);
    }

    /**
     * 啟用/停用 Job
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ScheduledJob toggleJob(Long id, Boolean enabled) {
        Optional<ScheduledJob> jobOpt = scheduledJobRepository.findById(id);
        if (jobOpt.isEmpty()) {
            throw new RuntimeException("Job not found with id: " + id);
        }

        ScheduledJob job = jobOpt.get();
        job.setEnabled(enabled);
        ScheduledJob saved = scheduledJobRepository.save(job);

        if (enabled) {
            scheduleJob(saved);
        } else {
            cancelJob(id);
        }

        return saved;
    }

    /**
     * 調度 Job
     */
    private void scheduleJob(ScheduledJob job) {
        Runnable executor = jobExecutors.get(job.getJobClass());
        if (executor == null) {
            log.warn("⚠️ [Church] Job executor not found for class: {}", job.getJobClass());
            return;
        }

        try {
            // 指定時區為台灣時間 (Asia/Taipei, UTC+8)
            ZoneId taiwanZone = ZoneId.of("Asia/Taipei");
            CronTrigger trigger = new CronTrigger(job.getCronExpression(), taiwanZone);
            ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                // 創建執行記錄
                JobExecution execution = new JobExecution();
                execution.setJobId(job.getId());
                execution.setStatus("RUNNING");
                execution.setStartedAt(LocalDateTime.now());
                execution = jobExecutionRepository.save(execution);
                final Long executionId = execution.getId();
                
                try {
                    log.info("🔄 [Church] 執行定時任務: {} (Execution ID: {})", job.getJobName(), executionId);
                    
                    // 清除之前的結果
                    com.example.helloworld.scheduler.church.JobResultHolder.clear();
                    
                    executor.run();
                    
                    // 重新載入執行記錄
                    JobExecution currentExecution = jobExecutionRepository.findById(executionId).orElse(null);
                    if (currentExecution != null) {
                        currentExecution.setStatus("SUCCESS");
                        currentExecution.setCompletedAt(LocalDateTime.now());
                        
                        // 從 ThreadLocal 獲取詳細結果
                        String detailedResult = com.example.helloworld.scheduler.church.JobResultHolder.getResult();
                        if (detailedResult != null && !detailedResult.isEmpty()) {
                            currentExecution.setResultMessage("[自動排程] " + detailedResult);
                        } else {
                            currentExecution.setResultMessage("[自動排程] 定時任務執行成功");
                        }
                        currentExecution.setErrorMessage(null);
                        jobExecutionRepository.save(currentExecution);
                        log.info("✅ [Church] 定時任務執行成功: {}", job.getJobName());
                    }
                    
                    // 清除 ThreadLocal
                    com.example.helloworld.scheduler.church.JobResultHolder.clear();
                    
                } catch (Exception e) {
                    log.error("❌ [Church] 定時任務執行失敗: {} - {}", job.getJobName(), e.getMessage(), e);
                    // 更新執行記錄為失敗狀態
                    try {
                        JobExecution failedExecution = jobExecutionRepository.findById(executionId).orElse(null);
                        if (failedExecution != null) {
                            failedExecution.setStatus("FAILED");
                            failedExecution.setCompletedAt(LocalDateTime.now());
                            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                            failedExecution.setErrorMessage("[自動排程] " + errorMsg);
                            jobExecutionRepository.save(failedExecution);
                        }
                    } catch (Exception ex) {
                        log.error("❌ [Church] 更新失敗狀態時發生錯誤: {}", ex.getMessage(), ex);
                    }
                }
            }, trigger);

            scheduledTasks.put(job.getId(), future);
            log.info("✅ [Church] Job 已調度: {} (Cron: {})", job.getJobName(), job.getCronExpression());
        } catch (Exception e) {
            log.error("❌ [Church] 調度 Job 失敗: {} - {}", job.getJobName(), e.getMessage(), e);
        }
    }

    /**
     * 取消 Job
     */
    private void cancelJob(Long id) {
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) {
            future.cancel(false);
            log.info("⏹️ [Church] Job 已取消: {}", id);
        }
    }

    /**
     * 初始化所有啟用的 Job
     */
    public void initializeJobs() {
        List<ScheduledJob> enabledJobs = scheduledJobRepository.findByEnabledTrue();
        for (ScheduledJob job : enabledJobs) {
            scheduleJob(job);
        }
        log.info("✅ [Church] 已初始化 {} 個啟用的 Job", enabledJobs.size());
    }

    /**
     * 重試載入執行記錄（解決新線程中事務未提交的問題）
     */
    private JobExecution loadExecutionWithRetry(Long executionId, int maxRetries) {
        JobExecution execution = null;
        int retryCount = 0;
        while (execution == null && retryCount < maxRetries) {
            try {
                execution = jobExecutionRepository.findById(executionId).orElse(null);
                if (execution == null) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        Thread.sleep(100); // 等待 100ms 後重試
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                retryCount++;
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return execution;
    }
}

