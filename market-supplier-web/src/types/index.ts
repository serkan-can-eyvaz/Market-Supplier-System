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
  user: {
    id: number;
    name: string;
    email: string;
    role: UserRole;
  };
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

export interface ProductResponse {
  id: number;
  name: string;
  description?: string;
  unit: string;
  price: number;
  stockQuantity: number;
  isActive: boolean;
  supplierCompanyName?: string;
  supplierId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductCreateRequest {
  name: string;
  description?: string;
  unit: string;
  price: number;
  stockQuantity: number;
}

export interface ProductUpdateRequest {
  name?: string;
  description?: string;
  unit?: string;
  price?: number;
  stockQuantity?: number;
  isActive?: boolean;
}

// Cart Types
export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  unit: string;
  price: number;
  quantity: number;
  lineTotal: number;
  supplierName?: string;
}

export interface Cart {
  id: number;
  marketId: number;
  marketName: string;
  items: CartItem[];
  totalAmount: number;
  totalItems: number;
  createdAt: string;
  updatedAt: string;
}

export interface CartItemRequest {
  productId: number;
  quantity: number;
}

export interface CartResponse {
  id: number;
  marketId: number;
  marketName: string;
  items: CartItem[];
  totalAmount: number;
  totalItems: number;
  createdAt: string;
  updatedAt: string;
}