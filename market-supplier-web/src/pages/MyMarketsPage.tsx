import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Button,
  Alert,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  ListItemSecondaryAction,
  Stack,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Pagination,
  Paper,
  Card,
  CardContent,
  CardHeader,
  Avatar,
  Chip,
  IconButton,
  Fade,
  Slide,
  Divider,
  LinearProgress,
} from '@mui/material';
import {
  Delete as DeleteIcon,
  Add as AddIcon,
  Storefront as StorefrontIcon,
  LocationOn as LocationIcon,
  Phone as PhoneIcon,
  Visibility as ViewIcon,
  Edit as EditIcon,
  Refresh as RefreshIcon,
  Business as BusinessIcon,
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';
import { Market, PaginatedResponse } from '../types';
import { useNavigate } from 'react-router-dom';
import { LogoIcon } from '../components/ui/Logo';

// Removed local LogoIcon; using shared responsive LogoIcon

const MyMarketsPage: React.FC = () => {
  const { user } = useAuth();
  const [markets, setMarkets] = useState<Market[]>([]);
  const [pagination, setPagination] = useState({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0
  });
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [marketToDelete, setMarketToDelete] = useState<Market | null>(null);
  const navigate = useNavigate();

  const loadMarkets = async (page: number = pagination.page) => {
    try {
      setLoading(true);
      setError('');
      const response: PaginatedResponse<Market> = await apiService.getMyMarkets(page, pagination.size);
      setMarkets(response.content || []);
      setPagination({
        page: response.page,
        size: response.size,
        totalElements: response.totalElements,
        totalPages: response.totalPages
      });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Marketler yüklenirken hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMarkets(0);
  }, []);

  const handleDeleteClick = (market: Market) => {
    setMarketToDelete(market);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!marketToDelete) return;
    
    try {
      await apiService.deleteMarket(marketToDelete.id);
      await loadMarkets(pagination.page);
      setDeleteDialogOpen(false);
      setMarketToDelete(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Market silinirken hata oluştu');
      setDeleteDialogOpen(false);
    }
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setMarketToDelete(null);
  };

  const handleRefresh = () => {
    loadMarkets(0);
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
          <Typography variant="h6">Marketler yükleniyor...</Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <Box sx={{ 
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      p: { xs: 2, md: 3 }
    }}>
      <Slide direction="down" in={true} timeout={800}>
        <Box>
          {/* Header Section */}
          <Paper sx={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            borderRadius: 4,
            p: { xs: 2, md: 3 },
            mb: 3,
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)'
          }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: { xs: 'stretch', md: 'center' }, gap: 2, mb: 2, flexWrap: 'wrap' }}>
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="h5" sx={{ 
                  fontWeight: 'bold',
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  backgroundClip: 'text',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  mb: 0.5,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis'
                }}>
                  Marketlerim
                </Typography>
                <Typography variant="subtitle2" color="text.secondary">
                  Toplam {pagination.totalElements} market
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                <IconButton 
                  onClick={handleRefresh}
                  size="small"
                  sx={{
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    color: 'white',
                    '&:hover': { background: 'linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%)' }
                  }}
                >
                  <RefreshIcon fontSize="small" />
                </IconButton>
                <Button
                  variant="contained"
                  startIcon={<AddIcon fontSize="small" />}
                  onClick={() => navigate('/markets/add')}
                  sx={{
                    background: 'linear-gradient(135deg, #4caf50 0%, #8bc34a 100%)',
                    '&:hover': { background: 'linear-gradient(135deg, #45a049 0%, #7cb342 100%)' },
                    px: { xs: 1.5, md: 3 },
                    py: { xs: 0.75, md: 1.5 },
                    borderRadius: 2,
                    fontSize: { xs: '0.75rem', md: '0.875rem' }
                  }}
                >
                  Yeni Market Ekle
                </Button>
              </Box>
            </Box>

            {error && (
              <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>
                {error}
              </Alert>
            )}
          </Paper>

          {/* Markets List */}
          <Fade in={true} timeout={1000}>
            <Paper sx={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              borderRadius: 4,
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
              overflow: 'hidden'
            }}>
              {markets.length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 8 }}>
                  <StorefrontIcon sx={{ fontSize: 80, color: 'text.secondary', mb: 3 }} />
                  <Typography variant="h5" color="text.secondary" gutterBottom>
                    Henüz market yok
                  </Typography>
                  <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                    İlk marketinizi ekleyerek başlayın
                  </Typography>
                  <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={() => navigate('/markets/add')}
                    sx={{
                      background: 'linear-gradient(135deg, #4caf50 0%, #8bc34a 100%)',
                      '&:hover': {
                        background: 'linear-gradient(135deg, #45a049 0%, #7cb342 100%)'
                      },
                      px: 4,
                      py: 1.5,
                      borderRadius: 2
                    }}
                  >
                    Market Ekle
                  </Button>
                </Box>
              ) : (
                <List>
                  {markets.map((market) => (
                    <ListItem key={market.id} sx={{ py: { xs: 2, md: 3 } }}>
                      <ListItemIcon>
                        <LogoIcon><StorefrontIcon /></LogoIcon>
                      </ListItemIcon>
                      <ListItemText
                        primary={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5, flexWrap: 'wrap' }}>
                            <Typography variant="subtitle1" fontWeight="bold" sx={{ mr: 1 }}>{market.name}</Typography>
                            {market.orderCount && market.orderCount > 0 && (
                              <Chip label={`${market.orderCount} sipariş`} color="info" size="small" />
                            )}
                          </Box>
                        }
                        secondary={
                          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 0.5 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                              <LocationIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
                              <Typography variant="body2" color="text.secondary">{market.address}</Typography>
                            </Box>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                              <PhoneIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
                              <Typography variant="body2" color="text.secondary">{market.phone}</Typography>
                            </Box>
                          </Box>
                        }
                      />
                      <ListItemSecondaryAction>
                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                          <Button size="small" variant="contained" onClick={() => navigate(`/markets/${market.id}`)} sx={{ px: 1.25, py: 0.5 }}>Detay</Button>
                          <Button size="small" color="error" variant="contained" onClick={() => handleDeleteClick(market)} sx={{ px: 1.25, py: 0.5 }}>Sil</Button>
                        </Stack>
                      </ListItemSecondaryAction>
                    </ListItem>
                  ))}
                </List>
              )}
            </Paper>
          </Fade>

          {/* Pagination */}
          {pagination.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
              <Pagination
                count={pagination.totalPages}
                page={pagination.page + 1}
                onChange={(_, newPage) => loadMarkets(newPage - 1)}
                color="primary"
                size="large"
                sx={{
                  '& .MuiPaginationItem-root': {
                    background: 'rgba(255, 255, 255, 0.9)',
                    backdropFilter: 'blur(10px)',
                    '&.Mui-selected': {
                      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                      color: 'white'
                    }
                  }
                }}
              />
            </Box>
          )}
        </Box>
      </Slide>

      {/* Silme Onay Dialogu */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
        PaperProps={{
          sx: {
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            borderRadius: 4
          }
        }}
      >
        <DialogTitle id="delete-dialog-title" sx={{ 
          background: 'linear-gradient(135deg, #f44336 0%, #d32f2f 100%)',
          color: 'white',
          borderRadius: '16px 16px 0 0'
        }}>
          Market Silme Onayı
        </DialogTitle>
        <DialogContent sx={{ p: 3 }}>
          <DialogContentText id="delete-dialog-description" sx={{ fontSize: '1.1rem' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
              <BusinessIcon sx={{ fontSize: 40, color: 'error.main' }} />
              <Typography variant="h6" fontWeight="bold">
                "{marketToDelete?.name}" marketini silmek istediğinizden emin misiniz?
              </Typography>
            </Box>
            <Typography variant="body1" color="error" fontWeight="bold" sx={{ mb: 2 }}>
              ⚠️ Bu işlem geri alınamaz!
            </Typography>
            {marketToDelete?.orderCount && marketToDelete.orderCount > 0 && (
              <Box sx={{ 
                background: 'rgba(255, 152, 0, 0.1)', 
                p: 2, 
                borderRadius: 2,
                border: '1px solid #ff9800'
              }}>
                <Typography variant="body2" color="warning.main" sx={{ fontWeight: 'bold' }}>
                  📦 Bu markette {marketToDelete.orderCount} adet sipariş bulunmaktadır.
                </Typography>
                <Typography variant="body2" color="error.main" sx={{ fontWeight: 'bold', mt: 1 }}>
                  🗑️ Market silindiğinde tüm siparişler ve teslimatlar da silinecektir.
                </Typography>
              </Box>
            )}
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ p: 3, gap: 2 }}>
          <Button 
            onClick={handleDeleteCancel}
            variant="outlined"
            sx={{ 
              borderColor: 'grey.400',
              color: 'grey.600',
              '&:hover': {
                borderColor: 'grey.600',
                background: 'rgba(0, 0, 0, 0.04)'
              }
            }}
          >
            İptal
          </Button>
          <Button 
            onClick={handleDeleteConfirm} 
            variant="contained"
            sx={{
              background: 'linear-gradient(135deg, #f44336 0%, #d32f2f 100%)',
              '&:hover': {
                background: 'linear-gradient(135deg, #d32f2f 0%, #b71c1c 100%)'
              }
            }}
          >
            Sil
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default MyMarketsPage;


