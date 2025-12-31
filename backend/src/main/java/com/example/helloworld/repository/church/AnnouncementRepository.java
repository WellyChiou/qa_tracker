package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByIsActiveTrueOrderByIsPinnedDescPublishDateDesc();
    List<Announcement> findAllByOrderByIsPinnedDescPublishDateDesc();
}

