package com.example.helloworld.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final String UPLOAD_DIR = "./uploads/public";

    @Autowired
    @Qualifier("personalConfigurationRefreshService")
    private com.example.helloworld.service.personal.ConfigurationRefreshService personalConfigurationRefreshService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

        // 從資料庫讀取最新的 Max File Size
        long MAX_FILE_SIZE = personalConfigurationRefreshService.getConfigValueAsInt("public.max-file-size", 10);

        // 檢查檔案大小（MB 限制）
        if (file.getSize() > (MAX_FILE_SIZE * 1024 * 1024)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", String.format("檔案大小超過限制，最大允許 %d MB", MAX_FILE_SIZE));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        // 建立上傳目錄（如果不存在）
        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);

        // 產生檔名：時間戳記 + 原檔名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "file";
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = timestamp + "_" + originalFilename;

        // 儲存檔案
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // 回傳 JSON
        Map<String, String> response = new HashMap<>();
        response.put("url", "/uploads/public/" + filename);
        return ResponseEntity.ok(response);
    }
}
