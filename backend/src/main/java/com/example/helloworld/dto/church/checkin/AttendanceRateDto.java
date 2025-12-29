package com.example.helloworld.dto.church.checkin;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AttendanceRateDto {
    private Long personId;
    private String personName;
    private String displayName;
    private String memberNo;
    private String category; // 類別：SATURDAY, SUNDAY, WEEKDAY, SPECIAL 或小組名稱
    private Integer totalSessions; // 總場次數
    private Integer checkedInSessions; // 已簽到場次數
    private BigDecimal attendanceRate; // 出席率（百分比）
    private Integer year; // 年度
    private String groupStatus; // 小組狀態：CURRENT（目前）或 HISTORICAL（歷史）
    private LocalDate joinedAt; // 加入小組的時間
    private LocalDate leftAt; // 離開小組的時間（可選）

    public AttendanceRateDto() {}

    public AttendanceRateDto(Long personId, String personName, String displayName, String memberNo, 
                           String category, Integer totalSessions, Integer checkedInSessions, 
                           BigDecimal attendanceRate, Integer year) {
        this.personId = personId;
        this.personName = personName;
        this.displayName = displayName;
        this.memberNo = memberNo;
        this.category = category;
        this.totalSessions = totalSessions;
        this.checkedInSessions = checkedInSessions;
        this.attendanceRate = attendanceRate;
        this.year = year;
    }

    public AttendanceRateDto(Long personId, String personName, String displayName, String memberNo, 
                           String category, Integer totalSessions, Integer checkedInSessions, 
                           BigDecimal attendanceRate, Integer year, String groupStatus, 
                           LocalDate joinedAt, LocalDate leftAt) {
        this.personId = personId;
        this.personName = personName;
        this.displayName = displayName;
        this.memberNo = memberNo;
        this.category = category;
        this.totalSessions = totalSessions;
        this.checkedInSessions = checkedInSessions;
        this.attendanceRate = attendanceRate;
        this.year = year;
        this.groupStatus = groupStatus;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
    }

    // Getters and Setters
    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMemberNo() {
        return memberNo;
    }

    public void setMemberNo(String memberNo) {
        this.memberNo = memberNo;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }

    public Integer getCheckedInSessions() {
        return checkedInSessions;
    }

    public void setCheckedInSessions(Integer checkedInSessions) {
        this.checkedInSessions = checkedInSessions;
    }

    public BigDecimal getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(BigDecimal attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getGroupStatus() {
        return groupStatus;
    }

    public void setGroupStatus(String groupStatus) {
        this.groupStatus = groupStatus;
    }

    public LocalDate getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDate joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDate getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDate leftAt) {
        this.leftAt = leftAt;
    }
}

