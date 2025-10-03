package com.example.marketsupplier.config;

import com.example.marketsupplier.service.JwtService;
import com.example.marketsupplier.service.JwtTokenService;
import com.example.marketsupplier.service.RateLimitService;
import com.example.marketsupplier.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private JwtTokenService jwtTokenService;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/utils/**").permitAll()
                .requestMatchers("/api/whatsapp/webhook").permitAll() // Allow both GET and POST for WhatsApp
                .requestMatchers("/webhook/whatsapp").permitAll() // WhatsApp webhook
                .requestMatchers("/api/n8n/**").permitAll() // N8n workflow endpoints - PUBLIC for testing
                .requestMatchers("/api/products/ai/**").permitAll()
                .requestMatchers("/api/ai/**").permitAll()
                .requestMatchers("/api/orders/status/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/suppliers/check-company-name").permitAll()
                // Harita sayfasında başlangıç adresini okuyabilmek için, geçici olarak public
                .requestMatchers("/api/suppliers/my-supplier").permitAll()
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
                .requestMatchers("/api/products/**").hasAnyRole("SUPPLIER", "ADMIN")
                .requestMatchers("/api/deliveries/**").hasAnyRole("SUPPLIER", "ADMIN")
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
