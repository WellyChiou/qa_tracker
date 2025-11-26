package com.example.helloworld.config;

import com.example.helloworld.filter.UrlPermissionFilter;
import com.example.helloworld.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired(required = false)
    private UrlPermissionFilter urlPermissionFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
                // ===== 公開的端點（所有人可訪問，無需認證）=====
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/hello").permitAll()
                .requestMatchers("/api/utils/**").permitAll() // 工具 API（生成密碼 hash 等）
                // 靜態資源和登入頁面
                .requestMatchers("/login.html").permitAll()
                .requestMatchers("/*.css", "/*.js", "/api.js", "/style.css").permitAll()
                .requestMatchers("/", "/index.html").permitAll()
                .requestMatchers("/expenses.html", "/tracker.html").permitAll()
                .requestMatchers("/admin/**").authenticated() // 管理頁面需要認證
                
                // ===== 管理端點（只有 ADMIN 可以訪問）=====
                // 注意：這些規則必須放在通用規則之前，因為 Spring Security 按順序匹配
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/roles/**").hasRole("ADMIN")
                .requestMatchers("/api/permissions/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // ===== 需要認證的 API 端點 =====
                // ADMIN 和 USER 都可以訪問這些端點
                .requestMatchers("/api/menus/**").authenticated()
                .requestMatchers("/api/records/**").authenticated()
                .requestMatchers("/api/expenses/**").authenticated()
                .requestMatchers("/api/assets/**").authenticated()
                .requestMatchers("/api/exchange-rates/**").authenticated()
                .requestMatchers("/api/config/**").authenticated()
                
                // ===== 所有其他 API 端點 =====
                // 如果有新的 API 端點，這裡會匹配
                // ADMIN 可以訪問所有 API（因為 ADMIN 角色包含所有權限）
                .requestMatchers("/api/**").authenticated()
                
                // ===== 其他 HTML 頁面 =====
                .requestMatchers("/*.html").authenticated()
                
                // ===== 其他所有請求 =====
                .anyRequest().authenticated()
            )
            // 禁用默認的表單登入，因為我們使用 REST API 登入
            // .formLogin() 會與 REST API 衝突導致重定向循環
            // 禁用 Spring Security 的登出處理器，使用 AuthController 的登出端點
            // 因為我們使用 REST API，不需要重定向，而是返回 JSON
            .logout(logout -> logout.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    // 如果是 API 請求，返回 JSON 錯誤
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"未授權\",\"authenticated\":false}");
                    } else {
                        // 否則重定向到登入頁
                        response.sendRedirect("/login.html");
                    }
                })
            )
            .userDetailsService(userDetailsService);
        
        // 添加動態 URL 權限 Filter（在認證檢查之前）
        // 如果數據庫中有配置，使用數據庫配置；否則使用上面的靜態配置
        if (urlPermissionFilter != null) {
            http.addFilterBefore(urlPermissionFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允許的來源（根據實際部署情況調整）
        // 使用具體域名，因為使用 credentials: 'include' 時不能使用 '*'
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost",
            "http://localhost:80",
            "http://38.54.89.136",
            "http://38.54.89.136:80",
            "https://38.54.89.136",
            "https://38.54.89.136:443"
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("*"));
        
        // 啟用憑證支援（用於 session cookie）
        // 注意：當 allowCredentials 為 true 時，allowedOrigins 不能包含 "*"
        configuration.setAllowCredentials(true);
        
        // 預檢請求的緩存時間（秒）
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

