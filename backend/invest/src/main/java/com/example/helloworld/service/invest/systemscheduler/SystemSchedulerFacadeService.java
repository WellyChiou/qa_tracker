package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class SystemSchedulerFacadeService {

    private final Map<SystemJobCode, SystemJobHandler> handlerMap;

    public SystemSchedulerFacadeService(List<SystemJobHandler> handlers) {
        Map<SystemJobCode, SystemJobHandler> map = new EnumMap<>(SystemJobCode.class);
        for (SystemJobHandler handler : handlers) {
            map.put(handler.getJobCode(), handler);
        }
        this.handlerMap = Collections.unmodifiableMap(map);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<SystemSchedulerJobDto> getJobs() {
        return handlerMap.values().stream()
            .sorted(Comparator.comparingInt(SystemJobHandler::getDisplayOrder))
            .map(handler -> {
                SystemSchedulerJobDto dto = handler.buildJob();
                SystemSchedulerJobLogPagedDto latestLog = handler.getLatestLog();
                if (latestLog != null) {
                    dto.setLastRunAt(latestLog.getStartedAt());
                    dto.setLastRunStatus(latestLog.getStatus());
                    dto.setLastRunMessage(latestLog.getMessage());
                }
                return dto;
            })
            .toList();
    }

    public SystemSchedulerRunNowResponseDto runNow(String jobCodeValue) {
        SystemJobHandler handler = resolveHandler(jobCodeValue);
        return handler.runNow();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SystemSchedulerJobLogPagedDto> getLogs(String jobCodeValue, int page, int size) {
        SystemJobHandler handler = resolveHandler(jobCodeValue);
        return handler.getLogs(page, size);
    }

    private SystemJobHandler resolveHandler(String jobCodeValue) {
        SystemJobCode jobCode = SystemJobCode.fromPathValue(jobCodeValue);
        SystemJobHandler handler = handlerMap.get(jobCode);
        if (handler == null) {
            throw new RuntimeException("找不到 job handler：" + jobCode.name());
        }
        return handler;
    }
}
