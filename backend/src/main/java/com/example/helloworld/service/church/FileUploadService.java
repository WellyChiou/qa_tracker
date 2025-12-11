package com.example.helloworld.service.church;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadService {
    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    @Value("${church.upload.path:/app/uploads/church}")
    private String uploadPath;

    @Value("${church.upload.url-prefix:/uploads/church}")
    private String urlPrefix;

    /**
     * 上傳圖片文件
     * 
     * @param file 上傳的文件
     * @param type 文件類型：activities 或 sunday-messages
     * @return 文件的訪問 URL
     * @throws IOException 文件操作異常
     * @throws IllegalArgumentException 文件類型或格式不正確
     */
    public String uploadImage(MultipartFile file, String type) throws IOException {
        // 驗證文件類型
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能為空");
        }

        // 驗證文件類型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只能上傳圖片文件");
        }

        // 驗證文件大小（5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超過 5MB");
        }

        // 驗證 type 參數
        if (!type.equals("activities") && !type.equals("sunday-messages")) {
            throw new IllegalArgumentException("無效的文件類型，只能是 activities 或 sunday-messages");
        }

        // 獲取文件擴展名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 生成唯一文件名
        String filename = UUID.randomUUID().toString() + extension;

        // 創建目標目錄
        Path targetDir = Paths.get(uploadPath, type);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // 保存文件
        Path targetPath = targetDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath);

        // 返回訪問 URL
        return urlPrefix + "/" + type + "/" + filename;
    }

    /**
     * 刪除圖片文件
     * 
     * @param url 圖片的 URL
     * @return 是否刪除成功
     */
    public boolean deleteImage(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // 如果 URL 不是以 urlPrefix 開頭，可能是外部 URL，不刪除
        if (!url.startsWith(urlPrefix)) {
            return false;
        }

        try {
            // 從 URL 中提取相對路徑
            String relativePath = url.substring(urlPrefix.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            Path filePath = Paths.get(uploadPath, relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return true;
            }
        } catch (IOException e) {
            log.error("❌ 刪除文件失敗: {}", e.getMessage());
        }

        return false;
    }
}
