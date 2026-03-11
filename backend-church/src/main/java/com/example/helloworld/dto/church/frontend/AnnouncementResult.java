package com.example.helloworld.dto.church.frontend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResult {
    private Long id;
    private String title;
    private String content;
    private String category;
    private LocalDate publishDate;
    private LocalDate expireDate;
    private Boolean isPinned;
    private Boolean active;
    private LocalDateTime createdAt;
}

