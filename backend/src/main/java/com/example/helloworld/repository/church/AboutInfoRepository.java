package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.AboutInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AboutInfoRepository extends JpaRepository<AboutInfo, Long> {
    List<AboutInfo> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<AboutInfo> findBySectionKeyAndIsActiveTrue(String sectionKey);
}

