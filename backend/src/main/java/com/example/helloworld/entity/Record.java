package com.example.helloworld.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "records")
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_id", length = 128, unique = true)
    private String firebaseId;

    @Column(name = "issue_number")
    private Integer issueNumber;

    @Column(name = "issue_link", columnDefinition = "TEXT")
    private String issueLink;

    @Column(name = "status")
    private Integer status; // 0=執行中止, 1=執行中, 2=完成

    @Column(name = "category")
    private Integer category; // 1=BUG, 2=改善, 3=優化, 4=模組, 5=QA

    @Column(name = "feature", columnDefinition = "TEXT")
    private String feature;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "test_plan", length = 1)
    private String testPlan; // "0"=否, "1"=是

    @Column(name = "bug_found")
    private Integer bugFound; // 0=否, 1=是

    @Column(name = "optimization_points")
    private Integer optimizationPoints;

    @Column(name = "verify_failed")
    private Integer verifyFailed; // 0=否, 1=是

    @Column(name = "test_cases")
    private Integer testCases;

    @Column(name = "file_count")
    private Integer fileCount;

    @Column(name = "test_start_date")
    private LocalDate testStartDate;

    @Column(name = "eta_date")
    private LocalDate etaDate;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @Column(name = "created_by_uid", length = 128)
    private String createdByUid;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by_uid", length = 128)
    private String updatedByUid;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    public Integer getIssueNumber() { return issueNumber; }
    public void setIssueNumber(Integer issueNumber) { this.issueNumber = issueNumber; }

    public String getIssueLink() { return issueLink; }
    public void setIssueLink(String issueLink) { this.issueLink = issueLink; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }

    public String getFeature() { return feature; }
    public void setFeature(String feature) { this.feature = feature; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public String getTestPlan() { return testPlan; }
    public void setTestPlan(String testPlan) { this.testPlan = testPlan; }

    public Integer getBugFound() { return bugFound; }
    public void setBugFound(Integer bugFound) { this.bugFound = bugFound; }

    public Integer getOptimizationPoints() { return optimizationPoints; }
    public void setOptimizationPoints(Integer optimizationPoints) { this.optimizationPoints = optimizationPoints; }

    public Integer getVerifyFailed() { return verifyFailed; }
    public void setVerifyFailed(Integer verifyFailed) { this.verifyFailed = verifyFailed; }

    public Integer getTestCases() { return testCases; }
    public void setTestCases(Integer testCases) { this.testCases = testCases; }

    public Integer getFileCount() { return fileCount; }
    public void setFileCount(Integer fileCount) { this.fileCount = fileCount; }

    public LocalDate getTestStartDate() { return testStartDate; }
    public void setTestStartDate(LocalDate testStartDate) { this.testStartDate = testStartDate; }

    public LocalDate getEtaDate() { return etaDate; }
    public void setEtaDate(LocalDate etaDate) { this.etaDate = etaDate; }

    public LocalDate getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDate completedAt) { this.completedAt = completedAt; }

    public String getCreatedByUid() { return createdByUid; }
    public void setCreatedByUid(String createdByUid) { this.createdByUid = createdByUid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUpdatedByUid() { return updatedByUid; }
    public void setUpdatedByUid(String updatedByUid) { this.updatedByUid = updatedByUid; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

