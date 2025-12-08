package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ChurchPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChurchPermissionRepository extends JpaRepository<ChurchPermission, Long> {
    Optional<ChurchPermission> findByPermissionCode(String permissionCode);
    
    @Query("SELECT p FROM ChurchPermission p WHERE p.resource = :resource")
    List<ChurchPermission> findByResource(@Param("resource") String resource);
}

