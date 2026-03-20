package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.PriceUpdateJobDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceUpdateJobDetailRepository extends JpaRepository<PriceUpdateJobDetail, Long> {
}
