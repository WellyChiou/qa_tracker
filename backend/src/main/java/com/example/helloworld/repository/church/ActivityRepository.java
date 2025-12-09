package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByIsActiveTrueOrderByActivityDateAsc();
    List<Activity> findByActivityDateGreaterThanEqualAndIsActiveTrueOrderByActivityDateAsc(LocalDate date);
    List<Activity> findAllByOrderByActivityDateAsc();
}

