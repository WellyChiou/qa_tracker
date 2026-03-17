package com.example.helloworld.scheduler.church;

import com.example.helloworld.service.church.ImageCleanupService;
import com.example.helloworld.service.church.CleanupResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 圖片清理定時任務
 * 每週執行一次，清理未使用的圖片文件
 */
@Component
public class ImageCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(ImageCleanupScheduler.class);

    @Autowired
    private ImageCleanupService imageCleanupService;

    /**
     * 圖片清理任務
     */
    public static class ImageCleanupJob implements Runnable {
        private final ImageCleanupScheduler scheduler;

        public ImageCleanupJob(ImageCleanupScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            scheduler.cleanupUnusedImages();
        }
    }

    /**
     * 清理未使用的圖片
     */
    public void cleanupUnusedImages() {
        try {
            log.info("🧹 [圖片清理] 開始清理未使用的圖片...");
            CleanupResult result = imageCleanupService.cleanupUnusedImages();
            
            // 格式化結果消息
            String resultMessage = formatResult(result);
            JobResultHolder.setResult(resultMessage);
            
            log.info("✅ [圖片清理] 完成，共刪除 {} 個未使用的圖片文件", result.getCount());
        } catch (Exception e) {
            log.error("❌ [圖片清理] 執行失敗: {}", e.getMessage(), e);
            JobResultHolder.clear();
            // 重新拋出異常，確保 Job 狀態標記為 FAILED
            throw new RuntimeException("圖片清理失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 格式化結果消息
     */
    private String formatResult(CleanupResult result) {
        if (result.getCount() == 0) {
            return "未發現未使用的圖片文件";
        }
        
        StringBuilder message = new StringBuilder();
        message.append("共刪除 ").append(result.getCount()).append(" 個未使用的圖片文件：\n");
        
        // 限制顯示數量，避免消息過長（最多顯示50個）
        int displayCount = Math.min(result.getCount(), 50);
        for (int i = 0; i < displayCount; i++) {
            message.append("- ").append(result.getDeletedFiles().get(i)).append("\n");
        }
        
        if (result.getCount() > 50) {
            message.append(String.format("... 還有 %d 個圖片文件未顯示\n", result.getCount() - 50));
        }
        
        return message.toString();
    }

    /**
     * 獲取圖片清理任務實例
     */
    public ImageCleanupJob getImageCleanupJob() {
        return new ImageCleanupJob(this);
    }
}
