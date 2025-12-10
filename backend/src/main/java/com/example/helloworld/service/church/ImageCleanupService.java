package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.Activity;
import com.example.helloworld.entity.church.SundayMessage;
import com.example.helloworld.repository.church.ActivityRepository;
import com.example.helloworld.repository.church.SundayMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
public class ImageCleanupService {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private SundayMessageRepository sundayMessageRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @Value("${church.upload.path:/app/uploads/church}")
    private String uploadPath;

    @Value("${church.upload.url-prefix:/uploads/church}")
    private String urlPrefix;

    /**
     * 清理未使用的圖片文件
     * 掃描上傳目錄中的所有圖片，與數據庫中使用的圖片 URL 對比，刪除未使用的圖片
     * 
     * @return 清理結果，包含刪除的圖片列表
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public CleanupResult cleanupUnusedImages() {
        AtomicInteger deletedCount = new AtomicInteger(0);
        List<String> deletedFiles = new ArrayList<>();

        try {
            // 1. 獲取所有活動和主日信息使用的圖片 URL
            Set<String> usedImageUrls = new HashSet<>();
            
            activityRepository.findAll().forEach(activity -> {
                if (activity.getImageUrl() != null && !activity.getImageUrl().isEmpty()) {
                    usedImageUrls.add(activity.getImageUrl());
                }
            });
            
            sundayMessageRepository.findAll().forEach(message -> {
                if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    usedImageUrls.add(message.getImageUrl());
                }
            });

            // 2. 掃描上傳目錄
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                return new CleanupResult(0, deletedFiles);
            }

            // 掃描 activities 和 sunday-messages 目錄
            String[] subDirs = {"activities", "sunday-messages"};
            
            for (String subDir : subDirs) {
                Path subDirPath = uploadDir.resolve(subDir);
                if (!Files.exists(subDirPath)) {
                    continue;
                }

                try (Stream<Path> paths = Files.list(subDirPath)) {
                    paths.filter(Files::isRegularFile)
                         .forEach(filePath -> {
                             try {
                                 // 構建 URL，格式與 FileUploadService 一致：urlPrefix + "/" + type + "/" + filename
                                 String filename = filePath.getFileName().toString();
                                 String imageUrl = urlPrefix + "/" + subDir + "/" + filename;

                                 // 如果圖片不在使用列表中，刪除它
                                 if (!usedImageUrls.contains(imageUrl)) {
                                     try {
                                         Files.delete(filePath);
                                         deletedCount.incrementAndGet();
                                         deletedFiles.add(imageUrl);
                                         System.out.println("刪除未使用的圖片: " + filePath);
                                     } catch (IOException e) {
                                         System.err.println("刪除圖片失敗: " + filePath + " - " + e.getMessage());
                                     }
                                 }
                             } catch (Exception e) {
                                 System.err.println("處理圖片時發生錯誤: " + filePath + " - " + e.getMessage());
                             }
                         });
                } catch (IOException e) {
                    System.err.println("掃描目錄失敗: " + subDirPath + " - " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("清理未使用圖片時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }

        return new CleanupResult(deletedCount.get(), deletedFiles);
    }
}

