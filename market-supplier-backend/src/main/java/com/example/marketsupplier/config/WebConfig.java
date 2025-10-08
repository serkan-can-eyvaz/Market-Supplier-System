package com.example.marketsupplier.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web konfigürasyonu - CORS SecurityConfig'te handle ediliyor
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS yapılandırması SecurityConfig'te centralized olarak yapılıyor
    // Duplicate CORS header problemini önlemek için burada CORS mapping yok
}
