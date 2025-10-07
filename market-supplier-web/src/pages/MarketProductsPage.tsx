import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  CardActions,
  Grid,
  TextField,
  InputAdornment,
  IconButton,
  Chip,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Badge,
  Fab,
  Snackbar,
  LinearProgress,
  Stack,
  Divider,
} from '@mui/material';
import {
  Search as SearchIcon,
  ShoppingCart as ShoppingCartIcon,
  Add as AddIcon,
  Remove as RemoveIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  Refresh as RefreshIcon,
  ShoppingBag as ShoppingBagIcon,
  AttachMoney as MoneyIcon,
  Inventory as InventoryIcon,
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';
import { ProductResponse, CartResponse, CartItem } from '../types';

const MarketProductsPage: React.FC = () => {
  const { isMarket, user } = useAuth();
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [cartLoading, setCartLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');
  const [cartDialogOpen, setCartDialogOpen] = useState(false);
  const [quantities, setQuantities] = useState<{ [key: number]: number }>({});

  useEffect(() => {
    if (isMarket) {
      fetchProducts();
      fetchCart();
    }
  }, [isMarket]);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      setError('');
      const productList = await apiService.getAvailableProductsForMarket();
      setProducts(productList);
      
      // Initialize quantities
      const initialQuantities: { [key: number]: number } = {};
      productList.forEach(product => {
        initialQuantities[product.id] = 1;
      });
      setQuantities(initialQuantities);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ürünler yüklenirken hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  const fetchCart = async () => {
    try {
      setCartLoading(true);
      const cartData = await apiService.getCartDetailed();
      setCart(cartData);
    } catch (err: any) {
      console.error('Cart yüklenirken hata:', err);
    } finally {
      setCartLoading(false);
    }
  };

  const handleAddToCart = async (productId: number) => {
    try {
      setError('');
      const quantity = quantities[productId] || 1;
      await apiService.addItemToCart(productId, quantity);
      await fetchCart();
      setSuccess('Ürün sepete eklendi!');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ürün sepete eklenirken hata oluştu');
    }
  };

  const handleUpdateCartItem = async (itemId: number, newQuantity: number) => {
    try {
      setError('');
      if (newQuantity <= 0) {
        await apiService.removeCartItem(itemId);
      } else {
        await apiService.updateCartItem(itemId, newQuantity);
      }
      await fetchCart();
      setSuccess('Sepet güncellendi!');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Sepet güncellenirken hata oluştu');
    }
  };

  const handleRemoveFromCart = async (itemId: number) => {
    try {
      setError('');
      await apiService.removeCartItem(itemId);
      await fetchCart();
      setSuccess('Ürün sepetten kaldırıldı!');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ürün sepetten kaldırılırken hata oluştu');
    }
  };

  const handleClearCart = async () => {
    if (window.confirm('Sepeti temizlemek istediğinizden emin misiniz?')) {
      try {
        setError('');
        await apiService.clearCart();
        await fetchCart();
        setSuccess('Sepet temizlendi!');
      } catch (err: any) {
        setError(err.response?.data?.message || 'Sepet temizlenirken hata oluştu');
      }
    }
  };

  const handleDownloadCartPdf = async () => {
    try {
      setError('');
      const pdfBlob = await apiService.downloadCartPdf();
      
      // Create download link
      const url = window.URL.createObjectURL(pdfBlob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `sepet-detayi-${new Date().toISOString().split('T')[0]}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      setSuccess('PDF başarıyla indirildi!');
    } catch (err: any) {
      setError(err.response?.data?.message || 'PDF indirilirken hata oluştu');
    }
  };

  const handleQuantityChange = (productId: number, newQuantity: number) => {
    setQuantities(prev => ({
      ...prev,
      [productId]: Math.max(1, newQuantity)
    }));
  };

  const filteredProducts = products.filter(product =>
    product.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    product.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    product.supplierCompanyName?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (!isMarket) {
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
          <Alert severity="error">Bu sayfaya erişim için market rolü gereklidir.</Alert>
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
      {/* Header */}
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
              Ürün Kataloğu
            </Typography>
            <Typography variant="h6" color="text.secondary">
              {filteredProducts.length} ürün mevcut
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <IconButton 
              onClick={fetchProducts}
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
            <Badge badgeContent={cart?.totalItems || 0} color="error">
              <Button
                variant="contained"
                startIcon={<ShoppingCartIcon />}
                onClick={() => setCartDialogOpen(true)}
                sx={{
                  background: 'linear-gradient(135deg, #ff9800 0%, #f57c00 100%)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #f57c00 0%, #ef6c00 100%)',
                    transform: 'translateY(-2px)',
                    boxShadow: '0 8px 25px rgba(255, 152, 0, 0.3)'
                  },
                  px: 3,
                  py: 1.5,
                  borderRadius: 2
                }}
              >
                Sepetim ({cart?.totalItems || 0})
              </Button>
            </Badge>
          </Box>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert severity="success" sx={{ mb: 3, borderRadius: 2 }}>
            {success}
          </Alert>
        )}

        {/* Search */}
        <Card sx={{ 
          background: 'rgba(255, 255, 255, 0.7)',
          backdropFilter: 'blur(10px)',
          borderRadius: 3
        }}>
          <CardContent>
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
              }}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 2
                }
              }}
            />
          </CardContent>
        </Card>
      </Paper>

      {/* Products Grid */}
      {loading ? (
        <Paper sx={{
          background: 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(20px)',
          borderRadius: 4,
          p: 4,
          textAlign: 'center'
        }}>
          <LinearProgress sx={{ mb: 2 }} />
          <Typography variant="h6">Ürünler yükleniyor...</Typography>
        </Paper>
      ) : (
        <Grid container spacing={3}>
          {filteredProducts.map((product) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={product.id}>
              <Card sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                background: 'rgba(255, 255, 255, 0.95)',
                backdropFilter: 'blur(20px)',
                borderRadius: 4,
                boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
                transition: 'all 0.3s ease',
                '&:hover': {
                  transform: 'translateY(-8px)',
                  boxShadow: '0 30px 60px rgba(0, 0, 0, 0.15)'
                }
              }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <InventoryIcon sx={{ color: 'primary.main', mr: 1 }} />
                    <Typography variant="h6" fontWeight="bold" noWrap>
                      {product.name}
                    </Typography>
                  </Box>
                  
                  {product.description && (
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                      {product.description}
                    </Typography>
                  )}

                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Chip
                      label={product.unit}
                      size="small"
                      color="info"
                      sx={{ mr: 1 }}
                    />
                    <Chip
                      label={product.supplierCompanyName || 'Tedarikçi'}
                      size="small"
                      color="secondary"
                    />
                  </Box>

                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <MoneyIcon sx={{ color: 'success.main', mr: 1 }} />
                    <Typography variant="h5" fontWeight="bold" color="success.main">
                      ₺{product.price.toFixed(2)}
                    </Typography>
                  </Box>

                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Typography variant="body2" color="text.secondary">
                      Stok: {product.stockQuantity}
                    </Typography>
                  </Box>

                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                    <IconButton
                      size="small"
                      onClick={() => handleQuantityChange(product.id, (quantities[product.id] || 1) - 1)}
                      disabled={(quantities[product.id] || 1) <= 1}
                    >
                      <RemoveIcon />
                    </IconButton>
                    <TextField
                      size="small"
                      value={quantities[product.id] || 1}
                      onChange={(e) => handleQuantityChange(product.id, parseInt(e.target.value) || 1)}
                      inputProps={{ min: 1, style: { textAlign: 'center' } }}
                      sx={{ width: 60 }}
                    />
                    <IconButton
                      size="small"
                      onClick={() => handleQuantityChange(product.id, (quantities[product.id] || 1) + 1)}
                      disabled={(quantities[product.id] || 1) >= product.stockQuantity}
                    >
                      <AddIcon />
                    </IconButton>
                  </Box>
                </CardContent>

                <CardActions sx={{ p: 2, pt: 0 }}>
                  <Button
                    fullWidth
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={() => handleAddToCart(product.id)}
                    disabled={product.stockQuantity <= 0}
                    sx={{
                      background: 'linear-gradient(135deg, #4caf50 0%, #8bc34a 100%)',
                      '&:hover': {
                        background: 'linear-gradient(135deg, #45a049 0%, #7cb342 100%)',
                        transform: 'translateY(-2px)',
                        boxShadow: '0 8px 25px rgba(76, 175, 80, 0.3)'
                      },
                      borderRadius: 2
                    }}
                  >
                    Sepete Ekle
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Cart Dialog */}
      <Dialog
        open={cartDialogOpen}
        onClose={() => setCartDialogOpen(false)}
        maxWidth="md"
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
          background: 'linear-gradient(135deg, #ff9800 0%, #f57c00 100%)',
          color: 'white',
          borderRadius: '16px 16px 0 0'
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <ShoppingBagIcon sx={{ mr: 1 }} />
              Sepetim
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Typography variant="h6">
                Toplam: ₺{cart?.totalAmount.toFixed(2) || '0.00'}
              </Typography>
              <Button
                size="small"
                variant="outlined"
                onClick={handleClearCart}
                disabled={!cart?.items.length}
                sx={{ color: 'white', borderColor: 'white' }}
              >
                Temizle
              </Button>
            </Box>
          </Box>
        </DialogTitle>
        
        <DialogContent sx={{ p: 3 }}>
          {cartLoading ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <LinearProgress sx={{ mb: 2 }} />
              <Typography>Sepet yükleniyor...</Typography>
            </Box>
          ) : !cart?.items.length ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <ShoppingCartIcon sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                Sepetiniz boş
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Ürün ekleyerek başlayın
              </Typography>
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow sx={{ background: 'linear-gradient(135deg, #ff9800 0%, #f57c00 100%)' }}>
                    <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Ürün</TableCell>
                    <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Birim Fiyat</TableCell>
                    <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Miktar</TableCell>
                    <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Toplam</TableCell>
                    <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>İşlemler</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {cart.items.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>
                        <Box>
                          <Typography variant="subtitle2" fontWeight="bold">
                            {item.productName}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {item.unit} • {item.supplierName}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Typography variant="subtitle2" color="success.main">
                          ₺{item.price.toFixed(2)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <IconButton
                            size="small"
                            onClick={() => handleUpdateCartItem(item.id, item.quantity - 1)}
                          >
                            <RemoveIcon />
                          </IconButton>
                          <Typography variant="body2" sx={{ minWidth: 30, textAlign: 'center' }}>
                            {item.quantity}
                          </Typography>
                          <IconButton
                            size="small"
                            onClick={() => handleUpdateCartItem(item.id, item.quantity + 1)}
                          >
                            <AddIcon />
                          </IconButton>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Typography variant="subtitle2" fontWeight="bold">
                          ₺{item.lineTotal.toFixed(2)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <IconButton
                          size="small"
                          onClick={() => handleRemoveFromCart(item.id)}
                          sx={{ color: 'error.main' }}
                        >
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DialogContent>

        <DialogActions sx={{ p: 3, gap: 2 }}>
          <Button 
            onClick={() => setCartDialogOpen(false)}
            variant="outlined"
          >
            Kapat
          </Button>
          <Button
            variant="contained"
            startIcon={<DownloadIcon />}
            onClick={handleDownloadCartPdf}
            disabled={!cart?.items.length}
            sx={{
              background: 'linear-gradient(135deg, #2196f3 0%, #21cbf3 100%)',
              '&:hover': {
                background: 'linear-gradient(135deg, #1976d2 0%, #1cb5e0 100%)'
              }
            }}
          >
            PDF İndir
          </Button>
        </DialogActions>
      </Dialog>

      {/* Success/Error Snackbar */}
      <Snackbar
        open={!!success}
        autoHideDuration={3000}
        onClose={() => setSuccess('')}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert onClose={() => setSuccess('')} severity="success" sx={{ width: '100%' }}>
          {success}
        </Alert>
      </Snackbar>

      <Snackbar
        open={!!error}
        autoHideDuration={5000}
        onClose={() => setError('')}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert onClose={() => setError('')} severity="error" sx={{ width: '100%' }}>
          {error}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default MarketProductsPage;
