package com.example.helloworld.service.invest;

import com.example.helloworld.entity.invest.SchedulerJobLog;
import com.example.helloworld.entity.invest.SchedulerJobStatusCode;
import com.example.helloworld.repository.invest.SchedulerJobLogRepository;
import com.example.helloworld.service.invest.system.InvestBackupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class DatabaseBackupExecutionService {

    public static final String JOB_NAME_DATABASE_BACKUP = "DATABASE_BACKUP";
    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");
    private static final int MAX_LOG_MESSAGE_LENGTH = 1000;
    private static final Pattern BACKUP_FILE_PATTERN = Pattern.compile("(/app/backups/[^\\s]+\\.sql\\.gz)");

    private final InvestBackupService investBackupService;
    private final SchedulerJobLogRepository schedulerJobLogRepository;

    public DatabaseBackupExecutionService(InvestBackupService investBackupService,
                                          SchedulerJobLogRepository schedulerJobLogRepository) {
        this.investBackupService = investBackupService;
        this.schedulerJobLogRepository = schedulerJobLogRepository;
    }

    public ExecutionResult runWithLog(String triggerDescription) {
        LocalDateTime now = LocalDateTime.now(TAIPEI_ZONE);

        SchedulerJobLog log = new SchedulerJobLog();
        log.setJobName(JOB_NAME_DATABASE_BACKUP);
        log.setRunDate(LocalDate.now(TAIPEI_ZONE));
        log.setStatus(SchedulerJobStatusCode.RUNNING);
        log.setStartedAt(now);
        log.setMessage(limitMessage("資料庫備份執行中：" + triggerDescription));
        log = schedulerJobLogRepository.save(log);

        try {
            Map<String, Object> backupResult = investBackupService.createBackup();
            String summary = buildSuccessSummary(triggerDescription, backupResult);

            log.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
            log.setStatus(SchedulerJobStatusCode.SUCCESS);
            log.setMessage(limitMessage(summary));
            schedulerJobLogRepository.save(log);

            return new ExecutionResult(log.getId(), SchedulerJobStatusCode.SUCCESS.name(), summary);
        } catch (Exception e) {
            String errorSummary = "資料庫備份失敗（" + triggerDescription + "）：" + safeText(e.getMessage());
            log.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
            log.setStatus(SchedulerJobStatusCode.FAILED);
            log.setMessage(limitMessage(errorSummary));
            schedulerJobLogRepository.save(log);
            return new ExecutionResult(log.getId(), SchedulerJobStatusCode.FAILED.name(), errorSummary);
        }
    }

    private String buildSuccessSummary(String triggerDescription, Map<String, Object> backupResult) {
        String message = safeText(backupResult == null ? null : (String) backupResult.get("message"));
        String output = safeText(backupResult == null ? null : (String) backupResult.get("output"));
        String backupFile = parseBackupFilePath(output);

        StringBuilder summary = new StringBuilder("資料庫備份完成（");
        summary.append(triggerDescription).append("）");
        if (!message.isEmpty()) {
            summary.append("。").append(message);
        }
        if (!backupFile.isEmpty()) {
            summary.append("，檔案：").append(backupFile);
        }
        return summary.toString();
    }

    private String parseBackupFilePath(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        Matcher matcher = BACKUP_FILE_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String limitMessage(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() <= MAX_LOG_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_LOG_MESSAGE_LENGTH - 3) + "...";
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    public record ExecutionResult(Long logId, String status, String message) {
    }
}
