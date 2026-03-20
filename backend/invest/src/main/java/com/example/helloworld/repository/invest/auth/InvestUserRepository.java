package com.example.helloworld.repository.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvestUserRepository extends JpaRepository<InvestUser, String> {
    Optional<InvestUser> findByUsername(String username);

    Optional<InvestUser> findByEmail(String email);

    @Query("SELECT u FROM InvestUser u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "LEFT JOIN FETCH u.permissions " +
           "WHERE u.username = :username")
    Optional<InvestUser> findByUsernameWithRolesAndPermissions(@Param("username") String username);
}
