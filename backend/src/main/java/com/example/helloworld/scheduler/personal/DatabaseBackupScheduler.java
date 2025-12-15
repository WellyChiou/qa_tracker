package com.example.helloworld.scheduler.personal;

import com.example.helloworld.service.personal.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Personal 系統資料庫備份定時任務
 * 自動備份 qa_tracker 資料庫
 */
@Component("personalDatabaseBackupScheduler")
public class DatabaseBackupScheduler {
    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupScheduler.class);

    @Autowired
    @Qualifier("personalSystemSettingService")
    private SystemSettingService systemSettingService;

    /**
     * 資料庫備份任務
     */
    public static class DatabaseBackupJob implements Runnable {
        private final DatabaseBackupScheduler scheduler;

        public DatabaseBackupJob(DatabaseBackupScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            scheduler.executeBackup();
        }
    }

    /**
     * 執行資料庫備份
     */
    public void executeBackup() {
        try {
            log.info("💾 [Personal 資料庫備份] 開始執行備份...");
            
            // 檢查備份是否啟用
            String enabled = systemSettingService.getSettingValue("backup.enabled", "true");
            if (!Boolean.parseBoolean(enabled)) {
                String message = "備份功能已停用，跳過備份";
                log.warn("⚠️ [Personal 資料庫備份] {}", message);
                JobResultHolder.setResult(message);
                return;
            }
            
            // 獲取備份配置
            String mysqlService = systemSettingService.getSettingValue("backup.mysql_service", "mysql");
            String mysqlRootPassword = systemSettingService.getSettingValue("backup.mysql_root_password", "rootpassword");
            // 從環境變數讀取備份目錄（在 docker-compose.yml 中設定）
            String backupDir = System.getenv("BACKUP_DIR");
            if (backupDir == null || backupDir.isEmpty()) {
                backupDir = "/app/backups"; // 預設值
            }
            int retentionDays = systemSettingService.getSettingValueAsInt("backup.retention_days", 7);
            
            // 執行備份腳本（容器內版本）
            // 備份腳本已複製到容器內的 /app/personal-backup-database.sh
            String backupScript = "/app/personal-backup-database.sh";
            
            // 檢查腳本是否存在
            java.io.File scriptFile = new java.io.File(backupScript);
            if (!scriptFile.exists()) {
                String message = "備份腳本不存在: " + backupScript + "\n" +
                    "請確保備份腳本已正確複製到容器內";
                log.error("❌ [Personal 資料庫備份] {}", message);
                JobResultHolder.setResult(message);
                return;
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", backupScript);
            // 不重定向錯誤流，分別讀取 stdout 和 stderr
            processBuilder.redirectErrorStream(false);
            // 設置環境變數（容器內版本使用這些環境變數）
            processBuilder.environment().put("MYSQL_HOST", mysqlService); // 使用服務名作為主機名
            processBuilder.environment().put("MYSQL_PORT", "3306");
            processBuilder.environment().put("MYSQL_ROOT_PASSWORD", mysqlRootPassword);
            processBuilder.environment().put("BACKUP_DIR", backupDir);
            processBuilder.environment().put("RETENTION_DAYS", String.valueOf(retentionDays));
            processBuilder.environment().put("BACKUP_ENABLED", enabled);
            processBuilder.environment().put("DATABASE_NAME", "qa_tracker");
            
            Process process = processBuilder.start();
            
            // 讀取 stdout 和 stderr
            StringBuilder stdoutOutput = new StringBuilder();
            StringBuilder stderrOutput = new StringBuilder();
            
            // 使用線程同時讀取 stdout 和 stderr
            Thread stdoutReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdoutOutput.append(line).append("\n");
                        log.info("[Personal 備份腳本 stdout] {}", line);
                    }
                } catch (Exception e) {
                    log.error("讀取 stdout 失敗: {}", e.getMessage(), e);
                }
            });
            
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrOutput.append(line).append("\n");
                        log.warn("[Personal 備份腳本 stderr] {}", line);
                    }
                } catch (Exception e) {
                    log.error("讀取 stderr 失敗: {}", e.getMessage(), e);
                }
            });
            
            stdoutReader.start();
            stderrReader.start();
            
            int exitCode = process.waitFor();
            
            // 等待讀取線程完成
            stdoutReader.join();
            stderrReader.join();
            
            // 組合輸出
            StringBuilder fullOutput = new StringBuilder();
            if (stdoutOutput.length() > 0) {
                fullOutput.append("標準輸出:\n").append(stdoutOutput.toString());
            }
            if (stderrOutput.length() > 0) {
                fullOutput.append("錯誤輸出:\n").append(stderrOutput.toString());
            }
            
            if (exitCode == 0) {
                String message = "備份執行成功\n" + fullOutput.toString();
                JobResultHolder.setResult(message);
                log.info("✅ [Personal 資料庫備份] 完成，備份目錄: {}", backupDir);
            } else {
                String message = "備份執行失敗，退出碼: " + exitCode + "\n" + fullOutput.toString();
                JobResultHolder.setResult(message);
                log.error("❌ [Personal 資料庫備份] 備份失敗，退出碼: {}", exitCode);
                if (stderrOutput.length() > 0) {
                    log.error("錯誤訊息:\n{}", stderrOutput.toString());
                }
                if (stdoutOutput.length() > 0) {
                    log.error("標準輸出:\n{}", stdoutOutput.toString());
                }
            }
        } catch (Exception e) {
            String errorMsg = "備份執行失敗: " + e.getMessage();
            log.error("❌ [Personal 資料庫備份] 執行失敗: {}", e.getMessage(), e);
            JobResultHolder.setResult(errorMsg);
            // 不拋出異常，只記錄錯誤
        }
    }

    /**
     * 獲取備份任務實例
     */
    public DatabaseBackupJob getDatabaseBackupJob() {
        return new DatabaseBackupJob(this);
    }
}

