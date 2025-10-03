package com.example.marketsupplier.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:3600000}")
    private long jwtExpiration;

    @Value("${app.jwt.refresh-expiration:86400000}")
    private long jwtRefreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userPrincipal.getUsername());
    }

    public String generateTokenFromUsername(String username) {
        return generateTokenFromUsername(username, new HashMap<>());
    }

    public String generateTokenFromUsername(String username, Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateTokenWithRoles(String username, String[] roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("type", "access");
        return generateTokenFromUsername(username, claims);
    }

    public String generateTokenWithUserInfo(String username, String userId, String[] roles, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roles", roles);
        claims.put("type", "access");
        claims.putAll(additionalClaims);
        return generateTokenFromUsername(username, claims);
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String[] getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> rolesList = (java.util.List<String>) rolesObj;
            return rolesList.toArray(new String[0]);
        }
        // Fallback for single role
        Object roleObj = claims.get("role");
        if (roleObj instanceof String) {
            return new String[]{(String) roleObj};
        }
        return new String[0];
    }

    public String getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("userId");
    }

    public String getTokenType(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("type");
    }

    public boolean isRefreshToken(String token) {
        String type = getTokenType(token);
        return "refresh".equals(type);
    }

    public boolean isAccessToken(String token) {
        String type = getTokenType(token);
        return "access".equals(type);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    public long getRefreshExpirationTime() {
        return jwtRefreshExpiration;
    }

    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    public boolean hasRole(String token, String role) {
        String[] roles = getRolesFromToken(token);
        for (String userRole : roles) {
            if (userRole.equals(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyRole(String token, String... roles) {
        String[] userRoles = getRolesFromToken(token);
        for (String userRole : userRoles) {
            for (String requiredRole : roles) {
                if (userRole.equals(requiredRole)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasAllRoles(String token, String... roles) {
        String[] userRoles = getRolesFromToken(token);
        for (String requiredRole : roles) {
            boolean found = false;
            for (String userRole : userRoles) {
                if (userRole.equals(requiredRole)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public Map<String, Object> getTokenInfo(String token) {
        Map<String, Object> info = new HashMap<>();
        try {
            Claims claims = getAllClaimsFromToken(token);
            info.put("username", claims.getSubject());
            info.put("userId", claims.get("userId"));
            info.put("roles", claims.get("roles"));
            info.put("type", claims.get("type"));
            info.put("issuedAt", claims.getIssuedAt());
            info.put("expiration", claims.getExpiration());
            info.put("valid", true);
        } catch (Exception e) {
            info.put("valid", false);
            info.put("error", e.getMessage());
        }
        return info;
    }

    public String refreshToken(String refreshToken) {
        if (!isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        
        String username = getUsernameFromToken(refreshToken);
        String[] roles = getRolesFromToken(refreshToken);
        
        return generateTokenWithRoles(username, roles);
    }
}
