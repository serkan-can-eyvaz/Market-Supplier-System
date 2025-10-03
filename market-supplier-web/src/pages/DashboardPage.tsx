import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Paper,
  Avatar,
  Fade,
  Slide,
  Zoom,
  Chip,
  Stack,
  Divider,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  ShoppingCart,
  LocalShipping,
  People,
  Store,
  Business,
  Assignment,
  Dashboard as DashboardIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  AdminPanelSettings as AdminIcon,
  Storefront as StorefrontIcon,
  BusinessCenter as BusinessCenterIcon,
  Refresh as RefreshIcon,
  ArrowForward as ArrowForwardIcon,
  Speed as SpeedIcon,
  Analytics as AnalyticsIcon,
  Assessment as AssessmentIcon,
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import apiService from '../services/api';
import { UserStats, MarketStats, SupplierStats, OrderStats, DeliveryStats } from '../types';

// Professional Logo Component
const LogoIcon = ({ size = 48, children, bg }: { size?: number; children: React.ReactNode; bg: string }) => (
  <Box
    sx={{
      width: size,
      height: size,
      borderRadius: '50%',
      background: bg,
      boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
      border: '2px solid rgba(255,255,255,0.25)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}
  >
    {children}
  </Box>
);

const gradientFor = (color: string): string => {
  switch (color) {
    case 'primary':
      return 'linear-gradient(135deg, #667eea, #764ba2)';
    case 'secondary':
      return 'linear-gradient(135deg, #f093fb, #f5576c)';
    case 'info':
      return 'linear-gradient(135deg, #4facfe, #00f2fe)';
    case 'warning':
      return 'linear-gradient(135deg, #ff9800, #ff5722)';
    case 'success':
    default:
      return 'linear-gradient(135deg, #4caf50, #8bc34a)';
  }
};

interface StatCardProps {
  title: string;
  value: number;
  subtitle?: string;
  icon: React.ReactNode;
  color: string;
  trend?: {
    value: number;
    isPositive: boolean;
  };
}

const StatCard: React.FC<StatCardProps> = ({ title, value, subtitle, icon, color, trend }) => (
  <Fade in timeout={600}>
    <Card
      elevation={0}
      sx={{
        background: 'rgba(255, 255, 255, 0.95)',
        backdropFilter: 'blur(20px)',
        border: '1px solid rgba(255, 255, 255, 0.2)',
        borderRadius: 3,
        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        position: 'relative',
        overflow: 'hidden',
        '&::before': {
          content: '""',
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: '4px',
          background: `linear-gradient(90deg, ${color}, ${color}80)`,
        },
        '&:hover': {
          transform: 'translateY(-8px)',
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
        },
      }}
    >
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <LogoIcon size={48} bg={`linear-gradient(135deg, ${color}, ${color}80)`}>
            {icon}
          </LogoIcon>
          {trend && (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 0.5,
                px: 1.5,
                py: 0.5,
                borderRadius: 2,
                background: trend.isPositive ? 'rgba(76, 175, 80, 0.1)' : 'rgba(244, 67, 54, 0.1)',
                color: trend.isPositive ? '#4caf50' : '#f44336',
              }}
            >
              {trend.isPositive ? <TrendingUpIcon sx={{ fontSize: 16 }} /> : <TrendingDownIcon sx={{ fontSize: 16 }} />}
              <Typography variant="caption" fontWeight="600">
                {trend.value}%
              </Typography>
            </Box>
          )}
        </Box>
        <Typography variant="h3" fontWeight="800" sx={{ mb: 1, color: 'text.primary' }}>
          {value}
        </Typography>
        <Typography variant="h6" color="text.secondary" sx={{ mb: 1, fontWeight: 600 }}>
          {title}
        </Typography>
        {subtitle && (
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        )}
      </CardContent>
    </Card>
  </Fade>
);

const DashboardPage: React.FC = () => {
  const { user, isAdmin, isMarket, isSupplier } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState<{
    users?: UserStats;
    markets?: MarketStats;
    suppliers?: SupplierStats;
    orders?: OrderStats;
    deliveries?: DeliveryStats;
  }>({});

  useEffect(() => {
    const fetchStats = async () => {
      try {
        if (isAdmin) {
          const [usersStats, marketsStats, suppliersStats, ordersStats, deliveriesStats] = await Promise.all([
            apiService.getUserStats(),
            apiService.getMarketStats(),
            apiService.getSupplierStats(),
            apiService.getOrderStats(),
            apiService.getDeliveryStats(),
          ]);
          setStats({
            users: usersStats,
            markets: marketsStats,
            suppliers: suppliersStats,
            orders: ordersStats,
            deliveries: deliveriesStats,
          });
        }
      } catch (error) {
        console.error('Error fetching stats:', error);
      }
    };

    fetchStats();
  }, [isAdmin]);

  const getQuickActions = () => {
    if (isMarket) {
      return [
        { title: 'Sipariş Oluştur', icon: <ShoppingCart />, path: '/orders', color: 'primary' },
        { title: 'Siparişlerim', icon: <ShoppingCart />, path: '/orders', color: 'secondary' },
        { title: 'Market Ayarları', icon: <Store />, path: '/market-settings', color: 'info' },
      ];
    }
    
    if (isSupplier) {
      return [
        { title: 'Bekleyen Siparişler', icon: <ShoppingCart />, path: '/orders', color: 'primary' },
        { title: 'Teslimatlarım', icon: <LocalShipping />, path: '/deliveries', color: 'secondary' },
        { title: 'Tedarikçi Bilgilerim', icon: <Business />, path: '/supplier-dashboard', color: 'info' },
      ];
    }
    
    if (isAdmin) {
      return [
        { title: 'Kullanıcıları Yönet', icon: <People />, path: '/users', color: 'primary' },
        { title: 'Marketleri Yönet', icon: <Store />, path: '/markets', color: 'secondary' },
        { title: 'Tedarikçileri Yönet', icon: <Business />, path: '/suppliers', color: 'info' },
        { title: 'Tüm Siparişler', icon: <ShoppingCart />, path: '/orders', color: 'warning' },
        { title: 'Tüm Teslimatlar', icon: <LocalShipping />, path: '/deliveries', color: 'success' },
      ];
    }
    
    return [];
  };

  const getRoleSpecificDashboard = () => {
    if (isMarket) {
      return '/market-dashboard';
    }
    if (isSupplier) {
      return '/supplier-dashboard';
    }
    if (isAdmin) {
      return '/admin-dashboard';
    }
    return '/dashboard';
  };

  const getRoleIcon = () => {
    if (isAdmin) return <AdminIcon sx={{ fontSize: 24 }} />;
    if (isMarket) return <StorefrontIcon sx={{ fontSize: 24 }} />;
    if (isSupplier) return <BusinessCenterIcon sx={{ fontSize: 24 }} />;
    return <DashboardIcon sx={{ fontSize: 24 }} />;
  };

  const getRoleText = () => {
    if (isAdmin) return 'Yönetici Paneli';
    if (isMarket) return 'Market Sahibi Paneli';
    if (isSupplier) return 'Tedarikçi Paneli';
    return 'Dashboard';
  };

  const getRoleColor = () => {
    if (isAdmin) return '#667eea';
    if (isMarket) return '#f093fb';
    if (isSupplier) return '#4facfe';
    return '#43e97b';
  };

  return (
    <Box sx={{ minHeight: '100vh', background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)' }}>
      {/* Header Section */}
      <Slide direction="down" in timeout={600}>
        <Paper
          elevation={0}
          sx={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 3,
            p: 4,
            mb: 4,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 3 }}>
              <LogoIcon size={64} bg={`linear-gradient(135deg, ${getRoleColor()}, ${getRoleColor()}80)`}>
                {getRoleIcon()}
              </LogoIcon>
              <Box>
                <Typography variant="h3" fontWeight="800" sx={{ mb: 1 }}>
                  Hoş Geldiniz, {user?.name}!
                </Typography>
                <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 500 }}>
                  {getRoleText()}
                </Typography>
              </Box>
            </Box>
            <Tooltip title="Verileri Yenile">
              <IconButton
                onClick={() => window.location.reload()}
                sx={{
                  background: 'rgba(102, 126, 234, 0.1)',
                  color: 'primary.main',
                  '&:hover': {
                    background: 'rgba(102, 126, 234, 0.2)',
                    transform: 'scale(1.05)',
                  },
                }}
              >
                <RefreshIcon />
              </IconButton>
            </Tooltip>
          </Box>
        </Paper>
      </Slide>

      {/* Quick Actions Section */}
      <Slide direction="up" in timeout={800}>
        <Paper
          elevation={0}
          sx={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 3,
            p: 4,
            mb: 4,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
            <LogoIcon size={40} bg={'linear-gradient(135deg, #667eea, #764ba2)'}>
              <SpeedIcon />
            </LogoIcon>
            <Typography variant="h4" fontWeight="700" sx={{ background: 'linear-gradient(135deg, #667eea, #764ba2)', backgroundClip: 'text', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              Hızlı İşlemler
            </Typography>
          </Box>
          
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)', lg: 'repeat(5, 1fr)' }, gap: 3 }}>
            {getQuickActions().map((action, index) => (
              <Zoom in timeout={1000 + index * 200} key={index}>
                <Card
                  elevation={0}
                  sx={{
                    background: 'rgba(255, 255, 255, 0.8)',
                    backdropFilter: 'blur(10px)',
                    border: '1px solid rgba(255, 255, 255, 0.3)',
                    borderRadius: 3,
                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                    cursor: 'pointer',
                    position: 'relative',
                    overflow: 'hidden',
                    '&:hover': {
                      transform: 'translateY(-8px) scale(1.02)',
                      boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
                      '& .action-icon': {
                        transform: 'scale(1.1) rotate(5deg)',
                      },
                      '& .action-arrow': {
                        transform: 'translateX(4px)',
                      },
                    },
                  }}
                  onClick={() => navigate(action.path)}
                >
                  <CardContent sx={{ p: 3, textAlign: 'center' }}>
                    <LogoIcon size={56} bg={gradientFor(action.color)}>
                      {action.icon}
                    </LogoIcon>
                    <Typography variant="h6" fontWeight="600" sx={{ mb: 1, color: 'text.primary' }}>
                      {action.title}
                    </Typography>
                    <Box
                      className="action-arrow"
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: action.color === 'primary' ? '#667eea' : action.color === 'secondary' ? '#f093fb' : action.color === 'info' ? '#4facfe' : action.color === 'warning' ? '#ff9800' : '#4caf50',
                        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                      }}
                    >
                      <ArrowForwardIcon sx={{ fontSize: 20 }} />
                    </Box>
                  </CardContent>
                </Card>
              </Zoom>
            ))}
          </Box>
        </Paper>
      </Slide>

      {/* Detailed Panel Section */}
      <Slide direction="up" in timeout={1000}>
        <Paper
          elevation={0}
          sx={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 3,
            p: 4,
            mb: 4,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
            <LogoIcon size={40} bg={'linear-gradient(135deg, #667eea, #764ba2)'}>
              <AnalyticsIcon />
            </LogoIcon>
            <Typography variant="h4" fontWeight="700" sx={{ background: 'linear-gradient(135deg, #667eea, #764ba2)', backgroundClip: 'text', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              Detaylı Panel
            </Typography>
          </Box>
          
          <Button
            variant="contained"
            size="large"
            startIcon={<Assignment />}
            onClick={() => navigate(getRoleSpecificDashboard())}
            sx={{
              background: 'linear-gradient(135deg, #667eea, #764ba2)',
              borderRadius: 3,
              py: 2,
              px: 4,
              fontSize: '1.1rem',
              fontWeight: 600,
              textTransform: 'none',
              boxShadow: '0 8px 25px rgba(102, 126, 234, 0.3)',
              '&:hover': {
                background: 'linear-gradient(135deg, #5a6fd8, #6a4190)',
                transform: 'translateY(-2px)',
                boxShadow: '0 12px 35px rgba(102, 126, 234, 0.4)',
              },
            }}
          >
            {isMarket && 'Market Dashboard'}
            {isSupplier && 'Tedarikçi Dashboard'}
            {isAdmin && 'Admin Dashboard'}
          </Button>
        </Paper>
      </Slide>

      {/* Admin Statistics Section */}
      {isAdmin && (
        <Slide direction="up" in timeout={1200}>
          <Paper
            elevation={0}
            sx={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: 3,
              p: 4,
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 4 }}>
              <LogoIcon size={40} bg={'linear-gradient(135deg, #667eea, #764ba2)'}>
                <AssessmentIcon />
              </LogoIcon>
              <Typography variant="h4" fontWeight="700" sx={{ background: 'linear-gradient(135deg, #667eea, #764ba2)', backgroundClip: 'text', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                Sistem İstatistikleri
              </Typography>
            </Box>
            
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' }, gap: 3 }}>
              {stats.users && (
                <StatCard
                  title="Toplam Kullanıcı"
                  value={stats.users.totalUsers}
                  subtitle={`Market: ${stats.users.marketUsers} | Tedarikçi: ${stats.users.supplierUsers}`}
                  icon={<People />}
                  color="#667eea"
                  trend={{ value: 12, isPositive: true }}
                />
              )}

              {stats.markets && (
                <StatCard
                  title="Toplam Market"
                  value={stats.markets.totalMarkets}
                  subtitle="Aktif market sayısı"
                  icon={<Store />}
                  color="#f093fb"
                  trend={{ value: 8, isPositive: true }}
                />
              )}

              {stats.suppliers && (
                <StatCard
                  title="Toplam Tedarikçi"
                  value={stats.suppliers.totalSuppliers}
                  subtitle="Kayıtlı tedarikçi"
                  icon={<Business />}
                  color="#4facfe"
                  trend={{ value: 15, isPositive: true }}
                />
              )}

              {stats.orders && (
                <StatCard
                  title="Toplam Sipariş"
                  value={stats.orders.totalOrders}
                  subtitle={`Bekleyen: ${stats.orders.pendingOrders} | Toplam: ${stats.orders.totalOrders}`}
                  icon={<ShoppingCart />}
                  color="#43e97b"
                  trend={{ value: -3, isPositive: false }}
                />
              )}
            </Box>
          </Paper>
        </Slide>
      )}
    </Box>
  );
};

export default DashboardPage;
