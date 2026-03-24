package com.example.helloworld.dto.invest;

import java.time.LocalDateTime;

public class DashboardAlertSummaryDto {
    private Long totalActiveAlerts;
    private Long highSeverityCount;
    private LocalDateTime latestTriggeredAt;

    public Long getTotalActiveAlerts() {
        return totalActiveAlerts;
    }

    public void setTotalActiveAlerts(Long totalActiveAlerts) {
        this.totalActiveAlerts = totalActiveAlerts;
    }

    public Long getHighSeverityCount() {
        return highSeverityCount;
    }

    public void setHighSeverityCount(Long highSeverityCount) {
        this.highSeverityCount = highSeverityCount;
    }

    public LocalDateTime getLatestTriggeredAt() {
        return latestTriggeredAt;
    }

    public void setLatestTriggeredAt(LocalDateTime latestTriggeredAt) {
        this.latestTriggeredAt = latestTriggeredAt;
    }
}
