package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.SundayMessage;
import com.example.helloworld.repository.church.SundayMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class SundayMessageService {

    @Autowired
    private SundayMessageRepository sundayMessageRepository;

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 獲取所有主日信息（管理用，包含未啟用的）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<SundayMessage> getAllMessages() {
        return sundayMessageRepository.findAllByOrderByServiceDateDesc();
    }

    /**
     * 獲取所有啟用的主日信息
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<SundayMessage> getAllActiveMessages() {
        return sundayMessageRepository.findByIsActiveTrueOrderByServiceDateDesc();
    }

    /**
     * 根據 ID 獲取主日信息
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<SundayMessage> getMessageById(Long id) {
        return sundayMessageRepository.findById(id);
    }

    /**
     * 創建主日信息
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public SundayMessage createMessage(SundayMessage message) {
        return sundayMessageRepository.save(message);
    }

    /**
     * 更新主日信息
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public SundayMessage updateMessage(Long id, SundayMessage message) {
        SundayMessage existing = sundayMessageRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("主日信息不存在: " + id));
        
        // 如果圖片 URL 改變，刪除舊圖片
        String oldImageUrl = existing.getImageUrl();
        String newImageUrl = message.getImageUrl();
        if (oldImageUrl != null && !oldImageUrl.isEmpty() && 
            !oldImageUrl.equals(newImageUrl)) {
            try {
                fileUploadService.deleteImage(oldImageUrl);
            } catch (Exception e) {
                // 圖片刪除失敗不影響數據更新，只記錄錯誤
                System.err.println("刪除舊圖片失敗: " + e.getMessage());
            }
        }
        
        existing.setServiceDate(message.getServiceDate());
        existing.setServiceType(message.getServiceType());
        existing.setImageUrl(message.getImageUrl());
        existing.setTitle(message.getTitle());
        existing.setScripture(message.getScripture());
        existing.setSpeaker(message.getSpeaker());
        existing.setContent(message.getContent());
        existing.setIsActive(message.getIsActive());
        
        return sundayMessageRepository.save(existing);
    }

    /**
     * 刪除主日信息
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteMessage(Long id) {
        SundayMessage message = sundayMessageRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("主日信息不存在: " + id));
        
        // 刪除相關圖片
        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            try {
                fileUploadService.deleteImage(message.getImageUrl());
            } catch (Exception e) {
                // 圖片刪除失敗不影響數據刪除，只記錄錯誤
                System.err.println("刪除主日信息圖片失敗: " + e.getMessage());
            }
        }
        
        sundayMessageRepository.deleteById(id);
    }

    /**
     * 停用過期的主日信息
     * 如果主日信息日期已過（小於今天），且目前是啟用狀態，則設為不啟用
     * 
     * @return 停用的主日信息詳細信息
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public DeactivationResult deactivateExpiredMessages() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        List<SundayMessage> expiredMessages = sundayMessageRepository.findAll().stream()
            .filter(message -> message.getIsActive() != null && message.getIsActive())
            .filter(message -> message.getServiceDate() != null)
            .filter(message -> message.getServiceDate().isBefore(today))
            .collect(Collectors.toList());
        
        List<DeactivationResult.ItemInfo> itemInfos = new ArrayList<>();
        for (SundayMessage message : expiredMessages) {
            message.setIsActive(false);
            sundayMessageRepository.save(message);
            
            String dateStr = message.getServiceDate() != null 
                ? message.getServiceDate().format(formatter) 
                : "-";
            itemInfos.add(new DeactivationResult.ItemInfo(
                message.getId(),
                message.getTitle() != null ? message.getTitle() : "-",
                dateStr
            ));
        }
        
        return new DeactivationResult(expiredMessages.size(), itemInfos);
    }
}

