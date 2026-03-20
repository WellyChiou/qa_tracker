package com.example.helloworld.service.invest;

import com.example.helloworld.dto.invest.WatchlistDefaultDto;
import com.example.helloworld.dto.invest.WatchlistItemDto;
import com.example.helloworld.dto.invest.WatchlistItemUpsertRequestDto;
import com.example.helloworld.entity.invest.Stock;
import com.example.helloworld.entity.invest.Watchlist;
import com.example.helloworld.entity.invest.WatchlistItem;
import com.example.helloworld.repository.invest.StockRepository;
import com.example.helloworld.repository.invest.WatchlistItemRepository;
import com.example.helloworld.repository.invest.WatchlistRepository;
import com.example.helloworld.service.invest.auth.InvestCurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class WatchlistService {

    private static final String DEFAULT_WATCHLIST_NAME = "我的觀察清單";

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final StockRepository stockRepository;
    private final InvestCurrentUserService investCurrentUserService;

    public WatchlistService(WatchlistRepository watchlistRepository,
                            WatchlistItemRepository watchlistItemRepository,
                            StockRepository stockRepository,
                            InvestCurrentUserService investCurrentUserService) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.stockRepository = stockRepository;
        this.investCurrentUserService = investCurrentUserService;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public WatchlistDefaultDto getDefaultWatchlist() {
        Watchlist watchlist = getOrCreateDefaultWatchlist(resolveCurrentUserUid());
        return toDefaultDto(watchlist);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Page<WatchlistItemDto> getDefaultItemsPaged(int page, int size) {
        Watchlist watchlist = getOrCreateDefaultWatchlist(resolveCurrentUserUid());
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchlistItem> itemPage = watchlistItemRepository.findActiveByWatchlistId(watchlist.getId(), pageable);
        return new PageImpl<>(
            itemPage.getContent().stream().map(this::toItemDto).toList(),
            pageable,
            itemPage.getTotalElements()
        );
    }

    public WatchlistItemDto addDefaultItem(WatchlistItemUpsertRequestDto request) {
        if (request == null || request.getStockId() == null) {
            throw new RuntimeException("stockId 為必填");
        }

        String userUid = resolveCurrentUserUid();
        Watchlist watchlist = getOrCreateDefaultWatchlist(userUid);

        Stock stock = stockRepository.findById(request.getStockId())
            .orElseThrow(() -> new RuntimeException("找不到股票資料，id=" + request.getStockId()));

        WatchlistItem item = watchlistItemRepository.findByWatchlistIdAndStockId(watchlist.getId(), stock.getId())
            .orElseGet(WatchlistItem::new);
        item.setWatchlist(watchlist);
        item.setStock(stock);
        item.setNote(normalize(request.getNote()));
        item.setIsActive(Boolean.TRUE);

        return toItemDto(watchlistItemRepository.save(item));
    }

    public void removeDefaultItem(Long itemId) {
        if (itemId == null) {
            throw new RuntimeException("itemId 為必填");
        }

        String userUid = resolveCurrentUserUid();
        WatchlistItem item = watchlistItemRepository.findByIdAndUserId(itemId, userUid)
            .orElseThrow(() -> new RuntimeException("找不到觀察清單標的或無權限，id=" + itemId));
        item.setIsActive(Boolean.FALSE);
        watchlistItemRepository.save(item);
    }

    public Watchlist getOrCreateDefaultWatchlist(String userUid) {
        return watchlistRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userUid)
            .orElseGet(() -> {
                Watchlist watchlist = new Watchlist();
                watchlist.setUserId(userUid);
                watchlist.setName(DEFAULT_WATCHLIST_NAME);
                watchlist.setIsDefault(Boolean.TRUE);
                watchlist.setIsActive(Boolean.TRUE);
                return watchlistRepository.save(watchlist);
            });
    }

    private WatchlistDefaultDto toDefaultDto(Watchlist watchlist) {
        WatchlistDefaultDto dto = new WatchlistDefaultDto();
        dto.setId(watchlist.getId());
        dto.setName(watchlist.getName());
        dto.setIsDefault(watchlist.getIsDefault());
        dto.setIsActive(watchlist.getIsActive());
        dto.setActiveItemCount((int) watchlistItemRepository.countByWatchlistIdAndIsActiveTrue(watchlist.getId()));
        dto.setUpdatedAt(watchlist.getUpdatedAt());
        return dto;
    }

    private WatchlistItemDto toItemDto(WatchlistItem item) {
        WatchlistItemDto dto = new WatchlistItemDto();
        dto.setId(item.getId());
        dto.setWatchlistId(item.getWatchlist().getId());
        dto.setStockId(item.getStock().getId());
        dto.setMarket(item.getStock().getMarket());
        dto.setTicker(item.getStock().getTicker());
        dto.setStockName(item.getStock().getName());
        dto.setNote(item.getNote());
        dto.setIsActive(item.getIsActive());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
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
