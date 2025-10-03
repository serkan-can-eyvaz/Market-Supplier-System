package com.example.marketsupplier.controller;

import com.example.marketsupplier.dto.*;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.entity.UserRole;
import com.example.marketsupplier.service.AuthService;
import com.example.marketsupplier.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;
    
    // Get all users (Admin only)
    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            List<User> users = userService.getAllUsers();
            List<UserResponse> userResponses = users.stream()
                .map(user -> new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(userResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve users: " + e.getMessage()));
        }
    }
    
    // Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            // Check if user is admin or accessing their own profile
            if (!isAdmin(token) && !isOwnProfile(id, token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            var userOptional = userService.findById(id);
            if (userOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOptional.get();
            UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
            );
            
            return ResponseEntity.ok(userResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve user: " + e.getMessage()));
        }
    }
    
    // Get users by role
    @GetMapping("/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable UserRole role, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            List<User> users = userService.getUsersByRole(role);
            List<UserResponse> userResponses = users.stream()
                .map(user -> new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(userResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve users: " + e.getMessage()));
        }
    }
    
    // Search users by name
    @GetMapping("/search")
    public ResponseEntity<?> searchUsersByName(@RequestParam String name, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            List<User> users = userService.searchUsersByName(name);
            List<UserResponse> userResponses = users.stream()
                .map(user -> new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(userResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to search users: " + e.getMessage()));
        }
    }
    
    // Update user
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, 
                                      @Valid @RequestBody UpdateUserRequest updateRequest,
                                      @RequestHeader("Authorization") String token) {
        try {
            // Check if user is admin or updating their own profile
            if (!isAdmin(token) && !isOwnProfile(id, token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            User updatedUser = userService.updateUser(
                id,
                updateRequest.getName(),
                updateRequest.getEmail()
            );
            
            UserResponse userResponse = new UserResponse(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getRole(),
                updatedUser.getCreatedAt()
            );
            
            return ResponseEntity.ok(userResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to update user: " + e.getMessage()));
        }
    }
    
    // Delete user
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            userService.deleteUser(id);
            return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to delete user: " + e.getMessage()));
        }
    }
    
    // Get user statistics
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(@RequestHeader("Authorization") String token) {
        try {
            if (!isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            UserService.UserStats stats = userService.getUserStats();
            return ResponseEntity.ok(stats);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve user statistics: " + e.getMessage()));
        }
    }
    
    // Helper methods
    private boolean isAdmin(String token) {
        try {
            String jwtToken = token.substring(7);
            var userOptional = authService.validateTokenAndGetUser(jwtToken);
            return userOptional.isPresent() && userOptional.get().getRole() == UserRole.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isOwnProfile(Long userId, String token) {
        try {
            String jwtToken = token.substring(7);
            var userOptional = authService.validateTokenAndGetUser(jwtToken);
            return userOptional.isPresent() && userOptional.get().getId().equals(userId);
        } catch (Exception e) {
            return false;
        }
    }
    
    // Inner classes for responses
    public static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    public static class MessageResponse {
        private String message;
        
        public MessageResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
