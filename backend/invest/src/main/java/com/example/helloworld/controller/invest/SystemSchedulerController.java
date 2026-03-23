package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.SystemSchedulerExecutionViewDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobUpsertRequestDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import com.example.helloworld.service.invest.systemscheduler.SystemSchedulerFacadeService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    public ResponseEntity<ApiResponse<List<SystemSchedulerJobDto>>> getJobs(
        @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(systemSchedulerFacadeService.getJobs(includeInactive)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢排程任務失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/jobs/{jobCode}")
    public ResponseEntity<ApiResponse<SystemSchedulerJobDto>> getJob(@PathVariable String jobCode) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(systemSchedulerFacadeService.getJob(jobCode)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢排程任務失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<SystemSchedulerJobDto>> createJob(
        @RequestBody SystemSchedulerJobUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(systemSchedulerFacadeService.createJob(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("新增排程任務失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/jobs/{jobCode}")
    public ResponseEntity<ApiResponse<SystemSchedulerJobDto>> updateJob(
        @PathVariable String jobCode,
        @RequestBody SystemSchedulerJobUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(systemSchedulerFacadeService.updateJob(jobCode, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("編輯排程任務失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/jobs/{jobCode}/toggle")
    public ResponseEntity<ApiResponse<SystemSchedulerJobDto>> toggleJob(
        @PathVariable String jobCode,
        @RequestParam Boolean enabled
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(systemSchedulerFacadeService.toggleJob(jobCode, enabled)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("切換排程任務狀態失敗：" + e.getMessage()));
        }
    }

    @DeleteMapping("/jobs/{jobCode}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable String jobCode) {
        try {
            systemSchedulerFacadeService.deleteJob(jobCode);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除排程任務失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/jobs/{jobCode}/run-now")
    public ResponseEntity<ApiResponse<SystemSchedulerRunNowResponseDto>> runNow(
        @PathVariable String jobCode
    ) {
        try {
            SystemSchedulerRunNowResponseDto dto = systemSchedulerFacadeService.runNow(jobCode);
            dto = normalizeExecutionId(jobCode, dto);
            return ResponseEntity.ok(ApiResponse.ok(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("執行 Run Now 失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/jobs/{jobCode}/execute")
    public ResponseEntity<ApiResponse<SystemSchedulerRunNowResponseDto>> execute(
        @PathVariable String jobCode
    ) {
        try {
            SystemSchedulerRunNowResponseDto dto = systemSchedulerFacadeService.runNow(jobCode);
            dto = normalizeExecutionId(jobCode, dto);
            return ResponseEntity.ok(ApiResponse.ok(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("執行 execute 失敗：" + e.getMessage()));
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

    @GetMapping("/jobs/{jobCode}/executions")
    public ResponseEntity<ApiResponse<PageResponse<SystemSchedulerExecutionViewDto>>> getExecutions(
        @PathVariable String jobCode,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<SystemSchedulerJobLogPagedDto> logsPage = systemSchedulerFacadeService.getLogs(jobCode, page, size);
            List<SystemSchedulerExecutionViewDto> content = logsPage.getContent().stream()
                .map(this::toExecutionView)
                .toList();
            PageResponse<SystemSchedulerExecutionViewDto> response = new PageResponse<>(
                content,
                logsPage.getTotalElements(),
                logsPage.getTotalPages(),
                logsPage.getNumber(),
                logsPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢排程 executions 失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/jobs/{jobCode}/executions/latest")
    public ResponseEntity<ApiResponse<SystemSchedulerExecutionViewDto>> getLatestExecution(
        @PathVariable String jobCode
    ) {
        try {
            Page<SystemSchedulerJobLogPagedDto> logsPage = systemSchedulerFacadeService.getLogs(jobCode, 0, 1);
            if (logsPage.getContent().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("找不到執行記錄"));
            }
            return ResponseEntity.ok(ApiResponse.ok(toExecutionView(logsPage.getContent().get(0))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢最新 execution 失敗：" + e.getMessage()));
        }
    }

    private SystemSchedulerRunNowResponseDto normalizeExecutionId(String jobCode, SystemSchedulerRunNowResponseDto dto) {
        if (dto == null) {
            return null;
        }
        if (dto.getExecutionId() == null) {
            dto.setExecutionId(dto.getReferenceLogId());
        }
        if (dto.getReferenceLogId() == null) {
            dto.setReferenceLogId(dto.getExecutionId());
        }
        if (dto.getExecutionId() == null || dto.getReferenceLogId() == null) {
            Page<SystemSchedulerJobLogPagedDto> logsPage = systemSchedulerFacadeService.getLogs(jobCode, 0, 1);
            if (logsPage != null && !logsPage.getContent().isEmpty()) {
                Long latestLogId = logsPage.getContent().get(0).getId();
                if (dto.getExecutionId() == null) {
                    dto.setExecutionId(latestLogId);
                }
                if (dto.getReferenceLogId() == null) {
                    dto.setReferenceLogId(latestLogId);
                }
            }
        }
        return dto;
    }

    private SystemSchedulerExecutionViewDto toExecutionView(SystemSchedulerJobLogPagedDto log) {
        SystemSchedulerExecutionViewDto dto = new SystemSchedulerExecutionViewDto();
        dto.setExecutionId(log.getId());
        dto.setReferenceLogId(log.getId());
        dto.setJobCode(log.getJobCode());
        dto.setStatus(log.getStatus());
        dto.setStartedAt(log.getStartedAt());
        dto.setCompletedAt(log.getFinishedAt());
        dto.setBatchId(log.getBatchId());

        if ("FAILED".equals(log.getStatus()) || "PARTIAL_FAILED".equals(log.getStatus())) {
            dto.setErrorMessage(buildErrorMessage(log));
            dto.setResultMessage(null);
        } else {
            dto.setResultMessage(log.getMessage());
            dto.setErrorMessage(null);
        }
        return dto;
    }

    private String buildErrorMessage(SystemSchedulerJobLogPagedDto log) {
        if (log.getFailedItems() == null || log.getFailedItems().isEmpty()) {
            return log.getMessage();
        }
        List<String> parts = new ArrayList<>();
        if (log.getMessage() != null && !log.getMessage().isBlank()) {
            parts.add(log.getMessage());
        }
        for (var item : log.getFailedItems()) {
            String ticker = item.getTicker() == null ? "-" : item.getTicker();
            String reason = item.getReason() == null ? "未知原因" : item.getReason();
            parts.add(ticker + ": " + reason);
        }
        return String.join(" | ", parts);
    }
}
