package com.example.helloworld.dto.church.checkin;

import java.time.LocalDateTime;

/**
 * Interface projection for SessionCheckinRow
 * Used for mapping native SQL query results in Spring Data JPA
 */
public interface SessionCheckinRow {
    Long getId();
    String getMemberNo();
    String getMemberName();
    LocalDateTime getCheckedInAt();
    Integer getManual();
    
    default Boolean isManual() {
        Integer manual = getManual();
        return manual != null && manual == 1;
    }
    String getManualBy();
    String getIp();
    String getUserAgent();
    Integer getCanceled();
    
    default Boolean isCanceled() {
        Integer canceled = getCanceled();
        return canceled != null && canceled == 1;
    }
}

