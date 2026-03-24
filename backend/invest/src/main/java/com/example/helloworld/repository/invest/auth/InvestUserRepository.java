package com.example.helloworld.repository.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvestUserRepository extends JpaRepository<InvestUser, String> {
    Optional<InvestUser> findByUsername(String username);

    Optional<InvestUser> findByEmail(String email);

    Optional<InvestUser> findByUid(String uid);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Query(
        value = "SELECT DISTINCT u FROM InvestUser u " +
            "LEFT JOIN u.roles r " +
            "WHERE (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) " +
            "  AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
            "  AND (:isEnabled IS NULL OR u.isEnabled = :isEnabled)",
        countQuery = "SELECT COUNT(DISTINCT u.uid) FROM InvestUser u " +
            "LEFT JOIN u.roles r " +
            "WHERE (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) " +
            "  AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
            "  AND (:isEnabled IS NULL OR u.isEnabled = :isEnabled)"
    )
    Page<InvestUser> findByFilters(
        @Param("username") String username,
        @Param("email") String email,
        @Param("isEnabled") Boolean isEnabled,
        Pageable pageable
    );

    @Query("SELECT u FROM InvestUser u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "LEFT JOIN FETCH u.permissions " +
           "WHERE u.username = :username")
    Optional<InvestUser> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM InvestUser u " +
        "LEFT JOIN FETCH u.roles r " +
        "LEFT JOIN FETCH r.permissions " +
        "LEFT JOIN FETCH u.permissions " +
        "WHERE u.uid = :uid")
    Optional<InvestUser> findByUidWithRolesAndPermissions(@Param("uid") String uid);
}
