package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.personal.JobExecution;
import com.example.helloworld.entity.personal.ScheduledJob;
import com.example.helloworld.service.personal.ScheduledJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/personal/scheduled-jobs")
@CrossOrigin(origins = "*")
public class ScheduledJobController {

    @Autowired
    private ScheduledJobService scheduledJobService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduledJob>>> getAllJobs() {
        return ResponseEntity.ok(ApiResponse.ok(scheduledJobService.getAllJobs()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduledJob>> getJobById(@PathVariable Long id) {
        Optional<ScheduledJob> job = scheduledJobService.getJobById(id);
        return job.map(j -> ResponseEntity.ok(ApiResponse.ok(j)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("任務不存在")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledJob>> createJob(@RequestBody ScheduledJob job) {
        try {
            ScheduledJob created = scheduledJobService.createJob(job);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduledJob>> updateJob(@PathVariable Long id, @RequestBody ScheduledJob job) {
        try {
            ScheduledJob updated = scheduledJobService.updateJob(id, job);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        try {
            scheduledJobService.deleteJob(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeJob(@PathVariable Long id) {
        try {
            Long executionId = scheduledJobService.executeJob(id);
            Map<String, Object> data = new HashMap<>();
            data.put("message", "Job 執行中");
            data.put("executionId", executionId);
            return ResponseEntity.ok(ApiResponse.ok(data));
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
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("執行記錄不存在")));
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ApiResponse<JobExecution>> getExecutionById(@PathVariable Long executionId) {
        Optional<JobExecution> execution = scheduledJobService.getExecutionById(executionId);
        return execution.map(e -> ResponseEntity.ok(ApiResponse.ok(e)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("執行記錄不存在")));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<ScheduledJob>> toggleJob(@PathVariable Long id, @RequestParam Boolean enabled) {
        try {
            ScheduledJob job = scheduledJobService.toggleJob(id, enabled);
            return ResponseEntity.ok(ApiResponse.ok(job));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
