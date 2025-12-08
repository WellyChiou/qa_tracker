package com.example.helloworld.config;

import com.example.helloworld.service.church.ChurchUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class ChurchSecurityConfig {

    @Autowired
    @Qualifier("churchUserDetailsService")
    private ChurchUserDetailsService churchUserDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    @Qualifier("churchAuthenticationManager")
    public AuthenticationManager churchAuthenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(churchUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        
        return new ProviderManager(Arrays.asList(authProvider));
    }

    @Bean
    @Qualifier("churchDaoAuthenticationProvider")
    public DaoAuthenticationProvider churchDaoAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(churchUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}

