package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    Optional<Watchlist> findByUserIdAndIsDefaultTrueAndIsActiveTrue(String userId);

    List<Watchlist> findByUserIdAndIsActiveTrueOrderByIdAsc(String userId);
}
