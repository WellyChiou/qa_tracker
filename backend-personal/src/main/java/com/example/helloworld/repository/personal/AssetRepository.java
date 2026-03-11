package com.example.helloworld.repository.personal;

import com.example.helloworld.entity.personal.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByFirebaseId(String firebaseId);
    
    List<Asset> findByOrderByOrderIndexAsc();
    
    @Query("SELECT a FROM Asset a ORDER BY a.orderIndex ASC, a.createdAt ASC")
    List<Asset> findAllOrdered();
}
