package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.PageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageContentRepository extends JpaRepository<PageContent, Long> {
    Optional<PageContent> findByPageCode(String pageCode);
    Optional<PageContent> findByPageCodeAndIsActiveTrue(String pageCode);
}

