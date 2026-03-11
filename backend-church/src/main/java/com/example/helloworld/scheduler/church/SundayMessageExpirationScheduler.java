package com.example.helloworld.scheduler.church;

import com.example.helloworld.service.church.SundayMessageService;
import com.example.helloworld.service.church.DeactivationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 主日信息過期檢查排程器
 * 每週一晚上 01:00 檢查並停用過期的主日信息
 */
@Component
public class SundayMessageExpirationScheduler {
    private static final Logger log = LoggerFactory.getLogger(SundayMessageExpirationScheduler.class);

    @Autowired
    private SundayMessageService sundayMessageService;

    /**
     * 主日信息過期檢查任務
     */
    public static class SundayMessageExpirationJob implements Runnable {
        private final SundayMessageExpirationScheduler scheduler;

        public SundayMessageExpirationJob(SundayMessageExpirationScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            scheduler.checkAndDeactivateExpiredMessages();
        }
    }

    /**
     * 檢查並停用過期的主日信息
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void checkAndDeactivateExpiredMessages() {
        try {
            log.info("📅 [主日信息過期檢查] 開始檢查過期主日信息...");
            DeactivationResult result = sundayMessageService.deactivateExpiredMessages();
            
            // 格式化結果消息
            String resultMessage = formatResult(result);
            JobResultHolder.setResult(resultMessage);
            
            log.info("✅ [主日信息過期檢查] 已停用 {} 個過期主日信息", result.getCount());
        } catch (Exception e) {
            log.error("❌ [主日信息過期檢查] 執行失敗: {}", e.getMessage(), e);
            JobResultHolder.clear();
            // 重新拋出異常，確保 Job 狀態標記為 FAILED
            throw new RuntimeException("主日信息過期檢查失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 格式化結果消息
     */
    private String formatResult(DeactivationResult result) {
        if (result.getCount() == 0) {
            return "未發現過期主日信息";
        }
        
        StringBuilder message = new StringBuilder();
        message.append("共停用 ").append(result.getCount()).append(" 個過期主日信息：\n");
        
        // 限制顯示數量，避免消息過長（最多顯示50個）
        int displayCount = Math.min(result.getCount(), 50);
        for (int i = 0; i < displayCount; i++) {
            DeactivationResult.ItemInfo item = result.getItems().get(i);
            log.debug("  - 主日信息 ID: {}, 標題: {}, 日期: {}", item.getId(), item.getTitle(), item.getDate());
            message.append(String.format("- ID: %d, 標題: %s, 日期: %s\n", 
                item.getId(), item.getTitle(), item.getDate()));
        }
        
        if (result.getCount() > 50) {
            message.append(String.format("... 還有 %d 個主日信息未顯示\n", result.getCount() - 50));
        }
        
        return message.toString();
    }

    /**
     * 獲取主日信息過期檢查任務實例
     */
    public SundayMessageExpirationJob getSundayMessageExpirationJob() {
        return new SundayMessageExpirationJob(this);
    }
}
