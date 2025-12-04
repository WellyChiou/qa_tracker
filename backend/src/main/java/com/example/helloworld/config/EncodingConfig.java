package com.example.helloworld.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class EncodingConfig implements WebMvcConfigurer {

    /**
     * 配置 HTTP 消息轉換器，確保字符串響應使用 UTF-8
     * 注意：characterEncodingFilter 由 Spring Boot 自動配置（通過 application.properties 中的 server.servlet.encoding.* 配置）
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converters.add(stringConverter);
    }
}

