package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobUpsertRequestDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import com.example.helloworld.entity.invest.SystemScheduledJobConfig;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class SystemSchedulerFacadeService {

    private final Map<SystemJobCode, SystemJobHandler> handlerMap;
    private final SystemSchedulerJobConfigService systemSchedulerJobConfigService;

    public SystemSchedulerFacadeService(List<SystemJobHandler> handlers,
                                        SystemSchedulerJobConfigService systemSchedulerJobConfigService) {
        Map<SystemJobCode, SystemJobHandler> map = new EnumMap<>(SystemJobCode.class);
        for (SystemJobHandler handler : handlers) {
            map.put(handler.getJobCode(), handler);
        }
        this.handlerMap = Collections.unmodifiableMap(map);
        this.systemSchedulerJobConfigService = systemSchedulerJobConfigService;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemSchedulerJobDto> getJobs(boolean includeInactive) {
        Map<String, SystemScheduledJobConfig> configMap = systemSchedulerJobConfigService.findAllAsMap();
        return handlerMap.values().stream()
            .sorted(Comparator.comparingInt(SystemJobHandler::getDisplayOrder))
            .map(handler -> {
                SystemSchedulerJobDto dto = handler.buildJob();
                applyConfigOverride(dto, configMap.get(handler.getJobCode().name()));
                if (!includeInactive && Boolean.FALSE.equals(dto.getActive())) {
                    return null;
                }
                SystemSchedulerJobLogPagedDto latestLog = handler.getLatestLog();
                if (latestLog != null) {
                    dto.setLastRunAt(latestLog.getStartedAt());
                    dto.setLastRunStatus(latestLog.getStatus());
                    dto.setLastRunMessage(latestLog.getMessage());
                }
                return dto;
            })
            .filter(Objects::nonNull)
            .toList();
    }

    public SystemSchedulerRunNowResponseDto runNow(String jobCodeValue) {
        SystemJobHandler handler = resolveHandler(jobCodeValue);
        if (!systemSchedulerJobConfigService.isRunNowAllowed(handler.getJobCode())) {
            throw new RuntimeException("任務目前為停用狀態或不允許 Run Now");
        }
        return handler.runNow();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemSchedulerJobLogPagedDto> getLogs(String jobCodeValue, int page, int size) {
        SystemJobHandler handler = resolveHandler(jobCodeValue);
        return handler.getLogs(page, size);
    }

    public SystemSchedulerJobDto createJob(SystemSchedulerJobUpsertRequestDto request) {
        if (request == null || request.getJobCode() == null || request.getJobCode().isBlank()) {
            throw new RuntimeException("jobCode 不可為空");
        }
        SystemJobHandler handler = resolveHandler(request.getJobCode());
        SystemSchedulerJobDto defaultJob = handler.buildJob();
        systemSchedulerJobConfigService.create(handler.getJobCode(), request, defaultJob);
        return getJob(handler.getJobCode().name());
    }

    public SystemSchedulerJobDto updateJob(String jobCodeValue, SystemSchedulerJobUpsertRequestDto request) {
        SystemJobHandler handler = resolveHandler(jobCodeValue);
        SystemSchedulerJobDto defaultJob = handler.buildJob();
        systemSchedulerJobConfigService.update(handler.getJobCode(), request, defaultJob);
        return getJob(handler.getJobCode().name());
    }

    public SystemSchedulerJobDto toggleJob(String jobCodeValue, Boolean enabled) {
        SystemJobHandler handler = resolveHandler(jobCodeValue);
        SystemSchedulerJobDto defaultJob = handler.buildJob();
        systemSchedulerJobConfigService.toggle(handler.getJobCode(), enabled, defaultJob);
        return getJob(handler.getJobCode().name());
    }

    public void deleteJob(String jobCodeValue) {
        SystemJobHandler handler = resolveHandler(jobCodeValue);
        SystemSchedulerJobDto defaultJob = handler.buildJob();
        systemSchedulerJobConfigService.delete(handler.getJobCode(), defaultJob);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public SystemSchedulerJobDto getJob(String jobCodeValue) {
        SystemJobHandler handler = resolveHandler(jobCodeValue);
        SystemSchedulerJobDto dto = handler.buildJob();
        SystemScheduledJobConfig config = systemSchedulerJobConfigService
            .findByJobCode(handler.getJobCode())
            .orElse(null);
        applyConfigOverride(dto, config);
        SystemSchedulerJobLogPagedDto latestLog = handler.getLatestLog();
        if (latestLog != null) {
            dto.setLastRunAt(latestLog.getStartedAt());
            dto.setLastRunStatus(latestLog.getStatus());
            dto.setLastRunMessage(latestLog.getMessage());
        }
        return dto;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public boolean isEnabled(SystemJobCode jobCode) {
        return systemSchedulerJobConfigService.isEnabled(jobCode);
    }

    private SystemJobHandler resolveHandler(String jobCodeValue) {
        SystemJobCode jobCode = SystemJobCode.fromPathValue(jobCodeValue);
        SystemJobHandler handler = handlerMap.get(jobCode);
        if (handler == null) {
            throw new RuntimeException("找不到 job handler：" + jobCode.name());
        }
        return handler;
    }

    private void applyConfigOverride(SystemSchedulerJobDto dto, SystemScheduledJobConfig config) {
        if (config == null) {
            dto.setActive(true);
            dto.setAllowRunNow(dto.getAllowRunNow() == null || dto.getAllowRunNow());
            dto.setScheduleEditable(false);
            return;
        }

        dto.setConfigId(config.getId());
        dto.setJobName(config.getJobName());
        dto.setDescription(config.getDescription());
        dto.setEnabled(config.getEnabled());
        dto.setScheduleType(config.getScheduleType());
        dto.setScheduleExpression(config.getScheduleExpression());
        dto.setAllowRunNow(config.getAllowRunNow());
        dto.setActive(config.getIsActive());
        dto.setScheduleEditable(false);
    }
}
