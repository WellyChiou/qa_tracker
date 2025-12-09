package com.example.helloworld.service.personal;

import com.example.helloworld.entity.personal.JobExecution;
import com.example.helloworld.entity.personal.ScheduledJob;
import com.example.helloworld.repository.personal.JobExecutionRepository;
import com.example.helloworld.repository.personal.ScheduledJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Service
public class ScheduledJobService {

    @Autowired
    @Qualifier("personalScheduledJobRepository")
    private ScheduledJobRepository scheduledJobRepository;

    @Autowired
    @Qualifier("personalJobExecutionRepository")
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
    public void deleteJob(Long id) {
        cancelJob(id);
        scheduledJobRepository.deleteById(id);
    }

    /**
     * 立即執行 Job
     */
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
                JobExecution currentExecution = jobExecutionRepository.findById(executionId)
                    .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));
                
                // 更新狀態為執行中
                currentExecution.setStatus("RUNNING");
                currentExecution.setStartedAt(LocalDateTime.now());
                jobExecutionRepository.save(currentExecution);
                runningExecutions.put(id, executionId);

                System.out.println("🚀 立即執行 Job: " + jobName + " (Execution ID: " + executionId + ")");
                executor.run();

                // 重新載入執行記錄
                currentExecution = jobExecutionRepository.findById(executionId)
                    .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));
                
                // 更新狀態為成功
                currentExecution.setStatus("SUCCESS");
                currentExecution.setCompletedAt(LocalDateTime.now());
                currentExecution.setResultMessage("Job 執行成功");
                jobExecutionRepository.save(currentExecution);
                System.out.println("✅ Job 執行完成: " + jobName);
            } catch (Exception e) {
                // 重新載入執行記錄
                JobExecution currentExecution = jobExecutionRepository.findById(executionId).orElse(null);
                if (currentExecution != null) {
                    // 更新狀態為失敗
                    currentExecution.setStatus("FAILED");
                    currentExecution.setCompletedAt(LocalDateTime.now());
                    currentExecution.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
                    jobExecutionRepository.save(currentExecution);
                }
                System.err.println("❌ Job 執行失敗: " + jobName + " - " + e.getMessage());
                e.printStackTrace();
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
            System.err.println("⚠️ Job executor not found for class: " + job.getJobClass());
            return;
        }

        try {
            // 指定時區為台灣時間 (Asia/Taipei, UTC+8)
            ZoneId taiwanZone = ZoneId.of("Asia/Taipei");
            CronTrigger trigger = new CronTrigger(job.getCronExpression(), taiwanZone);
            ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                try {
                    System.out.println("🔄 執行定時任務: " + job.getJobName());
                    executor.run();
                    System.out.println("✅ 定時任務完成: " + job.getJobName());
                } catch (Exception e) {
                    System.err.println("❌ 定時任務執行失敗: " + job.getJobName() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }, trigger);

            scheduledTasks.put(job.getId(), future);
            System.out.println("✅ Job 已調度: " + job.getJobName() + " (Cron: " + job.getCronExpression() + ")");
        } catch (Exception e) {
            System.err.println("❌ 調度 Job 失敗: " + job.getJobName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 取消 Job
     */
    private void cancelJob(Long id) {
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) {
            future.cancel(false);
            System.out.println("⏹️ Job 已取消: " + id);
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
        System.out.println("✅ 已初始化 " + enabledJobs.size() + " 個啟用的 Job");
    }
}
