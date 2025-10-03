import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Container, 
  Paper, 
  Typography, 
  Box, 
  Alert, 
  Button, 
  Stack,
  Divider,
  Card,
  CardContent
} from '@mui/material';
import { ArrowBack, LocationOn, Phone, Person, Email } from '@mui/icons-material';
import apiService from '../services/api';
import { Market } from '../types';

const MarketDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [market, setMarket] = useState<Market | null>(null);
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadMarket = async () => {
      if (!id) {
        setError('Market ID bulunamadı');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError('');
        const data = await apiService.getMarketById(parseInt(id));
        setMarket(data);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Market bilgileri yüklenirken hata oluştu');
      } finally {
        setLoading(false);
      }
    };

    loadMarket();
  }, [id]);

  if (loading) {
    return (
      <Container component="main" maxWidth="md">
        <Paper elevation={3} sx={{ mt: 8, p: 4 }}>
          <Typography>Yükleniyor...</Typography>
        </Paper>
      </Container>
    );
  }

  if (error) {
    return (
      <Container component="main" maxWidth="md">
        <Paper elevation={3} sx={{ mt: 8, p: 4 }}>
          <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
          <Button 
            variant="contained" 
            startIcon={<ArrowBack />}
            onClick={() => navigate(-1)}
          >
            Geri Dön
          </Button>
        </Paper>
      </Container>
    );
  }

  if (!market) {
    return (
      <Container component="main" maxWidth="md">
        <Paper elevation={3} sx={{ mt: 8, p: 4 }}>
          <Alert severity="warning">Market bulunamadı</Alert>
          <Button 
            variant="contained" 
            startIcon={<ArrowBack />}
            onClick={() => navigate(-1)}
            sx={{ mt: 2 }}
          >
            Geri Dön
          </Button>
        </Paper>
      </Container>
    );
  }

  return (
    <Container component="main" maxWidth="md">
      <Paper elevation={3} sx={{ mt: 8, p: 4 }}>
        <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 3 }}>
          <Button 
            variant="outlined" 
            startIcon={<ArrowBack />}
            onClick={() => navigate(-1)}
          >
            Geri
          </Button>
          <Typography component="h1" variant="h4" sx={{ flex: 1 }}>
            Market Detayları
          </Typography>
        </Stack>

        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h5" component="h2" gutterBottom color="primary">
              {market.name}
            </Typography>
            
            <Stack spacing={2} sx={{ mt: 2 }}>
              <Box display="flex" alignItems="center" gap={1}>
                <LocationOn color="action" />
                <Typography variant="body1">
                  <strong>Adres:</strong> {market.address}
                </Typography>
              </Box>
              
              <Box display="flex" alignItems="center" gap={1}>
                <Phone color="action" />
                <Typography variant="body1">
                  <strong>Telefon:</strong> {market.phone}
                </Typography>
              </Box>
            </Stack>
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Kullanıcı Bilgileri
            </Typography>
            <Divider sx={{ mb: 2 }} />
            
            <Stack spacing={2}>
              <Box display="flex" alignItems="center" gap={1}>
                <Person color="action" />
                <Typography variant="body1">
                  <strong>Kullanıcı Adı:</strong> {market.userName}
                </Typography>
              </Box>
              
              <Box display="flex" alignItems="center" gap={1}>
                <Email color="action" />
                <Typography variant="body1">
                  <strong>E-posta:</strong> {market.userEmail}
                </Typography>
              </Box>
              
              <Typography variant="body2" color="textSecondary">
                <strong>Oluşturulma Tarihi:</strong> {new Date(market.createdAt).toLocaleString('tr-TR')}
              </Typography>
              
              {market.orderCount !== undefined && (
                <Typography variant="body2" color="textSecondary">
                  <strong>Toplam Sipariş Sayısı:</strong> {market.orderCount}
                </Typography>
              )}
            </Stack>
          </CardContent>
        </Card>
      </Paper>
    </Container>
  );
};

export default MarketDetailPage;
