package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByGroupName(String groupName);
    List<Group> findByIsActiveTrue();
    List<Group> findByGroupNameContainingIgnoreCase(String groupName);
}

