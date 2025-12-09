package com.example.helloworld.controller.church;

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
    public ResponseEntity<List<ScheduledJob>> getAllJobs() {
        return ResponseEntity.ok(scheduledJobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledJob> getJobById(@PathVariable Long id) {
        Optional<ScheduledJob> job = scheduledJobService.getJobById(id);
        return job.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ScheduledJob> createJob(@RequestBody ScheduledJob job) {
        try {
            ScheduledJob created = scheduledJobService.createJob(job);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledJob> updateJob(@PathVariable Long id, @RequestBody ScheduledJob job) {
        try {
            ScheduledJob updated = scheduledJobService.updateJob(id, job);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        try {
            scheduledJobService.deleteJob(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeJob(@PathVariable Long id) {
        try {
            Long executionId = scheduledJobService.executeJob(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Job 執行中");
            response.put("executionId", executionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<JobExecution>> getJobExecutions(@PathVariable Long id) {
        return ResponseEntity.ok(scheduledJobService.getJobExecutions(id));
    }

    @GetMapping("/{id}/executions/latest")
    public ResponseEntity<JobExecution> getLatestExecution(@PathVariable Long id) {
        Optional<JobExecution> execution = scheduledJobService.getLatestExecution(id);
        return execution.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<JobExecution> getExecutionById(@PathVariable Long executionId) {
        Optional<JobExecution> execution = scheduledJobService.getExecutionById(executionId);
        return execution.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ScheduledJob> toggleJob(@PathVariable Long id, @RequestParam Boolean enabled) {
        try {
            ScheduledJob job = scheduledJobService.toggleJob(id, enabled);
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

