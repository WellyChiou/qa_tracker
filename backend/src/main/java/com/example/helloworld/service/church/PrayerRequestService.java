package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.PrayerRequest;
import com.example.helloworld.repository.church.PrayerRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PrayerRequestService {
    private static final Logger log = LoggerFactory.getLogger(PrayerRequestService.class);

    @Autowired
    private PrayerRequestRepository prayerRequestRepository;

    /**
     * 獲取所有代禱事項（管理用，包含未啟用的）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<PrayerRequest> getAllPrayerRequests() {
        return prayerRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 獲取所有啟用的代禱事項
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<PrayerRequest> getAllActivePrayerRequests() {
        return prayerRequestRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    /**
     * 根據 ID 獲取代禱事項
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<PrayerRequest> getPrayerRequestById(Long id) {
        return prayerRequestRepository.findById(id);
    }

    /**
     * 創建代禱事項
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public PrayerRequest createPrayerRequest(PrayerRequest prayerRequest) {
        return prayerRequestRepository.save(prayerRequest);
    }

    /**
     * 更新代禱事項
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public PrayerRequest updatePrayerRequest(Long id, PrayerRequest prayerRequest) {
        PrayerRequest existing = prayerRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("代禱事項不存在: " + id));
        
        existing.setTitle(prayerRequest.getTitle());
        existing.setContent(prayerRequest.getContent());
        existing.setCategory(prayerRequest.getCategory());
        existing.setIsUrgent(prayerRequest.getIsUrgent());
        existing.setIsActive(prayerRequest.getIsActive());
        
        return prayerRequestRepository.save(existing);
    }

    /**
     * 刪除代禱事項
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deletePrayerRequest(Long id) {
        PrayerRequest prayerRequest = prayerRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("代禱事項不存在: " + id));
        
        prayerRequestRepository.deleteById(id);
    }
}

