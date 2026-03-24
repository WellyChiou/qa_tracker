package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.DailyReportDetailDto;
import com.example.helloworld.dto.invest.DailyReportPagedDto;
import com.example.helloworld.dto.invest.DailyReportSummaryDto;
import com.example.helloworld.entity.invest.DailyReport;
import com.example.helloworld.entity.invest.DailyReportStatusCode;
import com.example.helloworld.entity.invest.DailyReportTypeCode;
import com.example.helloworld.repository.invest.DailyReportRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class DailyReportService {

    private final DailyReportRepository dailyReportRepository;
    private final InvestCurrentUserService investCurrentUserService;
    private final ObjectMapper objectMapper;

    public DailyReportService(DailyReportRepository dailyReportRepository,
                              InvestCurrentUserService investCurrentUserService,
                              ObjectMapper objectMapper) {
        this.dailyReportRepository = dailyReportRepository;
        this.investCurrentUserService = investCurrentUserService;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<DailyReportPagedDto> getPaged(LocalDate reportDateFrom,
                                              LocalDate reportDateTo,
                                              String reportType,
                                              String status,
                                              int page,
                                              int size) {
        String userId = resolveCurrentUserUid();
        DailyReportTypeCode typeCode = parseReportType(reportType);
        DailyReportStatusCode statusCode = parseStatus(status);

        Pageable pageable = PageRequest.of(page, size);
        Page<DailyReport> result = dailyReportRepository.findByUserAndFilters(
            userId, reportDateFrom, reportDateTo, typeCode, statusCode, pageable
        );

        return result.map(this::toPagedDto);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public DailyReportDetailDto getById(Long id) {
        String userId = resolveCurrentUserUid();
        DailyReport report = dailyReportRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new RuntimeException("找不到每日報告或無權限，id=" + id));
        return toDetailDto(report);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public DailyReportDetailDto getLatest(String reportType) {
        String userId = resolveCurrentUserUid();
        DailyReportTypeCode parsedTypeCode = parseReportType(reportType);
        final DailyReportTypeCode typeCode = parsedTypeCode == null
            ? DailyReportTypeCode.PORTFOLIO_RISK_DAILY
            : parsedTypeCode;

        DailyReport report = dailyReportRepository.findTopByUserIdAndReportTypeOrderByReportDateDescCreatedAtDesc(userId, typeCode)
            .orElse(null);
        if (report == null) {
            return null;
        }
        return toDetailDto(report);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public DailyReport findLatestEntityByCurrentUser(DailyReportTypeCode reportType) {
        return dailyReportRepository
            .findTopByUserIdAndReportTypeOrderByReportDateDescCreatedAtDesc(resolveCurrentUserUid(), reportType)
            .orElse(null);
    }

    public DailyReportSummaryDto parseSummary(String summaryJson) {
        if (!StringUtils.hasText(summaryJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(summaryJson, DailyReportSummaryDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析每日報告摘要失敗：" + e.getMessage(), e);
        }
    }

    private DailyReportPagedDto toPagedDto(DailyReport report) {
        DailyReportSummaryDto summary = parseSummary(report.getSummaryJson());
        DailyReportPagedDto dto = new DailyReportPagedDto();
        dto.setId(report.getId());
        dto.setReportDate(report.getReportDate());
        dto.setReportType(report.getReportType().name());
        dto.setStatus(report.getStatus().name());
        dto.setCreatedAt(report.getCreatedAt());
        if (summary != null) {
            dto.setDataAsOfTradeDate(summary.getDataAsOfTradeDate());
            dto.setHoldingCount(summary.getHoldingCount());
            dto.setHighRiskCount(summary.getHighRiskCount());
            dto.setCriticalRiskCount(summary.getCriticalRiskCount());
        }
        return dto;
    }

    private DailyReportDetailDto toDetailDto(DailyReport report) {
        DailyReportDetailDto dto = new DailyReportDetailDto();
        dto.setId(report.getId());
        dto.setUserId(report.getUserId());
        dto.setReportDate(report.getReportDate());
        dto.setReportType(report.getReportType().name());
        dto.setStatus(report.getStatus().name());
        dto.setSummary(parseSummary(report.getSummaryJson()));
        dto.setCreatedAt(report.getCreatedAt());
        return dto;
    }

    private DailyReportTypeCode parseReportType(String reportType) {
        if (!StringUtils.hasText(reportType)) {
            return null;
        }
        try {
            return DailyReportTypeCode.valueOf(reportType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("reportType 無效，請使用 PORTFOLIO_RISK_DAILY");
        }
    }

    private DailyReportStatusCode parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return DailyReportStatusCode.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("status 無效，請使用 SUCCESS/FAILED");
        }
    }

    private String resolveCurrentUserUid() {
        return investCurrentUserService.resolveCurrentUserUid();
    }
}
