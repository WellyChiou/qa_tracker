package com.example.helloworld.service.personal;

import com.example.helloworld.entity.personal.Record;
import com.example.helloworld.entity.personal.User;
import com.example.helloworld.repository.personal.RecordRepository;
import com.example.helloworld.repository.personal.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RecordService {
    
    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private UserRepository userRepository;
    
    // 取得所有記錄
    public List<Record> getAllRecords() {
        return recordRepository.findAll();
    }
    
    // 根據 ID 取得記錄
    public Optional<Record> getRecordById(Long id) {
        return recordRepository.findById(id);
    }
    
    // 建立記錄
    @Transactional
    public Record createRecord(Record record) {
        // 設置用戶 ID
        String currentUserUid = getCurrentUserUid();
        if (record.getCreatedByUid() == null) {
            record.setCreatedByUid(currentUserUid);
        }
        record.setUpdatedByUid(currentUserUid);

        return recordRepository.save(record);
    }
    
    // 更新記錄
    @Transactional
    public Record updateRecord(Long id, Record record) {
        Record existingRecord = recordRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Record not found with id: " + id));
        
        // 更新欄位
        if (record.getIssueNumber() != null) existingRecord.setIssueNumber(record.getIssueNumber());
        if (record.getIssueLink() != null) existingRecord.setIssueLink(record.getIssueLink());
        if (record.getStatus() != null) existingRecord.setStatus(record.getStatus());
        if (record.getCategory() != null) existingRecord.setCategory(record.getCategory());
        if (record.getFeature() != null) existingRecord.setFeature(record.getFeature());
        if (record.getMemo() != null) existingRecord.setMemo(record.getMemo());
        if (record.getTestPlan() != null) existingRecord.setTestPlan(record.getTestPlan());
        if (record.getBugFound() != null) existingRecord.setBugFound(record.getBugFound());
        if (record.getOptimizationPoints() != null) existingRecord.setOptimizationPoints(record.getOptimizationPoints());
        if (record.getVerifyFailed() != null) existingRecord.setVerifyFailed(record.getVerifyFailed());
        if (record.getTestCases() != null) existingRecord.setTestCases(record.getTestCases());
        if (record.getFileCount() != null) existingRecord.setFileCount(record.getFileCount());
        if (record.getTestStartDate() != null) existingRecord.setTestStartDate(record.getTestStartDate());
        if (record.getEtaDate() != null) existingRecord.setEtaDate(record.getEtaDate());
        if (record.getCompletedAt() != null) existingRecord.setCompletedAt(record.getCompletedAt());

        // 設置更新用戶 ID
        existingRecord.setUpdatedByUid(getCurrentUserUid());
        
        return recordRepository.save(existingRecord);
    }
    
    // 刪除記錄
    @Transactional
    public void deleteRecord(Long id) {
        recordRepository.deleteById(id);
    }
    
    // 搜尋記錄
    public Page<Record> searchRecords(
        Integer status,
        Integer category,
        String testPlan,
        Integer bugFound,
        Integer issueNumber,
        String keyword,
        LocalDate testStartDateFrom,
        LocalDate testStartDateTo,
        LocalDate etaDateFrom,
        LocalDate etaDateTo,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size, 
            Sort.by("issueNumber").descending()
                .and(Sort.by("status").descending())
                .and(Sort.by("createdAt").descending()));
        
        return recordRepository.searchRecords(
            status, category, testPlan, bugFound, issueNumber, keyword,
            testStartDateFrom, testStartDateTo, etaDateFrom, etaDateTo, pageable
        );
    }
    
    // 取得所有記錄（不分頁，用於匯出）
    public List<Record> getAllRecordsForExport(
        Integer status,
        Integer category,
        String testPlan,
        Integer bugFound,
        Integer issueNumber,
        String keyword,
        LocalDate testStartDateFrom,
        LocalDate testStartDateTo,
        LocalDate etaDateFrom,
        LocalDate etaDateTo
    ) {
        // 使用大分頁來取得所有符合條件的記錄
        Pageable pageable = PageRequest.of(0, 10000);
        Page<Record> page = recordRepository.searchRecords(
            status, category, testPlan, bugFound, issueNumber, keyword,
            testStartDateFrom, testStartDateTo, etaDateFrom, etaDateTo, pageable
        );
        return page.getContent();
    }
    
    // 統計執行中筆數
    public long countInProgress() {
        return recordRepository.countByStatus(1);
    }

    // 統計指定年份的執行中筆數
    public long countInProgressByYear(int year) {
        return recordRepository.countByTestStartDateYearAndStatus(year, 1);
    }

    // 統計指定年份的已完成筆數
    public long countCompletedByYear(int year) {
        return recordRepository.countByTestStartDateYearAndStatus(year, 2);
    }
    
    // 統計符合查詢條件的記錄中各種狀態的數量
    public long countBySearchConditionsAndStatus(
        Integer status,
        Integer category,
        String testPlan,
        Integer bugFound,
        Integer issueNumber,
        String keyword,
        LocalDate testStartDateFrom,
        LocalDate testStartDateTo,
        LocalDate etaDateFrom,
        LocalDate etaDateTo,
        Integer targetStatus
    ) {
        return recordRepository.countBySearchConditionsAndStatus(
            status, category, testPlan, bugFound, issueNumber, keyword,
            testStartDateFrom, testStartDateTo, etaDateFrom, etaDateTo, targetStatus
        );
    }

    // 統計指定年份的總記錄數
    public long countTotalRecordsByYear(int year) {
        return recordRepository.countByTestStartDateYear(year);
    }

    /**
     * 獲取當前登入用戶的 UID
     */
    private String getCurrentUserUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            // 根據 username 查找對應的 uid
            return userRepository.findByUsername(username)
                    .map(User::getUid)
                    .orElse(null);
        }
        return null;
    }
}
