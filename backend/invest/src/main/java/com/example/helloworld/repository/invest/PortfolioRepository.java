package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    @Query("SELECT p FROM Portfolio p JOIN p.stock s WHERE " +
        "(:userId IS NULL OR :userId = '' OR p.userId = :userId) AND " +
        "(:stockId IS NULL OR s.id = :stockId) AND " +
        "(:isActive IS NULL OR p.isActive = :isActive) " +
        "ORDER BY p.updatedAt DESC, p.id DESC")
    Page<Portfolio> findByFilters(
        @Param("userId") String userId,
        @Param("stockId") Long stockId,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );

    @Query("SELECT p FROM Portfolio p JOIN p.stock s WHERE " +
        "(:userId IS NULL OR :userId = '' OR p.userId = :userId) AND " +
        "(:stockId IS NULL OR s.id = :stockId) AND " +
        "(:isActive IS NULL OR p.isActive = :isActive) " +
        "ORDER BY p.updatedAt DESC, p.id DESC")
    List<Portfolio> findAllByFilters(
        @Param("userId") String userId,
        @Param("stockId") Long stockId,
        @Param("isActive") Boolean isActive
    );

    List<Portfolio> findByUserIdAndIsActiveTrue(String userId);

    Optional<Portfolio> findByIdAndUserId(Long id, String userId);

    @Query("SELECT DISTINCT p.userId FROM Portfolio p " +
        "WHERE p.isActive = true " +
        "AND EXISTS (SELECT 1 FROM InvestUser u WHERE u.uid = p.userId) " +
        "ORDER BY p.userId ASC")
    List<String> findDistinctActiveUserIds();
}
