package com.example.helloworld.dto.church.checkin;

import java.time.LocalDateTime;

/**
 * Interface projection for SessionCheckinRow
 * Used for mapping native SQL query results
 */
public interface SessionCheckinRowProjection {
    Long getId();
    String getMemberNo();
    String getMemberName();
    LocalDateTime getCheckedInAt();
    Boolean getManual();
    String getManualBy();
    String getIp();
}

