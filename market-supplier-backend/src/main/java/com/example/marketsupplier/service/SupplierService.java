package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.Supplier;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.entity.UserRole;
import com.example.marketsupplier.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SupplierService {
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private UserService userService;
    
    // Create new supplier
    public Supplier createSupplier(Long userId, String companyName, String phone) {
        return createSupplier(userId, companyName, phone, null, null);
    }
    
    // Create new supplier with address and phone number ID
    public Supplier createSupplier(Long userId, String companyName, String phone, String address, String phoneNumberId) {
        // Get user
        User user = userService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Check if user is SUPPLIER role
        if (user.getRole() != UserRole.SUPPLIER) {
            throw new RuntimeException("User must have SUPPLIER role to create a supplier");
        }
        
        // Check if supplier already exists for this user
        if (supplierRepository.existsByUser(user)) {
            throw new RuntimeException("Supplier already exists for this user");
        }
        
        // Check if company name already exists
        if (supplierRepository.existsByCompanyName(companyName)) {
            throw new RuntimeException("Company name already exists: " + companyName);
        }
        
        // Check if phone already exists
        if (supplierRepository.findByPhone(phone).isPresent()) {
            throw new RuntimeException("Phone number already exists: " + phone);
        }
        
        // Check if phone number ID already exists (if provided)
        if (phoneNumberId != null && !phoneNumberId.trim().isEmpty()) {
            if (supplierRepository.findByPhoneNumberId(phoneNumberId).isPresent()) {
                throw new RuntimeException("Phone number ID already exists: " + phoneNumberId);
            }
        }
        
        // Create supplier
        Supplier supplier = new Supplier(user, companyName, phone, phoneNumberId);
        if (address != null && !address.trim().isEmpty()) {
            supplier.setAddress(address);
        }
        
        return supplierRepository.save(supplier);
    }
    
    // Find supplier by ID
    public Optional<Supplier> findById(Long id) {
        return supplierRepository.findById(id);
    }
    
    // Find supplier by user
    public Optional<Supplier> findByUser(User user) {
        return supplierRepository.findByUser(user);
    }
    
    // Find supplier by user ID
    public Optional<Supplier> findByUserId(Long userId) {
        return supplierRepository.findByUserId(userId);
    }
    
    // Get all suppliers
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAllByOrderByCreatedAtDesc();
    }
    
    // Search suppliers by company name
    public List<Supplier> searchSuppliersByCompanyName(String companyName) {
        return supplierRepository.findByCompanyNameContainingIgnoreCase(companyName);
    }
    
    // Find supplier by company name
    public Optional<Supplier> findByCompanyName(String companyName) {
        return supplierRepository.findByCompanyName(companyName);
    }
    
    // Update supplier
    public Supplier updateSupplier(Long id, String companyName, String phone) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        
        // Check if company name is being changed and if new company name already exists
        if (!supplier.getCompanyName().equals(companyName)) {
            if (supplierRepository.existsByCompanyName(companyName)) {
                throw new RuntimeException("Company name already exists: " + companyName);
            }
        }
        
        // Check if phone is being changed and if new phone already exists
        if (!supplier.getPhone().equals(phone)) {
            Optional<Supplier> existingSupplier = supplierRepository.findByPhone(phone);
            if (existingSupplier.isPresent() && !existingSupplier.get().getId().equals(id)) {
                throw new RuntimeException("Phone number already exists: " + phone);
            }
        }
        
        supplier.setCompanyName(companyName);
        supplier.setPhone(phone);
        
        return supplierRepository.save(supplier);
    }

    // Update supplier address only
    public Supplier updateSupplierAddress(Long id, String address) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        supplier.setAddress(address);
        return supplierRepository.save(supplier);
    }
    
    // Delete supplier
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
        
        // Check if supplier has deliveries
        if (!supplier.getDeliveries().isEmpty()) {
            throw new RuntimeException("Cannot delete supplier with existing deliveries");
        }
        
        supplierRepository.delete(supplier);
    }
    
    // Get supplier statistics
    public SupplierStats getSupplierStats() {
        long totalSuppliers = supplierRepository.count();
        List<Object[]> suppliersWithDeliveryCounts = supplierRepository.findSuppliersWithDeliveryCounts();
        
        return new SupplierStats(totalSuppliers, suppliersWithDeliveryCounts);
    }
    
    // Check if supplier exists for user
    public boolean supplierExistsForUser(Long userId) {
        return supplierRepository.existsByUserId(userId);
    }
    
    // Get supplier by user email
    public Optional<Supplier> findByUserEmail(String email) {
        Optional<User> user = userService.findByEmail(email);
        if (user.isPresent()) {
            return supplierRepository.findByUser(user.get());
        }
        return Optional.empty();
    }
    
    // Validate supplier ownership
    public boolean isSupplierOwner(Long supplierId, Long userId) {
        Optional<Supplier> supplier = supplierRepository.findById(supplierId);
        return supplier.isPresent() && supplier.get().getUser().getId().equals(userId);
    }
    
    // Check if company name exists
    public boolean companyNameExists(String companyName) {
        return supplierRepository.existsByCompanyName(companyName);
    }
    
    // Find supplier by WhatsApp phone number ID
    public Optional<Supplier> findByPhoneNumberId(String phoneNumberId) {
        return supplierRepository.findByPhoneNumberId(phoneNumberId);
    }
    
    // Update supplier phone number ID
    public Supplier updatePhoneNumberId(Long supplierId, String phoneNumberId) {
        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + supplierId));
        
        // Check if phone number ID already exists for another supplier
        Optional<Supplier> existingSupplier = supplierRepository.findByPhoneNumberId(phoneNumberId);
        if (existingSupplier.isPresent() && !existingSupplier.get().getId().equals(supplierId)) {
            throw new RuntimeException("Phone number ID already exists for another supplier: " + phoneNumberId);
        }
        
        supplier.setPhoneNumberId(phoneNumberId);
        return supplierRepository.save(supplier);
    }
    
    // Inner class for supplier statistics
    public static class SupplierStats {
        private final long totalSuppliers;
        private final List<Object[]> suppliersWithDeliveryCounts;
        
        public SupplierStats(long totalSuppliers, List<Object[]> suppliersWithDeliveryCounts) {
            this.totalSuppliers = totalSuppliers;
            this.suppliersWithDeliveryCounts = suppliersWithDeliveryCounts;
        }
        
        // Getters
        public long getTotalSuppliers() { return totalSuppliers; }
        public List<Object[]> getSuppliersWithDeliveryCounts() { return suppliersWithDeliveryCounts; }
    }
}
