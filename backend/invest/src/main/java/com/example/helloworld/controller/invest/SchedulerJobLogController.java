package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.SchedulerJobLogPagedDto;
import com.example.helloworld.service.invest.SchedulerJobLogService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/invest/scheduler-job-logs")
@CrossOrigin(origins = "*")
public class SchedulerJobLogController {

    private final SchedulerJobLogService schedulerJobLogService;

    public SchedulerJobLogController(SchedulerJobLogService schedulerJobLogService) {
        this.schedulerJobLogService = schedulerJobLogService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<SchedulerJobLogPagedDto>>> getPaged(
        @RequestParam(required = false) String jobName,
        @RequestParam(required = false) LocalDate runDateFrom,
        @RequestParam(required = false) LocalDate runDateTo,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<SchedulerJobLogPagedDto> result = schedulerJobLogService.getPaged(
                jobName, runDateFrom, runDateTo, status, page, size
            );
            PageResponse<SchedulerJobLogPagedDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢排程執行紀錄失敗：" + e.getMessage()));
        }
    }
}
