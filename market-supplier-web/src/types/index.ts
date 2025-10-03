// Pagination Types
export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// User Types
export interface User {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  createdAt: string;
}

export enum UserRole {
  MARKET = 'MARKET',
  SUPPLIER = 'SUPPLIER',
  ADMIN = 'ADMIN'
}

// Auth Types
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  userId: number;
  name: string;
  email: string;
  role: UserRole;
  message?: string;
}

// Market Types
export interface Market {
  id: number;
  name: string;
  address: string;
  phone: string;
  userId: number;
  userName: string;
  userEmail: string;
  createdAt: string;
  orderCount?: number;
}

export interface MarketRequest {
  name: string;
  address: string;
  phone: string;
}

// Supplier Types
export interface Supplier {
  id: number;
  companyName: string;
  phone: string;
  address?: string;
  phoneNumberId?: string;
  userId: number;
  userName: string;
  userEmail: string;
  createdAt: string;
  deliveryCount?: number;
}

export interface SupplierRequest {
  companyName: string;
  phone: string;
  phoneNumberId?: string;
}

// Order Types
export interface Order {
  id: number;
  marketId: number;
  marketName: string;
  marketAddress: string;
  status: OrderStatus;
  createdAt: string;
  orderItems: OrderItem[];
  totalAmount?: number;
  itemCount?: number;
  market?: {
    id: number;
    name: string;
    address: string;
  };
}

export enum OrderStatus {
  PENDING = 'PENDING',
  DELIVERED = 'DELIVERED'
}

export interface OrderItem {
  id?: number;
  productName: string;
  quantity: number;
  unit: string;
  price: number;
  totalPrice?: number;
}

export interface OrderItemRequest {
  productName: string;
  quantity: number;
  unit: string;
  price: number;
}

// Delivery Types
export interface Delivery {
  id: number;
  orderId: number;
  marketName: string;
  marketAddress: string;
  marketLat?: number;
  marketLng?: number;
  supplierId: number;
  supplierCompanyName: string;
  deliveryStatus: DeliveryStatus;
  deliveryTime?: string;
  routeInfo?: string;
  createdAt: string;
  updatedAt?: string;
}

export enum DeliveryStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  DELIVERED = 'DELIVERED'
}

export interface DeliveryRequest {
  orderId: number;
  supplierId: number;
}

export interface RoutePlanRequest {
  routeInfo: string;
}

// API Response Types
export interface ApiResponse<T> {
  data?: T;
  message?: string;
  error?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// Statistics Types
export interface UserStats {
  totalUsers: number;
  marketUsers: number;
  supplierUsers: number;
  adminUsers: number;
}

export interface MarketStats {
  totalMarkets: number;
  marketsWithOrderCounts: any[];
}

export interface SupplierStats {
  totalSuppliers: number;
  suppliersWithDeliveryCounts: any[];
}

export interface OrderStats {
  totalOrders: number;
  pendingOrders: number;
  deliveredOrders: number;
  ordersWithTotalAmounts?: any[];
}

export interface DeliveryStats {
  totalDeliveries: number;
  inProgressDeliveries: number;
  completedDeliveries: number;
}

// Route Metrics Types
export interface RouteMetricsRequest {
  supplierId: number;
  totalDistanceKm: number;
  totalDurationMin: number;
  stopsCount: number;
  fuelConsumptionLPer100km?: number;
  fuelPriceTlPerL?: number;
}

export interface RouteMetricsResponse {
  id: number;
  supplierId: number;
  totalDistanceKm: number;
  totalDurationMin: number;
  stopsCount: number;
  fuelConsumptionLPer100km: number;
  fuelPriceTlPerL: number;
  fuelEstimateLiters: number;
  fuelCostEstimateTl: number;
  createdAt: string;
}

// Product Types
export interface Product {
  id: number;
  name: string;
  description?: string;
  unit: string;
  price: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  supplier?: {
    id: number;
    companyName: string;
  };
  stockQuantity?: number;
}
