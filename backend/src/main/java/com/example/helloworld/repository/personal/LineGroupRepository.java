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
}
