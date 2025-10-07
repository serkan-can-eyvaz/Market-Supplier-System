package com.example.marketsupplier.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/utils/**").permitAll()
                .requestMatchers("/api/orders/status/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/suppliers/check-company-name").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                // Admin only endpoints
                .requestMatchers("/api/users/**").hasAnyRole("ADMIN")
                .requestMatchers("/api/markets/stats").hasAnyRole("ADMIN", "SUPPLIER")
                .requestMatchers("/api/suppliers/stats").hasAnyRole("ADMIN", "SUPPLIER")
                .requestMatchers("/api/orders/stats").hasAnyRole("ADMIN", "SUPPLIER")
                .requestMatchers("/api/deliveries/stats").hasAnyRole("ADMIN", "SUPPLIER")
                // Market endpoints - tedarikçi market oluşturabilir/yönetebilir
                .requestMatchers("/api/markets/**").hasAnyRole("SUPPLIER", "ADMIN")
                .requestMatchers("/api/orders/**").hasAnyRole("MARKET", "SUPPLIER", "ADMIN")
                // Supplier endpoints
                .requestMatchers("/api/suppliers/**").hasAnyRole("SUPPLIER", "ADMIN")
                .requestMatchers("/api/products/**").hasAnyRole("SUPPLIER", "ADMIN", "MARKET")
                .requestMatchers("/api/deliveries/**").hasAnyRole("SUPPLIER", "ADMIN")
                .requestMatchers("/api/cart/**").hasAnyRole("MARKET", "ADMIN")
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider());
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Content-Disposition"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
