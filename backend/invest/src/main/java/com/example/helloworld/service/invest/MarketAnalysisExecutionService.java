package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.RunMarketAnalysisResponseDto;
import com.example.helloworld.entity.invest.SchedulerJobLog;
import com.example.helloworld.entity.invest.SchedulerJobStatusCode;
import com.example.helloworld.repository.invest.SchedulerJobLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class MarketAnalysisExecutionService {

    public static final String JOB_NAME_MARKET_ANALYSIS = "MARKET_ANALYSIS";
    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");

    private final MarketAnalysisService marketAnalysisService;
    private final SchedulerJobLogRepository schedulerJobLogRepository;

    public MarketAnalysisExecutionService(MarketAnalysisService marketAnalysisService,
                                          SchedulerJobLogRepository schedulerJobLogRepository) {
        this.marketAnalysisService = marketAnalysisService;
        this.schedulerJobLogRepository = schedulerJobLogRepository;
    }

    public RunMarketAnalysisResponseDto runForCurrentUserWithLog(String scope, String triggerDescription) {
        LocalDateTime now = LocalDateTime.now(TAIPEI_ZONE);
        SchedulerJobLog log = new SchedulerJobLog();
        log.setJobName(JOB_NAME_MARKET_ANALYSIS);
        log.setRunDate(LocalDate.now(TAIPEI_ZONE));
        log.setStatus(SchedulerJobStatusCode.RUNNING);
        log.setStartedAt(now);
        log.setMessage("市場分析執行中：" + triggerDescription);
        log = schedulerJobLogRepository.save(log);

        try {
            RunMarketAnalysisResponseDto result = marketAnalysisService.runForCurrentUser(scope);
            log.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
            log.setStatus(resolveLogStatus(result.getStatus()));
            log.setMessage(String.format(
                "市場分析完成（%s）。狀態=%s，目標=%d，完成=%d，失敗=%d。%s",
                triggerDescription,
                nvl(result.getStatus()),
                nvlInt(result.getTargetCount()),
                nvlInt(result.getAnalyzedCount()),
                nvlInt(result.getFailCount()),
                nvl(result.getMessage())
            ));
            schedulerJobLogRepository.save(log);
            return result;
        } catch (Exception e) {
            log.setFinishedAt(LocalDateTime.now(TAIPEI_ZONE));
            log.setStatus(SchedulerJobStatusCode.FAILED);
            log.setMessage("市場分析失敗（" + triggerDescription + "）：" + nvl(e.getMessage()));
            schedulerJobLogRepository.save(log);
            throw e;
        }
    }

    private SchedulerJobStatusCode resolveLogStatus(String status) {
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return SchedulerJobStatusCode.SUCCESS;
        }
        return SchedulerJobStatusCode.FAILED;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private int nvlInt(Integer value) {
        return value == null ? 0 : value;
    }
}
