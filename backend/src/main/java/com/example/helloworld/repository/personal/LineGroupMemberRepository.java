package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.LineGroup;
import com.example.helloworld.entity.personal.LineGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LineGroupMemberRepository extends JpaRepository<LineGroupMember, Long> {
    Optional<LineGroupMember> findByLineGroupAndUserId(LineGroup lineGroup, String userId);
    List<LineGroupMember> findByLineGroup(LineGroup lineGroup);
    
    // 查詢啟用的成員
    List<LineGroupMember> findByLineGroupAndIsActiveTrue(LineGroup lineGroup);
    
    // 計算群組成員數（只計算啟用的成員）
    long countByLineGroupAndIsActiveTrue(LineGroup group);
    
    // 計算群組成員數（包含所有成員）
    long countByLineGroup(LineGroup group);
}

