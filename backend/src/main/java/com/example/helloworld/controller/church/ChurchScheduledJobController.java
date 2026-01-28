package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.church.JobExecution;
import com.example.helloworld.entity.church.ScheduledJob;
import com.example.helloworld.service.church.ChurchScheduledJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/scheduled-jobs")
@CrossOrigin(origins = "*")
public class ChurchScheduledJobController {

    @Autowired
    private ChurchScheduledJobService scheduledJobService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduledJob>>> getAllJobs() {
        return ResponseEntity.ok(ApiResponse.ok(scheduledJobService.getAllJobs()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduledJob>> getJobById(@PathVariable Long id) {
        Optional<ScheduledJob> job = scheduledJobService.getJobById(id);
        return job.map(j -> ResponseEntity.ok(ApiResponse.ok(j)))
                .orElse(ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到指定的任務")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledJob>> createJob(@RequestBody ScheduledJob job) {
        try {
            ScheduledJob created = scheduledJobService.createJob(job);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建任務失敗: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduledJob>> updateJob(@PathVariable Long id, @RequestBody ScheduledJob job) {
        try {
            ScheduledJob updated = scheduledJobService.updateJob(id, job);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新任務失敗: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        try {
            scheduledJobService.deleteJob(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除任務失敗: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeJob(@PathVariable Long id) {
        try {
            Long executionId = scheduledJobService.executeJob(id);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Job 執行中");
            result.put("executionId", executionId);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<ApiResponse<List<JobExecution>>> getJobExecutions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(scheduledJobService.getJobExecutions(id)));
    }

    @GetMapping("/{id}/executions/latest")
    public ResponseEntity<ApiResponse<JobExecution>> getLatestExecution(@PathVariable Long id) {
        Optional<JobExecution> execution = scheduledJobService.getLatestExecution(id);
        return execution.map(e -> ResponseEntity.ok(ApiResponse.ok(e)))
                .orElse(ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到執行記錄")));
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ApiResponse<JobExecution>> getExecutionById(@PathVariable Long executionId) {
        Optional<JobExecution> execution = scheduledJobService.getExecutionById(executionId);
        return execution.map(e -> ResponseEntity.ok(ApiResponse.ok(e)))
                .orElse(ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到執行記錄")));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<ScheduledJob>> toggleJob(@PathVariable Long id, @RequestParam Boolean enabled) {
        try {
            ScheduledJob job = scheduledJobService.toggleJob(id, enabled);
            return ResponseEntity.ok(ApiResponse.ok(job));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("切換任務狀態失敗: " + e.getMessage()));
        }
    }
}

