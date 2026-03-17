package com.example.helloworld.config;

import com.example.helloworld.service.common.DomainAuthenticationManagerFactory;
import com.example.helloworld.service.common.TokenBlacklistService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonalAuthBeanConfig {

    @Bean
    public TokenBlacklistService tokenBlacklistService() {
        return new TokenBlacklistService();
    }

    @Bean
    public DomainAuthenticationManagerFactory domainAuthenticationManagerFactory() {
        return new DomainAuthenticationManagerFactory();
    }
}
