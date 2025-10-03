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
  Switch,
  FormControlLabel,
  Card,
  CardContent,
  CardHeader,
  InputAdornment,
  Pagination,
  Avatar,
  Fade,
  Slide,
  LinearProgress,
  Stack,
  Divider,
} from '@mui/material';
import { Package } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { LogoIcon } from '../components/ui/Logo';
import apiService from '../services/api';
import { Product, PaginatedResponse } from '../types';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  Refresh as RefreshIcon,
  AttachMoney as MoneyIcon,
  Category as CategoryIcon,
  Schedule as ScheduleIcon,
} from '@mui/icons-material';

const ProductsPage: React.FC = () => {
  const { isSupplier, user, isAdmin } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [pagination, setPagination] = useState({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0
  });
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [showInactive, setShowInactive] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    unit: 'adet',
    price: 0,
    stockQuantity: 0,
  });

  const [stockEditId, setStockEditId] = useState<number | null>(null);
  const [stockEditValue, setStockEditValue] = useState<number>(0);
  
  // Fiyat düzenleme için state
  const [priceEditId, setPriceEditId] = useState<number | null>(null);
  const [priceEditValue, setPriceEditValue] = useState<number>(0);
  const [openPriceDialog, setOpenPriceDialog] = useState(false);

  const fetchProducts = useCallback(async (page: number = 0) => {
    try {
      setLoading(true);
      setError('');
      
      // Admin için tüm ürünleri göster (kalem olarak)
      if (isAdmin) {
        const productList = await apiService.getAllProducts();
        setProducts(productList);
        setPagination({
          page: 0,
          size: productList.length,
          totalElements: productList.length,
          totalPages: 1
        });
      } else if (showInactive) {
        const productList = await apiService.getAllProducts();
        setProducts(productList);
        // getAllProducts returns a simple array, no pagination
      } else {
        const response: PaginatedResponse<Product> = await apiService.getProducts(page, 10);
        setProducts(response.content || []);
        setPagination({
          page: response.page,
          size: response.size,
          totalElements: response.totalElements,
          totalPages: response.totalPages
        });
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ürünler yüklenirken hata oluştu');
    } finally {
      setLoading(false);
    }
  }, [showInactive, isAdmin]);

  useEffect(() => {
    if (isSupplier || isAdmin) {
      fetchProducts();
    }
  }, [isSupplier, isAdmin, fetchProducts]);

  const handleCreateProduct = () => {
    setEditingProduct(null);
    setFormData({
      name: '',
      description: '',
      unit: 'adet',
      price: 0,
      stockQuantity: 0,
    });
    setOpenDialog(true);
  };

  const handleEditProduct = (product: Product) => {
    setEditingProduct(product);
    setFormData({
      name: product.name,
      description: product.description || '',
      unit: product.unit,
      price: product.price,
      stockQuantity: product.stockQuantity ?? 0,
    });
    setOpenDialog(true);
  };

  const handleSaveProduct = async () => {
    try {
      if (editingProduct) {
        await apiService.updateProduct(editingProduct.id, formData as any);
        if (typeof formData.stockQuantity === 'number') {
          await apiService.updateProductStock(editingProduct.id, formData.stockQuantity);
        }
      } else {
        const created = await apiService.createProduct(formData as any);
        if (typeof formData.stockQuantity === 'number' && created?.id) {
          await apiService.updateProductStock(created.id, formData.stockQuantity);
        }
      }
      setOpenDialog(false);
      fetchProducts();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ürün kaydedilirken hata oluştu');
    }
  };

  const handleDeleteProduct = async (productId: number) => {
    if (window.confirm('Bu ürünü silmek istediğinizden emin misiniz?')) {
      try {
        await apiService.deleteProduct(productId);
        fetchProducts();
      } catch (err: any) {
        const status = err?.response?.status;
        // İlişkili kayıtlar nedeniyle silinemiyorsa: pasife al
        if (status === 400 || status === 409) {
          try {
            await apiService.toggleProductStatus(productId);
            fetchProducts();
            setError('Ürün ilişkili olduğu için silinemedi; pasife alındı.');
          } catch (e2: any) {
            setError(e2.response?.data?.message || 'Ürün silinemedi ve pasife alınamadı');
          }
        } else {
          setError(err.response?.data?.message || 'Ürün silinirken hata oluştu');
        }
      }
    }
  };

  const handleToggleStatus = async (productId: number) => {
    try {
      await apiService.toggleProductStatus(productId);
      fetchProducts();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ürün durumu güncellenirken hata oluştu');
    }
  };

  const handleSearch = async () => {
    if (searchTerm.trim()) {
      try {
        const searchResults = await apiService.searchProducts(searchTerm);
        setProducts(searchResults);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Arama yapılırken hata oluştu');
      }
    } else {
      fetchProducts();
    }
  };

  const openStockDialog = (p: Product) => {
    setStockEditId(p.id);
    setStockEditValue(p.stockQuantity ?? 0);
  };

  const saveStock = async () => {
    if (stockEditId == null) return;
    try {
      await apiService.updateProductStock(stockEditId, Math.max(0, stockEditValue));
      setStockEditId(null);
      fetchProducts();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Stok güncellenirken hata oluştu');
    }
  };

  const filteredProducts = products.filter(product => 
    showInactive || product.isActive
  );

  const handleRefresh = () => {
    fetchProducts(0);
  };

  // Fiyat düzenleme fonksiyonları
  const handleEditPrice = (product: Product) => {
    setPriceEditId(product.id);
    setPriceEditValue(product.price);
    setOpenPriceDialog(true);
  };

  const handleSavePrice = async () => {
    if (priceEditId && priceEditValue >= 0) {
      try {
        await apiService.updateProduct(priceEditId, { price: priceEditValue } as any);
        setOpenPriceDialog(false);
        setPriceEditId(null);
        setPriceEditValue(0);
        fetchProducts();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Fiyat güncellenirken hata oluştu');
      }
    }
  };

  const handleCancelPriceEdit = () => {
    setOpenPriceDialog(false);
    setPriceEditId(null);
    setPriceEditValue(0);
  };

  if (!isSupplier && !isAdmin) {
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
          <Alert severity="error">Bu sayfaya erişim için tedarikçi veya admin rolü gereklidir.</Alert>
        </Paper>
      </Box>
    );
  }

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
          <Typography variant="h6">Ürünler yükleniyor...</Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <Box sx={{ 
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      p: 3
    }}>
      <Slide direction="down" in={true} timeout={800}>
        <Box>
          {/* Header Section */}
          <Paper sx={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            borderRadius: 4,
            p: 3,
            mb: 3,
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)'
          }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
              <Box>
                <Typography variant="h4" sx={{ 
                  fontWeight: 'bold',
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  backgroundClip: 'text',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  mb: 1
                }}>
                  {isAdmin ? 'Tüm Ürünler (Kalem)' : 'Ürünlerim'}
                </Typography>
                <Typography variant="h6" color="text.secondary">
                  {isAdmin ? `Toplam ${pagination.totalElements} kalem` : `Toplam ${pagination.totalElements} ürün`}
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', gap: 2 }}>
                <IconButton 
                  onClick={handleRefresh}
                  sx={{
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    color: 'white',
                    '&:hover': {
                      background: 'linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%)',
                      transform: 'scale(1.05)'
                    }
                  }}
                >
                  <RefreshIcon />
                </IconButton>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={handleCreateProduct}
                  sx={{
                    background: 'linear-gradient(135deg, #4caf50 0%, #8bc34a 100%)',
                    '&:hover': {
                      background: 'linear-gradient(135deg, #45a049 0%, #7cb342 100%)',
                      transform: 'translateY(-2px)',
                      boxShadow: '0 8px 25px rgba(76, 175, 80, 0.3)'
                    },
                    px: 3,
                    py: 1.5,
                    borderRadius: 2
                  }}
                >
                  Yeni Ürün Ekle
                </Button>
              </Box>
            </Box>

            {error && (
              <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
                {error}
              </Alert>
            )}

            {/* Arama ve Filtreler */}
            <Card sx={{ 
              background: 'rgba(255, 255, 255, 0.7)',
              backdropFilter: 'blur(10px)',
              borderRadius: 3
            }}>
              <CardContent>
                <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
                  <Box sx={{ flex: 1, minWidth: 200 }}>
                    <TextField
                      fullWidth
                      placeholder="Ürün ara..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      InputProps={{
                        startAdornment: (
                          <InputAdornment position="start">
                            <SearchIcon sx={{ color: 'text.secondary' }} />
                          </InputAdornment>
                        ),
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton onClick={handleSearch} sx={{
                              background: 'linear-gradient(135deg, #2196f3 0%, #21cbf3 100%)',
                              color: 'white',
                              '&:hover': {
                                background: 'linear-gradient(135deg, #1976d2 0%, #1cb5e0 100%)'
                              }
                            }}>
                              <SearchIcon />
                            </IconButton>
                          </InputAdornment>
                        ),
                      }}
                      onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                      sx={{
                        '& .MuiOutlinedInput-root': {
                          borderRadius: 2
                        }
                      }}
                    />
                  </Box>
                  <Box>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={showInactive}
                          onChange={(e) => setShowInactive(e.target.checked)}
                          sx={{
                            '& .MuiSwitch-switchBase.Mui-checked': {
                              color: '#4caf50',
                            },
                            '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                              backgroundColor: '#4caf50',
                            },
                          }}
                        />
                      }
                      label="Pasif ürünleri göster"
                      sx={{ color: 'text.secondary', fontWeight: 'medium' }}
                    />
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Paper>

          {/* Products Table */}
          <Fade in={true} timeout={1000}>
            <Paper sx={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              borderRadius: 4,
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
              overflow: 'hidden'
            }}>
              {filteredProducts.length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 8 }}>
                  <Package size={80} color="#9e9e9e" style={{ marginBottom: 24, display: 'block', margin: '0 auto 24px auto' }} />
                  <Typography variant="h5" color="text.secondary" gutterBottom>
                    {searchTerm ? 'Arama kriterlerine uygun ürün bulunamadı' : 'Henüz ürün eklenmemiş'}
                  </Typography>
                  <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                    {searchTerm ? 'Farklı arama terimleri deneyin' : 'İlk ürününüzü ekleyerek başlayın'}
                  </Typography>
                  {!searchTerm && (
                    <Button
                      variant="contained"
                      startIcon={<AddIcon />}
                      onClick={handleCreateProduct}
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
                      Ürün Ekle
                    </Button>
                  )}
                </Box>
              ) : (
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow sx={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
                        <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>{isAdmin ? 'Kalem Adı' : 'Ürün Adı'}</TableCell>
                        <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Açıklama</TableCell>
                        <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Birim</TableCell>
                        <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Fiyat</TableCell>
                        <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Stok</TableCell>
                        <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Durum</TableCell>
                        <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Oluşturulma</TableCell>
                        <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>İşlemler</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {filteredProducts.map((product, index) => (
                        <TableRow 
                          key={product.id}
                          sx={{
                            '&:hover': {
                              background: 'rgba(76, 175, 80, 0.05)'
                            },
                            '&:nth-of-type(even)': {
                              background: 'rgba(0, 0, 0, 0.02)'
                            }
                          }}
                        >
                          <TableCell>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                              <LogoIcon size={40} />
                              <Typography variant="subtitle2" fontWeight="bold">
                                {product.name}
                              </Typography>
                            </Box>
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 200 }}>
                              {product.description || '-'}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={product.unit}
                              color="info"
                              size="small"
                              icon={<CategoryIcon />}
                            />
                          </TableCell>
                          <TableCell>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Typography variant="subtitle2" fontWeight="bold" color="success.main">
                                ₺{product.price.toFixed(2)}
                              </Typography>
                              {isAdmin && (
                                <IconButton
                                  size="small"
                                  onClick={() => handleEditPrice(product)}
                                  sx={{ 
                                    color: 'primary.main',
                                    '&:hover': { backgroundColor: 'primary.light', color: 'white' }
                                  }}
                                >
                                  <EditIcon fontSize="small" />
                                </IconButton>
                              )}
                            </Box>
                          </TableCell>
                          <TableCell>
                            <Stack direction="row" spacing={1} alignItems="center">
                              <Chip label={(product.stockQuantity ?? 0).toString()} color={(product.stockQuantity ?? 0) > 0 ? 'success' : 'default'} size="small" />
                              <Button size="small" variant="outlined" onClick={() => openStockDialog(product)}>Düzenle</Button>
                            </Stack>
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={product.isActive ? 'Aktif' : 'Pasif'}
                              color={product.isActive ? 'success' : 'default'}
                              size="small"
                              icon={product.isActive ? <VisibilityIcon /> : <VisibilityOffIcon />}
                            />
                          </TableCell>
                          <TableCell>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <ScheduleIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                              <Typography variant="body2" color="text.secondary">
                                {new Date(product.createdAt).toLocaleDateString('tr-TR')}
                              </Typography>
                            </Box>
                          </TableCell>
                          <TableCell>
                            <Stack direction="row" spacing={1}>
                              <IconButton
                                size="small"
                                onClick={() => handleToggleStatus(product.id)}
                                title={product.isActive ? 'Pasif yap' : 'Aktif yap'}
                                sx={{
                                  background: product.isActive ? 'rgba(255, 152, 0, 0.1)' : 'rgba(76, 175, 80, 0.1)',
                                  color: product.isActive ? 'warning.main' : 'success.main',
                                  '&:hover': {
                                    background: product.isActive ? 'rgba(255, 152, 0, 0.2)' : 'rgba(76, 175, 80, 0.2)',
                                    transform: 'scale(1.1)'
                                  }
                                }}
                              >
                                {product.isActive ? <VisibilityOffIcon /> : <VisibilityIcon />}
                              </IconButton>
                              <IconButton
                                size="small"
                                onClick={() => handleEditProduct(product)}
                                sx={{
                                  background: 'rgba(33, 150, 243, 0.1)',
                                  color: 'primary.main',
                                  '&:hover': {
                                    background: 'rgba(33, 150, 243, 0.2)',
                                    transform: 'scale(1.1)'
                                  }
                                }}
                              >
                                <EditIcon />
                              </IconButton>
                              <IconButton
                                size="small"
                                onClick={() => handleDeleteProduct(product.id)}
                                sx={{
                                  background: 'rgba(244, 67, 54, 0.1)',
                                  color: 'error.main',
                                  '&:hover': {
                                    background: 'rgba(244, 67, 54, 0.2)',
                                    transform: 'scale(1.1)'
                                  }
                                }}
                              >
                                <DeleteIcon />
                              </IconButton>
                            </Stack>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Paper>
          </Fade>

          {/* Pagination */}
          {!showInactive && pagination.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
              <Pagination
                count={pagination.totalPages}
                page={pagination.page + 1}
                onChange={(_, newPage) => fetchProducts(newPage - 1)}
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

          {/* Ürün Ekleme/Düzenleme Dialog */}
          <Dialog 
            open={openDialog} 
            onClose={() => setOpenDialog(false)} 
            maxWidth="sm" 
            fullWidth
            PaperProps={{
              sx: {
                background: 'rgba(255, 255, 255, 0.95)',
                backdropFilter: 'blur(20px)',
                borderRadius: 4
              }
            }}
          >
            <DialogTitle sx={{ 
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              color: 'white',
              borderRadius: '16px 16px 0 0'
            }}>
              {editingProduct ? 'Ürün Düzenle' : 'Yeni Ürün Ekle'}
            </DialogTitle>
            <DialogContent sx={{ p: 3 }}>
              <Box sx={{ pt: 2 }}>
                <TextField
                  fullWidth
                  label={isAdmin ? "Kalem Adı" : "Ürün Adı"}
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  margin="normal"
                  required
                  sx={{ mb: 2 }}
                />
                <TextField
                  fullWidth
                  label="Açıklama"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  margin="normal"
                  multiline
                  rows={3}
                  sx={{ mb: 2 }}
                />
                <TextField
                  fullWidth
                  label="Birim"
                  value={formData.unit}
                  onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                  margin="normal"
                  required
                  select
                  SelectProps={{ native: true }}
                  sx={{ mb: 2 }}
                >
                  <option value="adet">Adet</option>
                  <option value="kg">Kilogram</option>
                  <option value="g">Gram</option>
                  <option value="lt">Litre</option>
                  <option value="ml">Mililitre</option>
                  <option value="koli">Koli</option>
                  <option value="paket">Paket</option>
                  <option value="düzine">Düzine</option>
                </TextField>
                <TextField
                  fullWidth
                  label="Birim Fiyat (₺)"
                  type="number"
                  value={formData.price}
                  onChange={(e) => setFormData({ ...formData, price: parseFloat(e.target.value) || 0 })}
                  margin="normal"
                  required
                  inputProps={{ min: 0, step: 0.01 }}
                />
                <TextField
                  fullWidth
                  label="Stok Miktarı"
                  type="number"
                  value={formData.stockQuantity}
                  onChange={(e) => setFormData({ ...formData, stockQuantity: Math.max(0, parseInt(e.target.value || '0', 10)) })}
                  margin="normal"
                  inputProps={{ min: 0, step: 1 }}
                />
              </Box>
            </DialogContent>
            <DialogActions sx={{ p: 3, gap: 2 }}>
              <Button 
                onClick={() => setOpenDialog(false)}
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
                variant="contained"
                onClick={handleSaveProduct}
                disabled={!formData.name.trim() || formData.price <= 0}
                sx={{
                  background: 'linear-gradient(135deg, #4caf50 0%, #8bc34a 100%)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #45a049 0%, #7cb342 100%)'
                  }
                }}
              >
                {editingProduct ? 'Güncelle' : 'Ekle'}
              </Button>
            </DialogActions>
          </Dialog>

          {/* Stok Düzenleme Dialog */}
          <Dialog open={stockEditId != null} onClose={() => setStockEditId(null)} maxWidth="xs" fullWidth>
            <DialogTitle>Stok Güncelle</DialogTitle>
            <DialogContent>
              <TextField
                fullWidth
                autoFocus
                label="Stok Miktarı"
                type="number"
                value={stockEditValue}
                onChange={(e) => setStockEditValue(Math.max(0, parseInt(e.target.value || '0', 10)))}
                margin="normal"
                inputProps={{ min: 0, step: 1 }}
              />
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setStockEditId(null)}>İptal</Button>
              <Button onClick={saveStock} variant="contained">Kaydet</Button>
            </DialogActions>
          </Dialog>

          {/* Fiyat Düzenleme Dialog */}
          <Dialog open={openPriceDialog} onClose={handleCancelPriceEdit} maxWidth="xs" fullWidth>
            <DialogTitle>Fiyat Güncelle</DialogTitle>
            <DialogContent>
              <TextField
                fullWidth
                label="Yeni Fiyat (₺)"
                type="number"
                value={priceEditValue}
                onChange={(e) => setPriceEditValue(parseFloat(e.target.value) || 0)}
                margin="normal"
                InputProps={{
                  startAdornment: <InputAdornment position="start">₺</InputAdornment>,
                }}
                inputProps={{ min: 0, step: 0.01 }}
              />
            </DialogContent>
            <DialogActions>
              <Button onClick={handleCancelPriceEdit}>İptal</Button>
              <Button 
                onClick={handleSavePrice} 
                variant="contained"
                disabled={priceEditValue < 0}
                sx={{
                  background: 'linear-gradient(135deg, #4caf50 0%, #8bc34a 100%)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #45a049 0%, #7cb342 100%)'
                  }
                }}
              >
                Kaydet
              </Button>
            </DialogActions>
          </Dialog>
        </Box>
      </Slide>
    </Box>
  );
};

export default ProductsPage;
