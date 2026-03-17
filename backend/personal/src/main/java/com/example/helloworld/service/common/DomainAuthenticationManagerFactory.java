package com.example.helloworld.service.common;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DomainAuthenticationManagerFactory {

    public AuthenticationManager createAuthenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        return new ProviderManager(List.of(createDaoAuthenticationProvider(userDetailsService, passwordEncoder)));
    }

    public DaoAuthenticationProvider createDaoAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}
