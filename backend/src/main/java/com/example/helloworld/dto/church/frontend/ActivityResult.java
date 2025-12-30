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
    private LocalDate activityDate;   // ✅ 只有活動日
    private String imageUrl;
    private Boolean active;
}
