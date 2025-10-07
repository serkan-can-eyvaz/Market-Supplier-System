package com.example.marketsupplier.controller;

import com.example.marketsupplier.dto.*;
import com.example.marketsupplier.entity.Supplier;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.entity.UserRole;
import com.example.marketsupplier.service.AuthService;
import com.example.marketsupplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private AuthService authService;
    
    // Create supplier
    @PostMapping
    public ResponseEntity<?> createSupplier(@Valid @RequestBody SupplierRequest supplierRequest,
                                          Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            Supplier supplier = supplierService.createSupplier(
                userId,
                supplierRequest.getCompanyName(),
                supplierRequest.getPhone(),
                supplierRequest.getAddress(),
                supplierRequest.getPhoneNumberId()
            );
            
            SupplierResponse supplierResponse = new SupplierResponse(
                supplier.getId(),
                supplier.getCompanyName(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getPhoneNumberId(),
                supplier.getUser().getId(),
                supplier.getUser().getName(),
                supplier.getUser().getEmail(),
                supplier.getCreatedAt()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(supplierResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to create supplier: " + e.getMessage()));
        }
    }
    
    // Admin creates supplier (also creates SUPPLIER user)
    @PostMapping("/admin-create")
    public ResponseEntity<?> adminCreateSupplier(@Valid @RequestBody AdminCreateSupplierRequest req,
                                                 Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            // Create user with SUPPLIER role
            var auth = authService.register(req.getName(), req.getEmail(), req.getPassword(), UserRole.SUPPLIER);
            // Create supplier for that user
            var supplier = supplierService.createSupplier(auth.getUserId(), req.getCompanyName(), req.getPhone());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SupplierResponse(
                    supplier.getId(),
                    supplier.getCompanyName(),
                    supplier.getPhone(),
                    supplier.getAddress(),
                    supplier.getUser().getId(),
                    supplier.getUser().getName(),
                    supplier.getUser().getEmail(),
                    supplier.getCreatedAt()
                ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to create supplier by admin: " + e.getMessage()));
        }
    }

    // Get all suppliers
    @GetMapping
    public ResponseEntity<?> getAllSuppliers(Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            List<Supplier> suppliers = supplierService.getAllSuppliers();
            List<SupplierResponse> supplierResponses = suppliers.stream()
                .map(supplier -> new SupplierResponse(
                    supplier.getId(),
                    supplier.getCompanyName(),
                    supplier.getPhone(),
                    supplier.getAddress(),
                    supplier.getPhoneNumberId(),
                    supplier.getUser().getId(),
                    supplier.getUser().getName(),
                    supplier.getUser().getEmail(),
                    supplier.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(supplierResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve suppliers: " + e.getMessage()));
        }
    }
    
    // Get supplier by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getSupplierById(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or supplier owner
            if (!isAdmin(authentication) && !supplierService.isSupplierOwner(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            var supplierOptional = supplierService.findById(id);
            if (supplierOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Supplier supplier = supplierOptional.get();
            SupplierResponse supplierResponse = new SupplierResponse(
                supplier.getId(),
                supplier.getCompanyName(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getPhoneNumberId(),
                supplier.getUser().getId(),
                supplier.getUser().getName(),
                supplier.getUser().getEmail(),
                supplier.getCreatedAt()
            );
            
            return ResponseEntity.ok(supplierResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve supplier: " + e.getMessage()));
        }
    }
    
    // Get current user's supplier
    @GetMapping("/my-supplier")
    public ResponseEntity<?> getMySupplier(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            var supplierOptional = supplierService.findByUserId(userId);
            if (supplierOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Supplier supplier = supplierOptional.get();
            SupplierResponse supplierResponse = new SupplierResponse(
                supplier.getId(),
                supplier.getCompanyName(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getPhoneNumberId(),
                supplier.getUser().getId(),
                supplier.getUser().getName(),
                supplier.getUser().getEmail(),
                supplier.getCreatedAt()
            );
            
            return ResponseEntity.ok(supplierResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve supplier: " + e.getMessage()));
        }
    }
    
    // Search suppliers by company name
    @GetMapping("/search")
    public ResponseEntity<?> searchSuppliersByCompanyName(@RequestParam String companyName, 
                                                         Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            List<Supplier> suppliers = supplierService.searchSuppliersByCompanyName(companyName);
            List<SupplierResponse> supplierResponses = suppliers.stream()
                .map(supplier -> new SupplierResponse(
                    supplier.getId(),
                    supplier.getCompanyName(),
                    supplier.getPhone(),
                    supplier.getAddress(),
                    supplier.getPhoneNumberId(),
                    supplier.getUser().getId(),
                    supplier.getUser().getName(),
                    supplier.getUser().getEmail(),
                    supplier.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(supplierResponses);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to search suppliers: " + e.getMessage()));
        }
    }
    
    // Update supplier
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSupplier(@PathVariable Long id,
                                          @Valid @RequestBody SupplierRequest supplierRequest,
                                          Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }
            
            // Check if user is admin or supplier owner
            if (!isAdmin(authentication) && !supplierService.isSupplierOwner(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied."));
            }
            
            Supplier updatedSupplier = supplierService.updateSupplier(
                id,
                supplierRequest.getCompanyName(),
                supplierRequest.getPhone()
            );
            if (supplierRequest.getAddress() != null) {
                updatedSupplier = supplierService.updateSupplierAddress(id, supplierRequest.getAddress());
            }
            
            SupplierResponse supplierResponse = new SupplierResponse(
                updatedSupplier.getId(),
                updatedSupplier.getCompanyName(),
                updatedSupplier.getPhone(),
                updatedSupplier.getAddress(),
                updatedSupplier.getUser().getId(),
                updatedSupplier.getUser().getName(),
                updatedSupplier.getUser().getEmail(),
                updatedSupplier.getCreatedAt()
            );
            
            return ResponseEntity.ok(supplierResponse);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to update supplier: " + e.getMessage()));
        }
    }
    
    // Delete supplier
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupplier(@PathVariable Long id, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            supplierService.deleteSupplier(id);
            return ResponseEntity.ok(new MessageResponse("Supplier deleted successfully"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to delete supplier: " + e.getMessage()));
        }
    }
    
    // Get supplier statistics
    @GetMapping("/stats")
    public ResponseEntity<?> getSupplierStats(Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            SupplierService.SupplierStats stats = supplierService.getSupplierStats();
            return ResponseEntity.ok(stats);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve supplier statistics: " + e.getMessage()));
        }
    }
    
    // Check if company name exists
    @GetMapping("/check-company-name")
    public ResponseEntity<?> checkCompanyNameExists(@RequestParam String companyName) {
        try {
            boolean exists = supplierService.companyNameExists(companyName);
            return ResponseEntity.ok(new CompanyNameCheckResponse(exists));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to check company name: " + e.getMessage()));
        }
    }
    
    // Helper methods
    private Long getUserIdFromAuthentication(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            return user.getId();
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean isAdmin(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            return user.getRole() == UserRole.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Update supplier phone number ID
    @PutMapping("/{id}/phone-number-id")
    public ResponseEntity<?> updatePhoneNumberId(@PathVariable Long id,
                                               @RequestBody PhoneNumberIdRequest request,
                                               Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid token"));
            }

            // ADMIN ise herhangi bir tedarikçiyi güncelleyebilmesine izin ver
            if (isAdmin(authentication)) {
                Supplier supplier = supplierService.updatePhoneNumberId(id, request.getPhoneNumberId());
                return ResponseEntity.ok(new MessageResponse("Phone number ID updated successfully"));
            }
            
            // Check if user owns this supplier
            if (!supplierService.isSupplierOwner(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You can only update your own supplier profile"));
            }
            
            Supplier supplier = supplierService.updatePhoneNumberId(id, request.getPhoneNumberId());
            
            return ResponseEntity.ok(new MessageResponse("Phone number ID updated successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // Get supplier by phone number ID (for debugging)
    @GetMapping("/by-phone-number-id/{phoneNumberId}")
    public ResponseEntity<?> getSupplierByPhoneNumberId(@PathVariable String phoneNumberId) {
        try {
            return supplierService.findByPhoneNumberId(phoneNumberId)
                .map(supplier -> ResponseEntity.ok(new SupplierResponse(
                    supplier.getId(),
                    supplier.getCompanyName(),
                    supplier.getPhone(),
                    supplier.getAddress(),
                    supplier.getPhoneNumberId(),
                    supplier.getUser().getId(),
                    supplier.getUser().getName(),
                    supplier.getUser().getEmail(),
                    supplier.getCreatedAt()
                )))
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error retrieving supplier: " + e.getMessage()));
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
    
    public static class CompanyNameCheckResponse {
        private boolean exists;
        
        public CompanyNameCheckResponse(boolean exists) {
            this.exists = exists;
        }
        
        public boolean isExists() {
            return exists;
        }
        
        public void setExists(boolean exists) {
            this.exists = exists;
        }
    }
    
    public static class PhoneNumberIdRequest {
        private String phoneNumberId;
        
        public PhoneNumberIdRequest() {}
        
        public PhoneNumberIdRequest(String phoneNumberId) {
            this.phoneNumberId = phoneNumberId;
        }
        
        public String getPhoneNumberId() {
            return phoneNumberId;
        }
        
        public void setPhoneNumberId(String phoneNumberId) {
            this.phoneNumberId = phoneNumberId;
        }
    }
}
