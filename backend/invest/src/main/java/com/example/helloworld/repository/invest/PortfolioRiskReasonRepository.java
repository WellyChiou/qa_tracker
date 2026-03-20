package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.PortfolioRiskReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRiskReasonRepository extends JpaRepository<PortfolioRiskReason, Long> {
    List<PortfolioRiskReason> findByRiskResultId(Long riskResultId);

    List<PortfolioRiskReason> findByRiskResultIdIn(List<Long> riskResultIds);

    void deleteByRiskResultId(Long riskResultId);
}
