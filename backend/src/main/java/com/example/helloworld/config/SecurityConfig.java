package com.example.helloworld.config;

import com.example.helloworld.filter.JwtAuthenticationFilter;
import com.example.helloworld.filter.UrlPermissionFilter;
import com.example.helloworld.service.personal.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
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
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired(required = false)
    private UrlPermissionFilter urlPermissionFilter;

    @Autowired(required = false)
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 使用 STATELESS 以支持 JWT Token 認證
            )
            .authorizeHttpRequests(auth -> auth
                // ===== 必須公開的端點（這些不應該由資料庫控制）=====
                // 認證相關 API 必須公開，否則無法登入
                .requestMatchers("/api/church/auth/**").permitAll() // 教會認證 API
                .requestMatchers("/api/auth/**").permitAll() // 個人網站認證 API
                .requestMatchers("/api/public/**").permitAll() // 明確標記為公開的 API
                .requestMatchers("/api/hello").permitAll() // Hello API
                .requestMatchers("/api/utils/**").permitAll() // 工具 API（生成密碼 hash 等）
                .requestMatchers("/api/line/**").permitAll() // LINE Bot Webhook（LINE 平台會直接調用，無需認證）
                
                // ===== 靜態資源和前端入口 =====
                .requestMatchers("/*.css", "/*.js", "/api.js", "/style.css").permitAll()
                .requestMatchers("/", "/index.html").permitAll() // Vue SPA 入口文件
                
                // ===== 管理頁面 =====
                .requestMatchers("/admin/**").authenticated() // 管理頁面需要認證
                
                // ===== 所有 API 端點（由 UrlPermissionFilter 動態控制）=====
                // 包括 URL 權限管理 API 也可以由資料庫動態控制
                // 建議在資料庫中預先配置這些端點的權限（例如：需要 ADMIN 角色）
                // 如果資料庫中沒有配置，則使用預設規則（需要認證）
                // 注意：這裡設定為 authenticated() 作為預設，但 UrlPermissionFilter 會：
                // 1. 如果資料庫中配置了 is_public = 1，會設置認證狀態讓請求通過
                // 2. 如果資料庫中配置了需要角色/權限，會進行檢查
                // 3. 如果資料庫中沒有配置，則使用這個預設規則（需要認證）
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
            .httpBasic(httpBasic -> httpBasic.disable()) // 禁用 HTTP Basic 認證，避免重定向
            .formLogin(formLogin -> formLogin.disable()) // 明確禁用表單登入
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String requestURI = request.getRequestURI();
                    // 如果是 API 請求（包括教會 API），返回 JSON 錯誤，不要重定向
                    if (requestURI.startsWith("/api/")) {
                        // 設置 CORS headers，確保 401 響應也能通過 CORS 檢查
                        String origin = request.getHeader("Origin");
                        if (origin != null) {
                            response.setHeader("Access-Control-Allow-Origin", origin);
                            response.setHeader("Access-Control-Allow-Credentials", "true");
                            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
                            response.setHeader("Access-Control-Allow-Headers", "*");
                        }
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"未授權\",\"authenticated\":false}");
                        response.getWriter().flush();
                    } else {
                        // 否則重定向到登入頁（使用相對路徑，自動匹配當前協議）
                        // 前端已改為 Vue SPA，登入頁面是路由 /login，重定向到根路徑讓 Vue Router 處理
                        // 注意：對於教會網站前端，不應該重定向到登入頁
                        // 因為教會網站是公開的，不需要登入
                        // 使用相對路徑，讓瀏覽器自動匹配協議
                        String redirectUrl = "/";
                        response.sendRedirect(redirectUrl);
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String requestURI = request.getRequestURI();
                    // 如果是 API 請求，返回 JSON 錯誤
                    if (requestURI.startsWith("/api/")) {
                        // 設置 CORS headers，確保 403 響應也能通過 CORS 檢查
                        String origin = request.getHeader("Origin");
                        if (origin != null) {
                            response.setHeader("Access-Control-Allow-Origin", origin);
                            response.setHeader("Access-Control-Allow-Credentials", "true");
                            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
                            response.setHeader("Access-Control-Allow-Headers", "*");
                        }
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"禁止訪問\",\"authenticated\":true}");
                        response.getWriter().flush();
                    } else {
                        response.sendRedirect("/");
                    }
                })
            )
            .userDetailsService(userDetailsService);
        
        // 添加 JWT Filter（在認證檢查之前）
        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }
        
        // 添加動態 URL 權限 Filter（在 JWT Filter 之後）
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
    @org.springframework.beans.factory.annotation.Qualifier("defaultAuthenticationManager")
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        
        return new ProviderManager(Arrays.asList(authProvider));
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
            "https://38.54.89.136:443",
            "http://power-light-church.duckdns.org",
            "https://power-light-church.duckdns.org"
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

