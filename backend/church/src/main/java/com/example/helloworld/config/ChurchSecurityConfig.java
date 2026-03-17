package com.example.helloworld.config;

import com.example.helloworld.filter.JwtAuthenticationFilter;
import com.example.helloworld.filter.UrlPermissionFilter;
import com.example.helloworld.service.common.AuthDomain;
import com.example.helloworld.service.common.AuthGateway;
import com.example.helloworld.service.common.DomainAuthenticationManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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

import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class ChurchSecurityConfig {
    @Autowired
    private AuthGateway authGateway;

    @Autowired
    private DomainAuthenticationManagerFactory authenticationManagerFactory;

    @Autowired(required = false)
    private UrlPermissionFilter urlPermissionFilter;

    @Autowired(required = false)
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain churchSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/church/auth/**").permitAll()
                        .requestMatchers("/api/church/public/**").permitAll()
                        .requestMatchers("/api/church/checkin/public/**").permitAll()
                        .requestMatchers("/api/church/line/**").permitAll()
                        .requestMatchers("/*.css", "/*.js", "/api.js", "/style.css").permitAll()
                        .requestMatchers("/", "/index.html").permitAll()
                        .requestMatchers("/api/church/**").authenticated()
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
                .userDetailsService(authGateway.userDetailsService(AuthDomain.CHURCH));

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        if (urlPermissionFilter != null) {
            http.addFilterBefore(urlPermissionFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    @Qualifier("churchAuthenticationManager")
    public AuthenticationManager churchAuthenticationManager() {
        return authenticationManagerFactory.createAuthenticationManager(
                authGateway.userDetailsService(AuthDomain.CHURCH),
                passwordEncoder()
        );
    }

    @Bean
    @Qualifier("churchDaoAuthenticationProvider")
    public DaoAuthenticationProvider churchDaoAuthenticationProvider() {
        return authenticationManagerFactory.createDaoAuthenticationProvider(
                authGateway.userDetailsService(AuthDomain.CHURCH),
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
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost",
                "http://localhost:80",
                "https://localhost",
                "https://localhost:80",
                "http://38.54.89.136",
                "http://38.54.89.136:80",
                "https://38.54.89.136",
                "https://38.54.89.136:443",
                "http://power-light-church.duckdns.org",
                "https://power-light-church.duckdns.org"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeCorsHeaders(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
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
