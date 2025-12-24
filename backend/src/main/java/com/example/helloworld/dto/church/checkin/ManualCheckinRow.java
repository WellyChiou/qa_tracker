package com.example.helloworld.dto.church.checkin;

import java.time.LocalDateTime;

/**
 * Interface projection for ManualCheckinRow
 * Used for mapping native SQL query results in Spring Data JPA
 */
public interface ManualCheckinRow {
    Long getId();
    Long getSessionId();
    String getSessionTitle();
    String getSessionDate();
    String getMemberNo();
    String getMemberName();
    LocalDateTime getCheckedInAt();
    String getIp();
    String getUserAgent();
    String getManualBy();
    String getManualNote();
    Integer getCanceled();
    
    default Boolean isCanceled() {
        Integer canceled = getCanceled();
        return canceled != null && canceled == 1;
    }
    
    LocalDateTime getCanceledAt();
    String getCanceledBy();
    String getCanceledNote();
}

