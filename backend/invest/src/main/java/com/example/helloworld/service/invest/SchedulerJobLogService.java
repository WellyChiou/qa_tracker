package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.SchedulerJobLogPagedDto;
import com.example.helloworld.entity.invest.SchedulerJobLog;
import com.example.helloworld.entity.invest.SchedulerJobStatusCode;
import com.example.helloworld.repository.invest.SchedulerJobLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class SchedulerJobLogService {

    private final SchedulerJobLogRepository schedulerJobLogRepository;

    public SchedulerJobLogService(SchedulerJobLogRepository schedulerJobLogRepository) {
        this.schedulerJobLogRepository = schedulerJobLogRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<SchedulerJobLogPagedDto> getPaged(String jobName,
                                                  LocalDate runDateFrom,
                                                  LocalDate runDateTo,
                                                  String status,
                                                  int page,
                                                  int size) {
        SchedulerJobStatusCode statusCode = parseStatus(status);
        Pageable pageable = PageRequest.of(page, size);
        return schedulerJobLogRepository.findByFilters(jobName, runDateFrom, runDateTo, statusCode, pageable)
            .map(this::toDto);
    }

    private SchedulerJobLogPagedDto toDto(SchedulerJobLog log) {
        SchedulerJobLogPagedDto dto = new SchedulerJobLogPagedDto();
        dto.setId(log.getId());
        dto.setJobName(log.getJobName());
        dto.setRunDate(log.getRunDate());
        dto.setStatus(log.getStatus().name());
        dto.setStartedAt(log.getStartedAt());
        dto.setFinishedAt(log.getFinishedAt());
        dto.setMessage(log.getMessage());
        return dto;
    }

    private SchedulerJobStatusCode parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return SchedulerJobStatusCode.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("status 無效，請使用 RUNNING/SUCCESS/FAILED");
        }
    }
}
