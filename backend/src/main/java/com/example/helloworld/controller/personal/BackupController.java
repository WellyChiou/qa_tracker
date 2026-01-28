package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.service.personal.SystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController("personalBackupController")
@RequestMapping("/api/personal/backups")
@CrossOrigin(origins = "*")
public class BackupController {
    private static final Logger log = LoggerFactory.getLogger(BackupController.class);

    @Autowired
    @Qualifier("personalSystemSettingService")
    private SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBackupList() {
        try {
            String backupDir = System.getenv("BACKUP_DIR");
            if (backupDir == null || backupDir.isEmpty()) {
                backupDir = "/app/backups";
            }
            Path backupPath = Paths.get(backupDir);

            if (!Files.exists(backupPath)) {
                Map<String, Object> data = new HashMap<>();
                data.put("backups", new ArrayList<>());
                data.put("message", "備份目錄不存在");
                return ResponseEntity.ok(ApiResponse.ok(data));
            }

            List<Map<String, Object>> backups;
            try (Stream<Path> paths = Files.walk(backupPath)) {
                backups = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".sql.gz") || path.toString().endsWith(".sql"))
                    .filter(path -> {
                        // Remote logic: Filter qa_tracker
                        Path relativePath = backupPath.relativize(path);
                        if (relativePath.getNameCount() > 0) {
                            String firstSegment = relativePath.getName(0).toString();
                            return firstSegment.equals("qa_tracker");
                        }
                        String filename = path.getFileName().toString();
                        return filename.startsWith("qa_tracker_");
                    })
                    .map(path -> {
                        Map<String, Object> backup = new HashMap<>();
                        File file = path.toFile();
                        backup.put("filename", file.getName());
                        backup.put("size", file.length());
                        backup.put("sizeFormatted", formatFileSize(file.length()));
                        backup.put("modified", new Date(file.lastModified()));
                        
                        Path relativePath = backupPath.relativize(path);
                        String dbName = "qa_tracker"; // Since we filtered, it is qa_tracker
                        
                        backup.put("database", dbName);
                        backup.put("relativePath", relativePath.toString().replace("\\", "/"));
                        return backup;
                    })
                    .sorted((a, b) -> {
                        Date dateA = (Date) a.get("modified");
                        Date dateB = (Date) b.get("modified");
                        return dateB.compareTo(dateA);
                    })
                    .collect(Collectors.toList());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("backups", backups);
            data.put("message", "獲取備份列表成功");
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取備份列表失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBackup() {
        try {
            String mysqlService = systemSettingService.getSettingValue("backup.mysql_service", "mysql");
            String mysqlRootPassword = systemSettingService.getSettingValue("backup.mysql_root_password", "rootpassword");
            String backupDir = System.getenv("BACKUP_DIR");
            if (backupDir == null || backupDir.isEmpty()) {
                backupDir = "/app/backups";
            }
            int retentionDays = systemSettingService.getSettingValueAsInt("backup.retention_days", 7);
            String enabled = systemSettingService.getSettingValue("backup.enabled", "true");
            
            // Remote: Use personal-backup-database.sh
            String backupScript = "/app/personal-backup-database.sh";
            
            // Remote logic for ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", backupScript);
            processBuilder.redirectErrorStream(false);
            
            processBuilder.environment().put("MYSQL_HOST", mysqlService);
            processBuilder.environment().put("MYSQL_PORT", "3306");
            processBuilder.environment().put("MYSQL_ROOT_PASSWORD", mysqlRootPassword);
            processBuilder.environment().put("BACKUP_DIR", backupDir);
            processBuilder.environment().put("RETENTION_DAYS", String.valueOf(retentionDays));
            processBuilder.environment().put("BACKUP_ENABLED", enabled);
            processBuilder.environment().put("DATABASE_NAME", "qa_tracker");
            
            Process process = processBuilder.start();
            
            StringBuilder stdoutOutput = new StringBuilder();
            StringBuilder stderrOutput = new StringBuilder();
            
            Thread stdoutReader = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdoutOutput.append(line).append("\n");
                        log.info("[備份腳本] {}", line);
                    }
                } catch (Exception e) {
                    log.error("❌ 讀取備份腳本輸出失敗", e);
                }
            });
            
            Thread stderrReader = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrOutput.append(line).append("\n");
                        log.error("[備份腳本錯誤] {}", line);
                    }
                } catch (Exception e) {
                    log.error("❌ 讀取備份腳本錯誤輸出失敗", e);
                }
            });
            
            stdoutReader.start();
            stderrReader.start();
            
            int exitCode = process.waitFor();
            
            stdoutReader.join();
            stderrReader.join();
            
            StringBuilder fullOutput = new StringBuilder();
            if (stdoutOutput.length() > 0) {
                fullOutput.append("標準輸出:\n").append(stdoutOutput.toString());
            }
            if (stderrOutput.length() > 0) {
                fullOutput.append("錯誤輸出:\n").append(stderrOutput.toString());
            }
            
            Map<String, Object> data = new HashMap<>();
            if (exitCode == 0) {
                data.put("success", true);
                data.put("message", "備份創建成功");
                if (fullOutput.length() > 0) data.put("output", fullOutput.toString());
                return ResponseEntity.ok(ApiResponse.ok(data));
            }
            String errorMessage = "備份創建失敗，退出碼: " + exitCode;
            if (stderrOutput.length() > 0) errorMessage += "\n錯誤訊息:\n" + stderrOutput.toString();
            if (stdoutOutput.length() > 0) errorMessage += "\n標準輸出:\n" + stdoutOutput.toString();
            return ResponseEntity.badRequest().body(ApiResponse.fail(errorMessage));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.fail("備份創建失敗: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteBackup(@RequestParam("path") String relativePath) {
        try {
            String backupDir = System.getenv("BACKUP_DIR");
            if (backupDir == null || backupDir.isEmpty()) {
                backupDir = "/app/backups";
            }
            relativePath = relativePath.replace("..", "").replaceAll("^/", "");
            Path backupPath = Paths.get(backupDir, relativePath);
            Path normalizedBackupPath = backupPath.normalize();
            Path normalizedBackupDir = Paths.get(backupDir).normalize();
            if (!normalizedBackupPath.startsWith(normalizedBackupDir)) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("無效的檔案路徑"));
            }
            if (!relativePath.startsWith("qa_tracker/") && !relativePath.startsWith("qa_tracker_")) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("只能刪除 Personal 系統的備份檔案"));
            }
            if (Files.exists(backupPath)) {
                Files.delete(backupPath);
                return ResponseEntity.ok(ApiResponse.ok(null));
            }
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("備份檔案不存在"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除失敗: " + e.getMessage()));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadBackup(@RequestParam("path") String relativePath) {
        try {
            String backupDir = System.getenv("BACKUP_DIR");
            if (backupDir == null || backupDir.isEmpty()) {
                backupDir = "/app/backups";
            }
            
            relativePath = relativePath.replace("..", "").replaceAll("^/", "");
            Path backupPath = Paths.get(backupDir, relativePath);
            
            Path normalizedBackupPath = backupPath.normalize();
            Path normalizedBackupDir = Paths.get(backupDir).normalize();
            if (!normalizedBackupPath.startsWith(normalizedBackupDir)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Remote: Safety check
             if (!relativePath.startsWith("qa_tracker/") && !relativePath.startsWith("qa_tracker_")) {
                return ResponseEntity.badRequest().build();
            }
            
            if (Files.exists(backupPath)) {
                byte[] fileContent = Files.readAllBytes(backupPath);
                String filename = backupPath.getFileName().toString();
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", filename);
                headers.setContentLength(fileContent.length);
                
                return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
