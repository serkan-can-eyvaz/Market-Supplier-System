import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { 
  AuthResponse, 
  LoginRequest, 
  PhoneLoginRequest,
  RegisterRequest, 
  User, 
  UserRole,
  Market, 
  MarketRequest,
  Supplier,
  SupplierRequest,
  Order,
  OrderItemRequest,
  Delivery,
  DeliveryRequest,
  PaginatedResponse,
  UserStats,
  MarketStats,
  SupplierStats,
  OrderStats,
  DeliveryStats,
  Product,
  ProductResponse,
  ProductCreateRequest,
  ProductUpdateRequest,
  CartItem,
  CartResponse,
  Notification
} from '../types';

// API base URL - environment'a göre ayarla
const getApiBaseUrl = () => {
  const hostname = window.location.hostname;
  
  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    // Development ortamı - local backend kullan
    return 'http://localhost:8080/api';
  }
  
  // Production ortamı (commented out for local development)
  // return 'https://tedarikasistani.com/api';
  return 'http://localhost:8080/api';
};

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: process.env.REACT_APP_API_URL || getApiBaseUrl(),
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 10000, // 10 saniye timeout
    });

    // Request interceptor for JWT authentication
    this.api.interceptors.request.use(
      (config) => {
        // JWT Token authentication
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
          console.log('[ApiService] Request interceptor - Token added:', token.substring(0, 20) + '...');
        } else {
          console.log('[ApiService] Request interceptor - No token found in localStorage');
        }
        
        console.log('[ApiService] Making request to:', config.method?.toUpperCase(), config.url);
        return config;
      },
      (error) => {
        console.log('[ApiService] Request interceptor error:', error);
        return Promise.reject(error);
      }
    );

        // Response interceptor to handle errors
        this.api.interceptors.response.use(
          (response) => {
            console.log('[ApiService] Response received:', response.status, response.config.url);
            return response;
          },
          async (error) => {
            console.log('[ApiService] Response error:', error.response?.status, error.response?.data, error.config?.url);
            
            // Handle JWT authentication errors
            if (error.response?.status === 401) {
              console.log('[ApiService] 401 Unauthorized - token invalid or expired');
              console.log('[ApiService] Error response data:', error.response.data);
              
              // Check if the error is actually about invalid token
              if (error.response.data?.message === 'Invalid token' || 
                  error.response.data?.error === 'Unauthorized') {
                console.log('[ApiService] Confirmed invalid token - clearing localStorage');
                // Only clear localStorage and redirect if not already on login page
                if (!window.location.pathname.includes('/login')) {
                  console.log('[ApiService] Redirecting to login page');
                  localStorage.removeItem('user');
                  localStorage.removeItem('token');
                  // Use history API instead of window.location.href to avoid page reload
                  window.history.pushState(null, '', '/login');
                  window.dispatchEvent(new PopStateEvent('popstate'));
                }
              } else {
                console.log('[ApiService] 401 error but not token-related, keeping localStorage');
              }
            } else if (error.response?.status === 403) {
              console.log('[ApiService] 403 Forbidden - insufficient permissions');
              // Don't clear localStorage for 403 errors, just log the issue
            }
            
            return Promise.reject(error);
          }
        );
  }

  // Auth API
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    console.log('[ApiService] Starting login process...');
    const response: AxiosResponse<any> = await this.api.post('/auth/login', credentials);
    
    console.log('[ApiService] Login response:', response.data);
    console.log('[ApiService] Token exists:', !!response.data.token);
    console.log('[ApiService] User exists:', !!response.data.user);
    
    // Store token and user if present
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      console.log('[ApiService] Token stored in localStorage:', response.data.token.substring(0, 20) + '...');
      
      // Verify token was stored correctly
      const storedToken = localStorage.getItem('token');
      console.log('[ApiService] Token verification - stored correctly:', storedToken === response.data.token);
    } else {
      console.log('[ApiService] No token in response!');
    }
    
    if (response.data.user) {
      localStorage.setItem('user', JSON.stringify(response.data.user));
      console.log('[ApiService] User stored in localStorage');
    } else {
      console.log('[ApiService] No user in response!');
    }
    
    return response.data;
  }

  async loginWithPhone(credentials: PhoneLoginRequest): Promise<AuthResponse> {
    console.log('[ApiService] Starting phone login process...');
    const response: AxiosResponse<any> = await this.api.post('/auth/login-phone', credentials);
    
    console.log('[ApiService] Phone login response:', response.data);
    console.log('[ApiService] Token exists:', !!response.data.token);
    console.log('[ApiService] User exists:', !!response.data.user);
    
    // Store token and user if present
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      console.log('[ApiService] Token stored in localStorage:', response.data.token.substring(0, 20) + '...');
    } else {
      console.log('[ApiService] No token in response!');
    }
    
    if (response.data.user) {
      localStorage.setItem('user', JSON.stringify(response.data.user));
      console.log('[ApiService] User stored in localStorage');
    } else {
      console.log('[ApiService] No user in response!');
    }
    
    return response.data;
  }

  async register(userData: RegisterRequest): Promise<AuthResponse> {
    const response: AxiosResponse<AuthResponse> = await this.api.post('/auth/register', userData);
    return response.data;
  }

  async getCurrentUser(): Promise<User> {
    // JWT authentication - localStorage'dan user bilgisini al
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      return JSON.parse(storedUser);
    }
    
    // Eğer localStorage'da user yoksa, token'dan bilgi alınabilir
    // Şimdilik boş user döndür
    throw new Error('User not found in localStorage');
  }

  async logout(): Promise<void> {
    // JWT authentication - sadece localStorage'ı temizle
    localStorage.removeItem('user');
    localStorage.removeItem('token');
  }


  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await this.api.post('/auth/change-password', { currentPassword, newPassword });
  }

  // User API
  async getUsers(): Promise<User[]> {
    const response: AxiosResponse<User[]> = await this.api.get('/users');
    return response.data;
  }

  async getUserById(id: number): Promise<User> {
    const response: AxiosResponse<User> = await this.api.get(`/users/${id}`);
    return response.data;
  }

  async updateUser(id: number, userData: { name: string; email: string }): Promise<User> {
    const response: AxiosResponse<User> = await this.api.put(`/users/${id}`, userData);
    return response.data;
  }

  async deleteUser(id: number): Promise<void> {
    await this.api.delete(`/users/${id}`);
  }

  async getUserStats(): Promise<UserStats> {
    const response: AxiosResponse<UserStats> = await this.api.get('/users/stats');
    return response.data;
  }

  // Market API
  async createMarket(marketData: MarketRequest): Promise<Market> {
    const response: AxiosResponse<Market> = await this.api.post('/markets', marketData);
    return response.data;
  }

  async getMarkets(): Promise<Market[]> {
    const response: AxiosResponse<Market[]> = await this.api.get('/markets');
    return response.data;
  }

  async getMarketById(id: number): Promise<Market> {
    const response: AxiosResponse<Market> = await this.api.get(`/markets/${id}`);
    return response.data;
  }

  // Removed: getMyMarket (single)

  async getMyMarkets(page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc'): Promise<PaginatedResponse<Market>> {
    console.log('[ApiService] getMyMarkets called with params:', { page, size, sortBy, sortDir });
    console.log('[ApiService] Current token in localStorage:', localStorage.getItem('token') ? 'EXISTS' : 'NOT_FOUND');
    
    const response: AxiosResponse<PaginatedResponse<Market>> = await this.api.get('/markets/my-markets', {
      params: { page, size, sortBy, sortDir }
    });
    
    console.log('[ApiService] getMyMarkets response received:', response.status);
    return response.data;
  }

  async updateMarket(id: number, marketData: MarketRequest): Promise<Market> {
    const response: AxiosResponse<Market> = await this.api.put(`/markets/${id}`, marketData);
    return response.data;
  }

  async deleteMarket(id: number): Promise<void> {
    await this.api.delete(`/markets/${id}`);
  }

  async getMarketStats(): Promise<MarketStats> {
    const response: AxiosResponse<MarketStats> = await this.api.get('/markets/stats');
    return response.data;
  }

  // Supplier API
  async createSupplier(supplierData: SupplierRequest): Promise<Supplier> {
    const response: AxiosResponse<Supplier> = await this.api.post('/suppliers', supplierData);
    return response.data;
  }

  async getSuppliers(): Promise<Supplier[]> {
    const response: AxiosResponse<Supplier[]> = await this.api.get('/suppliers');
    return response.data;
  }

  async getSupplierById(id: number): Promise<Supplier> {
    const response: AxiosResponse<Supplier> = await this.api.get(`/suppliers/${id}`);
    return response.data;
  }

  async getMySupplier(): Promise<Supplier> {
    const response: AxiosResponse<Supplier> = await this.api.get('/suppliers/my-supplier');
    return response.data;
  }

  async updateMySupplier(payload: { companyName?: string; phone?: string; address?: string }): Promise<Supplier> {
    // İlk olarak kendi supplier id'mizi alalım
    const me: Supplier = await this.getMySupplier();
    const response: AxiosResponse<Supplier> = await this.api.put(`/suppliers/${me.id}`, {
      companyName: payload.companyName ?? me.companyName,
      phone: payload.phone ?? me.phone,
      address: payload.address,
    } as any);
    return response.data;
  }

  async updateSupplier(id: number, supplierData: SupplierRequest): Promise<Supplier> {
    const response: AxiosResponse<Supplier> = await this.api.put(`/suppliers/${id}`, supplierData);
    return response.data;
  }

  // Update supplier's WhatsApp phone_number_id
  async updateSupplierPhoneNumberId(id: number, phoneNumberId: string): Promise<Supplier> {
    // Backend @RequestBody PhoneNumberIdRequest bekliyor → JSON body ile gönder
    const response: AxiosResponse<Supplier> = await this.api.put(`/suppliers/${id}/phone-number-id`, {
      phoneNumberId
    });
    return response.data;
  }

  async deleteSupplier(id: number): Promise<void> {
    await this.api.delete(`/suppliers/${id}`);
  }

  async getSupplierStats(): Promise<SupplierStats> {
    const response: AxiosResponse<SupplierStats> = await this.api.get('/suppliers/stats');
    return response.data;
  }

  async adminCreateSupplier(payload: { name: string; email: string; password: string; companyName: string; phone: string; phoneNumberId?: string }): Promise<Supplier> {
    const response: AxiosResponse<Supplier> = await this.api.post('/suppliers/admin-create', payload);
    return response.data;
  }

  async checkCompanyNameExists(companyName: string): Promise<boolean> {
    const response: AxiosResponse<{ exists: boolean }> = await this.api.get(`/suppliers/check-company-name?companyName=${companyName}`);
    return response.data.exists;
  }

  // Order API - Individual item operations
  async addItemToOrder(orderId: number, itemData: OrderItemRequest): Promise<any> {
    const response: AxiosResponse<any> = await this.api.post(`/orders/${orderId}/items`, itemData);
    return response.data;
  }

  async updateOrderItem(itemId: number, itemData: OrderItemRequest): Promise<any> {
    const response: AxiosResponse<any> = await this.api.put(`/orders/items/${itemId}`, itemData);
    return response.data;
  }

  async removeItemFromOrder(itemId: number): Promise<void> {
    await this.api.delete(`/orders/items/${itemId}`);
  }

  async completeOrder(orderId: number): Promise<Order> {
    const response: AxiosResponse<Order> = await this.api.post(`/orders/${orderId}/complete`);
    return response.data;
  }

  async approveOrder(orderId: number): Promise<void> {
    await this.api.post(`/orders/${orderId}/approve`);
  }

  async rejectOrder(orderId: number): Promise<void> {
    await this.api.post(`/orders/${orderId}/reject`);
  }

  async setPlannedDeliveryDate(orderId: number, dateTimeIso: string): Promise<void> {
    await this.api.post(`/orders/${orderId}/planned-date`, null, { params: { dateTimeIso } });
  }

  async getOrdersByMarketId(marketId: number): Promise<Order[]> {
    const response: AxiosResponse<Order[]> = await this.api.get(`/orders/market/${marketId}`);
    return response.data;
  }

  async getPendingOrders(): Promise<Order[]> {
    const response: AxiosResponse<Order[]> = await this.api.get('/orders/pending');
    return response.data;
  }

  async getPendingOrdersPaged(page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc'): Promise<PaginatedResponse<Order>> {
    const response: AxiosResponse<PaginatedResponse<Order>> = await this.api.get('/orders/pending', {
      params: { page, size, sortBy, sortDir }
    });
    return response.data;
  }

  async getOrderStats(): Promise<OrderStats> {
    const response: AxiosResponse<OrderStats> = await this.api.get('/orders/stats');
    return response.data;
  }

  // Delivery API
  async createDelivery(deliveryData: DeliveryRequest): Promise<Delivery> {
    const response: AxiosResponse<Delivery> = await this.api.post('/deliveries', deliveryData);
    return response.data;
  }


  async completeDelivery(deliveryId: number): Promise<Delivery> {
    const response: AxiosResponse<Delivery> = await this.api.post(`/deliveries/${deliveryId}/complete`);
    return response.data;
  }

  async getDeliveriesBySupplier(supplierId: number): Promise<Delivery[]> {
    const response: AxiosResponse<Delivery[]> = await this.api.get(`/deliveries/supplier/${supplierId}`);
    return response.data;
  }

  async getMyDeliveries(page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc'): Promise<PaginatedResponse<Delivery>> {
    const response: AxiosResponse<PaginatedResponse<Delivery>> = await this.api.get('/deliveries/my-deliveries', {
      params: { page, size, sortBy, sortDir }
    });
    return response.data;
  }

  async getAllDeliveries(page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc'): Promise<PaginatedResponse<Delivery>> {
    const response: AxiosResponse<PaginatedResponse<Delivery>> = await this.api.get('/deliveries/all', {
      params: { page, size, sortBy, sortDir }
    });
    return response.data;
  }

  async getInProgressDeliveriesForSupplier(supplierId: number): Promise<Delivery[]> {
    const response: AxiosResponse<Delivery[]> = await this.api.get(`/deliveries/supplier/${supplierId}/in-progress`);
    return response.data;
  }

  async getDailyDeliveriesForSupplier(supplierId: number, date: string): Promise<Delivery[]> {
    const response: AxiosResponse<Delivery[]> = await this.api.get(`/deliveries/supplier/${supplierId}/daily?date=${date}`);
    return response.data;
  }

  async getDeliveryById(id: number): Promise<Delivery> {
    const response: AxiosResponse<Delivery> = await this.api.get(`/deliveries/${id}`);
    return response.data;
  }

  async getDeliveryStats(): Promise<DeliveryStats> {
    const response: AxiosResponse<DeliveryStats> = await this.api.get('/deliveries/stats');
    return response.data;
  }

  // Dispatch delivery (start route)
  async dispatchDelivery(deliveryId: number): Promise<{ message: string }> {
    const response: AxiosResponse<{ message: string }> = await this.api.post(`/deliveries/dispatch/${deliveryId}`);
    return response.data;
  }


  // Order methods
  async createOrder(orderData: any): Promise<Order> {
    const response: AxiosResponse<Order> = await this.api.post('/orders', orderData);
    return response.data;
  }

  async getMarketOrders(): Promise<Order[]> {
    const response: AxiosResponse<Order[]> = await this.api.get('/orders/market');
    return response.data;
  }

  async getAllOrders(page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc'): Promise<PaginatedResponse<Order>> {
    const response: AxiosResponse<PaginatedResponse<Order>> = await this.api.get('/orders', {
      params: { page, size, sortBy, sortDir }
    });
    return response.data;
  }

  async downloadOrderPdf(orderId: number): Promise<Blob> {
    const response = await this.api.get(`/orders/${orderId}/pdf`, { responseType: 'blob' });
    return response.data as Blob;
  }

  async updateOrder(orderId: number, orderData: any): Promise<Order> {
    const response: AxiosResponse<Order> = await this.api.put(`/orders/${orderId}`, orderData);
    return response.data;
  }

  async deleteOrder(orderId: number): Promise<void> {
    await this.api.delete(`/orders/${orderId}`);
  }

  async getOrderById(orderId: number): Promise<Order> {
    const response: AxiosResponse<Order> = await this.api.get(`/orders/${orderId}`);
    return response.data;
  }

  // Product methods
  async getProducts(page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc', filter: string = 'active'): Promise<PaginatedResponse<Product>> {
    const response: AxiosResponse<PaginatedResponse<Product>> = await this.api.get('/products', {
      params: { page, size, sortBy, sortDir, filter }
    });
    return response.data;
  }

  async getAllProducts(): Promise<Product[]> {
    const response: AxiosResponse<Product[]> = await this.api.get('/products/all');
    return response.data;
  }

  async createProduct(productData: ProductCreateRequest): Promise<ProductResponse> {
    const response: AxiosResponse<ProductResponse> = await this.api.post('/products', productData);
    return response.data;
  }

  async updateProduct(productId: number, productData: ProductUpdateRequest): Promise<ProductResponse> {
    const response: AxiosResponse<ProductResponse> = await this.api.put(`/products/${productId}`, productData);
    return response.data;
  }

  async updateProductStock(productId: number, stockQuantity: number): Promise<Product> {
    const response: AxiosResponse<Product> = await this.api.put(`/products/${productId}/stock`, null, {
      params: { stockQuantity }
    });
    return response.data;
  }

  async deleteProduct(productId: number): Promise<void> {
    await this.api.delete(`/products/${productId}`);
  }

  async toggleProductStatus(productId: number): Promise<Product> {
    const response: AxiosResponse<Product> = await this.api.put(`/products/${productId}/toggle`);
    return response.data;
  }

  async searchProducts(query: string): Promise<Product[]> {
    const response: AxiosResponse<Product[]> = await this.api.get('/products/search', {
      params: { q: query }
    });
    return response.data;
  }

  async getProductById(productId: number): Promise<Product> {
    const response: AxiosResponse<Product> = await this.api.get(`/products/${productId}`);
    return response.data;
  }

  async refreshToken(refreshToken: string): Promise<{ token: string; refreshToken?: string }> {
    const response: AxiosResponse<{ token: string; refreshToken?: string }> = await this.api.post('/auth/refresh', { refreshToken });
    return response.data;
  }

  // Admin için sipariş verilen toplam ürün (kalem) sayısını getir
  async getTotalOrderItems(): Promise<{ totalItems: number }> {
    const response: AxiosResponse<{ totalItems: number }> = await this.api.get('/orders/total-items');
    return response.data;
  }

  // Market kullanıcıları için tüm aktif ürünleri getir
  async getAvailableProductsForMarket(): Promise<ProductResponse[]> {
    const response: AxiosResponse<ProductResponse[]> = await this.api.get('/products/market/available');
    return response.data;
  }

  // Tedarikçi ürünlerini ProductResponse formatında getir
  async getSupplierProductsFormatted(): Promise<ProductResponse[]> {
    const response: AxiosResponse<ProductResponse[]> = await this.api.get('/products/supplier/formatted');
    return response.data;
  }

  // Cart API methods
  async getCart(): Promise<CartItem[]> {
    const response: AxiosResponse<CartItem[]> = await this.api.get('/cart');
    return response.data;
  }

  async getCartDetailed(): Promise<CartResponse> {
    const response: AxiosResponse<CartResponse> = await this.api.get('/cart/detailed');
    return response.data;
  }

  async getCartSummary(): Promise<{ totalAmount: number; totalItems: number }> {
    const response: AxiosResponse<{ totalAmount: number; totalItems: number }> = await this.api.get('/cart/summary');
    return response.data;
  }

  async addItemToCart(productId: number, quantity: number): Promise<CartResponse> {
    const response: AxiosResponse<CartResponse> = await this.api.post('/cart/add-item', {
      productId,
      quantity
    });
    return response.data;
  }

  async updateCartItem(itemId: number, quantity: number): Promise<CartResponse> {
    const response: AxiosResponse<CartResponse> = await this.api.put(`/cart/update-item/${itemId}`, null, {
      params: { quantity }
    });
    return response.data;
  }

  async removeCartItem(itemId: number): Promise<CartResponse> {
    const response: AxiosResponse<CartResponse> = await this.api.delete(`/cart/remove-item/${itemId}`);
    return response.data;
  }

  async clearCart(): Promise<void> {
    await this.api.post('/cart/clear');
  }

  async downloadCartPdf(): Promise<Blob> {
    const response = await this.api.get('/cart/pdf', { responseType: 'blob' });
    return response.data as Blob;
  }

  // Notification Methods
  async getNotifications(): Promise<Notification[]> {
    const response: AxiosResponse<Notification[]> = await this.api.get('/notifications');
    return response.data;
  }

  async getUnreadNotifications(): Promise<Notification[]> {
    const response: AxiosResponse<Notification[]> = await this.api.get('/notifications/unread');
    return response.data;
  }

  async getUnreadCount(): Promise<{ count: number }> {
    const response: AxiosResponse<{ count: number }> = await this.api.get('/notifications/unread-count');
    return response.data;
  }

  async getRecentNotifications(limit: number = 10): Promise<Notification[]> {
    const response: AxiosResponse<Notification[]> = await this.api.get('/notifications/recent', {
      params: { limit }
    });
    return response.data;
  }

  async markNotificationAsRead(notificationId: number): Promise<Notification> {
    const response: AxiosResponse<Notification> = await this.api.put(`/notifications/${notificationId}/read`);
    return response.data;
  }

  async markAllNotificationsAsRead(): Promise<{ message: string }> {
    const response: AxiosResponse<{ message: string }> = await this.api.put('/notifications/mark-all-read');
    return response.data;
  }
}

const apiService = new ApiService();
export default apiService;
