package com.example.marketsupplier.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date issuedAt = new Date(System.currentTimeMillis());
        Date expirationDate = new Date(System.currentTimeMillis() + expiration);
        
        System.out.println("[JwtService] Creating token:");
        System.out.println("[JwtService] Subject: " + subject);
        System.out.println("[JwtService] Issued at: " + issuedAt);
        System.out.println("[JwtService] Expires at: " + expirationDate);
        System.out.println("[JwtService] Expiration duration: " + expiration + " ms (" + (expiration / 1000 / 60 / 60) + " hours)");
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(issuedAt)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, String email) {
        try {
            final String username = extractUsername(token);
            final boolean usernameMatch = username.equals(email);
            final boolean notExpired = !isTokenExpired(token);
            
            System.out.println("[JwtService] Token validation - Username match: " + usernameMatch + ", Not expired: " + notExpired);
            System.out.println("[JwtService] Token validation - Expected: " + email + ", Actual: " + username);
            
            return (usernameMatch && notExpired);
        } catch (Exception e) {
            System.out.println("[JwtService] Token validation failed with exception: " + e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            Date now = new Date();
            boolean expired = expiration.before(now);
            
            System.out.println("[JwtService] Token expiration check:");
            System.out.println("[JwtService] Token expires at: " + expiration);
            System.out.println("[JwtService] Current time: " + now);
            System.out.println("[JwtService] Is expired: " + expired);
            
            return expired;
        } catch (Exception e) {
            System.out.println("[JwtService] Error checking token expiration: " + e.getMessage());
            return true; // If we can't check, consider it expired
        }
    }
}
