package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.PositionPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionPersonRepository extends JpaRepository<PositionPerson, Long> {
    List<PositionPerson> findByPositionId(Long positionId);
    
    List<PositionPerson> findByPositionIdAndDayType(Long positionId, String dayType);
    
    List<PositionPerson> findByPersonId(Long personId);
    
    Optional<PositionPerson> findByPositionIdAndPersonIdAndDayType(Long positionId, Long personId, String dayType);
    
    void deleteByPositionIdAndPersonIdAndDayType(Long positionId, Long personId, String dayType);
    
    @Query("SELECT pp FROM PositionPerson pp JOIN FETCH pp.person WHERE pp.position.id = :positionId AND pp.dayType = :dayType ORDER BY pp.sortOrder ASC, pp.person.personName ASC")
    List<PositionPerson> findByPositionIdAndDayTypeOrdered(@Param("positionId") Long positionId, @Param("dayType") String dayType);
    
    @Query("SELECT pp FROM PositionPerson pp JOIN FETCH pp.person WHERE pp.position.id = :positionId ORDER BY pp.dayType ASC, pp.sortOrder ASC, pp.person.personName ASC")
    List<PositionPerson> findByPositionIdOrdered(@Param("positionId") Long positionId);
}

