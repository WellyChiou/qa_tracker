package com.example.helloworld.scheduler.church;

import com.example.helloworld.service.church.ActivityService;
import com.example.helloworld.service.church.DeactivationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 活動過期檢查排程器
 * 每天晚上 23:00 檢查並停用過期的活動
 */
@Component
public class ActivityExpirationScheduler {

    @Autowired
    private ActivityService activityService;
    
    private static final Logger log = LoggerFactory.getLogger(ActivityExpirationScheduler.class);

    /**
     * 活動過期檢查任務
     */
    public static class ActivityExpirationJob implements Runnable {
        private final ActivityExpirationScheduler scheduler;

        public ActivityExpirationJob(ActivityExpirationScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public void run() {
            scheduler.checkAndDeactivateExpiredActivities();
        }
    }

    /**
     * 檢查並停用過期的活動
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void checkAndDeactivateExpiredActivities() {
        try {
            log.info("📅 [活動過期檢查] 開始檢查過期活動...");
            DeactivationResult result = activityService.deactivateExpiredActivities();
            
            // 格式化結果消息
            String resultMessage = formatResult(result);
            JobResultHolder.setResult(resultMessage);
            
            log.info("✅ [活動過期檢查] 完成，共停用 {} 個過期活動", result.getCount());
        } catch (Exception e) {
            log.error("❌ [活動過期檢查] 執行失敗: {}", e.getMessage(), e);
            JobResultHolder.clear();
            throw e;
        }
    }

    /**
     * 格式化結果消息
     */
    private String formatResult(DeactivationResult result) {
        if (result.getCount() == 0) {
            return "未發現過期活動";
        }
        
        StringBuilder message = new StringBuilder();
        message.append("共停用 ").append(result.getCount()).append(" 個過期活動：\n");
        
        // 限制顯示數量，避免消息過長（最多顯示50個）
        int displayCount = Math.min(result.getCount(), 50);
        for (int i = 0; i < displayCount; i++) {
            DeactivationResult.ItemInfo item = result.getItems().get(i);
            message.append(String.format("- ID: %d, 標題: %s, 日期: %s\n", 
                item.getId(), item.getTitle(), item.getDate()));
        }
        
        if (result.getCount() > 50) {
            message.append(String.format("... 還有 %d 個活動未顯示\n", result.getCount() - 50));
        }
        
        return message.toString();
    }

    /**
     * 獲取活動過期檢查任務實例
     */
    public ActivityExpirationJob getActivityExpirationJob() {
        return new ActivityExpirationJob(this);
    }
}

