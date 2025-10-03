package com.example.marketsupplier.config;

import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("[CustomUserDetailsService] Loading user by email: " + email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                System.out.println("[CustomUserDetailsService] User not found with email: " + email);
                return new UsernameNotFoundException("User not found with email: " + email);
            });
        
        System.out.println("[CustomUserDetailsService] User found: " + user.getName() + ", Role: " + user.getRole());
        System.out.println("[CustomUserDetailsService] User password hash: " + user.getPassword());
        
        return new CustomUserPrincipal(user);
    }
    
    // Custom UserPrincipal class
    public static class CustomUserPrincipal implements UserDetails {
        private final User user;
        
        public CustomUserPrincipal(User user) {
            this.user = user;
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }
        
        @Override
        public String getPassword() {
            return user.getPassword();
        }
        
        @Override
        public String getUsername() {
            return user.getEmail();
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return true;
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return true;
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }
        
        @Override
        public boolean isEnabled() {
            return true;
        }
        
        // Getter for User entity
        public User getUser() {
            return user;
        }
    }
}
