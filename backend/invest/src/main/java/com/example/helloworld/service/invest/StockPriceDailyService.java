package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.StockPriceDailyResponseDto;
import com.example.helloworld.dto.invest.StockPriceDailyUpsertRequestDto;
import com.example.helloworld.entity.invest.Stock;
import com.example.helloworld.entity.invest.StockPriceDaily;
import com.example.helloworld.repository.invest.StockPriceDailyRepository;
import com.example.helloworld.repository.invest.StockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class StockPriceDailyService {
    // Step 1 keeps simple CRUD for verification.
    // Step 2+ can add import/overwrite/recalculate behaviors in this service.

    private final StockPriceDailyRepository stockPriceDailyRepository;
    private final StockRepository stockRepository;

    public StockPriceDailyService(StockPriceDailyRepository stockPriceDailyRepository,
                                  StockRepository stockRepository) {
        this.stockPriceDailyRepository = stockPriceDailyRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<StockPriceDailyResponseDto> getPaged(Long stockId,
                                                     String ticker,
                                                     LocalDate tradeDateFrom,
                                                     LocalDate tradeDateTo,
                                                     int page,
                                                     int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockPriceDailyRepository.findByFilters(stockId, normalize(ticker), tradeDateFrom, tradeDateTo, pageable)
            .map(this::toResponse);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<StockPriceDailyResponseDto> getAll(Long stockId,
                                                    String ticker,
                                                    LocalDate tradeDateFrom,
                                                    LocalDate tradeDateTo) {
        return stockPriceDailyRepository.findAllByFilters(stockId, normalize(ticker), tradeDateFrom, tradeDateTo)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public StockPriceDailyResponseDto create(StockPriceDailyUpsertRequestDto request) {
        validateRequest(request);

        Stock stock = stockRepository.findById(request.getStockId())
            .orElseThrow(() -> new RuntimeException("找不到股票資料，id=" + request.getStockId()));

        if (stockPriceDailyRepository.findByStockIdAndTradeDate(request.getStockId(), request.getTradeDate()).isPresent()) {
            throw new RuntimeException("同一檔股票同一交易日資料已存在");
        }

        StockPriceDaily entity = new StockPriceDaily();
        copyFields(entity, request, stock);
        return toResponse(stockPriceDailyRepository.save(entity));
    }

    public StockPriceDailyResponseDto update(Long id, StockPriceDailyUpsertRequestDto request) {
        validateRequest(request);

        StockPriceDaily existing = stockPriceDailyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到每日行情資料，id=" + id));

        Stock stock = stockRepository.findById(request.getStockId())
            .orElseThrow(() -> new RuntimeException("找不到股票資料，id=" + request.getStockId()));

        stockPriceDailyRepository.findByStockIdAndTradeDate(request.getStockId(), request.getTradeDate())
            .ifPresent(conflict -> {
                if (!conflict.getId().equals(id)) {
                    throw new RuntimeException("同一檔股票同一交易日資料已存在");
                }
            });

        copyFields(existing, request, stock);
        return toResponse(stockPriceDailyRepository.save(existing));
    }

    public void delete(Long id) {
        StockPriceDaily existing = stockPriceDailyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到每日行情資料，id=" + id));
        stockPriceDailyRepository.delete(existing);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<StockPriceDailyResponseDto> getRecentHistoryByStockId(Long stockId) {
        return stockPriceDailyRepository.findTop30ByStockIdOrderByTradeDateDesc(stockId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private void copyFields(StockPriceDaily entity, StockPriceDailyUpsertRequestDto request, Stock stock) {
        entity.setStock(stock);
        entity.setTradeDate(request.getTradeDate());
        entity.setOpenPrice(request.getOpenPrice());
        entity.setHighPrice(request.getHighPrice());
        entity.setLowPrice(request.getLowPrice());
        entity.setClosePrice(request.getClosePrice());
        entity.setVolume(request.getVolume());
        entity.setChangeAmount(request.getChangeAmount());
        entity.setChangePercent(request.getChangePercent());
    }

    private void validateRequest(StockPriceDailyUpsertRequestDto request) {
        if (request.getStockId() == null) {
            throw new RuntimeException("stockId 為必填");
        }
        if (request.getTradeDate() == null) {
            throw new RuntimeException("tradeDate 為必填");
        }
        if (request.getOpenPrice() == null || request.getHighPrice() == null || request.getLowPrice() == null || request.getClosePrice() == null) {
            throw new RuntimeException("open/high/low/close price 均為必填");
        }
        if (request.getVolume() == null || request.getVolume() < 0) {
            throw new RuntimeException("volume 不可為負數");
        }

        if (request.getHighPrice().compareTo(request.getLowPrice()) < 0) {
            throw new RuntimeException("highPrice 不可小於 lowPrice");
        }

        validatePositive(request.getOpenPrice(), "openPrice");
        validatePositive(request.getHighPrice(), "highPrice");
        validatePositive(request.getLowPrice(), "lowPrice");
        validatePositive(request.getClosePrice(), "closePrice");
    }

    private void validatePositive(BigDecimal value, String field) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(field + " 必須大於 0");
        }
    }

    private StockPriceDailyResponseDto toResponse(StockPriceDaily entity) {
        StockPriceDailyResponseDto dto = new StockPriceDailyResponseDto();
        dto.setId(entity.getId());
        dto.setStockId(entity.getStock().getId());
        dto.setMarket(entity.getStock().getMarket());
        dto.setTicker(entity.getStock().getTicker());
        dto.setStockName(entity.getStock().getName());
        dto.setTradeDate(entity.getTradeDate());
        dto.setOpenPrice(entity.getOpenPrice());
        dto.setHighPrice(entity.getHighPrice());
        dto.setLowPrice(entity.getLowPrice());
        dto.setClosePrice(entity.getClosePrice());
        dto.setVolume(entity.getVolume());
        dto.setChangeAmount(entity.getChangeAmount());
        dto.setChangePercent(entity.getChangePercent());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
