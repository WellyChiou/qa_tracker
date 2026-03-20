package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.PortfolioAlertSettingDto;
import com.example.helloworld.dto.invest.PortfolioAlertSettingUpsertRequestDto;
import com.example.helloworld.entity.invest.Portfolio;
import com.example.helloworld.entity.invest.PortfolioAlertSetting;
import com.example.helloworld.repository.invest.PortfolioAlertSettingRepository;
import com.example.helloworld.repository.invest.PortfolioRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class PortfolioAlertSettingService {

    private static final int PRICE_SCALE = 4;
    private static final int PERCENT_SCALE = 2;

    private final PortfolioAlertSettingRepository portfolioAlertSettingRepository;
    private final PortfolioRepository portfolioRepository;
    private final InvestCurrentUserService investCurrentUserService;

    public PortfolioAlertSettingService(PortfolioAlertSettingRepository portfolioAlertSettingRepository,
                                        PortfolioRepository portfolioRepository,
                                        InvestCurrentUserService investCurrentUserService) {
        this.portfolioAlertSettingRepository = portfolioAlertSettingRepository;
        this.portfolioRepository = portfolioRepository;
        this.investCurrentUserService = investCurrentUserService;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public PortfolioAlertSettingDto getByPortfolioId(Long portfolioId) {
        Portfolio portfolio = getOwnedPortfolio(portfolioId);
        PortfolioAlertSetting setting = portfolioAlertSettingRepository
            .findByPortfolioIdAndPortfolioUserId(portfolioId, portfolio.getUserId())
            .orElse(null);
        if (setting == null) {
            PortfolioAlertSettingDto dto = new PortfolioAlertSettingDto();
            dto.setPortfolioId(portfolioId);
            dto.setStopLossPrice(null);
            dto.setAlertDropPercent(null);
            dto.setEnabled(Boolean.FALSE);
            return dto;
        }
        return toDto(setting);
    }

    public PortfolioAlertSettingDto upsert(Long portfolioId, PortfolioAlertSettingUpsertRequestDto request) {
        Portfolio portfolio = getOwnedPortfolio(portfolioId);
        validateRequest(request);

        PortfolioAlertSetting setting = portfolioAlertSettingRepository.findByPortfolioId(portfolioId)
            .orElseGet(PortfolioAlertSetting::new);
        setting.setPortfolio(portfolio);
        setting.setStopLossPrice(scalePrice(request.getStopLossPrice()));
        setting.setAlertDropPercent(scalePercent(request.getAlertDropPercent()));
        setting.setEnabled(Boolean.TRUE.equals(request.getEnabled()));

        return toDto(portfolioAlertSettingRepository.save(setting));
    }

    private Portfolio getOwnedPortfolio(Long portfolioId) {
        String userId = resolveCurrentUserUid();
        return portfolioRepository.findByIdAndUserId(portfolioId, userId)
            .orElseThrow(() -> new RuntimeException("找不到持股或無權限，portfolioId=" + portfolioId));
    }

    private void validateRequest(PortfolioAlertSettingUpsertRequestDto request) {
        if (request == null) {
            throw new RuntimeException("請求內容不可為空");
        }
        if (request.getStopLossPrice() != null && request.getStopLossPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("stopLossPrice 必須大於 0");
        }
        if (request.getAlertDropPercent() != null && request.getAlertDropPercent().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("alertDropPercent 必須大於 0");
        }
    }

    private BigDecimal scalePrice(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scalePercent(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private PortfolioAlertSettingDto toDto(PortfolioAlertSetting setting) {
        PortfolioAlertSettingDto dto = new PortfolioAlertSettingDto();
        dto.setId(setting.getId());
        dto.setPortfolioId(setting.getPortfolio().getId());
        dto.setStopLossPrice(setting.getStopLossPrice());
        dto.setAlertDropPercent(setting.getAlertDropPercent());
        dto.setEnabled(Boolean.TRUE.equals(setting.getEnabled()));
        dto.setCreatedAt(setting.getCreatedAt());
        dto.setUpdatedAt(setting.getUpdatedAt());
        return dto;
    }

    private String resolveCurrentUserUid() {
        return investCurrentUserService.resolveCurrentUserUid();
    }
}
