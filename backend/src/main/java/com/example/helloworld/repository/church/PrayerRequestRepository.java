package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.PrayerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrayerRequestRepository extends JpaRepository<PrayerRequest, Long> {
    List<PrayerRequest> findByIsActiveTrueOrderByCreatedAtDesc();
    List<PrayerRequest> findAllByOrderByCreatedAtDesc();
}

