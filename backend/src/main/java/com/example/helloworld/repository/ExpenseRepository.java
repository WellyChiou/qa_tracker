package com.example.helloworld.repository;

import com.example.helloworld.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Optional<Expense> findByFirebaseId(String firebaseId);
    
    Page<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    List<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT e FROM Expense e WHERE " +
           "(:year IS NULL OR YEAR(e.date) = :year) AND " +
           "(:month IS NULL OR MONTH(e.date) = :month) AND " +
           "(:member IS NULL OR e.member = :member) AND " +
           "(:type IS NULL OR e.type = :type) AND " +
           "(:mainCategory IS NULL OR e.mainCategory = :mainCategory) " +
           "ORDER BY e.date DESC, e.createdAt DESC")
    Page<Expense> findByFilters(
        @Param("year") Integer year,
        @Param("month") Integer month,
        @Param("member") String member,
        @Param("type") String type,
        @Param("mainCategory") String mainCategory,
        Pageable pageable
    );
    
    @Query("SELECT e FROM Expense e WHERE " +
           "(:year IS NULL OR YEAR(e.date) = :year) AND " +
           "(:month IS NULL OR MONTH(e.date) = :month) AND " +
           "(:member IS NULL OR e.member = :member) AND " +
           "(:type IS NULL OR e.type = :type) AND " +
           "(:mainCategory IS NULL OR e.mainCategory = :mainCategory) " +
           "ORDER BY e.date DESC, e.createdAt DESC")
    List<Expense> findByFiltersList(
        @Param("year") Integer year,
        @Param("month") Integer month,
        @Param("member") String member,
        @Param("type") String type,
        @Param("mainCategory") String mainCategory
    );
}

