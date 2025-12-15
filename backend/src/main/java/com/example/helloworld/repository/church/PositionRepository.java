package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByPositionCode(String positionCode);
    
    Optional<Position> findByPositionName(String positionName);
    
    List<Position> findByIsActiveTrueOrderBySortOrderAsc();
    
    List<Position> findAllByOrderBySortOrderAsc();
}
