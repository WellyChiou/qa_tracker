package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.OpportunitySignalReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpportunitySignalReasonRepository extends JpaRepository<OpportunitySignalReason, Long> {

    List<OpportunitySignalReason> findBySignalIdOrderByScoreImpactDescSortOrderAsc(Long signalId);

    void deleteBySignalId(Long signalId);
}
