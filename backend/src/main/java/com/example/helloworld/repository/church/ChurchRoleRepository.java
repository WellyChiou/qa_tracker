package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ChurchRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChurchRoleRepository extends JpaRepository<ChurchRole, Long> {
    Optional<ChurchRole> findByRoleName(String roleName);
    
    @Query("SELECT DISTINCT r FROM ChurchRole r LEFT JOIN FETCH r.permissions")
    List<ChurchRole> findAllWithPermissions();
}

