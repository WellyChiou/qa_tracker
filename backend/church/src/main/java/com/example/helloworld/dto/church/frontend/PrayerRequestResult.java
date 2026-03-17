package com.example.helloworld.dto.church.frontend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrayerRequestResult {
    private Long id;
    private String title;
    private String content;
    private String category;
    private Boolean isUrgent;
    private Boolean active;
    private LocalDateTime createdAt;
}

