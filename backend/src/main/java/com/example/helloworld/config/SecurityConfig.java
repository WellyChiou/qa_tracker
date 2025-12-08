package com.example.helloworld.config;

import com.example.helloworld.filter.UrlPermissionFilter;
import com.example.helloworld.service.CustomUserDetailsService;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 使用 IF_REQUIRED 以支持 session 認證
                    .sessionFixation().migrateSession() // 遷移 session，提高安全性
            )
            .authorizeHttpRequests(auth -> auth
                // ===== 公開的端點（所有人可訪問，無需認證）=====
                // 注意：必須按照從具體到通用的順序排列
                .requestMatchers("/api/church/auth/**").permitAll() // 教會認證 API（公開訪問）
                .requestMatchers("/api/church/menus/frontend").permitAll() // 教會前台菜單（公開訪問）
                .requestMatchers("/api/church/service-schedules").permitAll() // 教會服事表查詢（公開訪問）
                .requestMatchers("/api/church/persons").permitAll() // 教會人員查詢（公開訪問）
                .requestMatchers("/api/church/positions").permitAll() // 教會崗位查詢（公開訪問）
                .requestMatchers("/api/church/positions/config/**").permitAll() // 教會崗位配置（公開訪問）
                .requestMatchers("/api/church/**").authenticated() // 其他教會 API 需要認證
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/hello").permitAll()
                .requestMatchers("/api/utils/**").permitAll() // 工具 API（生成密碼 hash 等）
                .requestMatchers("/api/line/**").permitAll() // LINE Bot Webhook（LINE 平台會直接調用，無需認證）
                // 靜態資源和前端入口
                .requestMatchers("/*.css", "/*.js", "/api.js", "/style.css").permitAll()
                .requestMatchers("/", "/index.html").permitAll() // Vue SPA 入口文件
                .requestMatchers("/admin/**").authenticated() // 管理頁面需要認證
                
                // ===== 管理端點（只有 ADMIN 可以訪問）=====
                // 注意：這些規則必須放在通用規則之前，因為 Spring Security 按順序匹配
                // 用戶管理端點：要求認證，具體權限檢查在 Controller 層面進行
                // LINE 帳號綁定相關端點允許已認證的用戶訪問自己的帳號（通過 Controller 的 hasPermission 檢查）
                // 其他用戶管理端點需要 ADMIN 角色（通過 @PreAuthorize 註解檢查）
                .requestMatchers("/api/users/**").authenticated()
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
            .httpBasic(httpBasic -> httpBasic.disable()) // 禁用 HTTP Basic 認證，避免重定向
            .formLogin(formLogin -> formLogin.disable()) // 明確禁用表單登入
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String requestURI = request.getRequestURI();
                    // 如果是 API 請求（包括教會 API），返回 JSON 錯誤，不要重定向
                    if (requestURI.startsWith("/api/")) {
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
            "http://wc-project.duckdns.org",
            "https://wc-project.duckdns.org"
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

