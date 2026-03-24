package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.LineGroup;
import com.example.helloworld.entity.invest.LineGroupMember;
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
