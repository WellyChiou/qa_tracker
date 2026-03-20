package com.example.helloworld.service.invest.auth;

import com.example.helloworld.entity.invest.auth.InvestUser;
import com.example.helloworld.repository.invest.auth.InvestUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InvestCurrentUserService {

    private final InvestUserRepository investUserRepository;

    public InvestCurrentUserService(InvestUserRepository investUserRepository) {
        this.investUserRepository = investUserRepository;
    }

    public String resolveCurrentUserUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "anonymous";
        }

        String principalName = authentication.getName();
        if (!StringUtils.hasText(principalName)) {
            return "anonymous";
        }

        return investUserRepository.findByUsername(principalName)
            .map(InvestUser::getUid)
            .orElse(principalName);
    }
}
