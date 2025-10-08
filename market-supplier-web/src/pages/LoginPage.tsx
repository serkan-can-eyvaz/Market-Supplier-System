import React, { useState } from 'react';
import {
  Box,
  TextField,
  Button,
  Typography,
  Link,
  Alert,
  Container,
  Paper,
  Avatar,
  InputAdornment,
  IconButton,
  Fade,
  Slide,
  Chip,
  Stack,
  Divider,
} from '@mui/material';
import {
  Visibility,
  VisibilityOff,
  Email,
  Lock,
  Business,
  Storefront,
  AdminPanelSettings,
  Login as LoginIcon,
  Security,
  SmartToy,
} from '@mui/icons-material';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LoginRequest } from '../types';

const LoginPage: React.FC = () => {
  const [formData, setFormData] = useState<LoginRequest>({
    email: '',
    password: '',
  });
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [showPassword, setShowPassword] = useState<boolean>(false);
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleTogglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await login(formData);
      
      // Role'e göre yönlendirme yap
      const userRole = response.user?.role;
      
      if (userRole === 'ADMIN') {
        window.location.href = '/admin-dashboard';
      } else if (userRole === 'SUPPLIER') {
        window.location.href = '/supplier-dashboard';
      } else if (userRole === 'MARKET') {
        window.location.href = '/market-dashboard';
      } else {
        window.location.href = '/dashboard';
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Giriş yapılırken bir hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      position: 'relative',
      overflow: 'hidden'
    }}>
      {/* Background Pattern */}
      <Box sx={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        background: `
          radial-gradient(circle at 20% 80%, rgba(102, 126, 234, 0.3) 0%, transparent 50%),
          radial-gradient(circle at 80% 20%, rgba(118, 75, 162, 0.3) 0%, transparent 50%),
          radial-gradient(circle at 40% 40%, rgba(139, 92, 246, 0.2) 0%, transparent 50%)
        `,
        zIndex: 0
      }} />

      <Container maxWidth="sm" sx={{ position: 'relative', zIndex: 1 }}>
        <Slide direction="up" in={true} timeout={800}>
          <Paper sx={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            borderRadius: 4,
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
            overflow: 'hidden',
            border: '1px solid rgba(255, 255, 255, 0.2)'
          }}>
            {/* Header Section */}
            <Box sx={{
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              color: 'white',
              p: 4,
              textAlign: 'center',
              position: 'relative'
            }}>
              <Box sx={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                background: 'url("data:image/svg+xml,%3Csvg width="60" height="60" viewBox="0 0 60 60" xmlns="http://www.w3.org/2000/svg"%3E%3Cg fill="none" fill-rule="evenodd"%3E%3Cg fill="%23ffffff" fill-opacity="0.1"%3E%3Ccircle cx="30" cy="30" r="2"/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")',
                opacity: 0.3
              }} />
              
              <Avatar sx={{
                width: 80,
                height: 80,
                mx: 'auto',
                mb: 2,
                background: 'rgba(255, 255, 255, 0.2)',
                border: '3px solid rgba(255, 255, 255, 0.3)',
                position: 'relative',
                zIndex: 1
              }}>
                <SmartToy sx={{ fontSize: 40 }} />
              </Avatar>
              
              <Typography variant="h4" sx={{ 
                fontWeight: 'bold', 
                mb: 1,
                position: 'relative',
                zIndex: 1
              }}>
                Tedarik Asistanı
              </Typography>
              
              <Typography variant="h6" sx={{ 
                opacity: 0.9,
                position: 'relative',
                zIndex: 1
              }}>
                Hoş Geldiniz
              </Typography>
            </Box>

            {/* Login Form */}
            <Box sx={{ p: 4 }}>
              <Fade in={true} timeout={1000}>
                <Box>
                  <Typography variant="h5" sx={{ 
                    textAlign: 'center', 
                    mb: 3, 
                    color: 'text.primary',
                    fontWeight: 'bold'
                  }}>
                    Giriş Yap
                  </Typography>

                  {error && (
                    <Alert 
                      severity="error" 
                      sx={{ 
                        mb: 3,
                        borderRadius: 2,
                        background: 'rgba(244, 67, 54, 0.1)',
                        border: '1px solid rgba(244, 67, 54, 0.2)'
                      }}
                    >
                      {error}
                    </Alert>
                  )}

                  <Box component="form" onSubmit={handleSubmit}>
                    <TextField
                      fullWidth
                      required
                      id="email"
                      label="E-posta Adresi"
                      name="email"
                      type="email"
                      autoComplete="email"
                      autoFocus
                      value={formData.email}
                      onChange={handleChange}
                      sx={{ mb: 3 }}
                      InputProps={{
                        startAdornment: (
                          <InputAdornment position="start">
                            <Email color="action" />
                          </InputAdornment>
                        ),
                      }}
                    />

                    <TextField
                      fullWidth
                      required
                      name="password"
                      label="Şifre"
                      type={showPassword ? 'text' : 'password'}
                      id="password"
                      autoComplete="current-password"
                      value={formData.password}
                      onChange={handleChange}
                      sx={{ mb: 3 }}
                      InputProps={{
                        startAdornment: (
                          <InputAdornment position="start">
                            <Lock color="action" />
                          </InputAdornment>
                        ),
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton
                              onClick={handleTogglePasswordVisibility}
                              edge="end"
                            >
                              {showPassword ? <VisibilityOff /> : <Visibility />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      }}
                    />

                    <Button
                      type="submit"
                      fullWidth
                      variant="contained"
                      size="large"
                      disabled={loading}
                      startIcon={<LoginIcon />}
                      sx={{
                        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                        py: 1.5,
                        fontSize: '1.1rem',
                        fontWeight: 'bold',
                        textTransform: 'none',
                        borderRadius: 2,
                        boxShadow: '0 8px 25px rgba(102, 126, 234, 0.3)',
                        '&:hover': {
                          background: 'linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%)',
                          boxShadow: '0 12px 35px rgba(102, 126, 234, 0.4)',
                          transform: 'translateY(-2px)',
                        },
                        '&:disabled': {
                          background: 'rgba(0, 0, 0, 0.12)',
                          color: 'rgba(0, 0, 0, 0.26)',
                        }
                      }}
                    >
                      {loading ? 'Giriş yapılıyor...' : 'Giriş Yap'}
                    </Button>
                  </Box>

                  {/* Features Section */}
                  <Box sx={{ mt: 4 }}>
                    <Divider sx={{ mb: 3 }}>
                      <Chip label="Sistem Özellikleri" size="small" />
                    </Divider>
                    
                    <Stack spacing={2}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Avatar sx={{ 
                          width: 40, 
                          height: 40, 
                          background: 'linear-gradient(135deg, #4caf50 0%, #8bc34a 100%)',
                          color: 'white'
                        }}>
                          <AdminPanelSettings />
                        </Avatar>
                        <Box>
                          <Typography variant="body2" fontWeight="bold">
                            Admin Yönetimi
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            Kullanıcı, market ve tedarikçi yönetimi
                          </Typography>
                        </Box>
                      </Box>

                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Avatar sx={{ 
                          width: 40, 
                          height: 40, 
                          background: 'linear-gradient(135deg, #2196f3 0%, #21cbf3 100%)',
                          color: 'white'
                        }}>
                          <Storefront />
                        </Avatar>
                        <Box>
                          <Typography variant="body2" fontWeight="bold">
                            Market Yönetimi
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            Sipariş ve stok takibi
                          </Typography>
                        </Box>
                      </Box>

                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Avatar sx={{ 
                          width: 40, 
                          height: 40, 
                          background: 'linear-gradient(135deg, #ff9800 0%, #ffc107 100%)',
                          color: 'white'
                        }}>
                          <Business />
                        </Avatar>
                        <Box>
                          <Typography variant="body2" fontWeight="bold">
                            Tedarikçi Ağı
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            Geniş tedarikçi ağı ve entegrasyon
                          </Typography>
                        </Box>
                      </Box>

                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Avatar sx={{ 
                          width: 40, 
                          height: 40, 
                          background: 'linear-gradient(135deg, #9c27b0 0%, #e91e63 100%)',
                          color: 'white'
                        }}>
                          <Security />
                        </Avatar>
                        <Box>
                          <Typography variant="body2" fontWeight="bold">
                            Güvenli Sistem
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            SSL şifreleme ve güvenli veri işleme
                          </Typography>
                        </Box>
                      </Box>
                    </Stack>
                  </Box>

                  {/* Register Link */}
                  <Box sx={{ textAlign: 'center', mt: 3 }}>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      Hesabınız yok mu?{' '}
                      <Link 
                        component={RouterLink} 
                        to="/register"
                        sx={{ 
                          color: '#667eea', 
                          textDecoration: 'none',
                          fontWeight: 'bold',
                          '&:hover': { 
                            textDecoration: 'underline',
                            color: '#5a6fd8'
                          }
                        }}
                      >
                        Kayıt Olun
                      </Link>
                    </Typography>
                  </Box>
                </Box>
              </Fade>
            </Box>
          </Paper>
        </Slide>
      </Container>
    </Box>
  );
};

export default LoginPage;
