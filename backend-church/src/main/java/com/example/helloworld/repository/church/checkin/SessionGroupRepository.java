package com.example.helloworld.repository.church.checkin;

import com.example.helloworld.entity.church.checkin.SessionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionGroupRepository extends JpaRepository<SessionGroup, Long> {
    List<SessionGroup> findBySessionId(Long sessionId);
    List<SessionGroup> findByGroupId(Long groupId);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SessionGroup sg WHERE sg.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SessionGroup sg WHERE sg.sessionId = :sessionId AND sg.groupId = :groupId")
    void deleteBySessionIdAndGroupId(@Param("sessionId") Long sessionId, @Param("groupId") Long groupId);
}

