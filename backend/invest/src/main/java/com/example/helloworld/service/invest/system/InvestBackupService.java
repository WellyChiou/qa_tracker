package com.example.helloworld.service.invest.system;

import com.example.helloworld.dto.invest.SystemMaintenanceBackupFileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class InvestBackupService {

    private static final Logger log = LoggerFactory.getLogger(InvestBackupService.class);
    private static final String DEFAULT_BACKUP_DIR = "/app/backups";
    private static final String DEFAULT_MYSQL_SERVICE = "mysql";
    private static final String DEFAULT_MYSQL_ROOT_PASSWORD = "rootpassword";
    private static final int DEFAULT_RETENTION_DAYS = 7;
    private static final String DEFAULT_DATABASE_NAME = "invest";

    private final InvestSystemSettingService investSystemSettingService;

    public InvestBackupService(InvestSystemSettingService investSystemSettingService) {
        this.investSystemSettingService = investSystemSettingService;
    }

    public List<SystemMaintenanceBackupFileDto> listBackups() {
        Path backupDir = getBackupDirPath();
        if (!Files.exists(backupDir)) {
            return List.of();
        }

        List<SystemMaintenanceBackupFileDto> backups = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(backupDir)) {
            backups = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".sql.gz") || path.toString().endsWith(".sql"))
                .map(path -> toBackupDto(backupDir, path))
                .filter(item -> item != null)
                .sorted(Comparator.comparing(SystemMaintenanceBackupFileDto::getModified, Comparator.nullsLast(Date::compareTo)).reversed())
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("讀取備份檔案清單失敗：" + e.getMessage(), e);
        }

        return backups;
    }

    public Map<String, Object> createBackup() {
        String mysqlService = investSystemSettingService.getSettingValue("backup.mysql_service", DEFAULT_MYSQL_SERVICE);
        String mysqlRootPassword = investSystemSettingService.getSettingValue("backup.mysql_root_password", DEFAULT_MYSQL_ROOT_PASSWORD);
        String backupEnabled = investSystemSettingService.getSettingValue("backup.enabled", "true");
        String retentionDays = String.valueOf(investSystemSettingService.getSettingValueAsInt("backup.retention_days", DEFAULT_RETENTION_DAYS));
        String databaseName = investSystemSettingService.getSettingValue("backup.database_name", DEFAULT_DATABASE_NAME);
        String backupDir = getBackupDir();

        String backupScript = "/app/invest-backup-database.sh";

        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", backupScript);
        processBuilder.redirectErrorStream(false);
        processBuilder.environment().put("MYSQL_HOST", mysqlService);
        processBuilder.environment().put("MYSQL_PORT", "3306");
        processBuilder.environment().put("MYSQL_ROOT_PASSWORD", mysqlRootPassword);
        processBuilder.environment().put("BACKUP_DIR", backupDir);
        processBuilder.environment().put("RETENTION_DAYS", retentionDays);
        processBuilder.environment().put("BACKUP_ENABLED", backupEnabled);
        processBuilder.environment().put("DATABASE_NAME", databaseName);

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        try {
            Process process = processBuilder.start();

            Thread stdoutReader = new Thread(() -> readOutput(process.getInputStream(), stdout, false));
            Thread stderrReader = new Thread(() -> readOutput(process.getErrorStream(), stderr, true));
            stdoutReader.start();
            stderrReader.start();

            int exitCode = process.waitFor();
            stdoutReader.join();
            stderrReader.join();

            String output = buildOutput(stdout, stderr);

            if (exitCode != 0) {
                String message = "建立備份失敗，退出碼=" + exitCode;
                if (!stderr.isEmpty()) {
                    message += "，錯誤訊息：" + stderr;
                }
                throw new RuntimeException(message);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("message", "備份建立成功");
            result.put("output", output);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("建立備份被中斷", e);
        } catch (IOException e) {
            throw new RuntimeException("執行備份腳本失敗：" + e.getMessage(), e);
        }
    }

    public byte[] readBackup(String relativePath) {
        Path filePath = resolveBackupPath(relativePath);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("備份檔案不存在");
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("讀取備份檔案失敗：" + e.getMessage(), e);
        }
    }

    public String resolveDownloadFilename(String relativePath) {
        return resolveBackupPath(relativePath).getFileName().toString();
    }

    public void deleteBackup(String relativePath) {
        Path filePath = resolveBackupPath(relativePath);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("備份檔案不存在");
        }
        try {
            Files.delete(filePath);
        } catch (IOException e) {
            throw new RuntimeException("刪除備份檔案失敗：" + e.getMessage(), e);
        }
    }

    private Path resolveBackupPath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new RuntimeException("path 參數不可為空");
        }

        String sanitized = relativePath.trim().replace("..", "").replaceAll("^/+", "");
        if (!(sanitized.startsWith("invest/") || sanitized.startsWith("invest_"))) {
            throw new RuntimeException("僅允許操作 invest 備份檔案");
        }

        Path backupDir = getBackupDirPath().normalize();
        Path filePath = backupDir.resolve(sanitized).normalize();
        if (!filePath.startsWith(backupDir)) {
            throw new RuntimeException("無效的備份檔案路徑");
        }

        return filePath;
    }

    private SystemMaintenanceBackupFileDto toBackupDto(Path backupDir, Path filePath) {
        File file = filePath.toFile();
        Path relativePath = backupDir.relativize(filePath);
        String normalizedPath = relativePath.toString().replace("\\", "/");

        String database = resolveDatabaseName(relativePath, file.getName());
        if (!"invest".equals(database)) {
            return null;
        }

        SystemMaintenanceBackupFileDto dto = new SystemMaintenanceBackupFileDto();
        dto.setFilename(file.getName());
        dto.setSize(file.length());
        dto.setSizeFormatted(formatFileSize(file.length()));
        dto.setModified(new Date(file.lastModified()));
        dto.setDatabase(database);
        dto.setRelativePath(normalizedPath);
        return dto;
    }

    private String resolveDatabaseName(Path relativePath, String fileName) {
        if (relativePath.getNameCount() > 0) {
            String firstSegment = relativePath.getName(0).toString();
            if ("invest".equals(firstSegment)) {
                return "invest";
            }
        }

        if (fileName.startsWith("invest_")) {
            return "invest";
        }

        return "unknown";
    }

    private String getBackupDir() {
        String envBackupDir = System.getenv("BACKUP_DIR");
        if (envBackupDir != null && !envBackupDir.trim().isEmpty()) {
            return envBackupDir;
        }
        return DEFAULT_BACKUP_DIR;
    }

    private Path getBackupDirPath() {
        return Paths.get(getBackupDir());
    }

    private void readOutput(java.io.InputStream inputStream, StringBuilder target, boolean isError) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                target.append(line).append('\n');
                if (isError) {
                    log.error("[invest-backup] {}", line);
                } else {
                    log.info("[invest-backup] {}", line);
                }
            }
        } catch (IOException e) {
            log.error("讀取備份腳本輸出失敗: {}", e.getMessage());
        }
    }

    private String buildOutput(StringBuilder stdout, StringBuilder stderr) {
        StringBuilder combined = new StringBuilder();
        if (!stdout.isEmpty()) {
            combined.append("標準輸出:\n").append(stdout);
        }
        if (!stderr.isEmpty()) {
            combined.append("錯誤輸出:\n").append(stderr);
        }
        return combined.toString();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
