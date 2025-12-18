package com.example.helloworld.dto.church;


public record ServiceUpdatePayload(
        String dateText,
        String dayText,
        String positionName,
        String beforeName,
        String afterName,
        String googleText,
        boolean testMode
) {

}