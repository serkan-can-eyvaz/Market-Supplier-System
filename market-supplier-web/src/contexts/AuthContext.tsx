import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, UserRole, AuthResponse, LoginRequest, RegisterRequest } from '../types';
import apiService from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (credentials: LoginRequest) => Promise<void>;
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
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem('token');
      const storedRefreshToken = localStorage.getItem('refreshToken');
      const storedUser = localStorage.getItem('user');

      if (storedToken && storedUser) {
        try {
          setToken(storedToken);
          setUser(JSON.parse(storedUser));
          
          // Verify token with backend
          const currentUser = await apiService.getCurrentUser();
          setUser(currentUser);
        } catch (error) {
          // Token is invalid, try refresh
          if (storedRefreshToken) {
            try {
              const refreshResp = await apiService.refreshToken(storedRefreshToken);
              setToken(refreshResp.token);
              localStorage.setItem('token', refreshResp.token);
              if (refreshResp.refreshToken) {
                localStorage.setItem('refreshToken', refreshResp.refreshToken);
              }
              const currentUser = await apiService.getCurrentUser();
              setUser(currentUser);
            } catch (refreshError) {
              // Both token and refresh failed, clear storage
              localStorage.removeItem('token');
              localStorage.removeItem('refreshToken');
              localStorage.removeItem('user');
              setToken(null);
              setUser(null);
            }
          } else {
            // No refresh token, clear storage
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            setToken(null);
            setUser(null);
          }
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = async (credentials: LoginRequest) => {
    try {
      const response: AuthResponse = await apiService.login(credentials);
      
      setToken(response.token);
      setUser({
        id: response.userId,
        name: response.name,
        email: response.email,
        role: response.role as UserRole,
        createdAt: new Date().toISOString()
      });

      localStorage.setItem('token', response.token);
      localStorage.setItem('refreshToken', response.refreshToken);
      localStorage.setItem('user', JSON.stringify({
        id: response.userId,
        name: response.name,
        email: response.email,
        role: response.role as UserRole,
        createdAt: new Date().toISOString()
      }));
    } catch (error) {
      throw error;
    }
  };

  const register = async (userData: RegisterRequest) => {
    try {
      const response: AuthResponse = await apiService.register(userData);
      
      setToken(response.token);
      setUser({
        id: response.userId,
        name: response.name,
        email: response.email,
        role: response.role as UserRole,
        createdAt: new Date().toISOString()
      });

      localStorage.setItem('token', response.token);
      localStorage.setItem('refreshToken', response.refreshToken);
      localStorage.setItem('user', JSON.stringify({
        id: response.userId,
        name: response.name,
        email: response.email,
        role: response.role as UserRole,
        createdAt: new Date().toISOString()
      }));
    } catch (error) {
      throw error;
    }
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  };

  const isAuthenticated = !!user && !!token;
  const isAdmin = user?.role === UserRole.ADMIN;
  const isMarket = user?.role === UserRole.MARKET;
  const isSupplier = user?.role === UserRole.SUPPLIER;

  const value: AuthContextType = {
    user,
    token,
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
