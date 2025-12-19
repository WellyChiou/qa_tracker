package com.example.helloworld.controller;

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
    
    private static final String UPLOAD_DIR = "./uploads/public";
    
    @PostMapping("/upload")
    public Map<String, String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
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
        return response;
    }
}
