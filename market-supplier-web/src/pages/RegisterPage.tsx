import React, { useState } from 'react';
import {
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Box,
  Alert,
  Link,
} from '@mui/material';
import { Formik, Form, Field } from 'formik';
import * as Yup from 'yup';
import { useNavigate } from 'react-router-dom';
import apiService from '../services/api';

const validationSchema = Yup.object({
  name: Yup.string().required('Ad soyad gereklidir'),
  email: Yup.string()
    .email('Geçerli bir email adresi giriniz')
    .required('Email gereklidir'),
  password: Yup.string()
    .min(6, 'Şifre en az 6 karakter olmalıdır')
    .required('Şifre gereklidir'),
  confirmPassword: Yup.string()
    .oneOf([Yup.ref('password')], 'Şifreler eşleşmiyor')
    .required('Şifre tekrarı gereklidir'),
});

const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const [error, setError] = useState<string>('');

  const handleSubmit = async (values: any) => {
    try {
      setError('');
      await apiService.register(values);
      navigate('/login');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Kayıt olurken bir hata oluştu');
    }
  };

  return (
    <Box sx={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #2E7D32 0%, #4CAF50 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      position: 'relative',
      overflow: 'hidden'
    }}>
      <Container component="main" maxWidth="sm" sx={{ position: 'relative', zIndex: 1 }}>
        <Paper elevation={3} sx={{ 
          mt: 0, 
          p: 4,
          background: 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(20px)',
          borderRadius: 4,
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.1)',
          border: '1px solid rgba(255, 255, 255, 0.2)'
        }}>
          <Typography component="h1" variant="h4" align="center" gutterBottom sx={{ color: '#2E7D32', fontWeight: 'bold' }}>
            Tedarik Asistanı
          </Typography>
          <Typography component="h2" variant="h6" align="center" gutterBottom sx={{ mb: 4, color: 'text.secondary' }}>
            Hesap Oluştur
          </Typography>
        
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Formik
          initialValues={{
            name: '',
            email: '',
            password: '',
            confirmPassword: '',
          }}
          validationSchema={validationSchema}
          onSubmit={handleSubmit}
        >
          {({ values, errors, touched, handleChange, handleBlur }) => (
            <Form>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Field
                  as={TextField}
                  name="name"
                  label="Ad Soyad"
                  fullWidth
                  value={values.name}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={touched.name && Boolean(errors.name)}
                  helperText={touched.name && errors.name}
                />

                <Field
                  as={TextField}
                  name="email"
                  label="Email"
                  type="email"
                  fullWidth
                  value={values.email}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={touched.email && Boolean(errors.email)}
                  helperText={touched.email && errors.email}
                />

                <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                  <Box sx={{ flex: '1 1 200px', minWidth: '200px' }}>
                    <Field
                      as={TextField}
                      name="password"
                      label="Şifre"
                      type="password"
                      fullWidth
                      value={values.password}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      error={touched.password && Boolean(errors.password)}
                      helperText={touched.password && errors.password}
                    />
                  </Box>

                  <Box sx={{ flex: '1 1 200px', minWidth: '200px' }}>
                    <Field
                      as={TextField}
                      name="confirmPassword"
                      label="Şifre Tekrar"
                      type="password"
                      fullWidth
                      value={values.confirmPassword}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      error={touched.confirmPassword && Boolean(errors.confirmPassword)}
                      helperText={touched.confirmPassword && errors.confirmPassword}
                    />
                  </Box>
                </Box>

                {/* Tedarikçi tek rol olduğu için telefon/rol/şirket/adres kaldırıldı */}

                <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
                  <Button
                    type="submit"
                    variant="contained"
                    fullWidth
                    size="large"
                    sx={{
                      background: 'linear-gradient(135deg, #2E7D32 0%, #4CAF50 100%)',
                      py: 1.5,
                      fontSize: '1.1rem',
                      fontWeight: 'bold',
                      textTransform: 'none',
                      borderRadius: 2,
                      boxShadow: '0 8px 25px rgba(46, 125, 50, 0.3)',
                      '&:hover': {
                        background: 'linear-gradient(135deg, #1B5E20 0%, #388E3C 100%)',
                        boxShadow: '0 12px 35px rgba(46, 125, 50, 0.4)',
                        transform: 'translateY(-2px)',
                      }
                    }}
                  >
                    Kayıt Ol
                  </Button>
                  <Button
                    variant="outlined"
                    fullWidth
                    size="large"
                    onClick={() => navigate('/login')}
                    sx={{
                      borderColor: '#2E7D32',
                      color: '#2E7D32',
                      py: 1.5,
                      fontSize: '1.1rem',
                      fontWeight: 'bold',
                      textTransform: 'none',
                      borderRadius: 2,
                      '&:hover': {
                        borderColor: '#1B5E20',
                        backgroundColor: 'rgba(46, 125, 50, 0.04)',
                      }
                    }}
                  >
                    Giriş Yap
                  </Button>
                </Box>
              </Box>
            </Form>
          )}
        </Formik>
      </Paper>
      
      {/* Login Link */}
      <Box sx={{ textAlign: 'center', mt: 3 }}>
        <Typography variant="body2" sx={{ color: 'white' }}>
          Zaten hesabınız var mı?{' '}
          <Link 
            component="button" 
            variant="body2" 
            onClick={() => navigate('/login')}
            sx={{ 
              color: 'white', 
              textDecoration: 'underline',
              '&:hover': { textDecoration: 'none' }
            }}
          >
            Giriş Yap
          </Link>
        </Typography>
      </Box>
    </Container>
    </Box>
  );
};

export default RegisterPage;