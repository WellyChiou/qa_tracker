package com.example.helloworld.repository.church.checkin;

import com.example.helloworld.entity.church.checkin.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findBySessionCode(String code);
    List<Session> findBySessionDate(LocalDate date);
}

