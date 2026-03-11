package com.example.helloworld.service.personal;

import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.JwtTokenGateway;
import com.example.helloworld.util.PersonalJwtUtil;
import org.springframework.stereotype.Service;

@Service
public class PersonalJwtTokenGateway implements JwtTokenGateway {
    private final PersonalJwtUtil personalJwtUtil;

    public PersonalJwtTokenGateway(PersonalJwtUtil personalJwtUtil) {
        this.personalJwtUtil = personalJwtUtil;
    }

    @Override
    public AuthDomain domain() {
        return AuthDomain.PERSONAL;
    }

    @Override
    public String extractUsername(String jwt) {
        return personalJwtUtil.extractUsername(jwt);
    }

    @Override
    public String extractSystem(String jwt) {
        return personalJwtUtil.extractSystem(jwt);
    }

    @Override
    public String extractTokenType(String jwt) {
        return personalJwtUtil.extractTokenType(jwt);
    }

    @Override
    public boolean validateAccessToken(String jwt, String username) {
        return personalJwtUtil.validatePersonalAccessToken(jwt, username);
    }
}
