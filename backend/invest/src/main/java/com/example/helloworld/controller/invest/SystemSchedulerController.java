package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import com.example.helloworld.service.invest.systemscheduler.SystemSchedulerFacadeService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invest/system/scheduler")
@CrossOrigin(origins = "*")
public class SystemSchedulerController {

    private final SystemSchedulerFacadeService systemSchedulerFacadeService;

    public SystemSchedulerController(SystemSchedulerFacadeService systemSchedulerFacadeService) {
        this.systemSchedulerFacadeService = systemSchedulerFacadeService;
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<SystemSchedulerJobDto>>> getJobs() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(systemSchedulerFacadeService.getJobs()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢排程任務失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/jobs/{jobCode}/run-now")
    public ResponseEntity<ApiResponse<SystemSchedulerRunNowResponseDto>> runNow(
        @PathVariable String jobCode
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(systemSchedulerFacadeService.runNow(jobCode)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("執行 Run Now 失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/jobs/{jobCode}/logs/paged")
    public ResponseEntity<ApiResponse<PageResponse<SystemSchedulerJobLogPagedDto>>> getLogsPaged(
        @PathVariable String jobCode,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<SystemSchedulerJobLogPagedDto> result = systemSchedulerFacadeService.getLogs(jobCode, page, size);
            PageResponse<SystemSchedulerJobLogPagedDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢排程紀錄失敗：" + e.getMessage()));
        }
    }
}
