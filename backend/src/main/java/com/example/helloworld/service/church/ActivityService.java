package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.Activity;
import com.example.helloworld.repository.church.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ActivityService {

    @Autowired
    private ActivityRepository activityRepository;

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
        
        existing.setTitle(activity.getTitle());
        existing.setDescription(activity.getDescription());
        existing.setActivityDate(activity.getActivityDate());
        existing.setActivityTime(activity.getActivityTime());
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
        activityRepository.deleteById(id);
    }
}

