package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.LineGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LineGroupRepository extends JpaRepository<LineGroup, String> {
    Optional<LineGroup> findByGroupId(String groupId);
    List<LineGroup> findByIsActiveTrue();
    List<LineGroup> findByGroupCodeAndIsActiveTrue(String groupCode);
    Optional<LineGroup> findByGroupIdAndGroupCode(String groupId, String groupCode);
}
