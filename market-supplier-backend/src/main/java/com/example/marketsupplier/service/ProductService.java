package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.Product;
import com.example.marketsupplier.entity.Supplier;
import com.example.marketsupplier.entity.User;
import com.example.marketsupplier.repository.ProductRepository;
import com.example.marketsupplier.repository.SupplierRepository;
import com.example.marketsupplier.dto.ProductCreateRequest;
import com.example.marketsupplier.dto.ProductUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    @Lazy
    private AsyncService asyncService;

    @Cacheable(value = "products", key = "#user.id + '_active'")
    public List<Product> getSupplierProducts(User user) {
        System.out.println("DEBUG: getSupplierProducts called for user: " + user.getEmail() + ", role: " + user.getRole());
        
        Supplier supplier = getOrCreateSupplier(user);
        System.out.println("DEBUG: Supplier found: " + supplier.getId());
        
        // Use optimized query with supplier ID instead of entity
        return productRepository.findBySupplierIdAndIsActiveTrueOrderByNameAsc(supplier.getId());
    }

    @Cacheable(value = "products", key = "#user.id + '_all'")
    public List<Product> getAllSupplierProducts(User user) {
        System.out.println("DEBUG: getAllSupplierProducts called for user: " + user.getEmail() + ", role: " + user.getRole());
        
        Supplier supplier = getOrCreateSupplier(user);
        System.out.println("DEBUG: Supplier found: " + supplier.getId());
        
        // Use optimized query with supplier ID instead of entity
        return productRepository.findBySupplierIdAndIsActiveTrueOrderByNameAsc(supplier.getId());
    }
    
    public Page<Product> getAllSupplierProductsPaginated(User user, Pageable pageable) {
        System.out.println("DEBUG: getAllSupplierProductsPaginated called for user: " + user.getEmail());
        
        Supplier supplier = getOrCreateSupplier(user);
        System.out.println("DEBUG: Supplier found: " + supplier.getId());
        
        // Use optimized query with supplier ID instead of entity
        return productRepository.findBySupplierIdAndIsActiveTrueOrderByNameAsc(supplier.getId(), pageable);
    }

    @CacheEvict(value = "products", key = "#user.id + '_*'")
    public Product createProduct(User user, String name, String description, String unit, BigDecimal price) {
        return createProduct(user, name, description, unit, price, 0);
    }
    
    @CacheEvict(value = "products", key = "#user.id + '_*'")
    public Product createProduct(User user, String name, String description, String unit, BigDecimal price, Integer stockQuantity) {
        System.out.println("DEBUG: createProduct called for user: " + user.getEmail() + ", role: " + user.getRole());
        
        Supplier supplier = getOrCreateSupplier(user);
        System.out.println("DEBUG: Supplier found: " + supplier.getId());

        if (productRepository.existsBySupplierAndNameIgnoreCase(supplier, name)) {
            System.out.println("DEBUG: Product name already exists: " + name);
            throw new RuntimeException("Bu ürün adı zaten mevcut");
        }

        Product product = new Product();
        product.setSupplier(supplier);
        product.setName(name);
        product.setDescription(description != null ? description : "");
        product.setUnit(unit);
        product.setPrice(price);
        product.setIsActive(true);
        product.setStockQuantity(stockQuantity != null ? stockQuantity : 0);
        
        System.out.println("DEBUG: Saving product: " + product.getName());
        Product savedProduct = productRepository.save(product);
        System.out.println("DEBUG: Product saved with ID: " + savedProduct.getId());
        
        // Async cache warmup
        asyncService.cacheWarmupAsync();
        
        return savedProduct;
    }
    
    public Product createProduct(User user, ProductCreateRequest request) {
        return createProduct(user, request.getName(), request.getDescription(), request.getUnit(), 
                           request.getPrice(), request.getStockQuantity());
    }

    public Product updateProduct(User user, Long productId, String name, String description, String unit, BigDecimal price) {
        Supplier supplier = supplierRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Tedarikçi bulunamadı"));

        Product product = productRepository.findByIdAndSupplier(productId, supplier)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı"));

        if (productRepository.existsBySupplierAndNameIgnoreCaseAndIdNot(supplier, name, productId)) {
            throw new RuntimeException("Bu ürün adı zaten mevcut");
        }

        product.setName(name);
        product.setDescription(description);
        product.setUnit(unit);
        product.setPrice(price);

        return productRepository.save(product);
    }
    
    public Product updateProduct(User user, Long productId, ProductUpdateRequest request) {
        Supplier supplier = supplierRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Tedarikçi bulunamadı"));

        Product product = productRepository.findByIdAndSupplier(productId, supplier)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı"));

        // Check name uniqueness if name is being updated
        if (request.getName() != null && !request.getName().equals(product.getName())) {
            if (productRepository.existsBySupplierAndNameIgnoreCaseAndIdNot(supplier, request.getName(), productId)) {
                throw new RuntimeException("Bu ürün adı zaten mevcut");
            }
            product.setName(request.getName());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getUnit() != null) {
            product.setUnit(request.getUnit());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }

        return productRepository.save(product);
    }

    public Product updateStock(User user, Long productId, Integer stockQuantity) {
        Supplier supplier = supplierRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Tedarikçi bulunamadı"));
        Product product = productRepository.findByIdAndSupplier(productId, supplier)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı"));
        product.setStockQuantity(Math.max(0, stockQuantity != null ? stockQuantity : 0));
        return productRepository.save(product);
    }

    public void deleteProduct(User user, Long productId) {
        Supplier supplier = supplierRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Tedarikçi bulunamadı"));

        Product product = productRepository.findByIdAndSupplier(productId, supplier)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı"));

        product.setIsActive(false);
        productRepository.save(product);
    }

    public Product toggleProductStatus(User user, Long productId) {
        Supplier supplier = supplierRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Tedarikçi bulunamadı"));

        Product product = productRepository.findByIdAndSupplier(productId, supplier)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı"));

        product.setIsActive(!product.getIsActive());
        return productRepository.save(product);
    }

    public List<Product> searchProducts(User user, String searchTerm) {
        Supplier supplier = supplierRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Tedarikçi bulunamadı"));
        return productRepository.findBySupplierAndNameContainingIgnoreCase(supplier, searchTerm);
    }

    public Optional<Product> getProductById(User user, Long productId) {
        Supplier supplier = supplierRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Tedarikçi bulunamadı"));
        return productRepository.findByIdAndSupplier(productId, supplier);
    }

    // AI Agent için tüm aktif ürünleri getir
    @Cacheable(value = "products", key = "'all_active'")
    public List<Product> getAllActiveProducts() {
        return productRepository.findByIsActiveTrueOrderByNameAsc();
    }

    // Performance optimized methods
    @Cacheable(value = "products", key = "#supplierId + '_summary'")
    public List<Object[]> getProductSummaryBySupplierId(Long supplierId) {
        return productRepository.findProductSummaryBySupplierId(supplierId);
    }

    @Cacheable(value = "products", key = "#supplierId + '_available'")
    public List<Product> getAvailableProductsBySupplierId(Long supplierId) {
        return productRepository.findAvailableProductsBySupplierId(supplierId);
    }

    @Cacheable(value = "products", key = "#supplierId + '_low_stock'")
    public List<Product> getLowStockProductsBySupplierId(Long supplierId, int lowStockThreshold) {
        return productRepository.findLowStockProductsBySupplierId(supplierId, lowStockThreshold);
    }

    @Cacheable(value = "products", key = "#supplierId + '_search_' + #searchTerm")
    public List<Product> searchProductsBySupplierId(Long supplierId, String searchTerm) {
        return productRepository.searchProductsBySupplierId(supplierId, searchTerm);
    }

    @Cacheable(value = "products", key = "'statistics'")
    public List<Object[]> getProductStatisticsBySupplier() {
        return productRepository.getProductStatisticsBySupplier();
    }

    @Cacheable(value = "products", key = "#supplierId + '_statistics'")
    public Object[] getProductStatisticsBySupplierId(Long supplierId) {
        return productRepository.getProductStatisticsBySupplierId(supplierId);
    }

    // Batch operations
    @Transactional
    public int updateStockById(Long productId, int quantity) {
        int updated = productRepository.updateStockById(productId, quantity);
        if (updated > 0) {
            // Evict related caches
            cacheService.evictPattern("products:*");
        }
        return updated;
    }

    @Transactional
    public int addStockById(Long productId, int quantity) {
        int updated = productRepository.addStockById(productId, quantity);
        if (updated > 0) {
            // Evict related caches
            cacheService.evictPattern("products:*");
        }
        return updated;
    }

    @Transactional
    public int updateStockByIds(Set<Long> productIds, int quantity) {
        int updated = productRepository.updateStockByIds(productIds, quantity);
        if (updated > 0) {
            // Evict related caches
            cacheService.evictPattern("products:*");
        }
        return updated;
    }

    @Transactional
    public int updatePriceBySupplierId(Long supplierId, BigDecimal newPrice) {
        int updated = productRepository.updatePriceBySupplierId(supplierId, newPrice);
        if (updated > 0) {
            // Evict related caches
            cacheService.evictPattern("products:*");
        }
        return updated;
    }

    // Async operations
    public CompletableFuture<List<Product>> getTopProductsByStockAsync(Long supplierId, Pageable pageable) {
        return CompletableFuture.supplyAsync(() -> 
            productRepository.findTopProductsByStock(supplierId, pageable));
    }

    public CompletableFuture<List<Product>> getCheapestProductsAsync(Long supplierId, Pageable pageable) {
        return CompletableFuture.supplyAsync(() -> 
            productRepository.findCheapestProducts(supplierId, pageable));
    }

    public CompletableFuture<List<Product>> getMostExpensiveProductsAsync(Long supplierId, Pageable pageable) {
        return CompletableFuture.supplyAsync(() -> 
            productRepository.findMostExpensiveProducts(supplierId, pageable));
    }

    // Helper method
    private Supplier getOrCreateSupplier(User user) {
        return supplierRepository.findByUser(user)
                .orElseGet(() -> {
                    System.out.println("DEBUG: Supplier not found, creating new supplier for user: " + user.getEmail());
                    // Create supplier automatically
                    Supplier newSupplier = new Supplier();
                    newSupplier.setUser(user);
                    newSupplier.setCompanyName(user.getName() + " Şirketi");
                    newSupplier.setPhone("+905551234567"); // Default phone
                    return supplierRepository.save(newSupplier);
                });
    }

    // Admin için tüm ürünleri getir (aktif ve pasif)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
