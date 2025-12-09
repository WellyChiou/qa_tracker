package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ChurchInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChurchInfoRepository extends JpaRepository<ChurchInfo, Long> {
    Optional<ChurchInfo> findByInfoKey(String infoKey);
    List<ChurchInfo> findByIsActiveTrueOrderByDisplayOrderAsc();
}

