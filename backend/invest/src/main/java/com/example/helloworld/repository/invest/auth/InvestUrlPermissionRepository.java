package com.example.helloworld.repository.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestUrlPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestUrlPermissionRepository extends JpaRepository<InvestUrlPermission, Long> {
    @Query("SELECT u FROM InvestUrlPermission u WHERE u.isActive = true ORDER BY u.orderIndex ASC")
    List<InvestUrlPermission> findActivePermissionsOrdered();
}
