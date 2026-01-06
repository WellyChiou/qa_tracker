package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.AboutInfo;
import com.example.helloworld.repository.church.AboutInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AboutInfoService {

    @Autowired
    private AboutInfoRepository aboutInfoRepository;

    /**
     * 獲取所有關於我們資訊（管理用，包含未啟用的）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<AboutInfo> getAllInfo() {
        return aboutInfoRepository.findAllByOrderByDisplayOrderAsc();
    }
    
    /**
     * 獲取所有關於我們資訊（支持分頁和過濾）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Page<AboutInfo> getAllInfo(String sectionKey, String title, Boolean isActive, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String filterSectionKey = (sectionKey != null && !sectionKey.trim().isEmpty()) ? sectionKey.trim() : null;
        String filterTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : null;
        return aboutInfoRepository.findByFilters(filterSectionKey, filterTitle, isActive, pageable);
    }

    /**
     * 獲取所有啟用的關於我們資訊
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<AboutInfo> getAllActiveInfo() {
        return aboutInfoRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * 根據 ID 獲取關於我們資訊
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public AboutInfo getInfoById(Long id) {
        return aboutInfoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("關於我們資訊不存在: " + id));
    }

    /**
     * 根據 sectionKey 獲取關於我們資訊
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<AboutInfo> getInfoBySectionKey(String sectionKey) {
        return aboutInfoRepository.findBySectionKeyAndIsActiveTrue(sectionKey);
    }

    /**
     * 創建關於我們資訊
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public AboutInfo createInfo(AboutInfo aboutInfo) {
        return aboutInfoRepository.save(aboutInfo);
    }

    /**
     * 更新關於我們資訊
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public AboutInfo updateInfo(Long id, AboutInfo aboutInfo) {
        AboutInfo existing = aboutInfoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("關於我們資訊不存在: " + id));
        
        existing.setSectionKey(aboutInfo.getSectionKey());
        existing.setTitle(aboutInfo.getTitle());
        existing.setContent(aboutInfo.getContent());
        existing.setDisplayOrder(aboutInfo.getDisplayOrder());
        existing.setIsActive(aboutInfo.getIsActive());
        
        return aboutInfoRepository.save(existing);
    }

    /**
     * 刪除關於我們資訊
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteInfo(Long id) {
        aboutInfoRepository.deleteById(id);
    }
}

