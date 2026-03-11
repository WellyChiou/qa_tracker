package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByPermissionCode(String permissionCode);
    List<Permission> findByResource(String resource);
    List<Permission> findByAction(String action);
}
