package com.example.helloworld.service.invest.systemscheduler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(transactionManager = "investTransactionManager", readOnly = true)
public class SystemSchedulerRuntimeGateService {

    private final SystemSchedulerFacadeService systemSchedulerFacadeService;

    public SystemSchedulerRuntimeGateService(SystemSchedulerFacadeService systemSchedulerFacadeService) {
        this.systemSchedulerFacadeService = systemSchedulerFacadeService;
    }

    public boolean isEnabled(SystemJobCode jobCode) {
        return systemSchedulerFacadeService.isEnabled(jobCode);
    }
}
