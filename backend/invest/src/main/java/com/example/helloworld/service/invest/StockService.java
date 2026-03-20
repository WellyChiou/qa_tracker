package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.StockOptionDto;
import com.example.helloworld.dto.invest.StockResponseDto;
import com.example.helloworld.dto.invest.StockUpsertRequestDto;
import com.example.helloworld.entity.invest.Stock;
import com.example.helloworld.repository.invest.StockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<StockResponseDto> getPaged(String market, String ticker, String name, Boolean isActive, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockRepository.findByFilters(normalize(market), normalize(ticker), normalize(name), isActive, pageable)
            .map(this::toResponse);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<StockResponseDto> getAll(Boolean isActive) {
        return stockRepository.findAllByIsActiveFilter(isActive)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<StockOptionDto> getOptions() {
        return stockRepository.findByIsActiveTrueOrderByTickerAsc()
            .stream()
            .map(s -> new StockOptionDto(s.getId(), s.getTicker(), s.getName()))
            .toList();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public StockResponseDto getById(Long id) {
        Stock stock = stockRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到股票資料，id=" + id));
        return toResponse(stock);
    }

    public StockResponseDto create(StockUpsertRequestDto request) {
        String market = requireAndNormalize(request.getMarket(), "market");
        String ticker = requireAndNormalize(request.getTicker(), "ticker");
        String name = require(request.getName(), "name");

        if (stockRepository.existsByMarketIgnoreCaseAndTickerIgnoreCase(market, ticker)) {
            throw new RuntimeException("股票代碼已存在：" + market + "-" + ticker);
        }

        Stock stock = new Stock();
        stock.setMarket(market);
        stock.setTicker(ticker);
        stock.setName(name);
        stock.setIndustry(normalize(request.getIndustry()));
        stock.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        return toResponse(stockRepository.save(stock));
    }

    public StockResponseDto update(Long id, StockUpsertRequestDto request) {
        Stock stock = stockRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到股票資料，id=" + id));

        String market = requireAndNormalize(request.getMarket(), "market");
        String ticker = requireAndNormalize(request.getTicker(), "ticker");
        String name = require(request.getName(), "name");

        if (stockRepository.existsByMarketAndTickerExcludingId(market, ticker, id)) {
            throw new RuntimeException("股票代碼已存在：" + market + "-" + ticker);
        }

        stock.setMarket(market);
        stock.setTicker(ticker);
        stock.setName(name);
        stock.setIndustry(normalize(request.getIndustry()));
        if (request.getIsActive() != null) {
            stock.setIsActive(request.getIsActive());
        }

        return toResponse(stockRepository.save(stock));
    }

    public void delete(Long id) {
        Stock stock = stockRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("找不到股票資料，id=" + id));
        stock.setIsActive(false);
        stockRepository.save(stock);
    }

    private StockResponseDto toResponse(Stock stock) {
        StockResponseDto dto = new StockResponseDto();
        dto.setId(stock.getId());
        dto.setMarket(stock.getMarket());
        dto.setTicker(stock.getTicker());
        dto.setName(stock.getName());
        dto.setIndustry(stock.getIndustry());
        dto.setIsActive(stock.getIsActive());
        dto.setCreatedAt(stock.getCreatedAt());
        dto.setUpdatedAt(stock.getUpdatedAt());
        return dto;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String require(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new RuntimeException(fieldName + " 為必填");
        }
        return normalized;
    }

    private String requireAndNormalize(String value, String fieldName) {
        return require(value, fieldName).toUpperCase();
    }
}
