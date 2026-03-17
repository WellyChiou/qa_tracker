package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ServiceScheduleDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ServiceScheduleDateRepository extends JpaRepository<ServiceScheduleDate, Long> {
    Optional<ServiceScheduleDate> findByDate(LocalDate date);
}

