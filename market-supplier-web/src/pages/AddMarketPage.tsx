import React, { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Alert,
  Slide,
  Fade,
  Card,
  CardContent,
  CardHeader,
  Avatar,
  InputAdornment,
  IconButton,
  Stack,
  Divider,
  Chip,
  LinearProgress,
} from '@mui/material';
import {
  Storefront as StorefrontIcon,
  LocationOn as LocationIcon,
  Phone as PhoneIcon,
  WhatsApp as WhatsAppIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  ArrowBack as ArrowBackIcon,
  Business as BusinessIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';

const AddMarketPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [name, setName] = useState<string>('');
  const [address, setAddress] = useState<string>('');
  const [phone, setPhone] = useState<string>('');
  const [marketNumber, setMarketNumber] = useState<string>(''); // WhatsApp doğrulaması için istemci alanı
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [success, setSuccess] = useState<boolean>(false);


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    setLoading(true);
    
    try {
      await apiService.createMarket({ name, address, phone });
      setSuccess(true);
      setTimeout(() => {
        navigate('/my-markets');
      }, 2000);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Market eklenirken bir hata oluştu';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate('/my-markets');
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
          <Typography variant="h6">Market ekleniyor...</Typography>
        </Paper>
      </Box>
    );
  }

  if (success) {
    return (
      <Box sx={{ 
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}>
        <Fade in={true} timeout={1000}>
          <Paper sx={{
            p: 6,
            textAlign: 'center',
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            borderRadius: 4,
            maxWidth: 400
          }}>
            <CheckCircleIcon sx={{ fontSize: 80, color: 'success.main', mb: 3 }} />
            <Typography variant="h4" gutterBottom color="success.main">
              Başarılı!
            </Typography>
            <Typography variant="h6" color="text.secondary" gutterBottom>
              Market başarıyla eklendi
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Marketlerim sayfasına yönlendiriliyorsunuz...
            </Typography>
          </Paper>
        </Fade>
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
        <Box sx={{ maxWidth: 600, mx: 'auto' }}>
          {/* Header Section */}
          <Paper sx={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            borderRadius: 4,
            p: 3,
            mb: 3,
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)'
          }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
              <IconButton 
                onClick={() => navigate('/my-markets')}
                sx={{
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  color: 'white',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%)',
                    transform: 'scale(1.05)'
                  }
                }}
              >
                <ArrowBackIcon />
              </IconButton>
              <Box>
                <Typography variant="h4" sx={{ 
                  fontWeight: 'bold',
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  backgroundClip: 'text',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  mb: 1
                }}>
                  Market Ekle
                </Typography>
                <Typography variant="h6" color="text.secondary">
                  Yeni market bilgilerini girin
                </Typography>
              </Box>
            </Box>

            {error && (
              <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
                {error}
              </Alert>
            )}
          </Paper>

          {/* Form Section */}
          <Fade in={true} timeout={1000}>
            <Card sx={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              borderRadius: 4,
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
              overflow: 'hidden'
            }}>
              <CardHeader
                title="Market Bilgileri"
                avatar={
                  <Avatar sx={{ background: 'linear-gradient(135deg, #4caf50 0%, #8bc34a 100%)' }}>
                    <StorefrontIcon />
                  </Avatar>
                }
                action={
                  <Chip
                    label="Tedarikçi"
                    color="primary"
                    icon={<BusinessIcon />}
                    sx={{ fontWeight: 'bold' }}
                  />
                }
              />
              <CardContent>
                <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
                  <Stack spacing={3}>
                    {/* Market Adı */}
                    <TextField
                      fullWidth
                      label="Market Adı"
                      required
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      InputProps={{
                        startAdornment: (
                          <InputAdornment position="start">
                            <StorefrontIcon sx={{ color: 'text.secondary' }} />
                          </InputAdornment>
                        ),
                      }}
                      sx={{
                        '& .MuiOutlinedInput-root': {
                          borderRadius: 2
                        }
                      }}
                    />

                    {/* Adres */}
                    <TextField
                      fullWidth
                      label="Adres"
                      required
                      multiline
                      rows={3}
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                      InputProps={{
                        startAdornment: (
                          <InputAdornment position="start" sx={{ alignSelf: 'flex-start', mt: 1 }}>
                            <LocationIcon sx={{ color: 'text.secondary' }} />
                          </InputAdornment>
                        ),
                      }}
                      sx={{
                        '& .MuiOutlinedInput-root': {
                          borderRadius: 2
                        }
                      }}
                    />

                    {/* Telefon */}
                    <TextField
                      fullWidth
                      label="Telefon"
                      required
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      InputProps={{
                        startAdornment: (
                          <InputAdornment position="start">
                            <PhoneIcon sx={{ color: 'text.secondary' }} />
                          </InputAdornment>
                        ),
                      }}
                      sx={{
                        '& .MuiOutlinedInput-root': {
                          borderRadius: 2
                        }
                      }}
                    />

                    {/* WhatsApp Numarası */}
                    <TextField
                      fullWidth
                      label="Market Numarası (WhatsApp doğrulaması için)"
                      value={marketNumber}
                      onChange={(e) => setMarketNumber(e.target.value)}
                      helperText="Bu alan backend'e gönderilmez; doğrulama süreciniz için bilgilendiricidir."
                      InputProps={{
                        startAdornment: (
                          <InputAdornment position="start">
                            <WhatsAppIcon sx={{ color: '#25D366' }} />
                          </InputAdornment>
                        ),
                      }}
                      sx={{
                        '& .MuiOutlinedInput-root': {
                          borderRadius: 2
                        },
                        '& .MuiFormHelperText-root': {
                          color: 'text.secondary',
                          fontSize: '0.875rem'
                        }
                      }}
                    />

                    <Divider sx={{ my: 2 }} />

                    {/* Action Buttons */}
                    <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
                      <Button
                        type="submit"
                        variant="contained"
                        fullWidth
                        disabled={loading || !name.trim() || !address.trim() || !phone.trim()}
                        startIcon={<SaveIcon />}
                        sx={{
                          background: 'linear-gradient(135deg, #4caf50 0%, #8bc34a 100%)',
                          '&:hover': {
                            background: 'linear-gradient(135deg, #45a049 0%, #7cb342 100%)',
                            transform: 'translateY(-2px)',
                            boxShadow: '0 8px 25px rgba(76, 175, 80, 0.3)'
                          },
                          py: 1.5,
                          borderRadius: 2,
                          fontSize: '1.1rem',
                          fontWeight: 'bold'
                        }}
                      >
                        {loading ? 'Kaydediliyor...' : 'Market Ekle'}
                      </Button>
                      <Button
                        variant="outlined"
                        fullWidth
                        onClick={handleCancel}
                        startIcon={<CancelIcon />}
                        sx={{
                          borderColor: 'grey.400',
                          color: 'grey.600',
                          '&:hover': {
                            borderColor: 'grey.600',
                            background: 'rgba(0, 0, 0, 0.04)',
                            transform: 'translateY(-2px)'
                          },
                          py: 1.5,
                          borderRadius: 2,
                          fontSize: '1.1rem',
                          fontWeight: 'bold'
                        }}
                      >
                        İptal
                      </Button>
                    </Stack>
                  </Stack>
                </Box>
              </CardContent>
            </Card>
          </Fade>

          {/* Info Card */}
          <Fade in={true} timeout={1200}>
            <Paper sx={{
              background: 'rgba(255, 255, 255, 0.8)',
              backdropFilter: 'blur(10px)',
              borderRadius: 3,
              p: 3,
              mt: 3,
              border: '1px solid rgba(255, 255, 255, 0.2)'
            }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <BusinessIcon sx={{ color: 'primary.main', fontSize: 40 }} />
                <Box>
                  <Typography variant="h6" fontWeight="bold" gutterBottom>
                    Market Ekleme Rehberi
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    • Market adı benzersiz olmalıdır<br/>
                    • Adres bilgisi detaylı ve doğru olmalıdır<br/>
                    • Telefon numarası geçerli olmalıdır<br/>
                    • WhatsApp numarası doğrulama için kullanılacaktır
                  </Typography>
                </Box>
              </Box>
            </Paper>
          </Fade>
        </Box>
      </Slide>
    </Box>
  );
};

export default AddMarketPage;


