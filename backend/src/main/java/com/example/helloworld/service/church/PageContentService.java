package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.PageContent;
import com.example.helloworld.repository.church.PageContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PageContentService {

    @Autowired
    private PageContentRepository pageContentRepository;

    /**
     * 獲取所有頁面內容
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<PageContent> getAllPageContents() {
        return pageContentRepository.findAll();
    }

    /**
     * 根據頁面代碼獲取頁面內容（公開訪問，只返回啟用的）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<PageContent> getPageContentByCode(String pageCode) {
        return pageContentRepository.findByPageCodeAndIsActiveTrue(pageCode);
    }

    /**
     * 根據頁面代碼獲取頁面內容（管理用，可獲取未啟用的）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<PageContent> getPageContentByCodeForAdmin(String pageCode) {
        return pageContentRepository.findByPageCode(pageCode);
    }

    /**
     * 根據 ID 獲取頁面內容
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<PageContent> getPageContentById(Long id) {
        return pageContentRepository.findById(id);
    }

    /**
     * 創建頁面內容
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public PageContent createPageContent(PageContent pageContent) {
        return pageContentRepository.save(pageContent);
    }

    /**
     * 更新頁面內容
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public PageContent updatePageContent(Long id, PageContent pageContentUpdate) {
        PageContent existing = pageContentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("頁面內容不存在: " + id));

        if (pageContentUpdate.getPageTitle() != null) {
            existing.setPageTitle(pageContentUpdate.getPageTitle());
        }
        if (pageContentUpdate.getContent() != null) {
            existing.setContent(pageContentUpdate.getContent());
        }
        if (pageContentUpdate.getMetaDescription() != null) {
            existing.setMetaDescription(pageContentUpdate.getMetaDescription());
        }
        if (pageContentUpdate.getMetaKeywords() != null) {
            existing.setMetaKeywords(pageContentUpdate.getMetaKeywords());
        }
        if (pageContentUpdate.getIsActive() != null) {
            existing.setIsActive(pageContentUpdate.getIsActive());
        }
        if (pageContentUpdate.getUpdatedBy() != null) {
            existing.setUpdatedBy(pageContentUpdate.getUpdatedBy());
        }

        return pageContentRepository.save(existing);
    }

    /**
     * 刪除頁面內容
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deletePageContent(Long id) {
        pageContentRepository.deleteById(id);
    }
}

