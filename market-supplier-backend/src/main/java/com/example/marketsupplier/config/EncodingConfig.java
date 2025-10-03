package com.example.marketsupplier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class EncodingConfig implements WebMvcConfigurer {

    // Spring Boot 3 already registers CharacterEncodingFilter (UTF-8) via HttpEncodingAutoConfiguration
    // Extra bean causes name conflict; keep only message converters below.

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Ensure String responses use UTF-8
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));

        // Ensure JSON uses UTF-8 explicitly (Spring defaults to UTF-8, this is explicit)
        MappingJackson2HttpMessageConverter json = new MappingJackson2HttpMessageConverter();
        json.setDefaultCharset(StandardCharsets.UTF_8);
        converters.add(json);
    }
}


