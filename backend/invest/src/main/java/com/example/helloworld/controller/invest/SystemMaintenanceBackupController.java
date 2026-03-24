package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.invest.SystemMaintenanceBackupFileDto;
import com.example.helloworld.service.invest.system.InvestBackupService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invest/system/maintenance/backups")
@CrossOrigin(origins = "*")
public class SystemMaintenanceBackupController {

    private final InvestBackupService investBackupService;

    public SystemMaintenanceBackupController(InvestBackupService investBackupService) {
        this.investBackupService = investBackupService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBackupList() {
        try {
            List<SystemMaintenanceBackupFileDto> backups = investBackupService.listBackups();
            Map<String, Object> result = new HashMap<>();
            result.put("backups", backups);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("取得 invest 備份清單失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBackup() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investBackupService.createBackup()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("建立 invest 備份失敗：" + e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteBackup(@RequestParam("path") String relativePath) {
        try {
            investBackupService.deleteBackup(relativePath);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除 invest 備份失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadBackup(@RequestParam("path") String relativePath) {
        try {
            byte[] content = investBackupService.readBackup(relativePath);
            String filename = investBackupService.resolveDownloadFilename(relativePath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(content.length);
            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
