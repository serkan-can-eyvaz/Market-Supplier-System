package com.example.marketsupplier.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long jwtExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenService(
            @Value("${app.jwt.secret:mySecretKey}") String jwtSecret,
            @Value("${app.jwt.expiration:3600000}") long jwtExpirationMs,
            @Value("${app.jwt.refresh-expiration:86400000}") long refreshExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(String phone, String role) {
        System.out.println("[JwtTokenService] Generating token for phone: " + phone + ", role: " + role);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("phone", phone);
        claims.put("role", role);
        claims.put("roles", new String[]{role}); // Add roles array for compatibility
        claims.put("type", "access");

        String token = createToken(claims, phone, jwtExpirationMs);
        System.out.println("[JwtTokenService] Generated token: " + token);
        return token;
    }

    public String generateRefreshToken(String phone) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("phone", phone);
        claims.put("type", "refresh");

        return createToken(claims, phone, refreshExpirationMs);
    }
    
    public String generateRefreshToken(String phone, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("phone", phone);
        claims.put("role", role);
        claims.put("type", "refresh");

        return createToken(claims, phone, refreshExpirationMs);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        System.out.println("[JwtTokenService] Creating token for subject: " + subject + ", expiration: " + expiration);
        
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration);

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(secretKey, SignatureAlgorithm.HS512)
                    .compact();
            
            System.out.println("[JwtTokenService] Token created successfully: " + token);
            return token;
        } catch (Exception e) {
            System.err.println("[JwtTokenService] Error creating token: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("Invalid JWT token: " + e.getMessage());
        }
    }
    
    // Backward compatibility
    public String getPhoneFromToken(String token) {
        return getUsernameFromToken(token);
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("Invalid JWT token: " + e.getMessage());
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }
    
    public String getTokenType(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("type", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }


    public static class JwtAuthenticationException extends RuntimeException {
        public JwtAuthenticationException(String message) {
            super(message);
        }
    }
}
