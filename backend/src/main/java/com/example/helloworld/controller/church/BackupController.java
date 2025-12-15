package com.example.helloworld.controller.church;

import com.example.helloworld.service.church.SystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/api/church/admin/backups")
@CrossOrigin(origins = "*")
public class BackupController {
    private static final Logger log = LoggerFactory.getLogger(BackupController.class);

    @Autowired
    private SystemSettingService systemSettingService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getBackupList() {
        try {
            // 從環境變數讀取備份目錄（在 docker-compose.yml 中設定）
            String backupDir = System.getenv("BACKUP_DIR");
            if (backupDir == null || backupDir.isEmpty()) {
                backupDir = "/app/backups"; // 預設值
            }
            Path backupPath = Paths.get(backupDir);

            if (!Files.exists(backupPath)) {
                Map<String, Object> response = new HashMap<>();
                response.put("backups", new ArrayList<>());
                response.put("message", "備份目錄不存在");
                return ResponseEntity.ok(response);
            }

            List<Map<String, Object>> backups = new ArrayList<>();
            // 使用 Files.walk() 遞迴掃描所有子資料夾
            try (Stream<Path> paths = Files.walk(backupPath)) {
                backups = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".sql.gz") || path.toString().endsWith(".sql"))
                    .map(path -> {
                        Map<String, Object> backup = new HashMap<>();
                        File file = path.toFile();
                        backup.put("filename", file.getName());
                        backup.put("size", file.length());
                        backup.put("sizeFormatted", formatFileSize(file.length()));
                        backup.put("modified", new Date(file.lastModified()));
                        
                        // 從路徑中解析資料庫名稱（從資料夾名稱）
                        Path relativePath = backupPath.relativize(path);
                        String dbName = "unknown";
                        if (relativePath.getNameCount() > 0) {
                            // 第一個路徑段是資料庫名稱（資料夾名稱）
                            String firstSegment = relativePath.getName(0).toString();
                            if (firstSegment.equals("qa_tracker") || firstSegment.equals("church")) {
                                dbName = firstSegment;
                            } else {
                                // 如果不在子資料夾中，從檔案名稱解析（向後兼容）
                                String filename = file.getName();
                                if (filename.startsWith("qa_tracker_")) {
                                    dbName = "qa_tracker";
                                } else if (filename.startsWith("church_")) {
                                    dbName = "church";
                                }
                            }
                        }
                        backup.put("database", dbName);
                        
                        // 保存相對路徑（包含資料夾路徑），用於下載和刪除
                        backup.put("relativePath", relativePath.toString().replace("\\", "/"));
                        
                        return backup;
                    })
                    // 只保留 church 的備份
                    .filter(backup -> "church".equals(backup.get("database")))
                    .sorted((a, b) -> {
                        Date dateA = (Date) a.get("modified");
                        Date dateB = (Date) b.get("modified");
                        return dateB.compareTo(dateA); // 最新的在前
                    })
                    .collect(Collectors.toList());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("backups", backups);
            response.put("message", "獲取備份列表成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "獲取備份列表失敗：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createBackup() {
        try {
            // 獲取備份配置
            String mysqlService = systemSettingService.getSettingValue("backup.mysql_service", "mysql");
            String mysqlRootPassword = systemSettingService.getSettingValue("backup.mysql_root_password", "rootpassword");
            // 從環境變數讀取備份目錄（在 docker-compose.yml 中設定）
            String backupDir = System.getenv("BACKUP_DIR");
            if (backupDir == null || backupDir.isEmpty()) {
                backupDir = "/app/backups"; // 預設值
            }
            int retentionDays = systemSettingService.getSettingValueAsInt("backup.retention_days", 7);
            String enabled = systemSettingService.getSettingValue("backup.enabled", "true");
            
            // 執行備份腳本（容器內版本），傳入 'church' 參數只備份教會資料庫
            String backupScript = "/app/backup-database.sh";
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", backupScript, "church");
            // 不重定向錯誤流，分別讀取 stdout 和 stderr
            processBuilder.redirectErrorStream(false);
            // 設置環境變數
            processBuilder.environment().put("MYSQL_HOST", mysqlService);
            processBuilder.environment().put("MYSQL_PORT", "3306");
            processBuilder.environment().put("MYSQL_ROOT_PASSWORD", mysqlRootPassword);
            processBuilder.environment().put("BACKUP_DIR", backupDir);
            processBuilder.environment().put("RETENTION_DAYS", String.valueOf(retentionDays));
            processBuilder.environment().put("BACKUP_ENABLED", enabled);
            
            Process process = processBuilder.start();
            
            // 讀取 stdout 和 stderr
            StringBuilder stdoutOutput = new StringBuilder();
            StringBuilder stderrOutput = new StringBuilder();
            
            // 使用線程同時讀取 stdout 和 stderr
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
            
            Map<String, Object> response = new HashMap<>();
            if (exitCode == 0) {
                response.put("success", true);
                response.put("message", "備份創建成功");
                if (fullOutput.length() > 0) {
                    response.put("output", fullOutput.toString());
                }
            } else {
                response.put("success", false);
                String errorMessage = "備份創建失敗，退出碼: " + exitCode;
                if (stderrOutput.length() > 0) {
                    errorMessage += "\n錯誤訊息:\n" + stderrOutput.toString();
                }
                if (stdoutOutput.length() > 0) {
                    errorMessage += "\n標準輸出:\n" + stdoutOutput.toString();
                }
                response.put("message", errorMessage);
                if (fullOutput.length() > 0) {
                    response.put("output", fullOutput.toString());
                }
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "備份創建失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteBackup(@RequestParam("path") String relativePath) {
        try {
            // 從環境變數讀取備份目錄（在 docker-compose.yml 中設定）
            String backupDir = System.getenv("BACKUP_DIR");
            if (backupDir == null || backupDir.isEmpty()) {
                backupDir = "/app/backups"; // 預設值
            }
            
            // 安全檢查：移除路徑中的 ".." 和開頭的 "/"
            relativePath = relativePath.replace("..", "").replaceAll("^/", "");
            
            // 檢查是否包含 church，防止刪除其他資料庫備份
            if (!relativePath.contains("church")) {
                 Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "只能刪除教會網站的備份");
                return ResponseEntity.badRequest().body(response);
            }

            Path backupPath = Paths.get(backupDir, relativePath);
            
            // 安全檢查：確保檔案在備份目錄內
            Path normalizedBackupPath = backupPath.normalize();
            Path normalizedBackupDir = Paths.get(backupDir).normalize();
            if (!normalizedBackupPath.startsWith(normalizedBackupDir)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "無效的檔案路徑");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (Files.exists(backupPath)) {
                Files.delete(backupPath);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "備份檔案刪除成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "備份檔案不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadBackup(@RequestParam("path") String relativePath) {
        try {
            // 從環境變數讀取備份目錄（在 docker-compose.yml 中設定）
            String backupDir = System.getenv("BACKUP_DIR");
            if (backupDir == null || backupDir.isEmpty()) {
                backupDir = "/app/backups"; // 預設值
            }
            
            // 安全檢查：移除路徑中的 ".." 和開頭的 "/"
            relativePath = relativePath.replace("..", "").replaceAll("^/", "");

            // 檢查是否包含 church
            if (!relativePath.contains("church")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Path backupPath = Paths.get(backupDir, relativePath);
            
            // 安全檢查
            Path normalizedBackupPath = backupPath.normalize();
            Path normalizedBackupDir = Paths.get(backupDir).normalize();
            if (!normalizedBackupPath.startsWith(normalizedBackupDir)) {
                return ResponseEntity.badRequest().build();
            }
            
            if (Files.exists(backupPath)) {
                byte[] fileContent = Files.readAllBytes(backupPath);
                
                // 從路徑中提取檔案名稱
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

