package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.LineGroup;
import com.example.helloworld.entity.church.LineGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LineGroupMemberRepository extends JpaRepository<LineGroupMember, Long> {
    Optional<LineGroupMember> findByLineGroupAndUserId(LineGroup lineGroup, String userId);
    List<LineGroupMember> findByLineGroup(LineGroup lineGroup);
    List<LineGroupMember> findByLineGroupAndIsActiveTrue(LineGroup lineGroup);
    long countByLineGroupAndIsActiveTrue(LineGroup group);
    long countByLineGroup(LineGroup group);
}
