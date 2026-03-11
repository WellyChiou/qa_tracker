package com.example.helloworld.dto.church.frontend;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SundayMessageResult {
    private Long id;
    private String title;        // 講題
    private String speaker;      // 講員
    private String scripture;    // 經文
    private LocalDate messageDate;
    private String imageUrl;   // DM 圖
    private Boolean active;
}

