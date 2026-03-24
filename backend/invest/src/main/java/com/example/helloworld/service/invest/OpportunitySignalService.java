package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.OpportunitySignalDetailDto;
import com.example.helloworld.dto.invest.OpportunitySignalPagedDto;
import com.example.helloworld.dto.invest.OpportunitySignalReasonDto;
import com.example.helloworld.entity.invest.OpportunitySignal;
import com.example.helloworld.entity.invest.OpportunitySignalReason;
import com.example.helloworld.entity.invest.OpportunitySignalStatusCode;
import com.example.helloworld.entity.invest.OpportunitySignalTypeCode;
import com.example.helloworld.entity.invest.StrengthRecommendationCode;
import com.example.helloworld.repository.invest.OpportunitySignalReasonRepository;
import com.example.helloworld.repository.invest.OpportunitySignalRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class OpportunitySignalService {

    private final OpportunitySignalRepository opportunitySignalRepository;
    private final OpportunitySignalReasonRepository opportunitySignalReasonRepository;
    private final InvestCurrentUserService investCurrentUserService;

    public OpportunitySignalService(OpportunitySignalRepository opportunitySignalRepository,
                                    OpportunitySignalReasonRepository opportunitySignalReasonRepository,
                                    InvestCurrentUserService investCurrentUserService) {
        this.opportunitySignalRepository = opportunitySignalRepository;
        this.opportunitySignalReasonRepository = opportunitySignalReasonRepository;
        this.investCurrentUserService = investCurrentUserService;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<OpportunitySignalPagedDto> getPaged(LocalDate tradeDate,
                                                    String status,
                                                    String recommendation,
                                                    String signalType,
                                                    String ticker,
                                                    int page,
                                                    int size) {
        String userUid = investCurrentUserService.resolveCurrentUserUid();
        OpportunitySignalStatusCode statusCode = parseStatus(status);
        StrengthRecommendationCode recommendationCode = parseRecommendation(recommendation);
        OpportunitySignalTypeCode signalTypeCode = parseSignalType(signalType);

        Pageable pageable = PageRequest.of(page, size);
        Page<OpportunitySignal> result = opportunitySignalRepository.findPagedByUser(
            userUid,
            tradeDate,
            statusCode,
            recommendationCode,
            signalTypeCode,
            ticker,
            pageable
        );

        return result.map(this::toPagedDto);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public OpportunitySignalDetailDto getById(Long id) {
        String userUid = investCurrentUserService.resolveCurrentUserUid();
        OpportunitySignal signal = opportunitySignalRepository.findByIdAndUserId(id, userUid)
            .orElseThrow(() -> new RuntimeException("找不到機會訊號或無權限，id=" + id));

        List<OpportunitySignalReason> reasons = opportunitySignalReasonRepository
            .findBySignalIdOrderByScoreImpactDescSortOrderAsc(signal.getId());
        return toDetailDto(signal, reasons);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public OpportunitySignalDetailDto getLatest(Long stockId) {
        String userUid = investCurrentUserService.resolveCurrentUserUid();
        List<OpportunitySignal> signals = opportunitySignalRepository.findLatestByUserAndStock(userUid, stockId);
        if (signals.isEmpty()) {
            throw new RuntimeException("找不到最新機會訊號，stockId=" + stockId);
        }

        OpportunitySignal signal = signals.get(0);
        List<OpportunitySignalReason> reasons = opportunitySignalReasonRepository
            .findBySignalIdOrderByScoreImpactDescSortOrderAsc(signal.getId());
        return toDetailDto(signal, reasons);
    }

    private OpportunitySignalPagedDto toPagedDto(OpportunitySignal signal) {
        List<OpportunitySignalReasonDto> topReasons = opportunitySignalReasonRepository
            .findBySignalIdOrderByScoreImpactDescSortOrderAsc(signal.getId())
            .stream()
            .sorted(Comparator
                .comparing(OpportunitySignalReason::getScoreImpact, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(OpportunitySignalReason::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(3)
            .map(this::toReasonDto)
            .toList();

        OpportunitySignalPagedDto dto = new OpportunitySignalPagedDto();
        dto.setId(signal.getId());
        dto.setStockId(signal.getStock().getId());
        dto.setMarket(signal.getStock().getMarket());
        dto.setTicker(signal.getStock().getTicker());
        dto.setStockName(signal.getStock().getName());
        dto.setTradeDate(signal.getTradeDate());
        dto.setSignalKey(signal.getSignalKey());
        dto.setSignalType(signal.getSignalType().name());
        dto.setSignalScore(signal.getSignalScore());
        dto.setRecommendation(signal.getRecommendationCode().name());
        dto.setStatus(signal.getStatus().name());
        dto.setStrategyVersion(signal.getStrategyVersion());
        dto.setSummary(signal.getSummaryText());
        dto.setConditionText(signal.getConditionText());
        dto.setUpdatedAt(signal.getUpdatedAt());
        dto.setTopReasons(topReasons);
        return dto;
    }

    private OpportunitySignalDetailDto toDetailDto(OpportunitySignal signal, List<OpportunitySignalReason> reasons) {
        OpportunitySignalDetailDto dto = new OpportunitySignalDetailDto();
        dto.setId(signal.getId());
        dto.setStockId(signal.getStock().getId());
        dto.setMarket(signal.getStock().getMarket());
        dto.setTicker(signal.getStock().getTicker());
        dto.setStockName(signal.getStock().getName());
        dto.setTradeDate(signal.getTradeDate());
        dto.setSignalKey(signal.getSignalKey());
        dto.setSignalType(signal.getSignalType().name());
        dto.setSignalScore(signal.getSignalScore());
        dto.setRecommendation(signal.getRecommendationCode().name());
        dto.setStatus(signal.getStatus().name());
        dto.setStrategyVersion(signal.getStrategyVersion());
        dto.setSummary(signal.getSummaryText());
        dto.setConditionText(signal.getConditionText());
        dto.setDisclaimer(signal.getDisclaimerText());
        dto.setCreatedAt(signal.getCreatedAt());
        dto.setUpdatedAt(signal.getUpdatedAt());
        dto.setReasons(reasons.stream().map(this::toReasonDto).toList());
        return dto;
    }

    private OpportunitySignalReasonDto toReasonDto(OpportunitySignalReason reason) {
        OpportunitySignalReasonDto dto = new OpportunitySignalReasonDto();
        dto.setReasonTitle(reason.getReasonTitle());
        dto.setReasonDetail(reason.getReasonDetail());
        dto.setScoreImpact(reason.getScoreImpact());
        return dto;
    }

    private OpportunitySignalStatusCode parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OpportunitySignalStatusCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("status 無效，請使用 ACTIVE/EXPIRED");
        }
    }

    private StrengthRecommendationCode parseRecommendation(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return StrengthRecommendationCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("recommendation 無效");
        }
    }

    private OpportunitySignalTypeCode parseSignalType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OpportunitySignalTypeCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("signalType 無效");
        }
    }
}
