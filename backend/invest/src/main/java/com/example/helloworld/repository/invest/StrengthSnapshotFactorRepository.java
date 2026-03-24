package com.example.helloworld.repository.invest;

import com.example.helloworld.entity.invest.StrengthSnapshotFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StrengthSnapshotFactorRepository extends JpaRepository<StrengthSnapshotFactor, Long> {

    List<StrengthSnapshotFactor> findBySnapshotIdOrderByScoreContributionDescSortOrderAsc(Long snapshotId);

    void deleteBySnapshotId(Long snapshotId);
}
