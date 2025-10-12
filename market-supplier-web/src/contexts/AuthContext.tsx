import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, UserRole, AuthResponse, LoginRequest, PhoneLoginRequest, RegisterRequest } from '../types';
import apiService from '../services/api';

interface AuthContextType {
  user: User | null;
  login: (credentials: LoginRequest) => Promise<AuthResponse>;
  loginWithPhone: (credentials: PhoneLoginRequest) => Promise<AuthResponse>;
  register: (userData: RegisterRequest) => Promise<void>;
  logout: () => void;
  loading: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isMarket: boolean;
  isSupplier: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Sadece localStorage'dan kontrol et, backend'e gitme
    const storedUser = localStorage.getItem('user');
    const storedToken = localStorage.getItem('token');
    
    if (storedUser && storedToken) {
      try {
        const user = JSON.parse(storedUser);
        setUser(user);
      } catch (error) {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        setUser(null);
      }
    } else {
      // Token yoksa user'ı da temizle
      if (!storedToken && storedUser) {
        localStorage.removeItem('user');
      }
      setUser(null);
    }
    setLoading(false);
  }, []);

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await apiService.login(credentials);
      
      // Login başarılı, user'ı set et
      setUser(response.user);
      localStorage.setItem('user', JSON.stringify(response.user));
      
      // Token'ı localStorage'a kaydet
      if (response.token) {
        localStorage.setItem('token', response.token);
      }
      
      // Response'u döndür ki LoginPage'de role kontrolü yapılabilsin
      return response;
    } catch (error) {
      throw error;
    }
  };

  const loginWithPhone = async (credentials: PhoneLoginRequest) => {
    try {
      const response = await apiService.loginWithPhone(credentials);
      
      // Login başarılı, user'ı set et
      setUser(response.user);
      localStorage.setItem('user', JSON.stringify(response.user));
      
      // Token'ı localStorage'a kaydet
      if (response.token) {
        localStorage.setItem('token', response.token);
      }
      
      // Response'u döndür ki LoginPage'de role kontrolü yapılabilsin
      return response;
    } catch (error) {
      throw error;
    }
  };

  const register = async (userData: RegisterRequest) => {
    try {
      const response: AuthResponse = await apiService.register(userData);
      
      setUser(response.user);
      localStorage.setItem('user', JSON.stringify(response.user));
      
      // Token'ı localStorage'a kaydet
      if (response.token) {
        localStorage.setItem('token', response.token);
      }
    } catch (error) {
      throw error;
    }
  };

  const logout = async () => {
    try {
      // Call backend logout endpoint
      await apiService.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
      localStorage.removeItem('user');
      localStorage.removeItem('token');
    }
  };

  const isAuthenticated = !!user;
  const isAdmin = user?.role === UserRole.ADMIN;
  const isMarket = user?.role === UserRole.MARKET;
  const isSupplier = user?.role === UserRole.SUPPLIER;

  const value: AuthContextType = {
    user,
    login,
    loginWithPhone,
    register,
    logout,
    loading,
    isAuthenticated,
    isAdmin,
    isMarket,
    isSupplier,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};