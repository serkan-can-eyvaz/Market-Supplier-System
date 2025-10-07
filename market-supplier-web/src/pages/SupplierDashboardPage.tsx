import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  CardHeader,
  Button,
  Stack,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  ListItemSecondaryAction,
  Divider,
  Paper,
  Avatar,
  Chip,
  IconButton,
  Fade,
  Slide,
  Alert,
  Badge,
  Tooltip,
  LinearProgress,
} from '@mui/material';
import {
  Storefront as StorefrontIcon,
  LocalShipping as DeliveryIcon,
  ShoppingCart as OrderIcon,
  Add as AddIcon,
  Visibility as ViewIcon,
  TrendingUp as TrendingUpIcon,
  CheckCircle as CheckCircleIcon,
  Pending as PendingIcon,
  Schedule as ScheduleIcon,
  Business as BusinessIcon,
  Phone as PhoneIcon,
  LocationOn as LocationIcon,
  AttachMoney as MoneyIcon,
  Inventory as InventoryIcon,
  Refresh as RefreshIcon,
  ArrowForward as ArrowForwardIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';
import { Market, Delivery, Order, OrderStatus, DeliveryStatus } from '../types';
import { LineChart } from '@mui/x-charts';
import { LogoIcon } from '../components/ui/Logo';

// Grid v7 tip farklarını aşmak için Box ile iki sütunlu düzen kullanıyoruz

const SupplierDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [myMarkets, setMyMarkets] = useState<Market[]>([]);
  const [recentDeliveries, setRecentDeliveries] = useState<Delivery[]>([]);
  const [pendingOrders, setPendingOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [stats, setStats] = useState({ totalMarkets: 0, totalDeliveries: 0, pendingOrders: 0, completedDeliveries: 0 });

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        setError('');
        
        // Markets
        try { 
          const marketsResponse = await apiService.getMyMarkets();
          const markets = marketsResponse.content || [];
          setMyMarkets(markets);
          setStats(prev => ({ ...prev, totalMarkets: markets.length }));
        } catch (err) {
          console.error('Markets yüklenemedi:', err);
        }
        
        // Deliveries
        try { 
          const deliveriesResponse = await apiService.getMyDeliveries();
          const deliveries = deliveriesResponse.content || [];
          setRecentDeliveries(deliveries.slice(0, 5));
          setStats(prev => ({ 
            ...prev, 
            totalDeliveries: deliveries.length,
            completedDeliveries: deliveries.filter(d => d.deliveryStatus === DeliveryStatus.DELIVERED).length
          }));
        } catch (err) {
          console.error('Deliveries yüklenemedi:', err);
        }

        
        // Pending Orders (paged)
        try { 
          const response = await apiService.getPendingOrdersPaged(0, 5);
          const pending = response.content || [];
          setPendingOrders(pending);
          setStats(prev => ({ ...prev, pendingOrders: response.totalElements || pending.length }));
        } catch (err) {
          console.error('Orders yüklenemedi:', err);
        }
      } catch (err) {
        setError('Veriler yüklenirken hata oluştu');
        console.error('Dashboard yükleme hatası:', err);
      } finally {
        setLoading(false);
      }
    };
    
    loadData();
  }, []);

  const handleRefresh = () => {
    window.location.reload();
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case DeliveryStatus.DELIVERED:
        return 'success';
      case DeliveryStatus.IN_PROGRESS:
        return 'warning';
      case 'PENDING':
        return 'info';
      default:
        return 'default';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case DeliveryStatus.DELIVERED:
        return 'Teslim Edildi';
      case DeliveryStatus.IN_PROGRESS:
        return 'Devam Ediyor';
      case 'PENDING':
        return 'Bekliyor';
      default:
        return status;
    }
  };

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
      <Paper sx={{ p: { xs: 2, md: 3 }, mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: { xs: 'stretch', md: 'center' }, justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }}>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="h5" fontWeight={800} sx={{ mb: 0.5, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>Tedarikçi Dashboard</Typography>
            <Typography variant="subtitle2" color="text.secondary">Hoş geldiniz, {user?.name}!</Typography>
          </Box>
          <Stack direction="row" spacing={1} alignItems="center">
            <Button size="small" variant="contained" startIcon={<AddIcon fontSize="small" />} onClick={() => navigate('/markets/add')}>Market Ekle</Button>
            <Button size="small" variant="contained" color="info" startIcon={<DeliveryIcon fontSize="small" />} onClick={() => navigate('/deliveries')}>Teslimatlarım</Button>
            <Button size="small" variant="contained" color="warning" startIcon={<OrderIcon fontSize="small" />} onClick={() => navigate('/orders')}>Siparişler</Button>
          </Stack>
        </Box>
      </Paper>

      {/* Stats cards */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(4, 1fr)' }, gap: 2, mb: 2 }}>
        {[{label:'Toplam Market',value:stats.totalMarkets, icon:<StorefrontIcon/> ,bg:'linear-gradient(135deg,#66bb6a,#43a047)'},{label:'Toplam Teslimat',value:stats.totalDeliveries, icon:<DeliveryIcon/>,bg:'linear-gradient(135deg,#29b6f6,#0288d1)'},{label:'Bekleyen Sipariş',value:stats.pendingOrders, icon:<PendingIcon/>,bg:'linear-gradient(135deg,#ffb74d,#fb8c00)'},{label:'Tamamlanan Teslimat',value:stats.completedDeliveries, icon:<CheckCircleIcon/>,bg:'linear-gradient(135deg,#ab47bc,#8e24aa)'}].map((s,idx)=> (
          <Paper key={idx} sx={{ p:{ xs:2, md:3 }, display:'flex', alignItems:'center', gap:2 }}>
            <LogoIcon size={40} bg={s.bg}>{s.icon}</LogoIcon>
            <Box>
              <Typography variant="caption" color="text.secondary">{s.label}</Typography>
              <Typography variant="h6" fontWeight={800}>{s.value}</Typography>
            </Box>
          </Paper>
        ))}
      </Box>

      {/* Two columns sections stack on mobile */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
        <Paper sx={{ p:{ xs:2, md:3 } }}>
          <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 1 }}>Marketlerim</Typography>
          <List>
            {myMarkets.slice(0,5).map(m => (
              <ListItem key={m.id} sx={{ py: 1.25 }}>
                <ListItemIcon><LogoIcon size={32}><StorefrontIcon/></LogoIcon></ListItemIcon>
                <ListItemText primary={<Typography variant="subtitle2" fontWeight={700}>{m.name}</Typography>} secondary={<Typography variant="body2" color="text.secondary">{m.address}</Typography>} />
                <ListItemSecondaryAction>
                  <Button size="small" variant="outlined" onClick={()=>navigate(`/markets/${m.id}`)}>Detay</Button>
                </ListItemSecondaryAction>
              </ListItem>
            ))}
          </List>
        </Paper>

        <Paper sx={{ p:{ xs:2, md:3 } }}>
          <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 1 }}>Son Teslimatlar</Typography>
          <List>
            {recentDeliveries.map((d,i)=> (
              <ListItem key={i} sx={{ py: 1 }}>
                <ListItemIcon><LogoIcon size={32} bg={'linear-gradient(135deg,#29b6f6,#0288d1)'}><DeliveryIcon/></LogoIcon></ListItemIcon>
                <ListItemText primary={<Typography variant="subtitle2" fontWeight={700}>{d.marketName}</Typography>} secondary={<Typography variant="body2" color="text.secondary">{d.createdAt}</Typography>} />
              </ListItem>
            ))}
          </List>
        </Paper>
      </Box>
    </Box>
  );
};

export default SupplierDashboardPage;
