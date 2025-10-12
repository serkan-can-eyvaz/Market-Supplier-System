import React, { useEffect, useState } from 'react';
import {
  Box,
  Container,
  Typography,
  Button,
  Card,
  CardContent,
  CardMedia,
  Chip,
  Avatar,
  Divider,
  IconButton,
  useTheme,
  useMediaQuery,
  Fade,
  Slide,
  Zoom
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import {
  SmartToy,
  LocalShipping,
  Route,
  Assessment,
  Store,
  Business,
  Speed,
  Security,
  Support,
  CheckCircle,
  TrendingUp,
  Group,
  Timeline,
  Map,
  Analytics,
  CloudSync,
  MobileFriendly
} from '@mui/icons-material';

const LandingPage: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const [activeFeature, setActiveFeature] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setActiveFeature((prev) => (prev + 1) % features.length);
    }, 4000);
    return () => clearInterval(interval);
  }, []);

  const features = [
    {
      icon: <SmartToy sx={{ fontSize: 40 }} />,
      title: 'AI Destekli WhatsApp Asistanı',
      description: 'Tamamen yapay zeka tabanlı, context-aware WhatsApp asistanı. Doğal dil anlayışı ile sipariş oluşturma.',
      color: '#4CAF50'
    },
    {
      icon: <Route sx={{ fontSize: 40 }} />,
      title: 'Otomatik Rota Planlama',
      description: 'Google Maps entegrasyonu ile akıllı rota optimizasyonu. En kısa yol hesaplama ve teslimat sıralaması.',
      color: '#2196F3'
    },
    {
      icon: <Assessment sx={{ fontSize: 40 }} />,
      title: 'PDF Raporlama Sistemi',
      description: 'Otomatik günlük/haftalık teslimat raporları. Detaylı analiz ve performans metrikleri.',
      color: '#FF9800'
    },
    {
      icon: <Timeline sx={{ fontSize: 40 }} />,
      title: 'Gerçek Zamanlı Takip',
      description: 'Canlı teslimat takibi ve durum güncellemeleri. Anlık bildirimler ve şeffaf süreç yönetimi.',
      color: '#9C27B0'
    }
  ];

  const benefits = [
    {
      title: 'Market Sahipleri İçin',
      icon: <Store />,
      items: [
        'WhatsApp üzerinden kolay sipariş',
        'Tedarikçi eşleştirme sistemi',
        'Otomatik fiyat hesaplama',
        'Sipariş geçmişi takibi'
      ]
    },
    {
      title: 'Tedarikçiler İçin',
      icon: <Business />,
      items: [
        'Akıllı rota planlama',
        'Teslimat optimizasyonu',
        'Detaylı performans raporları',
        'Müşteri iletişim yönetimi'
      ]
    }
  ];

  const techStack = [
    { name: 'Spring Boot', type: 'Backend' },
    { name: 'React', type: 'Frontend' },
    { name: 'Flutter', type: 'Mobile' },
    { name: 'PostgreSQL', type: 'Database' },
    { name: 'WhatsApp API', type: 'Integration' },
    { name: 'Google Maps', type: 'Maps' },
    { name: 'AI/LLM', type: 'AI' },
    { name: 'Docker', type: 'DevOps' }
  ];

  return (
    <Box sx={{ bgcolor: 'background.default', minHeight: '100vh' }}>
      {/* Hero Section */}
      <Box
        sx={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          py: { xs: 8, md: 12 },
          position: 'relative',
          overflow: 'hidden'
        }}
      >
        <Container maxWidth="lg">
          <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, alignItems: 'center', gap: 4 }}>
            <Box sx={{ flex: 1 }}>
              <Fade in timeout={1000}>
                <Box>
                  <Typography
                    variant="h1"
                    sx={{
                      fontSize: { xs: '2.5rem', md: '3.5rem' },
                      fontWeight: 700,
                      mb: 2,
                      lineHeight: 1.2
                    }}
                  >
                    Tedarik Asistanı
                  </Typography>
                  <Typography
                    variant="h4"
                    sx={{
                      fontSize: { xs: '1.2rem', md: '1.5rem' },
                      mb: 3,
                      opacity: 0.9,
                      fontWeight: 400
                    }}
                  >
                    AI Destekli Sipariş ve Teslimat Yönetimi
                  </Typography>
                  <Typography
                    variant="body1"
                    sx={{
                      fontSize: { xs: '1rem', md: '1.1rem' },
                      mb: 4,
                      opacity: 0.8,
                      lineHeight: 1.6
                    }}
                  >
                    Market sahipleri ile tedarikçileri buluşturan akıllı WhatsApp asistanı. 
                    Otomatik rota planlama, gerçek zamanlı takip ve kapsamlı raporlama sistemi.
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                    <Button
                      variant="contained"
                      size="large"
                      onClick={() => navigate('/login')}
                      sx={{
                        bgcolor: 'white',
                        color: '#667eea',
                        px: 4,
                        py: 1.5,
                        fontWeight: 600,
                        '&:hover': {
                          bgcolor: '#f5f5f5'
                        }
                      }}
                    >
                      Giriş Yap
                    </Button>
                    <Button
                      variant="outlined"
                      size="large"
                      onClick={() => {
                        const benefitsSection = document.getElementById('benefits-section');
                        if (benefitsSection) {
                          benefitsSection.scrollIntoView({ behavior: 'smooth' });
                        }
                      }}
                      sx={{
                        borderColor: 'white',
                        color: 'white',
                        px: 4,
                        py: 1.5,
                        fontWeight: 600,
                        '&:hover': {
                          borderColor: 'white',
                          bgcolor: 'rgba(255,255,255,0.1)'
                        }
                      }}
                    >
                      Hemen Başlayın
                    </Button>
                  </Box>
                </Box>
              </Fade>
            </Box>
            <Box sx={{ flex: 1 }}>
              <Slide direction="left" in timeout={1200}>
                <Box sx={{ textAlign: 'center' }}>
                  <Box
                    sx={{
                      width: { xs: 250, md: 350 },
                      height: { xs: 250, md: 350 },
                      mx: 'auto',
                      borderRadius: '50%',
                      bgcolor: 'rgba(255,255,255,0.1)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      backdropFilter: 'blur(10px)',
                      border: '2px solid rgba(255,255,255,0.2)'
                    }}
                  >
                    <SmartToy sx={{ fontSize: { xs: 120, md: 160 }, opacity: 0.8 }} />
                  </Box>
                </Box>
              </Slide>
            </Box>
          </Box>
        </Container>
      </Box>

      {/* Features Section */}
      <Container maxWidth="lg" sx={{ py: 8 }}>
        <Box sx={{ textAlign: 'center', mb: 6 }}>
          <Typography variant="h2" sx={{ mb: 2, fontWeight: 700, color: '#667eea' }}>
            Özellikler
          </Typography>
          <Typography variant="h6" sx={{ color: 'text.secondary', maxWidth: 600, mx: 'auto' }}>
            Modern teknoloji ile güçlendirilmiş kapsamlı çözüm
          </Typography>
        </Box>

        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)' }, gap: 4 }}>
          {features.map((feature, index) => (
            <Box key={index}>
              <Zoom in timeout={800 + index * 200}>
                <Card
                  sx={{
                    height: '100%',
                    textAlign: 'center',
                    p: 3,
                    transition: 'all 0.3s ease',
                    border: activeFeature === index ? `2px solid ${feature.color}` : '2px solid transparent',
                    '&:hover': {
                      transform: 'translateY(-8px)',
                      boxShadow: `0 8px 25px rgba(0,0,0,0.15)`
                    }
                  }}
                >
                  <Avatar
                    sx={{
                      bgcolor: feature.color,
                      width: 80,
                      height: 80,
                      mx: 'auto',
                      mb: 2
                    }}
                  >
                    {feature.icon}
                  </Avatar>
                  <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                    {feature.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {feature.description}
                  </Typography>
                </Card>
              </Zoom>
            </Box>
          ))}
        </Box>
      </Container>

      {/* Benefits Section */}
      <Box id="benefits-section" sx={{ bgcolor: '#f8f9fa', py: 8 }}>
        <Container maxWidth="lg">
          <Typography variant="h2" sx={{ textAlign: 'center', mb: 6, fontWeight: 700, color: '#667eea' }}>
            Kimler İçin?
          </Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 6 }}>
            {benefits.map((benefit, index) => (
              <Box key={index}>
                <Fade in timeout={1000 + index * 300}>
                  <Card sx={{ height: '100%', p: 4 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                      <Avatar sx={{ bgcolor: '#667eea', mr: 2 }}>
                        {benefit.icon}
                      </Avatar>
                      <Typography variant="h5" sx={{ fontWeight: 600 }}>
                        {benefit.title}
                      </Typography>
                    </Box>
                    <Box>
                      {benefit.items.map((item, itemIndex) => (
                        <Box key={itemIndex} sx={{ display: 'flex', alignItems: 'center', mb: 1.5 }}>
                          <CheckCircle sx={{ color: '#4CAF50', mr: 1, fontSize: 20 }} />
                          <Typography variant="body1">{item}</Typography>
                        </Box>
                      ))}
                    </Box>
                  </Card>
                </Fade>
              </Box>
            ))}
          </Box>
        </Container>
      </Box>

      {/* Tech Stack Section */}
      <Container maxWidth="lg" sx={{ py: 8 }}>
        <Box sx={{ textAlign: 'center', mb: 6 }}>
          <Typography variant="h2" sx={{ mb: 2, fontWeight: 700, color: '#667eea' }}>
            Teknoloji Stack
          </Typography>
          <Typography variant="h6" sx={{ color: 'text.secondary' }}>
            Modern ve güvenilir teknolojiler ile geliştirildi
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: 2 }}>
          {techStack.map((tech, index) => (
            <Box key={index}>
              <Chip
                label={tech.name}
                variant="outlined"
                sx={{
                  borderColor: '#667eea',
                  color: '#667eea',
                  fontWeight: 500,
                  px: 1
                }}
              />
            </Box>
          ))}
        </Box>
      </Container>

      {/* CTA Section */}
      <Box
        sx={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          py: 8
        }}
      >
        <Container maxWidth="md" sx={{ textAlign: 'center' }}>
          <Typography variant="h3" sx={{ mb: 3, fontWeight: 700 }}>
            Hemen Başlayın
          </Typography>
          <Typography variant="h6" sx={{ mb: 4, opacity: 0.9 }}>
            Market Supplier System ile işletmenizi dijitalleştirin ve verimliliğinizi artırın
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
            <Button
              variant="contained"
              size="large"
                      sx={{
                        bgcolor: 'white',
                        color: '#667eea',
                        px: 4,
                        py: 1.5,
                        fontWeight: 600,
                        '&:hover': {
                          bgcolor: '#f5f5f5'
                        }
                      }}
                    >
                      WhatsApp ile Demo
                    </Button>
            <Button
              variant="outlined"
              size="large"
              sx={{
                borderColor: 'white',
                color: 'white',
                px: 4,
                py: 1.5,
                fontWeight: 600,
                '&:hover': {
                  borderColor: 'white',
                  bgcolor: 'rgba(255,255,255,0.1)'
                }
              }}
            >
              İletişime Geç
            </Button>
          </Box>
        </Container>
      </Box>

      {/* Footer */}
      <Box sx={{ bgcolor: '#4a5568', color: 'white', py: 4 }}>
        <Container maxWidth="lg">
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 4 }}>
            <Box>
              <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                Tedarik Asistanı
              </Typography>
              <Typography variant="body2" sx={{ opacity: 0.8 }}>
                AI destekli sipariş ve teslimat yönetim platformu. 
                Market sahipleri ile tedarikçileri buluşturan modern çözüm.
              </Typography>
            </Box>
            <Box>
              <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                İletişim
              </Typography>
              <Typography variant="body2" sx={{ opacity: 0.8 }}>
                Web: localhost:3000<br />
                Email: info@localhost.com<br />
                WhatsApp: +90 XXX XXX XX XX
              </Typography>
            </Box>
          </Box>
          <Divider sx={{ my: 3, borderColor: 'rgba(255,255,255,0.2)' }} />
          <Typography variant="body2" sx={{ textAlign: 'center', opacity: 0.6 }}>
            © 2024 Tedarik Asistanı. Tüm hakları saklıdır.
          </Typography>
        </Container>
      </Box>
    </Box>
  );
};

export default LandingPage;
