package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.Announcement;
import com.example.helloworld.repository.church.AnnouncementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {
    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    @Autowired
    private AnnouncementRepository announcementRepository;

    /**
     * 獲取所有公告（管理用，包含未啟用的）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByIsPinnedDescPublishDateDesc();
    }

    /**
     * 獲取所有啟用的公告（前台用）
     * 置頂的排在前面，然後按發布日期降序
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<Announcement> getAllActiveAnnouncements() {
        return announcementRepository.findByIsActiveTrueOrderByIsPinnedDescPublishDateDesc();
    }

    /**
     * 獲取有效的公告（已發布且未過期）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<Announcement> getValidAnnouncements() {
        LocalDate today = LocalDate.now();
        List<Announcement> allActive = announcementRepository.findByIsActiveTrueOrderByIsPinnedDescPublishDateDesc();
        
        return allActive.stream()
            .filter(announcement -> {
                // 檢查發布日期
                if (announcement.getPublishDate() != null && announcement.getPublishDate().isAfter(today)) {
                    return false;
                }
                // 檢查到期日期
                if (announcement.getExpireDate() != null && announcement.getExpireDate().isBefore(today)) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * 根據 ID 獲取公告
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<Announcement> getAnnouncementById(Long id) {
        return announcementRepository.findById(id);
    }

    /**
     * 創建公告
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public Announcement createAnnouncement(Announcement announcement) {
        if (announcement.getPublishDate() == null) {
            announcement.setPublishDate(LocalDate.now());
        }
        return announcementRepository.save(announcement);
    }

    /**
     * 更新公告
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public Announcement updateAnnouncement(Long id, Announcement announcement) {
        Announcement existing = announcementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("公告不存在: " + id));
        
        existing.setTitle(announcement.getTitle());
        existing.setContent(announcement.getContent());
        existing.setCategory(announcement.getCategory());
        existing.setPublishDate(announcement.getPublishDate());
        existing.setExpireDate(announcement.getExpireDate());
        existing.setIsPinned(announcement.getIsPinned());
        existing.setIsActive(announcement.getIsActive());
        
        return announcementRepository.save(existing);
    }

    /**
     * 刪除公告
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("公告不存在: " + id));
        
        announcementRepository.deleteById(id);
    }
}

