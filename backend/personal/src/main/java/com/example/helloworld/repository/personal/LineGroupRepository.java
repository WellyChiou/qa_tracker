package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.LineGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LineGroupRepository extends JpaRepository<LineGroup, String> {
    Optional<LineGroup> findByGroupId(String groupId);
    
    List<LineGroup> findByIsActiveTrue();
    
    // 根據群組代碼查詢啟用的群組
    List<LineGroup> findByGroupCodeAndIsActiveTrue(String groupCode);
    
    // 根據群組 ID 和群組代碼查詢
    Optional<LineGroup> findByGroupIdAndGroupCode(String groupId, String groupCode);
}
