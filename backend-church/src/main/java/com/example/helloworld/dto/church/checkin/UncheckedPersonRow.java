package com.example.helloworld.dto.church.checkin;

/**
 * Interface projection for UncheckedPersonRow
 * Used for mapping native SQL query results in Spring Data JPA
 */
public interface UncheckedPersonRow {
    Long getId();
    String getMemberNo();
    String getMemberName();
}

