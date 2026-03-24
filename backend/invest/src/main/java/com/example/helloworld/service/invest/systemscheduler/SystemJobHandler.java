package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.SystemSchedulerJobDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.dto.invest.SystemSchedulerRunNowResponseDto;
import org.springframework.data.domain.Page;

public interface SystemJobHandler {

    SystemJobCode getJobCode();

    int getDisplayOrder();

    SystemSchedulerJobDto buildJob();

    SystemSchedulerJobLogPagedDto getLatestLog();

    Page<SystemSchedulerJobLogPagedDto> getLogs(int page, int size);

    SystemSchedulerRunNowResponseDto runNow();
}
