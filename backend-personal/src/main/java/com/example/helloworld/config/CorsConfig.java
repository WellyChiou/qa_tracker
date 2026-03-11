package com.example.helloworld.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 配置（已由 SecurityConfig 統一處理，此類保留作為備用）
 * 如果 SecurityConfig 中的 CORS 配置不工作，可以啟用此配置
 */
@Configuration
public class CorsConfig {

    // 註釋掉以使用 SecurityConfig 中的 CORS 配置
    // 如果 SecurityConfig 的 CORS 不工作，可以取消註釋此方法
    /*
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 使用具體域名而不是 "*"，因為 allowCredentials(true) 時不能使用 "*"
        config.addAllowedOrigin("http://localhost");
        config.addAllowedOrigin("http://localhost:80");
        config.addAllowedOrigin("http://38.54.89.136");
        config.addAllowedOrigin("http://38.54.89.136:80");
        
        // 允許所有 HTTP 方法
        config.addAllowedMethod("*");
        
        // 允許所有請求頭
        config.addAllowedHeader("*");
        
        // 允許發送憑證（如果需要）
        config.setAllowCredentials(true);
        
        // 預檢請求的緩存時間（秒）
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
    */
}

