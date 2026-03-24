package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.DashboardAlertSummaryDto;
import com.example.helloworld.dto.invest.DashboardLatestDailyReportSummaryDto;
import com.example.helloworld.dto.invest.DailyReportSummaryDto;
import com.example.helloworld.dto.invest.PortfolioDashboardOverviewDto;
import com.example.helloworld.entity.invest.DailyReport;
import com.example.helloworld.entity.invest.DailyReportTypeCode;
import com.example.helloworld.entity.invest.Portfolio;
import com.example.helloworld.entity.invest.StockPriceDaily;
import com.example.helloworld.repository.invest.PortfolioRepository;
import com.example.helloworld.repository.invest.StockPriceDailyRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class PortfolioDashboardService {

    private static final int MONEY_SCALE = 2;

    private final PortfolioRepository portfolioRepository;
    private final StockPriceDailyRepository stockPriceDailyRepository;
    private final InvestCurrentUserService investCurrentUserService;
    private final DailyReportService dailyReportService;
    private final PortfolioAlertEventService portfolioAlertEventService;
    private final InvestFxConversionService investFxConversionService;

    public PortfolioDashboardService(PortfolioRepository portfolioRepository,
                                     StockPriceDailyRepository stockPriceDailyRepository,
                                     InvestCurrentUserService investCurrentUserService,
                                     DailyReportService dailyReportService,
                                     PortfolioAlertEventService portfolioAlertEventService,
                                     InvestFxConversionService investFxConversionService) {
        this.portfolioRepository = portfolioRepository;
        this.stockPriceDailyRepository = stockPriceDailyRepository;
        this.investCurrentUserService = investCurrentUserService;
        this.dailyReportService = dailyReportService;
        this.portfolioAlertEventService = portfolioAlertEventService;
        this.investFxConversionService = investFxConversionService;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public PortfolioDashboardOverviewDto getOverview() {
        String userId = resolveCurrentUserUid();
        List<Portfolio> holdings = portfolioRepository.findByUserIdAndIsActiveTrue(userId);

        BigDecimal totalCost = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalMarketValue = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        Map<Long, StockPriceDaily> latestPriceCache = new HashMap<>();

        for (Portfolio portfolio : holdings) {
            String assetCurrency = investFxConversionService.resolveAssetCurrencyByMarket(portfolio.getStock().getMarket());
            BigDecimal totalCostInTwd = investFxConversionService.convertToBaseCurrency(portfolio.getTotalCost(), assetCurrency);
            totalCost = totalCost.add(totalCostInTwd).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            Long stockId = portfolio.getStock().getId();
            StockPriceDaily latest = latestPriceCache.computeIfAbsent(stockId,
                id -> stockPriceDailyRepository.findTopByStockIdOrderByTradeDateDesc(id).orElse(null));

            BigDecimal price = latest == null ? portfolio.getAvgCost() : latest.getClosePrice();
            BigDecimal marketValueInAssetCurrency = price.multiply(portfolio.getQuantity()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal marketValue = investFxConversionService.convertToBaseCurrency(marketValueInAssetCurrency, assetCurrency);
            totalMarketValue = totalMarketValue.add(marketValue).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal totalPnL = totalMarketValue.subtract(totalCost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalPnLPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalPnLPercent = totalPnL.divide(totalCost, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        PortfolioDashboardOverviewDto dto = new PortfolioDashboardOverviewDto();
        dto.setTotalCost(totalCost);
        dto.setTotalMarketValue(totalMarketValue);
        dto.setTotalPnL(totalPnL);
        dto.setTotalPnLPercent(totalPnLPercent);
        dto.setBaseCurrency(investFxConversionService.getBaseCurrency());
        dto.setUsdToTwdRate(investFxConversionService.getUsdToTwdRate());
        dto.setHoldingCount((long) holdings.size());
        dto.setRiskDistribution(List.of());
        dto.setRecommendationDistribution(List.of());
        dto.setTopRiskHoldings(List.of());

        DashboardAlertSummaryDto alertSummary = portfolioAlertEventService.buildDashboardSummaryForCurrentUser();
        dto.setAlertSummary(alertSummary);
        dto.setLatestDailyReportSummary(buildLatestDailyReportSummary());

        return dto;
    }

    private DashboardLatestDailyReportSummaryDto buildLatestDailyReportSummary() {
        DailyReport latest = dailyReportService.findLatestEntityByCurrentUser(DailyReportTypeCode.PORTFOLIO_RISK_DAILY);
        if (latest == null) {
            return null;
        }

        DailyReportSummaryDto summary = dailyReportService.parseSummary(latest.getSummaryJson());
        DashboardLatestDailyReportSummaryDto dto = new DashboardLatestDailyReportSummaryDto();
        dto.setReportId(latest.getId());
        dto.setReportDate(latest.getReportDate());
        dto.setReportType(latest.getReportType().name());
        dto.setStatus(latest.getStatus().name());
        if (summary != null) {
            dto.setDataAsOfTradeDate(summary.getDataAsOfTradeDate());
            dto.setHighRiskCount(summary.getHighRiskCount());
            dto.setCriticalRiskCount(summary.getCriticalRiskCount());
        }
        return dto;
    }

    private String resolveCurrentUserUid() {
        return investCurrentUserService.resolveCurrentUserUid();
    }
}
