package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String roleName);

    @Query("SELECT r FROM Role r WHERE " +
           "(:roleName IS NULL OR :roleName = '' OR LOWER(r.roleName) LIKE LOWER(CONCAT('%', :roleName, '%'))) " +
           "ORDER BY r.id")
    Page<Role> findByFilters(@Param("roleName") String roleName, Pageable pageable);
}
