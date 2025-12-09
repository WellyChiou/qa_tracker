package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    Optional<MenuItem> findByMenuCode(String menuCode);
    List<MenuItem> findByParentIdIsNullOrderByOrderIndexAsc();
    List<MenuItem> findByParentIdOrderByOrderIndexAsc(Long parentId);
    List<MenuItem> findByIsActiveTrueOrderByOrderIndexAsc();
    
    @Query("SELECT m FROM MenuItem m WHERE m.isActive = true AND m.parentId IS NULL ORDER BY m.orderIndex ASC")
    List<MenuItem> findActiveRootMenus();
    
    @Query("SELECT m FROM MenuItem m WHERE m.isActive = true AND m.parentId = :parentId ORDER BY m.orderIndex ASC")
    List<MenuItem> findActiveChildMenus(@Param("parentId") Long parentId);
}
