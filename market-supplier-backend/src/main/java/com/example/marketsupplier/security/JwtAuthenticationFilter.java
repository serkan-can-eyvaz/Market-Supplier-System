package com.example.marketsupplier.security;

import com.example.marketsupplier.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        System.out.println("[JwtAuthenticationFilter] Processing request: " + request.getMethod() + " " + request.getRequestURI());
        System.out.println("[JwtAuthenticationFilter] Authorization header: " + (authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : "null"));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[JwtAuthenticationFilter] No valid Authorization header found");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        userEmail = jwtService.extractUsername(jwt);
        
        System.out.println("[JwtAuthenticationFilter] Token received for user: " + userEmail);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            
            boolean isValid = jwtService.validateToken(jwt, userDetails.getUsername());
            System.out.println("[JwtAuthenticationFilter] Token validation result: " + isValid);
            
            if (isValid) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("[JwtAuthenticationFilter] Authentication set successfully for user: " + userEmail);
            } else {
                System.out.println("[JwtAuthenticationFilter] Token validation failed for user: " + userEmail);
                System.out.println("[JwtAuthenticationFilter] Token might be expired or invalid");
            }
        } else {
            System.out.println("[JwtAuthenticationFilter] User email is null or authentication already exists");
            if (userEmail == null) {
                System.out.println("[JwtAuthenticationFilter] User email is null");
            }
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                System.out.println("[JwtAuthenticationFilter] Authentication already exists");
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
