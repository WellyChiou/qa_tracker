package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.StrengthFactorDto;
import com.example.helloworld.dto.invest.StrengthSnapshotDetailDto;
import com.example.helloworld.dto.invest.StrengthSnapshotPagedDto;
import com.example.helloworld.entity.invest.MarketUniverseTypeCode;
import com.example.helloworld.entity.invest.StrengthLevelCode;
import com.example.helloworld.entity.invest.StrengthSnapshot;
import com.example.helloworld.entity.invest.StrengthSnapshotFactor;
import com.example.helloworld.repository.invest.StrengthSnapshotFactorRepository;
import com.example.helloworld.repository.invest.StrengthSnapshotRepository;
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
public class StrengthSnapshotService {

    private static final String DISCLAIMER = "本系統僅提供觀察輔助，不構成買進建議，請務必自行判斷風險。";

    private final StrengthSnapshotRepository strengthSnapshotRepository;
    private final StrengthSnapshotFactorRepository strengthSnapshotFactorRepository;
    private final InvestCurrentUserService investCurrentUserService;

    public StrengthSnapshotService(StrengthSnapshotRepository strengthSnapshotRepository,
                                   StrengthSnapshotFactorRepository strengthSnapshotFactorRepository,
                                   InvestCurrentUserService investCurrentUserService) {
        this.strengthSnapshotRepository = strengthSnapshotRepository;
        this.strengthSnapshotFactorRepository = strengthSnapshotFactorRepository;
        this.investCurrentUserService = investCurrentUserService;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<StrengthSnapshotPagedDto> getPaged(LocalDate tradeDate,
                                                   String level,
                                                   String universeType,
                                                   Long watchlistId,
                                                   String ticker,
                                                   int page,
                                                   int size) {
        String userUid = investCurrentUserService.resolveCurrentUserUid();
        StrengthLevelCode levelCode = parseStrengthLevel(level);
        MarketUniverseTypeCode universeCode = parseUniverseType(universeType);

        Pageable pageable = PageRequest.of(page, size);
        Page<StrengthSnapshot> result = strengthSnapshotRepository.findPagedByUser(
            userUid,
            tradeDate,
            levelCode,
            universeCode,
            watchlistId,
            ticker,
            pageable
        );
        return result.map(this::toPagedDto);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public StrengthSnapshotDetailDto getById(Long id) {
        String userUid = investCurrentUserService.resolveCurrentUserUid();
        StrengthSnapshot snapshot = strengthSnapshotRepository.findByIdAndUserId(id, userUid)
            .orElseThrow(() -> new RuntimeException("找不到強勢分析資料或無權限，id=" + id));

        List<StrengthSnapshotFactor> factors = strengthSnapshotFactorRepository
            .findBySnapshotIdOrderByScoreContributionDescSortOrderAsc(snapshot.getId());

        return toDetailDto(snapshot, factors);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public StrengthSnapshotDetailDto getLatest(Long stockId, String universeType) {
        String userUid = investCurrentUserService.resolveCurrentUserUid();
        MarketUniverseTypeCode universeCode = parseUniverseType(universeType);

        List<StrengthSnapshot> snapshots = strengthSnapshotRepository.findLatestByUserAndStock(userUid, stockId, universeCode);
        if (snapshots.isEmpty()) {
            throw new RuntimeException("找不到最新強勢分析資料，stockId=" + stockId);
        }

        StrengthSnapshot snapshot = snapshots.get(0);
        List<StrengthSnapshotFactor> factors = strengthSnapshotFactorRepository
            .findBySnapshotIdOrderByScoreContributionDescSortOrderAsc(snapshot.getId());
        return toDetailDto(snapshot, factors);
    }

    private StrengthSnapshotPagedDto toPagedDto(StrengthSnapshot snapshot) {
        List<StrengthFactorDto> topFactors = strengthSnapshotFactorRepository
            .findBySnapshotIdOrderByScoreContributionDescSortOrderAsc(snapshot.getId())
            .stream()
            .sorted(Comparator
                .comparing(StrengthSnapshotFactor::getScoreContribution, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StrengthSnapshotFactor::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(3)
            .map(this::toFactorDto)
            .toList();

        StrengthSnapshotPagedDto dto = new StrengthSnapshotPagedDto();
        dto.setId(snapshot.getId());
        dto.setStockId(snapshot.getStock().getId());
        dto.setMarket(snapshot.getStock().getMarket());
        dto.setTicker(snapshot.getStock().getTicker());
        dto.setStockName(snapshot.getStock().getName());
        dto.setUniverseType(snapshot.getUniverseType().name());
        dto.setWatchlistId(snapshot.getWatchlist() == null ? null : snapshot.getWatchlist().getId());
        dto.setWatchlistName(snapshot.getWatchlist() == null ? null : snapshot.getWatchlist().getName());
        dto.setPriceClose(snapshot.getPriceClose());
        dto.setPriceTradeDate(snapshot.getPriceTradeDate());
        dto.setStrengthScore(snapshot.getStrengthScore());
        dto.setStrengthLevel(snapshot.getStrengthLevel() == null ? null : snapshot.getStrengthLevel().name());
        dto.setRecommendation(snapshot.getRecommendationCode() == null ? null : snapshot.getRecommendationCode().name());
        dto.setDataQuality(snapshot.getDataQuality());
        dto.setSummary(snapshot.getSummaryText());
        dto.setTradeDate(snapshot.getTradeDate());
        dto.setComputedAt(snapshot.getComputedAt());
        dto.setTopFactors(topFactors);
        return dto;
    }

    private StrengthSnapshotDetailDto toDetailDto(StrengthSnapshot snapshot, List<StrengthSnapshotFactor> factors) {
        StrengthSnapshotDetailDto dto = new StrengthSnapshotDetailDto();
        dto.setId(snapshot.getId());
        dto.setStockId(snapshot.getStock().getId());
        dto.setMarket(snapshot.getStock().getMarket());
        dto.setTicker(snapshot.getStock().getTicker());
        dto.setStockName(snapshot.getStock().getName());
        dto.setUniverseType(snapshot.getUniverseType().name());
        dto.setWatchlistId(snapshot.getWatchlist() == null ? null : snapshot.getWatchlist().getId());
        dto.setWatchlistName(snapshot.getWatchlist() == null ? null : snapshot.getWatchlist().getName());
        dto.setPriceClose(snapshot.getPriceClose());
        dto.setPriceTradeDate(snapshot.getPriceTradeDate());
        dto.setStrengthScore(snapshot.getStrengthScore());
        dto.setStrengthLevel(snapshot.getStrengthLevel() == null ? null : snapshot.getStrengthLevel().name());
        dto.setRecommendation(snapshot.getRecommendationCode() == null ? null : snapshot.getRecommendationCode().name());
        dto.setDataQuality(snapshot.getDataQuality());
        dto.setSummary(snapshot.getSummaryText());
        dto.setTradeDate(snapshot.getTradeDate());
        dto.setComputedAt(snapshot.getComputedAt());
        dto.setFactors(factors.stream().map(this::toFactorDto).toList());
        dto.setDisclaimer(DISCLAIMER);
        return dto;
    }

    private StrengthFactorDto toFactorDto(StrengthSnapshotFactor factor) {
        StrengthFactorDto dto = new StrengthFactorDto();
        dto.setFactorCode(factor.getFactorCode());
        dto.setFactorName(factor.getFactorName());
        dto.setRawValue(factor.getRawValueText());
        dto.setThreshold(factor.getThresholdText());
        dto.setScoreContribution(factor.getScoreContribution());
        dto.setExplanation(factor.getExplanationText());
        return dto;
    }

    private StrengthLevelCode parseStrengthLevel(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return StrengthLevelCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("strengthLevel 無效，請使用 STRONG/GOOD/NEUTRAL/WEAK");
        }
    }

    private MarketUniverseTypeCode parseUniverseType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return MarketUniverseTypeCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("universeType 無效，請使用 HOLDING/WATCHLIST");
        }
    }
}
