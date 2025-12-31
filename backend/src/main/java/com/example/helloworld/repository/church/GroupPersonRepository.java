package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.GroupPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupPersonRepository extends JpaRepository<GroupPerson, Long> {
    // 預設只查詢活躍的記錄（isActive = true）
    Optional<GroupPerson> findByGroupIdAndPersonIdAndIsActiveTrue(Long groupId, Long personId);
    List<GroupPerson> findByGroupIdAndIsActiveTrue(Long groupId);
    List<GroupPerson> findByPersonIdAndIsActiveTrue(Long personId);
    
    // 查詢所有記錄（包括歷史記錄）的方法
    Optional<GroupPerson> findByGroupIdAndPersonId(Long groupId, Long personId);
    List<GroupPerson> findByGroupId(Long groupId);
    List<GroupPerson> findByPersonId(Long personId);
    
    void deleteByGroupIdAndPersonId(Long groupId, Long personId);
    
    @Query("SELECT gp FROM GroupPerson gp JOIN FETCH gp.person WHERE gp.group.id = :groupId AND gp.person.isActive = true AND gp.isActive = true ORDER BY gp.person.personName")
    List<GroupPerson> findActiveMembersByGroupId(@Param("groupId") Long groupId);
    
    @Query("SELECT gp FROM GroupPerson gp JOIN FETCH gp.group WHERE gp.person.id = :personId AND gp.group.isActive = true AND gp.isActive = true")
    List<GroupPerson> findActiveGroupsByPersonId(@Param("personId") Long personId);
    
    // 查詢所有記錄（包括歷史記錄）的方法
    @Query("SELECT gp FROM GroupPerson gp JOIN FETCH gp.person WHERE gp.group.id = :groupId AND gp.person.isActive = true ORDER BY gp.person.personName")
    List<GroupPerson> findAllMembersByGroupId(@Param("groupId") Long groupId);
    
    @Query("SELECT gp FROM GroupPerson gp JOIN FETCH gp.group WHERE gp.person.id = :personId AND gp.group.isActive = true")
    List<GroupPerson> findAllGroupsByPersonId(@Param("personId") Long personId);

    // 批量查詢多個小組的活躍成員
    List<GroupPerson> findByGroupIdInAndIsActiveTrue(List<Long> groupIds);

    // 批量查詢多個人員的所有小組（包括歷史記錄）
    @Query("SELECT gp FROM GroupPerson gp JOIN FETCH gp.group WHERE gp.person.id IN :personIds AND gp.group.isActive = true")
    List<GroupPerson> findAllGroupsByPersonIds(@Param("personIds") List<Long> personIds);
}

