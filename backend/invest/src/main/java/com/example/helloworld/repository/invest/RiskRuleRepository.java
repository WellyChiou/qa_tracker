package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.RiskRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskRuleRepository extends JpaRepository<RiskRule, Long> {
    List<RiskRule> findAllByOrderByIdAsc();

    Optional<RiskRule> findByRuleCode(String ruleCode);
}
