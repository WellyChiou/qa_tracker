package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobUpsertRequestDto;
import com.example.helloworld.entity.invest.SystemScheduledJobConfig;
import com.example.helloworld.repository.invest.SystemScheduledJobConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class SystemSchedulerJobConfigService {

    private final SystemScheduledJobConfigRepository systemScheduledJobConfigRepository;

    public SystemSchedulerJobConfigService(SystemScheduledJobConfigRepository systemScheduledJobConfigRepository) {
        this.systemScheduledJobConfigRepository = systemScheduledJobConfigRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Map<String, SystemScheduledJobConfig> findAllAsMap() {
        Map<String, SystemScheduledJobConfig> map = new HashMap<>();
        for (SystemScheduledJobConfig config : systemScheduledJobConfigRepository.findAllByOrderByUpdatedAtDesc()) {
            map.put(config.getJobCode(), config);
        }
        return map;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Optional<SystemScheduledJobConfig> findByJobCode(SystemJobCode jobCode) {
        return systemScheduledJobConfigRepository.findByJobCode(jobCode.name());
    }

    public SystemScheduledJobConfig create(SystemJobCode jobCode,
                                           SystemSchedulerJobUpsertRequestDto request,
                                           SystemSchedulerJobDto defaultJob) {
        Optional<SystemScheduledJobConfig> existingOpt = findByJobCode(jobCode);
        if (existingOpt.isPresent() && Boolean.TRUE.equals(existingOpt.get().getIsActive())) {
            throw new RuntimeException("任務已存在，請改用編輯");
        }

        SystemScheduledJobConfig entity = existingOpt.orElseGet(SystemScheduledJobConfig::new);
        applyDefault(entity, jobCode, defaultJob);
        applyRequest(entity, request, defaultJob);
        entity.setIsActive(true);
        return systemScheduledJobConfigRepository.save(entity);
    }

    public SystemScheduledJobConfig update(SystemJobCode jobCode,
                                           SystemSchedulerJobUpsertRequestDto request,
                                           SystemSchedulerJobDto defaultJob) {
        SystemScheduledJobConfig entity = getOrCreate(jobCode, defaultJob);
        if (Boolean.FALSE.equals(entity.getIsActive())) {
            throw new RuntimeException("任務已刪除，請先新增後再編輯");
        }
        applyRequest(entity, request, defaultJob);
        return systemScheduledJobConfigRepository.save(entity);
    }

    public SystemScheduledJobConfig toggle(SystemJobCode jobCode, Boolean enabled, SystemSchedulerJobDto defaultJob) {
        if (enabled == null) {
            throw new RuntimeException("enabled 不可為空");
        }
        SystemScheduledJobConfig entity = getOrCreate(jobCode, defaultJob);
        if (Boolean.FALSE.equals(entity.getIsActive())) {
            throw new RuntimeException("任務已刪除，請先新增後再啟用/停用");
        }
        entity.setEnabled(enabled);
        return systemScheduledJobConfigRepository.save(entity);
    }

    public void delete(SystemJobCode jobCode, SystemSchedulerJobDto defaultJob) {
        SystemScheduledJobConfig entity = getOrCreate(jobCode, defaultJob);
        entity.setIsActive(false);
        entity.setEnabled(false);
        systemScheduledJobConfigRepository.save(entity);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public boolean isEnabled(SystemJobCode jobCode) {
        return findByJobCode(jobCode)
            .map(c -> Boolean.TRUE.equals(c.getIsActive()) && Boolean.TRUE.equals(c.getEnabled()))
            .orElse(true);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public boolean isRunNowAllowed(SystemJobCode jobCode) {
        return findByJobCode(jobCode)
            .map(c -> Boolean.TRUE.equals(c.getIsActive())
                && Boolean.TRUE.equals(c.getEnabled())
                && Boolean.TRUE.equals(c.getAllowRunNow()))
            .orElse(true);
    }

    private SystemScheduledJobConfig getOrCreate(SystemJobCode jobCode, SystemSchedulerJobDto defaultJob) {
        return findByJobCode(jobCode).orElseGet(() -> {
            SystemScheduledJobConfig created = new SystemScheduledJobConfig();
            applyDefault(created, jobCode, defaultJob);
            return created;
        });
    }

    private void applyDefault(SystemScheduledJobConfig entity, SystemJobCode jobCode, SystemSchedulerJobDto defaultJob) {
        entity.setJobCode(jobCode.name());
        entity.setJobName(defaultJob.getJobName());
        entity.setDescription(defaultJob.getDescription());
        entity.setEnabled(defaultJob.getEnabled() == null || defaultJob.getEnabled());
        entity.setScheduleType(defaultJob.getScheduleType());
        entity.setScheduleExpression(defaultJob.getScheduleExpression());
        entity.setAllowRunNow(defaultJob.getAllowRunNow() == null || defaultJob.getAllowRunNow());
        entity.setIsActive(true);
    }

    private void applyRequest(SystemScheduledJobConfig entity,
                              SystemSchedulerJobUpsertRequestDto request,
                              SystemSchedulerJobDto defaultJob) {
        if (request == null) {
            return;
        }

        if (StringUtils.hasText(request.getJobName())) {
            entity.setJobName(request.getJobName().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription().trim());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        if (request.getAllowRunNow() != null) {
            entity.setAllowRunNow(request.getAllowRunNow());
        }

        // 第一版延續 adapter+facade，不做動態 cron 引擎。
        // scheduleExpression 允許保存管理值，但對 @Scheduled 不會即時改變觸發時間。
        if (request.getScheduleExpression() != null) {
            entity.setScheduleExpression(request.getScheduleExpression().trim());
        }

        if (!StringUtils.hasText(entity.getScheduleType())) {
            entity.setScheduleType(defaultJob.getScheduleType());
        }
    }
}
