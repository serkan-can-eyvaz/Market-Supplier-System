import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Button,
  Stack,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  ListItemSecondaryAction,
  Paper,
  LinearProgress,
} from '@mui/material';
import {
  Storefront as StorefrontIcon,
  LocalShipping as DeliveryIcon,
  ShoppingCart as OrderIcon,
  Add as AddIcon,
  CheckCircle as CheckCircleIcon,
  Pending as PendingIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';
import { Market, Delivery, Order, DeliveryStatus } from '../types';
import { LogoIcon } from '../components/ui/Logo';

// Grid v7 tip farklarını aşmak için Box ile iki sütunlu düzen kullanıyoruz

const SupplierDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [myMarkets, setMyMarkets] = useState<Market[]>([]);
  const [recentDeliveries, setRecentDeliveries] = useState<Delivery[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ totalMarkets: 0, totalDeliveries: 0, pendingOrders: 0, completedDeliveries: 0 });

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        
        // Markets - sadece bu isteği yapalım
        try { 
          console.log('[SupplierDashboard] Loading markets...');
          const marketsResponse = await apiService.getMyMarkets();
          const markets = marketsResponse.content || [];
          setMyMarkets(markets);
          setStats(prev => ({ ...prev, totalMarkets: markets.length }));
          console.log('[SupplierDashboard] Markets loaded successfully:', markets.length);
        } catch (err) {
          console.error('Markets yüklenemedi:', err);
        }
        
        // Diğer istekleri şimdilik devre dışı bırakalım
        // TODO: Bu isteklerin neden 403 hatası verdiğini araştır
        
        // Deliveries
        // try { 
        //   console.log('[SupplierDashboard] Loading deliveries...');
        //   const deliveriesResponse = await apiService.getMyDeliveries();
        //   const deliveries = deliveriesResponse.content || [];
        //   setRecentDeliveries(deliveries.slice(0, 5));
        //   setStats(prev => ({ 
        //     ...prev, 
        //     totalDeliveries: deliveries.length,
        //     completedDeliveries: deliveries.filter(d => d.deliveryStatus === DeliveryStatus.DELIVERED).length
        //   }));
        //   console.log('[SupplierDashboard] Deliveries loaded successfully:', deliveries.length);
        // } catch (err) {
        //   console.error('Deliveries yüklenemedi:', err);
        // }

        // Pending Orders (paged)
        // try { 
        //   console.log('[SupplierDashboard] Loading pending orders...');
        //   const response = await apiService.getPendingOrdersPaged(0, 5);
        //   const pending = response.content || [];
        //   setStats(prev => ({ ...prev, pendingOrders: response.totalElements || pending.length }));
        //   console.log('[SupplierDashboard] Pending orders loaded successfully:', pending.length);
        // } catch (err) {
        //   console.error('Orders yüklenemedi:', err);
        // }
      } catch (err) {
        console.error('Dashboard yükleme hatası:', err);
      } finally {
        setLoading(false);
      }
    };
    
    loadData();
  }, []);


  if (loading) {
    return (
      <Box sx={{ 
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}>
        <Paper sx={{
          p: 4,
          textAlign: 'center',
          background: 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(20px)',
          borderRadius: 4
        }}>
          <LinearProgress sx={{ mb: 2 }} />
          <Typography variant="h6">Dashboard yükleniyor...</Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      {/* Header + quick actions */}
      <Paper sx={{ 
        p: { xs: 3, md: 4 }, 
        mb: 3,
        background: 'linear-gradient(135deg, rgba(255,255,255,0.95) 0%, rgba(248,250,252,0.95) 100%)',
        backdropFilter: 'blur(20px)',
        border: '1px solid rgba(255,255,255,0.2)',
        boxShadow: '0 8px 32px rgba(0,0,0,0.08)'
      }}>
        <Box sx={{ display: 'flex', alignItems: { xs: 'stretch', md: 'center' }, justifyContent: 'space-between', gap: 3, flexWrap: 'wrap' }}>
          <Box sx={{ minWidth: 0 }}>
            <Typography 
              variant="h4" 
              fontWeight={900} 
              sx={{ 
                mb: 1, 
                whiteSpace: 'nowrap', 
                overflow: 'hidden', 
                textOverflow: 'ellipsis',
                background: 'linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%)',
                backgroundClip: 'text',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                fontSize: { xs: '1.75rem', md: '2.25rem' },
                letterSpacing: '-0.02em'
              }}
            >
              Tedarikçi Dashboard
            </Typography>
            <Typography 
              variant="h6" 
              sx={{ 
                color: 'text.secondary',
                fontWeight: 500,
                fontSize: { xs: '1rem', md: '1.125rem' },
                opacity: 0.8
              }}
            >
              Hoş geldiniz, <strong style={{ color: '#1e40af' }}>{user?.name}</strong>!
            </Typography>
          </Box>
          <Stack direction="row" spacing={2} alignItems="center" sx={{ flexWrap: 'wrap', gap: 1.5 }}>
            <Button 
              size="medium" 
              variant="contained" 
              startIcon={<AddIcon sx={{ fontSize: '1.2rem' }} />} 
              onClick={() => navigate('/markets/add')}
              sx={{
                background: 'linear-gradient(135deg, #059669 0%, #10b981 100%)',
                boxShadow: '0 4px 14px rgba(16, 185, 129, 0.3)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #047857 0%, #059669 100%)',
                  boxShadow: '0 6px 20px rgba(16, 185, 129, 0.4)',
                  transform: 'translateY(-1px)'
                },
                transition: 'all 0.3s ease',
                borderRadius: 2,
                px: 3,
                py: 1.5,
                fontWeight: 600,
                fontSize: '0.875rem'
              }}
            >
              Market Ekle
            </Button>
            <Button 
              size="medium" 
              variant="contained" 
              color="info" 
              startIcon={<DeliveryIcon sx={{ fontSize: '1.2rem' }} />} 
              onClick={() => navigate('/deliveries')}
              sx={{
                background: 'linear-gradient(135deg, #0284c7 0%, #0ea5e9 100%)',
                boxShadow: '0 4px 14px rgba(14, 165, 233, 0.3)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #0369a1 0%, #0284c7 100%)',
                  boxShadow: '0 6px 20px rgba(14, 165, 233, 0.4)',
                  transform: 'translateY(-1px)'
                },
                transition: 'all 0.3s ease',
                borderRadius: 2,
                px: 3,
                py: 1.5,
                fontWeight: 600,
                fontSize: '0.875rem'
              }}
            >
              Teslimatlarım
            </Button>
            <Button 
              size="medium" 
              variant="contained" 
              color="warning" 
              startIcon={<OrderIcon sx={{ fontSize: '1.2rem' }} />} 
              onClick={() => navigate('/orders')}
              sx={{
                background: 'linear-gradient(135deg, #d97706 0%, #f59e0b 100%)',
                boxShadow: '0 4px 14px rgba(245, 158, 11, 0.3)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #b45309 0%, #d97706 100%)',
                  boxShadow: '0 6px 20px rgba(245, 158, 11, 0.4)',
                  transform: 'translateY(-1px)'
                },
                transition: 'all 0.3s ease',
                borderRadius: 2,
                px: 3,
                py: 1.5,
                fontWeight: 600,
                fontSize: '0.875rem'
              }}
            >
              Siparişler
            </Button>
          </Stack>
        </Box>
      </Paper>

      {/* Stats cards */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(4, 1fr)' }, gap: 3, mb: 4 }}>
        {[
          {label:'Toplam Market',value:stats.totalMarkets, icon:<StorefrontIcon sx={{ fontSize: '1.5rem' }}/> ,bg:'linear-gradient(135deg,#059669,#10b981)',color:'#047857'},
          {label:'Toplam Teslimat',value:stats.totalDeliveries, icon:<DeliveryIcon sx={{ fontSize: '1.5rem' }}/>,bg:'linear-gradient(135deg,#0284c7,#0ea5e9)',color:'#0369a1'},
          {label:'Bekleyen Sipariş',value:stats.pendingOrders, icon:<PendingIcon sx={{ fontSize: '1.5rem' }}/>,bg:'linear-gradient(135deg,#d97706,#f59e0b)',color:'#b45309'},
          {label:'Tamamlanan Teslimat',value:stats.completedDeliveries, icon:<CheckCircleIcon sx={{ fontSize: '1.5rem' }}/>,bg:'linear-gradient(135deg,#7c3aed,#a855f7)',color:'#6d28d9'}
        ].map((s,idx)=> (
          <Paper 
            key={idx} 
            sx={{ 
              p: { xs: 2.5, md: 3.5 }, 
              display: 'flex', 
              alignItems: 'center', 
              gap: 2.5,
              background: 'linear-gradient(135deg, rgba(255,255,255,0.95) 0%, rgba(248,250,252,0.95) 100%)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255,255,255,0.2)',
              boxShadow: '0 8px 32px rgba(0,0,0,0.08)',
              transition: 'all 0.3s ease',
              '&:hover': {
                transform: 'translateY(-4px)',
                boxShadow: '0 12px 40px rgba(0,0,0,0.12)',
                border: `1px solid ${s.color}20`
              }
            }}
          >
            <Box
              sx={{
                width: 56,
                height: 56,
                borderRadius: '16px',
                background: s.bg,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: `0 8px 24px ${s.color}30`,
                transition: 'all 0.3s ease'
              }}
            >
              {s.icon}
            </Box>
            <Box sx={{ flex: 1 }}>
              <Typography 
                variant="body2" 
                sx={{ 
                  color: 'text.secondary',
                  fontWeight: 500,
                  fontSize: '0.875rem',
                  mb: 0.5,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em'
                }}
              >
                {s.label}
              </Typography>
              <Typography 
                variant="h4" 
                sx={{ 
                  fontWeight: 800,
                  fontSize: { xs: '1.75rem', md: '2rem' },
                  background: s.bg,
                  backgroundClip: 'text',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  lineHeight: 1.2
                }}
              >
                {s.value}
              </Typography>
            </Box>
          </Paper>
        ))}
      </Box>

      {/* Two columns sections stack on mobile */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
        <Paper sx={{ 
          p: { xs: 3, md: 4 },
          background: 'linear-gradient(135deg, rgba(255,255,255,0.95) 0%, rgba(248,250,252,0.95) 100%)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255,255,255,0.2)',
          boxShadow: '0 8px 32px rgba(0,0,0,0.08)',
          borderRadius: 3
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2.5 }}>
            <Box
              sx={{
                width: 40,
                height: 40,
                borderRadius: '12px',
                background: 'linear-gradient(135deg, #059669, #10b981)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 6px 20px rgba(16, 185, 129, 0.3)'
              }}
            >
              <StorefrontIcon sx={{ fontSize: '1.25rem', color: 'white' }} />
            </Box>
            <Typography 
              variant="h6" 
              sx={{ 
                fontWeight: 700,
                background: 'linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%)',
                backgroundClip: 'text',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                fontSize: '1.25rem'
              }}
            >
              Marketlerim
            </Typography>
          </Box>
          <List>
            {myMarkets.slice(0,5).map(m => (
              <ListItem 
                key={m.id} 
                sx={{
                  py: 1.25,
                  display: 'flex',
                  flexDirection: { xs: 'column', sm: 'row' }, // Küçük ekranlarda dikey, büyüklerde yatay
                  alignItems: { xs: 'flex-start', sm: 'center' }, // Hizalama
                  justifyContent: 'space-between', // İçerikleri iki yana yasla
                  width: '100%',
                  gap: { xs: 1, sm: 2 } // Elemanlar arası boşluk
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', flexGrow: 1 }}>
                  <ListItemIcon sx={{ minWidth: 48 }}>
                    <Box
                      sx={{
                        width: 36,
                        height: 36,
                        borderRadius: '10px',
                        background: 'linear-gradient(135deg, #059669, #10b981)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)'
                      }}
                    >
                      <StorefrontIcon sx={{ fontSize: '1.1rem', color: 'white' }} />
                    </Box>
                  </ListItemIcon>
                  <ListItemText 
                    primary={<Typography variant="subtitle2" fontWeight={700}>{m.name}</Typography>} 
                    secondary={<Typography variant="body2" color="text.secondary">{m.address}</Typography>}
                    sx={{ m: 0 }}
                  />
                </Box>
                <Button 
                  size="small" 
                  variant="outlined" 
                  onClick={()=>navigate(`/markets/${m.id}`)}
                  sx={{
                    minWidth: 'auto',
                    px: 2,
                    py: 0.5,
                    fontSize: '0.75rem',
                    borderRadius: 2,
                    borderColor: 'primary.main',
                    color: 'primary.main',
                    '&:hover': {
                      backgroundColor: 'primary.light',
                      color: 'white',
                      borderColor: 'primary.light'
                    },
                    alignSelf: { xs: 'flex-end', sm: 'center' } // Küçük ekranlarda sağa, büyüklerde ortaya hizala
                  }}
                >
                  Detay
                </Button>
              </ListItem>
            ))}
          </List>
        </Paper>

        <Paper sx={{ 
          p: { xs: 3, md: 4 },
          background: 'linear-gradient(135deg, rgba(255,255,255,0.95) 0%, rgba(248,250,252,0.95) 100%)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255,255,255,0.2)',
          boxShadow: '0 8px 32px rgba(0,0,0,0.08)',
          borderRadius: 3
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2.5 }}>
            <Box
              sx={{
                width: 40,
                height: 40,
                borderRadius: '12px',
                background: 'linear-gradient(135deg, #0284c7, #0ea5e9)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 6px 20px rgba(14, 165, 233, 0.3)'
              }}
            >
              <DeliveryIcon sx={{ fontSize: '1.25rem', color: 'white' }} />
            </Box>
            <Typography 
              variant="h6" 
              sx={{ 
                fontWeight: 700,
                background: 'linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%)',
                backgroundClip: 'text',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                fontSize: '1.25rem'
              }}
            >
              Son Teslimatlar
            </Typography>
          </Box>
          <List>
            {recentDeliveries.map((d,i)=> (
              <ListItem key={i} sx={{ py: 1.5 }}>
                <ListItemIcon sx={{ minWidth: 48 }}>
                  <Box
                    sx={{
                      width: 36,
                      height: 36,
                      borderRadius: '10px',
                      background: 'linear-gradient(135deg, #0284c7, #0ea5e9)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      boxShadow: '0 4px 12px rgba(14, 165, 233, 0.3)'
                    }}
                  >
                    <DeliveryIcon sx={{ fontSize: '1.1rem', color: 'white' }} />
                  </Box>
                </ListItemIcon>
                <ListItemText 
                  primary={<Typography variant="subtitle2" fontWeight={700}>{d.marketName}</Typography>} 
                  secondary={<Typography variant="body2" color="text.secondary">{d.createdAt}</Typography>} 
                />
              </ListItem>
            ))}
          </List>
        </Paper>
      </Box>
    </Box>
  );
};

export default SupplierDashboardPage;
