package com.example.marketsupplier.security;

import com.example.marketsupplier.util.LoggerUtility;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private LoggerUtility loggerUtility;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String clientIP = getClientIPAddress(request);
        String userAgent = request.getHeader("User-Agent");

        LoggerUtility.LogContext context = LoggerUtility.LogContext.create("JWT_AUTH_FILTER")
            .withMetadata("request_uri", requestURI)
            .withMetadata("method", method)
            .withMetadata("client_ip", clientIP);

        try {
            String token = getTokenFromRequest(request);
            
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                String[] roles = jwtTokenProvider.getRolesFromToken(token);
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());
                    
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    // Log successful authentication
                    auditLogService.logAuthentication(username, "JWT_AUTH", "SUCCESS", clientIP, userAgent);
                    loggerUtility.logSecurity("JWT authentication successful", context, 
                        java.util.Map.of("username", username, "roles", Arrays.toString(roles)));
                }
            } else if (token != null) {
                // Log failed authentication
                auditLogService.logAuthentication("unknown", "JWT_AUTH", "FAILED", clientIP, userAgent);
                loggerUtility.logSecurity("JWT authentication failed - invalid token", context, Map.of("operation", "jwt_auth_failed"));
            }
            
        } catch (Exception e) {
            // Log authentication error
            auditLogService.logAuthentication("unknown", "JWT_AUTH", "ERROR", clientIP, userAgent);
            loggerUtility.logError("JWT authentication error", context, e);
        }
        
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getClientIPAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}
