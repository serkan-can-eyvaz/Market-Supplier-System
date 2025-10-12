import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';
import { Notification } from '../types';
import {
  AppBar,
  Box,
  CssBaseline,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Menu,
  MenuItem,
  Divider,
  useTheme,
  useMediaQuery,
  Badge,
  Tooltip,
  Slide,
  Chip,
  Paper,
  Fade,
  Avatar,
  Button,
} from '@mui/material';
import {
  LayoutDashboard,
  Package,
  Store,
  Truck,
  ShoppingCart,
  Shield,
  LogOut,
  Menu as MenuIcon,
  Bell,
  RefreshCw,
  Users,
  Settings,
  ChevronDown,
  MoreVertical,
  Clock,
  CheckCircle,
  AlertCircle
} from 'lucide-react';
import { LogoIcon, UserAvatar } from './ui/Logo';

const drawerWidth = 280;


interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [moreEl, setMoreEl] = useState<null | HTMLElement>(null);
  const [notificationAnchor, setNotificationAnchor] = useState<null | HTMLElement>(null);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const notificationIdCounter = useRef(1);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout, isAdmin, isMarket, isSupplier } = useAuth();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleProfileMenuClose = () => {
    setAnchorEl(null);
  };

  const openMore = (e: React.MouseEvent<HTMLElement>) => setMoreEl(e.currentTarget);
  const closeMore = () => setMoreEl(null);

  // Bildirim fonksiyonları
  const addNotification = (type: Notification['type'], title: string, message: string, actionUrl?: string, priority: 'high' | 'normal' = 'normal') => {
    const newNotification: Notification = {
      id: notificationIdCounter.current++,
      user: user!,
      type,
      title,
      message,
      createdAt: new Date().toISOString(),
      isRead: false,
      actionUrl,
      priority,
    };
    
    setNotifications(prev => {
      // Yüksek öncelikli bildirimleri en üste koy
        const sorted = [newNotification, ...prev].sort((a, b) => {
          if (a.priority === 'high' && b.priority !== 'high') return -1;
          if (b.priority === 'high' && a.priority !== 'high') return 1;
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        });
      return sorted.slice(0, 50); // Maksimum 50 bildirim
    });
  };

  const markAsRead = (id: number) => {
    setNotifications(prev => 
      prev.map(notif => 
        notif.id === id ? { ...notif, isRead: true } : notif
      )
    );
  };

  const markAllAsRead = async () => {
    try {
      // Backend'e tümünü okundu olarak işaretle
      await apiService.markAllNotificationsAsRead();
      // Local state'i güncelle
      setNotifications(prev => 
        prev.map(notif => ({ ...notif, isRead: true }))
      );
    } catch (error) {
      console.error('Tüm bildirimler okundu olarak işaretlenirken hata:', error);
    }
  };

  const handleNotificationClick = async (notification: Notification) => {
    try {
      // Backend'e okundu olarak işaretle
      await apiService.markNotificationAsRead(notification.id);
      // Local state'i güncelle
      markAsRead(notification.id);
    } catch (error) {
      console.error('Bildirim okundu olarak işaretlenirken hata:', error);
    }
    
    setNotificationAnchor(null);
    
    if (notification.actionUrl) {
      navigate(notification.actionUrl);
    }
  };

  const openNotifications = (event: React.MouseEvent<HTMLElement>) => {
    setNotificationAnchor(event.currentTarget);
  };

  const closeNotifications = () => {
    setNotificationAnchor(null);
  };

  const handleSettings = () => {
    navigate('/settings');
    handleProfileMenuClose();
    closeMore();
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
    handleProfileMenuClose();
    closeMore();
  };

  const handleNavigation = (path: string) => {
    navigate(path);
    if (isMobile) {
      setMobileOpen(false);
    }
  };

  const getMenuItems = () => {
    const baseItems = [
      { text: 'Dashboard', icon: <LayoutDashboard size={24} />, path: '/dashboard' },
    ];

    if (isMarket) {
      baseItems.push(
        { text: 'Market Dashboard', icon: <Store size={24} />, path: '/market-dashboard' },
        { text: 'Ürün Kataloğu', icon: <Package size={24} />, path: '/market-products' },
        { text: 'Siparişlerim', icon: <ShoppingCart size={24} />, path: '/orders' }
      );
    }

    if (isSupplier) {
      baseItems.push(
        { text: 'Tedarikçi Dashboard', icon: <Shield size={24} />, path: '/supplier-dashboard' },
        { text: 'Ürünlerim', icon: <Package size={24} />, path: '/products' },
        { text: 'Marketlerim', icon: <Store size={24} />, path: '/my-markets' },
        { text: 'Teslimatlarım', icon: <Truck size={24} />, path: '/deliveries' },
        { text: 'Bekleyen Siparişler', icon: <ShoppingCart size={24} />, path: '/orders' }
      );
    }

    if (isAdmin) {
      baseItems.push(
        { text: 'Admin Dashboard', icon: <LayoutDashboard size={24} />, path: '/admin-dashboard' },
        { text: 'Kullanıcılar', icon: <Users size={24} />, path: '/users' },
        { text: 'Marketler', icon: <Store size={24} />, path: '/markets' },
        { text: 'Tedarikçiler', icon: <Shield size={24} />, path: '/suppliers' },
        { text: 'Tüm Ürünler', icon: <Package size={24} />, path: '/products' },
        { text: 'Tüm Siparişler', icon: <ShoppingCart size={24} />, path: '/orders' },
        { text: 'Tüm Teslimatlar', icon: <Truck size={24} />, path: '/deliveries' }
      );
    }

    return baseItems;
  };


  const getRoleText = () => {
    if (isAdmin) return 'Sistem Yöneticisi';
    if (isMarket) return 'Market Yöneticisi';
    if (isSupplier) return 'Tedarikçi';
    return 'Kullanıcı';
  };

  const unreadNotifications = notifications.filter(n => !n.isRead).length;

  // Gerçek bildirimleri yükle
  useEffect(() => {
    const loadNotifications = async () => {
      if (user) {
        try {
          const recentNotifications = await apiService.getRecentNotifications(20);
          // Backend'den gelen bildirimleri state'e ekle
          setNotifications(prev => {
            // Mevcut bildirimlerle backend'den gelenleri birleştir
            const existingIds = new Set(prev.map(n => n.id.toString()));
            const newNotifications = recentNotifications
              .filter(n => !existingIds.has(n.id.toString()))
              .map(backendNotification => ({
                id: backendNotification.id,
                user: backendNotification.user,
                type: backendNotification.type,
                title: backendNotification.title,
                message: backendNotification.message,
                isRead: backendNotification.isRead,
                priority: backendNotification.priority,
                actionUrl: backendNotification.actionUrl,
                relatedEntityType: backendNotification.relatedEntityType,
                relatedEntityId: backendNotification.relatedEntityId,
                createdAt: backendNotification.createdAt
              }));
            
            return [...newNotifications, ...prev].slice(0, 50); // Son 50 bildirimi tut
          });
        } catch (error) {
          console.error('Bildirimler yüklenirken hata:', error);
        }
      }
    };

    loadNotifications();
    
    // Her 30 saniyede bir bildirimleri kontrol et
    const interval = setInterval(loadNotifications, 30000);
    
    return () => clearInterval(interval);
  }, [user]);

  const getRoleColor = () => {
    if (isAdmin) return '#667eea';
    if (isMarket) return '#f093fb';
    if (isSupplier) return '#4facfe';
    return '#43e97b';
  };

  const drawer = (
    <Box sx={{ 
      height: '100%',
      background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
      display: 'flex',
      flexDirection: 'column'
    }}>
      {/* Header Section */}
      <Box sx={{
        p: 3,
        background: 'linear-gradient(135deg, #667eea, #764ba2)',
        color: 'white',
        position: 'relative',
        '&::after': {
          content: '""',
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          height: '4px',
          background: 'linear-gradient(90deg, rgba(255,255,255,0.3), rgba(255,255,255,0.1))'
        }
      }}>
        <Box sx={{ 
          display: 'grid', 
          gridTemplateColumns: 'auto 1fr', 
          gap: 2, 
          mb: 2,
          alignItems: 'center',
          justifyItems: 'start'
        }}>
          <LogoIcon size={56} />
          <Box sx={{ 
            display: 'flex', 
            flexDirection: 'column', 
            justifyContent: 'center',
            minWidth: 0,
            width: '100%'
          }}>
            <Typography variant="h5" fontWeight="800" sx={{ 
              mb: 0.5, 
              lineHeight: 1.2, 
              fontSize: { xs: '1.2rem', sm: '1.3rem', md: '1.4rem' },
              textAlign: 'left'
            }}>
              Market Supplier
            </Typography>
            <Typography variant="body2" sx={{ 
              opacity: 0.9, 
              lineHeight: 1.2, 
              fontSize: { xs: '0.8rem', sm: '0.82rem', md: '0.85rem' },
              textAlign: 'left'
            }}>
              Yönetim Paneli
            </Typography>
          </Box>
        </Box>
        
        {/* User Info */}
        <Box sx={{
          p: 2,
          background: 'rgba(255, 255, 255, 0.1)',
          borderRadius: 2,
          backdropFilter: 'blur(10px)',
          border: '1px solid rgba(255, 255, 255, 0.2)'
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
               <UserAvatar size={44} role={user?.role?.toLowerCase()} />
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="subtitle2" fontWeight="600" sx={{ mb: 0.5 }}>
                {user?.name}
              </Typography>
              <Chip
                label={getRoleText()}
                size="small"
                sx={{
                  background: getRoleColor(),
                  color: 'white',
                  fontSize: '10px',
                  height: 20,
                  fontWeight: 'bold'
                }}
              />
            </Box>
          </Box>
        </Box>
      </Box>

      {/* Navigation Menu */}
      <Box sx={{ flex: 1, p: 2 }}>
        <List sx={{ '& .MuiListItem-root': { mb: 0.5 } }}>
          {getMenuItems().map((item, index) => (
            <Slide direction="right" in timeout={300 + index * 100} key={item.text}>
              <ListItem disablePadding>
                <ListItemButton
                  selected={location.pathname === item.path}
                  onClick={() => handleNavigation(item.path)}
                  sx={{
                    borderRadius: 2,
                    mb: 0.5,
                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                    '&.Mui-selected': {
                      background: 'linear-gradient(135deg, #667eea, #764ba2)',
                      color: 'white',
                      boxShadow: '0 8px 25px rgba(102, 126, 234, 0.3)',
                      '&:hover': {
                        background: 'linear-gradient(135deg, #5a6fd8, #6a4190)',
                      },
                      '& .MuiListItemIcon-root': {
                        color: 'white'
                      }
                    },
                    '&:hover': {
                      background: 'rgba(102, 126, 234, 0.1)',
                      transform: 'translateX(4px)',
                      boxShadow: '0 4px 15px rgba(102, 126, 234, 0.2)'
                    }
                  }}
                >
                  <ListItemIcon sx={{ 
                    minWidth: 40,
                    color: location.pathname === item.path ? 'white' : 'text.secondary'
                  }}>
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText 
                    primary={item.text}
                    sx={{
                      '& .MuiTypography-root': {
                        fontWeight: location.pathname === item.path ? 600 : 500,
                        fontSize: '14px'
                      }
                    }}
                  />
                </ListItemButton>
              </ListItem>
            </Slide>
          ))}
        </List>
      </Box>

      {/* Footer */}
      <Box sx={{
        p: 2,
        background: 'rgba(255, 255, 255, 0.5)',
        backdropFilter: 'blur(10px)',
        borderTop: '1px solid rgba(255, 255, 255, 0.2)'
      }}>
        <Typography variant="caption" color="text.secondary" sx={{ textAlign: 'center', display: 'block' }}>
          © 2024 Market Supplier System
        </Typography>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <CssBaseline />
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          ml: { sm: `${drawerWidth}px` },
          background: 'linear-gradient(135deg, #667eea, #764ba2)',
          backdropFilter: 'blur(20px)',
          borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)'
        }}
      >
        <Toolbar sx={{ px: { xs: 2, md: 4 } }}>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ 
              mr: 2, 
              display: { sm: 'none' },
              background: 'rgba(255, 255, 255, 0.1)',
              backdropFilter: 'blur(10px)',
              '&:hover': {
                background: 'rgba(255, 255, 255, 0.2)',
                transform: 'scale(1.05)'
              }
            }}
          >
            <MenuIcon size={24} />
          </IconButton>
          
          <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center', gap: 2 }}>
            <Typography 
              variant="h5" 
              fontWeight="800" 
              sx={{ 
                background: 'linear-gradient(135deg, #ffffff, #f0f0f0)',
                backgroundClip: 'text',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                textShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
                lineHeight: 1.2
              }}
            >
              Market Supplier System
            </Typography>
          </Box>
          
          {/* Right actions */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {/* md+ full actions */}
            <Box sx={{ display: { xs: 'none', md: 'flex' }, alignItems: 'center', gap: 1 }}>
              <Tooltip title="Bildirimler">
                <IconButton 
                  onClick={openNotifications}
                  sx={{ color: 'white', background: 'rgba(255, 255, 255, 0.1)', backdropFilter: 'blur(10px)', '&:hover': { background: 'rgba(255, 255, 255, 0.2)', transform: 'scale(1.05)' } }}
                >
                  <Badge badgeContent={unreadNotifications} color="error">
                    <Bell size={24} />
                  </Badge>
                </IconButton>
              </Tooltip>
              <Tooltip title="Verileri Yenile">
                <IconButton onClick={() => console.log('Refresh clicked')} sx={{ color: 'white', background: 'rgba(255, 255, 255, 0.1)', backdropFilter: 'blur(10px)', '&:hover': { background: 'rgba(255, 255, 255, 0.2)', transform: 'scale(1.05)' } }}>
                  <RefreshCw size={24} />
                </IconButton>
              </Tooltip>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, ml: 2 }}>
                <Box sx={{ textAlign: 'right', display: 'flex', flexDirection: 'column', justifyContent: 'center', minWidth: 0 }}>
                  <Typography variant="subtitle2" fontWeight="600" color="white" sx={{ lineHeight: 1.1, whiteSpace: 'nowrap', fontSize: '0.9rem' }}>{user?.name}</Typography>
                  <Typography variant="caption" sx={{ color: 'rgba(255, 255, 255, 0.8)', lineHeight: 1.1, whiteSpace: 'nowrap', fontSize: '0.75rem' }}>{getRoleText()}</Typography>
                </Box>
                <Box onClick={handleProfileMenuOpen} sx={{ display: 'flex', alignItems: 'center', gap: 2, padding: '8px 16px', borderRadius: '25px', background: 'rgba(255, 255, 255, 0.1)', backdropFilter: 'blur(10px)', border: '1px solid rgba(255, 255, 255, 0.2)', cursor: 'pointer', transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)', '&:hover': { background: 'rgba(255, 255, 255, 0.15)', transform: 'translateY(-2px)', boxShadow: '0 8px 25px rgba(0, 0, 0, 0.15)', border: '1px solid rgba(255, 255, 255, 0.3)' } }}>
                  <UserAvatar size={40} role={user?.role?.toLowerCase()} />
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                    <Typography variant="subtitle2" sx={{ color: 'white', fontWeight: 600, lineHeight: 1.2 }}>{user?.name}</Typography>
                    <Typography variant="caption" sx={{ color: 'rgba(255, 255, 255, 0.8)', fontSize: '0.75rem' }}>{getRoleText()}</Typography>
                  </Box>
                  <ChevronDown size={16} color="rgba(255, 255, 255, 0.8)" />
                </Box>
              </Box>
            </Box>

            {/* xs compact menu */}
            <Box sx={{ display: { xs: 'flex', md: 'none' }, alignItems: 'center', gap: 1 }}>
              <IconButton onClick={openMore} sx={{ color: 'white', background: 'rgba(255, 255, 255, 0.1)', backdropFilter: 'blur(10px)', '&:hover': { background: 'rgba(255, 255, 255, 0.2)' } }} aria-label="more">
                <MoreVertical size={22} />
              </IconButton>
              <Menu
                anchorEl={moreEl}
                open={Boolean(moreEl)}
                onClose={closeMore}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                PaperProps={{ sx: { background: 'rgba(255,255,255,0.95)', backdropFilter: 'blur(20px)', borderRadius: 2, mt: 1 } }}
              >
                <MenuItem onClick={() => { closeMore(); handleProfileMenuOpen({ currentTarget: document.body } as any); }}>
                  <ListItemIcon><UserAvatar size={28} role={user?.role?.toLowerCase()} /></ListItemIcon>
                  <ListItemText primary={user?.name || 'Profil'} secondary={getRoleText()} />
                </MenuItem>
                <MenuItem onClick={() => { console.log('Refresh clicked'); closeMore(); }}>
                  <ListItemIcon><RefreshCw size={18} /></ListItemIcon>
                  <ListItemText primary="Yenile" />
                </MenuItem>
                <MenuItem onClick={handleSettings}>
                  <ListItemIcon><Settings size={18} /></ListItemIcon>
                  <ListItemText primary="Ayarlar" />
                </MenuItem>
                <Divider />
                <MenuItem onClick={handleLogout}>
                  <ListItemIcon><LogOut size={18} color="#d32f2f" /></ListItemIcon>
                  <ListItemText primary="Çıkış Yap" />
                </MenuItem>
              </Menu>
            </Box>
          </Box>

          {/* Profile menu for md+ */}
          <Menu
            anchorEl={anchorEl}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            keepMounted
            transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            open={Boolean(anchorEl)}
            onClose={handleProfileMenuClose}
            PaperProps={{ sx: { background: 'rgba(255, 255, 255, 0.95)', backdropFilter: 'blur(20px)', border: '1px solid rgba(255, 255, 255, 0.2)', boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)', borderRadius: 3, mt: 1, minWidth: 200 } }}
          >
            <MenuItem onClick={handleSettings}><ListItemIcon><Settings size={20} color="#1976d2" /></ListItemIcon><ListItemText primary="Ayarlar" /></MenuItem>
            <Divider />
            <MenuItem onClick={handleLogout}><ListItemIcon><LogOut size={20} color="#d32f2f" /></ListItemIcon><ListItemText primary="Çıkış Yap" /></MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>
      <Box
        component="nav"
        sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}
        aria-label="mailbox folders"
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true,
          }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { 
              boxSizing: 'border-box', 
              width: drawerWidth,
              background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
              border: 'none',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)'
            },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { 
              boxSizing: 'border-box', 
              width: drawerWidth,
              background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
              border: 'none',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)'
            },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: { xs: 2, md: 4 },
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
          minHeight: '100vh'
        }}
      >
        <Toolbar />
        {children}
      </Box>

      {/* Bildirim Dropdown */}
      <Menu
        anchorEl={notificationAnchor}
        open={Boolean(notificationAnchor)}
        onClose={closeNotifications}
        PaperProps={{
          sx: {
            mt: 1,
            minWidth: 350,
            maxWidth: 450,
            maxHeight: 500,
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 3,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.15)',
          },
        }}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      >
        <Box sx={{ p: 2, borderBottom: '1px solid rgba(0, 0, 0, 0.1)' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="h6" fontWeight="700" sx={{ color: 'text.primary' }}>
              Bildirimler
            </Typography>
            {unreadNotifications > 0 && (
              <Chip 
                label={unreadNotifications} 
                size="small" 
                color="error" 
                sx={{ fontWeight: 600 }}
              />
            )}
          </Box>
        </Box>
        
        <Box sx={{ maxHeight: 400, overflowY: 'auto' }}>
          {notifications.length === 0 ? (
            <Box sx={{ p: 3, textAlign: 'center' }}>
              <Bell size={48} style={{ color: '#ccc', marginBottom: 16 }} />
              <Typography variant="body2" color="text.secondary">
                Henüz bildirim yok
              </Typography>
            </Box>
          ) : (
            notifications.map((notification) => (
              <MenuItem
                key={notification.id}
                onClick={() => handleNotificationClick(notification)}
                sx={{
                  p: 2,
                  borderBottom: '1px solid rgba(0, 0, 0, 0.05)',
                  background: notification.isRead ? 'transparent' : 'rgba(25, 118, 210, 0.04)',
                  '&:hover': {
                    background: notification.isRead ? 'rgba(0, 0, 0, 0.04)' : 'rgba(25, 118, 210, 0.08)',
                  },
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2, width: '100%' }}>
                  <Avatar
                    sx={{
                      width: 40,
                      height: 40,
                      background: notification.type === 'order' 
                        ? 'linear-gradient(135deg, #ff6b6b, #ee5a24)'
                        : notification.type === 'delivery'
                        ? 'linear-gradient(135deg, #4facfe, #00f2fe)'
                        : 'linear-gradient(135deg, #667eea, #764ba2)',
                      fontSize: '1rem',
                    }}
                  >
                    {notification.type === 'order' && <ShoppingCart size={20} />}
                    {notification.type === 'delivery' && <Truck size={20} />}
                    {notification.type === 'system' && <AlertCircle size={20} />}
                  </Avatar>
                  
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                      <Typography 
                        variant="subtitle2" 
                        fontWeight={notification.isRead ? 500 : 700}
                        sx={{ 
                          color: 'text.primary',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap'
                        }}
                      >
                        {notification.title}
                      </Typography>
                      {notification.priority === 'high' && (
                        <Chip 
                          label="Öncelikli" 
                          size="small" 
                          color="error" 
                          sx={{ fontSize: '0.7rem', height: 20 }}
                        />
                      )}
                      {!notification.isRead && (
                        <Box sx={{ 
                          width: 8, 
                          height: 8, 
                          borderRadius: '50%', 
                          background: '#1976d2',
                          ml: 'auto'
                        }} />
                      )}
                    </Box>
                    
                    <Typography 
                      variant="body2" 
                      color="text.secondary"
                      sx={{ 
                        mb: 1,
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden',
                      }}
                    >
                      {notification.message}
                    </Typography>
                    
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Clock size={14} style={{ color: '#999' }} />
                      <Typography variant="caption" color="text.secondary">
                        {new Date(notification.createdAt).toLocaleTimeString('tr-TR', { 
                          hour: '2-digit', 
                          minute: '2-digit' 
                        })}
                      </Typography>
                    </Box>
                  </Box>
                </Box>
              </MenuItem>
            ))
          )}
        </Box>
        
        {notifications.length > 0 && unreadNotifications > 0 && (
          <Box sx={{ p: 2, borderTop: '1px solid rgba(0, 0, 0, 0.1)' }}>
            <Button
              fullWidth
              variant="text"
              onClick={markAllAsRead}
              sx={{ 
                color: 'primary.main',
                fontWeight: 600,
                textTransform: 'none'
              }}
            >
              Tümünü Okundu İşaretle
            </Button>
          </Box>
        )}
      </Menu>
    </Box>
  );
};

export default Layout;
