package com.example.helloworld.repository.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvestRoleRepository extends JpaRepository<InvestRole, Long> {
    Optional<InvestRole> findByRoleName(String roleName);
}
