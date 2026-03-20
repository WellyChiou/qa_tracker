package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.DashboardAlertSummaryDto;
import com.example.helloworld.dto.invest.PortfolioAlertEventPagedDto;
import com.example.helloworld.entity.invest.AlertTriggerTypeCode;
import com.example.helloworld.entity.invest.PortfolioAlertEvent;
import com.example.helloworld.repository.invest.PortfolioAlertEventRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class PortfolioAlertEventService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Taipei");
    private static final List<AlertTriggerTypeCode> HIGH_SEVERITY_TYPES = List.of(
        AlertTriggerTypeCode.STOP_LOSS,
        AlertTriggerTypeCode.ABNORMAL_DROP
    );

    private final PortfolioAlertEventRepository portfolioAlertEventRepository;
    private final InvestCurrentUserService investCurrentUserService;

    public PortfolioAlertEventService(PortfolioAlertEventRepository portfolioAlertEventRepository,
                                      InvestCurrentUserService investCurrentUserService) {
        this.portfolioAlertEventRepository = portfolioAlertEventRepository;
        this.investCurrentUserService = investCurrentUserService;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<PortfolioAlertEventPagedDto> getPaged(Long portfolioId,
                                                      String triggerType,
                                                      LocalDateTime triggeredFrom,
                                                      LocalDateTime triggeredTo,
                                                      int page,
                                                      int size) {
        AlertTriggerTypeCode typeCode = parseTriggerType(triggerType);
        Pageable pageable = PageRequest.of(page, size);
        String userId = resolveCurrentUserUid();

        return portfolioAlertEventRepository.findByUserAndFilters(
            userId, portfolioId, typeCode, triggeredFrom, triggeredTo, pageable
        ).map(this::toDto);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<PortfolioAlertEventPagedDto> getLatest(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        Pageable pageable = PageRequest.of(0, safeLimit);
        return portfolioAlertEventRepository.findLatestByUserId(resolveCurrentUserUid(), pageable)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public DashboardAlertSummaryDto buildDashboardSummaryForCurrentUser() {
        String userId = resolveCurrentUserUid();
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        Long totalActiveAlerts = defaultZero(
            portfolioAlertEventRepository.countByUserIdAndTriggeredAtBetween(userId, start, end)
        );
        Long highSeverityCount = defaultZero(
            portfolioAlertEventRepository.countByUserIdAndTriggerTypesAndTriggeredAtBetween(
                userId, HIGH_SEVERITY_TYPES, start, end
            )
        );
        LocalDateTime latestTriggeredAt = portfolioAlertEventRepository.findLatestTriggeredAtByUserId(userId);

        DashboardAlertSummaryDto dto = new DashboardAlertSummaryDto();
        dto.setTotalActiveAlerts(totalActiveAlerts);
        dto.setHighSeverityCount(highSeverityCount);
        dto.setLatestTriggeredAt(latestTriggeredAt);
        return dto;
    }

    public boolean isHighSeverity(AlertTriggerTypeCode triggerType) {
        return triggerType == AlertTriggerTypeCode.STOP_LOSS || triggerType == AlertTriggerTypeCode.ABNORMAL_DROP;
    }

    private Long defaultZero(Long value) {
        return value == null ? 0L : value;
    }

    private PortfolioAlertEventPagedDto toDto(PortfolioAlertEvent event) {
        PortfolioAlertEventPagedDto dto = new PortfolioAlertEventPagedDto();
        dto.setId(event.getId());
        dto.setPortfolioId(event.getPortfolio().getId());
        dto.setTicker(event.getPortfolio().getStock().getTicker());
        dto.setStockName(event.getPortfolio().getStock().getName());
        dto.setTriggerType(event.getTriggerType().name());
        dto.setSeverity(isHighSeverity(event.getTriggerType()) ? "HIGH" : "NORMAL");
        dto.setTriggerValue(event.getTriggerValue());
        dto.setMessage(event.getMessage());
        dto.setTriggeredAt(event.getTriggeredAt());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }

    private AlertTriggerTypeCode parseTriggerType(String triggerType) {
        if (!StringUtils.hasText(triggerType)) {
            return null;
        }
        try {
            return AlertTriggerTypeCode.valueOf(triggerType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("triggerType 無效，請使用 STOP_LOSS / DROP_PERCENT / ABNORMAL_DROP");
        }
    }

    private String resolveCurrentUserUid() {
        return investCurrentUserService.resolveCurrentUserUid();
    }
}
