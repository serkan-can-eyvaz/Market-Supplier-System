import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { 
  AuthResponse, 
  LoginRequest, 
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
  RoutePlanRequest,
  PaginatedResponse,
  UserStats,
  MarketStats,
  SupplierStats,
  OrderStats,
  DeliveryStats,
  Product
} from '../types';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor to add auth token
    this.api.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Response interceptor to handle errors
    this.api.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;
        if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
          originalRequest._retry = true;
          try {
            const refreshToken = localStorage.getItem('refreshToken');
            if (!refreshToken) throw new Error('no refresh token');
            const refreshResp = await axios.post((process.env.REACT_APP_API_URL || 'http://localhost:8080/api') + '/auth/refresh', { refreshToken: refreshToken });
            const newToken = refreshResp.data?.token;
            const newRefreshToken = refreshResp.data?.refreshToken;
            if (newToken) {
              localStorage.setItem('token', newToken);
              if (newRefreshToken) {
                localStorage.setItem('refreshToken', newRefreshToken);
              }
              originalRequest.headers = originalRequest.headers || {};
              originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
              return this.api(originalRequest);
            }
          } catch (_) {
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            window.location.href = '/login';
          }
        }
        return Promise.reject(error);
      }
    );
  }

  // Auth API
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response: AxiosResponse<AuthResponse> = await this.api.post('/auth/login', credentials);
    return response.data;
  }

  async register(userData: RegisterRequest): Promise<AuthResponse> {
    const response: AxiosResponse<AuthResponse> = await this.api.post('/auth/register', userData);
    return response.data;
  }

  async getCurrentUser(): Promise<User> {
    const response: AxiosResponse<any> = await this.api.get('/auth/me');
    return {
      id: response.data.id,
      name: response.data.name,
      email: response.data.email,
      role: response.data.role as UserRole,
      createdAt: response.data.createdAt
    };
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
    const response: AxiosResponse<PaginatedResponse<Market>> = await this.api.get('/markets/my-markets', {
      params: { page, size, sortBy, sortDir }
    });
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

  async planDeliveryRoute(deliveryId: number, routeData: RoutePlanRequest): Promise<Delivery> {
    const response: AxiosResponse<Delivery> = await this.api.post(`/deliveries/${deliveryId}/plan-route`, routeData);
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

  // Route metrics
  async saveRouteMetrics(payload: {
    supplierId: number;
    totalDistanceKm: number;
    totalDurationMin: number;
    stopsCount: number;
    fuelConsumptionLPer100km?: number;
    fuelPriceTlPerL?: number;
  }): Promise<any> {
    const response = await this.api.post('/deliveries/route-metrics', payload);
    return response.data;
  }

  async recentRouteMetrics(supplierId: number): Promise<any[]> {
    const response = await this.api.get(`/deliveries/route-metrics/recent/${supplierId}`);
    return response.data;
  }

  // Route plan snapshot
  async saveRoutePlan(planJson: string): Promise<number> {
    const response = await this.api.post('/deliveries/route-plan', planJson, { headers: { 'Content-Type': 'application/json' } });
    return response.data as number;
  }

  async getActiveRoutePlan(): Promise<string | null> {
    const response = await this.api.get('/deliveries/route-plan/active');
    return response.data ?? null;
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
  async getProducts(page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc'): Promise<PaginatedResponse<Product>> {
    const response: AxiosResponse<PaginatedResponse<Product>> = await this.api.get('/products', {
      params: { page, size, sortBy, sortDir }
    });
    return response.data;
  }

  async getAllProducts(): Promise<Product[]> {
    const response: AxiosResponse<Product[]> = await this.api.get('/products/all');
    return response.data;
  }

  async createProduct(productData: {
    name: string;
    description?: string;
    unit: string;
    price: number;
    stockQuantity?: number;
  }): Promise<Product> {
    const response: AxiosResponse<Product> = await this.api.post('/products', null, {
      params: productData
    });
    return response.data;
  }

  async updateProduct(productId: number, productData: {
    name: string;
    description?: string;
    unit: string;
    price: number;
    stockQuantity?: number;
  }): Promise<Product> {
    const response: AxiosResponse<Product> = await this.api.put(`/products/${productId}`, null, {
      params: productData
    });
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
}

const apiService = new ApiService();
export default apiService;
