package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ChurchLineGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChurchLineGroupRepository extends JpaRepository<ChurchLineGroup, String> {
    Optional<ChurchLineGroup> findByGroupId(String groupId);
    
    List<ChurchLineGroup> findByIsActiveTrue();
}

