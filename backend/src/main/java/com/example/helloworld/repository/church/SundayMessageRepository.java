package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.SundayMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SundayMessageRepository extends JpaRepository<SundayMessage, Long> {
    List<SundayMessage> findByIsActiveTrueOrderByServiceDateDesc();
    List<SundayMessage> findAllByOrderByServiceDateDesc();
    Optional<SundayMessage> findByServiceDateAndServiceType(LocalDate serviceDate, String serviceType);
}

