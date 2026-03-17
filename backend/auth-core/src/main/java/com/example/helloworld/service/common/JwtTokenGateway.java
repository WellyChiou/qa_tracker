package com.example.helloworld.service.common;

public interface JwtTokenGateway {
    AuthDomain domain();

    String extractUsername(String jwt);

    String extractSystem(String jwt);

    String extractTokenType(String jwt);

    boolean validateAccessToken(String jwt, String username);
}
