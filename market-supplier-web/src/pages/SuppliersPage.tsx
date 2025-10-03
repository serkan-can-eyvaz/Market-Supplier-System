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
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Avatar,
  Fade,
  Slide,
  IconButton,
  Tooltip,
  InputAdornment,
  Stack,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Divider,
} from '@mui/material';
import {
  Search as SearchIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  BusinessCenter as BusinessCenterIcon,
  Refresh as RefreshIcon,
  Phone as PhoneIcon,
  Email as EmailIcon,
  CalendarToday as CalendarIcon,
  Visibility as VisibilityIcon,
  Add as AddIcon,
  PersonAdd as PersonAddIcon,
  Business as CompanyIcon,
} from '@mui/icons-material';
import apiService from '../services/api';
import { Supplier } from '../types';

// Professional Logo Component
const LogoIcon = ({ size = 48, children, bg = 'linear-gradient(135deg, #4facfe, #00f2fe)' }: { size?: number; children: React.ReactNode; bg?: string }) => (
  <Box
    sx={{
      width: size,
      height: size,
      borderRadius: '50%',
      background: bg,
      boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
      border: '2px solid rgba(255,255,255,0.25)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}
  >
    {children}
  </Box>
);

const SuppliersPage: React.FC = () => {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [query, setQuery] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedSupplier, setSelectedSupplier] = useState<Supplier | null>(null);
  const [showSupplierDialog, setShowSupplierDialog] = useState<boolean>(false);
  const [showEditDialog, setShowEditDialog] = useState<boolean>(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState<boolean>(false);
  const [open, setOpen] = useState<boolean>(false);
  const [form, setForm] = useState({ name: '', email: '', password: '', companyName: '', phone: '', phoneNumberId: '' });
  const [editForm, setEditForm] = useState<{ name: string; email: string; companyName: string; phone: string; phoneNumberId?: string }>({ 
    name: '', 
    email: '', 
    companyName: '', 
    phone: '',
    phoneNumberId: ''
  });

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, supplier: Supplier) => {
    setAnchorEl(event.currentTarget);
    setSelectedSupplier(supplier);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedSupplier(null);
  };

  const handleViewSupplier = async (supplier: Supplier) => {
    console.log('handleViewSupplier called with supplier:', supplier);
    try {
      const supplierDetails = await apiService.getSupplierById(supplier.id);
      console.log('Supplier details fetched:', supplierDetails);
      setSelectedSupplier(supplierDetails);
      setShowSupplierDialog(true);
      console.log('showSupplierDialog set to true');
    } catch (e: any) {
      console.error('Error fetching supplier details:', e);
      setError(e.response?.data?.message || 'Tedarikçi detayları yüklenemedi');
    }
    handleMenuClose();
  };

  const handleEditSupplier = (supplier: Supplier) => {
    console.log('handleEditSupplier called with supplier:', supplier);
    setSelectedSupplier(supplier);
    setEditForm({ 
      name: supplier.userName, 
      email: supplier.userEmail, 
      companyName: supplier.companyName, 
      phone: supplier.phone,
      phoneNumberId: supplier.phoneNumberId || ''
    });
    setShowEditDialog(true);
    console.log('showEditDialog set to true');
    handleMenuClose();
  };

  const handleDeleteSupplier = (supplier: Supplier) => {
    setSelectedSupplier(supplier);
    setShowDeleteDialog(true);
    handleMenuClose();
  };

  const handleUpdateSupplier = async () => {
    if (!selectedSupplier) return;
    
    try {
      setLoading(true);
      // önce temel alanları güncelle
      const { phoneNumberId, ...basic } = editForm;
      const updatedSupplier = await apiService.updateSupplier(selectedSupplier.id, basic as any);
      // phone_number_id değişmişse güncelle
      if (typeof phoneNumberId === 'string') {
        await apiService.updateSupplierPhoneNumberId(selectedSupplier.id, phoneNumberId);
      }
      setSuppliers(suppliers.map(s => s.id === selectedSupplier.id ? updatedSupplier : s));
      setShowEditDialog(false);
      setSelectedSupplier(null);
      setEditForm({ name: '', email: '', companyName: '', phone: '', phoneNumberId: '' });
    } catch (e: any) {
      setError(e.response?.data?.message || 'Tedarikçi güncellenemedi');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!selectedSupplier) return;
    
    try {
      setLoading(true);
      await apiService.deleteSupplier(selectedSupplier.id);
      setSuppliers(suppliers.filter(s => s.id !== selectedSupplier.id));
      setShowDeleteDialog(false);
      setSelectedSupplier(null);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Tedarikçi silinemedi');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setLoading(true);
      try {
        setError('');
        const data = await apiService.getSuppliers();
        setSuppliers(data);
      } catch (e: any) {
        setError(e.response?.data?.message || 'Tedarikçiler yüklenemedi');
    } finally {
      setLoading(false);
      }
    };

  useEffect(() => {
    handleRefresh();
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return suppliers;
    return suppliers.filter(s =>
      s.companyName.toLowerCase().includes(q) ||
      s.phone.toLowerCase().includes(q) ||
      s.userEmail.toLowerCase().includes(q)
    );
  }, [suppliers, query]);

  console.log('Current state:', { showSupplierDialog, showEditDialog, showDeleteDialog, selectedSupplier });

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
              <LogoIcon size={64}>
                <BusinessCenterIcon sx={{ fontSize: 32, color: 'white' }} />
              </LogoIcon>
              <Box>
                <Typography variant="h3" fontWeight="800" sx={{ mb: 1 }}>
        Tedarikçiler
      </Typography>
                <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 500 }}>
                  Sistem tedarikçilerini yönetin
                </Typography>
              </Box>
            </Box>
            <Stack direction="row" spacing={2}>
              <Tooltip title="Verileri Yenile">
                <IconButton
                  onClick={handleRefresh}
                  disabled={loading}
                  sx={{
                    background: 'rgba(79, 172, 254, 0.1)',
                    color: 'primary.main',
                    '&:hover': {
                      background: 'rgba(79, 172, 254, 0.2)',
                      transform: 'scale(1.05)',
                    },
                  }}
                >
                  <RefreshIcon />
                </IconButton>
              </Tooltip>
              <Button
                variant="contained"
                startIcon={<PersonAddIcon />}
                onClick={() => setOpen(true)}
                sx={{
                  background: 'linear-gradient(135deg, #4facfe, #00f2fe)',
                  borderRadius: 2,
                  px: 3,
                  py: 1.5,
                  fontWeight: 600,
                  textTransform: 'none',
                  boxShadow: '0 8px 25px rgba(79, 172, 254, 0.3)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #3d8bfe, #00d4ff)',
                    transform: 'translateY(-2px)',
                    boxShadow: '0 12px 35px rgba(79, 172, 254, 0.4)',
                  },
                }}
              >
                Tedarikçi Ekle
              </Button>
            </Stack>
      </Box>

          {/* Search Section */}
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
      <TextField
        fullWidth
              placeholder="Ara (şirket adı, telefon, kullanıcı e-posta)"
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

      {/* Suppliers Table */}
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
                background: 'linear-gradient(135deg, #4facfe, #00f2fe)',
                borderRadius: '6px',
                border: '2px solid rgba(255, 255, 255, 0.2)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #3d8bfe, #00d4fe)',
                },
              },
              '&::-webkit-scrollbar-thumb:active': {
                background: 'linear-gradient(135deg, #2d7bfe, #00c6fe)',
              },
              '&::-webkit-scrollbar-corner': {
                background: 'rgba(0, 0, 0, 0.1)',
              },
              // Firefox için
              scrollbarWidth: 'thin',
              scrollbarColor: '#4facfe rgba(0, 0, 0, 0.1)',
            }}
          >
            <Table>
          <TableHead>
                <TableRow sx={{ background: 'linear-gradient(135deg, #4facfe, #00f2fe)' }}>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Tedarikçi
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
                {filtered.map((supplier, index) => (
                  <Fade in timeout={1200 + index * 100} key={supplier.id}>
                    <TableRow
                      sx={{
                        '&:hover': {
                          background: 'rgba(79, 172, 254, 0.05)',
                          transform: 'scale(1.01)',
                          boxShadow: '0 4px 15px rgba(79, 172, 254, 0.1)',
                        },
                        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                        borderBottom: '1px solid rgba(0, 0, 0, 0.05)',
                      }}
                    >
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <LogoIcon size={40}>
                            <CompanyIcon />
                          </LogoIcon>
                          <Box>
                            <Typography variant="subtitle1" fontWeight="600">
                              {supplier.companyName}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              ID: {supplier.id} | {supplier.userName}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <PhoneIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {supplier.phone}
                          </Typography>
                        </Box>
                        {supplier.phoneNumberId && (
                          <Typography variant="caption" color="text.secondary" sx={{ ml: 3 }}>
                            WhatsApp ID: {supplier.phoneNumberId}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <EmailIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {supplier.userEmail}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <CalendarIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {new Date(supplier.createdAt).toLocaleDateString('tr-TR', {
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
                          onClick={(e) => handleMenuOpen(e, supplier)}
                          sx={{
                            color: 'text.secondary',
                            '&:hover': {
                              background: 'rgba(79, 172, 254, 0.1)',
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
        <MenuItem onClick={() => selectedSupplier && handleViewSupplier(selectedSupplier)}>
          <ListItemIcon>
            <VisibilityIcon fontSize="small" sx={{ color: 'primary.main' }} />
          </ListItemIcon>
          <ListItemText primary="Görüntüle" />
        </MenuItem>
        <MenuItem onClick={() => selectedSupplier && handleEditSupplier(selectedSupplier)}>
          <ListItemIcon>
            <EditIcon fontSize="small" sx={{ color: 'warning.main' }} />
          </ListItemIcon>
          <ListItemText primary="Düzenle" />
        </MenuItem>
        <Divider />
        <MenuItem onClick={() => selectedSupplier && handleDeleteSupplier(selectedSupplier)}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" sx={{ color: 'error.main' }} />
          </ListItemIcon>
          <ListItemText primary="Sil" />
        </MenuItem>
      </Menu>

      {/* Supplier Details Dialog */}
      {showSupplierDialog && selectedSupplier && (
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
              <LogoIcon size={48}>
                <CompanyIcon />
              </LogoIcon>
              <Typography variant="h4" fontWeight="700">
                Tedarikçi Detayı
              </Typography>
            </Box>
            
            <Box sx={{ mb: 3 }}>
              <Typography variant="body1" sx={{ mb: 1 }}>
                <strong>Ad Soyad:</strong> {selectedSupplier.userName}
              </Typography>
              <Typography variant="body1" sx={{ mb: 1 }}>
                <strong>Email:</strong> {selectedSupplier.userEmail}
              </Typography>
              <Typography variant="body1" sx={{ mb: 1 }}>
                <strong>Telefon:</strong> {selectedSupplier.phone}
              </Typography>
              <Typography variant="body1" sx={{ mb: 1 }}>
                <strong>ID:</strong> {selectedSupplier.id}
              </Typography>
              <Typography variant="body1" sx={{ mb: 1 }}>
                <strong>Oluşturulma:</strong> {new Date(selectedSupplier.createdAt).toLocaleDateString('tr-TR')}
              </Typography>
            </Box>
            
            <Button
              variant="contained"
              fullWidth
              onClick={() => setShowSupplierDialog(false)}
              sx={{
                background: 'linear-gradient(135deg, #4facfe, #00f2fe)',
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

      {/* Edit Supplier Dialog */}
      {showEditDialog && selectedSupplier && (
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
              Tedarikçi Düzenle
            </Typography>
            
            <TextField
              fullWidth
              label="Ad Soyad"
              value={editForm.name}
              onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
              sx={{ mb: 2 }}
            />
            
            <TextField
              fullWidth
              label="Email"
              type="email"
              value={editForm.email}
              onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
              sx={{ mb: 2 }}
            />
            
            <TextField
              fullWidth
              label="Şirket Adı"
              value={editForm.companyName}
              onChange={(e) => setEditForm({ ...editForm, companyName: e.target.value })}
              sx={{ mb: 2 }}
            />
            
            <TextField
              fullWidth
              label="Telefon"
              value={editForm.phone}
              onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
              sx={{ mb: 3 }}
            />

            <TextField
              fullWidth
              label="WhatsApp Phone Number ID"
              value={editForm.phoneNumberId || ''}
              onChange={(e) => setEditForm({ ...editForm, phoneNumberId: e.target.value })}
              sx={{ mb: 3 }}
              placeholder="WhatsApp Business phone_number_id"
              helperText="WhatsApp Business API'deki phone_number_id. Cevapların yönlenmesi için gereklidir."
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
                onClick={handleUpdateSupplier}
                disabled={loading}
                sx={{
                  background: 'linear-gradient(135deg, #4facfe, #00f2fe)',
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
      {showDeleteDialog && selectedSupplier && (
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
              Tedarikçiyi Sil
            </Typography>
            
            <Typography variant="body1" sx={{ mb: 3 }}>
              <strong>{selectedSupplier.companyName}</strong> tedarikçisini silmek istediğinizden emin misiniz? 
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

      {/* Add Supplier Dialog */}
      <Dialog 
        open={open} 
        onClose={() => setOpen(false)} 
        maxWidth="sm" 
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
          background: 'linear-gradient(135deg, #4facfe, #00f2fe)',
          color: 'white',
          fontWeight: 700,
          textAlign: 'center',
          py: 2
        }}>
          Yeni Tedarikçi Oluştur
        </DialogTitle>
        <DialogContent sx={{ p: 3 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2, mt: 1 }}>
            <TextField 
              label="Ad Soyad" 
              value={form.name} 
              onChange={(e) => setForm({ ...form, name: e.target.value })} 
              fullWidth 
              sx={{ mb: 2 }}
            />
            <TextField 
              label="Email" 
              type="email" 
              value={form.email} 
              onChange={(e) => setForm({ ...form, email: e.target.value })} 
              fullWidth 
              sx={{ mb: 2 }}
            />
            <TextField 
              label="Şifre" 
              type="password" 
              value={form.password} 
              onChange={(e) => setForm({ ...form, password: e.target.value })} 
              fullWidth 
              sx={{ mb: 2 }}
            />
            <TextField 
              label="Şirket Adı" 
              value={form.companyName} 
              onChange={(e) => setForm({ ...form, companyName: e.target.value })} 
              fullWidth 
              sx={{ mb: 2 }}
            />
            <TextField 
              label="Telefon" 
              value={form.phone} 
              onChange={(e) => setForm({ ...form, phone: e.target.value })} 
              fullWidth 
              sx={{ mb: 2 }}
            />
            <TextField 
              label="WhatsApp Phone Number ID" 
              value={form.phoneNumberId} 
              onChange={(e) => setForm({ ...form, phoneNumberId: e.target.value })} 
              fullWidth 
              sx={{ mb: 2 }}
              placeholder="WhatsApp Business API'den alınan phone_number_id"
              helperText="Bu alan opsiyoneldir. WhatsApp Business API'den telefon doğruladıktan sonra phone_number_id'yi buraya ekleyebilirsiniz."
            />
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 3, gap: 2 }}>
          <Button 
            onClick={() => setOpen(false)}
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
            onClick={async () => {
            try {
              setError('');
              // Basit istemci doğrulamaları
              if (!form.name.trim() || !form.email.trim() || !form.password.trim() || !form.companyName.trim() || !form.phone.trim()) {
                setError('Tüm alanlar zorunludur');
                return;
              }
              if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
                setError('Geçerli bir email adresi giriniz');
                return;
              }
              if (form.password.length < 6) {
                setError('Şifre en az 6 karakter olmalıdır');
                return;
              }
              await apiService.adminCreateSupplier(form);
              setOpen(false);
              setForm({ name: '', email: '', password: '', companyName: '', phone: '', phoneNumberId: '' });
              const data = await apiService.getSuppliers();
              setSuppliers(data);
            } catch (e: any) {
              const msg = e.response?.data?.message || e.response?.data?.error || 'Tedarikçi oluşturulamadı';
              setError(msg);
            }
            }}
            sx={{
              background: 'linear-gradient(135deg, #4facfe, #00f2fe)',
              borderRadius: 2,
              py: 1.5,
              px: 3,
              fontWeight: 600,
              textTransform: 'none',
              '&:hover': {
                background: 'linear-gradient(135deg, #3d8bfe, #00d4ff)',
              },
            }}
          >
            Kaydet
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SuppliersPage;
