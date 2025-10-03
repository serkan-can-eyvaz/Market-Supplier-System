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
  Avatar,
  Fade,
  Slide,
  Zoom,
  IconButton,
  Tooltip,
  InputAdornment,
  Stack,
  Button,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Divider,
  Chip,
  Card,
  CardContent,
} from '@mui/material';
import { Store } from 'lucide-react';
import apiService from '../services/api';
import { Market } from '../types';
import {
  Search as SearchIcon,
  FilterList as FilterIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
  Store as StoreIcon,
  LocationOn as LocationIcon,
  Phone as PhoneIcon,
  Email as EmailIcon,
  CalendarToday as CalendarIcon,
  Visibility as VisibilityIcon,
  Add as AddIcon,
  Business as BusinessIcon,
} from '@mui/icons-material';
import { LogoIcon } from '../components/ui/Logo';

// Removed local LogoIcon; using shared responsive LogoIcon

const MarketsPage: React.FC = () => {
  const [markets, setMarkets] = useState<Market[]>([]);
  const [query, setQuery] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedMarket, setSelectedMarket] = useState<Market | null>(null);
  const [showMarketDialog, setShowMarketDialog] = useState<boolean>(false);
  const [showEditDialog, setShowEditDialog] = useState<boolean>(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState<boolean>(false);
  const [editForm, setEditForm] = useState<{ name: string; address: string; phone: string }>({ 
    name: '', 
    address: '', 
    phone: '' 
  });

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, market: Market) => {
    setAnchorEl(event.currentTarget);
    setSelectedMarket(market);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedMarket(null);
  };

  const handleViewMarket = async (market: Market) => {
    console.log('handleViewMarket called with market:', market);
    try {
      const marketDetails = await apiService.getMarketById(market.id);
      console.log('Market details fetched:', marketDetails);
      setSelectedMarket(marketDetails);
      setShowMarketDialog(true);
      console.log('showMarketDialog set to true');
    } catch (e: any) {
      console.error('Error fetching market details:', e);
      setError(e.response?.data?.message || 'Market detayları yüklenemedi');
    }
    handleMenuClose();
  };

  const handleEditMarket = (market: Market) => {
    console.log('handleEditMarket called with market:', market);
    setSelectedMarket(market);
    setEditForm({ 
      name: market.name, 
      address: market.address, 
      phone: market.phone 
    });
    setShowEditDialog(true);
    console.log('showEditDialog set to true');
    handleMenuClose();
  };

  const handleDeleteMarket = (market: Market) => {
    setSelectedMarket(market);
    setShowDeleteDialog(true);
    handleMenuClose();
  };

  const handleUpdateMarket = async () => {
    if (!selectedMarket) return;
    
    try {
      setLoading(true);
      const updatedMarket = await apiService.updateMarket(selectedMarket.id, editForm);
      setMarkets(markets.map(m => m.id === selectedMarket.id ? updatedMarket : m));
      setShowEditDialog(false);
      setSelectedMarket(null);
      setEditForm({ name: '', address: '', phone: '' });
    } catch (e: any) {
      setError(e.response?.data?.message || 'Market güncellenemedi');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!selectedMarket) return;
    
    try {
      setLoading(true);
      await apiService.deleteMarket(selectedMarket.id);
      setMarkets(markets.filter(m => m.id !== selectedMarket.id));
      setShowDeleteDialog(false);
      setSelectedMarket(null);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Market silinemedi');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setLoading(true);
    try {
      setError('');
      const data = await apiService.getMarkets();
      setMarkets(data);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Marketler yüklenemedi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    handleRefresh();
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return markets;
    return markets.filter(m =>
      m.name.toLowerCase().includes(q) ||
      m.address.toLowerCase().includes(q) ||
      m.phone.toLowerCase().includes(q) ||
      m.userEmail.toLowerCase().includes(q)
    );
  }, [markets, query]);

  console.log('Current state:', { showMarketDialog, showEditDialog, showDeleteDialog, selectedMarket });
  
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
              <LogoIcon size={64} />
              <Box>
                <Typography variant="h3" fontWeight="800" sx={{ mb: 1 }}>
                  Marketler
                </Typography>
                <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 500 }}>
                  Sistem marketlerini yönetin
                </Typography>
              </Box>
            </Box>
            <Stack direction="row" spacing={2}>
              <Tooltip title="Verileri Yenile">
                <IconButton
                  onClick={handleRefresh}
                  disabled={loading}
                  sx={{
                    background: 'rgba(240, 147, 251, 0.1)',
                    color: 'primary.main',
                    '&:hover': {
                      background: 'rgba(240, 147, 251, 0.2)',
                      transform: 'scale(1.05)',
                    },
                  }}
                >
                  <RefreshIcon />
                </IconButton>
              </Tooltip>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                sx={{
                  background: 'linear-gradient(135deg, #f093fb, #f5576c)',
                  borderRadius: 2,
                  px: 3,
                  py: 1.5,
                  fontWeight: 600,
                  textTransform: 'none',
                  boxShadow: '0 8px 25px rgba(240, 147, 251, 0.3)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #e085f0, #e54b6b)',
                    transform: 'translateY(-2px)',
                    boxShadow: '0 12px 35px rgba(240, 147, 251, 0.4)',
                  },
                }}
              >
                Yeni Market
              </Button>
            </Stack>
          </Box>

          {/* Search Section */}
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <TextField
              fullWidth
              placeholder="Ara (ad, adres, telefon, kullanıcı e-posta)"
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
          >
            {error}
          </Alert>
        </Slide>
      )}

      {/* Markets Table */}
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
                background: 'linear-gradient(135deg, #f093fb, #f5576c)',
                borderRadius: '6px',
                border: '2px solid rgba(255, 255, 255, 0.2)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #e085f0, #e54b6b)',
                },
              },
              '&::-webkit-scrollbar-thumb:active': {
                background: 'linear-gradient(135deg, #d077e8, #d93a5a)',
              },
              '&::-webkit-scrollbar-corner': {
                background: 'rgba(0, 0, 0, 0.1)',
              },
              // Firefox için
              scrollbarWidth: 'thin',
              scrollbarColor: '#f093fb rgba(0, 0, 0, 0.1)',
            }}
          >
            <Table>
              <TableHead>
                <TableRow sx={{ background: 'linear-gradient(135deg, #f093fb, #f5576c)' }}>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Market
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Adres
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    İletişim
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Kullanıcı
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Oluşturulma
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem', textAlign: 'center' }}>
                    İşlemler
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtered.map((market, index) => (
                  <Fade in timeout={1200 + index * 100} key={market.id}>
                    <TableRow
                      sx={{
                        '&:hover': {
                          background: 'rgba(240, 147, 251, 0.05)',
                          transform: 'scale(1.01)',
                          boxShadow: '0 4px 15px rgba(240, 147, 251, 0.1)',
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
                              {market.name}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              ID: {market.id}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <LocationIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2" sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            {market.address}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <PhoneIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {market.phone}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <EmailIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {market.userEmail}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <CalendarIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {new Date(market.createdAt).toLocaleDateString('tr-TR', {
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
                          onClick={(e) => handleMenuOpen(e, market)}
                          sx={{
                            color: 'text.secondary',
                            '&:hover': {
                              background: 'rgba(240, 147, 251, 0.1)',
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
        <MenuItem onClick={() => selectedMarket && handleViewMarket(selectedMarket)}>
          <ListItemIcon>
            <VisibilityIcon fontSize="small" sx={{ color: 'primary.main' }} />
          </ListItemIcon>
          <ListItemText primary="Görüntüle" />
        </MenuItem>
        <MenuItem onClick={() => selectedMarket && handleEditMarket(selectedMarket)}>
          <ListItemIcon>
            <EditIcon fontSize="small" sx={{ color: 'warning.main' }} />
          </ListItemIcon>
          <ListItemText primary="Düzenle" />
        </MenuItem>
        <Divider />
        <MenuItem onClick={() => selectedMarket && handleDeleteMarket(selectedMarket)}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" sx={{ color: 'error.main' }} />
          </ListItemIcon>
          <ListItemText primary="Sil" />
        </MenuItem>
      </Menu>

      {/* Market Details Dialog */}
      {showMarketDialog && selectedMarket && (
        <Box
          sx={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            p: 2,
          }}
        >
          <Paper
            elevation={24}
            sx={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: 3,
              p: 4,
              minWidth: 400,
              maxWidth: 500,
              width: '100%',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
              <LogoIcon size={48} />
              <Box>
                <Typography variant="h5" fontWeight="700">
                  {selectedMarket.name}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Market Detayları
                </Typography>
              </Box>
            </Box>
            
            <Box sx={{ mb: 3 }}>
              <Typography variant="body1" sx={{ mb: 1 }}>
                <strong>Adres:</strong> {selectedMarket.address}
              </Typography>
              <Typography variant="body1" sx={{ mb: 1 }}>
                <strong>Telefon:</strong> {selectedMarket.phone}
              </Typography>
              <Typography variant="body1" sx={{ mb: 1 }}>
                <strong>Kullanıcı:</strong> {selectedMarket.userEmail}
              </Typography>
              <Typography variant="body1" sx={{ mb: 1 }}>
                <strong>ID:</strong> {selectedMarket.id}
              </Typography>
              <Typography variant="body1" sx={{ mb: 1 }}>
                <strong>Oluşturulma:</strong> {new Date(selectedMarket.createdAt).toLocaleDateString('tr-TR')}
              </Typography>
            </Box>
            
            <Button
              variant="contained"
              fullWidth
              onClick={() => setShowMarketDialog(false)}
              sx={{
                background: 'linear-gradient(135deg, #f093fb, #f5576c)',
                borderRadius: 2,
                py: 1.5,
                fontWeight: 600,
                textTransform: 'none',
              }}
            >
              Kapat
            </Button>
          </Paper>
        </Box>
      )}

      {/* Edit Market Dialog */}
      {showEditDialog && selectedMarket && (
        <Box
          sx={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            p: 2,
          }}
        >
          <Paper
            elevation={24}
            sx={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: 3,
              p: 4,
              minWidth: 400,
              maxWidth: 500,
              width: '100%',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
            }}
          >
            <Typography variant="h5" fontWeight="700" sx={{ mb: 3 }}>
              Market Düzenle
            </Typography>
            
            <TextField
              fullWidth
              label="Market Adı"
              value={editForm.name}
              onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
              sx={{ mb: 2 }}
            />
            
            <TextField
              fullWidth
              label="Adres"
              multiline
              rows={3}
              value={editForm.address}
              onChange={(e) => setEditForm({ ...editForm, address: e.target.value })}
              sx={{ mb: 2 }}
            />
            
            <TextField
              fullWidth
              label="Telefon"
              value={editForm.phone}
              onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
              sx={{ mb: 3 }}
            />
            
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button
                variant="outlined"
                fullWidth
                onClick={() => setShowEditDialog(false)}
                sx={{ borderRadius: 2, py: 1.5 }}
              >
                İptal
              </Button>
              <Button
                variant="contained"
                fullWidth
                onClick={handleUpdateMarket}
                disabled={loading}
                sx={{
                  background: 'linear-gradient(135deg, #f093fb, #f5576c)',
                  borderRadius: 2,
                  py: 1.5,
                  fontWeight: 600,
                  textTransform: 'none',
                }}
              >
                {loading ? 'Güncelleniyor...' : 'Güncelle'}
              </Button>
            </Box>
          </Paper>
        </Box>
      )}

      {/* Delete Confirmation Dialog */}
      {showDeleteDialog && selectedMarket && (
        <Box
          sx={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            p: 2,
          }}
        >
          <Paper
            elevation={24}
            sx={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: 3,
              p: 4,
              minWidth: 400,
              maxWidth: 500,
              width: '100%',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
            }}
          >
            <Typography variant="h5" fontWeight="700" sx={{ mb: 2, color: 'error.main' }}>
              Marketi Sil
            </Typography>
            
            <Typography variant="body1" sx={{ mb: 3 }}>
              <strong>{selectedMarket.name}</strong> marketini silmek istediğinizden emin misiniz? 
              Bu işlem geri alınamaz.
            </Typography>
            
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button
                variant="outlined"
                fullWidth
                onClick={() => setShowDeleteDialog(false)}
                sx={{ borderRadius: 2, py: 1.5 }}
              >
                İptal
              </Button>
              <Button
                variant="contained"
                fullWidth
                onClick={handleConfirmDelete}
                disabled={loading}
                sx={{
                  background: 'linear-gradient(135deg, #f44336, #d32f2f)',
                  borderRadius: 2,
                  py: 1.5,
                  fontWeight: 600,
                  textTransform: 'none',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #d32f2f, #b71c1c)',
                  },
                }}
              >
                {loading ? 'Siliniyor...' : 'Sil'}
              </Button>
            </Box>
          </Paper>
        </Box>
      )}
    </Box>
  );
};

export default MarketsPage;
