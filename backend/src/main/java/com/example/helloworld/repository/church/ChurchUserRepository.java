package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ChurchUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChurchUserRepository extends JpaRepository<ChurchUser, String> {
    Optional<ChurchUser> findByEmail(String email);
    Optional<ChurchUser> findByUsername(String username);
    Optional<ChurchUser> findByUid(String uid);
    
    @Query("SELECT DISTINCT u FROM ChurchUser u LEFT JOIN FETCH u.roles LEFT JOIN FETCH u.permissions WHERE u.username = :username")
    Optional<ChurchUser> findByUsernameWithRolesAndPermissions(@Param("username") String username);
    
    @Query("SELECT DISTINCT u FROM ChurchUser u LEFT JOIN FETCH u.roles LEFT JOIN FETCH u.permissions WHERE u.uid = :uid")
    Optional<ChurchUser> findByUidWithRolesAndPermissions(@Param("uid") String uid);
    
    @Query("SELECT DISTINCT u FROM ChurchUser u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "LEFT JOIN FETCH u.permissions")
    List<ChurchUser> findAllWithRolesAndPermissions();
}

