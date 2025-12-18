package com.example.helloworld.dto.line;

import java.time.LocalDateTime;

public class LineQuotaLog {

    private String action;
    private String reason;
    private LocalDateTime time;

    public LineQuotaLog(String action, String reason) {
        this.action = action;
        this.reason = reason;
        this.time = LocalDateTime.now();
    }

    // getter
}
