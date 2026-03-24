package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.WatchlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    @Query("SELECT w FROM WatchlistItem w " +
        "JOIN FETCH w.stock s " +
        "WHERE w.watchlist.id = :watchlistId AND w.isActive = true " +
        "ORDER BY w.updatedAt DESC, w.id DESC")
    Page<WatchlistItem> findActiveByWatchlistId(@Param("watchlistId") Long watchlistId, Pageable pageable);

    @Query("SELECT w FROM WatchlistItem w " +
        "JOIN FETCH w.stock s " +
        "WHERE w.watchlist.id = :watchlistId AND w.isActive = true " +
        "ORDER BY w.updatedAt DESC, w.id DESC")
    List<WatchlistItem> findActiveByWatchlistId(@Param("watchlistId") Long watchlistId);

    Optional<WatchlistItem> findByWatchlistIdAndStockId(Long watchlistId, Long stockId);

    long countByWatchlistIdAndIsActiveTrue(Long watchlistId);

    @Query("SELECT w FROM WatchlistItem w " +
        "JOIN FETCH w.watchlist wl " +
        "JOIN FETCH w.stock s " +
        "WHERE w.id = :id AND w.isActive = true AND wl.userId = :userId")
    Optional<WatchlistItem> findByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);
}
