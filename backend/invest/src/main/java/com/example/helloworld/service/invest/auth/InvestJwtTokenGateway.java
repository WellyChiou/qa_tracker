package com.example.helloworld.service.invest.auth;

import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.JwtTokenGateway;
import com.example.helloworld.util.InvestJwtUtil;
import org.springframework.stereotype.Service;

@Service
public class InvestJwtTokenGateway implements JwtTokenGateway {

    private final InvestJwtUtil investJwtUtil;

    public InvestJwtTokenGateway(InvestJwtUtil investJwtUtil) {
        this.investJwtUtil = investJwtUtil;
    }

    @Override
    public AuthDomain domain() {
        return AuthDomain.INVEST;
    }

    @Override
    public String extractUsername(String jwt) {
        return investJwtUtil.extractUsername(jwt);
    }

    @Override
    public String extractSystem(String jwt) {
        return investJwtUtil.extractSystem(jwt);
    }

    @Override
    public String extractTokenType(String jwt) {
        return investJwtUtil.extractTokenType(jwt);
    }

    @Override
    public boolean validateAccessToken(String jwt, String username) {
        return investJwtUtil.validateInvestAccessToken(jwt, username);
    }
}
