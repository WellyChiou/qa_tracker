package com.example.helloworld.repository.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestMenuItemRepository extends JpaRepository<InvestMenuItem, Long> {
    @Query("SELECT m FROM InvestMenuItem m WHERE m.isActive = true AND m.parentId IS NULL ORDER BY m.orderIndex ASC")
    List<InvestMenuItem> findActiveRootMenus();

    @Query("SELECT m FROM InvestMenuItem m WHERE m.isActive = true AND m.parentId = :parentId ORDER BY m.orderIndex ASC")
    List<InvestMenuItem> findActiveChildMenus(@Param("parentId") Long parentId);
}
