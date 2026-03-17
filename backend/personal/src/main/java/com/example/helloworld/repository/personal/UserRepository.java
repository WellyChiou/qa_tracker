package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByUid(String uid);
    Optional<User> findByLineUserId(String lineUserId);
    
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "LEFT JOIN FETCH u.permissions " +
           "WHERE u.username = :username")
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);
    
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "LEFT JOIN FETCH u.permissions " +
           "WHERE u.uid = :uid")
    Optional<User> findByUidWithRolesAndPermissions(@Param("uid") String uid);

    @Query(value = "SELECT DISTINCT u FROM User u " +
                   "LEFT JOIN u.roles r " +
                   "WHERE (:username IS NULL OR :username = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) " +
                   "AND (:email IS NULL OR :email = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
                   "AND (:roleId IS NULL OR r.id = :roleId) " +
                   "AND (:isEnabled IS NULL OR u.isEnabled = :isEnabled) " +
                   "ORDER BY u.uid",
           countQuery = "SELECT COUNT(DISTINCT u.uid) FROM User u " +
                        "LEFT JOIN u.roles r " +
                        "WHERE (:username IS NULL OR :username = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) " +
                        "AND (:email IS NULL OR :email = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
                        "AND (:roleId IS NULL OR r.id = :roleId) " +
                        "AND (:isEnabled IS NULL OR u.isEnabled = :isEnabled)")
    Page<User> findByFilters(
            @Param("username") String username,
            @Param("email") String email,
            @Param("roleId") Long roleId,
            @Param("isEnabled") Boolean isEnabled,
            Pageable pageable
    );
}
