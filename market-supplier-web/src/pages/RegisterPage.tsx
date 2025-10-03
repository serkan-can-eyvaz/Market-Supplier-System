import React, { useState } from 'react';
import {
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Box,
  Alert,
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
    <Container component="main" maxWidth="sm">
      <Paper elevation={3} sx={{ mt: 8, p: 4 }}>
        <Typography component="h1" variant="h4" align="center" gutterBottom>
          Kayıt Ol
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
                  >
                    Kayıt Ol
                  </Button>
                  <Button
                    variant="outlined"
                    fullWidth
                    size="large"
                    onClick={() => navigate('/login')}
                  >
                    Giriş Yap
                  </Button>
                </Box>
              </Box>
            </Form>
          )}
        </Formik>
      </Paper>
    </Container>
  );
};

export default RegisterPage;