import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { CssBaseline, Box } from '@mui/material';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Layout from './components/Layout';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import AddMarketPage from './pages/AddMarketPage';
import MarketDashboardPage from './pages/MarketDashboardPage';
import SupplierDashboardPage from './pages/SupplierDashboardPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import OrdersPage from './pages/OrdersPage';
import DeliveriesPage from './pages/DeliveriesPage';
import UsersPage from './pages/UsersPage';
import MarketsPage from './pages/MarketsPage';
import SuppliersPage from './pages/SuppliersPage';
import LoadingSpinner from './components/LoadingSpinner';
import MyMarketsPage from './pages/MyMarketsPage';
import MarketDetailPage from './pages/MarketDetailPage';
import ProductsPage from './pages/ProductsPage';
import MarketProductsPage from './pages/MarketProductsPage';
import SettingsPage from './pages/SettingsPage';

// Material-UI theme
const theme = createTheme({
  palette: {
    primary: { main: '#667eea' },
    secondary: { main: '#764ba2' },
    background: { default: '#f5f5f5' },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
  components: {
    MuiToolbar: {
      styleOverrides: {
        root: {
          '@media (max-width:600px)': {
            minHeight: 56,
            paddingLeft: 8,
            paddingRight: 8,
          },
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          '@media (max-width:600px)': {
            paddingLeft: 10,
            paddingRight: 10,
            minWidth: 36,
          },
        },
      },
      defaultProps: {
        size: 'medium',
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          '@media (max-width:600px)': {
            height: 26,
            fontSize: '0.75rem',
          },
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          '@media (max-width:600px)': {
            padding: 6,
          },
        },
      },
    },
    MuiContainer: {
      styleOverrides: {
        root: {
          '@media (max-width:600px)': {
            paddingLeft: 12,
            paddingRight: 12,
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          '@media (max-width:600px)': {
            padding: 12,
          },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          '@media (max-width:600px)': {
            padding: '6px 8px',
            whiteSpace: 'nowrap',
          },
        },
      },
    },
  },
});

// Protected Route Component
const ProtectedRoute: React.FC<{ children: React.ReactNode; requiredRole?: string }> = ({ 
  children, 
  requiredRole 
}) => {
  const { isAuthenticated, user, loading } = useAuth();

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && user?.role !== requiredRole) {
    // Role'e göre uygun dashboard'a yönlendir
    if (user?.role === 'SUPPLIER') {
      return <Navigate to="/supplier-dashboard" replace />;
    } else if (user?.role === 'ADMIN') {
      return <Navigate to="/admin-dashboard" replace />;
    } else if (user?.role === 'MARKET') {
      return <Navigate to="/market-dashboard" replace />;
    } else {
      return <Navigate to="/dashboard" replace />;
    }
  }

  return <>{children}</>;
};

// Public Route Component (redirect if authenticated)
const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <LoadingSpinner />;
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};

// Main App Routes
const AppRoutes: React.FC = () => {

  return (
    <Routes>
      {/* Public Routes */}
      <Route 
        path="/login" 
        element={
          <PublicRoute>
            <LoginPage />
          </PublicRoute>
        } 
      />
      <Route 
        path="/register" 
        element={
          <PublicRoute>
            <RegisterPage />
          </PublicRoute>
        } 
      />

      {/* Protected Routes */}
      <Route 
        path="/dashboard" 
        element={
          <ProtectedRoute>
            <Layout>
              <DashboardPage />
            </Layout>
          </ProtectedRoute>
        } 
      />

      {/* Role-based Routes */}
      <Route 
        path="/market-dashboard" 
        element={
          <ProtectedRoute requiredRole="MARKET">
            <Layout>
              <MarketDashboardPage />
            </Layout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/supplier-dashboard" 
        element={
          <ProtectedRoute requiredRole="SUPPLIER">
            <Layout>
              <SupplierDashboardPage />
            </Layout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/admin-dashboard" 
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Layout>
              <AdminDashboardPage />
            </Layout>
          </ProtectedRoute>
        } 
      />

      {/* General Dashboard */}
      <Route 
        path="/dashboard" 
        element={
          <ProtectedRoute>
            <Layout>
              <DashboardPage />
            </Layout>
          </ProtectedRoute>
        } 
      />

      {/* Common Routes */}
      <Route 
        path="/orders" 
        element={
          <ProtectedRoute>
            <Layout>
              <OrdersPage />
            </Layout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/deliveries" 
        element={
          <ProtectedRoute>
            <Layout>
              <DeliveriesPage />
            </Layout>
          </ProtectedRoute>
        } 
      />

      {/* Supplier: Add Market */}
      <Route 
        path="/markets/add" 
        element={
          <ProtectedRoute requiredRole="SUPPLIER">
            <Layout>
              <AddMarketPage />
            </Layout>
          </ProtectedRoute>
        } 
      />

      {/* Admin Only Routes */}
      <Route 
        path="/users" 
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Layout>
              <UsersPage />
            </Layout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/markets" 
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Layout>
              <MarketsPage />
            </Layout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/my-markets" 
        element={
          <ProtectedRoute requiredRole="SUPPLIER">
            <Layout>
              <MyMarketsPage />
            </Layout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/markets/:id" 
        element={
          <ProtectedRoute>
            <Layout>
              <MarketDetailPage />
            </Layout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/products" 
        element={
          <ProtectedRoute>
            <Layout>
              <ProductsPage />
            </Layout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/market-products" 
        element={
          <ProtectedRoute requiredRole="MARKET">
            <Layout>
              <MarketProductsPage />
            </Layout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/suppliers" 
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Layout>
              <SuppliersPage />
            </Layout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/settings" 
        element={
          <ProtectedRoute>
            <Layout>
              <SettingsPage />
            </Layout>
          </ProtectedRoute>
        } 
      />

      {/* Landing Page - Public */}
      <Route path="/" element={<LandingPage />} />
      
      {/* Default Route for authenticated users */}
      <Route path="/app" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};

const App: React.FC = () => {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <Router>
          <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
            <AppRoutes />
          </Box>
        </Router>
      </AuthProvider>
    </ThemeProvider>
  );
};

export default App;