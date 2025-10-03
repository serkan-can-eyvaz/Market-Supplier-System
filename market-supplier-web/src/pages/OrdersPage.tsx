import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Chip,
  Alert,
  Avatar,
  Fade,
  Slide,
  Zoom,
  Tooltip,
  InputAdornment,
  Stack,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Divider,
  Card,
  CardContent,
  Pagination,
} from '@mui/material';
import { ShoppingCart } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';
import { useLocation } from 'react-router-dom';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  PictureAsPdf as PictureAsPdfIcon,
  Refresh as RefreshIcon,
  Visibility as VisibilityIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  MoreVert as MoreVertIcon,
  Search as SearchIcon,
  Store as StoreIcon,
  AttachMoney as AttachMoneyIcon,
  CalendarToday as CalendarIcon,
  Inventory as InventoryIcon,
  Check as CheckIcon,
  Close as CloseIcon,
} from '@mui/icons-material';

// Professional Logo Components
const LogoIcon = ({ size = 28, color = "white" }: { size?: number; color?: string }) => (
  <Box
    sx={{
      width: size,
      height: size,
      borderRadius: '50%',
      background: 'linear-gradient(135deg, #ffffff 0%, #f8f9ff 100%)',
      boxShadow: '0 4px 16px rgba(255, 255, 255, 0.3)',
      border: '2px solid rgba(255, 255, 255, 0.4)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      position: 'relative'
    }}
  >
    <ShoppingCart size={size * 0.6} color="#667eea" />
  </Box>
);

interface OrderItem {
  id?: number;
  productName: string;
  quantity: number;
  unit: string;
  price: number;
}

interface Order {
  id: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'DELIVERED';
  createdAt: string;
  orderItems: OrderItem[];
  marketName?: string;
  marketAddress?: string;
  marketPhone?: string;
  market?: {
    id: number;
    name: string;
    address: string;
    phone?: string;
  };
}

const OrdersPage: React.FC = () => {
  const { isMarket, isSupplier, isAdmin } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [query, setQuery] = useState<string>('');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingOrder, setEditingOrder] = useState<Order | null>(null);
  const [orderItems, setOrderItems] = useState<OrderItem[]>([]);
  const [newItem, setNewItem] = useState<OrderItem>({
    productName: '',
    quantity: 1,
    unit: 'adet',
    price: 0,
  });
  const [openDetailDialog, setOpenDetailDialog] = useState(false);
  const [plannedDate, setPlannedDate] = useState<string>('');
  const [page, setPage] = useState<number>(1);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'PENDING'>('ALL');
  const location = useLocation();

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, order: Order) => {
    setAnchorEl(event.currentTarget);
    setSelectedOrder(order);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedOrder(null);
  };

  const handleRefresh = async () => {
    setLoading(true);
    try {
      setError('');
      
      if (isMarket) {
        const marketOrders = await apiService.getMarketOrders();
        setOrders(marketOrders);
        setTotalPages(1);
      } else if (isSupplier || isAdmin) {
        if (statusFilter === 'PENDING') {
          const resp = await apiService.getPendingOrdersPaged(page - 1, 10);
          setOrders(resp.content || []);
          setTotalPages(resp.totalPages || 1);
        } else {
          const allOrders = await apiService.getAllOrders(page - 1, 10);
          setOrders(allOrders.content || []);
          setTotalPages(allOrders.totalPages || 1);
        }
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Siparişler yüklenirken hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  const fetchOrders = useCallback(async () => {
    await handleRefresh();
  }, [isMarket, isSupplier, isAdmin, page, statusFilter]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const statusParam = params.get('status');
    if (statusParam === 'pending') {
      setStatusFilter('PENDING');
      setPage(1);
    }
  }, []);

  const handleCreateOrder = () => {
    setEditingOrder(null);
    setOrderItems([]);
    setNewItem({
      productName: '',
      quantity: 1,
      unit: 'adet',
      price: 0,
    });
    setOpenDialog(true);
  };

  const handleEditOrder = (order: Order) => {
    if (order.status === 'DELIVERED') {
      setError('Teslim edilmiş siparişler düzenlenemez');
      return;
    }
    setEditingOrder(order);
    setOrderItems([...order.orderItems]);
    setOpenDialog(true);
  };

  const handleViewDetails = (order: Order) => {
    setSelectedOrder(order);
    setPlannedDate('');
    setOpenDetailDialog(true);
  };

  const handleAddItem = () => {
    if (newItem.productName.trim() && newItem.quantity > 0 && newItem.price > 0) {
      setOrderItems([...orderItems, { ...newItem }]);
      setNewItem({
        productName: '',
        quantity: 1,
        unit: 'adet',
        price: 0,
      });
    }
  };

  const handleRemoveItem = (index: number) => {
    setOrderItems(orderItems.filter((_, i) => i !== index));
  };

  const handleSaveOrder = async () => {
    try {
      if (orderItems.length === 0) {
        setError('En az bir ürün eklemelisiniz');
        return;
      }

      if (editingOrder) {
        await apiService.updateOrder(editingOrder.id, { orderItems });
      } else {
        await apiService.createOrder({ orderItems });
      }

      setOpenDialog(false);
      fetchOrders();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Sipariş kaydedilirken hata oluştu');
    }
  };

  const handleDeleteOrder = async (orderId: number) => {
    if (window.confirm('Bu siparişi silmek istediğinizden emin misiniz?')) {
      try {
        await apiService.deleteOrder(orderId);
        fetchOrders();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Sipariş silinirken hata oluştu');
      }
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'warning';
      case 'APPROVED':
        return 'success';
      case 'REJECTED':
        return 'error';
      case 'DELIVERED':
        return 'info';
      default:
        return 'default';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'Bekliyor';
      case 'APPROVED':
        return 'Onaylandı';
      case 'REJECTED':
        return 'Reddedildi';
      case 'DELIVERED':
        return 'Teslim Edildi';
      default:
        return status;
    }
  };

  const calculateTotal = () => {
    return orderItems.reduce((total, item) => total + (item.quantity * item.price), 0);
  };

  const filteredOrders = orders.filter(order => {
    const q = query.trim().toLowerCase();
    if (!q) return true;
    return (
      order.id.toString().includes(q) ||
      getStatusText(order.status).toLowerCase().includes(q) ||
      (order.market?.name || order.marketName || '').toLowerCase().includes(q) ||
      order.orderItems.some(item => item.productName.toLowerCase().includes(q))
    );
  });

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
              <LogoIcon size={68} />
              <Box>
                <Typography variant="h3" fontWeight="800" sx={{ mb: 1 }}>
                  {isMarket ? 'Siparişlerim' : 'Tüm Siparişler'}
                </Typography>
                <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 500 }}>
                  {isMarket ? 'Market siparişlerinizi yönetin' : 'Sistem siparişlerini yönetin'}
                </Typography>
              </Box>
            </Box>
            <Stack direction="row" spacing={2}>
              <Tooltip title="Verileri Yenile">
                <IconButton
                  onClick={handleRefresh}
                  disabled={loading}
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
              {isMarket && (
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={handleCreateOrder}
                  sx={{
                    background: 'linear-gradient(135deg, #667eea, #764ba2)',
                    borderRadius: 2,
                    px: 3,
                    py: 1.5,
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
                  Yeni Sipariş
                </Button>
              )}
            </Stack>
          </Box>

          {/* Search Section */}
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <TextField
              fullWidth
              placeholder="Ara (ID, durum, market, ürün adı)"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon sx={{ color: 'text.secondary' }} />
                  </InputAdornment>
                ),
              }}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 2,
                  background: 'rgba(255, 255, 255, 0.8)',
                  backdropFilter: 'blur(10px)',
                  '&:hover': {
                    background: 'rgba(255, 255, 255, 0.9)',
                  },
                  '&.Mui-focused': {
                    background: 'rgba(255, 255, 255, 1)',
                  },
                },
              }}
            />
            {(isSupplier || isAdmin) && (
              <Stack direction="row" spacing={1}>
                <Button
                  variant={statusFilter === 'ALL' ? 'contained' : 'outlined'}
                  onClick={() => { setStatusFilter('ALL'); setPage(1); }}
                >
                  Tümü
                </Button>
                <Button
                  variant={statusFilter === 'PENDING' ? 'contained' : 'outlined'}
                  onClick={() => { setStatusFilter('PENDING'); setPage(1); }}
                >
                  Bekleyen
                </Button>
              </Stack>
            )}
          </Box>
        </Paper>
      </Slide>

      {/* Error Alert */}
      {error && (
        <Slide direction="down" in timeout={800}>
          <Alert 
            severity="error" 
            sx={{ 
              mb: 3, 
              borderRadius: 2,
              background: 'rgba(244, 67, 54, 0.1)',
              backdropFilter: 'blur(10px)',
              border: '1px solid rgba(244, 67, 54, 0.2)',
            }}
            onClose={() => setError('')}
          >
            {error}
          </Alert>
        </Slide>
      )}

      {/* Orders Table */}
      <Slide direction="up" in timeout={1000}>
        <Paper
          elevation={0}
          sx={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 3,
            overflow: 'hidden',
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
          }}
        >
          <TableContainer
            sx={{
              '&::-webkit-scrollbar': {
                width: '12px',
                height: '12px',
              },
              '&::-webkit-scrollbar-track': {
                background: 'rgba(0, 0, 0, 0.1)',
                borderRadius: '6px',
              },
              '&::-webkit-scrollbar-thumb': {
                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                borderRadius: '6px',
                border: '2px solid rgba(255, 255, 255, 0.2)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #5a6fd8, #6a4190)',
                },
              },
              '&::-webkit-scrollbar-thumb:active': {
                background: 'linear-gradient(135deg, #4e5fc6, #5e377e)',
              },
              '&::-webkit-scrollbar-corner': {
                background: 'rgba(0, 0, 0, 0.1)',
              },
              // Firefox için
              scrollbarWidth: 'thin',
              scrollbarColor: '#667eea rgba(0, 0, 0, 0.1)',
            }}
          >
            <Table>
              <TableHead>
                <TableRow sx={{ background: 'linear-gradient(135deg, #667eea, #764ba2)' }}>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Sipariş
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Durum
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Ürünler
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Tutar
                  </TableCell>
                  {isSupplier && (
                    <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                      Market
                    </TableCell>
                  )}
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Tarih
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem', textAlign: 'center' }}>
                    İşlemler
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredOrders.map((order, index) => (
                  <Fade in timeout={1200 + index * 100} key={order.id}>
                    <TableRow
                      sx={{
                        '&:hover': {
                          background: 'rgba(102, 126, 234, 0.05)',
                          transform: 'scale(1.01)',
                          boxShadow: '0 4px 15px rgba(102, 126, 234, 0.1)',
                        },
                        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                        borderBottom: '1px solid rgba(0, 0, 0, 0.05)',
                      }}
                    >
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <LogoIcon size={40} />
                          <Box>
                            <Typography variant="subtitle1" fontWeight="600">
                              #{order.id}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              Sipariş Numarası
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={getStatusText(order.status)}
                          color={getStatusColor(order.status) as any}
                          size="small"
                          sx={{
                            fontWeight: 600,
                            borderRadius: 2,
                            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
                          }}
                        />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <InventoryIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2" fontWeight="600">
                            {order.orderItems.length} ürün
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="body2" fontWeight="600">
                            ₺{order.orderItems.reduce((total, item) => total + (item.quantity * item.price), 0).toFixed(2)}
                          </Typography>
                        </Box>
                      </TableCell>
                      {isSupplier && (
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <StoreIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                            <Typography variant="body2">
                              {order.market?.name || order.marketName || 'Bilinmiyor'}
                            </Typography>
                          </Box>
                        </TableCell>
                      )}
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <CalendarIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {new Date(order.createdAt).toLocaleDateString('tr-TR', {
                              day: '2-digit',
                              month: '2-digit',
                              year: 'numeric',
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell sx={{ textAlign: 'center' }}>
                        <IconButton
                          onClick={(e) => handleMenuOpen(e, order)}
                          sx={{
                            color: 'text.secondary',
                            '&:hover': {
                              background: 'rgba(102, 126, 234, 0.1)',
                              color: 'primary.main',
                            },
                          }}
                        >
                          <MoreVertIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  </Fade>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      </Slide>

      {/* Pagination */}
      {(isSupplier || isAdmin) && totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 3 }}>
          <Pagination
            count={totalPages}
            page={page}
            onChange={(_, value) => setPage(value)}
            color="primary"
          />
        </Box>
      )}

      {/* Action Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
        PaperProps={{
          sx: {
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
            borderRadius: 2,
            minWidth: 200,
          },
        }}
      >
        <MenuItem onClick={() => selectedOrder && handleViewDetails(selectedOrder)}>
          <ListItemIcon>
            <VisibilityIcon fontSize="small" sx={{ color: 'primary.main' }} />
          </ListItemIcon>
          <ListItemText primary="Detay" />
        </MenuItem>
        <MenuItem 
          onClick={() => selectedOrder && handleEditOrder(selectedOrder)}
          disabled={selectedOrder?.status === 'DELIVERED'}
        >
          <ListItemIcon>
            <EditIcon fontSize="small" sx={{ color: 'warning.main' }} />
          </ListItemIcon>
          <ListItemText primary="Düzenle" />
        </MenuItem>
        {isSupplier && selectedOrder?.status === 'PENDING' && (
          <>
            <Divider />
            <MenuItem onClick={async () => {
              try {
                await apiService.approveOrder(selectedOrder.id);
                fetchOrders();
                handleMenuClose();
              } catch (e) {
                setError('Onay başarısız');
              }
            }}>
              <ListItemIcon>
                <CheckCircleIcon fontSize="small" sx={{ color: 'success.main' }} />
              </ListItemIcon>
              <ListItemText primary="Onayla" />
            </MenuItem>
            <MenuItem onClick={async () => {
              try {
                await apiService.rejectOrder(selectedOrder.id);
                fetchOrders();
                handleMenuClose();
              } catch (e) {
                setError('Ret başarısız');
              }
            }}>
              <ListItemIcon>
                <CancelIcon fontSize="small" sx={{ color: 'error.main' }} />
              </ListItemIcon>
              <ListItemText primary="Reddet" />
            </MenuItem>
          </>
        )}
        <Divider />
        <MenuItem onClick={async () => {
          try {
            const blob = await apiService.downloadOrderPdf(selectedOrder!.id);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `order-${selectedOrder!.id}.pdf`;
            a.click();
            window.URL.revokeObjectURL(url);
            handleMenuClose();
          } catch (e) {
            setError('PDF indirilemedi');
          }
        }}>
          <ListItemIcon>
            <PictureAsPdfIcon fontSize="small" sx={{ color: 'error.main' }} />
          </ListItemIcon>
          <ListItemText primary="PDF İndir" />
        </MenuItem>
        {isMarket && selectedOrder?.status !== 'DELIVERED' && (
          <MenuItem onClick={() => {
            handleDeleteOrder(selectedOrder!.id);
            handleMenuClose();
          }}>
            <ListItemIcon>
              <DeleteIcon fontSize="small" sx={{ color: 'error.main' }} />
            </ListItemIcon>
            <ListItemText primary="Sil" />
          </MenuItem>
        )}
      </Menu>

      {/* Sipariş Oluşturma/Düzenleme Dialog */}
      <Dialog 
        open={openDialog} 
        onClose={() => setOpenDialog(false)} 
        maxWidth="md" 
        fullWidth
        PaperProps={{
          sx: {
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 3,
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
          },
        }}
      >
        <DialogTitle sx={{ 
          background: 'linear-gradient(135deg, #667eea, #764ba2)',
          color: 'white',
          fontWeight: 700,
          textAlign: 'center',
          py: 2
        }}>
          {editingOrder ? 'Sipariş Düzenle' : 'Yeni Sipariş Oluştur'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <Typography variant="h6" gutterBottom>
              Sipariş Kalemleri
            </Typography>
            
            {/* Yeni ürün ekleme */}
            <Box sx={{ display: 'flex', gap: 2, mb: 3, p: 2, border: '1px dashed #ccc', borderRadius: 1 }}>
              <TextField
                label="Ürün Adı"
                value={newItem.productName}
                onChange={(e) => setNewItem({ ...newItem, productName: e.target.value })}
                size="small"
                sx={{ flex: 2 }}
              />
              <TextField
                label="Miktar"
                type="number"
                value={newItem.quantity}
                onChange={(e) => setNewItem({ ...newItem, quantity: parseInt(e.target.value) || 0 })}
                size="small"
                sx={{ width: 100 }}
              />
              <TextField
                label="Birim"
                value={newItem.unit}
                onChange={(e) => setNewItem({ ...newItem, unit: e.target.value })}
                size="small"
                sx={{ width: 100 }}
              />
              <TextField
                label="Fiyat"
                type="number"
                value={newItem.price}
                onChange={(e) => setNewItem({ ...newItem, price: parseFloat(e.target.value) || 0 })}
                size="small"
                sx={{ width: 120 }}
              />
              <Button
                variant="outlined"
                onClick={handleAddItem}
                disabled={!newItem.productName.trim() || newItem.quantity <= 0 || newItem.price <= 0}
              >
                Ekle
              </Button>
            </Box>

            {/* Sipariş kalemleri listesi */}
            <TableContainer 
              component={Paper} 
              sx={{ 
                mb: 2,
                '&::-webkit-scrollbar': {
                  width: '8px',
                  height: '8px',
                },
                '&::-webkit-scrollbar-track': {
                  background: 'rgba(0, 0, 0, 0.1)',
                  borderRadius: '4px',
                },
                '&::-webkit-scrollbar-thumb': {
                  background: 'linear-gradient(135deg, #667eea, #764ba2)',
                  borderRadius: '4px',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #5a6fd8, #6a4190)',
                  },
                },
                scrollbarWidth: 'thin',
                scrollbarColor: '#667eea rgba(0, 0, 0, 0.1)',
              }}
            >
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Ürün Adı</TableCell>
                    <TableCell>Miktar</TableCell>
                    <TableCell>Birim</TableCell>
                    <TableCell>Fiyat</TableCell>
                    <TableCell>Tutar</TableCell>
                    <TableCell>İşlem</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {orderItems.map((item, index) => (
                    <TableRow key={index}>
                      <TableCell>{item.productName}</TableCell>
                      <TableCell>{item.quantity}</TableCell>
                      <TableCell>{item.unit}</TableCell>
                      <TableCell>₺{item.price.toFixed(2)}</TableCell>
                      <TableCell>₺{(item.quantity * item.price).toFixed(2)}</TableCell>
                      <TableCell>
                        <IconButton
                          size="small"
                          onClick={() => handleRemoveItem(index)}
                        >
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>

            <Box sx={{ textAlign: 'right' }}>
              <Typography variant="h6">
                Toplam: ₺{calculateTotal().toFixed(2)}
              </Typography>
            </Box>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 3, gap: 2 }}>
          <Button 
            onClick={() => setOpenDialog(false)}
            sx={{ 
              borderRadius: 2, 
              py: 1.5,
              px: 3,
              fontWeight: 600,
              textTransform: 'none'
            }}
          >
            İptal
          </Button>
          <Button
            variant="contained"
            onClick={handleSaveOrder}
            disabled={orderItems.length === 0}
            sx={{
              background: 'linear-gradient(135deg, #667eea, #764ba2)',
              borderRadius: 2,
              py: 1.5,
              px: 3,
              fontWeight: 600,
              textTransform: 'none',
              '&:hover': {
                background: 'linear-gradient(135deg, #5a6fd8, #6a4190)',
              },
            }}
          >
            {editingOrder ? 'Güncelle' : 'Oluştur'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Sipariş Detay Dialog */}
      <Dialog 
        open={openDetailDialog} 
        onClose={() => setOpenDetailDialog(false)} 
        maxWidth="md" 
        fullWidth
        PaperProps={{
          sx: {
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 3,
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
          },
        }}
      >
        <DialogTitle sx={{ 
          background: 'linear-gradient(135deg, #667eea, #764ba2)',
          color: 'white',
          fontWeight: 700,
          textAlign: 'center',
          py: 2
        }}>
          Sipariş Detayları - #{selectedOrder?.id}
        </DialogTitle>
        <DialogContent>
          {selectedOrder && (
            <Box>
              {/* Market Bilgileri */}
              <Box sx={{ mb: 3, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
                <Typography variant="h6" gutterBottom>
                  Market Bilgileri
                </Typography>
                <Typography><strong>Market Adı:</strong> {selectedOrder.market?.name || selectedOrder.marketName || 'Bilinmiyor'}</Typography>
                <Typography><strong>Adres:</strong> {selectedOrder.market?.address || selectedOrder.marketAddress || 'Bilinmiyor'}</Typography>
                <Typography><strong>Telefon:</strong> {selectedOrder.market?.phone || selectedOrder.marketPhone || 'Bilinmiyor'}</Typography>
              </Box>

              {/* Sipariş Durumu */}
              <Box sx={{ mb: 3, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
                <Typography variant="h6" gutterBottom>
                  Sipariş Durumu
                </Typography>
                <Chip
                  label={getStatusText(selectedOrder.status)}
                  color={getStatusColor(selectedOrder.status) as any}
                  size="medium"
                />
                <Typography sx={{ mt: 1 }}>
                  <strong>Oluşturulma Tarihi:</strong> {new Date(selectedOrder.createdAt).toLocaleString('tr-TR')}
                </Typography>
                {isSupplier && selectedOrder.status !== 'DELIVERED' && (
                  <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                    <TextField
                      size="small"
                      type="datetime-local"
                      label="Planlı Teslim Tarihi"
                      value={plannedDate}
                      onChange={(e) => setPlannedDate(e.target.value)}
                      InputLabelProps={{ shrink: true }}
                    />
                    <Button
                      variant="outlined"
                      onClick={async () => {
                        if (!plannedDate) return;
                        try {
                          await apiService.setPlannedDeliveryDate(selectedOrder.id, plannedDate);
                          setPlannedDate('');
                          setError('');
                        } catch (e) {
                          setError('Planlı tarih kaydedilemedi');
                        }
                      }}
                    >
                      Kaydet
                    </Button>
                  </Box>
                )}
              </Box>

              {/* Ürün Listesi */}
              <Box sx={{ mb: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Sipariş Kalemleri ({selectedOrder.orderItems.length} ürün)
                </Typography>
                <TableContainer 
                  component={Paper} 
                  variant="outlined"
                  sx={{
                    '&::-webkit-scrollbar': {
                      width: '8px',
                      height: '8px',
                    },
                    '&::-webkit-scrollbar-track': {
                      background: 'rgba(0, 0, 0, 0.1)',
                      borderRadius: '4px',
                    },
                    '&::-webkit-scrollbar-thumb': {
                      background: 'linear-gradient(135deg, #667eea, #764ba2)',
                      borderRadius: '4px',
                      '&:hover': {
                        background: 'linear-gradient(135deg, #5a6fd8, #6a4190)',
                      },
                    },
                    scrollbarWidth: 'thin',
                    scrollbarColor: '#667eea rgba(0, 0, 0, 0.1)',
                  }}
                >
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell><strong>Ürün Adı</strong></TableCell>
                        <TableCell align="right"><strong>Miktar</strong></TableCell>
                        <TableCell align="right"><strong>Birim</strong></TableCell>
                        <TableCell align="right"><strong>Birim Fiyat</strong></TableCell>
                        <TableCell align="right"><strong>Toplam</strong></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {selectedOrder.orderItems.map((item, index) => (
                        <TableRow key={index}>
                          <TableCell>{item.productName}</TableCell>
                          <TableCell align="right">{item.quantity}</TableCell>
                          <TableCell align="right">{item.unit}</TableCell>
                          <TableCell align="right">₺{item.price.toFixed(2)}</TableCell>
                          <TableCell align="right">₺{(item.quantity * item.price).toFixed(2)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>

              {/* Toplam Özet */}
              <Box sx={{ p: 2, bgcolor: 'primary.50', borderRadius: 1 }}>
                <Typography variant="h6" align="right">
                  <strong>Genel Toplam: ₺{selectedOrder.orderItems.reduce((total, item) => total + (item.quantity * item.price), 0).toFixed(2)}</strong>
                </Typography>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 3, gap: 2 }}>
          <Button 
            onClick={() => setOpenDetailDialog(false)}
            sx={{ 
              borderRadius: 2, 
              py: 1.5,
              px: 3,
              fontWeight: 600,
              textTransform: 'none'
            }}
          >
            Kapat
          </Button>
          {isSupplier && selectedOrder?.status === 'PENDING' && (
            <>
              <Button 
                variant="contained" 
                onClick={async () => {
                  try {
                    await apiService.approveOrder(selectedOrder.id);
                    fetchOrders();
                    setOpenDetailDialog(false);
                  } catch (e) {
                    setError('Onay başarısız');
                  }
                }}
                sx={{
                  background: 'linear-gradient(135deg, #4caf50, #2e7d32)',
                  borderRadius: 2,
                  py: 1.5,
                  px: 3,
                  fontWeight: 600,
                  textTransform: 'none',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #43a047, #1b5e20)',
                  },
                }}
              >
                Onayla
              </Button>
              <Button 
                variant="contained" 
                onClick={async () => {
                  try {
                    await apiService.rejectOrder(selectedOrder.id);
                    fetchOrders();
                    setOpenDetailDialog(false);
                  } catch (e) {
                    setError('Ret başarısız');
                  }
                }}
                sx={{
                  background: 'linear-gradient(135deg, #f44336, #d32f2f)',
                  borderRadius: 2,
                  py: 1.5,
                  px: 3,
                  fontWeight: 600,
                  textTransform: 'none',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #e53935, #b71c1c)',
                  },
                }}
              >
                Reddet
              </Button>
            </>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default OrdersPage;