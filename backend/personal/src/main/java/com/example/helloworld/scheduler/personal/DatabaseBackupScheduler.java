package com.example.helloworld.scheduler.personal;

import com.example.helloworld.service.personal.SystemSettingService;
import com.example.helloworld.scheduler.personal.JobResultHolder;
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
            
            String backupDir = System.getenv("BACKUP_DIR");
            if (backupDir == null || backupDir.isEmpty()) {
                backupDir = "/app/backups"; // 預設值
            }
            int retentionDays = systemSettingService.getSettingValueAsInt("backup.retention_days", 7);
            
            // Remote: Use personal-backup-database.sh
            String backupScript = "/app/personal-backup-database.sh";
            
            // 檢查腳本是否存在
            java.io.File scriptFile = new java.io.File(backupScript);
            if (!scriptFile.exists()) {
                String message = "備份腳本不存在: " + backupScript;
                log.error("❌ [Personal 資料庫備份] {}", message);
                JobResultHolder.setResult(message);
                
                // Local Fallback: 如果腳本不存在，嘗試使用之前的 Java 直接呼叫 mysqldump 方式作為後備方案
                executeFallbackBackup(mysqlService, mysqlRootPassword, backupDir, retentionDays);
                return;
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", backupScript);
            // 不重定向錯誤流，分別讀取 stdout 和 stderr
            processBuilder.redirectErrorStream(false);
            // 設置環境變數
            processBuilder.environment().put("MYSQL_HOST", mysqlService); 
            processBuilder.environment().put("MYSQL_PORT", "3306");
            processBuilder.environment().put("MYSQL_ROOT_PASSWORD", mysqlRootPassword);
            processBuilder.environment().put("BACKUP_DIR", backupDir);
            processBuilder.environment().put("RETENTION_DAYS", String.valueOf(retentionDays));
            processBuilder.environment().put("BACKUP_ENABLED", enabled);
            processBuilder.environment().put("DATABASE_NAME", "qa_tracker"); // Remote addition
            
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
                log.info("✅ [Personal 資料庫備份] 完成");
            } else {
                String message = "備份執行失敗，退出碼: " + exitCode + "\n" + fullOutput.toString();
                JobResultHolder.setResult(message);
                log.error("❌ [Personal 資料庫備份] 失敗: {}", message);
                // 拋出異常以標記任務失敗
                throw new RuntimeException("備份執行失敗，退出碼: " + exitCode);
            }
            
        } catch (Exception e) {
            String errorMsg = "備份執行失敗: " + e.getMessage();
            log.error("❌ [Personal 資料庫備份] 執行失敗: {}", e.getMessage(), e);
            JobResultHolder.setResult(errorMsg);
            // 重新拋出異常，確保 Job 狀態標記為 FAILED
            throw new RuntimeException("備份執行失敗: " + e.getMessage(), e);
        }
    }
    
    /**
     * 後備備份方案（直接使用 mysqldump）
     */
    private void executeFallbackBackup(String mysqlService, String mysqlRootPassword, String backupDir, int retentionDays) {
        try {
            log.info("⚠️ [Personal 資料庫備份] 腳本不存在，使用 Java 直接執行備份...");
            
            String dbName = "qa_tracker";
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String targetDir = backupDir + "/" + dbName;
            String backupFile = targetDir + "/" + dbName + "_" + timestamp + ".sql";
            
            // 確保目錄存在
            java.io.File dir = new java.io.File(targetDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 構建並執行命令
            String command = String.format(
                "mysqldump -h %s -P 3306 -u root -p%s --ssl-mode=DISABLED --single-transaction --routines --triggers --events %s > %s && gzip %s",
                mysqlService, mysqlRootPassword, dbName, backupFile, backupFile
            );
            
            ProcessBuilder shellBuilder = new ProcessBuilder("/bin/sh", "-c", command);
            Process process = shellBuilder.start();
            process.waitFor();
            
            if (process.exitValue() == 0) {
                String message = "備份執行成功 (Java Fallback): " + backupFile + ".gz";
                JobResultHolder.setResult(message);
                log.info("✅ [Personal 資料庫備份] 完成");
                cleanupOldBackups(targetDir, retentionDays);
            } else {
                JobResultHolder.setResult("備份失敗 (Java Fallback)");
                throw new RuntimeException("備份失敗 (Java Fallback)");
            }
        } catch (Exception e) {
            log.error("❌ [Personal 資料庫備份] Fallback 失敗", e);
            throw new RuntimeException("備份失敗 (Java Fallback)", e);
        }
    }
    
    /**
     * 清理舊備份
     */
    private void cleanupOldBackups(String dirPath, int retentionDays) {
        try {
            String command = String.format("find %s -name \"*.sql.gz\" -type f -mtime +%d -delete", dirPath, retentionDays);
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", command);
            processBuilder.start().waitFor();
            log.info("🧹 [Personal 資料庫備份] 已清理 {} 天前的舊備份", retentionDays);
        } catch (Exception e) {
            log.warn("⚠️ [Personal 資料庫備份] 清理舊備份失敗: {}", e.getMessage());
        }
    }

    /**
     * 獲取備份任務實例
     */
    public DatabaseBackupJob getDatabaseBackupJob() {
        return new DatabaseBackupJob(this);
    }
}
