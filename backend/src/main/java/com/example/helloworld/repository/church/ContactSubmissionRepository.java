package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ContactSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactSubmissionRepository extends JpaRepository<ContactSubmission, Long> {
    Page<ContactSubmission> findAllByOrderBySubmittedAtDesc(Pageable pageable);
    Page<ContactSubmission> findByIsReadOrderBySubmittedAtDesc(Boolean isRead, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM ContactSubmission c WHERE c.isRead = :isRead")
    Long countByIsRead(@Param("isRead") Boolean isRead);
}

