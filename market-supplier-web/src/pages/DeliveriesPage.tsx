import React, { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Alert,
  Chip,
  Button,
  Stack,
  Pagination,
  Avatar,
  Fade,
  Slide,
  Zoom,
  IconButton,
  Tooltip,
  InputAdornment,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Divider,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import { Truck } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';
import { Delivery, PaginatedResponse } from '../types';
import {
  Refresh as RefreshIcon,
  Search as SearchIcon,
  MoreVert as MoreVertIcon,
  Visibility as VisibilityIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Store as StoreIcon,
  Business as BusinessIcon,
  CalendarToday as CalendarIcon,
  CheckCircle as CheckCircleIcon,
  Schedule as ScheduleIcon,
  LocationOn as LocationIcon,
  Directions as DirectionsIcon,
} from '@mui/icons-material';
import { LogoIcon } from '../components/ui/Logo';

// Removed local LogoIcon; using shared responsive LogoIcon

const DeliveriesPage: React.FC = () => {
  const { isAdmin } = useAuth();
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const [pagination, setPagination] = useState({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0
  });
  const [query, setQuery] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedDelivery, setSelectedDelivery] = useState<Delivery | null>(null);
  const [openDetailsDialog, setOpenDetailsDialog] = useState<boolean>(false);

  const loadDeliveries = async (page: number = pagination.page) => {
    try {
      setError('');
      setLoading(true);
      const response: PaginatedResponse<Delivery> = isAdmin 
        ? await apiService.getAllDeliveries(page, pagination.size)
        : await apiService.getMyDeliveries(page, pagination.size);
      setDeliveries(response.content || []);
      setPagination({
        page: response.page,
        size: response.size,
        totalElements: response.totalElements,
        totalPages: response.totalPages
      });
    } catch (e: any) {
      setError(e.response?.data?.message || 'Teslimatlar yüklenemedi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDeliveries(0);
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return deliveries;
    return deliveries.filter(d =>
      d.marketName.toLowerCase().includes(q) ||
      d.supplierCompanyName.toLowerCase().includes(q) ||
      d.deliveryStatus.toLowerCase().includes(q)
    );
  }, [deliveries, query]);

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, delivery: Delivery) => {
    setAnchorEl(event.currentTarget);
    setSelectedDelivery(delivery);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedDelivery(null);
  };

  const handleRefresh = async () => {
    await loadDeliveries(0);
  };

  const handleViewDetails = (delivery: Delivery) => {
    setSelectedDelivery(delivery);
    setOpenDetailsDialog(true);
    handleMenuClose();
  };

  const handleCompleteDelivery = async (delivery: Delivery) => {
    try {
      await apiService.completeDelivery(delivery.id);
      await loadDeliveries(pagination.page);
      handleMenuClose();
    } catch (e) {
      setError('Teslimat tamamlanamadı');
    }
  };


  const statusColor = (s: string) => s === 'DELIVERED' ? 'success' : 'warning';

  const getStatusText = (status: string) => {
    switch (status) {
      case 'IN_PROGRESS':
        return 'Devam Ediyor';
      case 'DELIVERED':
        return 'Teslim Edildi';
      default:
        return status;
    }
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
              <LogoIcon size={68} />
              <Box>
                <Typography variant="h3" fontWeight="800" sx={{ mb: 1 }}>
                  {isAdmin ? 'Tüm Teslimatlar' : 'Teslimatlarım'}
                </Typography>
                <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 500 }}>
                  {isAdmin ? 'Sistem teslimatlarını yönetin' : 'Teslimatlarınızı takip edin'}
                </Typography>
              </Box>
            </Box>
            <Tooltip title="Verileri Yenile">
              <IconButton
                onClick={handleRefresh}
                disabled={loading}
                sx={{
                  background: 'rgba(255, 107, 107, 0.1)',
                  color: 'primary.main',
                  '&:hover': {
                    background: 'rgba(255, 107, 107, 0.2)',
                    transform: 'scale(1.05)',
                  },
                }}
              >
                <RefreshIcon />
              </IconButton>
            </Tooltip>
          </Box>

          {/* Search Section */}
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <TextField
              fullWidth
              placeholder="Ara (market, tedarikçi, durum)"
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

      {/* Deliveries Table */}
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
                background: 'linear-gradient(135deg, #ff6b6b, #ee5a24)',
                borderRadius: '6px',
                border: '2px solid rgba(255, 255, 255, 0.2)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #ff5252, #e64a19)',
                },
              },
              '&::-webkit-scrollbar-thumb:active': {
                background: 'linear-gradient(135deg, #ff4444, #d84315)',
              },
              '&::-webkit-scrollbar-corner': {
                background: 'rgba(0, 0, 0, 0.1)',
              },
              // Firefox için
              scrollbarWidth: 'thin',
              scrollbarColor: '#ff6b6b rgba(0, 0, 0, 0.1)',
            }}
          >
            <Table>
              <TableHead>
                <TableRow sx={{ background: 'linear-gradient(135deg, #ff6b6b, #ee5a24)' }}>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Teslimat
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Market
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Tedarikçi
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Durum
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Tarih
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem', textAlign: 'center' }}>
                    İşlemler
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtered.map((delivery, index) => (
                  <Fade in timeout={1200 + index * 100} key={delivery.id}>
                    <TableRow
                      sx={{
                        '&:hover': {
                          background: 'rgba(255, 107, 107, 0.05)',
                          transform: 'scale(1.01)',
                          boxShadow: '0 4px 15px rgba(255, 107, 107, 0.1)',
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
                              #{delivery.id}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              Teslimat Numarası
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <StoreIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2" fontWeight="600">
                            {delivery.marketName}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <BusinessIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {delivery.supplierCompanyName}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={getStatusText(delivery.deliveryStatus)}
                          color={statusColor(delivery.deliveryStatus) as any}
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
                          <CalendarIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {new Date(delivery.createdAt).toLocaleDateString('tr-TR', {
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
                          onClick={(e) => handleMenuOpen(e, delivery)}
                          sx={{
                            color: 'text.secondary',
                            '&:hover': {
                              background: 'rgba(255, 107, 107, 0.1)',
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
        <MenuItem onClick={() => selectedDelivery && handleViewDetails(selectedDelivery)}>
          <ListItemIcon>
            <VisibilityIcon fontSize="small" sx={{ color: 'primary.main' }} />
          </ListItemIcon>
          <ListItemText primary="Detay" />
        </MenuItem>
        {!isAdmin && selectedDelivery?.deliveryStatus !== 'DELIVERED' && (
          <>
            <Divider />
            <MenuItem onClick={() => selectedDelivery && handleCompleteDelivery(selectedDelivery)}>
              <ListItemIcon>
                <CheckCircleIcon fontSize="small" sx={{ color: 'success.main' }} />
              </ListItemIcon>
              <ListItemText primary="Teslim Edildi" />
            </MenuItem>
          </>
        )}
      </Menu>

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <Slide direction="up" in timeout={1400}>
          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
            <Paper
              elevation={0}
              sx={{
                background: 'rgba(255, 255, 255, 0.95)',
                backdropFilter: 'blur(20px)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                borderRadius: 3,
                p: 2,
                boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
              }}
            >
              <Pagination
                count={pagination.totalPages}
                page={pagination.page + 1}
                onChange={(_, newPage) => loadDeliveries(newPage - 1)}
                color="primary"
                size="large"
                sx={{
                  '& .MuiPaginationItem-root': {
                    borderRadius: 2,
                    fontWeight: 600,
                  },
                }}
              />
            </Paper>
          </Box>
        </Slide>
      )}

      {/* Teslimat Detay Dialog */}
      <Dialog 
        open={openDetailsDialog} 
        onClose={() => setOpenDetailsDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Truck size={24} color="#1976d2" />
            <Typography variant="h6">
              Teslimat Detayları - #{selectedDelivery?.id}
            </Typography>
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedDelivery && (
            <Box sx={{ mt: 2 }}>
              {/* Durum Kartı */}
              <Card sx={{ mb: 3, bgcolor: selectedDelivery.deliveryStatus === 'DELIVERED' ? 'success.50' : 'warning.50' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                    {selectedDelivery.deliveryStatus === 'DELIVERED' ? (
                      <CheckCircleIcon sx={{ color: 'success.main', fontSize: 32 }} />
                    ) : (
                      <ScheduleIcon sx={{ color: 'warning.main', fontSize: 32 }} />
                    )}
                    <Box>
                      <Typography variant="h6" color={selectedDelivery.deliveryStatus === 'DELIVERED' ? 'success.main' : 'warning.main'}>
                        {selectedDelivery.deliveryStatus === 'DELIVERED' ? 'Teslim Edildi' : 'Beklemede'}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Teslimat #{selectedDelivery.id}
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>

              {/* Genel Bilgiler */}
              <Card sx={{ mb: 3 }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <StoreIcon sx={{ color: 'primary.main' }} />
                    Genel Bilgiler
                  </Typography>
                  <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 3 }}>
                    <Box>
                      <Typography variant="body2" color="text.secondary" gutterBottom>Market</Typography>
                      <Typography variant="body1" fontWeight="bold">{selectedDelivery.marketName}</Typography>
                    </Box>
                    <Box>
                      <Typography variant="body2" color="text.secondary" gutterBottom>Tedarikçi</Typography>
                      <Typography variant="body1" fontWeight="bold">Quantumix</Typography>
                    </Box>
                    <Box>
                      <Typography variant="body2" color="text.secondary" gutterBottom>Sipariş Tarihi</Typography>
                      <Typography variant="body1">
                        {new Date(selectedDelivery.createdAt).toLocaleString('tr-TR', {
                          day: '2-digit',
                          month: '2-digit',
                          year: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        {selectedDelivery.deliveryStatus === 'DELIVERED' ? 'Teslim Tarihi' : 'Tahmini Teslimat'}
                      </Typography>
                      <Typography variant="body1">
                        {selectedDelivery.deliveryTime ? 
                          new Date(selectedDelivery.deliveryTime).toLocaleString('tr-TR', {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit'
                          }) : 
                          'Belirtilmemiş'
                        }
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>

              {/* Konum Bilgileri */}
              <Card sx={{ mb: 3 }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <LocationIcon sx={{ color: 'primary.main' }} />
                    Konum Bilgileri
                  </Typography>
                  <Box sx={{ 
                    height: 200, 
                    bgcolor: 'grey.100', 
                    borderRadius: 2, 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'center',
                    border: '2px dashed',
                    borderColor: 'grey.300'
                  }}>
                    <Box sx={{ textAlign: 'center' }}>
                      <Typography variant="body2" color="text.secondary">
                        Konum bilgisi mevcut değil
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>

              {/* Rota Bilgileri */}
              {selectedDelivery.routeInfo && (
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <DirectionsIcon sx={{ color: 'primary.main' }} />
                      Rota Bilgileri
                    </Typography>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', bgcolor: 'grey.50', p: 2, borderRadius: 1 }}>
                      {selectedDelivery.routeInfo}
                    </Typography>
                  </CardContent>
                </Card>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDetailsDialog(false)} variant="outlined">
            Kapat
          </Button>
          {selectedDelivery?.deliveryStatus !== 'DELIVERED' && (
            <Button 
              onClick={() => {
                if (selectedDelivery) {
                  handleCompleteDelivery(selectedDelivery);
                  setOpenDetailsDialog(false);
                }
              }}
              variant="contained"
              color="success"
              startIcon={<CheckCircleIcon />}
            >
              Teslimatı Tamamla
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DeliveriesPage;
