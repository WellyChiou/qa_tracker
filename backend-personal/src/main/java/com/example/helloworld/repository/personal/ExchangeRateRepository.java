package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByDate(LocalDate date);
    
    @Query(value = "SELECT * FROM exchange_rates WHERE date <= :date ORDER BY date DESC LIMIT 1", nativeQuery = true)
    Optional<ExchangeRate> findLatestByDate(@Param("date") LocalDate date);
}
