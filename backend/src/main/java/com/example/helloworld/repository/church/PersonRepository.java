package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByPersonName(String personName);
    
    Optional<Person> findByMemberNo(String memberNo);
    
    List<Person> findByIsActiveTrueOrderByPersonNameAsc();
    
    List<Person> findAllByOrderByPersonNameAsc();
    
    @Query("SELECT p FROM Person p WHERE p.isActive = true AND p.personName LIKE %:keyword% ORDER BY p.personName ASC")
    List<Person> searchActivePersons(String keyword);
    
    List<Person> findByGroupId(Long groupId);
    List<Person> findByGroupIdAndIsActiveTrue(Long groupId);
}

