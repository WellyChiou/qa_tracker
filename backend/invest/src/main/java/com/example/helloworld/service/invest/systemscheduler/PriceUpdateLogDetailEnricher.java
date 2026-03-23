package com.example.helloworld.service.invest.systemscheduler;

import com.example.helloworld.dto.invest.SystemSchedulerJobLogFailedItemDto;
import com.example.helloworld.dto.invest.SystemSchedulerJobLogPagedDto;
import com.example.helloworld.entity.invest.PriceUpdateDetailStatusCode;
import com.example.helloworld.entity.invest.PriceUpdateJobDetail;
import com.example.helloworld.repository.invest.PriceUpdateJobDetailRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@Transactional(transactionManager = "investTransactionManager", readOnly = true)
public class PriceUpdateLogDetailEnricher {

    private static final int MAX_FAILED_ITEMS = 8;

    private final PriceUpdateJobDetailRepository priceUpdateJobDetailRepository;

    public PriceUpdateLogDetailEnricher(PriceUpdateJobDetailRepository priceUpdateJobDetailRepository) {
        this.priceUpdateJobDetailRepository = priceUpdateJobDetailRepository;
    }

    public void enrich(SystemSchedulerJobLogPagedDto dto, Long jobLogId) {
        if (dto == null || jobLogId == null) {
            return;
        }

        List<PriceUpdateJobDetail> details = priceUpdateJobDetailRepository.findByJobLogIdOrderByIdAsc(jobLogId);
        if (details.isEmpty()) {
            return;
        }

        Set<String> tickerSet = new LinkedHashSet<>();
        for (PriceUpdateJobDetail detail : details) {
            if (detail.getTicker() != null && !detail.getTicker().isBlank()) {
                tickerSet.add(detail.getTicker().trim());
            }
        }
        dto.setTargetTickers(tickerSet.stream().toList());

        List<SystemSchedulerJobLogFailedItemDto> failedItems = details.stream()
            .filter(d -> d.getStatus() == PriceUpdateDetailStatusCode.FAILED)
            .limit(MAX_FAILED_ITEMS)
            .map(this::toFailedItemDto)
            .toList();
        dto.setFailedItems(failedItems);
    }

    private SystemSchedulerJobLogFailedItemDto toFailedItemDto(PriceUpdateJobDetail detail) {
        SystemSchedulerJobLogFailedItemDto dto = new SystemSchedulerJobLogFailedItemDto();
        dto.setTicker(detail.getTicker());
        dto.setTradeDate(detail.getTradeDate());
        dto.setReason(detail.getReason());
        return dto;
    }
}
