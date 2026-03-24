package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.PortfolioResponseDto;
import com.example.helloworld.dto.invest.PortfolioUpsertRequestDto;
import com.example.helloworld.entity.invest.Portfolio;
import com.example.helloworld.entity.invest.PortfolioRiskResult;
import com.example.helloworld.entity.invest.Stock;
import com.example.helloworld.entity.invest.StockPriceDaily;
import com.example.helloworld.repository.invest.PortfolioRepository;
import com.example.helloworld.repository.invest.PortfolioRiskResultRepository;
import com.example.helloworld.repository.invest.StockPriceDailyRepository;
import com.example.helloworld.repository.invest.StockRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class PortfolioService {

    private static final int MONEY_SCALE = 2;
    private static final int PRICE_SCALE = 4;

    private final PortfolioRepository portfolioRepository;
    private final StockRepository stockRepository;
    private final StockPriceDailyRepository stockPriceDailyRepository;
    private final StockPriceDailyService stockPriceDailyService;
    private final PortfolioRiskResultRepository portfolioRiskResultRepository;
    private final InvestCurrentUserService investCurrentUserService;
    private final InvestFxConversionService investFxConversionService;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            StockRepository stockRepository,
                            StockPriceDailyRepository stockPriceDailyRepository,
                            StockPriceDailyService stockPriceDailyService,
                            PortfolioRiskResultRepository portfolioRiskResultRepository,
                            InvestCurrentUserService investCurrentUserService,
                            InvestFxConversionService investFxConversionService) {
        this.portfolioRepository = portfolioRepository;
        this.stockRepository = stockRepository;
        this.stockPriceDailyRepository = stockPriceDailyRepository;
        this.stockPriceDailyService = stockPriceDailyService;
        this.portfolioRiskResultRepository = portfolioRiskResultRepository;
        this.investCurrentUserService = investCurrentUserService;
        this.investFxConversionService = investFxConversionService;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<PortfolioResponseDto> getPaged(String userId, Long stockId, Boolean isActive, int page, int size) {
        String effectiveUserId = resolveEffectiveUserId(userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Portfolio> portfolioPage = portfolioRepository.findByFilters(effectiveUserId, stockId, isActive, pageable);
        Map<Long, StockPriceDaily> latestPriceCache = new HashMap<>();
        Map<Long, PortfolioRiskResult> latestRiskMap = loadLatestRiskMap(portfolioPage.getContent());

        List<PortfolioResponseDto> content = portfolioPage.getContent().stream()
            .map(p -> toResponse(p, latestPriceCache, latestRiskMap, false))
            .toList();
        return new PageImpl<>(content, pageable, portfolioPage.getTotalElements());
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<PortfolioResponseDto> getAll(String userId, Long stockId, Boolean isActive) {
        String effectiveUserId = resolveEffectiveUserId(userId);

        Map<Long, StockPriceDaily> latestPriceCache = new HashMap<>();
        List<Portfolio> portfolios = portfolioRepository.findAllByFilters(effectiveUserId, stockId, isActive);
        Map<Long, PortfolioRiskResult> latestRiskMap = loadLatestRiskMap(portfolios);
        return portfolios.stream()
            .map(p -> toResponse(p, latestPriceCache, latestRiskMap, false))
            .toList();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public PortfolioResponseDto getById(Long id) {
        String currentUserId = resolveCurrentUserUid();
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(id, currentUserId)
            .orElseThrow(() -> new RuntimeException("找不到持股資料或無權限，id=" + id));

        Map<Long, PortfolioRiskResult> latestRiskMap = loadLatestRiskMap(List.of(portfolio));
        PortfolioResponseDto dto = toResponse(portfolio, new HashMap<>(), latestRiskMap, true);
        dto.setPriceHistory(stockPriceDailyService.getRecentHistoryByStockId(portfolio.getStock().getId()));
        return dto;
    }

    public PortfolioResponseDto create(PortfolioUpsertRequestDto request) {
        validateRequest(request);

        Stock stock = stockRepository.findById(request.getStockId())
            .orElseThrow(() -> new RuntimeException("找不到股票資料，id=" + request.getStockId()));

        Portfolio entity = new Portfolio();
        entity.setUserId(resolveRequestUserId(request.getUserId()));
        entity.setStock(stock);
        entity.setAvgCost(request.getAvgCost().setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        entity.setQuantity(request.getQuantity().setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        entity.setTotalCost(calculateTotalCost(entity.getAvgCost(), entity.getQuantity()));
        entity.setNote(normalize(request.getNote()));
        entity.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());

        return toResponse(portfolioRepository.save(entity), new HashMap<>(), Map.of(), false);
    }

    public PortfolioResponseDto update(Long id, PortfolioUpsertRequestDto request) {
        validateRequest(request);

        String currentUserId = resolveCurrentUserUid();
        Portfolio entity = portfolioRepository.findByIdAndUserId(id, currentUserId)
            .orElseThrow(() -> new RuntimeException("找不到持股資料或無權限，id=" + id));

        Stock stock = stockRepository.findById(request.getStockId())
            .orElseThrow(() -> new RuntimeException("找不到股票資料，id=" + request.getStockId()));

        entity.setUserId(resolveRequestUserId(request.getUserId()));
        entity.setStock(stock);
        entity.setAvgCost(request.getAvgCost().setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        entity.setQuantity(request.getQuantity().setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        entity.setTotalCost(calculateTotalCost(entity.getAvgCost(), entity.getQuantity()));
        entity.setNote(normalize(request.getNote()));
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }

        return toResponse(portfolioRepository.save(entity), new HashMap<>(), Map.of(), false);
    }

    public void delete(Long id) {
        String currentUserId = resolveCurrentUserUid();
        Portfolio entity = portfolioRepository.findByIdAndUserId(id, currentUserId)
            .orElseThrow(() -> new RuntimeException("找不到持股資料或無權限，id=" + id));
        entity.setIsActive(false);
        portfolioRepository.save(entity);
    }

    private void validateRequest(PortfolioUpsertRequestDto request) {
        if (request.getStockId() == null) {
            throw new RuntimeException("stockId 為必填");
        }
        if (request.getAvgCost() == null || request.getAvgCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("avgCost 必須大於 0");
        }
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("quantity 必須大於 0");
        }
    }

    private PortfolioResponseDto toResponse(Portfolio portfolio,
                                            Map<Long, StockPriceDaily> latestPriceCache,
                                            Map<Long, PortfolioRiskResult> latestRiskMap,
                                            boolean includeHistory) {
        Stock stock = portfolio.getStock();
        Long stockId = stock.getId();

        StockPriceDaily latestPrice = latestPriceCache.computeIfAbsent(stockId,
            id -> stockPriceDailyRepository.findTopByStockIdOrderByTradeDateDesc(id).orElse(null));

        boolean hasLatestClose = latestPrice != null;
        BigDecimal currentPrice = hasLatestClose ? latestPrice.getClosePrice() : portfolio.getAvgCost();
        currentPrice = currentPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP);

        BigDecimal marketValueInAssetCurrency = currentPrice
            .multiply(portfolio.getQuantity())
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        String assetCurrency = investFxConversionService.resolveAssetCurrencyByMarket(stock.getMarket());
        BigDecimal fxRateToBase = "USD".equals(assetCurrency)
            ? investFxConversionService.getUsdToTwdRate()
            : BigDecimal.ONE;
        BigDecimal marketValue = investFxConversionService
            .convertToBaseCurrency(marketValueInAssetCurrency, assetCurrency)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalCost = investFxConversionService
            .convertToBaseCurrency(portfolio.getTotalCost(), assetCurrency)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal pnl = marketValue.subtract(totalCost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal pnlPercent = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            pnlPercent = pnl.divide(totalCost, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        PortfolioResponseDto dto = new PortfolioResponseDto();
        dto.setId(portfolio.getId());
        dto.setUserId(portfolio.getUserId());
        dto.setStockId(stockId);
        dto.setMarket(stock.getMarket());
        dto.setTicker(stock.getTicker());
        dto.setStockName(stock.getName());
        dto.setAvgCost(portfolio.getAvgCost());
        dto.setQuantity(portfolio.getQuantity());
        dto.setTotalCost(totalCost);
        dto.setNote(portfolio.getNote());
        dto.setIsActive(portfolio.getIsActive());
        dto.setCreatedAt(portfolio.getCreatedAt());
        dto.setUpdatedAt(portfolio.getUpdatedAt());
        dto.setCurrentPrice(currentPrice);
        dto.setAssetCurrency(assetCurrency);
        dto.setBaseCurrency(investFxConversionService.getBaseCurrency());
        dto.setFxRateToBase(fxRateToBase.setScale(4, RoundingMode.HALF_UP));
        dto.setCurrentPriceTradeDate(latestPrice == null ? null : latestPrice.getTradeDate());
        // priceSource 是持股估值語意（LATEST_CLOSE/COST_FALLBACK），
        // 與 stock_price_daily.data_source（TWSE/TPEX/US_PROVIDER）不同。
        dto.setPriceSource(hasLatestClose ? "LATEST_CLOSE" : "COST_FALLBACK");
        dto.setCurrentPriceDataQuality(latestPrice == null ? null : latestPrice.getDataQuality());
        dto.setMarketValue(marketValue);
        dto.setUnrealizedProfitLoss(pnl);
        dto.setUnrealizedProfitLossPercent(pnlPercent);
        PortfolioRiskResult latestRisk = latestRiskMap.get(portfolio.getId());
        if (latestRisk != null) {
            dto.setLatestRiskScore(latestRisk.getRiskScore());
            dto.setLatestRiskLevel(latestRisk.getRiskLevel().name());
            dto.setLatestRecommendation(latestRisk.getRecommendation().name());
        } else {
            dto.setLatestRiskScore(null);
            dto.setLatestRiskLevel(null);
            dto.setLatestRecommendation(null);
        }

        if (!includeHistory) {
            dto.setPriceHistory(List.of());
        }

        return dto;
    }

    private Map<Long, PortfolioRiskResult> loadLatestRiskMap(List<Portfolio> portfolios) {
        if (portfolios == null || portfolios.isEmpty()) {
            return Map.of();
        }

        List<Long> portfolioIds = portfolios.stream().map(Portfolio::getId).toList();
        return portfolioRiskResultRepository.findLatestByPortfolioIds(portfolioIds)
            .stream()
            .collect(Collectors.toMap(r -> r.getPortfolio().getId(), r -> r, (a, b) -> a));
    }

    private BigDecimal calculateTotalCost(BigDecimal avgCost, BigDecimal quantity) {
        return avgCost.multiply(quantity).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String resolveEffectiveUserId(String userId) {
        String currentUserId = resolveCurrentUserUid();
        String normalized = normalize(userId);
        if (normalized != null && !normalized.equals(currentUserId)) {
            throw new RuntimeException("不可查詢其他使用者持股");
        }
        return currentUserId;
    }

    private String resolveRequestUserId(String requestedUserId) {
        String currentUserId = resolveCurrentUserUid();
        String normalized = normalize(requestedUserId);
        if (normalized != null && !normalized.equals(currentUserId)) {
            throw new RuntimeException("不可異動其他使用者持股");
        }
        return currentUserId;
    }

    private String resolveCurrentUserUid() {
        return investCurrentUserService.resolveCurrentUserUid();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
