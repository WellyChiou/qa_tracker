package com.example.helloworld.dto.church.frontend;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class NextServiceResult {
    private Long id;
    private LocalDate serviceDate;
    private String serviceType;
    private String serviceTypeLabel;
    private String displayDate;
    private String displayWeekday;
    private String displayTime;
}
