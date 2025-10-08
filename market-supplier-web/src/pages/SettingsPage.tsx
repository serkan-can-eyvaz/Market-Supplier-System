import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  Avatar,
  Button,
  TextField,
  Switch,
  FormControlLabel,
  Divider,
  Card,
  CardContent,
  CardHeader,
  IconButton,
  Alert,
  Snackbar,
  Slide,
  Fade,
  Chip,
  Stack,
  InputAdornment,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  Settings as SettingsIcon,
  Person as PersonIcon,
  Security as SecurityIcon,
  Notifications as NotificationsIcon,
  Save as SaveIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  Email as EmailIcon,
  Phone as PhoneIcon,
  Business as BusinessIcon,
  AdminPanelSettings as AdminIcon,
  Storefront as StorefrontIcon,
  BusinessCenter as BusinessCenterIcon,
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';

const SettingsPage: React.FC = () => {
  const { user, isAdmin, isMarket, isSupplier } = useAuth();
  const [activeTab, setActiveTab] = useState('profile');
  const [loading, setLoading] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' | 'warning' | 'info' });
  
  // Profile settings
  const [profileForm, setProfileForm] = useState({
    name: user?.name || '',
    email: user?.email || '',
    phone: '',
    company: '',
  });
  
  // Security settings
  const [securityForm, setSecurityForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showPasswords, setShowPasswords] = useState({
    current: false,
    new: false,
    confirm: false,
  });
  
  // Notification settings
  const [notifications, setNotifications] = useState({
    emailNotifications: true,
    pushNotifications: true,
    orderUpdates: true,
    deliveryUpdates: true,
    systemUpdates: true,
    marketingEmails: false,
  });
  
  // System settings
  const [systemSettings, setSystemSettings] = useState({
    language: 'tr',
    theme: 'light',
    supplierAddress: '',
    routeStart: 'address' as 'address' | 'mylocation',
  });

  useEffect(() => {
    const loadUserProfile = async () => {
      if (user) {
        try {
          setLoading(true);
          // Kullanıcı profil bilgilerini API'den çek
          const userProfile = await apiService.getUserById(user.id);
          setProfileForm({
            name: userProfile.name || user.name || '',
            email: userProfile.email || user.email || '',
            phone: (userProfile as any).phone || '',
            company: (userProfile as any).company || '',
          });
        } catch (error) {
          console.error('Profil bilgileri yüklenemedi:', error);
          // Hata durumunda mevcut user bilgilerini kullan
          setProfileForm({
            name: user.name || '',
            email: user.email || '',
            phone: '',
            company: '',
          });
          // local storage system settings
          const ss = JSON.parse(localStorage.getItem('supplier_settings') || '{}');
          setSystemSettings((prev) => ({
            ...prev,
            supplierAddress: ss.supplierAddress || '',
            routeStart: ss.routeStart || 'address',
          }));
        } finally {
          setLoading(false);
        }
      }
    };

    loadUserProfile();
  }, [user]);

  const handleSnackbarClose = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  const showSnackbar = (message: string, severity: 'success' | 'error' | 'warning' | 'info' = 'success') => {
    setSnackbar({ open: true, message, severity });
  };

  const handleProfileSave = async () => {
    try {
      setLoading(true);
      // API call to update profile
      await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate API call
      showSnackbar('Profil başarıyla güncellendi!', 'success');
    } catch (error) {
      showSnackbar('Profil güncellenirken hata oluştu!', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSecuritySave = async () => {
    if (securityForm.newPassword !== securityForm.confirmPassword) {
      showSnackbar('Yeni şifreler eşleşmiyor!', 'error');
      return;
    }
    
    try {
      setLoading(true);
      // API call to update password
      await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate API call
      showSnackbar('Şifre başarıyla güncellendi!', 'success');
      setSecurityForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (error) {
      showSnackbar('Şifre güncellenirken hata oluştu!', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleNotificationSave = async () => {
    try {
      setLoading(true);
      // API call to update notification settings
      await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate API call
      showSnackbar('Bildirim ayarları güncellendi!', 'success');
    } catch (error) {
      showSnackbar('Bildirim ayarları güncellenirken hata oluştu!', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSystemSave = async () => {
    try {
      setLoading(true);
      // API call to update system settings
      await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate API call
      localStorage.setItem('supplier_settings', JSON.stringify({
        supplierAddress: systemSettings.supplierAddress,
        routeStart: systemSettings.routeStart,
      }));
      showSnackbar('Sistem ayarları güncellendi!', 'success');
    } catch (error) {
      showSnackbar('Sistem ayarları güncellenirken hata oluştu!', 'error');
    } finally {
      setLoading(false);
    }
  };

  const getRoleIcon = () => {
    if (isAdmin) return <AdminIcon />;
    if (isMarket) return <StorefrontIcon />;
    if (isSupplier) return <BusinessCenterIcon />;
    return <PersonIcon />;
  };

  const getRoleText = () => {
    if (isAdmin) return 'Sistem Yöneticisi';
    if (isMarket) return 'Market Yöneticisi';
    if (isSupplier) return 'Tedarikçi';
    return 'Kullanıcı';
  };

  const getRoleColor = () => {
    if (isAdmin) return '#9c27b0';
    if (isMarket) return '#2196f3';
    if (isSupplier) return '#ff9800';
    return '#757575';
  };

  const tabs = [
    { id: 'profile', label: 'Profil', icon: <PersonIcon /> },
    { id: 'security', label: 'Güvenlik', icon: <SecurityIcon /> },
    { id: 'notifications', label: 'Bildirimler', icon: <NotificationsIcon /> },
    { id: 'system', label: 'Sistem', icon: <SettingsIcon /> },
  ];

  return (
    <Box sx={{ 
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      p: 3
    }}>
      <Slide direction="down" in={true} timeout={800}>
        <Paper sx={{
          maxWidth: 1200,
          mx: 'auto',
          background: 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(20px)',
          borderRadius: 4,
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
          overflow: 'hidden'
        }}>
          {/* Header */}
          <Box sx={{
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white',
            p: 4,
            textAlign: 'center'
          }}>
            <Avatar sx={{
              width: 80,
              height: 80,
              mx: 'auto',
              mb: 2,
              background: 'rgba(255, 255, 255, 0.2)',
              border: '3px solid rgba(255, 255, 255, 0.3)'
            }}>
              {getRoleIcon()}
            </Avatar>
            <Typography variant="h4" gutterBottom sx={{ fontWeight: 'bold' }}>
              Ayarlar
            </Typography>
            <Typography variant="h6" sx={{ opacity: 0.9 }}>
              {user?.name} - {getRoleText()}
            </Typography>
            <Chip
              label={getRoleText()}
              sx={{
                mt: 2,
                background: getRoleColor(),
                color: 'white',
                fontWeight: 'bold'
              }}
            />
          </Box>

          <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' } }}>
            {/* Sidebar */}
            <Box sx={{ width: { xs: '100%', md: '25%' }, minWidth: 250 }}>
              <Box sx={{ p: 2 }}>
                <List>
                  {tabs.map((tab) => (
                    <ListItem key={tab.id} disablePadding>
                      <ListItemButton
                        onClick={() => setActiveTab(tab.id)}
                        sx={{
                          borderRadius: 2,
                          mb: 1,
                          background: activeTab === tab.id 
                            ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
                            : 'transparent',
                          color: activeTab === tab.id ? 'white' : 'text.primary',
                          '&:hover': {
                            background: activeTab === tab.id 
                              ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
                              : 'rgba(102, 126, 234, 0.1)',
                          }
                        }}
                      >
                        <ListItemIcon sx={{ color: activeTab === tab.id ? 'white' : 'primary.main' }}>
                          {tab.icon}
                        </ListItemIcon>
                        <ListItemText primary={tab.label} />
                      </ListItemButton>
                    </ListItem>
                  ))}
                </List>
              </Box>
            </Box>

            {/* Content */}
            <Box sx={{ width: { xs: '100%', md: '75%' } }}>
              <Box sx={{ p: 3 }}>
                <Fade in={true} timeout={600}>
                  <Box>
                    {/* Profile Tab */}
                    {activeTab === 'profile' && (
                      <Card sx={{ mb: 3, background: 'rgba(255, 255, 255, 0.8)', backdropFilter: 'blur(10px)' }}>
                        <CardHeader
                          title="Profil Bilgileri"
                          avatar={<PersonIcon color="primary" />}
                          action={
                            <Button
                              variant="contained"
                              startIcon={<SaveIcon />}
                              onClick={handleProfileSave}
                              disabled={loading}
                              sx={{
                                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                '&:hover': {
                                  background: 'linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%)',
                                }
                              }}
                            >
                              Kaydet
                            </Button>
                          }
                        />
                        <CardContent>
                          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 3 }}>
                            <TextField
                              fullWidth
                              label="Ad Soyad"
                              value={profileForm.name}
                              onChange={(e) => setProfileForm({ ...profileForm, name: e.target.value })}
                              InputProps={{
                                startAdornment: (
                                  <InputAdornment position="start">
                                    <PersonIcon color="action" />
                                  </InputAdornment>
                                ),
                              }}
                            />
                            <TextField
                              fullWidth
                              label="E-posta"
                              type="email"
                              value={profileForm.email}
                              onChange={(e) => setProfileForm({ ...profileForm, email: e.target.value })}
                              InputProps={{
                                startAdornment: (
                                  <InputAdornment position="start">
                                    <EmailIcon color="action" />
                                  </InputAdornment>
                                ),
                              }}
                            />
                            <TextField
                              fullWidth
                              label="Telefon"
                              value={profileForm.phone}
                              onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                              InputProps={{
                                startAdornment: (
                                  <InputAdornment position="start">
                                    <PhoneIcon color="action" />
                                  </InputAdornment>
                                ),
                              }}
                            />
                            <TextField
                              fullWidth
                              label="Şirket"
                              value={profileForm.company}
                              onChange={(e) => setProfileForm({ ...profileForm, company: e.target.value })}
                              InputProps={{
                                startAdornment: (
                                  <InputAdornment position="start">
                                    <BusinessIcon color="action" />
                                  </InputAdornment>
                                ),
                              }}
                            />
                          </Box>
                        </CardContent>
                      </Card>
                    )}

                    {/* Security Tab */}
                    {activeTab === 'security' && (
                      <Card sx={{ mb: 3, background: 'rgba(255, 255, 255, 0.8)', backdropFilter: 'blur(10px)' }}>
                        <CardHeader
                          title="Güvenlik Ayarları"
                          avatar={<SecurityIcon color="primary" />}
                          action={
                            <Button
                              variant="contained"
                              startIcon={<SaveIcon />}
                              onClick={handleSecuritySave}
                              disabled={loading}
                              sx={{
                                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                '&:hover': {
                                  background: 'linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%)',
                                }
                              }}
                            >
                              Güncelle
                            </Button>
                          }
                        />
                        <CardContent>
                          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 3 }}>
                            <TextField
                              fullWidth
                              label="Mevcut Şifre"
                              type={showPasswords.current ? 'text' : 'password'}
                              value={securityForm.currentPassword}
                              onChange={(e) => setSecurityForm({ ...securityForm, currentPassword: e.target.value })}
                              sx={{ gridColumn: { xs: '1', sm: '1 / -1' } }}
                              InputProps={{
                                endAdornment: (
                                  <InputAdornment position="end">
                                    <IconButton
                                      onClick={() => setShowPasswords({ ...showPasswords, current: !showPasswords.current })}
                                      edge="end"
                                    >
                                      {showPasswords.current ? <VisibilityOffIcon /> : <VisibilityIcon />}
                                    </IconButton>
                                  </InputAdornment>
                                ),
                              }}
                            />
                            <TextField
                              fullWidth
                              label="Yeni Şifre"
                              type={showPasswords.new ? 'text' : 'password'}
                              value={securityForm.newPassword}
                              onChange={(e) => setSecurityForm({ ...securityForm, newPassword: e.target.value })}
                              InputProps={{
                                endAdornment: (
                                  <InputAdornment position="end">
                                    <IconButton
                                      onClick={() => setShowPasswords({ ...showPasswords, new: !showPasswords.new })}
                                      edge="end"
                                    >
                                      {showPasswords.new ? <VisibilityOffIcon /> : <VisibilityIcon />}
                                    </IconButton>
                                  </InputAdornment>
                                ),
                              }}
                            />
                            <TextField
                              fullWidth
                              label="Yeni Şifre Tekrar"
                              type={showPasswords.confirm ? 'text' : 'password'}
                              value={securityForm.confirmPassword}
                              onChange={(e) => setSecurityForm({ ...securityForm, confirmPassword: e.target.value })}
                              InputProps={{
                                endAdornment: (
                                  <InputAdornment position="end">
                                    <IconButton
                                      onClick={() => setShowPasswords({ ...showPasswords, confirm: !showPasswords.confirm })}
                                      edge="end"
                                    >
                                      {showPasswords.confirm ? <VisibilityOffIcon /> : <VisibilityIcon />}
                                    </IconButton>
                                  </InputAdornment>
                                ),
                              }}
                            />
                          </Box>
                        </CardContent>
                      </Card>
                    )}

                    {/* Notifications Tab */}
                    {activeTab === 'notifications' && (
                      <Card sx={{ mb: 3, background: 'rgba(255, 255, 255, 0.8)', backdropFilter: 'blur(10px)' }}>
                        <CardHeader
                          title="Bildirim Ayarları"
                          avatar={<NotificationsIcon color="primary" />}
                          action={
                            <Button
                              variant="contained"
                              startIcon={<SaveIcon />}
                              onClick={handleNotificationSave}
                              disabled={loading}
                              sx={{
                                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                '&:hover': {
                                  background: 'linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%)',
                                }
                              }}
                            >
                              Kaydet
                            </Button>
                          }
                        />
                        <CardContent>
                          <Stack spacing={2}>
                            <FormControlLabel
                              control={
                                <Switch
                                  checked={notifications.emailNotifications}
                                  onChange={(e) => setNotifications({ ...notifications, emailNotifications: e.target.checked })}
                                />
                              }
                              label="E-posta Bildirimleri"
                            />
                            <FormControlLabel
                              control={
                                <Switch
                                  checked={notifications.pushNotifications}
                                  onChange={(e) => setNotifications({ ...notifications, pushNotifications: e.target.checked })}
                                />
                              }
                              label="Push Bildirimleri"
                            />
                            <FormControlLabel
                              control={
                                <Switch
                                  checked={notifications.orderUpdates}
                                  onChange={(e) => setNotifications({ ...notifications, orderUpdates: e.target.checked })}
                                />
                              }
                              label="Sipariş Güncellemeleri"
                            />
                            <FormControlLabel
                              control={
                                <Switch
                                  checked={notifications.deliveryUpdates}
                                  onChange={(e) => setNotifications({ ...notifications, deliveryUpdates: e.target.checked })}
                                />
                              }
                              label="Teslimat Güncellemeleri"
                            />
                            <FormControlLabel
                              control={
                                <Switch
                                  checked={notifications.systemUpdates}
                                  onChange={(e) => setNotifications({ ...notifications, systemUpdates: e.target.checked })}
                                />
                              }
                              label="Sistem Güncellemeleri"
                            />
                            <FormControlLabel
                              control={
                                <Switch
                                  checked={notifications.marketingEmails}
                                  onChange={(e) => setNotifications({ ...notifications, marketingEmails: e.target.checked })}
                                />
                              }
                              label="Pazarlama E-postaları"
                            />
                          </Stack>
                        </CardContent>
                      </Card>
                    )}

                    {/* System Tab */}
                    {activeTab === 'system' && (
                      <Card sx={{ mb: 3, background: 'rgba(255, 255, 255, 0.8)', backdropFilter: 'blur(10px)' }}>
                        <CardHeader
                          title="Sistem Ayarları"
                          avatar={<SettingsIcon color="primary" />}
                          action={
                            <Button
                              variant="contained"
                              startIcon={<SaveIcon />}
                              onClick={handleSystemSave}
                              disabled={loading}
                              sx={{
                                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                '&:hover': {
                                  background: 'linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%)',
                                }
                              }}
                            >
                              Kaydet
                            </Button>
                          }
                        />
                        <CardContent>
                          <Stack spacing={3}>
                            <TextField
                              fullWidth
                              select
                              label="Dil"
                              value={systemSettings.language}
                              onChange={(e) => {
                                if (e.target.value !== 'tr') {
                                  setSnackbar({
                                    open: true,
                                    message: 'Diğer diller ilerleyen günlerde eklenecek!',
                                    severity: 'info'
                                  });
                                } else {
                                  setSystemSettings({ ...systemSettings, language: e.target.value });
                                }
                              }}
                              SelectProps={{
                                native: true,
                              }}
                            >
                              <option value="tr">Türkçe</option>
                              <option value="en">English (Yakında)</option>
                              <option value="de">Deutsch (Yakında)</option>
                              <option value="fr">Français (Yakında)</option>
                            </TextField>
                            
                            <TextField
                              fullWidth
                              select
                              label="Tema"
                              value={systemSettings.theme}
                              onChange={(e) => {
                                if (e.target.value !== 'light') {
                                  setSnackbar({
                                    open: true,
                                    message: 'Koyu tema ve otomatik tema seçenekleri ilerleyen günlerde eklenecek!',
                                    severity: 'info'
                                  });
                                } else {
                                  setSystemSettings({ ...systemSettings, theme: e.target.value });
                                }
                              }}
                              SelectProps={{
                                native: true,
                              }}
                            >
                              <option value="light">Açık</option>
                              <option value="dark">Koyu (Yakında)</option>
                              <option value="auto">Otomatik (Yakında)</option>
                            </TextField>

                            <Divider />
                            <Typography variant="subtitle1">Rota Başlangıç Ayarları</Typography>
                            <TextField
                              fullWidth
                              label="Tedarikçi Adresi (Başlangıç)"
                              placeholder="Örn: Konya, Selçuklu ..."
                              value={systemSettings.supplierAddress}
                              onChange={(e) => setSystemSettings({ ...systemSettings, supplierAddress: e.target.value })}
                            />
                            <TextField
                              fullWidth
                              select
                              label="Başlangıç Noktası"
                              value={systemSettings.routeStart}
                              onChange={(e) => setSystemSettings({ ...systemSettings, routeStart: e.target.value as any })}
                              SelectProps={{ native: true }}
                            >
                              <option value="address">Tedarikçi Adresi</option>
                              <option value="mylocation">Konumum</option>
                            </TextField>

                            <Alert severity="info" sx={{ mt: 2 }}>
                              <Typography variant="body2">
                                💡 <strong>Gelişmeler:</strong> Daha fazla dil desteği ve tema seçenekleri yakında eklenecek!
                              </Typography>
                            </Alert>
                          </Stack>
                        </CardContent>
                      </Card>
                    )}
                  </Box>
                </Fade>
              </Box>
            </Box>
          </Box>
        </Paper>
      </Slide>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert
          onClose={handleSnackbarClose}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default SettingsPage;
