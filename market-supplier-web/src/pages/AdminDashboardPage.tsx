import React, { useEffect, useState, useRef } from 'react';
import { 
  Box, 
  Typography, 
  Card, 
  CardContent, 
  Alert, 
  Button, 
  Stack, 
  Table, 
  TableBody, 
  TableCell, 
  TableContainer, 
  TableHead, 
  TableRow, 
  Chip, 
  Pagination,
  Paper,
  IconButton,
  Tooltip,
  LinearProgress,
  Divider,
  Avatar,
  Badge,
  Fade,
  Zoom,
  Slide
} from '@mui/material';
import { 
  People as PeopleIcon,
  Store as StoreIcon,
  Business as BusinessIcon,
  ShoppingCart as ShoppingCartIcon,
  LocalShipping as DeliveryIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  Assessment as AssessmentIcon,
  Refresh as RefreshIcon,
  Visibility as ViewIcon,
  CheckCircle as CheckCircleIcon,
  Pending as PendingIcon,
  Cancel as CancelIcon,
  Schedule as ScheduleIcon,
  Dashboard as DashboardIcon,
  Analytics as AnalyticsIcon,
  Speed as SpeedIcon,
  AttachMoney as MoneyIcon,
  Inventory as InventoryIcon,
  Notifications as NotificationsIcon,
  Settings as SettingsIcon,
  ArrowForward as ArrowForwardIcon,
  MoreVert as MoreVertIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  Error as ErrorIcon,
  CheckCircleOutline as CheckCircleOutlineIcon,
  Close as CloseIcon,
  ExpandMore as ExpandMoreIcon
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import apiService from '../services/api';
import { UserStats, MarketStats, SupplierStats, OrderStats, DeliveryStats, Order, Delivery, PaginatedResponse, OrderStatus, DeliveryStatus } from '../types';
import { LineChart } from '@mui/x-charts';

// Professional Logo Component
const LogoIcon = ({ size = 48, children, bg = 'linear-gradient(135deg, #667eea, #764ba2)' }: { size?: number; children: React.ReactNode; bg?: string }) => (
  <Box
    sx={{
      width: size,
      height: size,
      borderRadius: '50%',
      background: bg,
      boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
      border: '2px solid rgba(255,255,255,0.25)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}
  >
    {children}
  </Box>
);

const StatCard: React.FC<{ 
  title: string; 
  value: number | string; 
  icon: React.ReactNode; 
  color: string;
  subtitle?: string;
  trend?: number;
  delay?: number;
}> = ({ title, value, icon, color, subtitle, trend, delay = 0 }) => (
  <Fade in timeout={800 + delay}>
    <Card 
      elevation={0}
      sx={{ 
        background: 'rgba(255, 255, 255, 0.95)',
        backdropFilter: 'blur(20px)',
        border: '1px solid rgba(255, 255, 255, 0.2)',
        borderRadius: 4,
        transition: 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)',
        position: 'relative',
        overflow: 'hidden',
        '&:hover': {
          transform: 'translateY(-8px) scale(1.02)',
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
          border: '1px solid rgba(255, 255, 255, 0.3)',
        },
        '&::before': {
          content: '""',
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: '4px',
          background: `linear-gradient(90deg, ${color}, ${color}80)`,
        }
      }}
    >
      <CardContent sx={{ p: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 3 }}>
          <LogoIcon size={56} bg={`linear-gradient(135deg, ${color}, ${color}CC)`}>
            {icon}
          </LogoIcon>
          {trend !== undefined && (
            <Box sx={{ 
              display: 'flex', 
              alignItems: 'center', 
              px: 1.5,
              py: 0.5,
              borderRadius: 2,
              background: trend >= 0 ? 'rgba(76, 175, 80, 0.1)' : 'rgba(244, 67, 54, 0.1)',
              color: trend >= 0 ? 'success.main' : 'error.main'
            }}>
              {trend >= 0 ? <TrendingUpIcon sx={{ fontSize: 16, mr: 0.5 }} /> : <TrendingDownIcon sx={{ fontSize: 16, mr: 0.5 }} />}
              <Typography variant="caption" fontWeight="bold" fontSize="12px">
                {trend > 0 ? '+' : ''}{trend}%
              </Typography>
            </Box>
          )}
        </Box>
        
        <Typography variant="h3" fontWeight="800" color="text.primary" sx={{ mb: 1, lineHeight: 1.2 }}>
          {value}
        </Typography>
        
        <Typography variant="h6" color="text.secondary" sx={{ mb: 2, fontWeight: 500 }}>
          {title}
        </Typography>
        
        {subtitle && (
          <Typography variant="body2" color="text.secondary" sx={{ opacity: 0.8 }}>
            {subtitle}
          </Typography>
        )}
    </CardContent>
  </Card>
  </Fade>
);

interface Notification {
  id: number;
  type: 'success' | 'warning' | 'error' | 'info';
  title: string;
  message: string;
  time: string;
  isRead: boolean;
}

const AdminDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [error, setError] = useState<string>('');
  const [userStats, setUserStats] = useState<UserStats | null>(null);
  const [marketStats, setMarketStats] = useState<MarketStats | null>(null);
  const [supplierStats, setSupplierStats] = useState<SupplierStats | null>(null);
  const [orderStats, setOrderStats] = useState<OrderStats | null>(null);
  const [deliveryStats, setDeliveryStats] = useState<DeliveryStats | null>(null);
  const [productStats, setProductStats] = useState<{ totalProducts: number } | null>(null);
  
  // Notifications state
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const notificationRef = useRef<HTMLDivElement>(null);
  const [notificationIdCounter, setNotificationIdCounter] = useState(1);
  
  // Orders state
  const [orders, setOrders] = useState<Order[]>([]);
  const [ordersPagination, setOrdersPagination] = useState({
    page: 0,
    size: 5,
    totalElements: 0,
    totalPages: 0
  });
  
  // Deliveries state
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const [deliveriesPagination, setDeliveriesPagination] = useState({
    page: 0,
    size: 5,
    totalElements: 0,
    totalPages: 0
  });

  const loadOrders = async (page: number = ordersPagination.page) => {
    try {
      console.log('Loading orders for admin...');
      const response: PaginatedResponse<Order> = await apiService.getAllOrders(page, ordersPagination.size);
      console.log('Orders response:', response);
      
      // Check for new orders before updating state
      const previousOrders = orders;
      const newOrders = response.content || [];
      checkForNewOrders(newOrders, previousOrders);
      
      setOrders(newOrders);
      setOrdersPagination({
        page: response.page,
        size: response.size,
        totalElements: response.totalElements,
        totalPages: response.totalPages
      });
    } catch (e: any) {
      console.error('Orders loading error:', e);
      setError('Siparişler yüklenirken hata: ' + (e.response?.data?.message || e.message));
      addNotification('error', 'Sipariş Yükleme Hatası', 'Siparişler yüklenirken bir hata oluştu');
    }
  };

  const loadDeliveries = async (page: number = deliveriesPagination.page) => {
    try {
      console.log('Loading deliveries for admin...');
      const response: PaginatedResponse<Delivery> = await apiService.getAllDeliveries(page, deliveriesPagination.size);
      console.log('Deliveries response:', response);
      
      const newDeliveries = response.content || [];
      
      // Check for delayed deliveries
      checkForDelayedDeliveries(newDeliveries);
      
      setDeliveries(newDeliveries);
      setDeliveriesPagination({
        page: response.page,
        size: response.size,
        totalElements: response.totalElements,
        totalPages: response.totalPages
      });
    } catch (e: any) {
      console.error('Deliveries loading error:', e);
      setError('Teslimatlar yüklenirken hata: ' + (e.response?.data?.message || e.message));
      addNotification('error', 'Teslimat Yükleme Hatası', 'Teslimatlar yüklenirken bir hata oluştu');
    }
  };

  useEffect(() => {
    const load = async () => {
      try {
        setError('');
        
        // Check for system updates first
        checkForSystemUpdates();
        
        const [u, m, s, o, d, p] = await Promise.all([
          apiService.getUserStats(),
          apiService.getMarketStats(),
          apiService.getSupplierStats(),
          apiService.getOrderStats(),
          apiService.getDeliveryStats(),
          apiService.getTotalOrderItems().then(data => ({ totalProducts: data.totalItems }))
        ]);
        setUserStats(u);
        setMarketStats(m);
        setSupplierStats(s);
        setOrderStats(o);
        setDeliveryStats(d);
        setProductStats(p);
        
        // Load orders and deliveries
        await Promise.all([
          loadOrders(0),
          loadDeliveries(0)
        ]);
      } catch (e: any) {
        setError(e.response?.data?.message || 'İstatistikler yüklenemedi');
        addNotification('error', 'Sistem Hatası', 'İstatistikler yüklenirken bir hata oluştu');
      }
    };
    load();
  }, []);

  const getStatusColor = (status: string) => {
    switch (status) {
      case OrderStatus.PENDING: return 'warning';
      case OrderStatus.DELIVERED: return 'success';
      case DeliveryStatus.IN_PROGRESS: return 'primary';
      case DeliveryStatus.DELIVERED: return 'info';
      default: return 'default';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case OrderStatus.PENDING: return 'Beklemede';
      case OrderStatus.DELIVERED: return 'Teslim Edildi';
      case DeliveryStatus.IN_PROGRESS: return 'Devam Ediyor';
      case DeliveryStatus.DELIVERED: return 'Teslim Edildi';
      default: return status;
    }
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'success': return <CheckCircleOutlineIcon sx={{ fontSize: 20 }} />;
      case 'warning': return <WarningIcon sx={{ fontSize: 20 }} />;
      case 'error': return <ErrorIcon sx={{ fontSize: 20 }} />;
      case 'info': return <InfoIcon sx={{ fontSize: 20 }} />;
      default: return <InfoIcon sx={{ fontSize: 20 }} />;
    }
  };

  const getNotificationColor = (type: string) => {
    switch (type) {
      case 'success': return '#4caf50';
      case 'warning': return '#ff9800';
      case 'error': return '#f44336';
      case 'info': return '#2196f3';
      default: return '#2196f3';
    }
  };

  const markNotificationAsRead = (id: number) => {
    setNotifications(prev => 
      prev.map(notification => 
        notification.id === id 
          ? { ...notification, isRead: true }
          : notification
      )
    );
  };

  const markAllAsRead = () => {
    setNotifications(prev => 
      prev.map(notification => ({ ...notification, isRead: true }))
    );
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  // Notification creation functions
  const addNotification = (type: 'success' | 'warning' | 'error' | 'info', title: string, message: string) => {
    const newNotification: Notification = {
      id: notificationIdCounter,
      type,
      title,
      message,
      time: 'Şimdi',
      isRead: false
    };
    
    setNotifications(prev => [newNotification, ...prev]);
    setNotificationIdCounter(prev => prev + 1);
    
    // Update time progressively
    setTimeout(() => {
      setNotifications(prev => 
        prev.map(notification => 
          notification.id === newNotification.id 
            ? { ...notification, time: '1 dakika önce' }
            : notification
        )
      );
    }, 60000);
    
    setTimeout(() => {
      setNotifications(prev => 
        prev.map(notification => 
          notification.id === newNotification.id 
            ? { ...notification, time: '5 dakika önce' }
            : notification
        )
      );
    }, 300000);
    
    setTimeout(() => {
      setNotifications(prev => 
        prev.map(notification => 
          notification.id === newNotification.id 
            ? { ...notification, time: '15 dakika önce' }
            : notification
        )
      );
    }, 900000);
  };

  const checkForNewOrders = (currentOrders: Order[], previousOrders: Order[]) => {
    if (previousOrders.length === 0) return;
    
    const newOrders = currentOrders.filter(
      currentOrder => !previousOrders.some(prevOrder => prevOrder.id === currentOrder.id)
    );
    
    newOrders.forEach(order => {
      addNotification(
        'success',
        'Yeni Sipariş',
        `${order.marketName} marketinden yeni sipariş alındı (₺${order.totalAmount?.toFixed(2) || '0.00'})`
      );
    });
  };

  const checkForDelayedDeliveries = (deliveries: Delivery[]) => {
    const now = new Date();
    const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    
    const delayedDeliveries = deliveries.filter(delivery => {
      if (delivery.deliveryStatus === DeliveryStatus.DELIVERED) return false;
      
      const createdAt = new Date(delivery.createdAt);
      return createdAt < oneDayAgo;
    });
    
    if (delayedDeliveries.length > 0) {
      addNotification(
        'warning',
        'Teslimat Gecikmesi',
        `${delayedDeliveries.length} teslimatın teslim süresi 1 günü aştı`
      );
    }
  };

  const checkForSystemUpdates = () => {
    // Check if this is a page refresh (system update simulation)
    const lastUpdate = localStorage.getItem('lastSystemUpdate');
    const now = new Date().toISOString();
    
    if (!lastUpdate || lastUpdate !== now.split('T')[0]) {
      addNotification(
        'info',
        'Sistem Güncellemesi',
        'Sistem başarıyla güncellendi ve yeni özellikler eklendi'
      );
      localStorage.setItem('lastSystemUpdate', now.split('T')[0]);
    }
  };

  // Click outside to close notifications
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (notificationRef.current && !notificationRef.current.contains(event.target as Node)) {
        setShowNotifications(false);
      }
    };

    if (showNotifications) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showNotifications]);

  // Periodic check for new data and notifications
  useEffect(() => {
    const interval = setInterval(async () => {
      try {
        // Check for new orders
        const ordersResponse = await apiService.getAllOrders(0, 5);
        const currentOrders = ordersResponse.content || [];
        checkForNewOrders(currentOrders, orders);
        
        // Check for delayed deliveries
        const deliveriesResponse = await apiService.getAllDeliveries(0, 5);
        const currentDeliveries = deliveriesResponse.content || [];
        checkForDelayedDeliveries(currentDeliveries);
        
        // Update stats
        const [u, m, s, o, d, p] = await Promise.all([
          apiService.getUserStats(),
          apiService.getMarketStats(),
          apiService.getSupplierStats(),
          apiService.getOrderStats(),
          apiService.getDeliveryStats(),
          apiService.getTotalOrderItems().then(data => ({ totalProducts: data.totalItems }))
        ]);
        setUserStats(u);
        setMarketStats(m);
        setSupplierStats(s);
        setOrderStats(o);
        setDeliveryStats(d);
        setProductStats(p);
        
      } catch (e) {
        console.error('Periodic check error:', e);
      }
    }, 30000); // Check every 30 seconds

    return () => clearInterval(interval);
  }, [orders, deliveries]);

  return (
    <Box sx={{ 
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
      p: { xs: 2, md: 4 }
    }}>
      {/* Header Section */}
      <Slide direction="down" in timeout={600}>
        <Paper 
          elevation={0}
          sx={{ 
            p: { xs: 3, md: 4 }, 
            mb: 4, 
            borderRadius: 4,
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)'
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 3 }}>
              <LogoIcon size={64}>
                <DashboardIcon sx={{ fontSize: 32 }} />
              </LogoIcon>
    <Box>
                <Typography 
                  variant="h3" 
                  fontWeight="800" 
                  sx={{ 
                    background: 'linear-gradient(135deg, #667eea, #764ba2)',
                    backgroundClip: 'text',
                    WebkitBackgroundClip: 'text',
                    WebkitTextFillColor: 'transparent',
                    mb: 1,
                    lineHeight: 1.2
                  }}
                >
        Admin Dashboard
      </Typography>
                <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 400 }}>
                  Sistem yönetimi ve analiz paneli
                </Typography>
              </Box>
            </Box>
            
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, position: 'relative' }}>
              <Tooltip title="Bildirimler">
                <IconButton 
                  onClick={() => setShowNotifications(!showNotifications)}
                  sx={{ 
                    p: 2, 
                    background: 'rgba(102, 126, 234, 0.1)',
                    color: 'primary.main',
                    '&:hover': {
                      background: 'rgba(102, 126, 234, 0.2)',
                      transform: 'scale(1.05)'
                    }
                  }}
                >
                  <Badge badgeContent={unreadCount} color="error">
                    <NotificationsIcon />
                  </Badge>
                </IconButton>
              </Tooltip>
              
              <Tooltip title="Verileri Yenile">
                <IconButton 
                  onClick={() => {
                    addNotification('info', 'Veri Yenileme', 'Tüm veriler başarıyla yenilendi');
                    window.location.reload();
                  }} 
                  sx={{ 
                    p: 2, 
                    background: 'linear-gradient(135deg, #667eea, #764ba2)',
                    color: 'white',
                    '&:hover': {
                      background: 'linear-gradient(135deg, #5a6fd8, #6a4190)',
                      transform: 'scale(1.05)'
                    }
                  }}
                >
                  <RefreshIcon />
                </IconButton>
              </Tooltip>
            </Box>
          </Box>
          
          {error && (
            <Fade in timeout={800}>
              <Alert 
                severity="error" 
                sx={{ 
                  mb: 2, 
                  borderRadius: 3,
                  '& .MuiAlert-message': { fontWeight: 500 }
                }}
              >
                {error}
              </Alert>
            </Fade>
          )}
        </Paper>
      </Slide>

      {/* Notifications Dropdown */}
      {showNotifications && (
        <Fade in timeout={300}>
          <Paper
            ref={notificationRef}
            elevation={8}
            sx={{
              position: 'absolute',
              top: 120,
              right: 32,
              width: 400,
              maxHeight: 500,
              borderRadius: 3,
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
              zIndex: 1000,
              overflow: 'hidden'
            }}
          >
            {/* Notifications Header */}
            <Box sx={{
              p: 3,
              background: 'linear-gradient(135deg, #667eea, #764ba2)',
              color: 'white',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}>
              <Typography variant="h6" fontWeight="700">
                Bildirimler
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                {unreadCount > 0 && (
                  <Button
                    size="small"
                    onClick={markAllAsRead}
                    sx={{
                      color: 'white',
                      fontSize: '12px',
                      textTransform: 'none',
                      '&:hover': {
                        background: 'rgba(255, 255, 255, 0.1)'
                      }
                    }}
                  >
                    Tümünü Okundu İşaretle
                  </Button>
                )}
                <IconButton
                  size="small"
                  onClick={() => setShowNotifications(false)}
                  sx={{ color: 'white' }}
                >
                  <CloseIcon />
                </IconButton>
              </Box>
            </Box>

            {/* Notifications List */}
            <Box sx={{ maxHeight: 400, overflowY: 'auto' }}>
              {notifications.length === 0 ? (
                <Box sx={{ p: 4, textAlign: 'center' }}>
                  <NotificationsIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                  <Typography variant="h6" color="textSecondary">
                    Bildirim bulunmuyor
                  </Typography>
                </Box>
              ) : (
                notifications.map((notification, index) => (
                  <Fade in timeout={500 + index * 100} key={notification.id}>
                    <Box
                      sx={{
                        p: 3,
                        borderBottom: '1px solid rgba(0, 0, 0, 0.05)',
                        cursor: 'pointer',
                        background: notification.isRead ? 'transparent' : 'rgba(102, 126, 234, 0.05)',
                        transition: 'all 0.3s ease',
                        '&:hover': {
                          background: 'rgba(102, 126, 234, 0.1)',
                          transform: 'translateX(4px)'
                        }
                      }}
                      onClick={() => markNotificationAsRead(notification.id)}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
                        <LogoIcon size={40} bg={getNotificationColor(notification.type)}>
                          {getNotificationIcon(notification.type)}
                        </LogoIcon>
                        
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                            <Typography variant="subtitle2" fontWeight="600" color="text.primary">
                              {notification.title}
                            </Typography>
                            {!notification.isRead && (
                              <Box
                                sx={{
                                  width: 8,
                                  height: 8,
                                  borderRadius: '50%',
                                  background: 'primary.main'
                                }}
                              />
                            )}
                          </Box>
                          
                          <Typography variant="body2" color="text.secondary" sx={{ mb: 1, lineHeight: 1.4 }}>
                            {notification.message}
                          </Typography>
                          
                          <Typography variant="caption" color="text.secondary" sx={{ opacity: 0.7 }}>
                            {notification.time}
                          </Typography>
                        </Box>
                      </Box>
                    </Box>
                  </Fade>
                ))
              )}
            </Box>

            {/* Notifications Footer */}
            {notifications.length > 0 && (
              <Box sx={{
                p: 2,
                background: 'rgba(0, 0, 0, 0.02)',
                borderTop: '1px solid rgba(0, 0, 0, 0.05)',
                textAlign: 'center'
              }}>
                <Button
                  fullWidth
                  variant="text"
                  onClick={() => {
                    setShowNotifications(false);
                    // Navigate to full notifications page if exists
                  }}
                  sx={{
                    color: 'primary.main',
                    fontWeight: '600',
                    textTransform: 'none'
                  }}
                >
                  Tüm Bildirimleri Görüntüle
                </Button>
              </Box>
            )}
          </Paper>
        </Fade>
      )}
      
      {/* Statistics Cards */}
      <Box sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(4, 1fr)' },
        gap: 3,
        mb: 5
      }}>
        <StatCard 
          title="Toplam Kullanıcı" 
          value={userStats?.totalUsers ?? '-'} 
          icon={<PeopleIcon sx={{ fontSize: 32 }} />}
          color="#667eea"
          subtitle={`Market: ${userStats?.marketUsers ?? 0} | Tedarikçi: ${userStats?.supplierUsers ?? 0}`}
          trend={12}
          delay={0}
        />
        <StatCard 
          title="Toplam Market" 
          value={marketStats?.totalMarkets ?? '-'} 
          icon={<StoreIcon sx={{ fontSize: 32 }} />}
          color="#f093fb"
          subtitle="Aktif market sayısı"
          trend={8}
          delay={100}
        />
        <StatCard 
          title="Toplam Tedarikçi" 
          value={supplierStats?.totalSuppliers ?? '-'} 
          icon={<BusinessIcon sx={{ fontSize: 32 }} />}
          color="#4facfe"
          subtitle="Kayıtlı tedarikçi"
          trend={15}
          delay={200}
        />
        <StatCard 
          title="Toplam Sipariş" 
          value={orderStats?.totalOrders ?? '-'} 
          icon={<ShoppingCartIcon sx={{ fontSize: 32 }} />}
          color="#43e97b"
          subtitle={`Bekleyen: ${orderStats?.pendingOrders ?? 0} | Toplam: ${orderStats?.totalOrders ?? 0}`}
          trend={-3}
          delay={300}
        />
        <StatCard 
          title="Toplam Sipariş Kalemi" 
          value={productStats?.totalProducts ?? '-'} 
          icon={<InventoryIcon sx={{ fontSize: 32 }} />}
          color="#fa709a"
          subtitle="Sipariş verilen toplam ürün kalemi"
          trend={0}
          delay={400}
        />
      </Box>

      {/* Quick Actions */}
      <Slide direction="up" in timeout={800}>
        <Paper 
          elevation={0}
          sx={{ 
            p: 4, 
            mb: 5, 
            borderRadius: 4,
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)'
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 4 }}>
            <AnalyticsIcon sx={{ fontSize: 32, color: 'primary.main', mr: 2 }} />
            <Typography variant="h4" fontWeight="700" color="text.primary">
              Hızlı İşlemler
            </Typography>
          </Box>
          
          <Box sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(4, 1fr)' },
            gap: 3
          }}>
            {[
              {
                title: 'Tüm Siparişler',
                icon: <ShoppingCartIcon sx={{ fontSize: 24 }} />,
                color: '#667eea',
                path: '/orders',
                description: 'Siparişleri görüntüle ve yönet'
              },
              {
                title: 'Tüm Teslimatlar',
                icon: <DeliveryIcon sx={{ fontSize: 24 }} />,
                color: '#f093fb',
                path: '/deliveries',
                description: 'Teslimat durumlarını takip et'
              },
              {
                title: 'Kullanıcı Yönetimi',
                icon: <PeopleIcon sx={{ fontSize: 24 }} />,
                color: '#4facfe',
                path: '/users',
                description: 'Kullanıcıları düzenle ve yönet'
              },
              {
                title: 'Tedarikçiler',
                icon: <BusinessIcon sx={{ fontSize: 24 }} />,
                color: '#43e97b',
                path: '/suppliers',
                description: 'Tedarikçi bilgilerini yönet'
              }
            ].map((action, index) => (
              <Zoom in timeout={1000 + index * 200} key={action.title}>
                <Card
                  elevation={0}
                  sx={{
                    p: 3,
                    borderRadius: 3,
                    background: 'rgba(255, 255, 255, 0.8)',
                    border: '1px solid rgba(255, 255, 255, 0.3)',
                    cursor: 'pointer',
                    transition: 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)',
                    position: 'relative',
                    overflow: 'hidden',
                    '&:hover': {
                      transform: 'translateY(-8px) scale(1.02)',
                      boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
                      border: `1px solid ${action.color}40`,
                      '& .action-icon': {
                        transform: 'scale(1.1) rotate(5deg)',
                      },
                      '& .action-arrow': {
                        transform: 'translateX(4px)',
                      }
                    }
                  }}
                  onClick={() => navigate(action.path)}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                    <LogoIcon size={48} bg={`linear-gradient(135deg, ${action.color}, ${action.color}CC)`}>
                      {action.icon}
                    </LogoIcon>
                    <ArrowForwardIcon 
                      className="action-arrow"
                      sx={{ 
                        color: 'text.secondary',
                        transition: 'all 0.3s ease'
                      }} 
                    />
                  </Box>
                  
                  <Typography variant="h6" fontWeight="600" color="text.primary" sx={{ mb: 1 }}>
                    {action.title}
                  </Typography>
                  
                  <Typography variant="body2" color="text.secondary" sx={{ opacity: 0.8 }}>
                    {action.description}
                  </Typography>
                </Card>
              </Zoom>
            ))}
          </Box>
        </Paper>
      </Slide>

      {/* Recent Orders */}
      <Slide direction="up" in timeout={1000}>
        <Box>
          <Paper 
            elevation={0}
            sx={{ 
              mb: 5, 
              borderRadius: 4,
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
              overflow: 'hidden'
            }}
          >
          <Box sx={{ 
            p: 4, 
            background: 'linear-gradient(135deg, #667eea, #764ba2)',
            color: 'white',
            position: 'relative',
            '&::after': {
              content: '""',
              position: 'absolute',
              top: 0,
              right: 0,
              width: '200px',
              height: '200px',
              background: 'radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%)',
              borderRadius: '50%',
              transform: 'translate(50%, -50%)'
            }
          }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', position: 'relative', zIndex: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <LogoIcon size={48} bg={'rgba(255, 255, 255, 0.2)'}>
                  <ShoppingCartIcon sx={{ fontSize: 24 }} />
                </LogoIcon>
                <Box>
                  <Typography variant="h4" fontWeight="700" sx={{ mb: 0.5 }}>
                    Son Siparişler
                  </Typography>
                  <Typography variant="body1" sx={{ opacity: 0.9 }}>
                    En son siparişlerin durumu
          </Typography>
                </Box>
              </Box>
              
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Chip 
                  label={`${ordersPagination.totalElements} toplam`}
                  sx={{ 
                    background: 'rgba(255,255,255,0.2)',
                    color: 'white',
                    fontWeight: 'bold',
                    backdropFilter: 'blur(10px)'
                  }}
                />
                <Tooltip title="Detaylı Görünüm">
                  <IconButton 
                    onClick={() => navigate('/orders')}
                    sx={{ 
                      color: 'white',
                      background: 'rgba(255,255,255,0.1)',
                      backdropFilter: 'blur(10px)',
                      '&:hover': { 
                        background: 'rgba(255,255,255,0.2)',
                        transform: 'scale(1.05)'
                      }
                    }}
                  >
                    <ViewIcon />
                  </IconButton>
                </Tooltip>
              </Box>
            </Box>
          </Box>
        
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow sx={{ 
                  background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05))',
                  '& .MuiTableCell-head': {
                    borderBottom: '2px solid rgba(102, 126, 234, 0.2)',
                    fontWeight: '700',
                    fontSize: '14px',
                    color: 'text.primary',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }
                }}>
                  <TableCell>ID</TableCell>
                  <TableCell>Market</TableCell>
                  <TableCell>Durum</TableCell>
                  <TableCell>Ürün Sayısı</TableCell>
                  <TableCell>Toplam Tutar</TableCell>
                  <TableCell>Tarih</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {orders.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ py: 8 }}>
                      <Fade in timeout={1200}>
                        <Box sx={{ textAlign: 'center' }}>
                          <LogoIcon size={80}>
                            <ShoppingCartIcon sx={{ fontSize: 40 }} />
                          </LogoIcon>
                          <Typography variant="h5" color="textSecondary" sx={{ mb: 2, fontWeight: 600 }}>
                        Henüz sipariş bulunmuyor
                      </Typography>
                          <Typography variant="body1" color="textSecondary" sx={{ opacity: 0.8 }}>
                            Yeni siparişler geldiğinde burada görünecek
                          </Typography>
                        </Box>
                      </Fade>
                    </TableCell>
                  </TableRow>
                ) : (
                  orders.map((order, index) => (
                    <Fade in timeout={1200 + index * 100} key={order.id}>
                      <TableRow 
                        sx={{ 
                          '&:hover': { 
                            background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05))',
                            transform: 'scale(1.005)',
                            boxShadow: '0 4px 20px rgba(102, 126, 234, 0.1)',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
                          },
                          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                          '& .MuiTableCell-root': {
                            borderBottom: '1px solid rgba(0, 0, 0, 0.05)',
                            py: 2
                          }
                        }}
                      >
                        <TableCell sx={{ fontWeight: '700', color: 'primary.main', fontSize: '16px' }}>
                          #{order.id}
                        </TableCell>
                        <TableCell sx={{ fontWeight: '600', fontSize: '15px' }}>
                          {order.marketName}
                        </TableCell>
                      <TableCell>
                        <Chip 
                          label={getStatusText(order.status)} 
                          color={getStatusColor(order.status) as any}
                          size="small"
                            sx={{ 
                              fontWeight: '600',
                              borderRadius: 3,
                              px: 1,
                              height: 28
                            }}
                            icon={
                              order.status === OrderStatus.PENDING ? <PendingIcon sx={{ fontSize: 16 }} /> :
                              order.status === OrderStatus.DELIVERED ? <CheckCircleIcon sx={{ fontSize: 16 }} /> :
                              <ScheduleIcon sx={{ fontSize: 16 }} />
                            }
                        />
                      </TableCell>
                        <TableCell sx={{ fontWeight: '500', fontSize: '15px' }}>
                          {order.itemCount || 0} ürün
                        </TableCell>
                        <TableCell sx={{ fontWeight: '700', color: 'success.main', fontSize: '16px' }}>
                          ₺{order.totalAmount?.toFixed(2) || '0.00'}
                        </TableCell>
                        <TableCell sx={{ color: 'text.secondary', fontSize: '14px' }}>
                          {new Date(order.createdAt).toLocaleDateString('tr-TR')}
                        </TableCell>
                    </TableRow>
                    </Fade>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        
          {ordersPagination.totalPages > 1 && (
            <Box sx={{ 
              p: 4, 
              display: 'flex', 
              justifyContent: 'center',
              background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.02), rgba(118, 75, 162, 0.02))',
              borderTop: '1px solid rgba(0, 0, 0, 0.05)'
            }}>
              <Pagination
                count={ordersPagination.totalPages}
                page={ordersPagination.page + 1}
                onChange={(_, newPage) => loadOrders(newPage - 1)}
                color="primary"
                size="large"
                sx={{
                  '& .MuiPaginationItem-root': {
                    borderRadius: 3,
                    fontWeight: '600',
                    fontSize: '14px',
                    minWidth: 40,
                    height: 40,
                    '&.Mui-selected': {
                      background: 'linear-gradient(135deg, #667eea, #764ba2)',
                      color: 'white',
                      boxShadow: '0 4px 12px rgba(102, 126, 234, 0.3)',
                      '&:hover': {
                        background: 'linear-gradient(135deg, #5a6fd8, #6a4190)',
                      }
                    },
                    '&:hover': {
                      background: 'rgba(102, 126, 234, 0.1)',
                      transform: 'scale(1.05)'
                    }
                  }
                }}
              />
            </Box>
          )}
          </Paper>
        </Box>
      </Slide>

      {/* Recent Deliveries */}
      <Slide direction="up" in timeout={1200}>
        <Box>
          <Paper 
            elevation={0}
            sx={{ 
              borderRadius: 4,
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
              overflow: 'hidden'
            }}
          >
          <Box sx={{ 
            p: 4, 
            background: 'linear-gradient(135deg, #f093fb, #f5576c)',
            color: 'white',
            position: 'relative',
            '&::after': {
              content: '""',
              position: 'absolute',
              top: 0,
              right: 0,
              width: '200px',
              height: '200px',
              background: 'radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%)',
              borderRadius: '50%',
              transform: 'translate(50%, -50%)'
            }
          }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', position: 'relative', zIndex: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <LogoIcon size={48} bg={'rgba(255, 255, 255, 0.2)'}>
                  <DeliveryIcon sx={{ fontSize: 24 }} />
                </LogoIcon>
                <Box>
                  <Typography variant="h4" fontWeight="700" sx={{ mb: 0.5 }}>
                    Son Teslimatlar
                  </Typography>
                  <Typography variant="body1" sx={{ opacity: 0.9 }}>
                    En son teslimat durumları
          </Typography>
                </Box>
              </Box>
              
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Chip 
                  label={`${deliveriesPagination.totalElements} toplam`}
                  sx={{ 
                    background: 'rgba(255,255,255,0.2)',
                    color: 'white',
                    fontWeight: 'bold',
                    backdropFilter: 'blur(10px)'
                  }}
                />
                <Tooltip title="Detaylı Görünüm">
                  <IconButton 
                    onClick={() => navigate('/deliveries')}
                    sx={{ 
                      color: 'white',
                      background: 'rgba(255,255,255,0.1)',
                      backdropFilter: 'blur(10px)',
                      '&:hover': { 
                        background: 'rgba(255,255,255,0.2)',
                        transform: 'scale(1.05)'
                      }
                    }}
                  >
                    <ViewIcon />
                  </IconButton>
                </Tooltip>
              </Box>
            </Box>
          </Box>
        
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow sx={{ 
                  background: 'linear-gradient(135deg, rgba(240, 147, 251, 0.05), rgba(245, 87, 108, 0.05))',
                  '& .MuiTableCell-head': {
                    borderBottom: '2px solid rgba(240, 147, 251, 0.2)',
                    fontWeight: '700',
                    fontSize: '14px',
                    color: 'text.primary',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                  }
                }}>
                  <TableCell>ID</TableCell>
                  <TableCell>Market</TableCell>
                  <TableCell>Tedarikçi</TableCell>
                  <TableCell>Durum</TableCell>
                  <TableCell>Oluşturulma</TableCell>
                  <TableCell>Teslim</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {deliveries.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ py: 8 }}>
                      <Fade in timeout={1400}>
                        <Box sx={{ textAlign: 'center' }}>
                          <LogoIcon size={80} bg={'linear-gradient(135deg, #f093fb, #f5576c)'}>
                            <DeliveryIcon sx={{ fontSize: 40 }} />
                          </LogoIcon>
                          <Typography variant="h5" color="textSecondary" sx={{ mb: 2, fontWeight: 600 }}>
                        Henüz teslimat bulunmuyor
                      </Typography>
                          <Typography variant="body1" color="textSecondary" sx={{ opacity: 0.8 }}>
                            Yeni teslimatlar geldiğinde burada görünecek
                          </Typography>
                        </Box>
                      </Fade>
                    </TableCell>
                  </TableRow>
                ) : (
                  deliveries.map((delivery, index) => (
                    <Fade in timeout={1400 + index * 100} key={delivery.id}>
                      <TableRow 
                        sx={{ 
                          '&:hover': { 
                            background: 'linear-gradient(135deg, rgba(240, 147, 251, 0.05), rgba(245, 87, 108, 0.05))',
                            transform: 'scale(1.005)',
                            boxShadow: '0 4px 20px rgba(240, 147, 251, 0.1)',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)'
                          },
                          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                          '& .MuiTableCell-root': {
                            borderBottom: '1px solid rgba(0, 0, 0, 0.05)',
                            py: 2
                          }
                        }}
                      >
                        <TableCell sx={{ fontWeight: '700', color: 'primary.main', fontSize: '16px' }}>
                          #{delivery.id}
                        </TableCell>
                        <TableCell sx={{ fontWeight: '600', fontSize: '15px' }}>
                          {delivery.marketName}
                        </TableCell>
                        <TableCell sx={{ fontWeight: '600', fontSize: '15px' }}>
                          {delivery.supplierCompanyName}
                        </TableCell>
                      <TableCell>
                        <Chip 
                          label={getStatusText(delivery.deliveryStatus)} 
                          color={getStatusColor(delivery.deliveryStatus) as any}
                          size="small"
                            sx={{ 
                              fontWeight: '600',
                              borderRadius: 3,
                              px: 1,
                              height: 28
                            }}
                            icon={
                              delivery.deliveryStatus === DeliveryStatus.IN_PROGRESS ? <PendingIcon sx={{ fontSize: 16 }} /> :
                              delivery.deliveryStatus === DeliveryStatus.DELIVERED ? <CheckCircleIcon sx={{ fontSize: 16 }} /> :
                              <ScheduleIcon sx={{ fontSize: 16 }} />
                            }
                        />
                      </TableCell>
                        <TableCell sx={{ color: 'text.secondary', fontSize: '14px' }}>
                          {new Date(delivery.createdAt).toLocaleDateString('tr-TR')}
                        </TableCell>
                        <TableCell sx={{ color: 'text.secondary', fontSize: '14px' }}>
                        {delivery.deliveryTime ? new Date(delivery.deliveryTime).toLocaleDateString('tr-TR') : '-'}
                      </TableCell>
                    </TableRow>
                    </Fade>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        
          {deliveriesPagination.totalPages > 1 && (
            <Box sx={{ 
              p: 4, 
              display: 'flex', 
              justifyContent: 'center',
              background: 'linear-gradient(135deg, rgba(240, 147, 251, 0.02), rgba(245, 87, 108, 0.02))',
              borderTop: '1px solid rgba(0, 0, 0, 0.05)'
            }}>
              <Pagination
                count={deliveriesPagination.totalPages}
                page={deliveriesPagination.page + 1}
                onChange={(_, newPage) => loadDeliveries(newPage - 1)}
                color="primary"
                size="large"
                sx={{
                  '& .MuiPaginationItem-root': {
                    borderRadius: 3,
                    fontWeight: '600',
                    fontSize: '14px',
                    minWidth: 40,
                    height: 40,
                    '&.Mui-selected': {
                      background: 'linear-gradient(135deg, #f093fb, #f5576c)',
                      color: 'white',
                      boxShadow: '0 4px 12px rgba(240, 147, 251, 0.3)',
                      '&:hover': {
                        background: 'linear-gradient(135deg, #e085f0, #f44a5c)',
                      }
                    },
                    '&:hover': {
                      background: 'rgba(240, 147, 251, 0.1)',
                      transform: 'scale(1.05)'
                    }
                  }
                }}
              />
            </Box>
          )}
          </Paper>
        </Box>
      </Slide>
    </Box>
  );
};

export default AdminDashboardPage;
