package com.example.helloworld.config;

import com.example.helloworld.filter.JwtAuthenticationFilter;
import com.example.helloworld.filter.UrlPermissionFilter;
import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.AuthGateway;
import com.example.helloworld.service.common.DomainAuthenticationManagerFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class InvestSecurityConfig {

    private final AuthGateway authGateway;
    private final DomainAuthenticationManagerFactory authenticationManagerFactory;
    private final UrlPermissionFilter urlPermissionFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final List<String> allowedOriginPatterns;

    public InvestSecurityConfig(AuthGateway authGateway,
                                DomainAuthenticationManagerFactory authenticationManagerFactory,
                                UrlPermissionFilter urlPermissionFilter,
                                JwtAuthenticationFilter jwtAuthenticationFilter,
                                @Value("${invest.cors.allowed-origin-patterns:http://localhost,http://localhost:80,https://localhost,https://localhost:80,http://127.0.0.1,http://127.0.0.1:80,https://power-light-church.duckdns.org,http://power-light-church.duckdns.org,https://38.54.89.136,http://38.54.89.136}") String allowedOriginPatterns) {
        this.authGateway = authGateway;
        this.authenticationManagerFactory = authenticationManagerFactory;
        this.urlPermissionFilter = urlPermissionFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    @Bean
    public SecurityFilterChain investSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/invest/auth/**").permitAll()
                .requestMatchers("/api/invest/hello").permitAll()
                .requestMatchers("/api/invest/**").authenticated()
                .anyRequest().permitAll()
            )
            .logout(logout -> logout.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        writeCorsHeaders(request, response);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"未授權\",\"authenticated\":false}");
                        response.getWriter().flush();
                        return;
                    }

                    response.sendRedirect("/");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        writeCorsHeaders(request, response);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"禁止訪問\",\"authenticated\":true}");
                        response.getWriter().flush();
                        return;
                    }

                    response.sendRedirect("/");
                })
            )
            .userDetailsService(authGateway.userDetailsService(AuthDomain.INVEST));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(urlPermissionFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Qualifier("investAuthenticationManager")
    public AuthenticationManager investAuthenticationManager() {
        return authenticationManagerFactory.createAuthenticationManager(
            authGateway.userDetailsService(AuthDomain.INVEST),
            passwordEncoder()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeCorsHeaders(jakarta.servlet.http.HttpServletRequest request,
                                  jakarta.servlet.http.HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            return;
        }

        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "*");
    }
}
