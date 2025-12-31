package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.Activity;
import com.example.helloworld.repository.church.ActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActivityService {
    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 獲取所有活動（管理用，包含未啟用的）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<Activity> getAllActivities() {
        return activityRepository.findAllByOrderByActivityDateAsc();
    }

    /**
     * 獲取所有啟用的活動
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<Activity> getAllActiveActivities() {
        return activityRepository.findByIsActiveTrueOrderByActivityDateAsc();
    }

    /**
     * 獲取未來或當天的活動
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<Activity> getUpcomingActivities() {
        return activityRepository.findByActivityDateGreaterThanEqualAndIsActiveTrueOrderByActivityDateAsc(LocalDate.now());
    }

    /**
     * 根據 ID 獲取活動
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<Activity> getActivityById(Long id) {
        return activityRepository.findById(id);
    }

    /**
     * 創建活動
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public Activity createActivity(Activity activity) {
        return activityRepository.save(activity);
    }

    /**
     * 更新活動
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public Activity updateActivity(Long id, Activity activity) {
        Activity existing = activityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("活動不存在: " + id));
        
        // 如果圖片 URL 改變，刪除舊圖片
        String oldImageUrl = existing.getImageUrl();
        String newImageUrl = activity.getImageUrl();
        if (oldImageUrl != null && !oldImageUrl.isEmpty() && 
            !oldImageUrl.equals(newImageUrl)) {
            try {
                fileUploadService.deleteImage(oldImageUrl);
            } catch (Exception e) {
                // 圖片刪除失敗不影響數據更新，只記錄錯誤
                log.error("❌ 刪除舊圖片失敗: {}", e.getMessage());
            }
        }
        
        existing.setTitle(activity.getTitle());
        existing.setDescription(activity.getDescription());
        existing.setActivityDate(activity.getActivityDate());
        existing.setStartTime(activity.getStartTime());
        existing.setEndTime(activity.getEndTime());
        existing.setActivitySessions(activity.getActivitySessions());
        existing.setLocation(activity.getLocation());
        existing.setTags(activity.getTags());
        existing.setImageUrl(activity.getImageUrl());
        existing.setIsActive(activity.getIsActive());
        
        return activityRepository.save(existing);
    }

    /**
     * 刪除活動
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteActivity(Long id) {
        Activity activity = activityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("活動不存在: " + id));
        
        // 刪除相關圖片
        if (activity.getImageUrl() != null && !activity.getImageUrl().isEmpty()) {
            try {
                fileUploadService.deleteImage(activity.getImageUrl());
            } catch (Exception e) {
                // 圖片刪除失敗不影響數據刪除，只記錄錯誤
                log.error("❌ 刪除活動圖片失敗: {}", e.getMessage());
            }
        }
        
        activityRepository.deleteById(id);
    }

    /**
     * 停用過期的活動
     * 如果活動日期已過（小於今天），且目前是啟用狀態，則設為不啟用
     * 
     * @return 停用的活動詳細信息
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public DeactivationResult deactivateExpiredActivities() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        List<Activity> expiredActivities = activityRepository.findAll().stream()
            .filter(activity -> activity.getIsActive() != null && activity.getIsActive())
            .filter(activity -> activity.getActivityDate() != null)
            .filter(activity -> activity.getActivityDate().isBefore(today))
            .collect(Collectors.toList());
        
        List<DeactivationResult.ItemInfo> itemInfos = new ArrayList<>();
        for (Activity activity : expiredActivities) {
            activity.setIsActive(false);
            activityRepository.save(activity);
            
            String dateStr = activity.getActivityDate() != null 
                ? activity.getActivityDate().format(formatter) 
                : "-";
            itemInfos.add(new DeactivationResult.ItemInfo(
                activity.getId(),
                activity.getTitle() != null ? activity.getTitle() : "-",
                dateStr
            ));
        }
        
        return new DeactivationResult(expiredActivities.size(), itemInfos);
    }
}

