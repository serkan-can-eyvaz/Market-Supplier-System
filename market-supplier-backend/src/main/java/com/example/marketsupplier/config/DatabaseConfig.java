package com.example.marketsupplier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.example.marketsupplier.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    
    // JPA configuration is handled by application.properties
    // Additional database configurations can be added here if needed
}
