package com.example.marketsupplier.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    
    @Value("${app.jwt.secret}")
    private String secretKey;
    
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;
    
    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    // Extract expiration date from token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    // Extract claim from token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    // Generate token for user
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        // Get user role from database
        try {
            // This is a simplified approach - in a real app you'd inject UserService
            claims.put("role", "SUPPLIER"); // Default role for now
            claims.put("roles", new String[]{"SUPPLIER"});
        } catch (Exception e) {
            claims.put("role", "SUPPLIER");
            claims.put("roles", new String[]{"SUPPLIER"});
        }
        return createToken(claims, username);
    }
    
    // Generate token with extra claims
    public String generateToken(Map<String, Object> extraClaims, String username) {
        return createToken(extraClaims, username);
    }
    
    // Create token
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    // Check if token is valid
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username)) && !isTokenExpired(token);
    }
    
    // Check if token is expired
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    // Extract all claims from token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    // Get signing key
    private Key getSignInKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    // Get token expiration time in milliseconds
    public long getExpirationTime() {
        return jwtExpiration;
    }
}
