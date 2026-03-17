package com.example.helloworld.service.church;

import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.JwtTokenGateway;
import com.example.helloworld.util.ChurchJwtUtil;
import org.springframework.stereotype.Service;

@Service
public class ChurchJwtTokenGateway implements JwtTokenGateway {
    private final ChurchJwtUtil churchJwtUtil;

    public ChurchJwtTokenGateway(ChurchJwtUtil churchJwtUtil) {
        this.churchJwtUtil = churchJwtUtil;
    }

    @Override
    public AuthDomain domain() {
        return AuthDomain.CHURCH;
    }

    @Override
    public String extractUsername(String jwt) {
        return churchJwtUtil.extractUsername(jwt);
    }

    @Override
    public String extractSystem(String jwt) {
        return churchJwtUtil.extractSystem(jwt);
    }

    @Override
    public String extractTokenType(String jwt) {
        return churchJwtUtil.extractTokenType(jwt);
    }

    @Override
    public boolean validateAccessToken(String jwt, String username) {
        return churchJwtUtil.validateChurchAccessToken(jwt, username);
    }
}
