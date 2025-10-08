import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, UserRole, AuthResponse, LoginRequest, RegisterRequest } from '../types';
import apiService from '../services/api';

interface AuthContextType {
  user: User | null;
  login: (credentials: LoginRequest) => Promise<AuthResponse>;
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
    const initAuth = async () => {
      try {
        // Önce localStorage'dan kontrol et
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
          const user = JSON.parse(storedUser);
          setUser(user);
          setLoading(false);
          return; // LocalStorage'da user varsa backend'e gitme
        }
        
        // LocalStorage'da user yoksa direkt loading'i false yap
        setUser(null);
        setLoading(false);
      } catch (error) {
        // Hata durumunda da loading'i false yap
        localStorage.removeItem('user');
        setUser(null);
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = async (credentials: LoginRequest) => {
    try {
      console.log('AuthContext: Login başlıyor...', credentials);
      const response = await apiService.login(credentials);
      console.log('AuthContext: Login response:', response);
      
      // Login başarılı, user'ı set et
      setUser(response.user);
      localStorage.setItem('user', JSON.stringify(response.user));
      console.log('AuthContext: User set edildi:', response.user);
      
      // Response'u döndür ki LoginPage'de role kontrolü yapılabilsin
      return response;
    } catch (error) {
      console.error('AuthContext: Login hatası:', error);
      throw error;
    }
  };

  const register = async (userData: RegisterRequest) => {
    try {
      const response: AuthResponse = await apiService.register(userData);
      
      setUser(response.user);
      localStorage.setItem('user', JSON.stringify(response.user));
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
    }
  };

  const isAuthenticated = !!user;
  const isAdmin = user?.role === UserRole.ADMIN;
  const isMarket = user?.role === UserRole.MARKET;
  const isSupplier = user?.role === UserRole.SUPPLIER;

  const value: AuthContextType = {
    user,
    login,
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