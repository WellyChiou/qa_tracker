package com.example.helloworld.dto.church.frontend;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ActivityResult {
    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDate activityDate;
    private String startTime;     // 活動開始時間
    private String endTime;       // 活動結束時間
    private String activitySessions;  // 活動時間段（JSON 字串，用於多個上課時間）
    private String tags;          // 活動標籤（JSON 字串）
    private String imageUrl;
    private Boolean active;
}
