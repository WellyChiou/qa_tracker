package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ContactSubmission;
import com.example.helloworld.repository.church.ContactSubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ContactSubmissionService {

    @Autowired
    private ContactSubmissionRepository contactSubmissionRepository;

    /**
     * 獲取所有聯絡表單提交記錄（分頁）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Page<ContactSubmission> getAllSubmissions(Pageable pageable) {
        return contactSubmissionRepository.findAllByOrderBySubmittedAtDesc(pageable);
    }

    /**
     * 根據已讀狀態獲取聯絡表單提交記錄（分頁）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Page<ContactSubmission> getSubmissionsByIsRead(Boolean isRead, Pageable pageable) {
        return contactSubmissionRepository.findByIsReadOrderBySubmittedAtDesc(isRead, pageable);
    }

    /**
     * 根據 ID 獲取聯絡表單提交記錄
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ContactSubmission> getSubmissionById(Long id) {
        return contactSubmissionRepository.findById(id);
    }

    /**
     * 創建聯絡表單提交記錄
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ContactSubmission createSubmission(ContactSubmission submission) {
        submission.setIsRead(false);
        return contactSubmissionRepository.save(submission);
    }

    /**
     * 標記聯絡表單提交記錄為已讀
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ContactSubmission markAsRead(Long id, String readBy) {
        ContactSubmission existing = contactSubmissionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("聯絡表單記錄不存在: " + id));

        existing.setIsRead(true);
        existing.setReadAt(LocalDateTime.now());
        if (readBy != null) {
            existing.setReadBy(readBy);
        }

        return contactSubmissionRepository.save(existing);
    }

    /**
     * 刪除聯絡表單提交記錄
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public void deleteSubmission(Long id) {
        contactSubmissionRepository.deleteById(id);
    }

    /**
     * 獲取已讀/未讀的統計數量
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Long countByIsRead(Boolean isRead) {
        return contactSubmissionRepository.countByIsRead(isRead);
    }
}

