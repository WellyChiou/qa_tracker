package com.example.helloworld.dto.invest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RunMarketAnalysisResponseDto {
    private String status;
    private String scope;
    private Integer targetCount;
    private Integer analyzedCount;
    private Integer failCount;
    private Integer snapshotCount;
    private Integer signalActiveCount;
    private Integer strategyVersion;
    private LocalDate dataAsOfTradeDate;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Integer getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(Integer targetCount) {
        this.targetCount = targetCount;
    }

    public Integer getAnalyzedCount() {
        return analyzedCount;
    }

    public void setAnalyzedCount(Integer analyzedCount) {
        this.analyzedCount = analyzedCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Integer getSnapshotCount() {
        return snapshotCount;
    }

    public void setSnapshotCount(Integer snapshotCount) {
        this.snapshotCount = snapshotCount;
    }

    public Integer getSignalActiveCount() {
        return signalActiveCount;
    }

    public void setSignalActiveCount(Integer signalActiveCount) {
        this.signalActiveCount = signalActiveCount;
    }

    public Integer getStrategyVersion() {
        return strategyVersion;
    }

    public void setStrategyVersion(Integer strategyVersion) {
        this.strategyVersion = strategyVersion;
    }

    public LocalDate getDataAsOfTradeDate() {
        return dataAsOfTradeDate;
    }

    public void setDataAsOfTradeDate(LocalDate dataAsOfTradeDate) {
        this.dataAsOfTradeDate = dataAsOfTradeDate;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
